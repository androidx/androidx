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

package androidx.camera.core

import android.os.Build
import androidx.camera.core.CameraEffect.IMAGE_CAPTURE
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.UseCaseGroup.Builder.getHumanReadableTargets
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.fakes.FakeSurfaceEffect
import androidx.camera.testing.fakes.FakeSurfaceProcessor
import androidx.camera.testing.fakes.FakeUseCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [UseCaseGroup].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class UseCaseGroupTest {

    @Test
    fun duplicateTargets_throwsException() {
        // Arrange.
        val previewEffect = FakeSurfaceEffect(
            CameraXExecutors.mainThreadExecutor(),
            FakeSurfaceProcessor(CameraXExecutors.mainThreadExecutor())
        )
        val builder = UseCaseGroup.Builder().addUseCase(FakeUseCase())
            .addEffect(previewEffect)
            .addEffect(previewEffect)

        // Act.
        var message: String? = null
        try {
            builder.build()
        } catch (e: IllegalArgumentException) {
            message = e.message
        }

        // Assert.
        assertThat(message).isEqualTo(
            "Effects androidx.camera.testing.fakes.FakeSurfaceEffect " +
                "and androidx.camera.testing.fakes.FakeSurfaceEffect " +
                "contain duplicate targets PREVIEW."
        )
    }

    @Test
    fun verifyHumanReadableTargetsNames() {
        assertThat(getHumanReadableTargets(PREVIEW)).isEqualTo("PREVIEW")
        assertThat(getHumanReadableTargets(PREVIEW or VIDEO_CAPTURE))
            .isEqualTo("PREVIEW|VIDEO_CAPTURE")
        assertThat(getHumanReadableTargets(PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE))
            .isEqualTo("IMAGE_CAPTURE|PREVIEW|VIDEO_CAPTURE")
    }
}