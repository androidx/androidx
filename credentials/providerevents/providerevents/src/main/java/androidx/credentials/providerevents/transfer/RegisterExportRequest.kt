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

package androidx.credentials.providerevents.transfer

import android.graphics.Bitmap
import androidx.credentials.providerevents.internal.MatcherUtil
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.json.JSONArray
import org.json.JSONObject

/**
 * The request to register the export info.
 *
 * @param entries the entries to be displayed to the users on the provider selector ui. The entries
 *   will be displayed in the order provided.
 * @param exportMatcher the optional matcher. By default, a matcher that filters on credential types
 *   will be used. The provider can provide a matcher to filter based on custom filters
 */
public class RegisterExportRequest(
    public val entries: List<ExportEntry>,
    public val exportMatcher: ByteArray = MatcherUtil.CREDENTIAL_TRANSFER_DEFAULT_MATCHER,
) {
    public val credentialBytes: ByteArray = this.toCredentialBytes()

    internal companion object {
        private const val HEADER_SIZE = 3

        private fun getIconBytes(icon: Bitmap): ByteArrayOutputStream {
            val scaledIcon = Bitmap.createScaledBitmap(icon, 24, 24, true)
            val stream = ByteArrayOutputStream()
            scaledIcon.compress(Bitmap.CompressFormat.PNG, 100, stream)
            return stream
        }

        private fun ExportEntry.toJson(iconIndex: Int): JSONObject {
            val entry = JSONObject()
            val displayInfo = JSONObject()
            val credTypes = JSONArray()
            this.accountDisplayName?.let {
                displayInfo.put("account_name", this.accountDisplayName)
            }
            displayInfo.put("user_name", this.userDisplayName)
            displayInfo.put("icon_id", iconIndex)

            supportedCredentialTypes.forEach { credTypes.put(it) }

            entry.put("display_info", displayInfo)
            entry.put("supported_credential_types", credTypes)
            entry.put("id", id)
            return entry
        }

        private fun RegisterExportRequest.toCredentialBytes(): ByteArray {
            val json = JSONObject()
            val entryListJson = JSONArray()
            val icons = ByteArrayOutputStream()
            val iconSizeList = mutableListOf<Int>()
            this.entries.forEach { entry ->
                val iconBytes = getIconBytes(entry.icon)
                entryListJson.put(entry.toJson(iconSizeList.size))
                iconSizeList.add(iconBytes.size())
                iconBytes.writeTo(icons)
            }
            json.put("entries", entryListJson)
            val entriesBytes = json.toString(0).toByteArray()
            val result = ByteArrayOutputStream()
            // header_size (there are three headers)
            result.write(intBytes((HEADER_SIZE + iconSizeList.size) * Int.SIZE_BYTES))
            // creds_size
            result.write(intBytes(entriesBytes.size))
            // icon_size_array_size
            result.write(intBytes(iconSizeList.size))
            // icon offsets
            iconSizeList.forEach { result.write(intBytes(it)) }
            result.write(entriesBytes)
            icons.writeTo(result)
            return result.toByteArray()
        }

        private fun intBytes(num: Int): ByteArray =
            ByteBuffer.allocate(Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(num).array()
    }
}
