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

package androidx.aab.analysis

data class SubScore(val label: String, val score: Int?, val maxScore: Int, val issues: List<Issue>)

fun List<SubScore>.print() {
    var confirmedTotal = 0
    var possibleTotal = 0
    var max = 0

    forEach {
        println("----------------------- ${(it.label + " ").padEnd(50, '-')}")
        println("    ${it.score} / ${it.maxScore} Points\n")
        it.issues
            .sortedBy { issue -> issue.severity.order }
            .forEach { issue ->
                println("  ${issue.severity} - ${issue.summary}")
                if (issue.impact != null) {
                    println("    Impact:\n      ${issue.impact.trim().replace("\n", "\n      ")}")
                }
                if (issue.suggestion != null) {
                    println(
                        "    Suggestion:\n      ${issue.suggestion.trim().replace("\n", "\n      ")}"
                    )
                }
            }
        if (it.score == null) {
            possibleTotal += it.maxScore
        } else {
            confirmedTotal += it.score
            possibleTotal += it.score
        }
        max += it.maxScore
    }
    println("----------------------- FINAL SCORE --------------------------------------")
    if (possibleTotal == confirmedTotal) {
        println("    $confirmedTotal / $max Points")
    } else {
        println("    Between $confirmedTotal and $possibleTotal / $max Points")
        println("NOTE: range is reported due to missing metadata")
    }
}

interface ScoreReporter {
    fun getSubScore(): SubScore
}
