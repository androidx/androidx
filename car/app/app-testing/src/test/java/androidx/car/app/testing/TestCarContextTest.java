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

import android.app.PendingIntent;
import android.content.Intent;

import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.ScreenManager;
import androidx.car.app.navigation.NavigationManager;
import androidx.car.app.testing.navigation.TestNavigationManager;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link TestCarContext}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class TestCarContextTest {
    private final TestCarContext mCarContext =
            TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());

    @Test
    public void getCarService_appManager_returnsTestAppManager() {
        assertThat(mCarContext.getCarService(AppManager.class))
                .isSameInstanceAs(mCarContext.getCarService(TestAppManager.class));
    }

    @Test
    public void getCarService_navigationManager_returnsTestNavigationManager() {
        assertThat(mCarContext.getCarService(NavigationManager.class))
                .isSameInstanceAs(mCarContext.getCarService(TestNavigationManager.class));
    }

    @Test
    public void getCarService_screenManager_returnsTestAppManager() {
        assertThat(mCarContext.getCarService(ScreenManager.class))
                .isSameInstanceAs(mCarContext.getCarService(TestScreenManager.class));
    }

    @Test
    @SuppressWarnings("PendingIntentMutability")
    public void getStartCarAppIntents() {
        Intent startApp = new Intent(Intent.ACTION_VIEW);

        mCarContext.startCarApp(startApp);

        assertThat(mCarContext.getStartCarAppIntents()).containsExactly(startApp);

        Intent broadcast = new Intent("foo");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mCarContext, 1, broadcast, 0);

        mCarContext.getFakeHost().performNotificationActionClick(pendingIntent);

        Intent broadcastedIntent = Shadows.shadowOf(mCarContext).getBroadcastIntents().get(0);
        Intent startApp2 = new Intent(Intent.ACTION_SEND);
        CarContext.startCarApp(broadcastedIntent, startApp2);

        assertThat(mCarContext.getStartCarAppIntents()).containsExactly(startApp, startApp2);
    }

    @Test
    public void hasCalledFinishCarApp() {
        assertThat(mCarContext.hasCalledFinishCarApp()).isFalse();

        mCarContext.finishCarApp();

        assertThat(mCarContext.hasCalledFinishCarApp()).isTrue();
    }
}
