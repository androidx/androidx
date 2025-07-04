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
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListWatchFacesTest {
    private var context: Context = ApplicationProvider.getApplicationContext()
    private var wfp = WatchFacePushManagerFactory.createWatchFacePushManager(context)

    @Before
    fun setup() {
        setup(context, SAMPLE_WATCHFACE)
    }

    @Test
    @RequiresWatch
    fun listWatchFaces_beforeAddingAnything() {
        val response = runBlocking { wfp.listWatchFaces() }
        assertThat(response).isNotNull()
        assertThat(response.installedWatchFaceDetails).isEmpty()
        assertThat(response.remainingSlotCount).isEqualTo(1)
        assertThat(response.installedWatchFaceDetails).isInstanceOf(List::class.java)
    }

    @Test
    @RequiresWatch
    fun listWatchFaces_afterAddingWatchFace() {
        runBlocking {
            readWatchFace(context, VALID_APK).use { pipe ->
                wfp.addWatchFace(pipe.readFd, VALID_TOKEN)
            }
            val response = wfp.listWatchFaces()
            assertThat(response.installedWatchFaceDetails).isNotEmpty()
            assertThat(response.installedWatchFaceDetails.size).isEqualTo(1)
            assertThat(response.remainingSlotCount).isEqualTo(0)
        }
    }
}
