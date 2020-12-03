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

package com.android.tools.build.jetifier.standalone

import com.android.tools.build.jetifier.core.config.ConfigParser
import com.android.tools.build.jetifier.core.utils.Log
import com.android.tools.build.jetifier.processor.FileMapping
import com.android.tools.build.jetifier.processor.Processor
import com.android.tools.build.jetifier.processor.TimestampsPolicy
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import java.io.File
import java.nio.file.Paths

class Main {

    companion object {
        const val TAG = "Main"
        const val TOOL_NAME = "Jetifier (standalone)"

        val OPTIONS = Options()
        val OPTION_INPUT = createOption(
            argName = "i",
            argNameLong = "input",
            desc = "Input library path (jar, aar, zip), or source file (java, xml)",
            isRequired = true
        )
        val OPTION_OUTPUT = createOption(
            argName = "o",
            argNameLong = "output",
            desc = "Output file path",
            isRequired = true
        )
        val OPTION_CONFIG = createOption(
            argName = "c",
            argNameLong = "config",
            desc = "Input config path (otherwise default is used)",
            isRequired = false
        )
        val OPTION_LOG_LEVEL = createOption(
            argName = "l",
            argNameLong = "log",
            desc = "Logging level. Values: error, warning (default), info, verbose",
            isRequired = false
        )
        val OPTION_REVERSED = createOption(
            argName = "r",
            argNameLong = "reversed",
            desc = "Run reversed process (de-jetification)",
            hasArgs = false,
            isRequired = false
        )
        val OPTION_STRICT = createOption(
            argName = "s",
            argNameLong = "strict",
            desc = "Don't fallback in case rules are missing and throw errors instead",
            hasArgs = false,
            isRequired = false
        )
        val OPTION_REBUILD_TOP_OF_TREE = createOption(
            argName = "rebuildTopOfTree",
            argNameLong = "rebuildTopOfTree",
            desc = "Rebuild the zip of maven distribution according to the generated pom file." +
                "If set, all libraries being rewritten are assumed to be part of Support " +
                "Library. Not needed for jetification.",
            hasArgs = false,
            isRequired = false
        )
        val OPTION_STRIP_SIGNATURES = createOption(
            argName = "stripSignatures",
            argNameLong = "stripSignatures",
            desc = "Don't throw an error when jetifying a signed library and instead strip " +
                "the signature files.",
            hasArgs = false,
            isRequired = false
        )
        const val ELIGIBLE_TIMESTAMPS = "keepPrevious (default), epoch or now"
        val OPTION_TIMESTAMPS = createOption(
            argName = "t",
            argNameLong = "timestampsPolicy",
            desc = "Timestamps policy to use for the archived entries as their modified time. " +
                "Values: $ELIGIBLE_TIMESTAMPS.",
            hasArgs = true,
            isRequired = false
        )

        internal fun createOption(
            argName: String,
            argNameLong: String,
            desc: String,
            hasArgs: Boolean = true,
            isRequired: Boolean = true
        ): Option {
            return Option(argName, argNameLong, hasArgs, desc).apply {
                this.isRequired = isRequired
                OPTIONS.addOption(this)
            }
        }

        @JvmStatic fun main(args: Array<String>) {
            Main().run(args)
        }
    }

    fun run(args: Array<String>) {
        val cmd = parseCmdLine(args)
        if (cmd == null) {
            System.exit(1)
            return
        }

        Log.setLevel(cmd.getOptionValue(OPTION_LOG_LEVEL.opt))

        val input = File(cmd.getOptionValue(OPTION_INPUT.opt))
        val output = cmd.getOptionValue(OPTION_OUTPUT.opt)
        val rebuildTopOfTree = cmd.hasOption(OPTION_REBUILD_TOP_OF_TREE.opt)
        val isReversed = cmd.hasOption(OPTION_REVERSED.opt)
        val isStrict = cmd.hasOption(OPTION_STRICT.opt)
        val shouldStripSignatures = cmd.hasOption(OPTION_STRIP_SIGNATURES.opt)

        val timestampsPolicy = if (cmd.hasOption(OPTION_TIMESTAMPS.opt)) {
            when (val timestampOp = cmd.getOptionValue(OPTION_TIMESTAMPS.opt)) {
                "now" -> TimestampsPolicy.NOW
                "epoch" -> TimestampsPolicy.EPOCH
                "keepPrevious" -> TimestampsPolicy.KEEP_PREVIOUS
                else -> throw IllegalArgumentException(
                    "The provided value '$timestampOp' of " +
                        "'${OPTION_TIMESTAMPS.longOpt}' argument is not recognized. Eligible " +
                        "values are: $ELIGIBLE_TIMESTAMPS."
                )
            }
        } else {
            TimestampsPolicy.KEEP_PREVIOUS
        }

        val config = if (cmd.hasOption(OPTION_CONFIG.opt)) {
            val configPath = Paths.get(cmd.getOptionValue(OPTION_CONFIG.opt))
            ConfigParser.loadFromFile(configPath)
        } else {
            ConfigParser.loadDefaultConfig()
        }

        if (config == null) {
            Log.e(TAG, "Failed to load the config file")
            System.exit(1)
            return
        }

        val fileMappings = mutableSetOf<FileMapping>()
        if (rebuildTopOfTree) {
            @Suppress("DEPRECATION") // b/174695914
            val tempFile = createTempFile(suffix = "zip")
            fileMappings.add(FileMapping(input, tempFile))
        } else {
            fileMappings.add(FileMapping(input, File(output)))
        }

        val processor = Processor.createProcessor4(
            config = config,
            reversedMode = isReversed,
            rewritingSupportLib = rebuildTopOfTree,
            stripSignatures = shouldStripSignatures,
            useFallbackIfTypeIsMissing = !isStrict,
            timestampsPolicy = timestampsPolicy
        )
        val transformationResult = processor.transform2(fileMappings)

        val containsSingleJavaFiles = containsSingleJavaFiles(fileMappings)
        if (!containsSingleJavaFiles && transformationResult.numberOfLibsModified == 0) {
            // Jetifier is not needed here
            Log.w(TAG, "No references were rewritten. You don't need to run Jetifier.")
        }

        if (rebuildTopOfTree) {
            val tempFile = fileMappings.first().to
            TopOfTreeBuilder().rebuildFrom(inputZip = tempFile, outputZip = File(output))
            tempFile.delete()
        }
    }

    private fun containsSingleJavaFiles(fileMappings: Set<FileMapping>): Boolean {
        for (fileMapping in fileMappings) {
            if (fileMapping.from.name.endsWith(".java")) {
                return true
            }
        }
        return false
    }

    private fun parseCmdLine(args: Array<String>): CommandLine? {
        try {
            return DefaultParser().parse(OPTIONS, args)
        } catch (e: ParseException) {
            Log.e(TAG, e.message.orEmpty())
            HelpFormatter().printHelp(TOOL_NAME, OPTIONS)
        }
        return null
    }
}
