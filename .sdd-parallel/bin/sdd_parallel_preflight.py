#!/usr/bin/env python3

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
from typing import Any


def fail(message: str) -> None:
    print(f"Parallel SDD preflight failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def run(command: list[str], repo: Path) -> subprocess.CompletedProcess[str]:
    try:
        result = subprocess.run(
            command,
            cwd=repo,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
        )
    except OSError as exc:
        fail(f"cannot run {command[0]}: {exc}")
    if result.returncode != 0:
        fail(
            f"{' '.join(command)} exited {result.returncode}\n"
            f"{result.stderr or result.stdout}"
        )
    return result


def command_path(environment_name: str, default: str) -> str | None:
    explicit = os.environ.get(environment_name)
    if explicit:
        return explicit
    return shutil.which(default)


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(65536), b""):
            digest.update(block)
    return digest.hexdigest()


def tree_hash(root: Path) -> str:
    digest = hashlib.sha256()
    if not root.exists():
        return ""
    for file in sorted(path for path in root.rglob("*") if path.is_file()):
        relative = file.relative_to(root).as_posix()
        digest.update(relative.encode())
        digest.update(b"\0")
        digest.update(sha256_file(file).encode())
        digest.update(b"\n")
    return digest.hexdigest()


def load_json(path: Path, default: Any) -> Any:
    if not path.is_file():
        return default
    try:
        return json.loads(path.read_text())
    except (OSError, json.JSONDecodeError) as exc:
        fail(f"cannot read {path}: {exc}")


