/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.testing.rules

import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 21)
class FakeCameraTestRuleTest(
    @CameraSelector.LensFacing private val lensFacing: Int,
) {
    private val fakeCameraRule = FakeCameraTestRule(ApplicationProvider.getApplicationContext())

    @Test
    fun isCameraProviderInitialized_providerIsNotAvailable_whenTestRuleNotApplied() {
        assertThat(fakeCameraRule.isCameraProviderInitialized()).isFalse()
    }

    @Test
    fun isCameraProviderInitialized_providerAvailable_whenTestRuleApplied() {
        fakeCameraRule.applyRule {
            assertThat(fakeCameraRule.isCameraProviderInitialized()).isTrue()
        }
    }

    @Test
    fun bindUseCases_useCasesBoundToCameraProvider() {
        fakeCameraRule.applyRule {
            val fakeUseCase1 = FakeUseCase()
            val fakeUseCase2 = FakeUseCase()

            fakeCameraRule.bindUseCases(lensFacing, listOf(fakeUseCase1))
            fakeCameraRule.bindUseCases(lensFacing, listOf(fakeUseCase2))

            assertThat(fakeCameraRule.cameraProvider.isBound(fakeUseCase1)).isTrue()
            assertThat(fakeCameraRule.cameraProvider.isBound(fakeUseCase2)).isTrue()
        }
    }

    @Test
    fun unbindUseCases_correctUseCaseIsUnboundFromProvider_whenOneOfMultipleUseCasesRemoved() {
        fakeCameraRule.applyRule {
            val fakeUseCase1 = FakeUseCase()
            val fakeUseCase2 = FakeUseCase()

            fakeCameraRule.bindUseCases(lensFacing, listOf(fakeUseCase1))
            fakeCameraRule.bindUseCases(lensFacing, listOf(fakeUseCase2))
            fakeCameraRule.unbindUseCases(listOf(fakeUseCase1))

            assertThat(fakeCameraRule.cameraProvider.isBound(fakeUseCase1)).isFalse()
            assertThat(fakeCameraRule.cameraProvider.isBound(fakeUseCase2)).isTrue()
        }
    }

    @Test
    fun useCaseIsBoundToCameraProvider_whenUseCaseUnboundAndRebound() {
        fakeCameraRule.applyRule {
            val fakeUseCase = FakeUseCase()

            fakeCameraRule.bindUseCases(lensFacing, listOf(fakeUseCase))
            fakeCameraRule.unbindUseCases(listOf(fakeUseCase))
            fakeCameraRule.bindUseCases(lensFacing, listOf(fakeUseCase))

            assertThat(fakeCameraRule.cameraProvider.isBound(fakeUseCase)).isTrue()
        }
    }

    @Test
    fun getFakeCamera_backCameraAlwaysAvailable() {
        assertThat(fakeCameraRule.getFakeCamera(LENS_FACING_BACK)).isNotNull()
    }

    @Test
    fun getFakeCamera_frontCameraAlwaysAvailable() {
        assertThat(fakeCameraRule.getFakeCamera(LENS_FACING_FRONT)).isNotNull()
    }

    private fun FakeCameraTestRule.applyRule(testBody: () -> Unit) {
        apply(
                object : Statement() {
                    override fun evaluate() {
                        testBody()
                    }
                },
                null
            )
            .evaluate()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "LensFacing = {0}")
        fun data() =
            listOf(
                arrayOf(LENS_FACING_BACK),
                arrayOf(LENS_FACING_FRONT),
            )
    }
}
