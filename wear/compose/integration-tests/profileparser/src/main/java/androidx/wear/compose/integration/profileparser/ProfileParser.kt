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

package androidx.wear.compose.integration.profileparser

import java.io.File

class ProfileParser {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if (args.size < 3) {
                println(
                    "Usage: profileparser <input file> <target e.g. wear/compose/material> " +
                        "<output file>"
                )
                return
            }

            println("Input: ${args[0]}")
            println("Parse: ${args[1]}")
            println("Output: ${args[2]}")

            val input = File(args[0])
            val parse = args[1].let { if (it.endsWith("/")) it else "$it/" }
            val output = File(args[2]).printWriter()

            val lines =
                input.useLines {
                    it.toList()
                        .map { it.truncatedAt(';') }
                        .map { it.truncatedAt('$') }
                        .filter {
                            it.contains(parse) && ExcludePaths.none { path -> it.contains(path) }
                        }
                        .fold(mutableMapOf()) {
                            acc: MutableMap<String, MutableList<String>>,
                            item: String ->
                            // Collect unique keys based on the androidx/xxxx part of the name
                            // and accumulate a list of flags as the map value e.g. HSPL, L, SPL
                            val idx = item.indexOf("androidx")
                            val key = item.substring(idx)
                            val flags = item.substring(0, idx)
                            acc.getOrPut(key) { mutableListOf() }.add(flags)
                            acc
                        }
                        .map { (key, flags) ->
                            val flag = "HSPL".filter { c -> flags.any { flag -> flag.contains(c) } }
                            flag + key
                        }
                        .map {
                            // Tag on wild cards.
                            if (it.startsWith("L")) {
                                it + ";"
                            } else if (it.endsWith("Kt")) {
                                it + "**->**(**)**"
                            } else {
                                it + ";->**(**)**"
                            }
                        }
                        .sortedBy { it.substring(it.indexOf("androidx")) }
                }
            output.use { out -> lines.forEach { out.println(it) } }
            println("Success!")
        }

        private fun String.truncatedAt(c: Char): String {
            val idx = this.indexOf(c)
            return if (idx == -1) this else this.substring(0, idx)
        }

        private val ExcludePaths = listOf("/material3/macrobenchmark/", "/material3/samples/")
    }
}
