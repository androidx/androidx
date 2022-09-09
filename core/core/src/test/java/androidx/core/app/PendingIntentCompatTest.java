/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.core.app;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPendingIntent;

/** Unit test for {@link PendingIntentCompat}. */
@RunWith(RobolectricTestRunner.class)
public class PendingIntentCompatTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    @Config(maxSdk = 22)
    @Test
    public void addMutabilityFlags_immutableOnPreM() {
        int requestCode = 7465;
        Intent intent = new Intent();
        Bundle options = new Bundle();
        ShadowPendingIntent shadow =
                shadowOf(
                        PendingIntentCompat.getActivity(
                                context,
                                requestCode,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT,
                                options,
                                /* isMutable= */ false));
        assertThat(shadow.isActivityIntent()).isTrue();
        assertThat(shadow.getFlags()).isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT);
        assertThat(shadow.getRequestCode()).isEqualTo(requestCode);
    }

    @Config(minSdk = 23)
    @Test
    public void addMutabilityFlags_immutableOnMPlus() {
        int requestCode = 7465;
        Intent intent = new Intent();
        Bundle options = new Bundle();
        ShadowPendingIntent shadow =
                shadowOf(
                        PendingIntentCompat.getActivity(
                                context,
                                requestCode,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT,
                                options,
                                /* isMutable= */ false));
        assertThat(shadow.isActivityIntent()).isTrue();
        assertThat(shadow.getFlags())
                .isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        assertThat(shadow.getRequestCode()).isEqualTo(requestCode);
    }

    @Config(maxSdk = 30)
    @Test
    public void addMutabilityFlags_mutableOnPreS() {
        int requestCode = 7465;
        Intent intent = new Intent();
        Bundle options = new Bundle();
        ShadowPendingIntent shadow =
                shadowOf(
                        PendingIntentCompat.getActivity(
                                context,
                                requestCode,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT,
                                options,
                                /* isMutable= */ true));
        assertThat(shadow.isActivityIntent()).isTrue();
        assertThat(shadow.getFlags()).isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT);
        assertThat(shadow.getRequestCode()).isEqualTo(requestCode);
    }

    @Config(minSdk = 31)
    @Test
    public void addMutabilityFlags_mutableOnSPlus() {
        int requestCode = 7465;
        Intent intent = new Intent();
        Bundle options = new Bundle();
        ShadowPendingIntent shadow =
                shadowOf(
                        PendingIntentCompat.getActivity(
                                context,
                                requestCode,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT,
                                options,
                                /* isMutable= */ true));
        assertThat(shadow.isActivityIntent()).isTrue();
        assertThat(shadow.getFlags())
                .isEqualTo(PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        assertThat(shadow.getRequestCode()).isEqualTo(requestCode);
    }

    @Test
    public void getActivities_withBundle() {
        int requestCode = 7465;
        Intent[] intents = new Intent[] {};
        Bundle options = new Bundle();
        ShadowPendingIntent shadow =
                shadowOf(
                        PendingIntentCompat.getActivities(
                                context,
                                requestCode,
                                intents,
                                PendingIntent.FLAG_UPDATE_CURRENT,
                                options,
                                /* isMutable= */ false));
        assertThat(shadow.isActivityIntent()).isTrue();
    }

    @Test
    public void getActivities() {
        int requestCode = 7465;
        Intent[] intents = new Intent[] {};
        Bundle options = new Bundle();
        ShadowPendingIntent shadow =
                shadowOf(
                        PendingIntentCompat.getActivities(
                                context,
                                requestCode,
                                intents,
                                PendingIntent.FLAG_UPDATE_CURRENT,
                                /* isMutable= */ false));
        assertThat(shadow.isActivityIntent()).isTrue();
    }

    @Test
    public void getActivity_withBundle() {
        int requestCode = 7465;
        Intent intent = new Intent();
        Bundle options = new Bundle();
        ShadowPendingIntent shadow =
                shadowOf(
                        PendingIntentCompat.getActivity(
                                context,
                                requestCode,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT,
                                options,
                                /* isMutable= */ false));
        assertThat(shadow.isActivityIntent()).isTrue();
    }

    @Test
    public void getActivity() {
        int requestCode = 7465;
        Intent intent = new Intent();
        Bundle options = new Bundle();
        ShadowPendingIntent shadow =
                shadowOf(
                        PendingIntentCompat.getActivity(
                                context,
                                requestCode,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT,
                                /* isMutable= */ false));
        assertThat(shadow.isActivityIntent()).isTrue();
    }

    @Test
    public void getService() {
        int requestCode = 7465;
        Intent intent = new Intent();
        Bundle options = new Bundle();
        ShadowPendingIntent shadow =
                shadowOf(
                        PendingIntentCompat.getService(
                                context,
                                requestCode,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT,
                                /* isMutable= */ false));
        assertThat(shadow.isService()).isTrue();
    }

    @Test
    public void getBroadcast() {
        int requestCode = 7465;
        Intent intent = new Intent();
        Bundle options = new Bundle();
        ShadowPendingIntent shadow =
                shadowOf(
                        PendingIntentCompat.getBroadcast(
                                context,
                                requestCode,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT,
                                /* isMutable= */ false));
        assertThat(shadow.isBroadcast()).isTrue();
    }

    @TargetApi(26)
    @Config(minSdk = 26)
    @Test
    public void getForegroundService() {
        int requestCode = 7465;
        Intent intent = new Intent();
        Bundle options = new Bundle();
        ShadowPendingIntent shadow =
                shadowOf(
                        PendingIntentCompat.getForegroundService(
                                context,
                                requestCode,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT,
                                /* isMutable= */ false));
        assertThat(shadow.isForegroundService()).isTrue();
    }
}
