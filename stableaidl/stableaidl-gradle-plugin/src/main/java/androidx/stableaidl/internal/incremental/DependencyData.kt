/*
 * Copyright (C) 2013 The Android Open Source Project
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
package androidx.stableaidl.internal.incremental

import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.Lists
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * Holds dependency information, including the main compiled file, secondary input files (usually
 * headers), and output files.
 *
 * Cloned from `com.android.builder.internal.incremental.DependencyData`.
 */
class DependencyData internal constructor() {
    var mainFile: String? = null

    private val mSecondaryFiles: MutableList<String> = Lists.newArrayList()
    private val mOutputFiles: MutableList<String> = Lists.newArrayList()
    private val mSecondaryOutputFiles: MutableList<String> = Lists.newArrayList()

    fun addSecondaryFile(path: String) {
        mSecondaryFiles.add(path)
    }

    val outputFiles: List<String>
        get() = mOutputFiles

    fun addOutputFile(path: String) {
        mOutputFiles.add(path)
    }

    fun addSecondaryOutputFile(path: String) {
        mSecondaryOutputFiles.add(path)
    }

    private enum class ParseMode {
        OUTPUT,
        MAIN,
        SECONDARY,
        DONE,
    }

    override fun toString(): String {
        return "DependencyData{" +
            "mMainFile='" +
            mainFile +
            '\'' +
            ", mSecondaryFiles=" +
            mSecondaryFiles +
            ", mOutputFiles=" +
            mOutputFiles +
            '}'
    }

    companion object {
        /**
         * Parses the given dependency file and returns the parsed data
         *
         * @param dependencyFile the dependency file
         */
        @Throws(IOException::class)
        fun parseDependencyFile(dependencyFile: File): DependencyData? {
            // first check if the dependency file is here.
            if (!dependencyFile.isFile()) {
                return null
            }
            Files.lines(dependencyFile.toPath(), StandardCharsets.UTF_8).use { lines ->
                return processDependencyData(Iterable { lines.iterator() })
            }
        }

        @VisibleForTesting
        fun processDependencyData(content: Iterable<String>): DependencyData? {
            // The format is technically:
            // output1 output2 [...]: dep1 dep2 [...]
            // However, the current tools generating those files guarantee that each file path
            // is on its own line, making it simpler to handle windows paths as well as path
            // with spaces in them.
            val data = DependencyData()
            var parseMode = ParseMode.OUTPUT
            for (lineOrig in content) {
                var line = lineOrig.trim { it <= ' ' }

                // check for separator at the beginning
                if (line.startsWith(":")) {
                    parseMode = ParseMode.MAIN
                    line = line.substring(1).trim { it <= ' ' }
                }
                var nextMode = parseMode

                // remove the \ at the end.
                if (line.endsWith("\\")) {
                    line = line.substring(0, line.length - 1).trim { it <= ' ' }
                }

                // detect : at the end indicating a parse mode change *after* we process this line.
                if (line.endsWith(":")) {
                    nextMode =
                        if (parseMode == ParseMode.SECONDARY) {
                            ParseMode.DONE
                        } else {
                            ParseMode.MAIN
                        }
                    line = line.substring(0, line.length - 1).trim { it <= ' ' }
                }
                if (nextMode == ParseMode.DONE) {
                    break
                }
                if (line.isNotEmpty()) {
                    when (parseMode) {
                        ParseMode.OUTPUT -> data.addOutputFile(line)
                        ParseMode.MAIN -> {
                            data.mainFile = line
                            nextMode = ParseMode.SECONDARY
                        }
                        ParseMode.SECONDARY -> data.addSecondaryFile(line)
                        else -> {}
                    }
                }
                parseMode = nextMode
            }
            return if (data.mainFile == null) {
                null
            } else data
        }
    }
}
