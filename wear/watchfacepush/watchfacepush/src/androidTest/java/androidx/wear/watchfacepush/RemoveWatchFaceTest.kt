/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.watchfacepush.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.watchfacepush.*
import androidx.wear.watchfacepush.WatchFacePushManager.RemoveWatchFaceException
import androidx.wear.watchfacepush.WatchFacePushManager.WatchFaceDetails
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoveWatchFaceTest {
    private var context: Context = ApplicationProvider.getApplicationContext()
    private var wfp = WatchFacePushManagerFactory.createWatchFacePushManager(context)

    @Before
    fun setup() {
        setup(context, SAMPLE_WATCHFACE)
    }

    @Test
    @RequiresWatch
    fun removeWatchFace() {
        runBlocking {
            val details: WatchFaceDetails =
                readWatchFace(context, VALID_APK).use { pipe ->
                    wfp.addWatchFace(pipe.readFd, VALID_TOKEN)
                }
            assertThat(wfp.listWatchFaces().installedWatchFaceDetails).isNotEmpty()
            wfp.removeWatchFace(details.slotId)
            assertThat(wfp.listWatchFaces().installedWatchFaceDetails).isEmpty()
        }
    }

    @Test
    @RequiresWatch
    fun removeWatchFace_invalidSlot() {
        val exception =
            Assert.assertThrows(RemoveWatchFaceException::class.java) {
                runBlocking { wfp.removeWatchFace("an invalid slot") }
            }

        assertThat(exception.errorCode)
            .isEqualTo(RemoveWatchFaceException.Companion.ERROR_INVALID_SLOT_ID)
        assertThat(exception).hasMessageThat().contains("slot ID")
    }
}
