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
import androidx.credentials.R
import androidx.credentials.equals
import androidx.credentials.provider.BeginGetCredentialOption
import androidx.credentials.provider.BeginGetCustomCredentialOption
import androidx.credentials.provider.CustomCredentialEntry
import androidx.credentials.provider.CustomCredentialEntry.Companion.fromCredentialEntry
import androidx.credentials.provider.CustomCredentialEntry.Companion.fromSlice
import androidx.credentials.provider.CustomCredentialEntry.Companion.toSlice
import androidx.credentials.provider.ui.UiUtils.Companion.testBiometricPromptData
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Assert
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26) // Instant usage
@SmallTest
class CustomCredentialEntryTest {
    private val mContext = ApplicationProvider.getApplicationContext<Context>()
    private val mIntent = Intent()
    private val mPendingIntent =
        PendingIntent.getActivity(mContext, 0, mIntent, PendingIntent.FLAG_IMMUTABLE)

    @Test
    fun constructor_requiredParams_success() {
        val entry = constructEntryWithRequiredParams()
        assertNotNull(entry)
        assertEntryWithRequiredParams(entry)
    }

    @Test
    fun constructor_allParams_success() {
        val entry = constructEntryWithAllParams()
        assertNotNull(entry)
        assertEntryWithAllParams(entry)
    }

    @Test
    fun constructor_allParameters_success() {
        val entry: CustomCredentialEntry = constructEntryWithAllParams()
        assertNotNull(entry)
        assertEntryWithAllParams(entry)
    }

