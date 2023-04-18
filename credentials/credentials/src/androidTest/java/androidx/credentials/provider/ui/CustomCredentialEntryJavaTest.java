/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.credentials.provider.ui;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Bundle;

import androidx.core.os.BuildCompat;
import androidx.credentials.R;
import androidx.credentials.TestUtilsKt;
import androidx.credentials.provider.BeginGetCredentialOption;
import androidx.credentials.provider.BeginGetCustomCredentialOption;
import androidx.credentials.provider.CustomCredentialEntry;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;

@RunWith(AndroidJUnit4.class)
@SmallTest
@SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
public class CustomCredentialEntryJavaTest {
    private static final CharSequence TITLE = "title";
    private static final CharSequence SUBTITLE = "subtitle";

    private static final String TYPE = "custom_type";
    private static final CharSequence TYPE_DISPLAY_NAME = "Password";
    private static final Long LAST_USED_TIME = 10L;
    private static final Icon ICON = Icon.createWithBitmap(Bitmap.createBitmap(
            100, 100, Bitmap.Config.ARGB_8888));
    private static final boolean IS_AUTO_SELECT_ALLOWED = true;
    private final BeginGetCredentialOption mBeginCredentialOption =
            new BeginGetCustomCredentialOption(
            "id", "custom", new Bundle());

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final Intent mIntent = new Intent();
    private final PendingIntent mPendingIntent =
            PendingIntent.getActivity(mContext, 0, mIntent,
                    PendingIntent.FLAG_IMMUTABLE);

    @Test
    public void build_requiredParameters_success() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        CustomCredentialEntry entry = constructEntryWithRequiredParams();

