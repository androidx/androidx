/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro.perfetto

internal data class Slice(
    val name: String,
    val ts: Long,
    val dur: Long
) {
    val endTs: Long = ts + dur
    val frameId = name.substringAfterLast(" ").toIntOrNull()

    fun contains(targetTs: Long): Boolean {
        return targetTs >= ts && targetTs <= (ts + dur)
    }

    companion object {

        fun parseListFromQueryResult(queryResult: String): List<Slice> {
            val resultLines = queryResult.split("\n").onEach {
                println("query result line $it")
            }

            if (resultLines.first() != """"name","ts","dur"""") {
                throw IllegalStateException("query failed!")
            }

            // results are in CSV with a header row, and strings wrapped with quotes
            return resultLines
                .filter { it.isNotBlank() } // drop blank lines
                .drop(1) // drop the header row
                .map {
                    val columns = it.split(",")
                    Slice(
                        name = columns[0].unquote(),
                        ts = columns[1].toLong(),
                        dur = columns[2].toLong()
                    )
                }
        }
    }
}