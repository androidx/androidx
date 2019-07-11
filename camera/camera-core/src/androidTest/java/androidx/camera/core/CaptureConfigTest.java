/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.hardware.camera2.CameraDevice;
import android.view.Surface;

import androidx.camera.core.Config.Option;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@LargeTest
@RunWith(AndroidJUnit4.class)
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

        List<Surface> surfaces = DeferrableSurfaces.surfaceList(captureConfig.getSurfaces());
        assertThat(surfaces).isEmpty();
    }

    @Test
    public void builderClearSurface() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();

        builder.addSurface(mMockSurface0);
        builder.clearSurfaces();
        CaptureConfig captureConfig = builder.build();

        List<Surface> surfaces = DeferrableSurfaces.surfaceList(captureConfig.getSurfaces());
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

    @Test(expected = IllegalArgumentException.class)
    public void builderAddDuplicateCameraCaptureCallback_throwsException() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        CameraCaptureCallback callback0 = mock(CameraCaptureCallback.class);

        builder.addCameraCaptureCallback(callback0);
        builder.addCameraCaptureCallback(callback0);
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

    @Test(expected = UnsupportedOperationException.class)
    public void cameraCaptureCallbacks_areImmutable() {
        CaptureConfig.Builder builder = new CaptureConfig.Builder();
        CaptureConfig configuration = builder.build();

        configuration.getCameraCaptureCallbacks().add(mock(CameraCaptureCallback.class));
    }

    /**
     * A fake {@link MultiValueSet}.
     */
    static class FakeMultiValueSet extends MultiValueSet<Object> {
        @Override
        public MultiValueSet clone() {
            FakeMultiValueSet multiValueSet = new FakeMultiValueSet();
            multiValueSet.addAll(getAllItems());
            return multiValueSet;
        }
    }
}
