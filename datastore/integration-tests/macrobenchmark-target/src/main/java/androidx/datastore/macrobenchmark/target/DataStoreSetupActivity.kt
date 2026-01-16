/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.datastore.macrobenchmark.target

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.datastore.macrobenchmark.target.ui.theme.AndroidxTheme
import java.io.File
import java.io.FileOutputStream

class DataStoreSetupActivity : ComponentActivity() {
    var lastOperation by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        when (intent.getStringExtra(DELETE_DATASTORE_FILE)) {
            PREFERENCES -> delete(filename = "settings-preferences.preferences_pb")
            PROTO -> delete(filename = "settings-proto.pb")
            JSON -> delete(filename = "settings-json.json")
        }
        when (intent.getStringExtra(COPY_DATASTORE_FILE)) {
            PREFERENCES ->
                copyFile(
                    sourceFilename = "settings-preferences.preferences_pb",
                    destinationFilename = "settings-preferences.preferences_pb",
                )
            PROTO ->
                copyFile(
                    sourceFilename = "settings-proto.pb",
                    destinationFilename = "settings-proto.pb",
                )
            JSON ->
                copyFile(
                    sourceFilename = "settings-json.json",
                    destinationFilename = "settings-json.json",
                )
        }
        setContent {
            AndroidxTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    ) {
                        Text(text = lastOperation)
                    }
                }
            }
        }
    }

    fun copyFile(sourceFilename: String, destinationFilename: String) {
        val destinationDir = File(filesDir, "datastore")
        destinationDir.mkdirs()
        val destinationFile = File(destinationDir, destinationFilename)

        assets.open(sourceFilename).use { inputStream ->
            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        lastOperation = "Copied $destinationFilename"
    }

    fun delete(filename: String) {
        val directory = File(filesDir, "datastore")
        directory.mkdirs()
        val file = File(directory, filename)
        if (file.exists()) file.delete()

        lastOperation = "Deleted $filename"
    }

    companion object {
        const val COPY_DATASTORE_FILE = "copy datastore file"
        const val DELETE_DATASTORE_FILE = "delete datastore file"
        const val PREFERENCES = "preferences"
        const val PROTO = "proto"
        const val JSON = "json"
    }
}
