#!/usr/bin/env python3

import os
import sys
import subprocess


def main():
    """Invoked by the built-in `ktfmt` hook in repohooks.

    Handles two modes based on the contract defined in rh/hooks.py:
    1. Check mode (receives `--dry-run`): Runs `./gradlew :ktCheckFile --file=...`
    2. Fix mode (receives no flags): Runs `./gradlew :ktCheckFile --format --file=...`

    Contract reference:
    https://android.googlesource.com/platform/tools/repohooks/+/refs/heads/androidx-main/rh/hooks.py
    """
    args = sys.argv[1:]
    is_check = False
    files = []

    for arg in args:
        if arg == "--dry-run":
            is_check = True
        elif not arg.startswith("-"):
            files.append(arg)

    if not files:
        sys.exit(0)

    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    gradlew = os.path.join(project_root, "gradlew")

    cmd = [gradlew, "-q", "-p", project_root, "--continue", ":ktCheckFile"]

    if not is_check:
        cmd.append("--format")

    for f in files:
        cmd.append(f"--file={f}")

    try:
        result = subprocess.run(cmd, capture_output=True, text=True)

        if is_check:
            if result.returncode != 0:
                print(result.stdout)
                print(result.stderr, file=sys.stderr)
                sys.exit(result.returncode)
            else:
                sys.exit(0)
        else:
            print(result.stdout)
            print(result.stderr, file=sys.stderr)
            sys.exit(result.returncode)

    except Exception as e:
        print(f"Error running Gradle: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