def write_json_atomic(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(f".{path.name}.tmp-{os.getpid()}")
    temporary.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n")
    temporary.replace(path)


def semantic_version(value: str) -> tuple[int, int, int]:
    match = re.search(r"(\d+)\.(\d+)\.(\d+)", value)
    if not match:
        fail(f"cannot parse semantic version from {value!r}")
    return tuple(int(part) for part in match.groups())


def minimum_version(repo: Path, framework: str) -> str:
    config = repo / ".sdd-parallel/config.yaml"
    if not config.is_file():
        fail(f"Parallel SDD configuration is missing: {config}")
    in_minimum_versions = False
    for line in config.read_text().splitlines():
        if line == "minimum_versions:":
            in_minimum_versions = True
            continue
        if in_minimum_versions and line and not line.startswith(" "):
            break
        if in_minimum_versions:
            match = re.fullmatch(
                rf"  {re.escape(framework)}:\s*[\"']?([^\"']+)[\"']?\s*",
                line,
            )
            if match:
                return match.group(1)
    fail(f"minimum_versions.{framework} is missing from {config}")


def discover_repo() -> Path:
    script = Path(__file__).resolve()
    if len(script.parents) >= 3 and script.parents[1].name == ".sdd-parallel":
        return script.parents[2]
    result = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    if result.returncode != 0:
        fail("run from a Git repository or install under .sdd-parallel/bin")
    return Path(result.stdout.strip()).resolve()


def state_path_for(repo: Path) -> Path:
    result = subprocess.run(
        ["git", "rev-parse", "--git-path", "sdd-parallel-framework-state.json"],
        cwd=repo,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    if result.returncode == 0 and result.stdout.strip():
        candidate = Path(result.stdout.strip())
        return candidate if candidate.is_absolute() else repo / candidate
    return repo / ".sdd-parallel/.runtime/framework-state.json"


def openspec_agent_files(repo: Path) -> list[Path]:
    patterns = (
        ".cursor/commands/opsx-*.md",
        ".cursor/skills/openspec-*/SKILL.md",
        ".codex/skills/openspec-*/SKILL.md",
        ".agents/skills/openspec-*/SKILL.md",
        ".claude/commands/opsx/*.md",
        ".claude/skills/openspec-*/SKILL.md",
    )
    return sorted(
        {path for pattern in patterns for path in repo.glob(pattern) if path.is_file()}
    )


def openspec_continue_files(repo: Path) -> list[Path]:
    return [
        path
        for path in openspec_agent_files(repo)
        if path.name == "opsx-continue.md"
        or "openspec-continue-change" in path.parts
    ]


def openspec_profile(openspec: str, repo: Path) -> dict[str, Any]:
    result = run([openspec, "config", "list", "--json"], repo)
    try:
        profile = json.loads(result.stdout)
    except json.JSONDecodeError:
        fail(f"openspec config list returned invalid JSON: {result.stdout}")
    if not isinstance(profile, dict):
        fail("openspec config list did not return an object")
    return profile


def openspec_profile_fingerprint(
    repo: Path,
    version: str,
    profile: dict[str, Any],
    agent_files: list[Path],
) -> str:
    return hashlib.sha256(
        json.dumps(
            {
                "version": version,
                "profile": profile.get("profile"),
                "delivery": profile.get("delivery", "both"),
                "workflows": profile.get("workflows", []),
                "agent_files": [
                    path.relative_to(repo).as_posix() for path in agent_files
                ],
            },
            sort_keys=True,
        ).encode()
    ).hexdigest()


def openspec_preflight(
    repo: Path, replace_existing: bool, previous: dict[str, Any]
) -> dict[str, Any]:
    openspec = command_path("OPENSPEC_BIN", "openspec")
    if not openspec:
        return {"status": "activation-pending", "reason": "openspec-not-installed"}
    node = command_path("NODE_BIN", "node")
    if not node:
        fail("OpenSpec is installed but Node.js is unavailable")
    generator = repo / ".sdd-parallel/bin/generate_openspec_overlay.mjs"
    if not generator.is_file():
        fail(f"OpenSpec overlay generator is missing: {generator}")
    version = run([openspec, "--version"], repo).stdout.strip()
    agent_files = openspec_agent_files(repo)
    lifecycle_refreshed = False
    profile_fingerprint = ""
    if agent_files:
        profile = openspec_profile(openspec, repo)
        workflows = profile.get("workflows", [])
        delivery = profile.get("delivery", "both")
        if profile.get("profile", "core") != "custom" or "continue" not in workflows:
            fail(
                "OpenSpec agent integration is initialized but the official "
                "continue workflow is not enabled. Run repo AI-fication, approve "
                "adding continue to the developer-global OpenSpec profile, and "
                "then run openspec update."
            )
        if delivery not in ("commands", "both"):
            fail(
                "OpenSpec continue is enabled but command delivery is disabled. "
                "Run repo AI-fication and approve changing OpenSpec delivery to "
                "both, then run openspec update."
            )
        profile_fingerprint = openspec_profile_fingerprint(
            repo, version, profile, agent_files
        )
        if (
            previous.get("profile_fingerprint") != profile_fingerprint
            or not openspec_continue_files(repo)
        ):
            run([openspec, "update"], repo)
            lifecycle_refreshed = True
            agent_files = openspec_agent_files(repo)
            profile_fingerprint = openspec_profile_fingerprint(
                repo, version, profile, agent_files
            )
        if not openspec_continue_files(repo):
            fail(
                "openspec update completed without installing an official "
                "continue command/skill for the initialized agent integration"
            )
    command = [node, str(generator), "--repo", str(repo)]
    if replace_existing:
        command.append("--replace-existing")
    result = run(command, repo)
    try:
        payload = json.loads(result.stdout)
    except json.JSONDecodeError:
        fail(f"overlay generator returned invalid JSON: {result.stdout}")
    state = {
        "status": "ready" if agent_files else "activation-pending",
        "version": version,
        "schema": payload.get("schema"),
        "regenerated": bool(payload.get("changed")),
        "agent_integration": "ready" if agent_files else "not-initialized",
        "lifecycle_refreshed": lifecycle_refreshed,
    }
    if profile_fingerprint:
        state["profile_fingerprint"] = profile_fingerprint
    else:
        state["reason"] = "openspec-agent-integration-not-installed"
    print(
        "OpenSpec Parallel SDD schema regenerated."
        if state["regenerated"]
        else "OpenSpec Parallel SDD schema is current."
    )
    return state


def installed_integration_keys(repo: Path) -> list[str]:
    directory = repo / ".specify/integrations"
    if not directory.is_dir():
        return []
    return sorted(
        file.name.removesuffix(".manifest.json")
        for file in directory.glob("*.manifest.json")
        if file.name != "speckit.manifest.json"
    )


def speckit_preflight(repo: Path, previous: dict[str, Any]) -> dict[str, Any]:
    specify = command_path("SPECIFY_BIN", "specify")
    if not specify:
        return {"status": "activation-pending", "reason": "specify-not-installed"}
    version = run([specify, "--version"], repo).stdout.strip()
    required_version = minimum_version(repo, "speckit")
    if semantic_version(version) < semantic_version(required_version):
        fail(f"Spec Kit {version} is older than required {required_version}")
    keys = installed_integration_keys(repo)
    source = repo / ".sdd-parallel/adapters/spec-kit"
    source_hash = tree_hash(source)
    integration_hash = tree_hash(repo / ".specify/integrations")
    current_fingerprint = hashlib.sha256(
        json.dumps(
            {
                "version": version,
                "keys": keys,
                "source_hash": source_hash,
                "integration_hash": integration_hash,
            },
            sort_keys=True,
        ).encode()
    ).hexdigest()

    if not keys:
        return {
            "status": "activation-pending",
            "reason": "spec-kit-agent-integration-not-installed",
            "version": version,
        }
    if not source.is_dir():
        fail(f"Spec Kit extension source is missing: {source}")

    integration_refreshed = previous.get("input_fingerprint") != current_fingerprint
    if integration_refreshed:
        for key in keys:
            run([specify, "integration", "upgrade", key], repo)

    registry_path = repo / ".specify/extensions/.registry"
    registry = load_json(registry_path, {"extensions": {}})
    installed = registry.get("extensions", {}).get("parallel-sdd")
    installed_dir = repo / ".specify/extensions/parallel-sdd"

    if installed is None:
        if installed_dir.exists() and tree_hash(installed_dir) != source_hash:
            fail(
                "unregistered .specify/extensions/parallel-sdd differs from the "
                "managed source; review it before automatic registration"
            )
        run(
            [specify, "extension", "add", str(source), "--dev"],
            repo,
        )
    elif integration_refreshed or tree_hash(installed_dir) != source_hash:
        previous_installed_hash = previous.get("installed_extension_hash")
        current_installed_hash = tree_hash(installed_dir)
        if current_installed_hash != source_hash and (
            previous_installed_hash != current_installed_hash
        ):
            fail(
                "installed Parallel SDD extension contains untracked changes; "
                "review them before automatic refresh"
            )
        run([specify, "extension", "remove", "parallel-sdd", "--force"], repo)
        run([specify, "extension", "add", str(source), "--dev"], repo)

    refreshed_integration_hash = tree_hash(repo / ".specify/integrations")
    final_fingerprint = hashlib.sha256(
        json.dumps(
            {
                "version": version,
                "keys": keys,
                "source_hash": source_hash,
                "integration_hash": refreshed_integration_hash,
            },
            sort_keys=True,
        ).encode()
    ).hexdigest()
    state = {
        "status": "ready",
        "version": version,
        "integrations": keys,
        "input_fingerprint": final_fingerprint,
        "installed_extension_hash": tree_hash(installed_dir),
    }
    print("Spec Kit integration and Parallel SDD extension are current.")
    return state


def main() -> None:
    parser = argparse.ArgumentParser(description="Refresh Parallel SDD framework adapters")
    parser.add_argument(
        "--framework",
        choices=("all", "openspec", "speckit"),
        default="all",
    )
    parser.add_argument(
        "--replace-existing-openspec",
        action="store_true",
        help="Replace an older baseline-managed OpenSpec schema during migration",
    )
    args = parser.parse_args()
    repo = discover_repo()
    state_path = state_path_for(repo)
    state = load_json(state_path, {})

    if args.framework in ("all", "openspec"):
        state["openspec"] = openspec_preflight(
            repo,
            args.replace_existing_openspec,
            state.get("openspec", {}),
        )
    if args.framework in ("all", "speckit"):
        state["speckit"] = speckit_preflight(repo, state.get("speckit", {}))

    write_json_atomic(state_path, state)
    print(json.dumps(state, sort_keys=True))


if __name__ == "__main__":
    main()
