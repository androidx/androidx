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

package android.support.tools.jetifier.preprocessor

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
        const val TOOL_NAME = "preprocessor"

        val OPTIONS = Options()
        val OPTION_INPUT_LIBS = createOption("i", "Input libraries paths", multiple = true)
        val OPTION_INPUT_CONFIG = createOption("c", "Input config path")
        val OPTION_OUTPUT_CONFIG = createOption("o", "Output config path")
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

        val inputLibraries = cmd.getOptionValues(OPTION_INPUT_LIBS.opt).map { File(it) }
        val inputConfigPath = Paths.get(cmd.getOptionValue(OPTION_INPUT_CONFIG.opt))
        val outputConfigPath = Paths.get(cmd.getOptionValue(OPTION_OUTPUT_CONFIG.opt))

        val config = ConfigParser.loadFromFile(inputConfigPath)
        if (config == null) {
            System.exit(1)
            return
        }

        val generator = ConfigGenerator()
        generator.generateMapping(config, inputLibraries, outputConfigPath)
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