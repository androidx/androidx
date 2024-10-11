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

import static androidx.credentials.provider.ui.UiUtils.testBiometricPromptData;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;

import androidx.core.os.BuildCompat;
import androidx.credentials.provider.BiometricPromptData;
import androidx.credentials.provider.CreateEntry;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 26) // Instant usage
@SmallTest
public class CreateEntryJavaTest {
    private static final CharSequence ACCOUNT_NAME = "account_name";
    private static final int PASSWORD_COUNT = 10;
    private static final int PUBLIC_KEY_CREDENTIAL_COUNT = 10;
    private static final int TOTAL_COUNT = 10;

    private static final Long LAST_USED_TIME = 10L;
    private static final Icon ICON = Icon.createWithBitmap(Bitmap.createBitmap(
            100, 100, Bitmap.Config.ARGB_8888));

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final Intent mIntent = new Intent();
    private final PendingIntent mPendingIntent =
            PendingIntent.getActivity(mContext, 0, mIntent,
                    PendingIntent.FLAG_IMMUTABLE);

    @Test
    public void constructor_requiredParameters_success() {
        CreateEntry entry = constructEntryWithRequiredParams();

        assertNotNull(entry);
        assertEntryWithRequiredParams(entry);
        assertNull(entry.getIcon());
        assertNull(entry.getLastUsedTime());
        assertNull(entry.getPasswordCredentialCount());
        assertNull(entry.getPublicKeyCredentialCount());
        assertNull(entry.getTotalCredentialCount());
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    public void constructor_allParametersAboveApiO_success() {
        CreateEntry entry = constructEntryWithAllParams(/*nullBiometricPromptData=*/ false);

        assertNotNull(entry);
        assertEntryWithAllParams(entry);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    public void constructor_allParametersAboveApiOForcedBiometricPromptDataNull_success() {
        CreateEntry entry = constructEntryWithAllParams(/*nullBiometricPromptData=*/ true);

        assertNotNull(entry);
        assertEntryWithAllParams(entry);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    public void constructor_allParametersApiOAndBelow_success() {
        CreateEntry entry = constructEntryWithAllParams(/*nullBiometricPromptData=*/ false);

        assertNotNull(entry);
        assertEntryWithAllParams(entry);
    }

    @Test
    public void constructor_nullAccountName_throwsNPE() {
        assertThrows("Expected null title to throw NPE",
                NullPointerException.class,
                () -> new CreateEntry.Builder(
                        null, mPendingIntent).build());
    }

    @Test
    public void constructor_nullPendingIntent_throwsNPE() {
        assertThrows("Expected null pending intent to throw NPE",
                NullPointerException.class,
                () -> new CreateEntry.Builder(ACCOUNT_NAME, null).build());
    }

    @Test
    public void constructor_emptyAccountName_throwsIAE() {
        assertThrows("Expected empty account name to throw NPE",
                IllegalArgumentException.class,
                () -> new CreateEntry.Builder("", mPendingIntent).build());
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void fromSlice_requiredParams_success() {
        CreateEntry originalEntry = constructEntryWithRequiredParams();

        CreateEntry entry = CreateEntry.fromSlice(
                CreateEntry.toSlice(originalEntry));

        assertNotNull(entry);
        assertEntryWithRequiredParams(entry);
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void fromSlice_allParams_success() {
        CreateEntry originalEntry = constructEntryWithAllParams(/*nullBiometricPromptData=*/ false);

        CreateEntry entry = CreateEntry.fromSlice(
                CreateEntry.toSlice(originalEntry));

        assertNotNull(entry);
        assertEntryWithAllParams(entry);
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    @SuppressWarnings("deprecation")
    public void fromCreateEntry_allParams_success() {
        CreateEntry originalEntry = constructEntryWithAllParams(/*nullBiometricPromptData=*/ false);
        android.app.slice.Slice slice = CreateEntry.toSlice(originalEntry);
        assertNotNull(slice);

        CreateEntry entry = CreateEntry.fromCreateEntry(
                new android.service.credentials.CreateEntry(slice));

        assertNotNull(entry);
        assertEntryWithAllParams(entry);
    }

    private CreateEntry constructEntryWithRequiredParams() {
        return new CreateEntry.Builder(ACCOUNT_NAME, mPendingIntent).build();
    }

    private void assertEntryWithRequiredParams(CreateEntry entry) {
        assertThat(ACCOUNT_NAME.equals(entry.getAccountName()));
        assertThat(mPendingIntent).isEqualTo(entry.getPendingIntent());
        assertThat(entry.getBiometricPromptData()).isNull();
    }

    private CreateEntry constructEntryWithAllParams(boolean nullBiometricPromptData) {
        CreateEntry.Builder testBuilder = new CreateEntry.Builder(
                ACCOUNT_NAME,
                mPendingIntent)
                .setIcon(ICON)
                .setLastUsedTime(Instant.ofEpochMilli(LAST_USED_TIME))
                .setPasswordCredentialCount(PASSWORD_COUNT)
                .setPublicKeyCredentialCount(PUBLIC_KEY_CREDENTIAL_COUNT)
                .setTotalCredentialCount(TOTAL_COUNT);
        if (BuildCompat.isAtLeastV()) {
            BiometricPromptData biometricPromptData = null;
            if (!nullBiometricPromptData) {
                biometricPromptData = testBiometricPromptData();
            }
            testBuilder.setBiometricPromptData(biometricPromptData);
        }
        return testBuilder.build();
    }

    private void assertEntryWithAllParams(CreateEntry entry) {
        assertThat(ACCOUNT_NAME).isEqualTo(entry.getAccountName());
        assertThat(mPendingIntent).isEqualTo(entry.getPendingIntent());
        assertThat(ICON).isEqualTo(entry.getIcon());
        assertThat(Instant.ofEpochMilli(LAST_USED_TIME)).isEqualTo(entry.getLastUsedTime());
        assertThat(PASSWORD_COUNT).isEqualTo(entry.getPasswordCredentialCount());
        assertThat(PUBLIC_KEY_CREDENTIAL_COUNT).isEqualTo(entry.getPublicKeyCredentialCount());
        assertThat(TOTAL_COUNT).isEqualTo(entry.getTotalCredentialCount());
        if (BuildCompat.isAtLeastV() && entry.getBiometricPromptData() != null) {
            assertAboveApiV(entry);
        } else {
            assertThat(entry.getBiometricPromptData()).isNull();
        }
    }

    private static void assertAboveApiV(CreateEntry entry) {
        if (BuildCompat.isAtLeastV()) {
            assertThat(entry.getBiometricPromptData().getAllowedAuthenticators()).isEqualTo(
                    testBiometricPromptData().getAllowedAuthenticators());
        }
    }
}
