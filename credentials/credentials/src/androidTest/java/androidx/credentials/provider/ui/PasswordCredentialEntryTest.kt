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
import androidx.core.os.BuildCompat
import androidx.credentials.PasswordCredential
import androidx.credentials.R
import androidx.credentials.equals
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.PasswordCredentialEntry.Companion.fromSlice
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
@RunWith(AndroidJUnit4::class)
@SmallTest
class PasswordCredentialEntryTest {
    private val mContext = ApplicationProvider.getApplicationContext<Context>()
    private val mIntent = Intent()
    private val mPendingIntent = PendingIntent.getActivity(
        mContext, 0, mIntent,
        PendingIntent.FLAG_IMMUTABLE
    )

    @Test
    fun constructor_requiredParams_success() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val entry = constructEntryWithRequiredParamsOnly()

        assertNotNull(entry)
        assertNotNull(entry.slice)
        assertThat(entry.type).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL)
        assertEntryWithRequiredParamsOnly(entry)
    }

    @Test
    fun constructor_allParams_success() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val entry = constructEntryWithAllParams()

        assertNotNull(entry)
        assertNotNull(entry.slice)
        assertThat(entry.type).isEqualTo(PasswordCredential.TYPE_PASSWORD_CREDENTIAL)
        assertEntryWithAllParams(entry)
    }

    @Test
    fun constructor_emptyUsername_throwsIAE() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        assertThrows(
            "Expected empty username to throw IllegalArgumentException",
            IllegalArgumentException::class.java
        ) {
            PasswordCredentialEntry(
                mContext, "", mPendingIntent, BEGIN_OPTION
            )
        }
    }

    @Test
    fun constructor_nullIcon_defaultIconSet() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val entry = PasswordCredentialEntry.Builder(
            mContext, USERNAME, mPendingIntent, BEGIN_OPTION).build()

        assertThat(
            equals(
                entry.icon,
                Icon.createWithResource(mContext, R.drawable.ic_password)
            )
        ).isTrue()
    }

    @Test
    fun constructor_nullTypeDisplayName_defaultDisplayNameSet() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val entry = PasswordCredentialEntry(
            mContext, USERNAME, mPendingIntent, BEGIN_OPTION)

        assertThat(entry.typeDisplayName).isEqualTo(
            mContext.getString(
                R.string.android_credentials_TYPE_PASSWORD_CREDENTIAL
            )
        )
    }

    @Test
    fun constructor_isAutoSelectAllowedDefault_false() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val entry = constructEntryWithRequiredParamsOnly()
        val entry1 = constructEntryWithAllParams()

        assertFalse(entry.isAutoSelectAllowed)
        assertFalse(entry1.isAutoSelectAllowed)
    }

    @Test
    fun fromSlice_success() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }
        val originalEntry = constructEntryWithAllParams()

        val entry = fromSlice(originalEntry.slice)

        assertNotNull(entry)
        entry?.let {
            assertEntryWithAllParams(entry)
        }
    }

    private fun constructEntryWithRequiredParamsOnly(): PasswordCredentialEntry {
        return PasswordCredentialEntry(
            mContext,
            USERNAME,
            mPendingIntent,
            BEGIN_OPTION
        )
    }

    private fun constructEntryWithAllParams(): PasswordCredentialEntry {
        return PasswordCredentialEntry(
            mContext,
            USERNAME,
            mPendingIntent,
            BEGIN_OPTION,
            DISPLAYNAME,
            LAST_USED_TIME,
            ICON
        )
    }

    private fun assertEntryWithRequiredParamsOnly(entry: PasswordCredentialEntry) {
        assertThat(USERNAME == entry.username)
        assertThat(mPendingIntent).isEqualTo(entry.pendingIntent)
    }

    private fun assertEntryWithAllParams(entry: PasswordCredentialEntry) {
        assertThat(USERNAME == entry.username)
        assertThat(DISPLAYNAME == entry.displayName)
        assertThat(TYPE_DISPLAY_NAME == entry.typeDisplayName)
        assertThat(ICON).isEqualTo(entry.icon)
        assertNotNull(entry.lastUsedTime)
        entry.lastUsedTime?.let {
            assertThat(LAST_USED_TIME.toEpochMilli()).isEqualTo(
                it.toEpochMilli())
        }
        assertThat(mPendingIntent).isEqualTo(entry.pendingIntent)
    }

    companion object {
        private val USERNAME: CharSequence = "title"
        private val DISPLAYNAME: CharSequence = "subtitle"
        private val TYPE_DISPLAY_NAME: CharSequence = "Password"
        private val LAST_USED_TIME = Instant.now()
        private val BEGIN_OPTION = BeginGetPasswordOption(
            emptySet<String>(),
            Bundle.EMPTY, "id")
        private val ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(
                100, 100, Bitmap.Config.ARGB_8888
            )
        )
    }
}