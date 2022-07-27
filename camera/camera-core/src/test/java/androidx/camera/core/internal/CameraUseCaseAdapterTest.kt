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

package androidx.camera.core.internal

import android.os.Build
import androidx.camera.core.EffectBundle
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceEffect.PREVIEW
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.processing.SurfaceEffectWithExecutor
import androidx.camera.testing.fakes.FakeSurfaceEffect
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [CameraUseCaseAdapter].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraUseCaseAdapterTest {

    private lateinit var surfaceEffect: FakeSurfaceEffect
    private lateinit var mEffectBundle: EffectBundle
    private lateinit var executor: ExecutorService

    @Before
    fun setUp() {
        surfaceEffect = FakeSurfaceEffect(mainThreadExecutor())
        executor = Executors.newSingleThreadExecutor()
        mEffectBundle = EffectBundle.Builder(executor).addEffect(PREVIEW, surfaceEffect).build()
    }

    @After
    fun tearDown() {
        surfaceEffect.cleanUp()
        executor.shutdown()
    }

    @Test
    fun updateEffects_effectsAddedAndRemoved() {
        // Arrange.
        val preview = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
        // Act: update use cases with effects bundle
        CameraUseCaseAdapter.updateEffects(mEffectBundle, listOf(preview))
        // Assert: preview has effect wrapped with the right executor.
        val previewEffect = preview.effect as SurfaceEffectWithExecutor
        assertThat(previewEffect.surfaceEffect).isEqualTo(surfaceEffect)
        assertThat(previewEffect.executor).isEqualTo(executor)
        // Act: update again with null effects bundle
        CameraUseCaseAdapter.updateEffects(null, listOf(preview))
        // Assert: preview no longer has effects.
        assertThat(preview.effect).isNull()
    }
}