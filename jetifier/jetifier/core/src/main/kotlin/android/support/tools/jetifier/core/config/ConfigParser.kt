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
import com.google.gson.GsonBuilder
import java.io.FileNotFoundException
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path

object ConfigParser {

    private const val TAG : String = "Config"

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun writeToString(config: Config) : String {
        return gson.toJson(config.toJson())
    }

    fun writeToFile(config: Config, outputPath: Path) {
        FileWriter(outputPath.toFile()).use {
            gson.toJson(config.toJson(), it)
        }
    }

    fun parseFromString(inputText: String) : Config? {
        return gson.fromJson(inputText, Config.JsonData::class.java).toConfig()
    }

    fun loadFromFile(configPath: Path) : Config? {
        return loadConfigFileInternal(configPath)
    }

    fun loadDefaultConfig() : Config? {
        Log.v(TAG, "Using the default config '%s'", Config.DEFAULT_CONFIG_RES_PATH)

        val inputStream = javaClass.getResourceAsStream(Config.DEFAULT_CONFIG_RES_PATH)
        return parseFromString(inputStream.reader().readText())
    }

    fun loadConfigOrFail(configPath: Path?) : Config {
        if (configPath != null) {
            val config = loadConfigFileInternal(configPath)
            if (config != null) {
                return config
            }
            throw FileNotFoundException("Config file was not found at '$configPath'")
        }

        val config = loadDefaultConfig()
        if (config != null) {
            return config
        }
        throw AssertionError("The default config could not be found!")
    }

    private fun loadConfigFileInternal(configPath: Path) : Config? {
        if (!Files.isReadable(configPath)) {
            Log.e(TAG, "Cannot access the config file: '%s'", configPath)
            return null
        }

        Log.i(TAG, "Parsing config file: '%s'", configPath.toUri())
        val config = parseFromString(configPath.toFile().readText())

        if (config == null) {
            Log.e(TAG, "Failed to parseFromString the config file")
            return null
        }

        return config
    }
}
