#!/usr/bin/env python3

import os
import subprocess
import sys
from subprocess import CompletedProcess


def usage():
    print("""Usage: jspecify_update.py <dir>
This script updates all projects in a directory to use JSpecify annotations instead of AndroidX
nullness annotations. If no directory is provided, uses the root frameworks/support dir.
""")
    sys.exit(1)


def run_gradle_task(gradle_path: str, task: str, args: list[str] | None = None) -> \
        CompletedProcess[bytes]:
    """Runs a Gradle task ans returns the result."""
    cmd = [gradle_path, task] + (args or [])
    return subprocess.run(cmd, capture_output=True)


def update_annotations(dir_path: str, gradle_path: str) -> bool:
    """If the project in dir_path has a lint task, runs lintFix to shift all AndroidX nullness
    annotations to type-use position. Returns whether any updates were applied."""
    tasks_result = run_gradle_task(gradle_path, "tasks")
    if "lintFix" not in str(tasks_result.stdout):
        print(f"Lint task does not exist for {dir_path}")
        return False

    fix_result = run_gradle_task(gradle_path, "lintFix", ["-Pandroidx.useJSpecifyAnnotations=true"])
    return fix_result.returncode != 0


def update_imports_for_file(filepath: str) -> bool:
    """Replaces AndroidX nullness annotation imports with JSpecify imports. Returns whether any
    updates were needed."""
    with open(filepath, "r") as f:
        lines = f.readlines()

    replacements = {
        "import androidx.annotation.NonNull": "import org.jspecify.annotations.NonNull;\n",
        "import androidx.annotation.Nullable": "import org.jspecify.annotations.Nullable;\n"
    }
    updated_count = 0
    for i, line in enumerate(lines):
        for target, replacement in replacements.items():
            if target in line:
                lines[i] = replacement
                updated_count += 1
                break

        if updated_count == len(replacements):
            break

    replaced = updated_count > 0
    if replaced:
        with open(filepath, "w") as f:
            f.writelines(lines)
    return replaced


def reformat_files(gradle_path: str) -> None:
    """Runs the java formatter to fix imports for the specified files."""
    run_gradle_task(gradle_path, "javaFormat", ["--fix-imports-only"]),


def update_imports(dir_path: str, gradle_path: str) -> bool:
    """For each java file in the directory, replaces the AndroidX nullness imports with JSpecify
    imports and runs the java formatter to correct the new import order."""
    java_files = []
    for sub_dir_path, _, filenames in os.walk(dir_path):
        for filename in filenames:
            (_, ext) = os.path.splitext(filename)
            if ext == ".java":
                file_path = os.path.join(sub_dir_path, filename)
                if update_imports_for_file(file_path):
                    java_files.append(file_path)

    if java_files:
        reformat_files(gradle_path)
    return java_files


def add_jspecify_dependency(build_gradle_path: str) -> None:
    """Adds a JSpecify dependency to the build file."""
    with open(build_gradle_path, "r") as f:
        lines = f.readlines()

    jspecify_dependency = "    api(libs.jspecify)\n"
    if jspecify_dependency in lines:
        print(f"JSpecify dependency already present for {build_gradle_path}")
        return

    dependencies_start = None
    for i in range(len(lines)):
        line = lines[i]
        if line.startswith("dependencies {"):
            dependencies_start = i
            break

    if not dependencies_start:
        print(f"No dependencies block found for {build_gradle_path}")
        return

    lines.insert(dependencies_start + 1, "    api(libs.jspecify)\n")
    with open(build_gradle_path, "w") as f:
        f.writelines(lines)


def process_dir(dir_path: str, root_dir_path: str) -> None:
    """Updates the directory to use JSpecify annotations."""
    print(f"Processing {dir_path}")
    os.chdir(dir_path)
    gradle_path = os.path.join(root_dir_path, "gradlew")
    if update_annotations(dir_path, gradle_path):
        print(f"Lint fixes applied in {dir_path}")
        if update_imports(dir_path, gradle_path):
            add_jspecify_dependency(os.path.join(dir_path, "build.gradle"))


def main(start_dir: str | None) -> None:
    # Location of this script: under support/development
    script_path = os.path.realpath(__file__)
    # Move up to the support dir
    support_dir = os.path.dirname(os.path.dirname(script_path))

    # Search the specified directory, or the support dir if there wasn't one
    if not start_dir:
        start_dir = support_dir
    else:
        start_dir = os.path.abspath(start_dir)
    for dir_path, _, filenames in os.walk(start_dir):
        if dir_path == support_dir:
            continue
        if "build.gradle" in filenames:
            process_dir(dir_path, support_dir)


if __name__ == '__main__':
    if len(sys.argv) == 1:
        main(None)
    elif len(sys.argv) == 2:
        main(sys.argv[1])
    else:
        usage()
