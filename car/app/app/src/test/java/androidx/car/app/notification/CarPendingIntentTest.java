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

import static android.content.pm.PackageManager.FEATURE_AUTOMOTIVE;

import static androidx.car.app.notification.CarPendingIntent.COMPONENT_EXTRA_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.car.app.CarAppService;
import androidx.car.app.CarContext;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.security.InvalidParameterException;

/** Tests for {@link CarPendingIntent}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarPendingIntentTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final ComponentName mComponentName = new ComponentName(mContext, CarAppService.class);
    private final Intent mIntent = new Intent("fooAction").setComponent(mComponentName);

    @Test
    public void getCarApp_returnsTheExpectedPendingIntent() throws PendingIntent.CanceledException {
        PendingIntent pendingIntent = CarPendingIntent.getCarApp(mContext, 1, mIntent,
                PendingIntent.FLAG_ONE_SHOT);

        pendingIntent.send();

        assertThat(Shadows.shadowOf((Application) mContext).getNextStartedActivity()).isNull();

        Intent broadcastedIntent =
                Shadows.shadowOf((Application) mContext).getBroadcastIntents().get(0);
        assertThat(broadcastedIntent.getComponent()).isEqualTo(
                new ComponentName(mContext, CarAppNotificationBroadcastReceiver.class));
        assertThat((ComponentName) broadcastedIntent.getParcelableExtra(
                COMPONENT_EXTRA_KEY)).isEqualTo(mComponentName);
    }

    @Test
    public void getCarApp_embedded_returnsTheExpectedPendingIntent()
            throws PendingIntent.CanceledException {
        setEmbedded();
        PendingIntent pendingIntent = CarPendingIntent.getCarApp(mContext, 1, mIntent,
                PendingIntent.FLAG_ONE_SHOT);

        pendingIntent.send();

        assertThat(Shadows.shadowOf((Application) mContext).getBroadcastIntents()).isEmpty();

        Intent startedActivity = Shadows.shadowOf((Application) mContext).getNextStartedActivity();
        assertThat(startedActivity.getComponent()).isEqualTo(
                new ComponentName(mContext, CarPendingIntent.CAR_APP_ACTIVITY_CLASSNAME));
    }

    @Test
    public void validateIntent_self() {
        CarPendingIntent.validateIntent(mContext, mIntent);
    }

    @Test
    public void validateIntent_navigation_query() {
        CarPendingIntent.validateIntent(mContext,
                new Intent(CarContext.ACTION_NAVIGATE).setData(Uri.parse("geo:0,0?q=Home")));
    }

    @Test
    public void validateIntent_navigation_latLong() {
        CarPendingIntent.validateIntent(mContext,
                new Intent(CarContext.ACTION_NAVIGATE).setData(Uri.parse("geo:123.45,234.98")));
    }

    @Test
    public void validateIntent_navigation_latLong_query() {
        CarPendingIntent.validateIntent(mContext,
                new Intent(CarContext.ACTION_NAVIGATE).setData(Uri.parse("geo:123.45,234"
                        + ".98?q=Starbucks")));
    }

    @Test
    public void validateIntent_navigation_invalidGeo() {
        assertThrows(InvalidParameterException.class,
                () -> CarPendingIntent.validateIntent(mContext,
                        new Intent(CarContext.ACTION_NAVIGATE).setData(Uri.parse("geo:0,r"))));
    }

    @Test
    public void validateIntent_phone() {
        CarPendingIntent.validateIntent(mContext,
                new Intent(Intent.ACTION_DIAL).setData(Uri.parse("tel:+1234567")));

        CarPendingIntent.validateIntent(mContext,
                new Intent(Intent.ACTION_CALL).setData(Uri.parse("tel:+1234567")));
    }

    @Test
    public void validateIntent_phone_invalidUri() {
        assertThrows(InvalidParameterException.class,
                () -> CarPendingIntent.validateIntent(mContext,
                        new Intent(Intent.ACTION_DIAL).setData(Uri.parse("telrf"))));

        assertThrows(InvalidParameterException.class,
                () -> CarPendingIntent.validateIntent(mContext,
                        new Intent(Intent.ACTION_CALL).setData(Uri.parse("telr"))));
    }

    @Test
    public void validateIntent_notValid() {
        assertThrows(SecurityException.class,
                () -> CarPendingIntent.validateIntent(mContext,
                        new Intent(Intent.ACTION_VIEW).setComponent(
                                new ComponentName("foo", "bar"))));
    }

    @Test
    public void getCarApp_intentMissingComponent_throwsExpectedException() {
        assertThrows(InvalidParameterException.class, () -> CarPendingIntent.getCarApp(
                mContext, 1, mIntent.setComponent(null), 0));
    }

    @Test
    public void getCarApp_intentComponentHasWrongPackage_throwsExpectedException() {
        assertThrows(SecurityException.class, () -> CarPendingIntent.getCarApp(mContext,
                1, mIntent.setComponent(new ComponentName("foo", "bar")), 0));
    }

    private void setEmbedded() {
        shadowOf(mContext.getPackageManager()).setSystemFeature(FEATURE_AUTOMOTIVE, true);
    }
}
