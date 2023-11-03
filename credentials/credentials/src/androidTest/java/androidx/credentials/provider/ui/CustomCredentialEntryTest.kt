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
import androidx.credentials.R
import androidx.credentials.equals
import androidx.credentials.provider.BeginGetCredentialOption
import androidx.credentials.provider.BeginGetCustomCredentialOption
import androidx.credentials.provider.CustomCredentialEntry
import androidx.credentials.provider.CustomCredentialEntry.Companion.fromSlice
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
@SmallTest
class CustomCredentialEntryTest {
    private val mContext = ApplicationProvider.getApplicationContext<Context>()
    private val mIntent = Intent()
    private val mPendingIntent = PendingIntent.getActivity(mContext, 0, mIntent,
        PendingIntent.FLAG_IMMUTABLE)
    @Test
    @SdkSuppress(minSdkVersion = 28)
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
        assertThrows(
            "Expected empty title to throw NPE",
            IllegalArgumentException::class.java
        ) {
            CustomCredentialEntry(
                mContext, TITLE, mPendingIntent, BeginGetCustomCredentialOption(
                    "id", "", Bundle.EMPTY
                )
            )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun constructor_emptyType_throwsIAE() {
        assertThrows(
            "Expected empty type to throw NPE",
            IllegalArgumentException::class.java
        ) {
            CustomCredentialEntry(
                mContext, TITLE, mPendingIntent, BeginGetCustomCredentialOption(
                    "id", "", Bundle.EMPTY)
            )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    fun constructor_nullIcon_defaultIconSet() {
        val entry = constructEntryWithRequiredParams()

        assertThat(
            equals(
                entry.icon,
                Icon.createWithResource(mContext, R.drawable.ic_other_sign_in)
            )
        ).isTrue()
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun fromSlice_requiredParams_success() {
        val originalEntry = constructEntryWithRequiredParams()

        val slice = CustomCredentialEntry.toSlice(
            originalEntry)
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

        val slice = CustomCredentialEntry.toSlice(
        originalEntry)
        assertNotNull(slice)
        val entry = fromSlice(slice!!)

        assertNotNull(entry)
        if (entry != null) {
            assertEntryWithAllParamsFromSlice(entry)
        }
    }

    private fun constructEntryWithRequiredParams(): CustomCredentialEntry {
        return CustomCredentialEntry(
            mContext,
            TITLE,
            mPendingIntent,
            BEGIN_OPTION
        )
    }

    private fun constructEntryWithAllParams(): CustomCredentialEntry {
        return CustomCredentialEntry(
            mContext,
            TITLE,
            mPendingIntent,
            BEGIN_OPTION,
            SUBTITLE,
            TYPE_DISPLAY_NAME,
            Instant.ofEpochMilli(LAST_USED_TIME),
            ICON,
            IS_AUTO_SELECT_ALLOWED
        )
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
    }

    private fun assertEntryWithRequiredParams(entry: CustomCredentialEntry) {
        assertThat(TITLE == entry.title)
        assertThat(mPendingIntent).isEqualTo(entry.pendingIntent)
        assertThat(BEGIN_OPTION.type).isEqualTo(entry.type)
        assertThat(BEGIN_OPTION).isEqualTo(entry.beginGetCredentialOption)
    }

    private fun assertEntryWithRequiredParamsFromSlice(entry: CustomCredentialEntry) {
        assertThat(TITLE == entry.title)
        assertThat(mPendingIntent).isEqualTo(entry.pendingIntent)
        assertThat(BEGIN_OPTION.type).isEqualTo(entry.type)
    }

    companion object {
        private val TITLE: CharSequence = "title"
        private val BEGIN_OPTION: BeginGetCredentialOption = BeginGetCustomCredentialOption(
            "id", "custom_type", Bundle())
        private val SUBTITLE: CharSequence = "subtitle"
        private const val TYPE = "custom_type"
        private val TYPE_DISPLAY_NAME: CharSequence = "Password"
        private const val LAST_USED_TIME: Long = 10L
        private val ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(
                100, 100, Bitmap.Config.ARGB_8888
            )
        )
        private const val IS_AUTO_SELECT_ALLOWED = true
    }
}
