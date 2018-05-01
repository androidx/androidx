/*
 * Copyright 2017 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor.transform.proguard.patterns

/**
 * Runs multiple [GroupsReplacer]s on given strings.
 */
class ReplacersRunner(val replacers: List<GroupsReplacer>) {

    /**
     * Runs all the [GroupsReplacer]s on the given [input].
     *
     * The replacers have to be distinct as this method can't guarantee that output of one replacer
     * won't be matched by another replacer.
     */
    fun applyReplacers(input: String): String {
        val sb = StringBuilder()
        var lastSeenChar = 0
        var processedInput = input

        for (replacer in replacers) {
            val matcher = replacer.pattern.matcher(processedInput)

            while (matcher.find()) {
                if (lastSeenChar < matcher.start()) {
                    sb.append(processedInput, lastSeenChar, matcher.start())
                }

                val result = replacer.runReplacements(matcher)
                sb.append(result.joinToString(System.lineSeparator()))
                lastSeenChar = matcher.end()
            }

            if (lastSeenChar == 0) {
                continue
            }

            if (lastSeenChar <= processedInput.length - 1) {
                sb.append(processedInput, lastSeenChar, processedInput.length)
            }

            lastSeenChar = 0
            processedInput = sb.toString()
            sb.setLength(0)
        }
        return processedInput
    }
}