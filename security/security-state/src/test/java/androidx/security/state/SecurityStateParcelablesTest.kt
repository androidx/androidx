/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.security.state

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityStateParcelablesTest {

    @Test
    fun testUpdateInfo_parcelRoundTrip() {
        // GIVEN a fully populated UpdateInfo object
        val original =
            UpdateInfo(
                component = "SYSTEM",
                securityPatchLevel = "2026-01-01",
                publishedDateMillis = 123456789L,
                lastCheckTimeMillis = 987654321L,
            )

        // WHEN we write it to a Parcel and read it back
        val restored = parcelRoundTrip(original, UpdateInfo.CREATOR)

        // THEN the restored object is identical to the original
        assertEquals(original, restored)
    }

    @Test
    fun testUpdateInfo_ignoresUnknownKeys() {
        // GIVEN a Bundle populated with standard keys AND an unknown "future" key
        val futureBundle =
            Bundle().apply {
                putString("component", "SYSTEM")
                putString("securityPatchLevel", "2026-01-01")
                putLong("publishedDateMillis", 100L)
                putLong("lastCheckTimeMillis", 200L)
                // Unknown key
                putString("new_metadata_field", "future_value")
            }

        // WHEN we manually parcel this bundle and read it back using our Creator
        val parcel = Parcel.obtain()
        futureBundle.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        // (Simulate the creator reading the bundle)
        val restored = UpdateInfo.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        // THEN the object is created successfully, ignoring the unknown key
        assertEquals("SYSTEM", restored.component)
        assertEquals("2026-01-01", restored.securityPatchLevel)
        assertEquals(100L, restored.publishedDateMillis)
        assertEquals(200L, restored.lastCheckTimeMillis)
    }

    @Test
    fun testUpdateInfo_usesDefaultsForMissingKeys() {
        // GIVEN an empty Bundle (simulating missing data)
        val emptyBundle = Bundle()

        // WHEN we read it back
        val parcel = Parcel.obtain()
        emptyBundle.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = UpdateInfo.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        // THEN reasonable defaults are used instead of crashing or nulls
        assertEquals("", restored.component)
        assertEquals("", restored.securityPatchLevel)
        assertEquals(0L, restored.publishedDateMillis)
        assertEquals(0L, restored.lastCheckTimeMillis)
    }

    @Test
    fun testUpdateInfo_handlesWrongTypesGracefully() {
        // GIVEN a Bundle where 'securityPatchLevel' is an Int instead of a String
        val badTypeBundle =
            Bundle().apply {
                putString("component", "SYSTEM")
                putInt("securityPatchLevel", 12345) // Wrong type!
            }

        // WHEN we read it back
        val parcel = Parcel.obtain()
        badTypeBundle.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = UpdateInfo.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        // THEN it falls back to the default string instead of crashing
        // (Bundle.getString() returns null for type mismatch, and we use ?: "")
        assertEquals("SYSTEM", restored.component)
        assertEquals("", restored.securityPatchLevel)
    }

    @Test
    fun testUpdateCheckResult_parcelRoundTrip() {
        // GIVEN an UpdateCheckResult containing a list of updates
        val updates =
            listOf(
                UpdateInfo("SYSTEM", "2026-02-01", 100L, 200L),
                UpdateInfo("SYSTEM_MODULES", "2026-01-01", 300L, 400L),
            )
        val original =
            UpdateCheckResult(
                providerPackageName = "com.example.provider",
                updates = updates,
                lastCheckTimeMillis = 500L,
            )

        // WHEN we write it to a Parcel and read it back
        val restored = parcelRoundTrip(original, UpdateCheckResult.CREATOR)

        // THEN the restored object is identical
        assertEquals(original, restored)
        assertEquals(2, restored.updates.size)
        assertEquals("SYSTEM", restored.updates[0].component)
        assertEquals("SYSTEM_MODULES", restored.updates[1].component)
    }

    @Test
    fun testUpdateCheckResult_ignoresUnknownKeys() {
        // GIVEN a Bundle with standard keys AND an unknown future key
        val futureBundle =
            Bundle().apply {
                putString("providerPackageName", "com.example.provider")
                putLong("lastCheckTimeMillis", 100L)
                // Valid empty list for updates
                putParcelableArrayList("updates", ArrayList<UpdateInfo>())
                // Unknown key
                putString("new_metadata_field", "future_value")
            }

        // WHEN we read it back
        val parcel = Parcel.obtain()
        futureBundle.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = UpdateCheckResult.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        // THEN it succeeds and ignores the extra key
        assertEquals("com.example.provider", restored.providerPackageName)
        assertTrue(restored.updates.isEmpty())
    }

    @Test
    fun testUpdateCheckResult_usesDefaultsForMissingKeys() {
        // GIVEN an empty Bundle (simulating missing data from an older service)
        val emptyBundle = Bundle()

        // WHEN we read it back as an UpdateCheckResult
        val parcel = Parcel.obtain()
        emptyBundle.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = UpdateCheckResult.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        // THEN reasonable defaults are used
        assertEquals("", restored.providerPackageName)
        assertEquals(0L, restored.lastCheckTimeMillis)
        assertTrue("Updates list should default to empty", restored.updates.isEmpty())
    }

    @Test
    fun testUpdateCheckResult_handlesWrongTypesGracefully() {
        // GIVEN a Bundle where 'updates' is a String instead of an ArrayList
        // (Simulating a provider bug or data corruption)
        val badTypeBundle =
            Bundle().apply {
                putString("providerPackageName", "com.example.provider")
                putString("updates", "I am not a list") // Wrong type!
            }

        // WHEN we read it back
        val parcel = Parcel.obtain()
        badTypeBundle.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = UpdateCheckResult.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        // THEN it falls back to an empty list instead of crashing
        // (Bundle.getParcelableArrayList returns null on type mismatch, we handle it)
        assertTrue("Should return empty list on type mismatch", restored.updates.isEmpty())
        assertEquals("com.example.provider", restored.providerPackageName)
    }

    /** Helper function to simulate IPC transfer */
    private fun <T> parcelRoundTrip(input: T, creator: Parcelable.Creator<T>): T {
        val parcel = Parcel.obtain()
        try {
            // Write
            (input as Parcelable).writeToParcel(parcel, 0)

            // Reset for reading
            parcel.setDataPosition(0)

            // Read
            return creator.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
    }
}
