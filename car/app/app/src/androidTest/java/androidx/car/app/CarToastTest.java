/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.testing.TestAppManager;
import androidx.car.app.testing.TestCarContext;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link CarToast}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class CarToastTest {
    private TestCarContext mCarContext;

    @Before
    @UiThreadTest
    public void setUp() {
        mCarContext = TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());
        mCarContext.reset();
    }

    @Test
    public void makeText_setsTextAndDuration() {
        CharSequence text = "Toast makeText!";
        int duration = CarToast.LENGTH_LONG;
        CarToast.makeText(mCarContext, text, duration).show();

        assertThat(mCarContext.getCarService(TestAppManager.class).getToastsShown())
                .containsExactly(text);
    }

    @Test
    public void setText_setsText() {
        CharSequence text = "Toast setText!";
        CarToast carToast = new CarToast(mCarContext);
        carToast.setText(text);
        carToast.show();

        assertThat(mCarContext.getCarService(TestAppManager.class).getToastsShown())
                .containsExactly(text);
    }

    @Test
    public void setDuration_setsDuration() {
        int duration = CarToast.LENGTH_LONG;
        CharSequence text = "Toast with duration!";
        CarToast carToast = new CarToast(mCarContext);
        carToast.setText(text);
        carToast.setDuration(duration);
        carToast.show();

        assertThat(mCarContext.getCarService(TestAppManager.class).getToastsShown())
                .containsExactly(text);
    }

    @Test
    public void textNotSet_throwsRuntimeException() {
        CarToast carToast = new CarToast(mCarContext);

        assertThrows(RuntimeException.class, carToast::show);
    }
}