    @Test
    fun constructor_emptyTitle_throwsIAE() {
        assertThrows("Expected empty title to throw NPE", IllegalArgumentException::class.java) {
            CustomCredentialEntry(
                mContext,
                TITLE,
                mPendingIntent,
                BeginGetCustomCredentialOption("id", "", Bundle.EMPTY)
            )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun constructor_emptyType_throwsIAE() {
        assertThrows("Expected empty type to throw NPE", IllegalArgumentException::class.java) {
            CustomCredentialEntry(
                mContext,
                TITLE,
                mPendingIntent,
                BeginGetCustomCredentialOption("id", "", Bundle.EMPTY)
            )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun constructor_nullIcon_defaultIconSet() {
        val entry = constructEntryWithRequiredParams()
        assertThat(
                equals(entry.icon, Icon.createWithResource(mContext, R.drawable.ic_other_sign_in))
            )
            .isTrue()
    }

    @Test
    fun constructor_setPreferredDefaultIconBit_retrieveSetPreferredDefaultIconBit() {
        val expectedPreferredDefaultIconBit = SINGLE_PROVIDER_ICON_BIT
        val entry =
            CustomCredentialEntry(
                mContext,
                TITLE,
                mPendingIntent,
                BEGIN_OPTION,
                isDefaultIconPreferredAsSingleProvider = SINGLE_PROVIDER_ICON_BIT
            )
        assertThat(entry.isDefaultIconPreferredAsSingleProvider)
            .isEqualTo(expectedPreferredDefaultIconBit)
    }

    @Test
    fun constructor_preferredIconBitNotProvided_retrieveDefaultPreferredIconBit() {
        val entry =
            CustomCredentialEntry(
                mContext,
                TITLE,
                mPendingIntent,
                BEGIN_OPTION,
            )
        assertThat(entry.isDefaultIconPreferredAsSingleProvider)
            .isEqualTo(DEFAULT_SINGLE_PROVIDER_ICON_BIT)
    }

    @Test
    fun constructor_emptyEntryGroupId_defaultEntryGroupIdSet() {
        val expectedEntryGroupId = TITLE

        val entry =
            CustomCredentialEntry(
                mContext,
                expectedEntryGroupId,
                mPendingIntent,
                BEGIN_OPTION,
                entryGroupId = ""
            )

        assertThat(entry.entryGroupId).isEqualTo(expectedEntryGroupId)
    }

    @Test
    fun constructor_nonEmptyEntryGroupIdSet_getSetEntryGroupId() {
        val expectedEntryGroupId = "expected-dedupe"

        val entry =
            CustomCredentialEntry(
                mContext,
                expectedEntryGroupId,
                mPendingIntent,
                BEGIN_OPTION,
                entryGroupId = expectedEntryGroupId
            )

        assertThat(entry.entryGroupId).isEqualTo(expectedEntryGroupId)
    }

    @Test
    fun constructor_entryGroupIdNotProvided_getDefaultTitle() {
        val entry =
            CustomCredentialEntry(
                mContext,
                TITLE,
                mPendingIntent,
                BEGIN_OPTION,
            )

        assertThat(entry.entryGroupId).isEqualTo(TITLE)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun builder_constructDefault_containsOnlySetPropertiesAndDefaultValues() {
        val entry =
            CustomCredentialEntry.Builder(mContext, TYPE, TITLE, mPendingIntent, BEGIN_OPTION)
                .build()

        assertThat(entry.title).isEqualTo(TITLE)
        assertThat(entry.pendingIntent).isEqualTo(mPendingIntent)
        assertThat(entry.beginGetCredentialOption).isEqualTo(BEGIN_OPTION)
        assertThat(entry.subtitle).isNull()
        assertThat(entry.typeDisplayName).isNull()
        assertThat(entry.lastUsedTime).isNull()
        assertThat(entry.icon.toString())
            .isEqualTo(Icon.createWithResource(mContext, R.drawable.ic_other_sign_in).toString())
        assertThat(entry.isAutoSelectAllowed).isFalse()
        assertThat(entry.affiliatedDomain).isNull()
        assertThat(entry.entryGroupId).isEqualTo(TITLE)
        assertThat(entry.isDefaultIconPreferredAsSingleProvider)
            .isEqualTo(DEFAULT_SINGLE_PROVIDER_ICON_BIT)
        assertThat(entry.biometricPromptData).isNull()
    }

    @Test
    fun builder_setNonEmpyDeduplicationId_retrieveSetDeduplicationId() {
        val expectedIconBit = SINGLE_PROVIDER_ICON_BIT
        val entry =
            CustomCredentialEntry.Builder(mContext, TYPE, TITLE, mPendingIntent, BEGIN_OPTION)
                .setDefaultIconPreferredAsSingleProvider(SINGLE_PROVIDER_ICON_BIT)
                .build()
        assertThat(entry.isDefaultIconPreferredAsSingleProvider).isEqualTo(expectedIconBit)
    }

    @Test
    fun builder_setEmptyEntryGroupId_throwIAE() {
        assertThrows(
            "Expected empty dedupe id in setter to throw IAE",
            IllegalArgumentException::class.java
        ) {
            CustomCredentialEntry.Builder(mContext, TYPE, TITLE, mPendingIntent, BEGIN_OPTION)
                .setEntryGroupId("")
                .build()
        }
    }

    @Test
    fun builder_setNonEmpyEntryGroupId_retrieveSetEntryGroupId() {
        val expectedEntryGroupId = "noe-valley"

        val entry =
            CustomCredentialEntry.Builder(mContext, TYPE, TITLE, mPendingIntent, BEGIN_OPTION)
                .setEntryGroupId(expectedEntryGroupId)
                .build()

        assertThat(entry.entryGroupId).isEqualTo(expectedEntryGroupId)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun fromSlice_requiredParams_success() {
        val originalEntry = constructEntryWithRequiredParams()
        val slice = toSlice(originalEntry)
        assertNotNull(slice)
        val entry = fromSlice(slice!!)
        assertNotNull(entry)
        if (entry != null) {
            assertEntryWithRequiredParamsFromSlice(entry)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun fromSlice_allParams_success() {
        val originalEntry = constructEntryWithAllParams()
        val slice = CustomCredentialEntry.toSlice(originalEntry)
        assertNotNull(slice)
        val entry = fromSlice(slice!!)
        assertNotNull(entry)
        if (entry != null) {
            assertEntryWithAllParamsFromSlice(entry)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun fromCredentialEntry_allParams_success() {
        val originalEntry = constructEntryWithAllParams()
        val slice = toSlice(originalEntry)
        assertNotNull(slice)
        val entry = slice?.let { CredentialEntry("id", it) }?.let { fromCredentialEntry(it) }
        assertNotNull(entry)
        assertEntryWithAllParamsFromSlice(entry!!)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun isDefaultIcon_noIconSet_returnsTrue() {
        val entry =
            CustomCredentialEntry.Builder(mContext, TYPE, TITLE, mPendingIntent, BEGIN_OPTION)
                .build()
        Assert.assertTrue(entry.hasDefaultIcon)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun isDefaultIcon_customIcon_returnsFalse() {
        val entry =
            CustomCredentialEntry.Builder(mContext, TYPE, TITLE, mPendingIntent, BEGIN_OPTION)
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
            CustomCredentialEntry.Builder(mContext, TYPE, TITLE, mPendingIntent, BEGIN_OPTION)
                .build()
        Assert.assertTrue(entry.isAutoSelectAllowedFromOption)
    }

    @Test
    fun isAutoSelectAllowedFromOption_optionDisallows_returnsFalse() {
        val entry =
            CustomCredentialEntry.Builder(mContext, TYPE, TITLE, mPendingIntent, BEGIN_OPTION)
                .build()
        Assert.assertFalse(entry.isAutoSelectAllowedFromOption)
    }

    private fun constructEntryWithRequiredParams(): CustomCredentialEntry {
        return CustomCredentialEntry(mContext, TITLE, mPendingIntent, BEGIN_OPTION)
    }

    private fun constructEntryWithAllParams(): CustomCredentialEntry {
        return if (BuildCompat.isAtLeastV()) {
            CustomCredentialEntry(
                mContext,
                TITLE,
                mPendingIntent,
                BEGIN_OPTION,
                SUBTITLE,
                TYPE_DISPLAY_NAME,
                Instant.ofEpochMilli(LAST_USED_TIME),
                ICON,
                IS_AUTO_SELECT_ALLOWED,
                ENTRY_GROUP_ID,
                SINGLE_PROVIDER_ICON_BIT,
                testBiometricPromptData()
            )
        } else {
            CustomCredentialEntry(
                mContext,
                TITLE,
                mPendingIntent,
                BEGIN_OPTION,
                SUBTITLE,
                TYPE_DISPLAY_NAME,
                Instant.ofEpochMilli(LAST_USED_TIME),
                ICON,
                IS_AUTO_SELECT_ALLOWED,
                ENTRY_GROUP_ID,
                SINGLE_PROVIDER_ICON_BIT
            )
        }
    }

    private fun assertEntryWithAllParams(entry: CustomCredentialEntry) {
        assertThat(TITLE == entry.title)
        assertThat(TYPE == entry.type)
        assertThat(SUBTITLE == entry.subtitle)
        assertThat(TYPE_DISPLAY_NAME == entry.typeDisplayName)
        assertThat(ICON).isEqualTo(entry.icon)
        assertThat(Instant.ofEpochMilli(LAST_USED_TIME)).isEqualTo(entry.lastUsedTime)
        assertThat(IS_AUTO_SELECT_ALLOWED).isEqualTo(entry.isAutoSelectAllowed)
        assertThat(mPendingIntent).isEqualTo(entry.pendingIntent)
        assertThat(entry.isDefaultIconPreferredAsSingleProvider).isEqualTo(SINGLE_PROVIDER_ICON_BIT)
        assertThat(ENTRY_GROUP_ID).isEqualTo(entry.entryGroupId)
        if (BuildCompat.isAtLeastV() && entry.biometricPromptData != null) {
            assertThat(entry.biometricPromptData!!.allowedAuthenticators)
                .isEqualTo(testBiometricPromptData().allowedAuthenticators)
        } else {
            assertThat(entry.biometricPromptData).isNull()
        }
    }

    private fun assertEntryWithAllParamsFromSlice(entry: CustomCredentialEntry) {
        assertThat(TITLE == entry.title)
        assertThat(TYPE == entry.type)
        assertThat(SUBTITLE == entry.subtitle)
        assertThat(TYPE_DISPLAY_NAME == entry.typeDisplayName)
        assertThat(ICON).isEqualTo(entry.icon)
        assertThat(Instant.ofEpochMilli(LAST_USED_TIME)).isEqualTo(entry.lastUsedTime)
        assertThat(IS_AUTO_SELECT_ALLOWED).isEqualTo(entry.isAutoSelectAllowed)
        assertThat(mPendingIntent).isEqualTo(entry.pendingIntent)
        assertThat(BEGIN_OPTION.type).isEqualTo(entry.type)
        assertThat(entry.isDefaultIconPreferredAsSingleProvider).isEqualTo(SINGLE_PROVIDER_ICON_BIT)
        assertThat(ENTRY_GROUP_ID).isEqualTo(entry.entryGroupId)
        if (BuildCompat.isAtLeastV() && entry.biometricPromptData != null) {
            assertThat(entry.biometricPromptData!!.allowedAuthenticators)
                .isEqualTo(testBiometricPromptData().allowedAuthenticators)
        } else {
            assertThat(entry.biometricPromptData).isNull()
        }
    }

    private fun assertEntryWithRequiredParams(entry: CustomCredentialEntry) {
        assertThat(TITLE == entry.title)
        assertThat(mPendingIntent).isEqualTo(entry.pendingIntent)
        assertThat(BEGIN_OPTION.type).isEqualTo(entry.type)
        assertThat(BEGIN_OPTION).isEqualTo(entry.beginGetCredentialOption)
        assertThat(entry.isDefaultIconPreferredAsSingleProvider)
            .isEqualTo(DEFAULT_SINGLE_PROVIDER_ICON_BIT)
        assertThat(entry.entryGroupId).isEqualTo(TITLE)
        assertThat(entry.biometricPromptData).isNull()
    }

    private fun assertEntryWithRequiredParamsFromSlice(entry: CustomCredentialEntry) {
        assertThat(TITLE == entry.title)
        assertThat(mPendingIntent).isEqualTo(entry.pendingIntent)
        assertThat(BEGIN_OPTION.type).isEqualTo(entry.type)
        assertThat(entry.isDefaultIconPreferredAsSingleProvider)
            .isEqualTo(DEFAULT_SINGLE_PROVIDER_ICON_BIT)
        assertThat(entry.entryGroupId).isEqualTo(TITLE)
        assertThat(entry.biometricPromptData).isNull()
    }

    companion object {
        private val TITLE: CharSequence = "title"
        private val BEGIN_OPTION: BeginGetCredentialOption =
            BeginGetCustomCredentialOption("id", "custom_type", Bundle())
        private val SUBTITLE: CharSequence = "subtitle"
        private const val TYPE = "custom_type"
        private val TYPE_DISPLAY_NAME: CharSequence = "Password"
        private const val LAST_USED_TIME: Long = 10L
        private val ICON =
            Icon.createWithBitmap(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))
        private const val IS_AUTO_SELECT_ALLOWED = true
        private const val DEFAULT_SINGLE_PROVIDER_ICON_BIT = false
        private const val SINGLE_PROVIDER_ICON_BIT = true
        private const val ENTRY_GROUP_ID = "entryGroupId"
    }
}
