#!/usr/bin/env python3
"""PreToolUse hook to auto-format modified Kotlin and Java files on CoG commit."""

import json
import os
from pathlib import Path
import re
import subprocess
import sys

REPO_ROOT = Path(__file__).resolve().parents[2]


def run_cmd(cmd: list[str], env: dict[str, str] | None = None) -> str | None:
    res = subprocess.run(cmd, cwd=REPO_ROOT, env=env, capture_output=True, text=True, check=False)
    if res.returncode == 0:
        return None
    return "\n".join(filter(None, (res.stdout.strip(), res.stderr.strip())))


def changed_files() -> list[str]:
    out = subprocess.run(
        ["git", "citc", "cli.diff"], cwd=REPO_ROOT, capture_output=True, text=True, check=False
    ).stdout
    return re.findall(r"^\[[MA]\*?\] (.+)$", out, re.MULTILINE)


def project_path(path: str, dir_map: dict[str, str]) -> str | None:
    if "/src/" not in path or path.startswith("buildSrc/"):
        return None
    mod_dir = path.split("/src/", 1)[0]
    return dir_map.get(mod_dir) or f":{mod_dir.replace('/', ':')}"


def main() -> int:
    raw = "" if sys.stdin.isatty() else sys.stdin.read().strip()
    in_hook = bool(raw)
    if not str(REPO_ROOT).startswith("/google/cog/"):
        if in_hook:
            print('{"decision": "allow"}')
        return 0

    payload = json.loads(raw) if in_hook else {}
    cmd = payload.get("toolCall", {}).get("args", {}).get("CommandLine", "")

    is_commit = not in_hook or bool(
        re.search(
            r"(?:^|[;&|\n])\s*(?:python3?(?:\s+-\S+)*\s+)?(?:(?:\S*/)?commit\.py|(?:(?:\S*/)?cog_api\.(?:py|sh)|git\s+citc)\b[^;|&\n]*\b(?:commit|describe))\b",
            cmd,
        )
    )
    if not is_commit:
        print('{"decision": "allow"}')
        return 0

    files = changed_files()
    kt_files = [f for f in files if f.endswith((".kt", ".ktx"))]
    java_files = [f for f in files if f.endswith(".java")]
    if not kt_files and not java_files:
        if in_hook:
            print('{"decision": "allow"}')
        return 0

    commands: list[list[str]] = []
    if kt_files:
        commands.append(["./gradlew", "-q", "--continue", ":ktCheckFile", "--format", *(f"--file={f}" for f in kt_files)])
    prefixes: list[str] = []
    if java_files:
        dir_map = {
            m.group(2): m.group(1)
            for m in re.finditer(
                r'includeProject\(\s*"([^"]+)"\s*,\s*"([^"]+)"',
                (REPO_ROOT / "settings.gradle").read_text(),
            )
        }
        prefixes = sorted({p for f in java_files if (p := project_path(f, dir_map))})
        if prefixes:
            commands.append(["./gradlew", "-q", "--continue", *(f"{p}:javaFormat" for p in prefixes)])
    env = os.environ | {"PROJECT_PREFIX": ",".join(prefixes) or ":lint-checks"}

    errors = [err for c in commands if (err := run_cmd(c, env))]
    if errors:
        msg = "Formatting failed:\n" + "\n\n".join(errors)
        if in_hook:
            print(json.dumps({"decision": "deny", "reason": msg}))
            return 0
        print(msg, file=sys.stderr)
        return 1

    if in_hook:
        print('{"decision": "allow"}')
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
