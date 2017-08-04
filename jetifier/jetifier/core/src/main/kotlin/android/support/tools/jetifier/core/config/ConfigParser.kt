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

package android.support.tools.jetifier.core.config

import android.support.tools.jetifier.core.utils.Log
import com.google.gson.Gson
import java.nio.file.Files
import java.nio.file.Path

object ConfigParser {

    private val tag : String = "Config"

    fun parse(pathToFile: Path) : Config? {
        Log.i(tag, "Parsing config file: '%s'", pathToFile.toUri())
        return parse(pathToFile.toFile().readText())
    }

    fun parse(inputText: String) : Config? {

        val gson = Gson()
        val conf = gson.fromJson(inputText, ConfigJson::class.java)

        return conf.getConfig()
    }

    fun loadConfigFile(configPath: Path) : Config? {
        return loadConfigFileInternal(configPath)
    }

    fun loadDefaultConfig() : Config? {
        Log.v(tag, "Using the default config '%s'", Config.DEFAULT_CONFIG_RES_PATH)

        val inputStream = javaClass.getResourceAsStream(Config.DEFAULT_CONFIG_RES_PATH)
        return parse(inputStream.reader().readText())
    }

    private fun loadConfigFileInternal(configPath: Path) : Config? {
        if (!Files.isReadable(configPath)) {
            Log.e(tag, "Cannot access the config file: '%s'", configPath)
            return null
        }

        val config = parse(configPath)
        if (config == null) {
            Log.e(tag, "Failed to parse the config file")
            return null
        }

        return config
    }
}


