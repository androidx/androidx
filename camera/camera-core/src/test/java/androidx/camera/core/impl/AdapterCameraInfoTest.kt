/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ColorSpace
import androidx.camera.core.DynamicRange
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraConfig
import androidx.camera.testing.impl.fakes.FakeCameraExtensionCapabilities
import androidx.camera.testing.impl.fakes.FakeSessionProcessor
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 24)
class AdapterCameraInfoTest {

    @Test
    @Config(minSdk = 31)
    fun getSupportedExtensions_delegatesToCameraInfo() {
        val extensions = setOf(1, 2, 3)
        val cameraInfo = FakeCameraInfoInternal().apply { setSupportedExtensions(extensions) }
        val adapterCameraInfo = AdapterCameraInfo(cameraInfo, FakeCameraConfig())

        assertThat(adapterCameraInfo.supportedExtensions).isEqualTo(extensions)
    }

    @Test
    @Config(minSdk = 31)
    fun isExtensionSupported_delegatesToCameraInfo() {
        val extensionMode = 1
        val vendorMode = 100
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                setSupportedExtensions(setOf(extensionMode))
                setVendorDefinedModes(setOf(vendorMode))
            }
        val adapterCameraInfo = AdapterCameraInfo(cameraInfo, FakeCameraConfig())

