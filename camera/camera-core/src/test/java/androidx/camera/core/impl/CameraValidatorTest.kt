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

package androidx.camera.core.impl

import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.CameraIdentifier
import androidx.camera.core.CameraSelector
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
@DoNotInstrument
class CameraValidatorTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var fakeCameraFactory: FakeCameraFactory
    private lateinit var cameraRepository: CameraRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        packageManager = context.packageManager
        fakeCameraFactory = FakeCameraFactory()
        cameraRepository = CameraRepository()
    }

    @Test
    fun validateOnFirstInit_succeeds_whenAllRequiredCamerasExist() {
        // Arrange
        setSystemFeatures(hasBackCamera = true, hasFrontCamera = true)
        setupCameras(hasBack = true, hasFront = true)
        val validator = CameraValidator.create(context, null)

        // Act & Assert
        // Should not throw any exception
        validator.validateOnFirstInit(cameraRepository)
    }

    @Test
    fun validateOnFirstInit_throwsException_whenBackCameraIsMissing() {
        // Arrange
        setSystemFeatures(hasBackCamera = true, hasFrontCamera = false)
        setupCameras(hasBack = false, hasFront = true) // Missing required back camera
        val validator = CameraValidator.create(context, null)

        // Act & Assert
        assertThrows(CameraValidator.CameraIdListIncorrectException::class.java) {
            validator.validateOnFirstInit(cameraRepository)
        }
    }

    @Test
    fun validateOnFirstInit_throwsException_whenFrontCameraIsMissing() {
        // Arrange
        setSystemFeatures(hasBackCamera = true, hasFrontCamera = true)
        setupCameras(hasBack = true, hasFront = false) // Missing required front camera
        val validator = CameraValidator.create(context, null)

        // Act & Assert
        assertThrows(CameraValidator.CameraIdListIncorrectException::class.java) {
            validator.validateOnFirstInit(cameraRepository)
        }
    }

    @Test
    fun validateOnFirstInit_succeeds_whenMissingCameraIsNotInSelectorScope() {
        // Arrange: System requires both, but repository only has a BACK camera.
        setSystemFeatures(hasBackCamera = true, hasFrontCamera = true)
        setupCameras(hasBack = true, hasFront = false)
        // Act: Create a validator that ONLY cares about the back camera.
        val validator = CameraValidator.create(context, CameraSelector.DEFAULT_BACK_CAMERA)

        // Assert: No exception, because the missing FRONT camera is out of scope.
        validator.validateOnFirstInit(cameraRepository)
    }

    @Test
    fun validateOnFirstInit_reportsCorrectCameraCount_onFailure() {
        // Arrange: System requires a back camera, but it's missing. One front camera exists.
        setSystemFeatures(hasBackCamera = true, hasFrontCamera = false)
        setupCameras(hasBack = false, hasFront = true)
        val validator = CameraValidator.create(context, null)

        // Act: Catch the exception
        val exception =
            assertThrows(CameraValidator.CameraIdListIncorrectException::class.java) {
                validator.validateOnFirstInit(cameraRepository)
            }

        // Assert: The exception correctly reports that 1 camera was still available,
        // which allows recovery logic to proceed with a degraded state.
        assertThat(exception.availableCameraCount).isEqualTo(1)
    }

    @Test
    fun isChangeInvalid_returnsFalse_forRemovingNonRequiredCamera() {
        // Arrange
        setSystemFeatures(hasBackCamera = true, hasFrontCamera = true)
        val cameras = setupCameras(hasBack = true, hasFront = true, hasExternal = true)
        val validator = CameraValidator.create(context, null)

        // Act: Remove the external camera
        val isInvalid = validator.isChangeInvalid(cameras, setOf(IDENTIFIER_EXTERNAL))

        // Assert
        assertThat(isInvalid).isFalse()
    }

    @Test
    fun isChangeInvalid_returnsTrue_forRemovingRequiredBackCamera() {
        // Arrange
        setSystemFeatures(hasBackCamera = true, hasFrontCamera = true)
        val cameras = setupCameras(hasBack = true, hasFront = true)
        val validator = CameraValidator.create(context, null)

        // Act: Remove the BACK camera
        val isInvalid = validator.isChangeInvalid(cameras, setOf(IDENTIFIER_BACK))

        // Assert
        assertThat(isInvalid).isTrue()
    }

    @Test
    fun isChangeInvalid_returnsFalse_whenMissingCameraIsNotInSelectorScope() {
        // Arrange
        setSystemFeatures(hasBackCamera = true, hasFrontCamera = true)
        val cameras = setupCameras(hasBack = true, hasFront = true)
        // This validator only cares about the BACK camera.
        val validator = CameraValidator.create(context, CameraSelector.DEFAULT_BACK_CAMERA)

        // Act: Remove the FRONT camera.
        val isInvalid = validator.isChangeInvalid(cameras, setOf(IDENTIFIER_FRONT))

        // Assert: Change is valid because the removed camera was out of scope.
        assertThat(isInvalid).isFalse()
    }

    @Test
    fun isChangeInvalid_allowsNonRequiredRemoval_fromDegradedState() {
        // Arrange: System requires BACK and FRONT, but we start in a degraded state
        // with only BACK and an EXTERNAL camera (e.g. after a partial init).
        setSystemFeatures(hasBackCamera = true, hasFrontCamera = true)
        val cameras = setupCameras(hasBack = true, hasFront = false, hasExternal = true)
        val validator = CameraValidator.create(context, null)

        // Act: Remove the EXTERNAL camera.
        val isInvalid = validator.isChangeInvalid(cameras, setOf(IDENTIFIER_EXTERNAL))

        // Assert: This change is NOT invalid because we didn't lose a *required* camera
        // that we actually had. The state did not become worse.
        assertThat(isInvalid).isFalse()
    }

    private fun setSystemFeatures(hasBackCamera: Boolean, hasFrontCamera: Boolean) {
        val shadowPackageManager = Shadows.shadowOf(packageManager)
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA, hasBackCamera)
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA_FRONT, hasFrontCamera)
    }

    private fun setupCameras(
        hasBack: Boolean = false,
        hasFront: Boolean = false,
        hasExternal: Boolean = false,
    ): Set<CameraInternal> {
        if (hasBack) {
            fakeCameraFactory.insertCamera(CameraSelector.LENS_FACING_BACK, CAMERA_ID_BACK) {
                FakeCamera(
                    CAMERA_ID_BACK,
                    null,
                    FakeCameraInfoInternal(CAMERA_ID_BACK, 0, CameraSelector.LENS_FACING_BACK),
                )
            }
        }
        if (hasFront) {
            fakeCameraFactory.insertCamera(CameraSelector.LENS_FACING_FRONT, CAMERA_ID_FRONT) {
                FakeCamera(
                    CAMERA_ID_FRONT,
                    null,
                    FakeCameraInfoInternal(CAMERA_ID_FRONT, 0, CameraSelector.LENS_FACING_FRONT),
                )
            }
        }
        if (hasExternal) {
            fakeCameraFactory.insertCamera(
                CameraSelector.LENS_FACING_EXTERNAL,
                CAMERA_ID_EXTERNAL,
            ) {
                FakeCamera(
                    CAMERA_ID_EXTERNAL,
                    null,
                    FakeCameraInfoInternal(
                        CAMERA_ID_EXTERNAL,
                        0,
                        CameraSelector.LENS_FACING_EXTERNAL,
                    ),
                )
            }
        }
        cameraRepository.init(fakeCameraFactory)
        return cameraRepository.cameras
    }

    private companion object {
        private const val CAMERA_ID_BACK = "0"
        private const val CAMERA_ID_FRONT = "1"
        private const val CAMERA_ID_EXTERNAL = "2"

        private val IDENTIFIER_BACK = CameraIdentifier.Factory.create(CAMERA_ID_BACK)
        private val IDENTIFIER_FRONT = CameraIdentifier.Factory.create(CAMERA_ID_FRONT)
        private val IDENTIFIER_EXTERNAL = CameraIdentifier.Factory.create(CAMERA_ID_EXTERNAL)
    }
}
