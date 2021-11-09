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

package androidx.camera.video

import androidx.camera.testing.asFlow
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class VideoOutputTest {

    @Test
    fun getStreamState_defaultsToActive(): Unit = runBlocking {
        // Create an anonymous subclass of VideoOutput. Don't override
        // VideoOutput#getStreamState() so the default implementation is used.
        val videoOutput = VideoOutput { request -> request.willNotProvideSurface() }

        val streamState = videoOutput.streamState.asFlow().first()
        assertThat(streamState).isEqualTo(VideoOutput.StreamState.ACTIVE)
    }

    @Test
    fun getMediaSpec_defaultsToNull(): Unit = runBlocking {
        // Create an anonymous subclass of VideoOutput. Don't override
        // VideoOutput#getMediaSpec() so the default implementation is used.
        val videoOutput = VideoOutput { request -> request.willNotProvideSurface() }

        val mediaSpec = videoOutput.mediaSpec.asFlow().first()
        assertThat(mediaSpec).isNull()
    }
}