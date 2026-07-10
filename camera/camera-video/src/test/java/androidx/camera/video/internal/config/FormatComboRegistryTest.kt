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

package androidx.camera.video.internal.config

import android.media.MediaFormat.MIMETYPE_AUDIO_AAC
import android.media.MediaFormat.MIMETYPE_AUDIO_OPUS
import android.media.MediaFormat.MIMETYPE_VIDEO_AVC
import android.media.MediaFormat.MIMETYPE_VIDEO_VP9
import androidx.camera.video.MediaConstants.MIME_TYPE_UNSPECIFIED
import androidx.camera.video.MediaSpec.Companion.OUTPUT_FORMAT_MPEG_4
import androidx.camera.video.MediaSpec.Companion.OUTPUT_FORMAT_UNSPECIFIED
import androidx.camera.video.MediaSpec.Companion.OUTPUT_FORMAT_WEBM
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class FormatComboRegistryTest {

    companion object {
        private const val VIDEO_AVC = MIMETYPE_VIDEO_AVC
        private const val VIDEO_VP9 = MIMETYPE_VIDEO_VP9
        private const val AUDIO_AAC = MIMETYPE_AUDIO_AAC
        private const val AUDIO_OPUS = MIMETYPE_AUDIO_OPUS
    }

    private val sampleRegistry =
        FormatComboRegistry.Builder()
            .apply {
                container(OUTPUT_FORMAT_MPEG_4) { support(listOf(VIDEO_AVC), listOf(AUDIO_AAC)) }
                container(OUTPUT_FORMAT_WEBM) { support(listOf(VIDEO_VP9), listOf(AUDIO_OPUS)) }
            }
            .build()

    @Test
    fun getCombos_withSpecificParams_returnsExactMatch() {
        val results = sampleRegistry.getCombos(OUTPUT_FORMAT_MPEG_4, VIDEO_AVC, AUDIO_AAC)

        assertThat(results).hasSize(1)
        assertThat(results[0].container).isEqualTo(OUTPUT_FORMAT_MPEG_4)
        assertThat(results[0].videoMime).isEqualTo(VIDEO_AVC)
        assertThat(results[0].audioMime).isEqualTo(AUDIO_AAC)
    }

    @Test
    fun getCombos_withOutputFormatUnspecified_searchesAcrossAllContainers() {
        val results =
            sampleRegistry.getCombos(
                OUTPUT_FORMAT_UNSPECIFIED,
                MIME_TYPE_UNSPECIFIED,
                MIME_TYPE_UNSPECIFIED,
            )

        // MPEG_4: (AVC+AAC), (AVC+null), (null+AAC) -> 3
        // WEBM: (VP9+OPUS), (VP9+null), (null+OPUS) -> 3
        assertThat(results).hasSize(6)
    }

    @Test
    fun getCombos_withInvalidCombination_returnsEmpty() {
        // AVC is in MPEG_4, but OPUS is only in WEBM. This should yield no matches.
        val results = sampleRegistry.getCombos(OUTPUT_FORMAT_MPEG_4, VIDEO_AVC, AUDIO_OPUS)
        assertThat(results).isEmpty()
    }

    @Test
    fun getCombos_supportsMimeUnspecified() {
        val results =
            sampleRegistry.getCombos(OUTPUT_FORMAT_MPEG_4, VIDEO_AVC, MIME_TYPE_UNSPECIFIED)

        // Should find (AVC+AAC) and (AVC+null)
        assertThat(results).hasSize(2)
        assertThat(results.all { it.videoMime == VIDEO_AVC }).isTrue()
        assertThat(results.any { it.audioMime == AUDIO_AAC }).isTrue()
        assertThat(results.any { it.audioMime == null }).isTrue()
    }

    @Test
    fun getCombosForVideo_returnsAllPairingsForThatCodec() {
        val results = sampleRegistry.getCombosForVideo(VIDEO_AVC)

        // Should find (AVC+AAC) and (AVC+null)
        assertThat(results).hasSize(2)
        assertThat(results.all { it.videoMime == VIDEO_AVC }).isTrue()
        assertThat(results.any { it.audioMime == AUDIO_AAC }).isTrue()
        assertThat(results.any { it.audioMime == null }).isTrue()
    }

    @Test
    fun getCombosForAudio_returnsAllPairingsForThatCodec() {
        val results = sampleRegistry.getCombosForAudio(AUDIO_OPUS)

        // Should find (VP9+OPUS) and (null+OPUS)
        assertThat(results).hasSize(2)
        assertThat(results.all { it.audioMime == AUDIO_OPUS }).isTrue()
        assertThat(results.any { it.videoMime == VIDEO_VP9 }).isTrue()
        assertThat(results.any { it.videoMime == null }).isTrue()
    }

    @Test
    fun builder_handlesMultipleSupportBlocks_inSameContainer() {
        val multiRegistry =
            FormatComboRegistry.Builder()
                .apply {
                    container(OUTPUT_FORMAT_MPEG_4) {
                        support(listOf(VIDEO_AVC), listOf(AUDIO_AAC))
                        support(listOf(VIDEO_VP9), listOf(AUDIO_OPUS))
                    }
                }
                .build()

        assertThat(multiRegistry.getCombosForVideo(VIDEO_AVC)).isNotEmpty()
        assertThat(multiRegistry.getCombosForVideo(VIDEO_VP9)).isNotEmpty()
    }
}