        assertThat(adapterCameraInfo.isExtensionSupported(extensionMode)).isTrue()
        assertThat(adapterCameraInfo.isExtensionSupported(vendorMode)).isTrue()
        assertThat(adapterCameraInfo.supportedExtensions).containsExactly(extensionMode)
    }

    @Test
    @Config(minSdk = 31)
    fun getCameraExtensionCapabilities_delegatesToCameraInfo() {
        val extensionMode = 1
        val capabilities = FakeCameraExtensionCapabilities()
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                setCameraExtensionCapabilities(extensionMode, capabilities)
            }
        val adapterCameraInfo = AdapterCameraInfo(cameraInfo, FakeCameraConfig())

        assertThat(adapterCameraInfo.getCameraExtensionCapabilities(extensionMode))
            .isEqualTo(capabilities)
    }

    @Test
    fun whenPhysicalCameraIdIsPresent_delegatesPropertiesToPhysicalCamera() {
        val physicalCameraId = "2"
        val physicalZoomRatio = 0.57f
        val physicalCameraInfo =
            FakePhysicalCameraInfo(
                physicalCameraId,
                physicalCameraId = physicalCameraId,
                intrinsicZoom = physicalZoomRatio,
            )
        val logicalCameraInfo =
            FakePhysicalCameraInfo(
                "0",
                intrinsicZoom = 1.0f,
                physicalCameraInfos = setOf(physicalCameraInfo),
            )
        val adapterCameraInfo =
            AdapterCameraInfo(logicalCameraInfo, physicalCameraId, FakeCameraConfig())

        assertThat(adapterCameraInfo.intrinsicZoomRatio).isEqualTo(physicalZoomRatio)
        assertThat(adapterCameraInfo.cameraSelector.physicalCameraId).isEqualTo(physicalCameraId)
        assertThat(adapterCameraInfo.isLogicalMultiCameraSupported).isFalse()
        assertThat(adapterCameraInfo.physicalCameraInfos).isEmpty()
        assertThat(adapterCameraInfo.physicalCameraId).isEqualTo(physicalCameraId)
    }

    @Test
    fun unwrapAs_whenPhysicalCameraIdIsPresent_unwrapsFromPhysicalCamera() {
        val physicalCameraId = "2"
        val expectedObject = "PhysicalUnwrapped"
        val physicalCameraInfo =
            FakeUnsafeCameraInfoInternal(
                FakePhysicalCameraInfo(physicalCameraId, physicalCameraId = physicalCameraId),
                unwrappedObject = expectedObject,
            )
        val logicalCameraInfo =
            FakePhysicalCameraInfo("0", physicalCameraInfos = setOf(physicalCameraInfo))
        val adapterCameraInfo =
            AdapterCameraInfo(logicalCameraInfo, physicalCameraId, FakeCameraConfig())

        assertThat(adapterCameraInfo.unwrapAs(String::class.java)).isEqualTo(expectedObject)
    }

    @Test
    fun unwrapAs_whenPhysicalCameraReturnsNull_doesNotFallBackToLogicalCamera() {
        val physicalCameraId = "2"
        val physicalCameraInfo =
            FakePhysicalCameraInfo(physicalCameraId, physicalCameraId = physicalCameraId)
        val expectedLogicalObject = "LogicalUnwrapped"
        val fakeCameraInfo =
            FakePhysicalCameraInfo("0", physicalCameraInfos = setOf(physicalCameraInfo))
        val logicalCameraInfo =
            FakeUnsafeCameraInfoInternal(fakeCameraInfo, unwrappedObject = expectedLogicalObject)
        val adapterCameraInfo =
            AdapterCameraInfo(logicalCameraInfo, physicalCameraId, FakeCameraConfig())

        assertThat(adapterCameraInfo.unwrapAs(String::class.java)).isNull()
    }

    @Test
    fun unwrapAs_whenPhysicalCameraIdIsNull_unwrapsFromLogicalCamera() {
        val expectedLogicalObject = "LogicalUnwrapped"
        val fakeCameraInfo =
            FakeCameraInfoInternal("0", 0, androidx.camera.core.CameraSelector.LENS_FACING_BACK)
        val logicalCameraInfo =
            FakeUnsafeCameraInfoInternal(fakeCameraInfo, unwrappedObject = expectedLogicalObject)
        val adapterCameraInfo = AdapterCameraInfo(logicalCameraInfo, null, FakeCameraConfig())

        assertThat(adapterCameraInfo.unwrapAs(String::class.java)).isEqualTo(expectedLogicalObject)
    }

    @Test
    fun whenPhysicalCameraIdIsNull_delegatesPropertiesToLogicalCamera() {
        val logicalZoomRatio = 1.0f
        val logicalCameraInfo =
            FakeCameraInfoInternal("0", 0, androidx.camera.core.CameraSelector.LENS_FACING_BACK)
                .apply { setIntrinsicZoomRatio(logicalZoomRatio) }
        val adapterCameraInfo = AdapterCameraInfo(logicalCameraInfo, null, FakeCameraConfig())

        assertThat(adapterCameraInfo.physicalCameraId).isNull()
        assertThat(adapterCameraInfo.intrinsicZoomRatio).isEqualTo(logicalZoomRatio)
        assertThat(adapterCameraInfo.isLogicalMultiCameraSupported).isFalse()
    }

    @Test
    fun getCameraIdentifier_withPhysicalCameraId_returnsIdentifierWithPhysicalCameraId() {
        val physicalCameraId = "2"
        val physicalCameraInfo =
            FakePhysicalCameraInfo(physicalCameraId, physicalCameraId = physicalCameraId)
        val fakeCameraInfo =
            FakePhysicalCameraInfo("0", physicalCameraInfos = setOf(physicalCameraInfo))
        val compatId = Identifier.create("test_compat_id")
        val config = FakeCameraConfig(compatibilityId = compatId)
        val adapterCameraInfo = AdapterCameraInfo(fakeCameraInfo, physicalCameraId, config)

        val identifier = adapterCameraInfo.cameraIdentifier
        assertThat(identifier.cameraIds)
            .containsExactly(
                androidx.camera.core.CameraIdentifier.CompositeCameraId("0", physicalCameraId)
            )
        assertThat(identifier.compatibilityId).isNull()
        assertThat(identifier.internalId).isEqualTo("0")
    }

    @Test
    fun getCameraIdentifier_withoutPhysicalCameraId_returnsSuperCameraIdentifier() {
        val fakeCameraInfo =
            FakeCameraInfoInternal("0", 0, androidx.camera.core.CameraSelector.LENS_FACING_BACK)
        val compatId = Identifier.create("test_compat_id")
        val config = FakeCameraConfig(compatibilityId = compatId)
        val adapterCameraInfo = AdapterCameraInfo(fakeCameraInfo, config)

        val identifier = adapterCameraInfo.cameraIdentifier
        assertThat(identifier.cameraIds)
            .containsExactly(androidx.camera.core.CameraIdentifier.CompositeCameraId("0"))
        assertThat(identifier.compatibilityId).isNull()
        assertThat(identifier.internalId).isEqualTo("0")
    }

    @Test
    fun constructor_withInvalidPhysicalCameraId_throwsIllegalArgumentException() {
        val fakeCameraInfo =
            FakeCameraInfoInternal("0", 0, androidx.camera.core.CameraSelector.LENS_FACING_BACK)
        // No physical cameras set on fakeCameraInfo

        assertThrows(IllegalArgumentException::class.java) {
            AdapterCameraInfo(fakeCameraInfo, "invalid_physical_id", FakeCameraConfig())
        }
    }

    @Test
    fun getSupportedColorSpaces_withoutSessionProcessor_delegatesToCameraInfo() {
        val fakeCameraInfo =
            FakeCameraInfoInternal().apply {
                supportedDynamicRanges = setOf(DynamicRange.SDR, DynamicRange.HLG_10_BIT)
                setSupportedColorSpaces(
                    DynamicRange.SDR,
                    setOf(ColorSpace.SRGB, ColorSpace.DISPLAY_P3),
                )
                setSupportedColorSpaces(DynamicRange.HLG_10_BIT, setOf(ColorSpace.BT2020_HLG))
            }
        val adapterCameraInfo = AdapterCameraInfo(fakeCameraInfo, FakeCameraConfig())

        assertThat(adapterCameraInfo.getSupportedColorSpaces(DynamicRange.SDR))
            .containsExactly(ColorSpace.SRGB, ColorSpace.DISPLAY_P3)
        assertThat(adapterCameraInfo.getSupportedColorSpaces(DynamicRange.HLG_10_BIT))
            .containsExactly(ColorSpace.BT2020_HLG)
        assertThat(adapterCameraInfo.getSupportedColorSpaces(DynamicRange.UNSPECIFIED))
            .containsExactly(ColorSpace.SRGB, ColorSpace.DISPLAY_P3, ColorSpace.BT2020_HLG)
        assertThat(adapterCameraInfo.getSupportedColorSpaces(DynamicRange.HDR_UNSPECIFIED_10_BIT))
            .containsExactly(ColorSpace.BT2020_HLG)
    }

    @Test
    fun getSupportedColorSpaces_withSessionProcessor_restrictsToSrgbOnSdrOnly() {
        val fakeCameraInfo =
            FakeCameraInfoInternal().apply {
                supportedDynamicRanges = setOf(DynamicRange.SDR, DynamicRange.HLG_10_BIT)
                setSupportedColorSpaces(
                    DynamicRange.SDR,
                    setOf(ColorSpace.SRGB, ColorSpace.DISPLAY_P3),
                )
                setSupportedColorSpaces(DynamicRange.HLG_10_BIT, setOf(ColorSpace.BT2020_HLG))
            }
        val extensionConfig = FakeCameraConfig(sessionProcessor = FakeSessionProcessor())
        val adapterCameraInfo = AdapterCameraInfo(fakeCameraInfo, extensionConfig)

        assertThat(adapterCameraInfo.getSupportedColorSpaces(DynamicRange.SDR))
            .containsExactly(ColorSpace.SRGB)
        assertThat(adapterCameraInfo.getSupportedColorSpaces(DynamicRange.HLG_10_BIT)).isEmpty()
        assertThat(adapterCameraInfo.getSupportedColorSpaces(DynamicRange.UNSPECIFIED))
            .containsExactly(ColorSpace.SRGB)
        assertThat(adapterCameraInfo.getSupportedColorSpaces(DynamicRange.HDR_UNSPECIFIED_10_BIT))
            .isEmpty()
    }

    @Test
    fun cameraInfoDefaultGetSupportedColorSpaces_returnsSrgbForSdrResolvableOnly() {
        val defaultCameraInfo =
            object : CameraInfo by FakeCameraInfoInternal() {
                override fun getSupportedColorSpaces(dynamicRange: DynamicRange): Set<ColorSpace> =
                    super<CameraInfo>.getSupportedColorSpaces(dynamicRange)
            }

        assertThat(defaultCameraInfo.getSupportedColorSpaces(DynamicRange.SDR))
            .containsExactly(ColorSpace.SRGB)
        assertThat(defaultCameraInfo.getSupportedColorSpaces(DynamicRange.UNSPECIFIED))
            .containsExactly(ColorSpace.SRGB)
        assertThat(defaultCameraInfo.getSupportedColorSpaces(DynamicRange.HLG_10_BIT)).isEmpty()
        assertThat(defaultCameraInfo.getSupportedColorSpaces(DynamicRange.HDR_UNSPECIFIED_10_BIT))
            .isEmpty()
    }

    private class FakePhysicalCameraInfo(
        cameraId: String,
        sensorRotationDegrees: Int = 0,
        lensFacing: Int = androidx.camera.core.CameraSelector.LENS_FACING_BACK,
        private val physicalCameraId: String? = null,
        private val physicalCameraInfos: Set<CameraInfo> = emptySet(),
        private val intrinsicZoom: Float = CameraInfo.INTRINSIC_ZOOM_RATIO_UNKNOWN,
    ) : ForwardingCameraInfo(FakeCameraInfoInternal(cameraId, sensorRotationDegrees, lensFacing)) {

        override fun getCameraSelector(): CameraSelector {
            val base = super.getCameraSelector()
            return if (physicalCameraId != null) {
                CameraSelector.Builder.fromSelector(base)
                    .setPhysicalCameraId(physicalCameraId)
                    .build()
            } else {
                base
            }
        }

        override fun getPhysicalCameraInfos(): Set<CameraInfo> = physicalCameraInfos

        override fun getIntrinsicZoomRatio(): Float = intrinsicZoom
    }

    private class FakeUnsafeCameraInfoInternal(
        cameraInfoInternal: CameraInfoInternal,
        private val unwrappedObject: Any? = null,
    ) : ForwardingCameraInfo(cameraInfoInternal), androidx.camera.common.UnsafeWrapper {
        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> unwrapAs(type: Class<T>): T? {
            if (type.isInstance(unwrappedObject)) {
                return unwrappedObject as T
            }
            return null
        }
    }
}
