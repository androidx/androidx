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

import android.graphics.Rect;
import android.util.Pair;

import androidx.car.app.AppManager;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import androidx.car.app.model.Template;
import androidx.test.core.app.ApplicationProvider;

import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link TestAppManager}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class TestAppManagerTest {

    private TestCarContext mCarContext;

    private final Template mTemplate = new Template() {};

    private Screen mTestScreen;

    @Before
    public void setup() {
        mCarContext = TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());

        mTestScreen =
                new Screen(mCarContext) {
                    @Override
                    public @NonNull Template onGetTemplate() {
                        return mTemplate;
                    }
                };
    }

    @Test
    public void getSurfaceCallbacks() {
        SurfaceCallback callback1 = new TestSurfaceCallback();
        SurfaceCallback callback2 = new TestSurfaceCallback();
        SurfaceCallback callback3 = new TestSurfaceCallback();

        mCarContext.getCarService(AppManager.class).setSurfaceCallback(callback1);

        assertThat(mCarContext.getCarService(TestAppManager.class).getSurfaceCallback())
                .isEqualTo(callback1);

        mCarContext.getCarService(AppManager.class).setSurfaceCallback(callback2);
        mCarContext.getCarService(AppManager.class).setSurfaceCallback(callback3);

        assertThat(mCarContext.getCarService(TestAppManager.class).getSurfaceCallback())
                .isEqualTo(callback3);
    }

    @Test
    public void getToastsShown() {
        String toast1 = "foo";
        String toast2 = "bar";
        String toast3 = "baz";
        mCarContext.getCarService(AppManager.class).showToast(toast1, CarToast.LENGTH_LONG);

        assertThat(mCarContext.getCarService(TestAppManager.class).getToastsShown())
                .containsExactly(toast1);

        mCarContext.getCarService(AppManager.class).showToast(toast2, CarToast.LENGTH_LONG);
        mCarContext.getCarService(AppManager.class).showToast(toast3, CarToast.LENGTH_LONG);

        assertThat(mCarContext.getCarService(TestAppManager.class).getToastsShown())
                .containsExactly(toast1, toast2, toast3);
    }

    @Test
    public void getTemplatesReturned() {
        mCarContext.getCarService(ScreenManager.class).push(mTestScreen);
        mCarContext.getCarService(TestAppManager.class).reset();

        mCarContext.getCarService(AppManager.class).invalidate();

        assertThat(mCarContext.getCarService(TestAppManager.class).getTemplatesReturned())
                .containsExactly(Pair.create(mTestScreen, mTemplate));

        mCarContext.getCarService(AppManager.class).invalidate();

        Screen screen2 =
                new Screen(mCarContext) {
                    @Override
                    public @NonNull Template onGetTemplate() {
                        return mTemplate;
                    }
                };

        mCarContext.getCarService(ScreenManager.class).push(screen2);
        mCarContext.getCarService(AppManager.class).invalidate();

        assertThat(mCarContext.getCarService(TestAppManager.class).getTemplatesReturned())
                .containsExactly(
                        Pair.create(mTestScreen, mTemplate),
                        Pair.create(mTestScreen, mTemplate),
                        Pair.create(screen2, mTemplate));
    }

    @Test
    public void resetTemplatesStoredForScreen() {
        mCarContext.getCarService(ScreenManager.class).push(mTestScreen);
        mCarContext.getCarService(TestAppManager.class).reset();

        mCarContext.getCarService(AppManager.class).invalidate();

        assertThat(mCarContext.getCarService(TestAppManager.class).getTemplatesReturned())
                .containsExactly(Pair.create(mTestScreen, mTemplate));

        mCarContext.getCarService(AppManager.class).invalidate();

        Screen screen2 =
                new Screen(mCarContext) {
                    @Override
                    public @NonNull Template onGetTemplate() {
                        return mTemplate;
                    }
                };

        mCarContext.getCarService(ScreenManager.class).push(screen2);
        mCarContext.getCarService(AppManager.class).invalidate();

        assertThat(mCarContext.getCarService(TestAppManager.class).getTemplatesReturned())
                .containsExactly(
                        Pair.create(mTestScreen, mTemplate),
                        Pair.create(mTestScreen, mTemplate),
                        Pair.create(screen2, mTemplate));

        mCarContext.getCarService(TestAppManager.class).resetTemplatesStoredForScreen(mTestScreen);
        assertThat(mCarContext.getCarService(TestAppManager.class).getTemplatesReturned())
                .containsExactly(Pair.create(screen2, mTemplate));
    }

    private static class TestSurfaceCallback implements SurfaceCallback {
        @Override
        public void onSurfaceAvailable(@NonNull SurfaceContainer surfaceContainer) {}

        @Override
        public void onVisibleAreaChanged(@NonNull Rect visibleArea) {}

        @Override
        public void onStableAreaChanged(@NonNull Rect stableArea) {}

        @Override
        public void onSurfaceDestroyed(@NonNull SurfaceContainer surfaceContainer) {}
    }
}

