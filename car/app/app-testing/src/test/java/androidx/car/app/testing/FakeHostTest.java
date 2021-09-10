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

import static org.robolectric.Shadows.shadowOf;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;

import androidx.car.app.CarAppService;
import androidx.car.app.notification.CarPendingIntent;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link FakeHost}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class FakeHostTest {
    private final TestCarContext mCarContext =
            TestCarContext.createCarContext(ApplicationProvider.getApplicationContext());

    @Test
    public void performNotificationActionClick() {
        ComponentName componentName = new ComponentName(mCarContext,  CarAppService.class);
        Intent broadcast =
                new Intent("foo").setComponent(componentName);
        shadowOf(mCarContext.getPackageManager()).addServiceIfNotPresent(componentName);

        PendingIntent pendingIntent = CarPendingIntent.getCarApp(mCarContext, 1, broadcast, 0);

        mCarContext.getFakeHost().performNotificationActionClick(pendingIntent);

        assertThat(mCarContext.getStartCarAppIntents().get(0).getComponent())
                .isEqualTo(componentName);
        assertThat(mCarContext.getStartCarAppIntents().get(0).getAction()).isEqualTo("foo");
    }
}
