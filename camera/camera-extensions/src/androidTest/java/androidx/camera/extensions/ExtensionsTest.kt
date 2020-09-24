/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.camera.extensions

import android.content.Context
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LensFacing
import androidx.camera.core.CameraX
import androidx.camera.extensions.Extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager.EffectMode
import androidx.camera.extensions.util.ExtensionsTestUtil
import androidx.camera.testing.CameraUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.jvm.Throws

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
class ExtensionsTest(
    @field:ExtensionMode @param:ExtensionMode private val mExtensionMode: Int,
    @field:LensFacing @param:LensFacing private val mLensFacing: Int
) {
    private val mContext =
        ApplicationProvider.getApplicationContext<Context>()

    private val mEffectMode: EffectMode =
        ExtensionsTestUtil.extensionModeToEffectMode(mExtensionMode)

    private lateinit var mExtensions: Extensions

    @Before
    @Throws(Exception::class)
    fun setUp() {
        assumeTrue(CameraUtil.deviceHasCamera())
        val cameraXConfig = Camera2Config.defaultConfig()
        CameraX.initialize(mContext, cameraXConfig).get()
        assumeTrue(
            CameraUtil.hasCameraWithLensFacing(
                mLensFacing
            )
        )
        assumeTrue(ExtensionsTestUtil.initExtensions(mContext))
        mExtensions = ExtensionsManager.getExtensions(mContext)
    }

    @After
    @Throws(
        InterruptedException::class,
        ExecutionException::class,
        TimeoutException::class
    )
    fun cleanUp() {
        CameraX.shutdown()[10000, TimeUnit.MILLISECONDS]
        ExtensionsManager.deinit().get()
    }

    companion object {
        @JvmStatic
        @get:Parameterized.Parameters(name = "extension = {0}, facing = {1}")
        val parameters: Collection<Array<Any>>
            get() = ExtensionsTestUtil.getAllExtensionsLensFacingCombinations()
    }

    // TODO: Can be removed after the Extensions class is fully implemented.
    @Test
    fun isExtensionAvailable() {
        val cameraSelector = CameraSelector.Builder().requireLensFacing(mLensFacing).build()
        val camera = CameraUtil.createCameraUseCaseAdapter(mContext, cameraSelector)

        assertThat(
            ExtensionsManager.isExtensionAvailable(mEffectMode, mLensFacing) ==
                mExtensions.isExtensionAvailable(camera, mExtensionMode)
        ).isTrue()
    }

    @Test
    fun setExtensionSucceedsIfAvailable() {
        val cameraSelector = CameraSelector.Builder().requireLensFacing(mLensFacing).build()
        val camera = CameraUtil.createCameraUseCaseAdapter(mContext, cameraSelector)

        assumeTrue(mExtensions.isExtensionAvailable(camera, mExtensionMode))

        mExtensions.setExtension(camera, mExtensionMode)
        assertThat(mExtensions.getExtension(camera)).isEqualTo(mExtensionMode)
    }

    @Test
    fun setExtensionFailsIfNotAvailable() {
        val cameraSelector = CameraSelector.Builder().requireLensFacing(mLensFacing).build()
        val camera = CameraUtil.createCameraUseCaseAdapter(mContext, cameraSelector)

        assumeFalse(mExtensions.isExtensionAvailable(camera, mExtensionMode))

        assertThrows<IllegalArgumentException> {
            mExtensions.setExtension(camera, mExtensionMode)
        }

        assertThat(mExtensions.getExtension(camera)).isEqualTo(Extensions.EXTENSION_MODE_NONE)
    }
}