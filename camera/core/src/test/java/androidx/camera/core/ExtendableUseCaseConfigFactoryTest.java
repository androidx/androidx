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

import android.os.Build;

import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ExtendableUseCaseConfigFactoryTest {

    private ExtendableUseCaseConfigFactory mFactory;

    @Before
    public void setUp() {
        mFactory = new ExtendableUseCaseConfigFactory();
    }

    @Test
    public void canInstallProvider_andRetrieveConfig() {
        mFactory.installDefaultProvider(FakeUseCaseConfig.class, new FakeUseCaseConfigProvider());

        FakeUseCaseConfig config = mFactory.getConfig(FakeUseCaseConfig.class, null);
        assertThat(config).isNotNull();
        assertThat(config.getTargetClass(null)).isEqualTo(FakeUseCase.class);
    }

    private static class FakeUseCaseConfigProvider implements ConfigProvider<FakeUseCaseConfig> {

        @Override
        public FakeUseCaseConfig getConfig(CameraX.LensFacing lensFacing) {
            return new FakeUseCaseConfig.Builder().build();
        }
    }
}
