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

import android.hardware.camera2.CameraDevice
import android.os.Build
import android.util.Range
import androidx.camera.core.impl.Config.Option
import androidx.camera.core.impl.Config.OptionPriority
import androidx.camera.core.impl.stabilization.StabilizationMode
import androidx.camera.testing.impl.DeferrableSurfacesUtil
import com.google.common.collect.Lists
import com.google.common.truth.Truth.assertThat
import java.util.Arrays
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
class CaptureConfigTest {
    private var mMockSurface0: DeferrableSurface? = null

    @Before
    fun setup() {
        mMockSurface0 = mock(DeferrableSurface::class.java)
    }

    @Test
    fun builderSetTemplate() {
        val builder = CaptureConfig.Builder()

        builder.templateType = CameraDevice.TEMPLATE_PREVIEW
        val captureConfig = builder.build()

        assertThat(captureConfig.templateType).isEqualTo(CameraDevice.TEMPLATE_PREVIEW)
    }

    @Test
    fun builderNotSetTemplate() {
        val builder = CaptureConfig.Builder()

        val captureConfig = builder.build()

        assertThat(captureConfig.templateType).isEqualTo(CaptureConfig.TEMPLATE_TYPE_NONE)
    }

    @Test
    fun builderAddSurface() {
        val builder = CaptureConfig.Builder()

        builder.addSurface(mMockSurface0!!)
        val captureConfig = builder.build()

        val surfaces = captureConfig.surfaces

        assertThat(surfaces).hasSize(1)
        assertThat(surfaces).contains(mMockSurface0)
    }

    @Test
    fun builderRemoveSurface() {
        val builder = CaptureConfig.Builder()

        builder.addSurface(mMockSurface0!!)
        builder.removeSurface(mMockSurface0!!)
        val captureConfig = builder.build()

        val surfaces = DeferrableSurfacesUtil.surfaceList(captureConfig.surfaces)
        assertThat(surfaces).isEmpty()
    }

    @Test
    fun builderClearSurface() {
        val builder = CaptureConfig.Builder()

        builder.addSurface(mMockSurface0!!)
        builder.clearSurfaces()
        val captureConfig = builder.build()

        val surfaces = DeferrableSurfacesUtil.surfaceList(captureConfig.surfaces)
        assertThat(surfaces.size).isEqualTo(0)
    }

    @Test
    fun builderAddOption() {
        val builder = CaptureConfig.Builder()

        val options = MutableOptionsBundle.create()
        options.insertOption(OPTION, 1)
        builder.addImplementationOptions(options)
        val captureConfig = builder.build()

        val config = captureConfig.implementationOptions

        assertThat(config.containsOption(OPTION)).isTrue()
        assertThat(config.retrieveOption(OPTION)).isEqualTo(1)
    }

    @Test
    fun addOption_priorityIsKept() {
        val builder = CaptureConfig.Builder()

        val options = MutableOptionsBundle.create()
        options.insertOption(OPTION, OptionPriority.REQUIRED, 1)
        builder.addImplementationOptions(options)
        val captureConfig = builder.build()

        val config = captureConfig.implementationOptions

        assertThat(config.containsOption(OPTION)).isTrue()
        assertThat(config.retrieveOption(OPTION)).isEqualTo(1)
        assertThat(config.getOptionPriority(OPTION)).isEqualTo(OptionPriority.REQUIRED)
    }

    @Test
    fun builderSetUseTargetedSurface() {
        val builder = CaptureConfig.Builder()

        builder.isUseRepeatingSurface = true
        val captureConfig = builder.build()

        assertThat(captureConfig.isUseRepeatingSurface).isTrue()
    }

    @Test
    fun builderAddMultipleCameraCaptureCallbacks() {
        val builder = CaptureConfig.Builder()
        val callback0 = mock(CameraCaptureCallback::class.java)
        val callback1 = mock(CameraCaptureCallback::class.java)

        builder.addCameraCaptureCallback(callback0)
        builder.addCameraCaptureCallback(callback1)
        val configuration = builder.build()

        assertThat(configuration.cameraCaptureCallbacks).containsExactly(callback0, callback1)
    }

    @Test
    fun builderAddAllCameraCaptureCallbacks() {
        val builder = SessionConfig.Builder()
        val callback0 = mock(CameraCaptureCallback::class.java)
        val callback1 = mock(CameraCaptureCallback::class.java)
        val callbacks: List<CameraCaptureCallback> = Lists.newArrayList(callback0, callback1)

        builder.addAllRepeatingCameraCaptureCallbacks(callbacks)
        val configuration = builder.build()

        assertThat(configuration.repeatingCameraCaptureCallbacks)
            .containsExactly(callback0, callback1)
    }

