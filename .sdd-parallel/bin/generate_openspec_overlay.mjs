#!/usr/bin/env node

import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const GENERATOR_VERSION = 1;
const GENERATOR_PATH = fileURLToPath(import.meta.url);

function fail(message) {
  process.stderr.write(`Parallel SDD OpenSpec generation failed: ${message}\n`);
  process.exit(1);
}

function parseArgs(argv) {
  const args = { repo: process.cwd(), replaceExisting: false };
  for (let index = 0; index < argv.length; index += 1) {
    const value = argv[index];
    if (value === "--repo") {
      args.repo = argv[index + 1];
      index += 1;
    } else if (value === "--replace-existing") {
      args.replaceExisting = true;
    } else {
      fail(`unknown argument: ${value}`);
    }
  }
  return args;
}

function run(command, args, cwd) {
  const result = spawnSync(command, args, {
    cwd,
    encoding: "utf8",
    env: process.env,
  });
  if (result.error) {
    fail(`${command} could not run: ${result.error.message}`);
  }
  if (result.status !== 0) {
    fail(
      `${command} ${args.join(" ")} exited ${result.status}\n${result.stderr || result.stdout}`,
    );
  }
  return result.stdout.trim();
}

function sha256(content) {
  return crypto.createHash("sha256").update(content).digest("hex");
}

function hashFile(file) {
  return sha256(fs.readFileSync(file));
}

function isFile(file) {
  try {
    return fs.statSync(file).isFile();
  } catch {
    return false;
  }
}

function listFiles(root) {
  if (!fs.existsSync(root)) return [];
  const output = [];
  const visit = (directory) => {
    for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
      const absolute = path.join(directory, entry.name);
      if (entry.isDirectory()) visit(absolute);
      else if (entry.isFile()) output.push(absolute);
      else fail(`unsupported symlink or special file: ${absolute}`);
    }
  };
  visit(root);
  return output.sort();
}

function treeHashes(root, excluded = new Set()) {
  const hashes = {};
  for (const absolute of listFiles(root)) {
    const relative = path.relative(root, absolute).split(path.sep).join("/");
    if (!excluded.has(relative)) hashes[relative] = hashFile(absolute);
  }
  return hashes;
}

function treeFingerprint(root) {
  const hashes = treeHashes(root);
  return sha256(
    Object.entries(hashes)
      .map(([name, hash]) => `${name}\0${hash}`)
      .join("\n"),
  );
}

function parseVersion(value) {
  const match = String(value).match(/(\d+)\.(\d+)\.(\d+)/);
  if (!match) fail(`cannot parse semantic version from "${value}"`);
  return match.slice(1).map(Number);
}

function versionAtLeast(actual, minimum) {
  for (let index = 0; index < 3; index += 1) {
    if (actual[index] > minimum[index]) return true;
    if (actual[index] < minimum[index]) return false;
  }
  return true;
}

function appendInstruction(original, addition) {
  return [original || "", addition || ""]
    .map((item) => item.trim())
    .filter(Boolean)
    .join("\n\n");
}

function assertInside(root, candidate, label) {
  const relative = path.relative(root, candidate);
  if (relative.startsWith("..") || path.isAbsolute(relative)) {
    fail(`${label} escapes repository root: ${candidate}`);
  }
}

function validateGraph(schema, schemaDirectory) {
  if (!Array.isArray(schema.artifacts) || schema.artifacts.length === 0) {
    fail("generated schema has no artifacts");
  }
  const byId = new Map();
  for (const artifact of schema.artifacts) {
    if (!artifact?.id || byId.has(artifact.id)) {
      fail(`generated schema contains a missing or duplicate artifact id: ${artifact?.id}`);
    }
    byId.set(artifact.id, artifact);
    const template = path.resolve(schemaDirectory, "templates", artifact.template || "");
    assertInside(path.resolve(schemaDirectory, "templates"), template, "template");
    if (!isFile(template)) {
      fail(`template is missing for ${artifact.id}: ${artifact.template}`);
    }
  }

  for (const artifact of schema.artifacts) {
    for (const requirement of artifact.requires || []) {
      if (!byId.has(requirement)) {
        fail(`${artifact.id} requires unknown artifact ${requirement}`);
      }
    }
  }
  for (const requirement of schema.apply?.requires || []) {
    if (!byId.has(requirement)) {
      fail(`apply requires unknown artifact ${requirement}`);
    }
  }

  const visiting = new Set();
  const visited = new Set();
  const visit = (id) => {
    if (visiting.has(id)) fail(`artifact dependency cycle includes ${id}`);
    if (visited.has(id)) return;
    visiting.add(id);
    for (const dependency of byId.get(id).requires || []) visit(dependency);
    visiting.delete(id);
    visited.add(id);
  };
  for (const id of byId.keys()) visit(id);
}

