/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.support.tools.jetifier.standalone

import android.support.tools.jetifier.core.Processor
import android.support.tools.jetifier.core.config.Config
import android.support.tools.jetifier.core.config.ConfigParser
import android.support.tools.jetifier.core.utils.Log
import android.support.tools.jetifier.core.utils.LogLevel
import org.apache.commons.cli.*

import java.nio.file.Paths

class Main {

    val options : Options = Options()

    companion object {
        const val TAG = "Main"

        const val TOOL_NAME = "jetifier"

        const val ARG_NAME_INPUT = "i"
        const val ARG_NAME_OUTPUT = "o"
        const val ARG_NAME_CONFIG = "c"
        const val ARG_NAME_LOG = "l"
    }


    init {
        addOption(ARG_NAME_INPUT, "Input file path", /* isRequired */ true)
        addOption(ARG_NAME_OUTPUT, "Output file path", /* isRequired */ true)
        addOption(ARG_NAME_CONFIG, "Config file path", /* isRequired */ false)
        addOption(ARG_NAME_LOG, "Logging output level. debug, verbose, default",
                /* isRequired */ false)
    }


    fun run(args : Array<String>) {
        val formatter = HelpFormatter()
        val cmd: CommandLine

        try {
            cmd = DefaultParser().parse(options, args)
        } catch (e: ParseException) {
            println(e.message)
            formatter.printHelp(TOOL_NAME, options)

            System.exit(1)
            return
        }

        Log.currentLevel = when (cmd.getOptionValue("log")) {
            "debug" -> LogLevel.DEBUG
            "verbose" -> LogLevel.VERBOSE
            else -> LogLevel.INFO
        }

        val inputPath = Paths.get(cmd.getOptionValue(ARG_NAME_INPUT))
        val outputPath = Paths.get(cmd.getOptionValue(ARG_NAME_OUTPUT))

        var config : Config?
        if (cmd.hasOption(ARG_NAME_CONFIG)) {
            val configPath = Paths.get(cmd.getOptionValue(ARG_NAME_CONFIG))
            config = ConfigParser.loadConfigFile(configPath)
        } else {
            config = ConfigParser.loadDefaultConfig()
        }

        if (config == null) {
            Log.e(TAG, "Failed to load the config file")
            System.exit(1)
            return
        }

        val processor = Processor(config)
        processor.transform(listOf(inputPath), outputPath)
    }

    private fun addOption(name: String, description: String, isRequired: Boolean) {
        val argOption = Option(name, /* hasArg: */ true, description)
        argOption.isRequired = isRequired
        options.addOption(argOption)
    }
}


fun main(args : Array<String>) {
    Main().run(args)
}