        assertNotNull(entry);
        assertNotNull(entry.getSlice());
        assertEntryWithRequiredParams(entry);
    }

    @Test
    public void build_allParameters_success() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        CustomCredentialEntry entry = constructEntryWithAllParams();

        assertNotNull(entry);
        assertNotNull(entry.getSlice());
        assertEntryWithAllParams(entry);
    }

    @Test
    public void build_nullTitle_throwsNPE() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        assertThrows("Expected null title to throw NPE",
                NullPointerException.class,
                () -> new CustomCredentialEntry.Builder(
                        mContext, TYPE, null, mPendingIntent, mBeginCredentialOption
                ));
    }

    @Test
    public void build_nullContext_throwsNPE() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        assertThrows("Expected null title to throw NPE",
                NullPointerException.class,
                () -> new CustomCredentialEntry.Builder(
                        null, TYPE, TITLE, mPendingIntent, mBeginCredentialOption
                ).build());
    }

    @Test
    public void build_nullPendingIntent_throwsNPE() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        assertThrows("Expected null pending intent to throw NPE",
                NullPointerException.class,
                () -> new CustomCredentialEntry.Builder(
                        mContext, TYPE, TITLE, null, mBeginCredentialOption
                ).build());
    }

    @Test
    public void build_nullBeginOption_throwsNPE() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        assertThrows("Expected null option to throw NPE",
                NullPointerException.class,
                () -> new CustomCredentialEntry.Builder(
                        mContext, TYPE, TITLE, mPendingIntent, null
                ).build());
    }

    @Test
    public void build_emptyTitle_throwsIAE() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        assertThrows("Expected empty title to throw IAE",
                IllegalArgumentException.class,
                () -> new CustomCredentialEntry.Builder(
                        mContext, TYPE, "", mPendingIntent, mBeginCredentialOption
                ).build());
    }

    @Test
    public void build_emptyType_throwsIAE() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        assertThrows("Expected empty type to throw NPE",
                IllegalArgumentException.class,
                () -> new CustomCredentialEntry.Builder(
                        mContext, "", TITLE, mPendingIntent, mBeginCredentialOption
                ).build());
    }

    @Test
    public void build_nullIcon_defaultIconSet() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        CustomCredentialEntry entry = constructEntryWithRequiredParams();

        assertThat(TestUtilsKt.equals(entry.getIcon(),
                Icon.createWithResource(mContext, R.drawable.ic_other_sign_in))).isTrue();
    }

    @Test
    public void fromSlice_requiredParams_success() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        CustomCredentialEntry originalEntry = constructEntryWithRequiredParams();

        CustomCredentialEntry entry = CustomCredentialEntry.fromSlice(
                originalEntry.getSlice());

        assertNotNull(entry);
        assertEntryWithRequiredParamsFromSlice(entry);
    }

    @Test
    public void fromSlice_allParams_success() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        CustomCredentialEntry originalEntry = constructEntryWithAllParams();

        CustomCredentialEntry entry = CustomCredentialEntry.fromSlice(
                originalEntry.getSlice());

        assertNotNull(entry);
        assertEntryWithAllParamsFromSlice(entry);
    }

    private CustomCredentialEntry constructEntryWithRequiredParams() {
        return new CustomCredentialEntry.Builder(
                mContext,
                TYPE,
                TITLE,
                mPendingIntent,
                mBeginCredentialOption
        ).build();
    }

    private CustomCredentialEntry constructEntryWithAllParams() {
        return new CustomCredentialEntry.Builder(
                mContext,
                TYPE,
                TITLE,
                mPendingIntent,
                mBeginCredentialOption)
                .setIcon(ICON)
                .setLastUsedTime(Instant.ofEpochMilli(LAST_USED_TIME))
                .setAutoSelectAllowed(IS_AUTO_SELECT_ALLOWED)
                .setTypeDisplayName(TYPE_DISPLAY_NAME)
                .build();
    }

    private void assertEntryWithRequiredParams(CustomCredentialEntry entry) {
        assertThat(TITLE.equals(entry.getTitle()));
        assertThat(TYPE.equals(entry.getType()));
        assertThat(mPendingIntent).isEqualTo(entry.getPendingIntent());
    }

    private void assertEntryWithRequiredParamsFromSlice(CustomCredentialEntry entry) {
        assertThat(TITLE.equals(entry.getTitle()));
        assertThat(TYPE.equals(entry.getType()));
        assertThat(mPendingIntent).isEqualTo(entry.getPendingIntent());
    }

    private void assertEntryWithAllParams(CustomCredentialEntry entry) {
        assertThat(TITLE.equals(entry.getTitle()));
        assertThat(TYPE.equals(entry.getType()));
        assertThat(SUBTITLE.equals(entry.getSubtitle()));
        assertThat(TYPE_DISPLAY_NAME.equals(entry.getTypeDisplayName()));
        assertThat(ICON).isEqualTo(entry.getIcon());
        assertThat(Instant.ofEpochMilli(LAST_USED_TIME)).isEqualTo(entry.getLastUsedTime());
        assertThat(IS_AUTO_SELECT_ALLOWED).isEqualTo(entry.isAutoSelectAllowed());
        assertThat(mPendingIntent).isEqualTo(entry.getPendingIntent());
        // TODO: Assert BeginOption
    }

    private void assertEntryWithAllParamsFromSlice(CustomCredentialEntry entry) {
        assertThat(TITLE.equals(entry.getTitle()));
        assertThat(TYPE.equals(entry.getType()));
        assertThat(SUBTITLE.equals(entry.getSubtitle()));
        assertThat(TYPE_DISPLAY_NAME.equals(entry.getTypeDisplayName()));
        assertThat(ICON).isEqualTo(entry.getIcon());
        assertThat(Instant.ofEpochMilli(LAST_USED_TIME)).isEqualTo(entry.getLastUsedTime());
        assertThat(IS_AUTO_SELECT_ALLOWED).isEqualTo(entry.isAutoSelectAllowed());
        assertThat(mPendingIntent).isEqualTo(entry.getPendingIntent());
        // TODO: Assert BeginOption
    }
}
