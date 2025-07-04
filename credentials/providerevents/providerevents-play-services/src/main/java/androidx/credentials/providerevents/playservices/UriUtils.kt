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

package androidx.credentials.providerevents.playservices

import android.content.Context
import android.net.Uri
import androidx.annotation.RestrictTo
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/** A util class for reading or writing credentials to URI */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class UriUtils {
    public companion object {
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
    }
}
