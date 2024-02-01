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
import android.app.slice.Slice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Bundle;

import androidx.credentials.PublicKeyCredential;
import androidx.credentials.R;
import androidx.credentials.TestUtilsKt;
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption;
import androidx.credentials.provider.PublicKeyCredentialEntry;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 26)
@SmallTest
public class PublicKeyCredentialEntryJavaTest {
    private static final CharSequence USERNAME = "title";
    private static final CharSequence DISPLAYNAME = "subtitle";
    private static final CharSequence TYPE_DISPLAY_NAME = "Password";
    private static final Long LAST_USED_TIME = 10L;
    private static final Icon ICON = Icon.createWithBitmap(Bitmap.createBitmap(
            100, 100, Bitmap.Config.ARGB_8888));
    private static final boolean IS_AUTO_SELECT_ALLOWED = true;
    private final BeginGetPublicKeyCredentialOption mBeginOption =
            new BeginGetPublicKeyCredentialOption(
                    new Bundle(), "id", "{\"key1\":{\"key2\":{\"key3\":\"value3\"}}}");

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final Intent mIntent = new Intent();
    private final PendingIntent mPendingIntent =
            PendingIntent.getActivity(mContext, 0, mIntent,
                    PendingIntent.FLAG_IMMUTABLE);

    @Test
    public void build_requiredParamsOnly_success() {
        PublicKeyCredentialEntry entry = constructWithRequiredParamsOnly();

        assertNotNull(entry);
        assertThat(entry.getType()).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL);
        assertEntryWithRequiredParams(entry);
    }

    @Test
    public void build_allParams_success() {
        PublicKeyCredentialEntry entry = constructWithAllParams();

        assertNotNull(entry);
        assertThat(entry.getType()).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL);
        assertEntryWithAllParams(entry);
    }

    @Test
    public void build_withNullUsername_throwsNPE() {
        assertThrows("Expected null username to throw NPE",
                NullPointerException.class,
                () -> new PublicKeyCredentialEntry.Builder(
                        mContext, null, mPendingIntent, mBeginOption
                ).build());
    }

    @Test
    public void build_withNullBeginOption_throwsNPE() {
        assertThrows("Expected null option to throw NPE",
                NullPointerException.class,
                () -> new PublicKeyCredentialEntry.Builder(
                        mContext, USERNAME, mPendingIntent, null
                ).build());
    }

    @Test
    public void build_withNullPendingIntent_throwsNPE() {
        assertThrows("Expected null pending intent to throw NPE",
                NullPointerException.class,
                () -> new PublicKeyCredentialEntry.Builder(
                        mContext, USERNAME, null, mBeginOption
                ).build());
    }

    @Test
    public void build_withEmptyUsername_throwsIAE() {
        assertThrows("Expected empty username to throw IllegalArgumentException",
                IllegalArgumentException.class,
                () -> new PublicKeyCredentialEntry.Builder(
                        mContext, "", mPendingIntent, mBeginOption).build());
    }

    @Test
    public void build_withNullIcon_defaultIconSet() {
        PublicKeyCredentialEntry entry = new PublicKeyCredentialEntry
                .Builder(
                mContext, USERNAME, mPendingIntent, mBeginOption).build();

        assertThat(TestUtilsKt.equals(entry.getIcon(),
                Icon.createWithResource(mContext, R.drawable.ic_passkey))).isTrue();
    }

    @Test
    public void build_nullTypeDisplayName_defaultDisplayNameSet() {
        PublicKeyCredentialEntry entry = new PublicKeyCredentialEntry.Builder(
                        mContext, USERNAME, mPendingIntent, mBeginOption).build();

        assertThat(entry.getTypeDisplayName()).isEqualTo(
                mContext.getString(
                        R.string.androidx_credentials_TYPE_PUBLIC_KEY_CREDENTIAL)
        );
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void fromSlice_success() {
        PublicKeyCredentialEntry originalEntry = constructWithAllParams();

        PublicKeyCredentialEntry entry = PublicKeyCredentialEntry.fromSlice(
                PublicKeyCredentialEntry.toSlice(originalEntry));

        assertNotNull(entry);
        assertEntryWithRequiredParams(entry);
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    public void fromCredentialEntry_success() {
        PublicKeyCredentialEntry originalEntry = constructWithAllParams();
        Slice slice = PublicKeyCredentialEntry.toSlice(originalEntry);
        assertNotNull(slice);

        PublicKeyCredentialEntry entry = PublicKeyCredentialEntry.fromCredentialEntry(
                new android.service.credentials.CredentialEntry("id", slice));

        assertNotNull(entry);
        assertEntryWithRequiredParams(entry);
    }

    private PublicKeyCredentialEntry constructWithRequiredParamsOnly() {
        return new PublicKeyCredentialEntry.Builder(
                mContext,
                USERNAME,
                mPendingIntent,
                mBeginOption
        ).build();
    }

    private PublicKeyCredentialEntry constructWithAllParams() {
        return new PublicKeyCredentialEntry.Builder(
                mContext, USERNAME, mPendingIntent, mBeginOption)
                .setAutoSelectAllowed(IS_AUTO_SELECT_ALLOWED)
                .setDisplayName(DISPLAYNAME)
                .setLastUsedTime(Instant.ofEpochMilli(LAST_USED_TIME))
                .setIcon(ICON)
                .build();
    }

    private void assertEntryWithRequiredParams(PublicKeyCredentialEntry entry) {
        assertThat(USERNAME.equals(entry.getUsername()));
        assertThat(mPendingIntent).isEqualTo(entry.getPendingIntent());
    }

    private void assertEntryWithAllParams(PublicKeyCredentialEntry entry) {
        assertThat(USERNAME.equals(entry.getUsername()));
        assertThat(DISPLAYNAME.equals(entry.getDisplayName()));
        assertThat(TYPE_DISPLAY_NAME.equals(entry.getTypeDisplayName()));
        assertThat(ICON).isEqualTo(entry.getIcon());
        assertThat(Instant.ofEpochMilli(LAST_USED_TIME)).isEqualTo(entry.getLastUsedTime());
        assertThat(IS_AUTO_SELECT_ALLOWED).isEqualTo(entry.isAutoSelectAllowed());
        assertThat(mPendingIntent).isEqualTo(entry.getPendingIntent());
    }
}
