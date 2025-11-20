/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.abbenchmarking

import java.io.File

/** Contains utility functions for interacting with Git. */

/**
 * Finds the absolute path to the root of the current Git repository.
 *
 * This is crucial for reliably locating JSON files (benchmark outputs) from a consistent base path,
 * regardless of where the script is executed from. It works by running `git rev-parse
 * --show-toplevel`.
 *
 * @return A [File] object representing the repository's root directory.
 */
internal fun getGitRoot(): File {
    val output = runCommand("git", "rev-parse", "--show-toplevel", workingDir = File(".")).trim()
    return File(output)
}

/**
 * Checks if the Git working directory is "clean."
 *
 * A clean state means there are no modified, staged, or untracked files. This is a critical safety
 * check to ensure that benchmarks are run against a pristine branch, preventing skewed or invalid
 * results.
 *
 * @return `true` if the directory is clean, `false` otherwise.
 */
internal fun isGitStatusClean(): Boolean {
    println("DEBUG: Checking Git status in directory: ${projectRoot.absolutePath}")
    val statusOutput = runCommand("git", "status", "--porcelain", workingDir = projectRoot).trim()
    if (statusOutput.isNotEmpty()) {
        println("DEBUG: 'git status --porcelain' found the following changes:\n---")
        println(statusOutput)
        println("---")
    } else {
        println("DEBUG: 'git status --porcelain' reported a clean working tree.")
    }
    return statusOutput.isEmpty()
}

/**
 * Gets the name of the currently active Git branch or revision.
 *
 * The script uses this to save the user's original state at the start, so it can reliably return to
 * it after all benchmark tests have been completed.
 *
 * Note: If the repository is in a "detached HEAD" state, this function will return the string
 * "HEAD".
 *
 * @return The simple string name of the current branch (e.g., "main") or "HEAD".
 */
internal fun getCurrentGitRevision(): String =
    runCommand("git", "rev-parse", "--abbrev-ref", "HEAD", workingDir = projectRoot).trim()

/** Resolves a git reference (like a branch name or HEAD) to its full commit hash. */
internal fun resolveGitCommit(rev: String): String {
    val commitHash = runCommand(*(listOf("git", "rev-parse", rev)).toTypedArray())?.trim()
    if (commitHash == null) {
        throw IllegalArgumentException("Can't find a commit hash for revision '$rev'.")
    }
    return commitHash
}

/**
 * Switches the current working directory to the specified Git revison.
 *
 * This is a core function that enables the A/B comparison, as it allows the script to
 * programmatically move between different revisions (such as branches, tags, or commit hashes) to
 * run tests.
 *
 * @param rev The Git revision to check out (e.g., a branch name, commit hash, or tag).
 * @return `true` if the checkout was successful, `false` otherwise.
 */
internal fun checkoutGitRevision(rev: String): Boolean {
    println("--- Checking out revision : $rev ---")
    return try {
        runCommand("git", "checkout", rev, workingDir = projectRoot)
        true
    } catch (e: java.lang.RuntimeException) {
        java.lang.System.err.println(
            "FAIL: Could not checkout revision '$rev'. Error: ${e.message}"
        )
        false
    }
}
