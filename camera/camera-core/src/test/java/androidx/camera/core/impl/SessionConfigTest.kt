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
package androidx.camera.core.impl

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.util.Range
import android.view.Surface
import androidx.camera.camera2.impl.Camera2ImplConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.Config.Option
import androidx.camera.core.impl.SessionConfig.ValidatingBuilder
import androidx.camera.testing.impl.DeferrableSurfacesUtil
import androidx.camera.testing.impl.fakes.FakeMultiValueSet
import com.google.common.collect.Lists
import com.google.common.truth.Truth.assertThat
import java.util.Arrays
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SessionConfigTest {
    private var mMockSurface0: DeferrableSurface? = null
    private var mMockSurface1: DeferrableSurface? = null

    @Before
    fun setup() {
        mMockSurface0 = ImmediateSurface(mock(Surface::class.java))
        mMockSurface1 = ImmediateSurface(mock(Surface::class.java))
    }

    @After
    fun tearDown() {
        mMockSurface0!!.close()
        mMockSurface1!!.close()
    }

    @Test
    fun builderSetTemplate() {
        val builder = SessionConfig.Builder()

        builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
        val sessionConfig = builder.build()

        assertThat(sessionConfig.templateType).isEqualTo(CameraDevice.TEMPLATE_PREVIEW)
    }

    @Test
    fun builderAddSurface() {
        val builder = SessionConfig.Builder()

        builder.addSurface(mMockSurface0!!)
        val sessionConfig = builder.build()

        val surfaces = sessionConfig.surfaces
        val outputConfigs = sessionConfig.outputConfigs

        assertThat(surfaces).hasSize(1)
        assertThat(surfaces).contains(mMockSurface0)
        assertThat(outputConfigs).hasSize(1)
        assertThat(outputConfigs[0]!!.surface).isEqualTo(mMockSurface0)
        assertThat(outputConfigs[0]!!.sharedSurfaces).isEmpty()
        assertThat(outputConfigs[0]!!.physicalCameraId).isNull()
        assertThat(outputConfigs[0]!!.surfaceGroupId)
            .isEqualTo(SessionConfig.OutputConfig.SURFACE_GROUP_ID_NONE)
    }

    @Test
    fun builderAddOutputConfig() {
        val builder = SessionConfig.Builder()
        val sharedSurface1: DeferrableSurface = ImmediateSurface(mock(Surface::class.java))
        val sharedSurface2: DeferrableSurface = ImmediateSurface(mock(Surface::class.java))
        val outputConfig =
            SessionConfig.OutputConfig.builder(mMockSurface0!!)
                .setSurfaceGroupId(1)
                .setSharedSurfaces(Arrays.asList(sharedSurface1, sharedSurface2))
                .setPhysicalCameraId("4")
                .build()

        builder.addOutputConfig(outputConfig)
        val sessionConfig = builder.build()

        val surfaces = sessionConfig.surfaces
        val outputConfigs = sessionConfig.outputConfigs

        assertThat(surfaces).containsExactly(mMockSurface0, sharedSurface1, sharedSurface2)
        assertThat(outputConfigs).hasSize(1)
        assertThat(outputConfigs[0]!!.surface).isEqualTo(mMockSurface0)
        assertThat(outputConfigs[0]!!.sharedSurfaces)
            .containsExactly(sharedSurface1, sharedSurface2)
        assertThat(outputConfigs[0]!!.surfaceGroupId).isEqualTo(1)
        assertThat(outputConfigs[0]!!.physicalCameraId).isEqualTo("4")
    }

    @Test
    fun builderAddNonRepeatingSurface() {
        val builder = SessionConfig.Builder()

        builder.addNonRepeatingSurface(mMockSurface0!!)
        val sessionConfig = builder.build()

        val surfaces = sessionConfig.surfaces
        val outputConfigs = sessionConfig.outputConfigs
        val repeatingSurfaces = sessionConfig.repeatingCaptureConfig.surfaces

        assertThat(surfaces).containsExactly(mMockSurface0)
        assertThat(outputConfigs).hasSize(1)
        assertThat(outputConfigs[0]!!.surface).isEqualTo(mMockSurface0)
        assertThat(repeatingSurfaces).isEmpty()
        assertThat(repeatingSurfaces).doesNotContain(mMockSurface0)
    }

    @Test
    fun builderAddSurfaceContainsRepeatingSurface() {
        val builder = SessionConfig.Builder()

        builder.addSurface(mMockSurface0!!)
        builder.addNonRepeatingSurface(mMockSurface1!!)
        val sessionConfig = builder.build()

        val surfaces = DeferrableSurfacesUtil.surfaceList(sessionConfig.surfaces)
        val repeatingSurfaces =
            DeferrableSurfacesUtil.surfaceList(sessionConfig.repeatingCaptureConfig.surfaces)

        assertThat(surfaces.size).isAtLeast(repeatingSurfaces.size)
        assertThat(surfaces).containsAtLeastElementsIn(repeatingSurfaces)
    }

    @Test
    fun builderRemoveSurface() {
        val builder = SessionConfig.Builder()

        builder.addSurface(mMockSurface0!!)
        builder.addSurface(mMockSurface1!!)
        builder.removeSurface(mMockSurface0!!)
        val sessionConfig = builder.build()

        assertThat(sessionConfig.surfaces).containsExactly(mMockSurface1)
        assertThat(sessionConfig.outputConfigs).hasSize(1)
        assertThat(sessionConfig.outputConfigs[0].surface).isEqualTo(mMockSurface1)
    }

    @Test
    fun builderClearSurface() {
        val builder = SessionConfig.Builder()

        builder.addSurface(mMockSurface0!!)
        builder.clearSurfaces()
        val sessionConfig = builder.build()

        assertThat(sessionConfig.surfaces).isEmpty()
        assertThat(sessionConfig.outputConfigs).isEmpty()
    }

    @Test
    fun builderAddOption() {
        val builder = SessionConfig.Builder()

        val options = MutableOptionsBundle.create()
        options.insertOption(OPTION, 1)
        builder.addImplementationOptions(options)
        val sessionConfig = builder.build()

        val config = sessionConfig.implementationOptions

        assertThat(config.containsOption(OPTION)).isTrue()
        assertThat(config.retrieveOption(OPTION)).isEqualTo(1)
    }

    @Test
    fun builderDefaultSessionTypeIsRegular() {
        val builder = SessionConfig.Builder()
        val sessionConfig = builder.build()
        assertThat(sessionConfig.sessionType == SessionConfiguration.SESSION_REGULAR)
    }

    @Test
    fun builderSetSessionType() {
        val builder = SessionConfig.Builder().setSessionType(2)
        val sessionConfig = builder.build()
        assertThat(sessionConfig.sessionType == 2)
    }

    @Test
    fun builderSetPostviewSurface() {
        val builder = SessionConfig.Builder().setPostviewSurface(mMockSurface0!!)
        val sessionConfig = builder.build()
        assertThat(sessionConfig.postviewOutputConfig!!.surface).isEqualTo(mMockSurface0)
    }

    @Test
    fun prioritizeTemplateType_previewHigherThanUnsupportedType() {
        val builderPreview = SessionConfig.Builder()
        builderPreview.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
        val sessionConfigPreview = builderPreview.build()
        val builderManual = SessionConfig.Builder()
        builderManual.setTemplateType(CameraDevice.TEMPLATE_MANUAL)
        val sessionConfigManual = builderManual.build()

        val validatingBuilder = ValidatingBuilder()

        validatingBuilder.add(sessionConfigPreview)
        validatingBuilder.add(sessionConfigManual)

        assertThat(validatingBuilder.isValid).isTrue()

        assertThat(validatingBuilder.build().templateType).isEqualTo(CameraDevice.TEMPLATE_PREVIEW)
    }

    @Test
    fun prioritizeTemplateType_recordHigherThanPreview() {
        val builderPreview = SessionConfig.Builder()
        builderPreview.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
        val sessionConfigPreview = builderPreview.build()
        val builderRecord = SessionConfig.Builder()
        builderRecord.setTemplateType(CameraDevice.TEMPLATE_RECORD)
        val sessionConfigRecord = builderRecord.build()

        val validatingBuilder = ValidatingBuilder()

        validatingBuilder.add(sessionConfigPreview)
        validatingBuilder.add(sessionConfigRecord)

        assertThat(validatingBuilder.isValid).isTrue()

        assertThat(validatingBuilder.build().templateType).isEqualTo(CameraDevice.TEMPLATE_RECORD)
    }

    @Test
    fun prioritizeTemplateType_addZslFirst_zslHigherThanPreview() {
        val builderZsl = SessionConfig.Builder()
        builderZsl.setTemplateType(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG)
        val sessionConfigZsl = builderZsl.build()

        val builderPreview = SessionConfig.Builder()
        builderPreview.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
        val sessionConfigPreview = builderPreview.build()

        val validatingBuilder = ValidatingBuilder()

        validatingBuilder.add(sessionConfigZsl)
        validatingBuilder.add(sessionConfigPreview)

        assertThat(validatingBuilder.isValid).isTrue()

        assertThat(validatingBuilder.build().templateType)
            .isEqualTo(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG)
    }

    @Test
    fun prioritizeTemplateType_addPreviewFirst_zslHigherThanPreview() {
        val builderZsl = SessionConfig.Builder()
        builderZsl.setTemplateType(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG)
        val sessionConfigZsl = builderZsl.build()

        val builderPreview = SessionConfig.Builder()
        builderPreview.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
        val sessionConfigPreview = builderPreview.build()

        val validatingBuilder = ValidatingBuilder()

        validatingBuilder.add(sessionConfigPreview)
        validatingBuilder.add(sessionConfigZsl)

        assertThat(validatingBuilder.isValid).isTrue()

        assertThat(validatingBuilder.build().templateType)
            .isEqualTo(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG)
    }

    @Test
    fun setAndVerifyExpectedFrameRateRange_nullValue() {
        val builderPreview = SessionConfig.Builder()
        builderPreview.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
        val sessionConfigPreview = builderPreview.build()

        val validatingBuilder = ValidatingBuilder()

        validatingBuilder.add(sessionConfigPreview)

        assertThat(validatingBuilder.isValid).isTrue()

        assertThat(validatingBuilder.build().expectedFrameRateRange)
            .isEqualTo(StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED)
    }

    @Test
    fun setAndVerifyExpectedFrameRateRange_initialValue() {
        val fpsRangeLow = Range(30, 45)
        val builderPreview = SessionConfig.Builder()
        builderPreview.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
        builderPreview.setExpectedFrameRateRange(fpsRangeLow)
        val sessionConfigPreview = builderPreview.build()

        val validatingBuilder = ValidatingBuilder()

        validatingBuilder.add(sessionConfigPreview)

        assertThat(validatingBuilder.isValid).isTrue()

        assertThat(validatingBuilder.build().expectedFrameRateRange).isEqualTo(fpsRangeLow)
    }

    @Test
    fun setAndVerifyExpectedFrameRateRange_sameValues() {
        val fpsRangeLow = Range(30, 45)
        val builderZsl = SessionConfig.Builder()
        builderZsl.setTemplateType(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG)
        builderZsl.setExpectedFrameRateRange(fpsRangeLow)
        val sessionConfigZsl = builderZsl.build()

        val builderPreview = SessionConfig.Builder()
        builderPreview.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
        builderPreview.setExpectedFrameRateRange(fpsRangeLow)
        val sessionConfigPreview = builderPreview.build()

        val validatingBuilder = ValidatingBuilder()

        validatingBuilder.add(sessionConfigPreview)
        validatingBuilder.add(sessionConfigZsl)

        assertThat(validatingBuilder.isValid).isTrue()

        assertThat(validatingBuilder.build().expectedFrameRateRange).isEqualTo(fpsRangeLow)
    }

    @Test
    fun setAndVerifyExpectedFrameRateRange_differentValues() {
        val fpsRangeLow = Range(30, 45)
        val fpsRangeHigh = Range(45, 60)
        val builderZsl = SessionConfig.Builder()
        builderZsl.setTemplateType(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG)
        builderZsl.setExpectedFrameRateRange(fpsRangeLow)
        val sessionConfigZsl = builderZsl.build()

        val builderPreview = SessionConfig.Builder()
        builderPreview.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
        builderPreview.setExpectedFrameRateRange(fpsRangeHigh)
        val sessionConfigPreview = builderPreview.build()

        val validatingBuilder = ValidatingBuilder()

        validatingBuilder.add(sessionConfigPreview)
        validatingBuilder.add(sessionConfigZsl)

        assertThat(validatingBuilder.isValid).isFalse()
    }

    @Test
    fun addImplementationOptionForStreamUseCase() {
        val validatingBuilder = ValidatingBuilder()
        assertThat(
            !validatingBuilder
                .build()
                .implementationOptions
                .containsOption(Camera2ImplConfig.STREAM_USE_CASE_OPTION)
        )
        validatingBuilder.addImplementationOption(Camera2ImplConfig.STREAM_USE_CASE_OPTION, 1L)
        assertThat(
            validatingBuilder
                .build()
                .implementationOptions
                .retrieveOption(Camera2ImplConfig.STREAM_USE_CASE_OPTION) == 1L
        )
    }

    @Test
    fun addDifferentNonDefaultSessionType() {
        // 1. Arrange.
        val validatingBuilder = ValidatingBuilder()
        val sessionConfig1 =
            SessionConfig.Builder()
                .setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                .setSessionType(1)
                .build()
        val sessionConfig2 =
            SessionConfig.Builder()
                .setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                .setSessionType(2)
                .build()

        // 2. Act.
        validatingBuilder.add(sessionConfig1)
        validatingBuilder.add(sessionConfig2)

        // 3. Assert.
        assertThat(validatingBuilder.isValid).isFalse()
    }

    @Test
    fun addDefaultAndThenNonDefaultSessionType() {
        // 1. Arrange.
        val sessionTypeToVerify = 2
        val validatingBuilder = ValidatingBuilder()
        val sessionConfig1 =
            SessionConfig.Builder().setTemplateType(CameraDevice.TEMPLATE_PREVIEW).build()
        val sessionConfig2 =
            SessionConfig.Builder()
                .setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                .setSessionType(sessionTypeToVerify)
                .build()

        // 2. Act.
        validatingBuilder.add(sessionConfig1)
        validatingBuilder.add(sessionConfig2)

        // 3. Assert.
        assertThat(validatingBuilder.build().sessionType).isEqualTo(sessionTypeToVerify)
        assertThat(validatingBuilder.isValid).isTrue()
    }

    @Test
    fun addNonDefaultAndThenDefaultSessionType() {
        // 1. Arrange.
        val sessionTypeToVerify = 2
        val validatingBuilder = ValidatingBuilder()
        val sessionConfig1 =
            SessionConfig.Builder()
                .setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                .setSessionType(sessionTypeToVerify)
                .build()
        val sessionConfig2 =
            SessionConfig.Builder().setTemplateType(CameraDevice.TEMPLATE_PREVIEW).build()

        // 2. Act.
        validatingBuilder.add(sessionConfig1)
        validatingBuilder.add(sessionConfig2)

        // 3. Assert.
        assertThat(validatingBuilder.build().sessionType).isEqualTo(sessionTypeToVerify)
        assertThat(validatingBuilder.isValid).isTrue()
    }

    @Test
    fun addPostviewSurfaceTo_validatingBuilder() {
        // 1. Arrange.
        val validatingBuilder = ValidatingBuilder()
        val sessionConfig1 =
            SessionConfig.Builder()
                .setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                .setPostviewSurface(mMockSurface0!!)
                .build()

        // 2. Act.
        validatingBuilder.add(sessionConfig1)

        // 3. Assert.
        assertThat(validatingBuilder.build().postviewOutputConfig!!.surface)
            .isEqualTo(mMockSurface0)
        assertThat(validatingBuilder.isValid).isTrue()
    }

    @Test
    fun addDifferentPostviewSurfacesTo_validatingBuilder() {
        // 1. Arrange.
        val validatingBuilder = ValidatingBuilder()
        val sessionConfig1 =
            SessionConfig.Builder()
                .setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                .setPostviewSurface(mMockSurface0!!)
                .build()
        val sessionConfig2 =
            SessionConfig.Builder()
                .setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                .setPostviewSurface(mMockSurface1!!)
                .build()

        // 2. Act.
        validatingBuilder.add(sessionConfig1)
        validatingBuilder.add(sessionConfig2)

        // 3. Assert.
        assertThat(validatingBuilder.isValid).isFalse()
    }

    @Test
    fun conflictingOptions() {
        val builder0 = SessionConfig.Builder()
        val options0 = MutableOptionsBundle.create()
        options0.insertOption(OPTION, 1)
        builder0.addImplementationOptions(options0)
        val config0 = builder0.build()

        val builder1 = SessionConfig.Builder()
        val options1 = MutableOptionsBundle.create()
        options1.insertOption(OPTION, 2)
        builder1.addImplementationOptions(options1)
        val config1 = builder1.build()

        val validatingBuilder = ValidatingBuilder()

        validatingBuilder.add(config0)
        validatingBuilder.add(config1)

        assertThat(validatingBuilder.isValid).isFalse()
    }

    @Test
    fun combineTwoSessionsValid() {
        val builder0 = SessionConfig.Builder()
        builder0.addSurface(mMockSurface0!!)
        builder0.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
        val options0 = MutableOptionsBundle.create()
        options0.insertOption(OPTION, 1)
        builder0.addImplementationOptions(options0)

        val builder1 = SessionConfig.Builder()
        builder1.addSurface(mMockSurface1!!)
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
        val options1 = MutableOptionsBundle.create()
        options1.insertOption(OPTION_1, "test")
        builder1.addImplementationOptions(options1)

        val validatingBuilder = ValidatingBuilder()
        validatingBuilder.add(builder0.build())
        validatingBuilder.add(builder1.build())

        assertThat(validatingBuilder.isValid).isTrue()
    }

    @Test
    fun combineTwoSessionsTemplate() {
        val builder0 = SessionConfig.Builder()
        builder0.addSurface(mMockSurface0!!)
        builder0.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
        val options0 = MutableOptionsBundle.create()
        options0.insertOption(OPTION, 1)
        builder0.addImplementationOptions(options0)

        val builder1 = SessionConfig.Builder()
        builder1.addSurface(mMockSurface1!!)
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
        val options1 = MutableOptionsBundle.create()
        options1.insertOption(OPTION_1, "test")
        builder1.addImplementationOptions(options1)

        val validatingBuilder = ValidatingBuilder()
        validatingBuilder.add(builder0.build())
        validatingBuilder.add(builder1.build())

        val sessionConfig = validatingBuilder.build()

        assertThat(sessionConfig.templateType).isEqualTo(CameraDevice.TEMPLATE_PREVIEW)
    }

    private fun createSurface(containerClass: Class<*>): DeferrableSurface {
        val deferrableSurface: DeferrableSurface = ImmediateSurface(mock(Surface::class.java))
        deferrableSurface.setContainerClass(containerClass)
        return deferrableSurface
    }

    @Test
    fun combineTwoSessionsSurfaces() {
        val previewSurface = createSurface(Preview::class.java)
        val imageCaptureSurface = createSurface(ImageCapture::class.java)

        val builder1 = SessionConfig.Builder()
        builder1.addSurface(previewSurface)
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)

        val builder2 = SessionConfig.Builder()
        builder2.addSurface(imageCaptureSurface)
        builder2.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)

        val validatingBuilder = ValidatingBuilder()
        validatingBuilder.add(builder1.build())
        validatingBuilder.add(builder2.build())

        val sessionConfig = validatingBuilder.build()

        val surfaces = sessionConfig.surfaces
        // Ensures the surfaces are all added and sorted correctly.
        assertThat(surfaces).containsExactly(previewSurface, imageCaptureSurface).inOrder()
    }

    @Test
    fun combineTwoSessionsOutputConfigs() {
        val nonRepeatingSurface = mock(DeferrableSurface::class.java)

        val builder0 = SessionConfig.Builder()
        val outputConfig0 = SessionConfig.OutputConfig.builder(mMockSurface0!!).build()
        builder0.addOutputConfig(outputConfig0)
        builder0.addNonRepeatingSurface(nonRepeatingSurface)
        builder0.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)

        val builder1 = SessionConfig.Builder()
        val outputConfig1 = SessionConfig.OutputConfig.builder(mMockSurface1!!).build()
        builder1.addOutputConfig(outputConfig1)
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)

        val validatingBuilder = ValidatingBuilder()
        validatingBuilder.add(builder0.build())
        validatingBuilder.add(builder1.build())

        val sessionConfig = validatingBuilder.build()

        val surfaces = sessionConfig.surfaces
        assertThat(surfaces).containsExactly(mMockSurface0, mMockSurface1, nonRepeatingSurface)
        assertThat(sessionConfig.outputConfigs).hasSize(3)
        assertThat(sessionConfig.outputConfigs[0]).isEqualTo(outputConfig0)
        assertThat(sessionConfig.outputConfigs[1].surface).isEqualTo(nonRepeatingSurface)
        assertThat(sessionConfig.outputConfigs[2]).isEqualTo(outputConfig1)
        // Should not contain the nonRepeatingSurface.
        assertThat(sessionConfig.repeatingCaptureConfig.surfaces)
            .containsExactly(mMockSurface0, mMockSurface1)
    }

    @Test
    fun combineTwoSessionsOptions() {
        val builder0 = SessionConfig.Builder()
        builder0.addSurface(mMockSurface0!!)
        builder0.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
        val options0 = MutableOptionsBundle.create()
        options0.insertOption(OPTION, 1)
        builder0.addImplementationOptions(options0)

        val builder1 = SessionConfig.Builder()
        builder1.addSurface(mMockSurface1!!)
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
        val options1 = MutableOptionsBundle.create()
        options1.insertOption(OPTION_1, "test")
        builder1.addImplementationOptions(options1)

        val validatingBuilder = ValidatingBuilder()
        validatingBuilder.add(builder0.build())
        validatingBuilder.add(builder1.build())

        val sessionConfig = validatingBuilder.build()

        val config = sessionConfig.implementationOptions

        assertThat(config.retrieveOption(OPTION)).isEqualTo(1)
        assertThat(config.retrieveOption(OPTION_1)).isEqualTo("test")
    }

    @Test
    fun combineTwoSessionsMultiValueSetValid() {
        val option =
            Option.create<FakeMultiValueSet>("multiValueSet", FakeMultiValueSet::class.java)

        val builder0 = SessionConfig.Builder()
        builder0.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
        val options0 = MutableOptionsBundle.create()
        val multiValueSet0 = FakeMultiValueSet()
        options0.insertOption(option, multiValueSet0)
        builder0.addImplementationOptions(options0)
        val config0 = builder0.build()

        val builder1 = SessionConfig.Builder()
        val options1 = MutableOptionsBundle.create()
        val multiValueSet1 = FakeMultiValueSet()
        options1.insertOption(option, multiValueSet1)
        builder1.addImplementationOptions(options1)
        val config1 = builder1.build()

        val validatingBuilder = ValidatingBuilder()
        validatingBuilder.add(config0)
        validatingBuilder.add(config1)

        assertThat(validatingBuilder.isValid).isTrue()
    }

    @Test
    fun builderAddMultipleRepeatingCameraCaptureCallbacks() {
        val builder = SessionConfig.Builder()
        val callback0 = mock(CameraCaptureCallback::class.java)
        val callback1 = mock(CameraCaptureCallback::class.java)

        builder.addRepeatingCameraCaptureCallback(callback0)
        builder.addRepeatingCameraCaptureCallback(callback1)
        val configuration = builder.build()

        assertThat(configuration.repeatingCameraCaptureCallbacks)
            .containsExactly(callback0, callback1)
        assertThat(configuration.singleCameraCaptureCallbacks).containsNoneOf(callback0, callback1)
    }

    @Test
    fun builderAddAllRepeatingCameraCaptureCallbacks() {
        val builder = SessionConfig.Builder()
        val callback0 = mock(CameraCaptureCallback::class.java)
        val callback1 = mock(CameraCaptureCallback::class.java)
        val callbacks: List<CameraCaptureCallback> = Lists.newArrayList(callback0, callback1)

        builder.addAllRepeatingCameraCaptureCallbacks(callbacks)
        val configuration = builder.build()

        assertThat(configuration.repeatingCameraCaptureCallbacks)
            .containsExactly(callback0, callback1)
        assertThat(configuration.singleCameraCaptureCallbacks).containsNoneOf(callback0, callback1)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun repeatingCameraCaptureCallbacks_areImmutable() {
        val builder = SessionConfig.Builder()
        val configuration = builder.build()

        configuration.repeatingCameraCaptureCallbacks.add(mock(CameraCaptureCallback::class.java))
    }

    @Test
    fun builderAddMultipleDeviceStateCallbacks() {
        val builder = SessionConfig.Builder()
        val callback0 = mock(CameraDevice.StateCallback::class.java)
        val callback1 = mock(CameraDevice.StateCallback::class.java)

        builder.addDeviceStateCallback(callback0)
        builder.addDeviceStateCallback(callback1)
        val configuration = builder.build()

        assertThat(configuration.deviceStateCallbacks).containsExactly(callback0, callback1)
    }

    @Test
    fun builderAddAllDeviceStateCallbacks() {
        val builder = SessionConfig.Builder()
        val callback0 = mock(CameraDevice.StateCallback::class.java)
        val callback1 = mock(CameraDevice.StateCallback::class.java)
        val callbacks: List<CameraDevice.StateCallback> = Lists.newArrayList(callback0, callback1)

        builder.addAllDeviceStateCallbacks(callbacks)
        val configuration = builder.build()

        assertThat(configuration.deviceStateCallbacks).containsExactly(callback0, callback1)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun deviceStateCallbacks_areImmutable() {
        val builder = SessionConfig.Builder()
        val configuration = builder.build()

        configuration.deviceStateCallbacks.add(mock(CameraDevice.StateCallback::class.java))
    }

    @Test
    fun builderAddMultipleSessionStateCallbacks() {
        val builder = SessionConfig.Builder()
        val callback0 = mock(CameraCaptureSession.StateCallback::class.java)
        val callback1 = mock(CameraCaptureSession.StateCallback::class.java)

        builder.addSessionStateCallback(callback0)
        builder.addSessionStateCallback(callback1)
        val configuration = builder.build()

        assertThat(configuration.sessionStateCallbacks).containsExactly(callback0, callback1)
    }

    @Test
    fun builderAddAllSessionStateCallbacks() {
        val builder = SessionConfig.Builder()
        val callback0 = mock(CameraCaptureSession.StateCallback::class.java)
        val callback1 = mock(CameraCaptureSession.StateCallback::class.java)
        val callbacks: List<CameraCaptureSession.StateCallback> =
            Lists.newArrayList(callback0, callback1)

        builder.addAllSessionStateCallbacks(callbacks)
        val configuration = builder.build()

        assertThat(configuration.sessionStateCallbacks).containsExactly(callback0, callback1)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun sessionStateCallbacks_areImmutable() {
        val builder = SessionConfig.Builder()
        val configuration = builder.build()

        configuration.sessionStateCallbacks.add(
            mock(CameraCaptureSession.StateCallback::class.java)
        )
    }

    @Test
    fun builderAddMultipleCameraCallbacks() {
        val builder = SessionConfig.Builder()
        val callback0 = mock(CameraCaptureCallback::class.java)
        val callback1 = mock(CameraCaptureCallback::class.java)

        builder.addCameraCaptureCallback(callback0)
        builder.addCameraCaptureCallback(callback1)
        val configuration = builder.build()

        assertThat(configuration.singleCameraCaptureCallbacks).containsExactly(callback0, callback1)
        assertThat(configuration.repeatingCameraCaptureCallbacks)
            .containsExactly(callback0, callback1)
    }

    @Test
    fun builderAddAllCameraCallbacks() {
        val builder = SessionConfig.Builder()
        val callback0 = mock(CameraCaptureCallback::class.java)
        val callback1 = mock(CameraCaptureCallback::class.java)
        val callbacks: List<CameraCaptureCallback> = Lists.newArrayList(callback0, callback1)

        builder.addAllCameraCaptureCallbacks(callbacks)
        val configuration = builder.build()

        assertThat(configuration.singleCameraCaptureCallbacks).containsExactly(callback0, callback1)
        assertThat(configuration.repeatingCameraCaptureCallbacks)
            .containsExactly(callback0, callback1)
    }

    @Test
    fun removeCameraCaptureCallback_returnsFalseIfNotAdded() {
        val mockCallback = mock(CameraCaptureCallback::class.java)
        val builder = SessionConfig.Builder()

        assertThat(builder.removeCameraCaptureCallback(mockCallback)).isFalse()
    }

    @Test
    fun canAddAndRemoveCameraCaptureCallback_withBuilder() {
        // Arrange.
        val mockRepeatingCallback = mock(CameraCaptureCallback::class.java)
        val mockSingleCallback = mock(CameraCaptureCallback::class.java)
        val builder = SessionConfig.Builder()

        // Act.
        builder.addRepeatingCameraCaptureCallback(mockRepeatingCallback)
        builder.addCameraCaptureCallback(mockSingleCallback)
        val sessionConfigWithCallbacks = builder.build()

        // Assert.
        assertThat(sessionConfigWithCallbacks.singleCameraCaptureCallbacks)
            .contains(mockSingleCallback)
        assertThat(sessionConfigWithCallbacks.singleCameraCaptureCallbacks)
            .contains(mockSingleCallback)

        // Act.
        val removedSingle = builder.removeCameraCaptureCallback(mockSingleCallback)
        val sessionConfigWithoutSingleCallback = builder.build()

        // Assert.
        assertThat(removedSingle).isTrue()
        assertThat(sessionConfigWithoutSingleCallback.singleCameraCaptureCallbacks)
            .doesNotContain(mockSingleCallback)

        // Act.
        val removedRepeating = builder.removeCameraCaptureCallback(mockRepeatingCallback)
        val sessionConfigWithoutCallbacks = builder.build()

        // Assert.
        assertThat(removedRepeating).isTrue()
        assertThat(sessionConfigWithoutCallbacks.repeatingCameraCaptureCallbacks)
            .doesNotContain(mockRepeatingCallback)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun singleCameraCaptureCallbacks_areImmutable() {
        val builder = SessionConfig.Builder()
        val configuration = builder.build()

        configuration.singleCameraCaptureCallbacks.add(mock(CameraCaptureCallback::class.java))
    }

    @Test
    fun builderAddErrorListener() {
        val builder = SessionConfig.Builder()
        val callback = mock(SessionConfig.ErrorListener::class.java)

        builder.setErrorListener(callback)

        val config = builder.build()

        assertThat(config.errorListener).isSameInstanceAs(callback)
    }

    @Test
    fun combineTwoSessionsCallbacks() {
        val builder0 = SessionConfig.Builder()
        val sessionCallback0 = mock(CameraCaptureSession.StateCallback::class.java)
        val deviceCallback0 = mock(CameraDevice.StateCallback::class.java)
        val repeatingCallback0 = mock(CameraCaptureCallback::class.java)
        val cameraCallback0 = mock(CameraCaptureCallback::class.java)
        var errorCallbackInvoked0 = false
        val errorListener0 = SessionConfig.ErrorListener { _, _ -> errorCallbackInvoked0 = true }
        builder0.addSessionStateCallback(sessionCallback0)
        builder0.addDeviceStateCallback(deviceCallback0)
        builder0.addRepeatingCameraCaptureCallback(repeatingCallback0)
        builder0.addCameraCaptureCallback(cameraCallback0)
        builder0.setErrorListener(errorListener0)

        val builder1 = SessionConfig.Builder()
        val sessionCallback1 = mock(CameraCaptureSession.StateCallback::class.java)
        val deviceCallback1 = mock(CameraDevice.StateCallback::class.java)
        val repeatingCallback1 = mock(CameraCaptureCallback::class.java)
        val cameraCallback1 = mock(CameraCaptureCallback::class.java)
        var errorCallbackInvoked1 = false
        val errorListener1 = SessionConfig.ErrorListener { _, _ -> errorCallbackInvoked1 = true }
        builder1.addSessionStateCallback(sessionCallback1)
        builder1.addDeviceStateCallback(deviceCallback1)
        builder1.addRepeatingCameraCaptureCallback(repeatingCallback1)
        builder1.addCameraCaptureCallback(cameraCallback1)
        builder1.setErrorListener(errorListener1)

        val validatingBuilder = ValidatingBuilder()
        validatingBuilder.add(builder0.build())
        validatingBuilder.add(builder1.build())

        val sessionConfig = validatingBuilder.build()

        assertThat(sessionConfig.sessionStateCallbacks)
            .containsExactly(sessionCallback0, sessionCallback1)
        assertThat(sessionConfig.deviceStateCallbacks)
            .containsExactly(deviceCallback0, deviceCallback1)
        assertThat(sessionConfig.repeatingCameraCaptureCallbacks)
            .containsExactly(
                repeatingCallback0,
                cameraCallback0,
                repeatingCallback1,
                cameraCallback1
            )
        assertThat(sessionConfig.singleCameraCaptureCallbacks)
            .containsExactly(cameraCallback0, cameraCallback1)
        sessionConfig.errorListener!!.onError(
            sessionConfig,
            SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET
        )
        assertThat(errorCallbackInvoked0).isTrue()
        assertThat(errorCallbackInvoked1).isTrue()
    }

    @Test
    fun combineTwoSessionsTagsValid() {
        val session0 = createSessionConfigWithTag("TEST00", 0)
        val session1 = createSessionConfigWithTag("TEST01", "String")

        val validatingBuilder = ValidatingBuilder()
        validatingBuilder.add(session0)
        validatingBuilder.add(session1)

        val sessionCombined = validatingBuilder.build()

        assertThat(validatingBuilder.isValid).isTrue()

        val tag = sessionCombined.repeatingCaptureConfig.tagBundle

        assertThat(tag.getTag("TEST00")).isEqualTo(0)
        assertThat(tag.getTag("TEST01")).isEqualTo("String")
    }

    @Test
    fun builderChange_doNotChangeEarlierBuiltInstance() {
        // 1. Arrange
        val callback1 = mock(CameraCaptureCallback::class.java)
        val callback2 = mock(CameraCaptureCallback::class.java)
        val deferrableSurface1 = mock(DeferrableSurface::class.java)
        val deferrableSurface2 = mock(DeferrableSurface::class.java)
        val deviceStateCallback1 = mock(CameraDevice.StateCallback::class.java)
        val deviceStateCallback2 = mock(CameraDevice.StateCallback::class.java)
        val sessionCallback1 = mock(CameraCaptureSession.StateCallback::class.java)
        val sessionCallback2 = mock(CameraCaptureSession.StateCallback::class.java)
        val errorListener1 = mock(SessionConfig.ErrorListener::class.java)
        val errorListener2 = mock(SessionConfig.ErrorListener::class.java)
        val fpsRange1 = Range(30, 30)
        val fpsRange2 = Range(15, 30)
        val optionsBundle1 = MutableOptionsBundle.create()
        optionsBundle1.insertOption(OPTION, 1)
        val optionsBundle2 = MutableOptionsBundle.create()
        optionsBundle2.insertOption(OPTION, 2)
        val template1 = CameraDevice.TEMPLATE_PREVIEW
        val template2 = CameraDevice.TEMPLATE_RECORD

        val builder = SessionConfig.Builder()
        builder.addSurface(deferrableSurface1)
        builder.setExpectedFrameRateRange(fpsRange1)
        builder.addCameraCaptureCallback(callback1)
        builder.addRepeatingCameraCaptureCallback(callback1)
        builder.addDeviceStateCallback(deviceStateCallback1)
        builder.addSessionStateCallback(sessionCallback1)
        builder.setTemplateType(template1)
        builder.addImplementationOptions(optionsBundle1)
        builder.setErrorListener(errorListener1)
        val sessionConfig = builder.build()

        // 2. Act
        // builder change should not affect the instance built earlier.
        builder.addSurface(deferrableSurface2)
        builder.setExpectedFrameRateRange(fpsRange2)
        builder.addCameraCaptureCallback(callback2)
        builder.addRepeatingCameraCaptureCallback(callback2)
        builder.addDeviceStateCallback(deviceStateCallback2)
        builder.addSessionStateCallback(sessionCallback2)
        builder.setTemplateType(template2)
        builder.addImplementationOptions(optionsBundle2)
        builder.setErrorListener(errorListener2)

        // 3. Verify
        assertThat(sessionConfig.surfaces).containsExactly(deferrableSurface1)
        assertThat(sessionConfig.expectedFrameRateRange).isEqualTo(fpsRange1)
        assertThat(sessionConfig.singleCameraCaptureCallbacks).containsExactly(callback1)
        assertThat(sessionConfig.repeatingCaptureConfig.cameraCaptureCallbacks)
            .containsExactly(callback1)
        assertThat(sessionConfig.deviceStateCallbacks).containsExactly(deviceStateCallback1)
        assertThat(sessionConfig.sessionStateCallbacks).containsExactly(sessionCallback1)
        assertThat(sessionConfig.templateType).isEqualTo(template1)
        assertThat(sessionConfig.implementationOptions.retrieveOption(OPTION)).isEqualTo(1)
        assertThat(sessionConfig.errorListener).isEqualTo(errorListener1)
    }

    @Test
    fun validatingBuilderChange_doNotChangeEarlierBuiltInstance() {
        // 1. Arrange
        val callback1 = mock(CameraCaptureCallback::class.java)
        val callback2 = mock(CameraCaptureCallback::class.java)
        val deferrableSurface1 = mock(DeferrableSurface::class.java)
        val deferrableSurface2 = mock(DeferrableSurface::class.java)
        val deviceStateCallback1 = mock(CameraDevice.StateCallback::class.java)
        val deviceStateCallback2 = mock(CameraDevice.StateCallback::class.java)
        val sessionCallback1 = mock(CameraCaptureSession.StateCallback::class.java)
        val sessionCallback2 = mock(CameraCaptureSession.StateCallback::class.java)
        var errorCallbackInvoked1 = false
        val errorListener1 = SessionConfig.ErrorListener { _, _ -> errorCallbackInvoked1 = true }
        val errorListener2 = mock(SessionConfig.ErrorListener::class.java)
        val fpsRange1 = Range(30, 30)
        val fpsRange2 = Range(15, 30)
        val optionsBundle1 = MutableOptionsBundle.create()
        optionsBundle1.insertOption(OPTION, 1)
        val optionsBundle2 = MutableOptionsBundle.create()
        optionsBundle2.insertOption(OPTION, 2)
        val template1 = CameraDevice.TEMPLATE_PREVIEW
        val template2 = CameraDevice.TEMPLATE_RECORD

        val builder = SessionConfig.Builder()
        builder.addSurface(deferrableSurface1)
        builder.setExpectedFrameRateRange(fpsRange1)
        builder.addCameraCaptureCallback(callback1)
        builder.addRepeatingCameraCaptureCallback(callback1)
        builder.addDeviceStateCallback(deviceStateCallback1)
        builder.addSessionStateCallback(sessionCallback1)
        builder.setTemplateType(template1)
        builder.addImplementationOptions(optionsBundle1)
        builder.setErrorListener(errorListener1)

        val validatingBuilder = ValidatingBuilder()
        validatingBuilder.add(builder.build())
        val sessionConfig = validatingBuilder.build()

        // 2. Act
        // add another SessionConfig to ValidatingBuilder. This should not affect the
        // instance built earlier.
        val builder2 = SessionConfig.Builder()
        builder2.addSurface(deferrableSurface2)
        builder2.setExpectedFrameRateRange(fpsRange2)
        builder2.addCameraCaptureCallback(callback2)
        builder2.addRepeatingCameraCaptureCallback(callback2)
        builder2.addDeviceStateCallback(deviceStateCallback2)
        builder2.addSessionStateCallback(sessionCallback2)
        builder2.setTemplateType(template2)
        builder2.addImplementationOptions(optionsBundle2)
        builder2.setErrorListener(errorListener2)
        validatingBuilder.add(builder2.build())

        // 3. Verify
        assertThat(sessionConfig.surfaces).containsExactly(deferrableSurface1)
        assertThat(sessionConfig.expectedFrameRateRange).isEqualTo(fpsRange1)
        assertThat(sessionConfig.singleCameraCaptureCallbacks).containsExactly(callback1)
        assertThat(sessionConfig.repeatingCaptureConfig.cameraCaptureCallbacks)
            .containsExactly(callback1)
        assertThat(sessionConfig.deviceStateCallbacks).containsExactly(deviceStateCallback1)
        assertThat(sessionConfig.sessionStateCallbacks).containsExactly(sessionCallback1)
        assertThat(sessionConfig.templateType).isEqualTo(template1)
        assertThat(sessionConfig.implementationOptions.retrieveOption(OPTION)).isEqualTo(1)
        // The preview error callback can still be invoked after the builder aggregates the error
        // listeners
        sessionConfig.errorListener!!.onError(
            sessionConfig,
            SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET
        )
        assertThat(errorCallbackInvoked1).isTrue()
    }

    private fun createSessionConfigWithTag(key: String, tagValue: Any): SessionConfig {
        val builder1 = SessionConfig.Builder()
        builder1.addSurface(mMockSurface1!!)
        builder1.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
        builder1.addTag(key, tagValue)

        return builder1.build()
    }

    companion object {
        private val OPTION = Option.create<Int>("camerax.test.option_0", Int::class.java)
        private val OPTION_1 = Option.create<String>("camerax.test.option_1", String::class.java)
    }
}
