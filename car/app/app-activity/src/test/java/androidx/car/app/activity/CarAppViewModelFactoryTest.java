/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.activity;

import static com.google.common.truth.Truth.assertThat;

import android.app.Application;
import android.content.ComponentName;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link CarAppViewModelFactory} */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarAppViewModelFactoryTest {
    private static final ComponentName TEST_COMPONENT_NAME_1 = new ComponentName(
            ApplicationProvider.getApplicationContext(), "Class1");
    private static final ComponentName TEST_COMPONENT_NAME_2 = new ComponentName(
            ApplicationProvider.getApplicationContext(), "Class2");

    private final Application mApplication = ApplicationProvider.getApplicationContext();

    @Test
    public void getInstance_sameKey_returnsSame() {
        CarAppViewModelFactory factory1 = CarAppViewModelFactory.getInstance(mApplication,
                TEST_COMPONENT_NAME_1);

        CarAppViewModelFactory factory2 = CarAppViewModelFactory.getInstance(mApplication,
                TEST_COMPONENT_NAME_1);

        assertThat(factory1).isEqualTo(factory2);
    }

    @Test
    public void getInstance_differentKeys_returnsDifferent() {
        CarAppViewModelFactory factory1 = CarAppViewModelFactory.getInstance(mApplication,
                TEST_COMPONENT_NAME_1);

        CarAppViewModelFactory factory2 = CarAppViewModelFactory.getInstance(mApplication,
                TEST_COMPONENT_NAME_2);

        assertThat(factory1).isNotEqualTo(factory2);
    }

    @Test
    public void create_correctComponentName() {
        CarAppViewModelFactory factory = CarAppViewModelFactory.getInstance(mApplication,
                TEST_COMPONENT_NAME_1);

        CarAppViewModel viewModel = factory.create(CarAppViewModel.class);

        assertThat(viewModel).isNotNull();
        assertThat(viewModel.getServiceConnectionManager().getServiceComponentName())
                .isEqualTo(TEST_COMPONENT_NAME_1);
    }
}