function verifyGeneratedTree(target, metadata) {
  if (!metadata?.generated_files || typeof metadata.generated_files !== "object") {
    return false;
  }
  const actual = treeHashes(target, new Set([".parallel-sdd-generated.json"]));
  return JSON.stringify(actual) === JSON.stringify(metadata.generated_files);
}

const options = parseArgs(process.argv.slice(2));
const repo = path.resolve(options.repo);
const parallelRoot = path.join(repo, ".sdd-parallel");
const overlayPath = path.join(parallelRoot, "openspec-overlay.yaml");
const schemasRoot = path.join(repo, "openspec", "schemas");
const target = path.join(schemasRoot, "myntra-sdd");
const metadataPath = path.join(target, ".parallel-sdd-generated.json");
const openspec = process.env.OPENSPEC_BIN || "openspec";

if (!isFile(overlayPath)) fail(`overlay not found: ${overlayPath}`);

const openspecVersionOutput = run(openspec, ["--version"], repo);
const whichOutput = run(openspec, ["schema", "which", "spec-driven", "--json"], repo);
let resolution;
try {
  resolution = JSON.parse(whichOutput);
} catch {
  fail(`invalid JSON from OpenSpec schema resolution: ${whichOutput}`);
}
if (resolution.source !== "package") {
  fail(
    `base schema must resolve from the OpenSpec package, but resolved from ${resolution.source}`,
  );
}

const baseDirectory = fs.realpathSync(resolution.path);
const packageRoot = path.dirname(path.dirname(baseDirectory));
const requireFromOpenSpec = createRequire(path.join(packageRoot, "package.json"));
let yaml;
try {
  yaml = requireFromOpenSpec("yaml");
} catch (error) {
  fail(`cannot load OpenSpec's YAML dependency: ${error.message}`);
}

const overlay = yaml.parse(fs.readFileSync(overlayPath, "utf8"));
const actualVersion = parseVersion(openspecVersionOutput);
const minimumVersion = parseVersion(overlay.minimum_openspec_version);
if (!versionAtLeast(actualVersion, minimumVersion)) {
  fail(
    `OpenSpec ${openspecVersionOutput} is older than required ${overlay.minimum_openspec_version}`,
  );
}

const customTemplatePaths = Object.values(overlay.template_sources || {}).map((relative) => {
  const absolute = path.resolve(repo, relative);
  assertInside(repo, absolute, "custom template");
  if (!isFile(absolute)) fail(`custom template not found: ${relative}`);
  return absolute;
});
const sourceFingerprint = sha256(
  JSON.stringify({
    generator_version: GENERATOR_VERSION,
    generator_script: hashFile(GENERATOR_PATH),
    openspec_version: openspecVersionOutput,
    base_schema_tree: treeFingerprint(baseDirectory),
    overlay: hashFile(overlayPath),
    custom_templates: customTemplatePaths.map((file) => [path.basename(file), hashFile(file)]),
  }),
);

if (fs.existsSync(target)) {
  if (isFile(metadataPath)) {
    const metadata = JSON.parse(fs.readFileSync(metadataPath, "utf8"));
    if (!verifyGeneratedTree(target, metadata)) {
      fail(
        `generated schema contains local modifications; preserve them and resolve before regeneration: ${target}`,
      );
    }
    if (metadata.source_fingerprint === sourceFingerprint) {
      process.stdout.write(
        `${JSON.stringify({ changed: false, schema: "myntra-sdd", openspec_version: openspecVersionOutput })}\n`,
      );
      process.exit(0);
    }
  } else if (!options.replaceExisting) {
    fail(
      `existing schema is not marked as generated; rerun controlled AI-fication migration before replacing ${target}`,
    );
  }
}

const baseSchema = yaml.parse(fs.readFileSync(path.join(baseDirectory, "schema.yaml"), "utf8"));
const baseIds = new Set((baseSchema.artifacts || []).map((artifact) => artifact.id));
for (const required of overlay.required_base_artifacts || []) {
  if (!baseIds.has(required)) {
    fail(`upstream schema is incompatible: required artifact "${required}" is missing`);
  }
}