    @Test
    fun builderAddImplementationMultiValue() {
        val builder = CaptureConfig.Builder()

        val obj1 = Any()
        val fakeMultiValueSet1 = FakeMultiValueSet()
        fakeMultiValueSet1.addAll(Arrays.asList(obj1))
        val fakeConfig1 = MutableOptionsBundle.create()
        fakeConfig1.insertOption(FAKE_MULTI_VALUE_SET_OPTION, fakeMultiValueSet1)
        builder.addImplementationOptions(fakeConfig1)

        val obj2 = Any()
        val fakeMultiValueSet2 = FakeMultiValueSet()
        fakeMultiValueSet2.addAll(Arrays.asList(obj2))
        val fakeConfig2 = MutableOptionsBundle.create()
        fakeConfig2.insertOption(FAKE_MULTI_VALUE_SET_OPTION, fakeMultiValueSet2)
        builder.addImplementationOptions(fakeConfig2)

        val captureConfig = builder.build()

        val fakeMultiValueSet =
            captureConfig.implementationOptions.retrieveOption(FAKE_MULTI_VALUE_SET_OPTION)

        assertThat(fakeMultiValueSet).isNotNull()
        assertThat(fakeMultiValueSet!!.allItems).containsExactly(obj1, obj2)
    }

    @Test
    fun builderAddSingleImplementationOption() {
        val builder = CaptureConfig.Builder()

        builder.addImplementationOption(CaptureConfig.OPTION_ROTATION, 90)

        val captureConfig = builder.build()

        assertThat(
                captureConfig.implementationOptions.retrieveOption(CaptureConfig.OPTION_ROTATION)
            )
            .isEqualTo(90)
    }

    @Test
    fun builderFromPrevious_containsCameraCaptureCallbacks() {
        var builder = CaptureConfig.Builder()
        val callback0 = mock(CameraCaptureCallback::class.java)
        val callback1 = mock(CameraCaptureCallback::class.java)
        builder.addCameraCaptureCallback(callback0)
        builder.addCameraCaptureCallback(callback1)
        builder = CaptureConfig.Builder.from(builder.build())
        val callback2 = mock(CameraCaptureCallback::class.java)

        builder.addCameraCaptureCallback(callback2)
        val configuration = builder.build()

        assertThat(configuration.cameraCaptureCallbacks)
            .containsExactly(callback0, callback1, callback2)
    }

    @Test
    fun builderRemoveCameraCaptureCallback_returnsFalseIfNotAdded() {
        val mockCallback = mock(CameraCaptureCallback::class.java)
        val builder = CaptureConfig.Builder()

        assertThat(builder.removeCameraCaptureCallback(mockCallback)).isFalse()
    }

