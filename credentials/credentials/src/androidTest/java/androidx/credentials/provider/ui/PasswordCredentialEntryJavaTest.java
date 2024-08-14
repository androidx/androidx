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

import static androidx.credentials.CredentialOption.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED;
import static androidx.credentials.provider.ui.UiUtils.testBiometricPromptData;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.credentials.CredentialEntry;

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
@SdkSuppress(minSdkVersion = 26) // Instant usage
@SmallTest
public class PasswordCredentialEntryJavaTest {
    private static final CharSequence USERNAME = "title";
    private static final CharSequence DISPLAYNAME = "subtitle";
    private static final CharSequence TYPE_DISPLAY_NAME = "Password";
    private static final String AFFILIATED_DOMAIN = "affiliation-name";
    private static final Long LAST_USED_TIME = 10L;
    private static final boolean DEFAULT_SINGLE_PROVIDER_ICON_BIT = false;
    private static final boolean SINGLE_PROVIDER_ICON_BIT = true;
    private static final boolean IS_AUTO_SELECT_ALLOWED = true;
    private static final Icon ICON = Icon.createWithBitmap(Bitmap.createBitmap(
            100, 100, Bitmap.Config.ARGB_8888));
    private final BeginGetPasswordOption mBeginGetPasswordOption = new BeginGetPasswordOption(
            new HashSet<>(),
            new Bundle(), "id");
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final Intent mIntent = new Intent();
    private final PendingIntent mPendingIntent =
            PendingIntent.getActivity(mContext, 0, mIntent,
                    PendingIntent.FLAG_IMMUTABLE);
    @Test
    public void build_requiredParams_success() {
        PasswordCredentialEntry entry = constructEntryWithRequiredParamsOnly();
        assertNotNull(entry);
        assertThat(entry.getType()).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL);
        assertEntryWithRequiredParamsOnly(entry, false);
    }
    @Test
    public void build_allParams_success() {
        PasswordCredentialEntry entry = constructEntryWithAllParams();
        assertNotNull(entry);
        assertThat(entry.getType()).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL);
        assertEntryWithAllParams(entry);
    }
    @Test
    public void build_nullContext_throwsNPE() {
        assertThrows("Expected null context to throw NPE",
                NullPointerException.class,
                () -> new PasswordCredentialEntry.Builder(
                        null, USERNAME, mPendingIntent, mBeginGetPasswordOption
                ).build());
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    @SuppressWarnings("deprecation")
    public void isDefaultIcon_customIconSetFromSlice_returnsFalse() {
        PasswordCredentialEntry entry = new PasswordCredentialEntry.Builder(
                mContext,
                USERNAME,
                mPendingIntent,
                mBeginGetPasswordOption
        ).setIcon(ICON).build();

        android.app.slice.Slice slice = PasswordCredentialEntry.toSlice(entry);

        assertNotNull(slice);

        PasswordCredentialEntry entryFromSlice = PasswordCredentialEntry
                .fromSlice(slice);

        assertNotNull(entryFromSlice);
        assertFalse(entryFromSlice.hasDefaultIcon());
        assertFalse(entry.hasDefaultIcon());
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    @SuppressWarnings("deprecation")
    public void isDefaultIcon_noIconSetFromSlice_returnsTrue() {
        PasswordCredentialEntry entry = new PasswordCredentialEntry.Builder(
                mContext,
                USERNAME,
                mPendingIntent,
                mBeginGetPasswordOption
        ).build();

        android.app.slice.Slice slice = PasswordCredentialEntry.toSlice(entry);
        assertNotNull(slice);
        PasswordCredentialEntry entryFromSlice = PasswordCredentialEntry
                .fromSlice(slice);

        assertNotNull(entryFromSlice);
        assertTrue(entryFromSlice.hasDefaultIcon());
        assertTrue(entry.hasDefaultIcon());
    }

    @Test
    public void build_nullUsername_throwsNPE() {
        assertThrows("Expected null username to throw NPE",
                NullPointerException.class,
                () -> new PasswordCredentialEntry.Builder(
                        mContext, null, mPendingIntent, mBeginGetPasswordOption
                ).build());
    }
    @Test
    public void build_nullPendingIntent_throwsNPE() {
        assertThrows("Expected null pending intent to throw NPE",
                NullPointerException.class,
                () -> new PasswordCredentialEntry.Builder(
                        mContext, USERNAME, null, mBeginGetPasswordOption
                ).build());
    }
    @Test
    public void build_nullBeginOption_throwsNPE() {
        assertThrows("Expected null option to throw NPE",
                NullPointerException.class,
                () -> new PasswordCredentialEntry.Builder(
                        mContext, USERNAME, mPendingIntent, null
                ).build());
    }
    @Test
    public void build_emptyUsername_throwsIAE() {
        assertThrows("Expected empty username to throw IllegalArgumentException",
                IllegalArgumentException.class,
                () -> new PasswordCredentialEntry.Builder(
                        mContext, "", mPendingIntent, mBeginGetPasswordOption).build());
    }
    @Test
    public void build_nullIcon_defaultIconSet() {
        PasswordCredentialEntry entry = new PasswordCredentialEntry
                .Builder(mContext, USERNAME, mPendingIntent, mBeginGetPasswordOption).build();
        assertThat(TestUtilsKt.equals(entry.getIcon(),
                Icon.createWithResource(mContext, R.drawable.ic_password))).isTrue();
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void isDefaultIcon_noIconSet_returnsTrue() {
        PasswordCredentialEntry entry = new PasswordCredentialEntry
                .Builder(mContext, USERNAME, mPendingIntent, mBeginGetPasswordOption).build();

        assertTrue(entry.hasDefaultIcon());
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void isDefaultIcon_customIcon_returnsFalse() {
        PasswordCredentialEntry entry = new PasswordCredentialEntry
                .Builder(mContext, USERNAME, mPendingIntent, mBeginGetPasswordOption)
                .setIcon(ICON).build();

        assertFalse(entry.hasDefaultIcon());
    }

    @Test
    public void isAutoSelectAllowedFromOption_optionAllows_returnsTrue() {
        mBeginGetPasswordOption.getCandidateQueryData().putBoolean(
                BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED, true);
        PasswordCredentialEntry entry = new PasswordCredentialEntry
                .Builder(mContext, USERNAME, mPendingIntent, mBeginGetPasswordOption).build();

        assertTrue(entry.isAutoSelectAllowedFromOption());
    }

    @Test
    public void isAutoSelectAllowedFromOption_optionDisallows_returnsFalse() {
        PasswordCredentialEntry entry = new PasswordCredentialEntry
                .Builder(mContext, USERNAME, mPendingIntent, mBeginGetPasswordOption).build();

        assertFalse(entry.isAutoSelectAllowedFromOption());
    }

    @Test
    public void build_nullTypeDisplayName_defaultDisplayNameSet() {
        PasswordCredentialEntry entry = new PasswordCredentialEntry.Builder(
                mContext, USERNAME, mPendingIntent, mBeginGetPasswordOption).build();
        assertThat(entry.getTypeDisplayName()).isEqualTo(
                mContext.getString(
                        R.string.android_credentials_TYPE_PASSWORD_CREDENTIAL)
        );
    }
    @Test
    public void build_isAutoSelectAllowedDefault_false() {
        PasswordCredentialEntry entry = constructEntryWithRequiredParamsOnly();

        assertFalse(entry.isAutoSelectAllowed());
    }
    @Test
    public void constructor_defaultAffiliatedDomain() {
        PasswordCredentialEntry entry = constructEntryWithRequiredParamsOnly();

        assertThat(entry.getAffiliatedDomain()).isNull();
    }

    @Test
    public void constructor_nonEmptyAffiliatedDomainSet_nonEmptyAffiliatedDomainRetrieved() {
        String expectedAffiliatedDomain = "non-empty";

        PasswordCredentialEntry entryWithAffiliatedDomain = new PasswordCredentialEntry(
                mContext,
                USERNAME,
                mPendingIntent,
                mBeginGetPasswordOption,
                DISPLAYNAME,
                Instant.ofEpochMilli(LAST_USED_TIME),
                ICON,
                false,
                expectedAffiliatedDomain,
                false
        );

        assertThat(entryWithAffiliatedDomain.getAffiliatedDomain())
                .isEqualTo(expectedAffiliatedDomain);
    }
    @Test
    @SdkSuppress(minSdkVersion = 34)
    public void builder_constructDefault_containsOnlyDefaultValuesForSettableParameters() {
        PasswordCredentialEntry entry = new PasswordCredentialEntry.Builder(mContext, USERNAME,
                mPendingIntent, mBeginGetPasswordOption).build();

        assertThat(entry.getAffiliatedDomain()).isNull();
        assertThat(entry.getDisplayName()).isNull();
        assertThat(entry.getLastUsedTime()).isNull();
        assertThat(entry.isAutoSelectAllowed()).isFalse();
        assertThat(entry.getEntryGroupId()).isEqualTo(USERNAME);
        assertThat(entry.getBiometricPromptData()).isNull();
    }
    @Test
    public void builder_setAffiliatedDomainNull_retrieveNullAffiliatedDomain() {
        PasswordCredentialEntry entry = new PasswordCredentialEntry.Builder(mContext, USERNAME,
                mPendingIntent, mBeginGetPasswordOption).setAffiliatedDomain(null).build();

        assertThat(entry.getAffiliatedDomain()).isNull();
    }
    @Test
    public void builder_setAffiliatedDomainNonNull_retrieveNonNullAffiliatedDomain() {
        String expectedAffiliatedDomain = "affiliated-domain";

        PasswordCredentialEntry entry = new PasswordCredentialEntry.Builder(
                mContext,
                USERNAME,
                mPendingIntent,
                mBeginGetPasswordOption
        ).setAffiliatedDomain(expectedAffiliatedDomain).build();

        assertThat(entry.getAffiliatedDomain()).isEqualTo(expectedAffiliatedDomain);
    }
    @Test
    public void builder_setPreferredDefaultIconBit_retrieveSetIconBit() {
        boolean expectedPreferredDefaultIconBit = SINGLE_PROVIDER_ICON_BIT;

        PasswordCredentialEntry entry = new PasswordCredentialEntry.Builder(
                mContext,
                USERNAME,
                mPendingIntent,
                mBeginGetPasswordOption
        ).setDefaultIconPreferredAsSingleProvider(expectedPreferredDefaultIconBit)
                .build();

        assertThat(entry.isDefaultIconPreferredAsSingleProvider())
                .isEqualTo(expectedPreferredDefaultIconBit);
    }
    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void fromSlice_requiredParams_success() {
        PasswordCredentialEntry originalEntry = constructEntryWithRequiredParamsOnly();
        PasswordCredentialEntry entry = PasswordCredentialEntry.fromSlice(
                PasswordCredentialEntry.toSlice(originalEntry));
        assertNotNull(entry);
        assertEntryWithRequiredParamsOnly(entry, true);
    }
    @Test
    @SdkSuppress(minSdkVersion = 28)
    public void fromSlice_allParams_success() {
        PasswordCredentialEntry originalEntry = constructEntryWithAllParams();
        PasswordCredentialEntry entry = PasswordCredentialEntry.fromSlice(
                PasswordCredentialEntry.toSlice(originalEntry));
        assertNotNull(entry);
        assertEntryWithAllParams(entry);
    }
    @Test
    @SdkSuppress(minSdkVersion = 34)
    public void fromCredentialEntry_allParams_success() {
        PasswordCredentialEntry originalEntry = constructEntryWithAllParams();
        PasswordCredentialEntry entry = PasswordCredentialEntry.fromCredentialEntry(
                new CredentialEntry("id",
                        PasswordCredentialEntry.toSlice(originalEntry)));
        assertNotNull(entry);
        assertEntryWithAllParams(entry);
    }
    private PasswordCredentialEntry constructEntryWithRequiredParamsOnly() {
        return new PasswordCredentialEntry.Builder(
                mContext,
                USERNAME,
                mPendingIntent,
                mBeginGetPasswordOption).build();
    }

    private PasswordCredentialEntry constructEntryWithAllParams() {
        if (BuildCompat.isAtLeastV()) {
            return new PasswordCredentialEntry.Builder(
                    mContext, USERNAME, mPendingIntent, mBeginGetPasswordOption)
                    .setDisplayName(DISPLAYNAME)
                    .setLastUsedTime(Instant.ofEpochMilli(LAST_USED_TIME))
                    .setIcon(ICON)
                    .setAutoSelectAllowed(IS_AUTO_SELECT_ALLOWED)
                    .setAffiliatedDomain(AFFILIATED_DOMAIN)
                    .setDefaultIconPreferredAsSingleProvider(SINGLE_PROVIDER_ICON_BIT)
                    .setBiometricPromptData(testBiometricPromptData()).build();
        } else {
            return new PasswordCredentialEntry.Builder(
                    mContext, USERNAME, mPendingIntent, mBeginGetPasswordOption)
                    .setDisplayName(DISPLAYNAME)
                    .setLastUsedTime(Instant.ofEpochMilli(LAST_USED_TIME))
                    .setIcon(ICON)
                    .setAutoSelectAllowed(IS_AUTO_SELECT_ALLOWED)
                    .setAffiliatedDomain(AFFILIATED_DOMAIN)
                    .setDefaultIconPreferredAsSingleProvider(SINGLE_PROVIDER_ICON_BIT)
                    .build();
        }
    }

    private void assertEntryWithRequiredParamsOnly(PasswordCredentialEntry entry,
            Boolean assertOptionIdOnly) {
        assertThat(USERNAME.equals(entry.getUsername()));
        assertThat(mPendingIntent).isEqualTo(entry.getPendingIntent());
        assertThat(mBeginGetPasswordOption.getType()).isEqualTo(entry.getType());
        assertThat(entry.getAffiliatedDomain()).isNull();
        assertThat(entry.isDefaultIconPreferredAsSingleProvider()).isEqualTo(
                DEFAULT_SINGLE_PROVIDER_ICON_BIT);
        assertThat(entry.getEntryGroupId()).isEqualTo(USERNAME);
        assertThat(entry.getBiometricPromptData()).isNull();
    }
    private void assertEntryWithAllParams(PasswordCredentialEntry entry) {
        assertThat(USERNAME.equals(entry.getUsername()));
        assertThat(DISPLAYNAME.equals(entry.getDisplayName()));
        assertThat(TYPE_DISPLAY_NAME.equals(entry.getTypeDisplayName()));
        assertThat(ICON).isEqualTo(entry.getIcon());
        assertThat(IS_AUTO_SELECT_ALLOWED).isEqualTo(entry.isAutoSelectAllowed());
        assertThat(Instant.ofEpochMilli(LAST_USED_TIME)).isEqualTo(entry.getLastUsedTime());
        assertThat(mPendingIntent).isEqualTo(entry.getPendingIntent());
        assertThat(mBeginGetPasswordOption.getType()).isEqualTo(entry.getType());
        assertThat(entry.getAffiliatedDomain()).isEqualTo(AFFILIATED_DOMAIN);
        assertThat(entry.isDefaultIconPreferredAsSingleProvider()).isEqualTo(
                SINGLE_PROVIDER_ICON_BIT);
        assertThat(entry.getEntryGroupId()).isEqualTo(USERNAME);
        if (BuildCompat.isAtLeastV() && entry.getBiometricPromptData() != null) {
            assertThat(entry.getBiometricPromptData().getAllowedAuthenticators()).isEqualTo(
                    testBiometricPromptData().getAllowedAuthenticators());
        } else {
            assertThat(entry.getBiometricPromptData()).isNull();
        }
    }
}
