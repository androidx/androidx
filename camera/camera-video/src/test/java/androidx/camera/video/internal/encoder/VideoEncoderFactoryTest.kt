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

package androidx.camera.video.internal.encoder

import androidx.camera.testing.impl.fakes.FakeVideoEncoderInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class VideoEncoderFactoryTest {

    @Test
    fun finderFrom_findsEncoderInfoOfFactory() {
        val encoderInfo = FakeVideoEncoderInfo()
        val factory = createFactory { mimeType -> encoderInfo.takeIf { mimeType == MIME_TYPE } }

        val finder = VideoEncoderInfo.Finder.from(factory)

        assertThat(finder.find(MIME_TYPE)).isSameInstanceAs(encoderInfo)
        assertThat(finder.find(OTHER_MIME_TYPE)).isNull()
    }

    @Test
    fun finderFrom_sameFactory_finderIsEqual() {
        val factory = createFactory()

        val finder1 = VideoEncoderInfo.Finder.from(factory)
        val finder2 = VideoEncoderInfo.Finder.from(factory)

        assertThat(finder1).isEqualTo(finder2)
        assertThat(finder1.hashCode()).isEqualTo(finder2.hashCode())
    }

    @Test
    fun finderFrom_differentFactories_findersAreNotEqual() {
        assertThat(VideoEncoderInfo.Finder.from(createFactory()))
            .isNotEqualTo(VideoEncoderInfo.Finder.from(createFactory()))
    }

    @Test
    fun defaultFinder_isFinderOfDefaultFactory() {
        assertThat(DefaultVideoEncoderFactory.DEFAULT_FINDER)
            .isEqualTo(VideoEncoderInfo.Finder.from(DefaultVideoEncoderFactory.DEFAULT))
    }

    private fun createFactory(
        encoderInfoProvider: (String) -> VideoEncoderInfo? = { FakeVideoEncoderInfo() }
    ) =
        FakeVideoEncoderFactory(encoderInfoProvider) { _, _ ->
            throw UnsupportedOperationException()
        }

    private companion object {
        private const val MIME_TYPE = "video/avc"
        private const val OTHER_MIME_TYPE = "video/hevc"
    }
}
