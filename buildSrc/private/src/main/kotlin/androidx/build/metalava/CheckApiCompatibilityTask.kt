/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build.metalava

import androidx.build.Version
import androidx.build.logging.TERMINAL_RED
import androidx.build.logging.TERMINAL_RESET
import javax.inject.Inject
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor

/**
 * This task validates that the API described in one signature txt file is compatible with the API
 * in another.
 */
@CacheableTask
internal abstract class CheckApiCompatibilityTask
@Inject
constructor(workerExecutor: WorkerExecutor) : CompatibilityMetalavaTask(workerExecutor) {

    @TaskAction
    fun exec() {
        check(bootClasspath.files.isNotEmpty()) { "Android boot classpath not set." }

        // Don't allow *any* API changes if we're comparing against a finalized API surface within
        // the same major and minor version, e.g. between 1.1.0-beta01 and 1.1.0-beta02 or 1.1.0 and
        // 1.1.1. We'll still allow changes between 1.1.0-alpha05 and 1.1.0-beta01.
        val currentVersion = version.get()
        val referenceVersion = referenceApi.get().version()
        val freezeApis = shouldFreezeApis(referenceVersion, currentVersion)

        checkApiFile(restricted = false, referenceVersion, freezeApis)

        if (restrictedApisExist()) {
            checkApiFile(restricted = true, referenceVersion, freezeApis)
        }
    }

    /**
     * Confirms that there are no compatibility errors not already listed in the baseline file.
     *
     * @param restricted whether this compatibility check is for restricted APIs
     * @param referenceVersion the version of the previously released APIs
     * @param freezeApis whether APIs are frozen and no changes should be allowed
     */
    private fun checkApiFile(restricted: Boolean, referenceVersion: Version?, freezeApis: Boolean) {
        val baseline = getBaselineFile(restricted)
        val args = buildList {
            addAll(getCompatibilityArguments(restricted, freezeApis))

            add("--error-message:compatibility:released")
            if (freezeApis && referenceVersion != null) {
                add(createFrozenCompatibilityCheckError(referenceVersion.toString()))
            } else {
                add(CompatibilityCheckError)
            }

            if (baseline.exists()) {
                add("--baseline")
                add(baseline.toString())
            }
        }
        runWithArgs(args)
    }
}

fun shouldFreezeApis(referenceVersion: Version?, currentVersion: Version) =
    referenceVersion != null &&
        currentVersion.major == referenceVersion.major &&
        currentVersion.minor == referenceVersion.minor &&
        referenceVersion.isFinalApi()

private const val CompatibilityCheckError =
    """
    ${TERMINAL_RED}Your change has API compatibility issues. Fix the code according to the messages above.$TERMINAL_RESET

    If you *intentionally* want to break compatibility, you can suppress it with
    ./gradlew ignoreApiChanges && ./gradlew updateApi
"""

private fun createFrozenCompatibilityCheckError(referenceVersion: String) =
    """
    ${TERMINAL_RED}The API surface was finalized in $referenceVersion. Revert the changes noted in the errors above.$TERMINAL_RESET

    If you have obtained permission from Android API Council or Jetpack Working Group to bypass this policy, you can suppress this check with:
    ./gradlew ignoreApiChanges && ./gradlew updateApi
"""
