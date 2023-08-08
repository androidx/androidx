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
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.impl.fakes.FakeSurfaceEffect
import androidx.camera.testing.impl.fakes.FakeSurfaceProcessorInternal
import androidx.camera.testing.impl.fakes.FakeUseCase
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
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

    lateinit var processor: FakeSurfaceProcessorInternal

    @Before
    fun setUp() {
        processor = FakeSurfaceProcessorInternal(CameraXExecutors.mainThreadExecutor())
    }

    @After
    fun tearDown() {
        processor.cleanUp()
    }

    @Test
    fun setMutuallyExclusiveEffectsTargets_effectsSet() {
        // Arrange.
        val previewEffect = FakeSurfaceEffect(
            PREVIEW,
            processor
        )
        val videoEffect = FakeSurfaceEffect(
            VIDEO_CAPTURE,
            processor
        )

        // Act.
        val useCaseGroup = UseCaseGroup.Builder().addUseCase(FakeUseCase())
            .addEffect(previewEffect)
            .addEffect(videoEffect)
            .build()

        // Assert.
        assertThat(useCaseGroup.effects).containsExactly(previewEffect, videoEffect)
    }

    @Test
    fun setConflictingEffectTargets_throwsException() {
        // Arrange.
        val previewEffect = FakeSurfaceEffect(
            PREVIEW,
            processor
        )
        val previewVideoEffect = FakeSurfaceEffect(
            PREVIEW or VIDEO_CAPTURE,
            processor
        )
        // Act.
        val errorMessage = buildAndGetErrorMessage(
            UseCaseGroup.Builder().addUseCase(FakeUseCase())
                .addEffect(previewEffect)
                .addEffect(previewVideoEffect)
        )

        // Assert.
        assertThat(errorMessage).isEqualTo("More than one effects has targets PREVIEW.")
    }

    private fun buildAndGetErrorMessage(builder: UseCaseGroup.Builder): String? {
        var message: String? = null
        try {
            builder.build()
        } catch (e: IllegalArgumentException) {
            message = e.message
        }
        return message
    }
}
