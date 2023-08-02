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
import androidx.camera.core.SurfaceEffect.PREVIEW
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.testing.fakes.FakeSurfaceEffect
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [EffectBundle].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class EffectBundleTest {

    @Test(expected = IllegalArgumentException::class)
    fun noEffect_throwsException() {
        EffectBundle.Builder(mainThreadExecutor()).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun addMoreThanOnePreviewEffect_throwsException() {
        val surfaceEffect = FakeSurfaceEffect(mainThreadExecutor())
        EffectBundle.Builder(mainThreadExecutor())
            .addEffect(PREVIEW, surfaceEffect)
            .addEffect(PREVIEW, surfaceEffect)
    }

    @Test
    fun addPreviewEffect_hasPreviewEffect() {
        // Arrange.
        val surfaceEffect = FakeSurfaceEffect(mainThreadExecutor())
        // Act.
        val effectBundle = EffectBundle.Builder(mainThreadExecutor())
            .addEffect(PREVIEW, surfaceEffect)
            .build()
        // Assert.
        assertThat(effectBundle.effects.values.first() as SurfaceEffect).isEqualTo(surfaceEffect)
    }
}