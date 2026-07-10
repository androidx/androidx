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
package androidx.abbenchmarking.common

import androidx.abbenchmarking.util.getGitRoot
import androidx.abbenchmarking.util.isGitStatusClean
import kotlin.system.exitProcess

/**
 * Performs pre-flight checks on the Git repository to ensure a valid state for benchmarking.
 *
 * This function verifies that the Git working directory is clean (no uncommitted changes).
 *
 * @throws RuntimeException if the Git working directory is not clean.
 */
internal fun performGitPreflightChecks() {
    val repoRoot = getGitRoot()
    println("DEBUG: Found repository root at: ${repoRoot.absolutePath}")
    if (!isGitStatusClean()) {
        System.err.println(
            "Git status is not clean. Please commit or stash your changes before running."
        )
        exitProcess(1)
    }
}
