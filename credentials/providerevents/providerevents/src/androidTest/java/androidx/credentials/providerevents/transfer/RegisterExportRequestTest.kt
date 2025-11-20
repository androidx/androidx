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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class RegisterExportRequestTest {

    @Test
    fun credentialBytes_serialization_isCorrect() {
        // 1. Setup
        // Create mock Bitmaps for the icons. In a real Android test, you might load
        // a test image, but for a unit test, a simple dummy bitmap is sufficient.
        val testBitmap1 = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        val testBitmap2 = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)

        // Create a list of ExportEntry objects with known test data.
        val entries =
            listOf(
                ExportEntry(
                    id = "id_1",
                    accountDisplayName = "Test Account",
                    userDisplayName = "test.user@example.com",
                    icon = testBitmap1,
                    supportedCredentialTypes = setOf("typeA", "typeB"),
                ),
                ExportEntry(
                    id = "id_2",
                    accountDisplayName = "Another Account",
                    userDisplayName = "another.user@example.com",
                    icon = testBitmap2,
                    supportedCredentialTypes = setOf("typeC"),
                ),
            )

        // 2. Execution
        // Instantiate RegisterExportRequest to trigger the credentialBytes serialization.
        val request = RegisterExportRequest(entries)
        val credentialBytes = request.credentialBytes

        // 3. Deserialization and Verification
        // Use a ByteBuffer to parse the resulting byte array.
        // The byte order must be LITTLE_ENDIAN as specified in the source code.
        val buffer = ByteBuffer.wrap(credentialBytes).order(ByteOrder.LITTLE_ENDIAN)

        // Read the sizes from the header.
        val headerSize = buffer.int
        val credsJsonSize = buffer.int
        val iconSizeArraySize = buffer.int

        // Verify the number of icons matches the entry list size.
        assertEquals(
            "Icon size array should match number of entries",
            entries.size,
            iconSizeArraySize,
        )

        // Skip the icon size array for now as we are focused on the JSON.
        // The size of this array is iconSizeArraySize * Int.SIZE_BYTES.
        buffer.position(buffer.position() + iconSizeArraySize * Int.SIZE_BYTES)

        // Read the JSON data based on its size.
        val jsonDataBytes = ByteArray(credsJsonSize)
        buffer.get(jsonDataBytes)

        // Deserialize the byte array into a JSONObject for inspection.
        val resultJson = JSONObject(String(jsonDataBytes, Charsets.UTF_8))

        // 4. Assertions
        // Check that the top-level "entries" key exists and is a JSONArray.
        assertTrue("JSON should contain an 'entries' key", resultJson.has("entries"))
        val entriesJsonArray = resultJson.getJSONArray("entries")
        assertEquals(
            "JSON entry array size should match input",
            entries.size,
            entriesJsonArray.length(),
        )

        // --- Verify the first entry ---
        val entry1Json = entriesJsonArray.getJSONObject(0)
        val displayInfo1Json = entry1Json.getJSONObject("display_info")
        val credTypes1Json = entry1Json.getJSONArray("supported_credential_types")

        assertEquals("ID for entry 1 is incorrect", "id_1", entry1Json.getString("id"))
        assertEquals(
            "Account name for entry 1 is incorrect",
            "Test Account",
            displayInfo1Json.getString("account_name"),
        )
        assertEquals(
            "User name for entry 1 is incorrect",
            "test.user@example.com",
            displayInfo1Json.getString("user_name"),
        )
        assertEquals(
            "Icon ID for entry 1 should be the index",
            0,
            displayInfo1Json.getInt("icon_id"),
        )
        assertEquals("Credential types count for entry 1 is wrong", 2, credTypes1Json.length())
        assertEquals(
            "Credential type 'typeA' missing for entry 1",
            "typeA",
            credTypes1Json.getString(0),
        )
        assertEquals(
            "Credential type 'typeB' missing for entry 1",
            "typeB",
            credTypes1Json.getString(1),
        )

        // --- Verify the second entry ---
        val entry2Json = entriesJsonArray.getJSONObject(1)
        val displayInfo2Json = entry2Json.getJSONObject("display_info")
        val credTypes2Json = entry2Json.getJSONArray("supported_credential_types")

        assertEquals("ID for entry 2 is incorrect", "id_2", entry2Json.getString("id"))
        assertEquals(
            "Account name for entry 2 is incorrect",
            "Another Account",
            displayInfo2Json.getString("account_name"),
        )
        assertEquals(
            "User name for entry 2 is incorrect",
            "another.user@example.com",
            displayInfo2Json.getString("user_name"),
        )
        assertEquals(
            "Icon ID for entry 2 should be the index",
            1,
            displayInfo2Json.getInt("icon_id"),
        )
        assertEquals("Credential types count for entry 2 is wrong", 1, credTypes2Json.length())
        assertEquals(
            "Credential type 'typeC' missing for entry 2",
            "typeC",
            credTypes2Json.getString(0),
        )
    }
}
