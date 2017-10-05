/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.support.tools.jetifier.core.transform.pom

import android.support.tools.jetifier.core.utils.Log

/**
 * Rule that defines how to rewrite a dependency element in a POM file.
 *
 * Any dependency that is matched against [from] should be rewritten to list of the dependencies
 * defined in [to].
 */
data class PomRewriteRule(val from: PomDependency, val to: List<PomDependency>) {

    companion object {
        val TAG : String = "PomRule"
    }

    /**
     * Validates that the given [input] dependency has a valid version.
     */
    fun validateVersion(input: PomDependency, document: PomDocument? = null) : Boolean {
        if (from.version == null || input.version == null) {
            return true
        }

        if (!matches(input)) {
            return true
        }

        if (!areVersionsMatching(from.version!!, input.version!!)) {
            Log.e(TAG, "Version mismatch! Expected version '%s' but found version '%s' for " +
                    "'%s:%s' in '%s' file.", from.version, input.version, input.groupId,
                    input.artifactId, document?.file?.relativePath)
            return false
        }

        return true
    }

    /**
     * Checks if the given [version] is supported to be rewritten with a rule having [ourVersion].
     *
     * Version entry can be actually quite complicated, see the full documentation at:
     * https://maven.apache.org/pom.html#Dependencies
     */
    private fun areVersionsMatching(ourVersion: String, version: String) : Boolean {
        if (version == "latest" || version == "release") {
            return true
        }

        if (version.endsWith(",)") || version.endsWith(",]")) {
            return true
        }

        if (version.endsWith("$ourVersion]")) {
            return true
        }

        return ourVersion == version
    }

    fun matches(input: PomDependency) : Boolean {
        return input.artifactId == from.artifactId && input.groupId == from.groupId
    }
}