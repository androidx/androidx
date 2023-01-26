/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.streamsharing

import android.os.Build
import androidx.camera.core.Preview
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.internal.TargetConfig.OPTION_TARGET_CLASS
import androidx.camera.core.internal.TargetConfig.OPTION_TARGET_NAME
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeUseCase
import androidx.camera.testing.fakes.FakeUseCaseConfigFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [StreamSharing].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class StreamSharingTest {

    private val parentCamera = FakeCamera()
    private val preview = Preview.Builder().build()
    private val video = FakeUseCase()
    private val useCaseConfigFactory = FakeUseCaseConfigFactory()
    private lateinit var streamSharing: StreamSharing

    @Before
    fun setUp() {
        streamSharing = StreamSharing(parentCamera, setOf(preview, video), useCaseConfigFactory)
    }

    @Test
    fun getDefaultConfig_usesVideoCaptureType() {
        val config = streamSharing.getDefaultConfig(true, useCaseConfigFactory)!!

        assertThat(useCaseConfigFactory.lastRequestedCaptureType)
            .isEqualTo(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE)
        assertThat(
            config.retrieveOption(
                OPTION_TARGET_CLASS,
                null
            )
        ).isEqualTo(StreamSharing::class.java)
        assertThat(
            config.retrieveOption(
                OPTION_TARGET_NAME,
                null
            )
        ).startsWith("androidx.camera.core.streamsharing.StreamSharing-")
    }
}