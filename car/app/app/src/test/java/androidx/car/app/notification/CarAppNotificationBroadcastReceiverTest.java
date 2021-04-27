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

package androidx.car.app.notification;

import static com.google.common.truth.Truth.assertThat;

import android.app.Application;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.car.app.CarAppService;
import androidx.car.app.CarContext;
import androidx.car.app.testing.TestCarContext;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link CarAppNotificationBroadcastReceiver}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarAppNotificationBroadcastReceiverTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final TestCarContext mTestCarContext = TestCarContext.createCarContext(mContext);

    private final Intent mIntent =
            new Intent("fooAction").setComponent(
                    new ComponentName(mContext, CarAppService.class));

    @Test
    public void onReceive() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = CarPendingIntent.getCarApp(mContext, 1,
                new Intent(mIntent),
                0);

        Bundle extras = new Bundle(1);
        extras.putBinder(
                CarContext.EXTRA_START_CAR_APP_BINDER_KEY,
                mTestCarContext.getStartCarAppStub().asBinder());
        Intent extraData = new Intent().putExtras(extras);
        pendingIntent.send(mContext, 0, extraData);

        Intent broadcastedIntent =
                Shadows.shadowOf((Application) mContext).getBroadcastIntents().get(0);

        CarAppNotificationBroadcastReceiver receiver = new CarAppNotificationBroadcastReceiver();

        receiver.onReceive(mContext, broadcastedIntent);

        Intent startedIntent = mTestCarContext.getStartCarAppIntents().get(0);
        assertThat(startedIntent.getComponent()).isEqualTo(mIntent.getComponent());
        assertThat(startedIntent.getAction()).isEqualTo(mIntent.getAction());
    }
}
