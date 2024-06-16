/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.baselineprofile.gradle.consumer.task

// Implementation from:
// benchmark/benchmark-macro/src/main/java/androidx/benchmark/macro/ProfileRule.kt
internal data class ProfileRule(
    val underlying: String,
    val flags: String,
    val classDescriptor: String,
    val methodDescriptor: String?,
    val fullClassName: String,
) {
    companion object {

        private val PROFILE_RULE_REGEX = "(H?S?P?)L([^;]*);(->)?(.*)".toRegex()

        fun parse(rule: String): ProfileRule? =
            when (val result = PROFILE_RULE_REGEX.find(rule)) {
                null -> null
                else -> {
                    // Ignore `->`
                    val (flags, classDescriptor, _, methodDescriptor) = result.destructured
                    val fullClassName = classDescriptor.split("/").joinToString(".")
                    ProfileRule(rule, flags, classDescriptor, methodDescriptor, fullClassName)
                }
            }

        internal val comparator: Comparator<ProfileRule> =
            compareBy({ it.classDescriptor }, { it.methodDescriptor ?: "" })
    }
}
