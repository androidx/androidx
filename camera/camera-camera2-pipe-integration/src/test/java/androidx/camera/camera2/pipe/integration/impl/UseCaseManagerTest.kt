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

package androidx.camera.camera2.pipe.integration.impl

import android.os.Build
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCameraComponentBuilder
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.HashSet

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class UseCaseManagerTest {

    @Test
    fun enabledUseCasesEmpty_whenUseCaseAttachedOnly() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val useCase = Preview.Builder().build()

        // Act
        useCaseManager.attach(listOf(useCase))

        // Assert
        val enabledUseCases = useCaseManager.camera?.activeUseCases
        assertThat(enabledUseCases).isEmpty()
    }

    @Test
    fun enabledUseCasesNotEmpty_whenUseCaseEnabled() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val useCase = Preview.Builder().build()
        useCaseManager.attach(listOf(useCase))

        // Act
        useCaseManager.enable(useCase)

        // Assert
        val enabledUseCases = useCaseManager.camera?.activeUseCases
        assertThat(enabledUseCases).containsExactly(useCase)
    }

    @Test
    fun meteringRepeatingNotEnabled_whenPreviewEnabled() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        useCaseManager.attach(listOf(preview, imageCapture))

        // Act
        useCaseManager.enable(preview)
        useCaseManager.enable(imageCapture)

        // Assert
        val enabledUseCases = useCaseManager.camera?.activeUseCases
        assertThat(enabledUseCases).containsExactly(preview, imageCapture)
    }

    @Test
    fun meteringRepeatingEnabled_whenOnlyImageCaptureEnabled() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val imageCapture = ImageCapture.Builder().build()
        useCaseManager.attach(listOf(imageCapture))

        // Act
        useCaseManager.enable(imageCapture)

        // Assert
        val enabledUseCaseClasses = useCaseManager.camera?.activeUseCases?.map { it::class.java }
        assertThat(enabledUseCaseClasses).containsExactly(
            ImageCapture::class.java,
            MeteringRepeating::class.java
        )
    }

    @Test
    fun meteringRepeatingDisabled_whenPreviewBecomesEnabled() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val imageCapture = ImageCapture.Builder().build()
        useCaseManager.attach(listOf(imageCapture))
        useCaseManager.enable(imageCapture)

        // Act
        val preview = Preview.Builder().build()
        useCaseManager.attach(listOf(preview))
        useCaseManager.enable(preview)

        // Assert
        val activeUseCases = useCaseManager.camera?.activeUseCases
        assertThat(activeUseCases).containsExactly(preview, imageCapture)
    }

    @Test
    fun meteringRepeatingEnabled_afterAllUseCasesButImageCaptureDisabled() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        useCaseManager.attach(listOf(preview, imageCapture))
        useCaseManager.enable(preview)
        useCaseManager.enable(imageCapture)

        // Act
        useCaseManager.disable(preview)

        // Assert
        val enabledUseCaseClasses = useCaseManager.camera?.activeUseCases?.map { it::class.java }
        assertThat(enabledUseCaseClasses).containsExactly(
            ImageCapture::class.java,
            MeteringRepeating::class.java
        )
    }

    @Test
    fun meteringRepeatingDisabled_whenAllUseCasesDisabled() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val imageCapture = ImageCapture.Builder().build()
        useCaseManager.attach(listOf(imageCapture))
        useCaseManager.enable(imageCapture)

        // Act
        useCaseManager.disable(imageCapture)

        // Assert
        val enabledUseCases = useCaseManager.camera?.activeUseCases
        assertThat(enabledUseCases).isEmpty()
    }

    @Suppress("UNCHECKED_CAST", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun createUseCaseManager() = UseCaseManager(
        cameraConfig = CameraConfig(CameraId("0")),
        builder = FakeUseCaseCameraComponentBuilder(),
        controls = HashSet<UseCaseCameraControl>() as java.util.Set<UseCaseCameraControl>,
        cameraProperties = FakeCameraProperties()
    )
}
