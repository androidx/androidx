/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class ProfileInstallBroadcastTest {
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun installProfile() {
        assertNull(ProfileInstallBroadcast.installProfile(Packages.TARGET))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun installProfile_missing() {
        val errorString = ProfileInstallBroadcast.installProfile(Packages.MISSING)
        assertNotNull(errorString)
        assertContains(errorString!!, "The baseline profile install broadcast was not received")
    }

    @Test
    fun skipFileOperation() {
        assertNull(ProfileInstallBroadcast.skipFileOperation(Packages.TARGET, "WRITE_SKIP_FILE"))
        assertNull(ProfileInstallBroadcast.skipFileOperation(Packages.TARGET, "DELETE_SKIP_FILE"))
    }

    @Test
    fun skipFileOperation_missing() {
        ProfileInstallBroadcast.skipFileOperation(Packages.MISSING, "WRITE_SKIP_FILE").apply {
            assertNotNull(this)
            assertContains(this!!, "The baseline profile skip file broadcast was not received")
        }
        ProfileInstallBroadcast.skipFileOperation(Packages.MISSING, "DELETE_SKIP_FILE").apply {
            assertNotNull(this)
            assertContains(this!!, "The baseline profile skip file broadcast was not received")
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun saveProfile() {
        assertNull(ProfileInstallBroadcast.saveProfile(Packages.TARGET))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun saveProfile_missing() {
        val errorString = ProfileInstallBroadcast.saveProfile(Packages.MISSING)
        assertNotNull(errorString)
        assertContains(errorString!!, "The save profile broadcast event was not received")
    }

    @Test
    fun dropShaderCache() {
        assertNull(ProfileInstallBroadcast.dropShaderCache(Packages.TARGET))
    }

    @Test
    fun dropShaderCache_missing() {
        val errorString = ProfileInstallBroadcast.dropShaderCache(Packages.MISSING)
        assertNotNull(errorString)
        assertContains(errorString!!, "The DROP_SHADER_CACHE broadcast was not received")

        // validate extra instructions
        assertContains(
            errorString,
            "verify: 1) androidx.profileinstaller.ProfileInstallReceiver appears unobfuscated"
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun saveProfilesForAllProcesses() {
        assertEquals(
            expected = ProfileInstallBroadcast.SaveProfileResult(1, null),
            actual = ProfileInstallBroadcast.saveProfilesForAllProcesses(Packages.TARGET)
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun saveProfilesForAllProcesses_missing() {
        val result = ProfileInstallBroadcast.saveProfilesForAllProcesses(Packages.MISSING)
        assertEquals(0, result.processCount)
        assertNull(result.error)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    fun saveProfilesForAllProcesses_this() {
        // we remove the receiver in the test to enable testing against a running app without it,
        // but this doesn't test the BENCHMARK_OPERATION code path since the test is the
        // main process, so uses the legacy broadcast for compatibility
        val result = ProfileInstallBroadcast.saveProfilesForAllProcesses(Packages.TEST)
        assertEquals(1, result.processCount)
        assertNotNull(result.error)
        assertContains(result.error!!, "The save profile broadcast event was not received.")
    }
}
