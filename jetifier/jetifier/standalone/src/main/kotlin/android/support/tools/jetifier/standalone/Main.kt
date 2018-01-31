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
        val OPTION_OUTPUT = createOption("o", "Output config path")
        val OPTION_CONFIG = createOption("c", "Input config path", isRequired = false)
        val OPTION_LOG_LEVEL = createOption("l", "Logging level. debug, verbose, default",
            isRequired = false)

        private fun createOption(argName: String,
                                 desc: String,
                                 isRequired: Boolean = true,
                                 multiple: Boolean = false) : Option {
            val op = Option(argName, true, desc)
            op.isRequired = isRequired
            if (multiple) {
                op.args = Option.UNLIMITED_VALUES
            }
            OPTIONS.addOption(op)
            return op
        }
    }

    fun run(args : Array<String>) {
        val cmd = parseCmdLine(args)
        if (cmd == null) {
            System.exit(1)
            return
        }

        Log.setLevel(cmd.getOptionValue(OPTION_LOG_LEVEL.opt))

        val inputLibraries = cmd.getOptionValues(OPTION_INPUT.opt).map { File(it) }.toSet()
        val outputPath = Paths.get(cmd.getOptionValue(OPTION_OUTPUT.opt))

        val config : Config?
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

        val processor = Processor(config)
        processor.transform(inputLibraries, outputPath)
    }

    private fun parseCmdLine(args : Array<String>) : CommandLine? {
        try {
            return DefaultParser().parse(OPTIONS, args)
        } catch (e: ParseException) {
            Log.e(TAG, e.message.orEmpty())
            HelpFormatter().printHelp(TOOL_NAME, OPTIONS)
        }
        return null
    }

}


fun main(args : Array<String>) {
    Main().run(args)
}