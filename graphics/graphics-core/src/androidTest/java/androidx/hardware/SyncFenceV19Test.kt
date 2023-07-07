/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.hardware

import android.os.Build
import androidx.graphics.surface.JniBindings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
@SmallTest
class SyncFenceV19Test {

    @Test
    fun testDupSyncFenceFd() {
        val fileDescriptor = 7
        val syncFence = SyncFenceV19(7)
        // If the file descriptor is valid dup'ing it should return a different fd
        Assert.assertNotEquals(fileDescriptor, JniBindings.nDupFenceFd(syncFence))
    }

    @Test
    fun testWaitMethodLink() {
        try {
            SyncFenceV19(8).await(1000)
        } catch (linkError: UnsatisfiedLinkError) {
            fail("Unable to resolve wait method")
        } catch (exception: Exception) {
            // Ignore other exceptions
        }
    }

    @Test
    fun testDupSyncFenceFdWhenInvalid() {
        // If the fence is invalid there should be no attempt to dup the fd it and -1
        // should be returned
        Assert.assertEquals(-1, JniBindings.nDupFenceFd(SyncFenceV19(-1)))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testSignalTimeInvalid() {
        // Something other than -1 even though this is not technically a valid file descriptor
        // the internal APIs should not crash and instead return SIGNAL_TIME_INVALID
        // Because not all devices support the ability to create a native file descriptor from
        // an EGLSync, create a validity check to ensure we can get more presubmit test coverage
        Assert.assertEquals(
            SyncFenceCompat.SIGNAL_TIME_INVALID,
            SyncFenceV19(7).getSignalTimeNanos()
        )
        Assert.assertEquals(
            SyncFenceCompat.SIGNAL_TIME_INVALID,
            SyncFenceV19(-1).getSignalTimeNanos()
        )
    }

    @Test
    fun testIsValid() {
        assertFalse(SyncFenceV19(-1).isValid())
        assertTrue(SyncFenceV19(42).isValid())
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testResolveSyncFileInfo() {
        assertTrue(SyncFenceBindings.nResolveSyncFileInfo())
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testResolveSyncFileInfoFree() {
        assertTrue(SyncFenceBindings.nResolveSyncFileInfoFree())
    }
}
