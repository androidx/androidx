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

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.car.app.R;
import androidx.car.app.TestUtils;
import androidx.car.app.model.CarColor;
import androidx.core.app.NotificationCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link CarNotificationManager}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarNotificationManagerTest {
    private static final String DEFAULT_TITLE = "title";
    private static final String EXTENDED_TITLE = "carTitle";

    private static final String DEFAULT_TEXT = "text";
    private static final String EXTENDED_TEXT = "carText";

    private static final String DEFAULT_ACTION_TITLE = "phone";
    private static final String EXTENDED_ACTION_TITLE = "car";

    private static final String DEFAULT_CHANNEL = "channel1";
    private static final String EXTENDED_CHANNEL = "channel2";

    @ColorInt
    private static final int DEFAULT_COLOR = Color.GREEN;
    private static final CarColor EXTENDED_COLOR = CarColor.BLUE;

    private final Context mContext =
            ApplicationProvider.getApplicationContext();
    private final CarNotificationManager mCarNotificationManager = CarNotificationManager.from(
            mContext);

    private final PendingIntent mDefaultPendingIntent = PendingIntent.getBroadcast(mContext, 1,
            new Intent("foo"), PendingIntent.FLAG_IMMUTABLE);
    private final PendingIntent mExtendedPendingIntent = PendingIntent.getBroadcast(mContext, 1,
            new Intent("bar"), PendingIntent.FLAG_IMMUTABLE);

    private int mDefaultIcon;
    private int mExtendedIcon;

    private Bitmap mDefaultBitmap;
    private Bitmap mExtendedBitmap;

    private NotificationCompat.Action mDefaultAction;

    @Before
    public void setup() {
        mDefaultIcon = TestUtils.getTestDrawableResId(mContext, "ic_test_1");
        mExtendedIcon = TestUtils.getTestDrawableResId(mContext, "ic_test_2");

        mDefaultBitmap = BitmapFactory.decodeResource(mContext.getResources(), mDefaultIcon);
        mExtendedBitmap = BitmapFactory.decodeResource(mContext.getResources(), mExtendedIcon);

        mDefaultAction = new NotificationCompat.Action(mDefaultIcon, DEFAULT_ACTION_TITLE,
                mDefaultPendingIntent);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void updateForCar_embedded_overridesAsExpected() {
        setEmbedded();

        Notification changed = CarNotificationManager.from(mContext).updateForCar(
                extend(createNotification()));

        assertThat(changed.extras.getCharSequence(Notification.EXTRA_TITLE)).isEqualTo(
                EXTENDED_TITLE);
        assertThat(changed.extras.getCharSequence(Notification.EXTRA_TEXT)).isEqualTo(
                EXTENDED_TEXT);
        assertThat(changed.contentIntent).isSameInstanceAs(mExtendedPendingIntent);
        assertThat(changed.deleteIntent).isSameInstanceAs(mExtendedPendingIntent);
        assertThat(changed.color).isEqualTo(mContext.getColor(R.color.carColorBlue));
        assertThat(changed.getChannelId()).isEqualTo(EXTENDED_CHANNEL);
        assertThat(changed.icon).isEqualTo(mExtendedIcon);
        // Comparing the largeIcon does not work due to internally it creating another Bitmap and
        // then not being able to compare

        Notification.Action[] actions = changed.actions;
        assertThat(actions).hasLength(1);
        Notification.Action action = actions[0];
        assertThat(action.icon).isEqualTo(mExtendedIcon);
        assertThat(action.actionIntent).isSameInstanceAs(mExtendedPendingIntent);
        assertThat(action.title).isEqualTo(EXTENDED_ACTION_TITLE);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void updateForCar_notEmbedded_returnsTheSame() {
        Notification output = mCarNotificationManager.updateForCar(extend(createNotification()));

        assertThat(output.extras.getCharSequence(Notification.EXTRA_TITLE)).isEqualTo(
                DEFAULT_TITLE);
        assertThat(output.extras.getCharSequence(Notification.EXTRA_TEXT)).isEqualTo(
                DEFAULT_TEXT);
        assertThat(output.contentIntent).isSameInstanceAs(mDefaultPendingIntent);
        assertThat(output.deleteIntent).isSameInstanceAs(mDefaultPendingIntent);
        assertThat(output.color).isEqualTo(Color.GREEN);
        assertThat(output.getChannelId()).isEqualTo(DEFAULT_CHANNEL);
        assertThat(output.icon).isEqualTo(mDefaultIcon);
        // Comparing the largeIcon does not work due to internally it creating another Bitmap and
        // then not being able to compare

        Notification.Action[] actions = output.actions;
        assertThat(actions).hasLength(1);
        Notification.Action action = actions[0];
        assertThat(action.icon).isEqualTo(mDefaultIcon);
        assertThat(action.actionIntent).isSameInstanceAs(mDefaultPendingIntent);
        assertThat(action.title).isEqualTo(DEFAULT_ACTION_TITLE);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void updateForCar_notExtended_marksAsExtended()
            throws NoSuchFieldException {
        Notification output = mCarNotificationManager.updateForCar(createNotification());

        assertThat(CarAppExtender.isExtended(output)).isTrue();

        assertThat(output.extras.getCharSequence(Notification.EXTRA_TITLE)).isEqualTo(
                DEFAULT_TITLE);
        assertThat(output.extras.getCharSequence(Notification.EXTRA_TEXT)).isEqualTo(
                DEFAULT_TEXT);
        assertThat(output.contentIntent).isSameInstanceAs(mDefaultPendingIntent);
        assertThat(output.deleteIntent).isSameInstanceAs(mDefaultPendingIntent);
        assertThat(output.color).isEqualTo(Color.GREEN);
        assertThat(output.getChannelId()).isEqualTo(DEFAULT_CHANNEL);
        assertThat(output.icon).isEqualTo(mDefaultIcon);
        // Comparing the largeIcon does not work due to internally it creating another Bitmap and
        // then not being able to compare

        Notification.Action[] actions = output.actions;
        assertThat(actions).hasLength(1);
        Notification.Action action = actions[0];
        assertThat(action.icon).isEqualTo(mDefaultIcon);
        assertThat(action.actionIntent).isSameInstanceAs(mDefaultPendingIntent);
        assertThat(action.title).isEqualTo(DEFAULT_ACTION_TITLE);
    }

    @Test
    public void getColorInt_red() {
        assertThat(mCarNotificationManager.getColorInt(CarColor.RED)).isEqualTo(
                mContext.getColor(R.color.carColorRed));
    }

    @Test
    public void getColorInt_red_dark() {
        setDarkMode();
        assertThat(mCarNotificationManager.getColorInt(CarColor.RED)).isEqualTo(
                mContext.getColor(R.color.carColorRed));
    }

    @Test
    public void getColorInt_blue() {
        assertThat(mCarNotificationManager.getColorInt(CarColor.BLUE)).isEqualTo(
                mContext.getColor(R.color.carColorBlue));
    }

    @Test
    public void getColorInt_blue_dark() {
        setDarkMode();
        assertThat(mCarNotificationManager.getColorInt(CarColor.BLUE)).isEqualTo(
                mContext.getColor(R.color.carColorBlue));
    }

    @Test
    public void getColorInt_green() {
        assertThat(mCarNotificationManager.getColorInt(CarColor.GREEN)).isEqualTo(
                mContext.getColor(R.color.carColorGreen));
    }

    @Test
    public void getColorInt_green_dark() {
        setDarkMode();
        assertThat(mCarNotificationManager.getColorInt(CarColor.GREEN)).isEqualTo(
                mContext.getColor(R.color.carColorGreen));
    }

    @Test
    public void getColorInt_yellow() {
        assertThat(mCarNotificationManager.getColorInt(CarColor.YELLOW)).isEqualTo(
                mContext.getColor(R.color.carColorYellow));
    }

    @Test
    public void getColorInt_yellow_dark() {
        setDarkMode();
        assertThat(mCarNotificationManager.getColorInt(CarColor.YELLOW)).isEqualTo(
                mContext.getColor(R.color.carColorYellow));
    }

    @Test
    public void getColorInt_custom() {
        CarColor carColor = CarColor.createCustom(1, 2);
        assertThat(mCarNotificationManager.getColorInt(carColor)).isEqualTo(1);
    }

    @Test
    public void getColorInt_custom_dark() {
        setDarkMode();

        CarColor carColor = CarColor.createCustom(1, 2);
        assertThat(mCarNotificationManager.getColorInt(carColor)).isEqualTo(2);
    }

    @Test
    public void getColorInt_primary() {
        assertThat(mCarNotificationManager.getColorInt(CarColor.PRIMARY)).isEqualTo(
                Color.rgb(127, 57, 251));
        assertThat(mCarNotificationManager.getColorInt(CarColor.PRIMARY)).isNotEqualTo(0);
    }

    @Test
    public void getColorInt_primary_dark() {
        setDarkMode();

        assertThat(mCarNotificationManager.getColorInt(CarColor.PRIMARY)).isEqualTo(
                Color.rgb(89, 4, 223));
    }

    @Test
    public void getColorInt_secondary() {
        assertThat(mCarNotificationManager.getColorInt(CarColor.SECONDARY)).isEqualTo(
                Color.rgb(50, 142, 16));
    }

    @Test
    public void getColorInt_secondary_dark() {
        setDarkMode();

        assertThat(mCarNotificationManager.getColorInt(CarColor.SECONDARY)).isEqualTo(
                Color.rgb(26, 96, 4));
    }

    private NotificationCompat.Builder createNotification() {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(mContext,
                DEFAULT_CHANNEL);
        notification.setColor(DEFAULT_COLOR);
        notification.setSmallIcon(mDefaultIcon);
        notification.setLargeIcon(mDefaultBitmap);
        notification.setContentText(DEFAULT_TEXT);
        notification.setContentTitle(DEFAULT_TITLE);
        notification.setContentIntent(mDefaultPendingIntent);
        notification.setDeleteIntent(mDefaultPendingIntent);
        notification.addAction(mDefaultAction);

        return notification;
    }

    private NotificationCompat.Builder extend(NotificationCompat.Builder notification) {
        CarAppExtender.Builder extender = new CarAppExtender.Builder();
        extender.setColor(EXTENDED_COLOR);
        extender.setContentIntent(mExtendedPendingIntent);
        extender.setDeleteIntent(mExtendedPendingIntent);
        extender.setChannelId(EXTENDED_CHANNEL);
        extender.setContentText(EXTENDED_TEXT);
        extender.setContentTitle(EXTENDED_TITLE);
        extender.setLargeIcon(mExtendedBitmap);
        extender.setSmallIcon(mExtendedIcon);
        extender.addAction(mExtendedIcon, EXTENDED_ACTION_TITLE, mExtendedPendingIntent);

        notification.extend(extender.build());

        return notification;
    }

    private void setDarkMode() {
        mContext.getResources().getConfiguration().uiMode = Configuration.UI_MODE_NIGHT_YES;
    }

    private void setEmbedded() {
        shadowOf(mContext.getPackageManager()).setSystemFeature(FEATURE_AUTOMOTIVE, true);
    }
}
