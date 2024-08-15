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
package androidx.credentials.provider.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Bundle
import android.service.credentials.CredentialEntry
import androidx.core.os.BuildCompat
import androidx.credentials.CredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.R
import androidx.credentials.equals
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.PasswordCredentialEntry.Companion.fromSlice
import androidx.credentials.provider.ui.UiUtils.Companion.testBiometricPromptData
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import org.junit.Assert
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26) // Instant usage
@SmallTest
class PasswordCredentialEntryTest {
    private val mContext = ApplicationProvider.getApplicationContext<Context>()
    private val mIntent = Intent()
    private val mPendingIntent =
        PendingIntent.getActivity(mContext, 0, mIntent, PendingIntent.FLAG_IMMUTABLE)

    @Test
    fun constructor_requiredParams_success() {
        val entry = constructEntryWithRequiredParamsOnly()
        assertNotNull(entry)
        assertThat(entry.type).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL)
        assertEntryWithRequiredParamsOnly(entry)
    }

    @Test
    fun constructor_allParams_success() {
        val entry = constructEntryWithAllParams()
        assertNotNull(entry)
        assertThat(entry.type).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL)
        assertEntryWithAllParams(entry)
    }

    @Test
    @Suppress("DEPRECATION")
    fun constructor_emptyUsername_throwsIAE() {
        assertThrows(
            "Expected empty username to throw IllegalArgumentException",
            IllegalArgumentException::class.java
        ) {
            PasswordCredentialEntry(mContext, "", mPendingIntent, BEGIN_OPTION)
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun constructor_nullIcon_defaultIconSet() {
        val entry =
            PasswordCredentialEntry.Builder(mContext, USERNAME, mPendingIntent, BEGIN_OPTION)
                .build()
        assertThat(equals(entry.icon, Icon.createWithResource(mContext, R.drawable.ic_password)))
            .isTrue()
        Assert.assertTrue(entry.hasDefaultIcon)
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun isDefaultIcon_noIconSet_returnsTrue() {
        val entry =
            PasswordCredentialEntry.Builder(mContext, USERNAME, mPendingIntent, BEGIN_OPTION)
                .build()

        Assert.assertTrue(entry.hasDefaultIcon)
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun isDefaultIcon_customIconSetFromSlice_returnsFalse() {
        val entry =
            PasswordCredentialEntry.Builder(mContext, USERNAME, mPendingIntent, BEGIN_OPTION)
                .setIcon(ICON)
                .build()

        val slice = PasswordCredentialEntry.toSlice(entry)
        assertNotNull(slice)

        val entryFromSlice = fromSlice(slice!!)

        Assert.assertNotNull(entryFromSlice)
        Assert.assertFalse(entryFromSlice!!.hasDefaultIcon)
        Assert.assertFalse(entry.hasDefaultIcon)
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun isDefaultIcon_noIconSetFromSlice_returnsTrue() {
        val entry =
            PasswordCredentialEntry.Builder(mContext, USERNAME, mPendingIntent, BEGIN_OPTION)
                .build()

        val slice = PasswordCredentialEntry.toSlice(entry)
        assertNotNull(slice)
        val entryFromSlice = fromSlice(slice!!)

        Assert.assertNotNull(entryFromSlice)
        Assert.assertTrue(entryFromSlice!!.hasDefaultIcon)
        Assert.assertTrue(entry.hasDefaultIcon)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun isDefaultIcon_customIcon_returnsFalse() {
        val entry =
            PasswordCredentialEntry.Builder(mContext, USERNAME, mPendingIntent, BEGIN_OPTION)
                .setIcon(ICON)
                .build()
        Assert.assertFalse(entry.hasDefaultIcon)
    }

    @Test
    fun isAutoSelectAllowedFromOption_optionAllows_returnsTrue() {
        BEGIN_OPTION.candidateQueryData.putBoolean(
            CredentialOption.BUNDLE_KEY_IS_AUTO_SELECT_ALLOWED,
            true
        )
        val entry =
            PasswordCredentialEntry.Builder(mContext, USERNAME, mPendingIntent, BEGIN_OPTION)
                .build()
        Assert.assertTrue(entry.isAutoSelectAllowedFromOption)
    }

    @Test
    fun isAutoSelectAllowedFromOption_optionDisallows_returnsFalse() {
        val entry =
            PasswordCredentialEntry.Builder(mContext, USERNAME, mPendingIntent, BEGIN_OPTION)
                .build()
        Assert.assertFalse(entry.isAutoSelectAllowedFromOption)
    }

    @Test
    @Suppress("DEPRECATION")
    fun constructor_nullTypeDisplayName_defaultDisplayNameSet() {
        val entry = PasswordCredentialEntry(mContext, USERNAME, mPendingIntent, BEGIN_OPTION)
        assertThat(entry.typeDisplayName)
            .isEqualTo(mContext.getString(R.string.android_credentials_TYPE_PASSWORD_CREDENTIAL))
    }

    @Test
    fun constructor_isAutoSelectAllowedDefault_false() {
        val entry = constructEntryWithRequiredParamsOnly()
        val entry1 = constructEntryWithAllParams()
        assertFalse(entry.isAutoSelectAllowed)
        assertFalse(entry1.isAutoSelectAllowed)
    }

    @Test
    fun constructor_defaultAffiliatedDomain() {
        val defaultEntry = constructEntryWithRequiredParamsOnly()

        assertThat(defaultEntry.affiliatedDomain).isNull()
    }

    @Test
    fun constructor_defaultBiometricPromptData() {
        val defaultEntry = constructEntryWithRequiredParamsOnly()

        assertThat(defaultEntry.biometricPromptData).isNull()
    }

    @Test
    fun constructor_nonEmptyAffiliatedDomainSet_nonEmptyAffiliatedDomainRetrieved() {
        val expectedAffiliatedDomain = "non-empty"

        val entryWithAffiliationType =
            PasswordCredentialEntry(
                mContext,
                USERNAME,
                mPendingIntent,
                BEGIN_OPTION,
                DISPLAYNAME,
                LAST_USED_TIME,
                ICON,
                affiliatedDomain = expectedAffiliatedDomain
            )

        assertThat(entryWithAffiliationType.affiliatedDomain).isEqualTo(expectedAffiliatedDomain)
    }

    @Test
    fun constructor_setPreferredDefaultIconBit_retrieveSetPreferredDefaultIconBit() {
        val expectedPreferredDefaultIconBit = SINGLE_PROVIDER_ICON_BIT
        val entry =
            PasswordCredentialEntry(
                mContext,
                USERNAME,
                mPendingIntent,
                BEGIN_OPTION,
                DISPLAYNAME,
                LAST_USED_TIME,
                ICON,
                isDefaultIconPreferredAsSingleProvider = expectedPreferredDefaultIconBit
            )

        assertThat(entry.isDefaultIconPreferredAsSingleProvider)
            .isEqualTo(expectedPreferredDefaultIconBit)
    }

    @Test
    fun constructor_preferredIconBitNotProvided_retrieveDefaultPreferredIconBit() {
        val entry = PasswordCredentialEntry(mContext, USERNAME, mPendingIntent, BEGIN_OPTION)

        assertThat(entry.isDefaultIconPreferredAsSingleProvider)
            .isEqualTo(DEFAULT_SINGLE_PROVIDER_ICON_BIT)
    }

    @Test
    fun constructor_allRequiredParamsUsed_defaultUsernameEntryGroupIdRetrieved() {
        val entry = constructEntryWithAllParams()

        assertThat(entry.entryGroupId).isEqualTo(USERNAME)
    }

    @Test
    fun builder_constructDefault_containsOnlySetPropertiesAndDefaultValues() {
        val entry =
            PasswordCredentialEntry.Builder(mContext, USERNAME, mPendingIntent, BEGIN_OPTION)
                .build()

        assertThat(entry.username).isEqualTo(USERNAME)
        assertThat(entry.displayName).isNull()
        assertThat(entry.typeDisplayName)
            .isEqualTo(mContext.getString(R.string.android_credentials_TYPE_PASSWORD_CREDENTIAL))
        assertThat(entry.pendingIntent).isEqualTo(mPendingIntent)
        assertThat(entry.lastUsedTime).isNull()
        assertThat(entry.icon.toString())
            .isEqualTo(Icon.createWithResource(mContext, R.drawable.ic_password).toString())
        assertThat(entry.isAutoSelectAllowed).isFalse()
        assertThat(entry.beginGetCredentialOption).isEqualTo(BEGIN_OPTION)
        assertThat(entry.affiliatedDomain).isNull()
        assertThat(entry.entryGroupId).isEqualTo(USERNAME)
        assertThat(entry.biometricPromptData).isNull()
    }

    @Test
    fun builder_setAffiliatedDomainNull_retrieveNullAffiliatedDomain() {
        val entry =
            PasswordCredentialEntry.Builder(mContext, USERNAME, mPendingIntent, BEGIN_OPTION)
                .setAffiliatedDomain(null)
                .build()

        assertThat(entry.affiliatedDomain).isNull()
    }

    @Test
    fun builder_setAffiliatedDomainNonNull_retrieveNonNullAffiliatedDomain() {
        val expectedAffiliatedDomain = "name"

        val entry =
            PasswordCredentialEntry.Builder(mContext, USERNAME, mPendingIntent, BEGIN_OPTION)
                .setAffiliatedDomain(expectedAffiliatedDomain)
                .build()

        assertThat(entry.affiliatedDomain).isEqualTo(expectedAffiliatedDomain)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun fromSlice_success() {
        val originalEntry = constructEntryWithAllParams()
        val slice = PasswordCredentialEntry.toSlice(originalEntry)
        assertNotNull(slice)
        val entry = fromSlice(slice!!)
        assertNotNull(entry)
        entry?.let { assertEntryWithAllParams(entry) }
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun fromCredentialEntry_success() {
        val originalEntry = constructEntryWithAllParams()
        val slice = PasswordCredentialEntry.toSlice(originalEntry)
        assertNotNull(slice)
        val entry =
            slice
                ?.let { CredentialEntry("id", it) }
                ?.let { PasswordCredentialEntry.fromCredentialEntry(it) }
        assertNotNull(entry)
        entry?.let { assertEntryWithAllParams(entry) }
    }

    @Suppress("DEPRECATION")
    private fun constructEntryWithRequiredParamsOnly(): PasswordCredentialEntry {
        return PasswordCredentialEntry(mContext, USERNAME, mPendingIntent, BEGIN_OPTION)
    }

    private fun constructEntryWithAllParams(): PasswordCredentialEntry {
        return if (BuildCompat.isAtLeastV()) {
            PasswordCredentialEntry(
                mContext,
                USERNAME,
                mPendingIntent,
                BEGIN_OPTION,
                DISPLAYNAME,
                LAST_USED_TIME,
                ICON,
                IS_AUTO_SELECT_ALLOWED,
                AFFILIATED_DOMAIN,
                SINGLE_PROVIDER_ICON_BIT,
                testBiometricPromptData(),
            )
        } else {
            PasswordCredentialEntry(
                mContext,
                USERNAME,
                mPendingIntent,
                BEGIN_OPTION,
                DISPLAYNAME,
                LAST_USED_TIME,
                ICON,
                IS_AUTO_SELECT_ALLOWED,
                AFFILIATED_DOMAIN,
                SINGLE_PROVIDER_ICON_BIT,
            )
        }
    }

    private fun assertEntryWithRequiredParamsOnly(entry: PasswordCredentialEntry) {
        assertThat(USERNAME == entry.username)
        assertThat(mPendingIntent).isEqualTo(entry.pendingIntent)
        assertThat(entry.affiliatedDomain).isNull()
        assertThat(entry.isDefaultIconPreferredAsSingleProvider)
            .isEqualTo(DEFAULT_SINGLE_PROVIDER_ICON_BIT)
        assertThat(entry.entryGroupId).isEqualTo(USERNAME)
        assertThat(entry.biometricPromptData).isNull()
    }

    private fun assertEntryWithAllParams(entry: PasswordCredentialEntry) {
        assertThat(USERNAME == entry.username)
        assertThat(DISPLAYNAME == entry.displayName)
        assertThat(TYPE_DISPLAY_NAME == entry.typeDisplayName)
        assertThat(ICON).isEqualTo(entry.icon)
        assertNotNull(entry.lastUsedTime)
        entry.lastUsedTime?.let {
            assertThat(LAST_USED_TIME.toEpochMilli()).isEqualTo(it.toEpochMilli())
        }
        assertThat(mPendingIntent).isEqualTo(entry.pendingIntent)
        assertThat(entry.isAutoSelectAllowed).isEqualTo(IS_AUTO_SELECT_ALLOWED)
        assertThat(entry.affiliatedDomain).isEqualTo(AFFILIATED_DOMAIN)
        assertThat(entry.isDefaultIconPreferredAsSingleProvider).isEqualTo(SINGLE_PROVIDER_ICON_BIT)
        assertThat(entry.entryGroupId).isEqualTo(USERNAME)
        if (BuildCompat.isAtLeastV()) {
            // TODO(b/325469910) : Add cryptoObject tests once opId is retrievable
            assertThat(entry.biometricPromptData!!.allowedAuthenticators)
                .isEqualTo(testBiometricPromptData().allowedAuthenticators)
        } else {
            assertThat(entry.biometricPromptData).isNull()
        }
    }

    companion object {
        private val USERNAME: CharSequence = "title"
        private val DISPLAYNAME: CharSequence = "subtitle"
        private val TYPE_DISPLAY_NAME: CharSequence = "Password"
        private val LAST_USED_TIME = Instant.now()
        private val BEGIN_OPTION = BeginGetPasswordOption(emptySet<String>(), Bundle(), "id")
        private val ICON =
            Icon.createWithBitmap(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))
        private val IS_AUTO_SELECT_ALLOWED = false
        private val AFFILIATED_DOMAIN = "affiliation-name"
        private const val DEFAULT_SINGLE_PROVIDER_ICON_BIT = false
        private const val SINGLE_PROVIDER_ICON_BIT = true
    }
}
