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

package androidx.car.app.notification;

import static com.google.common.truth.Truth.assertThat;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.car.app.TestUtils;
import androidx.car.app.model.CarColor;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.List;

/** Tests for {@link CarAppExtender}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public final class CarAppExtenderTest {
    private static final String NOTIFICATION_CHANNEL_ID = "test carextender channel id";
    private static final String INTENT_PRIMARY_ACTION =
            "androidx.car.app.INTENT_PRIMARY_ACTION";
    private static final String INTENT_SECONDARY_ACTION =
            "androidx.car.app.INTENT_SECONDARY_ACTION";
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void carAppExtender_checkDefaultValues() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                        .extend(
                                // Simulate sending a notification that has the same bundle key
                                // but no value is set.
                                new NotificationCompat.Extender() {
                                    @NonNull
                                    @Override
                                    public NotificationCompat.Builder extend(
                                            @NonNull NotificationCompat.Builder builder) {
                                        Bundle carExtensions = new Bundle();

                                        builder.getExtras().putBundle("androidx.car.app.EXTENSIONS",
                                                carExtensions);
                                        return builder;
                                    }
                                });

        CarAppExtender carAppExtender = new CarAppExtender(builder.build());
        assertThat(carAppExtender.getContentTitle()).isNull();
        assertThat(carAppExtender.getContentText()).isNull();
        assertThat(carAppExtender.getSmallIcon()).isEqualTo(0);
        assertThat(carAppExtender.getLargeIcon()).isNull();
        assertThat(carAppExtender.getContentIntent()).isNull();
        assertThat(carAppExtender.getDeleteIntent()).isNull();
        assertThat(carAppExtender.getActions()).isEmpty();
        assertThat(carAppExtender.getImportance())
                .isEqualTo(NotificationManagerCompat.IMPORTANCE_UNSPECIFIED);
        assertThat(carAppExtender.getChannelId()).isNull();
    }

    @Test
    public void notification_extended() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                        .extend(new CarAppExtender.Builder().build());

        assertThat(CarAppExtender.isExtended(builder.build())).isTrue();
    }

    @Test
    public void notification_notExtended() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID);

        assertThat(CarAppExtender.isExtended(builder.build())).isFalse();
    }

    @Test
    public void notification_extended_setTitle() {
        CharSequence title = "TestTitle";
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                        .extend(new CarAppExtender.Builder().setContentTitle(title).build());

        assertThat(
                title.toString().contentEquals(
                        new CarAppExtender(builder.build()).getContentTitle()))
                .isTrue();
    }

    @Test
    public void notification_extended_setText() {
        CharSequence text = "TestText";
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                        .extend(new CarAppExtender.Builder().setContentText(text).build());

        assertThat(
                text.toString().contentEquals(new CarAppExtender(builder.build()).getContentText()))
                .isTrue();
    }

    @Test
    public void notification_extended_setSmallIcon() {
        int resId = TestUtils.getTestDrawableResId(mContext, "ic_test_1");
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                        .extend(new CarAppExtender.Builder().setSmallIcon(resId).build());

        assertThat(new CarAppExtender(builder.build()).getSmallIcon()).isEqualTo(resId);
    }

    @Test
    public void notification_extended_setLargeIcon() {
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),
                TestUtils.getTestDrawableResId(mContext, "ic_test_2"));
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                        .extend(new CarAppExtender.Builder().setLargeIcon(bitmap).build());

        assertThat(new CarAppExtender(builder.build()).getLargeIcon()).isEqualTo(bitmap);
    }

    @Test
    public void notification_extended_setContentIntent() {
        Intent intent = new Intent(INTENT_PRIMARY_ACTION);
        PendingIntent contentIntent = PendingIntent.getBroadcast(mContext, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                        .extend(new CarAppExtender.Builder().setContentIntent(
                                contentIntent).build());

        assertThat(new CarAppExtender(builder.build()).getContentIntent()).isEqualTo(contentIntent);
    }

    @Test
    public void notification_extended_setDeleteIntent() {
        Intent intent = new Intent(INTENT_PRIMARY_ACTION);
        PendingIntent deleteIntent = PendingIntent.getBroadcast(mContext, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                        .extend(new CarAppExtender.Builder().setDeleteIntent(deleteIntent).build());

        assertThat(new CarAppExtender(builder.build()).getDeleteIntent()).isEqualTo(deleteIntent);
    }

    @Test
    public void notification_extended_noActions() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                        .extend(new CarAppExtender.Builder().build());

        assertThat(new CarAppExtender(builder.build()).getActions()).isEmpty();
    }

    @Test
    @SuppressWarnings("deprecation") // Action.icon
    public void notification_extended_addActions() {
        int icon1 = TestUtils.getTestDrawableResId(mContext, "ic_test_1");
        CharSequence title1 = "FirstAction";
        Intent intent1 = new Intent(INTENT_PRIMARY_ACTION);
        PendingIntent actionIntent1 = PendingIntent.getBroadcast(mContext, 0, intent1,
                PendingIntent.FLAG_IMMUTABLE);

        int icon2 = TestUtils.getTestDrawableResId(mContext, "ic_test_2");
        CharSequence title2 = "SecondAction";
        Intent intent2 = new Intent(INTENT_SECONDARY_ACTION);
        PendingIntent actionIntent2 = PendingIntent.getBroadcast(mContext, 0, intent2,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                        .extend(
                                new CarAppExtender.Builder()
                                        .addAction(icon1, title1, actionIntent1)
                                        .addAction(icon2, title2, actionIntent2)
                                        .build());

        List<Action> actions = new CarAppExtender(builder.build()).getActions();
        assertThat(actions).hasSize(2);
        assertThat(actions.get(0).icon).isEqualTo(icon1);
        assertThat(title1.toString().contentEquals(actions.get(0).title)).isTrue();
        assertThat(actions.get(0).actionIntent).isEqualTo(actionIntent1);
        assertThat(actions.get(1).icon).isEqualTo(icon2);
        assertThat(title2.toString().contentEquals(actions.get(1).title)).isTrue();
        assertThat(actions.get(1).actionIntent).isEqualTo(actionIntent2);
    }

    @Test
    public void notification_extended_setImportance() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                        .extend(
                                new CarAppExtender.Builder()
                                        .setImportance(NotificationManagerCompat.IMPORTANCE_HIGH)
                                        .build());

        assertThat(new CarAppExtender(builder.build()).getImportance())
                .isEqualTo(NotificationManagerCompat.IMPORTANCE_HIGH);
    }

    @Test
    public void notification_extended_setColor() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                        .extend(
                                new CarAppExtender.Builder()
                                        .setColor(CarColor.BLUE)
                                        .build());

        assertThat(new CarAppExtender(builder.build()).getColor())
                .isEqualTo(CarColor.BLUE);
    }

    @Test
    public void notification_extended_channelId() {
        String channelId = "foo";
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                        .extend(
                                new CarAppExtender.Builder()
                                        .setChannelId(channelId)
                                        .build());

        assertThat(new CarAppExtender(builder.build()).getChannelId())
                .isEqualTo(channelId);
    }

    @Test
    @SuppressWarnings("deprecation") // Required for Notification.Builder(Context)
    public void extend_withNonCompatNotification_hasExtras() {
        CarAppExtender extender =
                new CarAppExtender.Builder()
                        .setContentTitle("Some title")
                        .setContentText("Some text")
                        .setSmallIcon(TestUtils.getTestDrawableResId(mContext, "ic_test_1"))
                        .setLargeIcon(
                                BitmapFactory.decodeResource(mContext.getResources(),
                                        TestUtils.getTestDrawableResId(mContext, "ic_test_2")))
                        .setContentIntent(PendingIntent.getBroadcast(mContext, 0, new Intent(), 0))
                        .setDeleteIntent(PendingIntent.getBroadcast(mContext, 0, new Intent(), 0))
                        .addAction(
                                TestUtils.getTestDrawableResId(mContext, "ic_test_1"),
                                "Title",
                                PendingIntent.getBroadcast(mContext, 0, new Intent(), 0))
                        .setImportance(NotificationManagerCompat.IMPORTANCE_MAX)
                        .setColor(CarColor.BLUE)
                        .setChannelId("Some id")
                        .build();

        Notification.Builder plainAndroidNotificationBuilder = new Notification.Builder(mContext);
        extender.extend(plainAndroidNotificationBuilder);

        Notification resultNotification = plainAndroidNotificationBuilder.build();
        CarAppExtender resultExtender = new CarAppExtender(resultNotification);

        assertThat(resultExtender.getContentTitle()).isEqualTo(extender.getContentTitle());
        assertThat(resultExtender.getContentText()).isEqualTo(extender.getContentText());
        assertThat(resultExtender.getSmallIcon()).isEqualTo(extender.getSmallIcon());
        assertThat(resultExtender.getContentIntent()).isNotNull();
        assertThat(resultExtender.getDeleteIntent()).isNotNull();
        assertThat(resultExtender.getActions()).hasSize(1);
        assertThat(resultExtender.getImportance()).isEqualTo(extender.getImportance());
        assertThat(resultExtender.getColor()).isEqualTo(extender.getColor());
        assertThat(resultExtender.getChannelId()).isEqualTo(extender.getChannelId());
    }
}
