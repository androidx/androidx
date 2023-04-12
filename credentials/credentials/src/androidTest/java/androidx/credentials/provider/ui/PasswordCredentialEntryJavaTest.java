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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Bundle;

import androidx.core.os.BuildCompat;
import androidx.credentials.PasswordCredential;
import androidx.credentials.R;
import androidx.credentials.TestUtilsKt;
import androidx.credentials.provider.BeginGetPasswordOption;
import androidx.credentials.provider.PasswordCredentialEntry;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.HashSet;

@RunWith(AndroidJUnit4.class)
@SmallTest
@SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
public class PasswordCredentialEntryJavaTest {
    private static final CharSequence USERNAME = "title";
    private static final CharSequence DISPLAYNAME = "subtitle";
    private static final CharSequence TYPE_DISPLAY_NAME = "Password";
    private static final Long LAST_USED_TIME = 10L;
    private static final Icon ICON = Icon.createWithBitmap(Bitmap.createBitmap(
            100, 100, Bitmap.Config.ARGB_8888));
    private final BeginGetPasswordOption mBeginGetPasswordOption = new BeginGetPasswordOption(
            new HashSet<>(),
            Bundle.EMPTY, "id");

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final Intent mIntent = new Intent();
    private final PendingIntent mPendingIntent =
            PendingIntent.getActivity(mContext, 0, mIntent,
                    PendingIntent.FLAG_IMMUTABLE);

    @Test
    public void build_requiredParams_success() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        PasswordCredentialEntry entry = constructEntryWithRequiredParamsOnly();

        assertNotNull(entry);
        assertNotNull(entry.getSlice());
        assertThat(entry.getType()).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL);
        assertEntryWithRequiredParamsOnly(entry, false);
    }

    @Test
    public void build_allParams_success() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        PasswordCredentialEntry entry = constructEntryWithAllParams();

        assertNotNull(entry);
        assertNotNull(entry.getSlice());
        assertThat(entry.getType()).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL);
        assertEntryWithAllParams(entry, false);
    }

    @Test
    public void build_nullContext_throwsNPE() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        assertThrows("Expected null context to throw NPE",
                NullPointerException.class,
                () -> new PasswordCredentialEntry.Builder(
                        null, USERNAME, mPendingIntent, mBeginGetPasswordOption
                ).build());
    }

    @Test
    public void build_nullUsername_throwsNPE() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        assertThrows("Expected null username to throw NPE",
                NullPointerException.class,
                () -> new PasswordCredentialEntry.Builder(
                        mContext, null, mPendingIntent, mBeginGetPasswordOption
                ).build());
    }

    @Test
    public void build_nullPendingIntent_throwsNPE() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        assertThrows("Expected null pending intent to throw NPE",
                NullPointerException.class,
                () -> new PasswordCredentialEntry.Builder(
                        mContext, USERNAME, null, mBeginGetPasswordOption
                ).build());
    }

    @Test
    public void build_nullBeginOption_throwsNPE() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        assertThrows("Expected null option to throw NPE",
                NullPointerException.class,
                () -> new PasswordCredentialEntry.Builder(
                        mContext, USERNAME, mPendingIntent, null
                ).build());
    }

    @Test
    public void build_emptyUsername_throwsIAE() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        assertThrows("Expected empty username to throw IllegalArgumentException",
                IllegalArgumentException.class,
                () -> new PasswordCredentialEntry.Builder(
                        mContext, "", mPendingIntent, mBeginGetPasswordOption).build());
    }

    @Test
    public void build_nullIcon_defaultIconSet() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        PasswordCredentialEntry entry = new PasswordCredentialEntry
                .Builder(mContext, USERNAME, mPendingIntent, mBeginGetPasswordOption).build();

        assertThat(TestUtilsKt.equals(entry.getIcon(),
                Icon.createWithResource(mContext, R.drawable.ic_password))).isTrue();
    }

    @Test
    public void build_nullTypeDisplayName_defaultDisplayNameSet() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        PasswordCredentialEntry entry = new PasswordCredentialEntry.Builder(
                        mContext, USERNAME, mPendingIntent, mBeginGetPasswordOption).build();

        assertThat(entry.getTypeDisplayName()).isEqualTo(
                mContext.getString(
                        R.string.android_credentials_TYPE_PASSWORD_CREDENTIAL)
        );
    }

    @Test
    public void build_isAutoSelectAllowedDefault_false() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        PasswordCredentialEntry entry = constructEntryWithRequiredParamsOnly();
        PasswordCredentialEntry entry1 = constructEntryWithAllParams();

        assertFalse(entry.isAutoSelectAllowed());
        assertFalse(entry1.isAutoSelectAllowed());
    }

    @Test
    public void fromSlice_requiredParams_success() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        PasswordCredentialEntry originalEntry = constructEntryWithRequiredParamsOnly();

        assertNotNull(originalEntry.getSlice());
        PasswordCredentialEntry entry = PasswordCredentialEntry.fromSlice(
                originalEntry.getSlice());

        assertNotNull(entry);
        assertEntryWithRequiredParamsOnly(entry, true);
    }

    @Test
    public void fromSlice_allParams_success() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        PasswordCredentialEntry originalEntry = constructEntryWithAllParams();

        assertNotNull(originalEntry.getSlice());
        PasswordCredentialEntry entry = PasswordCredentialEntry.fromSlice(
                originalEntry.getSlice());

        assertNotNull(entry);
        assertEntryWithAllParams(entry, true);
    }

    private PasswordCredentialEntry constructEntryWithRequiredParamsOnly() {
        return new PasswordCredentialEntry.Builder(
                mContext,
                USERNAME,
                mPendingIntent,
                mBeginGetPasswordOption).build();
    }

    private PasswordCredentialEntry constructEntryWithAllParams() {
        return new PasswordCredentialEntry.Builder(
                mContext,
                USERNAME,
                mPendingIntent,
                mBeginGetPasswordOption)
                .setDisplayName(DISPLAYNAME)
                .setLastUsedTime(Instant.ofEpochMilli(LAST_USED_TIME))
                .setIcon(ICON)
                .build();
    }

    private void assertEntryWithRequiredParamsOnly(PasswordCredentialEntry entry,
            Boolean assertOptionIdOnly) {
        assertThat(USERNAME.equals(entry.getUsername()));
        assertThat(mPendingIntent).isEqualTo(entry.getPendingIntent());
        // TODO: Assert BeginOption
    }

    private void assertEntryWithAllParams(PasswordCredentialEntry entry,
            Boolean assertOptionIdOnly) {
        assertThat(USERNAME.equals(entry.getUsername()));
        assertThat(DISPLAYNAME.equals(entry.getDisplayName()));
        assertThat(TYPE_DISPLAY_NAME.equals(entry.getTypeDisplayName()));
        assertThat(ICON).isEqualTo(entry.getIcon());
        assertThat(Instant.ofEpochMilli(LAST_USED_TIME)).isEqualTo(entry.getLastUsedTime());
        assertThat(mPendingIntent).isEqualTo(entry.getPendingIntent());
        // TODO: Assert BeginOption
    }
}
