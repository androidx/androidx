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

package androidx.car.app.testing;

import static com.google.common.truth.Truth.assertThat;

import androidx.annotation.NonNull;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.model.Template;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link TestScreenManager}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class TestScreenManagerTest {
    private TestCarContext mCarContext;

    @Before
    public void setup() {
        mCarContext = TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void push_getScreensPushed() {
        Screen screen1 = new TestScreen();

        mCarContext.getCarService(ScreenManager.class).push(screen1);
        assertThat(mCarContext.getCarService(TestScreenManager.class).getScreensPushed())
                .containsExactly(screen1);
    }

    @Test
    public void pushForResult_getScreensPushed() {
        Screen screen1 = new TestScreen();
        Screen screen2 = new TestScreen();

        mCarContext.getCarService(ScreenManager.class).push(screen1);
        assertThat(mCarContext.getCarService(TestScreenManager.class).getScreensPushed())
                .containsExactly(screen1);

        mCarContext.getCarService(ScreenManager.class).pushForResult(screen2, result -> {});
        assertThat(mCarContext.getCarService(TestScreenManager.class).getScreensPushed())
                .containsExactly(screen1, screen2);
    }

    @Ignore("b/218342974")
    @Test
    public void pop_getScreensRemoved() {
        Screen screen1 = new TestScreen();
        Screen screen2 = new TestScreen();
        Screen screen3 = new TestScreen();
        Screen screen4 = new TestScreen();

        mCarContext.getCarService(ScreenManager.class).push(screen1);
        mCarContext.getCarService(ScreenManager.class).push(screen2);
        mCarContext.getCarService(ScreenManager.class).push(screen3);
        mCarContext.getCarService(ScreenManager.class).push(screen4);

        mCarContext.getCarService(ScreenManager.class).pop();
        assertThat(mCarContext.getCarService(TestScreenManager.class).getScreensRemoved())
                .containsExactly(screen4);
    }

    @Ignore("b/218342974")
    @Test
    public void remove_getScreensRemoved() {
        Screen screen1 = new TestScreen();
        Screen screen2 = new TestScreen();
        Screen screen3 = new TestScreen();
        Screen screen4 = new TestScreen();

        mCarContext.getCarService(ScreenManager.class).push(screen1);
        mCarContext.getCarService(ScreenManager.class).push(screen2);
        mCarContext.getCarService(ScreenManager.class).push(screen3);
        mCarContext.getCarService(ScreenManager.class).push(screen4);

        mCarContext.getCarService(ScreenManager.class).remove(screen2);
        assertThat(mCarContext.getCarService(TestScreenManager.class).getScreensRemoved())
                .containsExactly(screen2);
    }

    @Ignore("b/218342974")
    @Test
    public void popTo_getScreensRemoved() {
        Screen screen1 = new TestScreen();
        Screen screen2 = new TestScreen();
        Screen screen3 = new TestScreen();
        Screen screen4 = new TestScreen();

        mCarContext.getCarService(ScreenManager.class).push(screen1);
        mCarContext.getCarService(ScreenManager.class).push(screen2);
        mCarContext.getCarService(ScreenManager.class).push(screen3);
        mCarContext.getCarService(ScreenManager.class).push(screen4);

        String marker = "foo";
        screen2.setMarker(marker);

        mCarContext.getCarService(ScreenManager.class).popTo(marker);
        assertThat(mCarContext.getCarService(TestScreenManager.class).getScreensRemoved())
                .containsExactly(screen4, screen3);
    }

    @Ignore("b/218342974")
    @Test
    public void popToRoot_getScreensRemoved() {
        Screen screen1 = new TestScreen();
        Screen screen2 = new TestScreen();
        Screen screen3 = new TestScreen();
        Screen screen4 = new TestScreen();

        mCarContext.getCarService(ScreenManager.class).push(screen1);
        mCarContext.getCarService(ScreenManager.class).push(screen2);
        mCarContext.getCarService(ScreenManager.class).push(screen3);
        mCarContext.getCarService(ScreenManager.class).push(screen4);

        mCarContext.getCarService(ScreenManager.class).popToRoot();
        assertThat(mCarContext.getCarService(TestScreenManager.class).getScreensRemoved())
                .containsExactly(screen4, screen3, screen2);
    }

    private static class TestScreen extends Screen {
        private TestScreen() {
            super(TestCarContext.createCarContext(ApplicationProvider.getApplicationContext()));
        }

        @NonNull
        @Override
        public Template onGetTemplate() {
            return new Template() {};
        }
    }
}