const generatedSchema = structuredClone(baseSchema);
generatedSchema.name = overlay.output_schema.name;
generatedSchema.version = overlay.output_schema.version;
generatedSchema.description = `${baseSchema.description || ""} ${overlay.output_schema.description_suffix || ""}`.trim();

for (const artifact of generatedSchema.artifacts) {
  artifact.instruction = appendInstruction(
    overlay.preflight_instruction,
    appendInstruction(
      artifact.instruction,
      overlay.artifact_instruction_suffixes?.[artifact.id],
    ),
  );
}

for (const insertion of overlay.insertions || []) {
  if (generatedSchema.artifacts.some((artifact) => artifact.id === insertion.artifact.id)) {
    fail(`overlay artifact already exists upstream: ${insertion.artifact.id}`);
  }
  const anchor = generatedSchema.artifacts.findIndex(
    (artifact) => artifact.id === insertion.after,
  );
  if (anchor < 0) fail(`overlay insertion anchor is missing: ${insertion.after}`);
  const artifact = structuredClone(insertion.artifact);
  artifact.instruction = appendInstruction(
    overlay.preflight_instruction,
    artifact.instruction,
  );
  generatedSchema.artifacts.splice(anchor + 1, 0, artifact);
}

for (const [artifactId, additions] of Object.entries(
  overlay.dependency_additions || {},
)) {
  const artifact = generatedSchema.artifacts.find((item) => item.id === artifactId);
  if (!artifact) fail(`dependency patch target is missing: ${artifactId}`);
  artifact.requires = [...new Set([...(artifact.requires || []), ...additions])];
}

if (!generatedSchema.apply) fail("upstream schema no longer defines apply");
generatedSchema.apply.requires = [
  ...new Set([
    ...(generatedSchema.apply.requires || []),
    ...(overlay.apply_requirements || []),
  ]),
];
generatedSchema.apply.instruction = appendInstruction(
  overlay.preflight_instruction,
  appendInstruction(generatedSchema.apply.instruction, overlay.apply_instruction_suffix),
);

fs.mkdirSync(schemasRoot, { recursive: true });
const stage = fs.mkdtempSync(path.join(schemasRoot, ".myntra-sdd-stage-"));
const backup = `${target}.backup-${process.pid}`;
let targetMoved = false;
let targetInstalled = false;
try {
  fs.cpSync(baseDirectory, stage, { recursive: true });
  for (const [templateName, sourceRelative] of Object.entries(
    overlay.template_sources || {},
  )) {
    const source = path.resolve(repo, sourceRelative);
    const destination = path.join(stage, "templates", templateName);
    assertInside(path.join(stage, "templates"), destination, "generated template");
    fs.copyFileSync(source, destination);
  }
  fs.writeFileSync(
    path.join(stage, "schema.yaml"),
    yaml.stringify(generatedSchema, { lineWidth: 0 }),
  );
  validateGraph(generatedSchema, stage);

  const metadata = {
    generated_by: "parallel-sdd-overlay",
    generator_version: GENERATOR_VERSION,
    overlay_version: overlay.overlay_version,
    openspec_version: openspecVersionOutput,
    base_schema: overlay.base_schema,
    source_fingerprint: sourceFingerprint,
    generated_files: treeHashes(stage),
  };
  fs.writeFileSync(
    path.join(stage, ".parallel-sdd-generated.json"),
    `${JSON.stringify(metadata, null, 2)}\n`,
  );

  if (fs.existsSync(target)) {
    fs.renameSync(target, backup);
    targetMoved = true;
  }
  fs.renameSync(stage, target);
  targetInstalled = true;

  run(openspec, ["schema", "validate", "myntra-sdd", "--json"], repo);
  if (targetMoved) fs.rmSync(backup, { recursive: true });
} catch (error) {
  if (targetInstalled && fs.existsSync(target)) fs.rmSync(target, { recursive: true });
  if (targetMoved && fs.existsSync(backup)) fs.renameSync(backup, target);
  if (fs.existsSync(stage)) fs.rmSync(stage, { recursive: true });
  fail(error instanceof Error ? error.message : String(error));
}

process.stdout.write(
  `${JSON.stringify({ changed: true, schema: "myntra-sdd", openspec_version: openspecVersionOutput })}\n`,
);
