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

package androidx.car.app.suggestion.model;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.car.app.model.CarIcon;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Objects;

/** Tests for {@link Suggestion}. */
@RunWith(RobolectricTestRunner.class)
public class SuggestionTest {
    private final String mIdentifier = "1";
    private final String mTitle = "car";
    private final String mSubTitle = "subtitle";
    private final Intent mIntent = new Intent();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final PendingIntent mPendingIntent = PendingIntent.getActivity(mContext, 0, mIntent, 0);
    private final CarIcon mIcon = CarIcon.APP_ICON;

    @Test
    public void noIdentifierProvided_throws() {
        Suggestion.Builder builder = new Suggestion.Builder();

        builder.setTitle(mTitle);
        builder.setSubtitle(mSubTitle);
        builder.setIcon(mIcon);
        builder.setAction(mPendingIntent);

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    public void noTitleProvided_throws() {
        Suggestion.Builder builder = new Suggestion.Builder();
        builder.setIdentifier(mIdentifier);
        builder.setSubtitle(mSubTitle);
        builder.setIcon(mIcon);
        builder.setAction(mPendingIntent);

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    public void noPendingIntentProvided_throws() {
        Suggestion.Builder builder = new Suggestion.Builder();
        builder.setIdentifier(mIdentifier);
        builder.setTitle(mTitle);
        builder.setSubtitle(mSubTitle);
        builder.setIcon(mIcon);

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    public void builds_correctly() {
        Suggestion.Builder builder = new Suggestion.Builder();
        builder.setIdentifier(mIdentifier);
        builder.setTitle(mTitle);
        builder.setSubtitle(mSubTitle);
        builder.setIcon(mIcon);
        builder.setAction(mPendingIntent);

        Suggestion suggestion = builder.build();

        assertThat(suggestion.getIdentifier()).isEqualTo(mIdentifier);
        assertThat(suggestion.getTitle().toString()).isEqualTo(mTitle);
        assertThat(Objects.requireNonNull(suggestion.getSubtitle()).toString()).isEqualTo(
                mSubTitle);
        assertThat(suggestion.getAction()).isEqualTo(mPendingIntent);

    }
}
