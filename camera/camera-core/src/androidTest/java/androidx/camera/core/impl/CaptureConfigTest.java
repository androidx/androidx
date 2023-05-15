/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core.impl;

import static androidx.camera.core.impl.Config.OptionPriority.REQUIRED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.hardware.camera2.CameraDevice;
import android.util.Range;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.Config.Option;
import androidx.camera.testing.DeferrableSurfacesUtil;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@LargeTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public class CaptureConfigTest {
    private static final Option<Integer> OPTION = Config.Option.create(
            "camerax.test.option_0", Integer.class);

    private DeferrableSurface mMockSurface0;
    private static final Config.Option<FakeMultiValueSet> FAKE_MULTI_VALUE_SET_OPTION =
            Config.Option.create("option.fakeMultiValueSet.1", FakeMultiValueSet.class);

    @Before
    public void setup() {
        mMockSurface0 = mock(DeferrableSurface.class);
    }

    @Test
    public void builderSetTemplate() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
        CaptureConfig captureConfig = builder.build();

        assertThat(captureConfig.getTemplateType()).isEqualTo(CameraDevice.TEMPLATE_PREVIEW);
    }

    @Test
    public void builderNotSetTemplate() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        CaptureConfig captureConfig = builder.build();

        assertThat(captureConfig.getTemplateType()).isEqualTo(CaptureConfig.TEMPLATE_TYPE_NONE);
    }

    @Test
    public void builderAddSurface() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        builder.addSurface(mMockSurface0);
        CaptureConfig captureConfig = builder.build();

        List<DeferrableSurface> surfaces = captureConfig.getSurfaces();

        assertThat(surfaces).hasSize(1);
        assertThat(surfaces).contains(mMockSurface0);
    }

    @Test
    public void builderRemoveSurface() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        builder.addSurface(mMockSurface0);
        builder.removeSurface(mMockSurface0);
        CaptureConfig captureConfig = builder.build();

        List<Surface> surfaces = DeferrableSurfacesUtil.surfaceList(captureConfig.getSurfaces());
        assertThat(surfaces).isEmpty();
    }

    @Test
    public void builderClearSurface() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        builder.addSurface(mMockSurface0);
        builder.clearSurfaces();
        CaptureConfig captureConfig = builder.build();

        List<Surface> surfaces = DeferrableSurfacesUtil.surfaceList(captureConfig.getSurfaces());
        assertThat(surfaces.size()).isEqualTo(0);
    }

    @Test
    public void builderAddOption() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        MutableOptionsBundle options = MutableOptionsBundle.create();
        options.insertOption(OPTION, 1);
        builder.addImplementationOptions(options);
        CaptureConfig captureConfig = builder.build();

        Config config = captureConfig.getImplementationOptions();

        assertThat(config.containsOption(OPTION)).isTrue();
        assertThat(config.retrieveOption(OPTION)).isEqualTo(1);
    }

    @Test
    public void addOption_priorityIsKept() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        MutableOptionsBundle options = MutableOptionsBundle.create();
        options.insertOption(OPTION, REQUIRED, 1);
        builder.addImplementationOptions(options);
        CaptureConfig captureConfig = builder.build();

        Config config = captureConfig.getImplementationOptions();

        assertThat(config.containsOption(OPTION)).isTrue();
        assertThat(config.retrieveOption(OPTION)).isEqualTo(1);
        assertThat(config.getOptionPriority(OPTION)).isEqualTo(REQUIRED);
    }

    @Test
    public void builderSetUseTargetedSurface() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        builder.setUseRepeatingSurface(true);
        CaptureConfig captureConfig = builder.build();

        assertThat(captureConfig.isUseRepeatingSurface()).isTrue();
    }

    @Test
    public void builderAddMultipleCameraCaptureCallbacks() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        CameraCaptureCallback callback0 = mock(CameraCaptureCallback.class);
        CameraCaptureCallback callback1 = mock(CameraCaptureCallback.class);

        builder.addCameraCaptureCallback(callback0);
        builder.addCameraCaptureCallback(callback1);
        CaptureConfig configuration = builder.build();

        assertThat(configuration.getCameraCaptureCallbacks()).containsExactly(callback0, callback1);
    }

    @Test
    public void builderAddAllCameraCaptureCallbacks() {
        SessionConfig.Builder builder = new SessionConfig.Builder();
        CameraCaptureCallback callback0 = mock(CameraCaptureCallback.class);
        CameraCaptureCallback callback1 = mock(CameraCaptureCallback.class);
        List<CameraCaptureCallback> callbacks = Lists.newArrayList(callback0, callback1);

        builder.addAllRepeatingCameraCaptureCallbacks(callbacks);
        SessionConfig configuration = builder.build();

        assertThat(configuration.getRepeatingCameraCaptureCallbacks())
                .containsExactly(callback0, callback1);
    }

    @Test
    public void builderAddImplementationMultiValue() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        Object obj1 = new Object();
        FakeMultiValueSet fakeMultiValueSet1 = new FakeMultiValueSet();
        fakeMultiValueSet1.addAll(Arrays.asList(obj1));
        MutableOptionsBundle fakeConfig1 = MutableOptionsBundle.create();
        fakeConfig1.insertOption(FAKE_MULTI_VALUE_SET_OPTION, fakeMultiValueSet1);
        builder.addImplementationOptions(fakeConfig1);

        Object obj2 = new Object();
        FakeMultiValueSet fakeMultiValueSet2 = new FakeMultiValueSet();
        fakeMultiValueSet2.addAll(Arrays.asList(obj2));
        MutableOptionsBundle fakeConfig2 = MutableOptionsBundle.create();
        fakeConfig2.insertOption(FAKE_MULTI_VALUE_SET_OPTION, fakeMultiValueSet2);
        builder.addImplementationOptions(fakeConfig2);

        CaptureConfig captureConfig = builder.build();

        FakeMultiValueSet fakeMultiValueSet =
                captureConfig.getImplementationOptions().retrieveOption(
                        FAKE_MULTI_VALUE_SET_OPTION);

        assertThat(fakeMultiValueSet).isNotNull();
        assertThat(fakeMultiValueSet.getAllItems()).containsExactly(obj1, obj2);
    }

    @Test
    public void builderAddSingleImplementationOption() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        builder.addImplementationOption(CaptureConfig.OPTION_ROTATION, 90);

        CaptureConfig captureConfig = builder.build();

        assertThat(captureConfig.getImplementationOptions().retrieveOption(
                CaptureConfig.OPTION_ROTATION)).isEqualTo(90);
    }

    @Test
    public void builderFromPrevious_containsCameraCaptureCallbacks() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        CameraCaptureCallback callback0 = mock(CameraCaptureCallback.class);
        CameraCaptureCallback callback1 = mock(CameraCaptureCallback.class);
        builder.addCameraCaptureCallback(callback0);
        builder.addCameraCaptureCallback(callback1);
        builder = CaptureConfig.Builder.from(builder.build());
        CameraCaptureCallback callback2 = mock(CameraCaptureCallback.class);

        builder.addCameraCaptureCallback(callback2);
        CaptureConfig configuration = builder.build();

        assertThat(configuration.getCameraCaptureCallbacks())
                .containsExactly(callback0, callback1, callback2);
    }

    @Test
    public void builderRemoveCameraCaptureCallback_returnsFalseIfNotAdded() {
        CameraCaptureCallback mockCallback = mock(CameraCaptureCallback.class);
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        assertThat(builder.removeCameraCaptureCallback(mockCallback)).isFalse();
    }

    @Test
    public void builderRemoveCameraCaptureCallback_removesAddedCallback() {
        // Arrange.
        CameraCaptureCallback mockCallback = mock(CameraCaptureCallback.class);
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        // Act.
        builder.addCameraCaptureCallback(mockCallback);
        CaptureConfig configWithCallback = builder.build();

        // Assert.
        assertThat(configWithCallback.getCameraCaptureCallbacks()).contains(mockCallback);

        // Act.
        boolean removedCallback = builder.removeCameraCaptureCallback(mockCallback);
        CaptureConfig configWithoutCallback = builder.build();

        // Assert.
        assertThat(removedCallback).isTrue();
        assertThat(configWithoutCallback.getCameraCaptureCallbacks()).doesNotContain(mockCallback);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void cameraCaptureCallbacks_areImmutable() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        CaptureConfig configuration = builder.build();

        configuration.getCameraCaptureCallbacks().add(mock(CameraCaptureCallback.class));
    }

    @Test
    public void builderChange_doNotChangeEarlierBuiltInstance() {
        // 1. Arrange
        CameraCaptureCallback callback1 = mock(CameraCaptureCallback.class);
        CameraCaptureCallback callback2 = mock(CameraCaptureCallback.class);
        DeferrableSurface deferrableSurface1 = mock(DeferrableSurface.class);
        DeferrableSurface deferrableSurface2 = mock(DeferrableSurface.class);
        Range<Integer> fpsRange1 = new Range<>(30, 30);
        Range<Integer> fpsRange2 = new Range<>(15, 30);
        int optionValue1 = 1;
        int optionValue2 = 2;
        int tagValue1 = 1;
        int tagValue2 = 2;
        int template1 = CameraDevice.TEMPLATE_PREVIEW;
        int template2 = CameraDevice.TEMPLATE_RECORD;

        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        builder.addSurface(deferrableSurface1);
        builder.setExpectedFrameRateRange(fpsRange1);
        builder.addCameraCaptureCallback(callback1);
        builder.setTemplateType(template1);
        builder.addTag("KEY", tagValue1);
        builder.addImplementationOption(OPTION, optionValue1);
        CaptureConfig captureConfig = builder.build();

        // 2. Act
        // builder change should not affect the instance built earlier.
        builder.addSurface(deferrableSurface2);
        builder.setExpectedFrameRateRange(fpsRange2);
        builder.addCameraCaptureCallback(callback2);
        builder.setTemplateType(template2);
        builder.addTag("KEY", tagValue2);
        builder.addImplementationOption(OPTION, optionValue2);

        // 3. Verify
        assertThat(captureConfig.getSurfaces()).containsExactly(deferrableSurface1);
        assertThat(captureConfig.getExpectedFrameRateRange()).isEqualTo(fpsRange1);
        assertThat(captureConfig.getCameraCaptureCallbacks()).containsExactly(callback1);
        assertThat(captureConfig.getTemplateType()).isEqualTo(template1);
        assertThat(captureConfig.getTagBundle().getTag("KEY")).isEqualTo(tagValue1);
        assertThat(captureConfig.getImplementationOptions().retrieveOption(OPTION))
                .isEqualTo(optionValue1);
    }

    /**
     * A fake {@link MultiValueSet}.
     */
    static class FakeMultiValueSet extends MultiValueSet<Object> {
        @NonNull
        @Override
        public MultiValueSet<Object> clone() {
            FakeMultiValueSet multiValueSet = new FakeMultiValueSet();
            multiValueSet.addAll(getAllItems());
            return multiValueSet;
        }
    }
}
