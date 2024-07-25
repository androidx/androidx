/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.matchers

import android.widget.TextView
import androidx.pdf.util.Preconditions
import androidx.test.espresso.ViewAssertion

class SearchViewAssertions {
    private var currentMatchNumber: Int = 0
    private var totalMatchNumber: Int = 0

    fun extractAndMatch(): ViewAssertion {
        return ViewAssertion { view, _ ->
            val extractedText = (view as TextView).text ?: ""
            val matchResult = Regex(MATCH_COUNT_PATTERN).find(extractedText)
            Preconditions.checkArgument(
                matchResult != null,
                "Extracted text $extractedText does not match pattern"
            )

            val matchNumber = matchResult!!.groups[1]?.value?.toInt() ?: -1
            val totalMatches = matchResult.groups[2]?.value?.toInt() ?: -1
            Preconditions.checkArgument(
                matchNumber >= 0 && totalMatches >= 0,
                "Could not extract page number from the TextView"
            )

            currentMatchNumber = matchNumber
            totalMatchNumber = totalMatches
        }
    }

    fun matchPrevious(): ViewAssertion {
        return ViewAssertion { view, _ ->
            val extractedText = (view as TextView).text ?: ""
            val matchResult = Regex(MATCH_COUNT_PATTERN).find(extractedText)
            Preconditions.checkArgument(
                matchResult != null,
                "Extracted text $extractedText does not match pattern"
            )

            val matchNumber = matchResult!!.groups[1]?.value?.toInt() ?: -1
            val totalMatches = matchResult.groups[2]?.value?.toInt() ?: -1
            Preconditions.checkArgument(
                matchNumber >= 0 && totalMatchNumber == totalMatches,
                "Could not extract page number from the TextView"
            )

            if (currentMatchNumber == 1) {
                currentMatchNumber = totalMatchNumber
            } else {
                currentMatchNumber -= 1
            }

            Preconditions.checkArgument(
                matchNumber == currentMatchNumber,
                "Extracted match $matchNumber does not equal $currentMatchNumber"
            )
        }
    }

    fun matchNext(): ViewAssertion {
        return ViewAssertion { view, _ ->
            val extractedText = (view as TextView).text ?: ""
            val matchResult = Regex(MATCH_COUNT_PATTERN).find(extractedText)
            Preconditions.checkArgument(
                matchResult != null,
                "Extracted text $extractedText does not match pattern"
            )

            val matchNumber = matchResult!!.groups[1]?.value?.toInt() ?: -1
            val totalMatches = matchResult.groups[2]?.value?.toInt() ?: -1
            Preconditions.checkArgument(
                matchNumber >= 0 && totalMatchNumber == totalMatches,
                "Could not extract page number from the TextView"
            )

            if (currentMatchNumber == totalMatchNumber) {
                currentMatchNumber = 1
            } else {
                currentMatchNumber += 1
            }

            Preconditions.checkArgument(
                matchNumber == currentMatchNumber,
                "Extracted match $matchNumber does not equal $currentMatchNumber"
            )
        }
    }

    companion object {
        private const val MATCH_COUNT_PATTERN = """(\d+)\s/\s(\d+)"""
    }
}
