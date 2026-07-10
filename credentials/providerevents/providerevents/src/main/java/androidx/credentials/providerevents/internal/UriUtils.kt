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

package androidx.credentials.providerevents.internal

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.RestrictTo
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/** A util class for reading or writing credentials to URI */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class UriUtils {
    public companion object {
        private const val CREDENTIAL_TRANSFER_FILE_PATH = "import_export_temp"
        private const val CREDENTIAL_TRANSFER_FILE_NAME = "tempImportedCredentials"
        private const val TAG = "UriUtils"

        /** Write the credentials json into the provided uri. */
        public fun writeToUri(uri: Uri, responseJson: String, context: Context) {
            context.contentResolver.openOutputStream(uri).use {
                val writer = BufferedWriter(OutputStreamWriter(it))
                writer.write(responseJson)
                writer.flush()
                writer.close()
            }
        }

        /** Read the credentials json from the provided uri. */
        public fun readFromUri(uri: Uri, context: Context): String {
            var credentialsJson = ""
            context.contentResolver.openInputStream(uri).use {
                val reader = BufferedReader(InputStreamReader(it))
                credentialsJson = reader.readText()
                reader.close()
            }
            return credentialsJson
        }

        /** Creates a new temp file responsible for credential transfer and return its uri. */
        public fun generateCredentialTransferFile(context: Context): File? {
            try {
                val importExportDir = File(context.cacheDir, CREDENTIAL_TRANSFER_FILE_PATH)
                importExportDir.mkdir()
                val importExportFile = File(importExportDir, CREDENTIAL_TRANSFER_FILE_NAME)
                if (!importExportFile.createNewFile()) {
                    Log.d(TAG, "The file already exists. Cleaning it up and retrying")
                    // Could be a residual from previous session. Clean up.
                    importExportFile.delete()
                    if (importExportFile.createNewFile()) {
                        return importExportFile
                    }
                }
                return importExportFile
            } catch (e: Exception) {
                Log.d(
                    TAG,
                    "Encountered an exception while trying to set up the transfer medium.",
                    e,
                )
            }
            return null
        }
    }
}
