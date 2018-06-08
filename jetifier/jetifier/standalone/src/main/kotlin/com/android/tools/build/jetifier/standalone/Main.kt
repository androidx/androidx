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
            desc = "Input library path (jar, aar, zip)",
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
        val OPTION_VERSIONS = createOption(
            argName = "v",
            argNameLong = "versions",
            desc = "Versions of dependencies to be substituted by Jetifier. In most cases you " +
                "want to leave the default which is 'latestReleased'. Check Jetifier's config " +
                "file for more types of configurations.",
            hasArgs = true,
            isRequired = false
        )

        private fun createOption(
            argName: String,
            argNameLong: String,
            desc: String,
            hasArgs: Boolean = true,
            isRequired: Boolean = true
        ): Option {
            val op = Option(argName, argNameLong, hasArgs, desc)
            op.isRequired = isRequired
            OPTIONS.addOption(op)
            return op
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

        val inputLibrary = File(cmd.getOptionValue(OPTION_INPUT.opt))
        val output = cmd.getOptionValue(OPTION_OUTPUT.opt)
        val rebuildTopOfTree = cmd.hasOption(OPTION_REBUILD_TOP_OF_TREE.opt)

        val fileMappings = mutableSetOf<FileMapping>()
        if (rebuildTopOfTree) {
            val tempFile = createTempFile(suffix = "zip")
            fileMappings.add(FileMapping(inputLibrary, tempFile))
        } else {
            fileMappings.add(FileMapping(inputLibrary, File(output)))
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

        val versionSetName = cmd.getOptionValue(OPTION_VERSIONS.opt)
        val isReversed = cmd.hasOption(OPTION_REVERSED.opt)
        val isStrict = cmd.hasOption(OPTION_STRICT.opt)

        val processor = Processor.createProcessor(
            config = config,
            reversedMode = isReversed,
            rewritingSupportLib = rebuildTopOfTree,
            useFallbackIfTypeIsMissing = !isStrict,
            versionSetName = versionSetName)
        processor.transform(fileMappings)

        if (rebuildTopOfTree) {
            val tempFile = fileMappings.first().to
            TopOfTreeBuilder().rebuildFrom(inputZip = tempFile, outputZip = File(output))
            tempFile.delete()
        }
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

