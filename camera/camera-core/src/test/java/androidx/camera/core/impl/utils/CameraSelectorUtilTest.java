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

package androidx.camera.core.impl.utils;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.impl.CameraDeviceConfig;
import androidx.camera.core.impl.CameraIdFilter;
import androidx.camera.testing.fakes.FakeAppConfig;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CameraSelectorUtilTest {

    private static final Set<String> SINGLE_ID_SET = Collections.singleton("0");

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        CameraXConfig cameraXConfig = FakeAppConfig.create();
        CameraX.initialize(context, cameraXConfig).get();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException {
        CameraX.shutdown().get();
    }

    @Test
    public void convertedCameraDeviceConfig_hasFrontLensFacing() {
        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(
                        CameraSelector.LENS_FACING_FRONT).build();
        CameraDeviceConfig convertedConfig =
                CameraSelectorUtil.toCameraDeviceConfig(cameraSelector);

        assertThat(convertedConfig.getLensFacing()).isEqualTo(CameraSelector.LENS_FACING_FRONT);
    }

    @Test
    public void convertedCameraDeviceConfig_hasBackLensFacing() {
        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(
                        CameraSelector.LENS_FACING_BACK).build();
        CameraDeviceConfig convertedConfig =
                CameraSelectorUtil.toCameraDeviceConfig(cameraSelector);

        assertThat(convertedConfig.getLensFacing()).isEqualTo(CameraSelector.LENS_FACING_BACK);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertedCameraDeviceConfig_doesNotContainFilterForEmptySelector() {
        CameraSelector cameraSelector = new CameraSelector.Builder().build();
        CameraDeviceConfig convertedConfig =
                CameraSelectorUtil.toCameraDeviceConfig(cameraSelector);

        convertedConfig.getCameraIdFilter();
    }

    public void convertedCameraDeviceConfig_containsAllFilters() {
        CameraIdFilter filter0 = createPassThroughMockFilter();
        CameraIdFilter filter1 = createPassThroughMockFilter();
        CameraIdFilter filter2 = createPassThroughMockFilter();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .appendFilter(filter0)
                .appendFilter(filter1)
                .appendFilter(filter2)
                .build();

        CameraDeviceConfig convertedConfig =
                CameraSelectorUtil.toCameraDeviceConfig(cameraSelector);

        CameraIdFilter combinedFilter = convertedConfig.getCameraIdFilter();

        combinedFilter.filter(SINGLE_ID_SET);

        verify(filter0, atLeastOnce()).filter(any());
        verify(filter1, atLeastOnce()).filter(any());
        verify(filter2, atLeastOnce()).filter(any());
    }

    private CameraIdFilter createPassThroughMockFilter() {
        CameraIdFilter mockFilter = mock(CameraIdFilter.class);
        when(mockFilter.filter(any())).then(i -> i.getArguments()[0]);
        return mockFilter;
    }
}
