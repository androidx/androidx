package android.support.tools.jetifier.preprocessor

import android.support.tools.jetifier.core.archive.Archive
import android.support.tools.jetifier.core.config.Config
import android.support.tools.jetifier.core.config.ConfigParser
import android.support.tools.jetifier.core.map.LibraryMapGenerator
import java.io.File
import java.nio.file.Path

class ConfigGenerator {

    companion object {
        private const val LEGAL_NOTICE =
            "# Copyright (C) 2017 The Android Open Source Project\n" +
            "#\n" +
            "# Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
            "# you may not use this file except in compliance with the License.\n" +
            "# You may obtain a copy of the License at\n" +
            "#\n" +
            "#      http://www.apache.org/licenses/LICENSE-2.0\n" +
            "#\n" +
            "# Unless required by applicable law or agreed to in writing, software\n" +
            "# distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
            "# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
            "# See the License for the specific language governing permissions and\n" +
            "# limitations under the License\n"

        private const val GEN_NOTICE =
            "# DO NOT EDIT MANUALLY! This file was auto-generated using Jetifier preprocessor.\n" +
            "# To make some changes in the configuration edit \"default.config\" and run\n" +
            "# preprocessor/scripts/processDefaultConfig.sh script to update this file.\n"
    }

    fun generateMapping(
        config: Config,
        inputLibraries: List<File>,
        outputConfigPath: Path) {

        val mapper = LibraryMapGenerator(config)
        inputLibraries.forEach {
            if (it.isDirectory) {
                it.listFiles().forEach { fileInDir ->
                    val library = Archive.Builder.extract(fileInDir.toPath())
                    mapper.scanLibrary(library)
                }
            } else {
                val library = Archive.Builder.extract(it.toPath())
                mapper.scanLibrary(library)
            }
        }

        val map = mapper.generateMap()
        val newConfig = config.setNewMap(map)

        saveConfigToFile(newConfig, outputConfigPath.toFile())
    }

    private fun saveConfigToFile(configToSave: Config, outputFile : File) {
        val sb = StringBuilder()
        sb.append(LEGAL_NOTICE)
        sb.append("\n")
        sb.append(GEN_NOTICE)
        sb.append("\n")
        sb.append(ConfigParser.writeToString(configToSave))

        if (outputFile.exists()) {
            outputFile.delete()
        }
        outputFile.createNewFile()
        outputFile.writeText(sb.toString())
    }
}