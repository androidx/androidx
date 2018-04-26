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

import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.core.config.ConfigParser
import com.android.tools.build.jetifier.core.pom.DependencyVersionsMap
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
        const val TOOL_NAME = "standalone"

        val OPTIONS = Options()
        val OPTION_INPUT = createOption("i", "Input libraries paths", multiple = true)
        val OPTION_OUTPUT_DIR = createOption("outputdir", "Output directory path",
            isRequired = false)
        val OPTION_OUTPUT_FILE = createOption("outputfile", "Output file", isRequired = false)
        val OPTION_CONFIG = createOption("c", "Input config path", isRequired = false)
        val OPTION_LOG_LEVEL = createOption("l", "Logging level. debug, verbose, error, info " +
            "(default)", isRequired = false)
        val OPTION_REVERSED = createOption("r", "Run reversed process", hasArgs = false,
            isRequired = false)
        val OPTION_REWRITE_SUPPORT_LIB = createOption("s", "If set, all libraries being rewritten" +
            " are assumed to be part of Support Library. Otherwise only general dependencies " +
            " are expected.", hasArgs = false, isRequired = false)
        val OPTION_STRICT = createOption("strict",
            "Don't fallback in case rules are missing", hasArgs = false, isRequired = false)
        val OPTION_REBUILD_TOP_OF_TREE = createOption("rebuildTopOfTree",
            "Rebuild the zip of maven distribution according to the generated pom file",
            hasArgs = false, isRequired = false)
        val OPTION_VERSIONS = createOption("versions",
                "Versions to be used with jetifier (latest or alpha1)",
                hasArgs = true, isRequired = false)

        private fun createOption(
            argName: String,
            desc: String,
            hasArgs: Boolean = true,
            isRequired: Boolean = true,
            multiple: Boolean = false
        ): Option {
            val op = Option(argName, hasArgs, desc)
            op.isRequired = isRequired
            if (multiple) {
                op.args = Option.UNLIMITED_VALUES
            }
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

        val inputLibraries = cmd.getOptionValues(OPTION_INPUT.opt).map { File(it) }.toSet()
        val outputDir = cmd.getOptionValue(OPTION_OUTPUT_DIR.opt)
        val outputFile = cmd.getOptionValue(OPTION_OUTPUT_FILE.opt)
        val rebuildTopOfTree = cmd.hasOption(OPTION_REBUILD_TOP_OF_TREE.opt)

        if (outputDir == null && outputFile == null) {
            throw IllegalArgumentException("Must specify -outputdir or -outputfile")
        }
        if (outputDir != null && outputFile != null) {
            throw IllegalArgumentException("Cannot specify both -outputdir and -outputfile")
        }
        if (inputLibraries.size > 1 && outputFile != null) {
            throw IllegalArgumentException(
                    "Cannot specify -outputfile when multiple input libraries are given")
        }

        val fileMappings = mutableSetOf<FileMapping>()
        if (outputFile != null) {
            if (rebuildTopOfTree) {
                val tempFile = createTempFile(suffix = "zip")
                fileMappings.add(FileMapping(inputLibraries.first(), tempFile))
            } else {
                fileMappings.add(FileMapping(inputLibraries.first(), File(outputFile)))
            }
        } else {
            inputLibraries.forEach {
                val newFileName = File(Paths.get(outputDir).toString(), it.name)
                fileMappings.add(FileMapping(it, newFileName))
            }
        }

        val config: Config?
        if (cmd.hasOption(OPTION_CONFIG.opt)) {
            val configPath = Paths.get(cmd.getOptionValue(OPTION_CONFIG.opt))
            config = ConfigParser.loadFromFile(configPath)
        } else {
            config = ConfigParser.loadDefaultConfig()
        }

        if (config == null) {
            Log.e(TAG, "Failed to load the config file")
            System.exit(1)
            return
        }

        val versionsMap = DependencyVersionsMap.parseFromVersionSetTypeId(
            if (cmd.hasOption(OPTION_VERSIONS.opt)) {
                cmd.getOptionValue(OPTION_VERSIONS.opt)
            } else {
                null
            })

        val isReversed = cmd.hasOption(OPTION_REVERSED.opt)
        val rewriteSupportLib = cmd.hasOption(OPTION_REWRITE_SUPPORT_LIB.opt)
        val isStrict = cmd.hasOption(OPTION_STRICT.opt)
        val processor = Processor.createProcessor(
            config = config,
            reversedMode = isReversed,
            rewritingSupportLib = rewriteSupportLib,
            useFallbackIfTypeIsMissing = !isStrict,
            versionsMap = versionsMap)
        processor.transform(fileMappings)

        if (rebuildTopOfTree) {
            val tempFile = fileMappings.first().to
            TopOfTreeBuilder().rebuildFrom(
                inputZip = tempFile,
                outputZip = File(outputFile))
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

