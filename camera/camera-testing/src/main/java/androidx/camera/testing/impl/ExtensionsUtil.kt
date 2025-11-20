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

package androidx.camera.testing.impl

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Pair
import android.util.Size
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.CameraConfig.REQUIRED_RULE_COEXISTING_PREVIEW_AND_IMAGE_CAPTURE
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.ExtendedCameraConfigProviderStore
import androidx.camera.core.impl.Identifier
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.UseCaseConfigFactory
import org.junit.Assume.assumeTrue

/** Utilities for Extensions related tests. */
public object ExtensionsUtil {
    private fun getOutputSizes(
        cameraProvider: CameraProvider,
        cameraSelector: CameraSelector,
        format: Int,
    ): Array<Size> {
        val cameraCharacteristics =
            (cameraProvider.getCameraInfo(cameraSelector) as CameraInfoInternal)
                .cameraCharacteristics as CameraCharacteristics
        return cameraCharacteristics
            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            .getOutputSizes(format)
    }

    /** Returns a {@link CameraSelector} with the given {@link SessionProcessor}. */
    @JvmStatic
    public fun getCameraSelectorWithSessionProcessor(
        cameraProvider: CameraProvider,
        cameraSelector: CameraSelector,
        sessionProcessor: SessionProcessor,
        outputYuvformatInCapture: Boolean = false,
    ): CameraSelector {
        val identifier = Identifier.create("idStr")
        ExtendedCameraConfigProviderStore.addConfig(identifier) { _, _ ->
            object : CameraConfig {
                override fun getConfig(): Config {
                    return MutableOptionsBundle.create()
                }

                override fun getCompatibilityId(): Identifier {
                    return Identifier.create(0)
                }

                override fun getSessionProcessor(
                    valueIfMissing: SessionProcessor?
                ): SessionProcessor {
                    return sessionProcessor
                }

                override fun getSessionProcessor(): SessionProcessor {
                    return sessionProcessor
                }

                override fun getUseCaseCombinationRequiredRule(): Int {
                    return REQUIRED_RULE_COEXISTING_PREVIEW_AND_IMAGE_CAPTURE
                }

                override fun getUseCaseConfigFactory(): UseCaseConfigFactory =
                    object : UseCaseConfigFactory {
                        override fun getConfig(
                            captureType: UseCaseConfigFactory.CaptureType,
                            captureMode: Int,
                        ): Config? {
                            if (captureType == UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) {
                                val builder = ImageCapture.Builder()
                                builder.setHighResolutionDisabled(true)
                                val supportedResolutions =
                                    mutableListOf<android.util.Pair<Int, Array<Size>>>()
                                if (outputYuvformatInCapture) {
                                    supportedResolutions.add(
                                        Pair(
                                            ImageFormat.YUV_420_888,
                                            getOutputSizes(
                                                cameraProvider,
                                                cameraSelector,
                                                ImageFormat.YUV_420_888,
                                            ),
                                        )
                                    )
                                } else {
                                    supportedResolutions.add(
                                        Pair(
                                            ImageFormat.JPEG,
                                            getOutputSizes(
                                                cameraProvider,
                                                cameraSelector,
                                                ImageFormat.JPEG,
                                            ),
                                        )
                                    )
                                }
                                builder.setSupportedResolutions(supportedResolutions)
                                return builder.useCaseConfig
                            }
                            return null
                        }
                    }
            }
        }

        val builder = CameraSelector.Builder.fromSelector(cameraSelector)
        builder.addCameraFilter(
            object : CameraFilter {
                override fun filter(cameraInfos: MutableList<CameraInfo>): MutableList<CameraInfo> {
                    val newCameraInfos = mutableListOf<CameraInfo>()
                    newCameraInfos.addAll(cameraInfos)
                    return newCameraInfos
                }

                override fun getIdentifier(): Identifier {
                    return identifier
                }
            }
        )

        return builder.build()
    }

    /**
     * Checks whether the device is Pixel 7, 8, 9 series and the installed PCS is suitable for test
     * or not.
     */
    @JvmStatic
    public fun assumePcsSupportedForImageCapture(appContext: Context) {
        // Only checks Pixel 7, 8, 9 series devices.
        // For the older devices, there is no ImageCapture problem on PCS.
        // For the newer devices, it is expected that the built-in PCS will work normally.
        if (!"google".equals(Build.BRAND, ignoreCase = true)) {
            return
        }

        if (
            !Build.MODEL.contains("pixel 7", ignoreCase = true) &&
                !Build.MODEL.contains("pixel 8", ignoreCase = true) &&
                !Build.MODEL.contains("pixel 9", ignoreCase = true)
        ) {
            return
        }

        // Checks whether the PCS version is v2.3 or above
        try {
            val pcsPackageName = "com.google.android.apps.camera.services"
            val packageInfo: PackageInfo =
                appContext.packageManager.getPackageInfo(pcsPackageName, 0)
            packageInfo.versionName?.let {
                assumeTrue("PCS_VERSION_UNSUITABLE_FOR_TEST, current version is $it", it >= "2.3")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Do nothing if the package can't be found.
        }
    }
}
