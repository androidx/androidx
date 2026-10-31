#!/usr/bin/env python3

import os
import re
import subprocess
import sys

TERMINAL_RED = "\033[31m"
TERMINAL_RESET = "\033[0m"


def get_ktfmt_version(project_root):
    toml_path = os.path.join(project_root, "gradle", "libs.versions.toml")
    with open(toml_path, "r", encoding="utf-8") as f:
        match = re.search(r'^ktfmt\s*=\s*"([^"]+)"', f.read(), re.MULTILINE)
    if not match:
        print(f"Error: Could not find ktfmt version in {toml_path}", file=sys.stderr)
        sys.exit(1)
    return match.group(1)


def main():
    """Invoked by the built-in `ktfmt` hook in repohooks.

    Handles two modes based on the contract defined in rh/hooks.py:
    1. Check mode (receives `--dry-run --set-exit-if-changed`): Checks formatting without modifying files.
    2. Fix mode (receives no flags): Formats files in-place.

    Contract reference:
    https://android.googlesource.com/platform/tools/repohooks/+/refs/heads/androidx-main/rh/hooks.py
    """
    args = sys.argv[1:]
    flags = [arg for arg in args if arg.startswith("-")]
    files = [
        arg
        for arg in args
        if not arg.startswith("-") and arg.endswith((".kt", ".kts"))
    ]

    if not files:
        sys.exit(0)

    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    checkout_root = os.path.abspath(os.path.join(project_root, "..", ".."))

    ktfmt_version = get_ktfmt_version(project_root)
    ktfmt_jar = os.path.join(
        checkout_root,
        "prebuilts",
        "androidx",
        "external",
        "com",
        "facebook",
        "ktfmt",
        ktfmt_version,
        f"ktfmt-{ktfmt_version}-with-dependencies.jar",
    )

    jdk_platform = "darwin-arm64" if sys.platform == "darwin" else "linux-x86"
    java_bin = os.path.join(
        checkout_root, "prebuilts", "jdk", "jdk25", jdk_platform, "bin", "java"
    )

    cmd = [
        java_bin,
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--sun-misc-unsafe-memory-access=allow",
        "-Dfile.encoding=UTF-8",
        "-Duser.country=US",
        "-Duser.language=en",
        "-Duser.variant",
        "-cp",
        ktfmt_jar,
        "com.facebook.ktfmt.cli.Main",
        "--kotlinlang-style",
        *flags,
        *files,
    ]

    try:
        result = subprocess.run(cmd, capture_output=True, text=True, check=False)
        if result.stderr:
            print(result.stderr, end="", file=sys.stderr)

        output = result.stdout.strip()
        if output:
            failed_files = " ".join(output.splitlines())
            print(
                f"Failed check for the following files:\n"
                f"{output}\n\n"
                f"********************************************************************************\n"
                f"{TERMINAL_RED}You can automatically fix these issues with:\n"
                f"./development/ktfmt_wrapper.py {failed_files}{TERMINAL_RESET}\n"
                f"********************************************************************************",
                file=sys.stderr,
            )
            sys.exit(1)

        sys.exit(result.returncode)
    except Exception as e:
        print(f"Error running ktfmt: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