    @Test
    fun builderRemoveCameraCaptureCallback_removesAddedCallback() {
        // Arrange.
        val mockCallback = mock(CameraCaptureCallback::class.java)
        val builder = CaptureConfig.Builder()

        // Act.
        builder.addCameraCaptureCallback(mockCallback)
        val configWithCallback = builder.build()

        // Assert.
        assertThat(configWithCallback.cameraCaptureCallbacks).contains(mockCallback)

        // Act.
        val removedCallback = builder.removeCameraCaptureCallback(mockCallback)
        val configWithoutCallback = builder.build()

        // Assert.
        assertThat(removedCallback).isTrue()
        assertThat(configWithoutCallback.cameraCaptureCallbacks).doesNotContain(mockCallback)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun cameraCaptureCallbacks_areImmutable() {
        val builder = CaptureConfig.Builder()
        val configuration = builder.build()

        configuration.cameraCaptureCallbacks.add(mock(CameraCaptureCallback::class.java))
    }

    @Test
    fun postviewEnabledDefaultIsFalse() {
        // 1. Arrange / Act
        val captureConfig = CaptureConfig.Builder().build()

        // 3. Assert
        assertThat(captureConfig.isPostviewEnabled).isFalse()
    }

    @Test
    fun canSetPostviewEnabled() {
        // 1. Arrange
        val builder = CaptureConfig.Builder()

        // 2. Act
        builder.setPostviewEnabled(true)
        val captureConfig = builder.build()

        // 3. Assert
        assertThat(captureConfig.isPostviewEnabled).isTrue()
    }

    @Test
    fun builderChange_doNotChangeEarlierBuiltInstance() {
        // 1. Arrange
        val callback1 = mock(CameraCaptureCallback::class.java)
        val callback2 = mock(CameraCaptureCallback::class.java)
        val deferrableSurface1 = mock(DeferrableSurface::class.java)
        val deferrableSurface2 = mock(DeferrableSurface::class.java)
        val fpsRange1 = Range(30, 30)
        val fpsRange2 = Range(15, 30)
        val optionValue1 = 1
        val optionValue2 = 2
        val tagValue1 = 1
        val tagValue2 = 2
        val template1 = CameraDevice.TEMPLATE_PREVIEW
        val template2 = CameraDevice.TEMPLATE_RECORD

        val builder = CaptureConfig.Builder()
        builder.addSurface(deferrableSurface1)
        builder.setExpectedFrameRateRange(fpsRange1)
        builder.addCameraCaptureCallback(callback1)
        builder.templateType = template1
        builder.addTag("KEY", tagValue1)
        builder.addImplementationOption(OPTION, optionValue1)
        val captureConfig = builder.build()

        // 2. Act
        // builder change should not affect the instance built earlier.
        builder.addSurface(deferrableSurface2)
        builder.setExpectedFrameRateRange(fpsRange2)
        builder.addCameraCaptureCallback(callback2)
        builder.templateType = template2
        builder.addTag("KEY", tagValue2)
        builder.addImplementationOption(OPTION, optionValue2)

        // 3. Verify
        assertThat(captureConfig.surfaces).containsExactly(deferrableSurface1)
        assertThat(captureConfig.expectedFrameRateRange).isEqualTo(fpsRange1)
        assertThat(captureConfig.cameraCaptureCallbacks).containsExactly(callback1)
        assertThat(captureConfig.templateType).isEqualTo(template1)
        assertThat(captureConfig.tagBundle.getTag("KEY")).isEqualTo(tagValue1)
        assertThat(captureConfig.implementationOptions.retrieveOption(OPTION))
            .isEqualTo(optionValue1)
    }

    @Test
    fun fpsRangeSetToBuilder_correctFpsRangeAtBuiltInstance() {
        val fpsRange = Range(7, 60)
        // 1. Arrange
        val builder = CaptureConfig.Builder()

        // 2. Act
        builder.setExpectedFrameRateRange(fpsRange)
        val captureConfig = builder.build()

        // 3. Assert
        assertThat(captureConfig.expectedFrameRateRange).isEqualTo(fpsRange)
    }

    @Test
    fun previewStabilizationModeSetToBuilder_correctModeAtBuiltInstance() {
        // 1. Arrange
        val builder = CaptureConfig.Builder()

        // 2. Act
        builder.setPreviewStabilization(StabilizationMode.ON)
        val captureConfig = builder.build()

        // 3. Assert
        assertThat(captureConfig.previewStabilizationMode).isEqualTo(StabilizationMode.ON)
    }

    @Test
    fun videoStabilizationModeSetToBuilder_correctModeAtBuiltInstance() {
        // 1. Arrange
        val builder = CaptureConfig.Builder()

        // 2. Act
        builder.setVideoStabilization(StabilizationMode.ON)
        val captureConfig = builder.build()

        // 3. Assert
        assertThat(captureConfig.videoStabilizationMode).isEqualTo(StabilizationMode.ON)
    }

    @Test
    fun videoStabilizationModeSetToOff_afterPreviewStabilizationSetToOn_noInterference() {
        // 1. Arrange
        val builder = CaptureConfig.Builder()

        // 2. Act
        builder.setPreviewStabilization(StabilizationMode.ON)
        builder.setVideoStabilization(StabilizationMode.OFF)
        val captureConfig = builder.build()

        // 3. Assert
        assertThat(captureConfig.previewStabilizationMode).isEqualTo(StabilizationMode.ON)
        assertThat(captureConfig.videoStabilizationMode).isEqualTo(StabilizationMode.OFF)
    }

    /** A fake [MultiValueSet]. */
    internal class FakeMultiValueSet : MultiValueSet<Any?>() {
        override fun clone(): MultiValueSet<Any?> {
            val multiValueSet = FakeMultiValueSet()
            multiValueSet.addAll(allItems)
            return multiValueSet
        }
    }

    companion object {
        private val OPTION = Option.create<Int>("camerax.test.option_0", Int::class.java)

        private val FAKE_MULTI_VALUE_SET_OPTION: Option<FakeMultiValueSet> =
            Option.create("option.fakeMultiValueSet.1", FakeMultiValueSet::class.java)
    }
}
