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

package androidx.car.app.sample.helloworld.common;

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;
import android.content.Intent;

import androidx.car.app.Screen;
import androidx.car.app.Session;
import androidx.car.app.testing.SessionController;
import androidx.car.app.testing.TestCarContext;
import androidx.car.app.testing.TestScreenManager;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/**
 * A sample test on the session instance from {@link HelloWorldService}.
 *
 * <p>Demonstrating the usage of {@link SessionController} and validating that the session is
 * pushing the expected screen when created.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class HelloWorldSessionTest {
    private final TestCarContext mTestCarContext =
            TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());

    @Test
    public void onCreateScreen_returnsExpectedScreen() {
        HelloWorldService service = Robolectric.setupService(HelloWorldService.class);
        Session session = service.onCreateSession();
        SessionController controller = SessionController.of(session, mTestCarContext);

        controller.create(new Intent().setComponent(
                new ComponentName(mTestCarContext, HelloWorldService.class)));
        Screen screenCreated =
                mTestCarContext.getCarService(TestScreenManager.class).getScreensPushed().get(0);
        assertThat(screenCreated).isInstanceOf(HelloWorldScreen.class);
    }

}
