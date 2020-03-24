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

package androidx.camera.testing.fakes;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.os.Build;
import android.util.Size;

import androidx.camera.core.impl.UseCaseConfig;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class FakeCameraDeviceSurfaceManagerTest {

    private static final int FAKE_WIDTH0 = 400;
    private static final int FAKE_HEIGHT0 = 300;

    private static final int FAKE_WIDTH1 = 800;
    private static final int FAKE_HEIGHT1 = 600;

    private static final String FAKE_CAMERA_ID0 = "0";
    private static final String FAKE_CAMERA_ID1 = "1";

    private FakeCameraDeviceSurfaceManager mFakeCameraDeviceSurfaceManager;

    private FakeUseCaseConfig mFakeUseCaseConfig;

    private List<UseCaseConfig<?>> mUseCaseConfigList;

    @Before
    public void setUp() {
        mFakeCameraDeviceSurfaceManager = new FakeCameraDeviceSurfaceManager();
        mFakeUseCaseConfig = mock(FakeUseCaseConfig.class);

        mFakeCameraDeviceSurfaceManager.setSuggestedResolution(FAKE_CAMERA_ID0,
                mFakeUseCaseConfig.getClass(), new Size(FAKE_WIDTH0, FAKE_HEIGHT0));
        mFakeCameraDeviceSurfaceManager.setSuggestedResolution(FAKE_CAMERA_ID1,
                mFakeUseCaseConfig.getClass(), new Size(FAKE_WIDTH1, FAKE_HEIGHT1));

        mUseCaseConfigList = Collections.singletonList((UseCaseConfig<?>) mFakeUseCaseConfig);
    }

    @Test
    public void canRetrieveInsertedSuggestedResolutions() {
        Map<UseCaseConfig<?>, Size> suggestedSizesCamera0 =
                mFakeCameraDeviceSurfaceManager.getSuggestedResolutions(FAKE_CAMERA_ID0,
                        Collections.emptyList(), mUseCaseConfigList);
        Map<UseCaseConfig<?>, Size> suggestedSizesCamera1 =
                mFakeCameraDeviceSurfaceManager.getSuggestedResolutions(FAKE_CAMERA_ID1,
                        Collections.emptyList(), mUseCaseConfigList);

        assertThat(suggestedSizesCamera0.get(mFakeUseCaseConfig)).isEqualTo(
                new Size(FAKE_WIDTH0, FAKE_HEIGHT0));
        assertThat(suggestedSizesCamera1.get(mFakeUseCaseConfig)).isEqualTo(
                new Size(FAKE_WIDTH1, FAKE_HEIGHT1));

    }

}
