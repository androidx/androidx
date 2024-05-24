/*
 * Copyright 2022 The Android Open Source Project
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

@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.benchmark.macro

import androidx.annotation.RestrictTo

private val PROFILE_RULE_REGEX = "(H?S?P?)L([^;]*);(->)?(.*)".toRegex()

/**  */
@RestrictTo(RestrictTo.Scope.LIBRARY)
data class ProfileRule(
    val underlying: String,
    val flags: String,
    val classDescriptor: String,
    val methodDescriptor: String?
) {
    companion object {
        /** Parses a profile rule to its constituent elements. */
        @JvmStatic
        fun parse(rule: String): ProfileRule? {
            return when (val result = PROFILE_RULE_REGEX.find(rule)) {
                null -> null
                else -> {
                    // Ignore `->`
                    val (flags, classDescriptor, _, methodDescriptor) = result.destructured
                    ProfileRule(rule, flags, classDescriptor, methodDescriptor)
                }
            }
        }

        internal val comparator: Comparator<ProfileRule> =
            compareBy(
                // When building the Comparator, we need to ignore method flags.
                { profileRule -> profileRule.classDescriptor },
                { profileRule -> profileRule.methodDescriptor ?: "" }
            )
    }
}
