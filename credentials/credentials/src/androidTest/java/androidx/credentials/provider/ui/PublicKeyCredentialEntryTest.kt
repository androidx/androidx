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
import androidx.credentials.PublicKeyCredential
import androidx.credentials.R
import androidx.credentials.equals
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.PublicKeyCredentialEntry
import androidx.credentials.provider.PublicKeyCredentialEntry.Companion.fromSlice
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import junit.framework.TestCase.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 26)
@RunWith(AndroidJUnit4::class)
@SmallTest
class PublicKeyCredentialEntryTest {
    private val mContext = ApplicationProvider.getApplicationContext<Context>()
    private val mIntent = Intent()
    private val mPendingIntent = PendingIntent.getActivity(
        mContext, 0, mIntent,
        PendingIntent.FLAG_IMMUTABLE
    )

    @Test
    fun constructor_requiredParamsOnly_success() {
        val entry = constructWithRequiredParamsOnly()

        assertNotNull(entry)
        assertThat(entry.type).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)
        assertEntryWithRequiredParams(entry)
    }

    @Test
    fun constructor_allParams_success() {
        val entry = constructWithAllParams()
        assertNotNull(entry)
        assertThat(entry.type).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)
        assertEntryWithAllParams(entry)
    }

    @Test
    fun constructor_emptyUsername_throwsIAE() {
        assertThrows(
            "Expected empty username to throw IllegalArgumentException",
            IllegalArgumentException::class.java
        ) {
            PublicKeyCredentialEntry(
                mContext, "", mPendingIntent, BEGIN_OPTION
            )
        }
    }

    @Test
    fun constructor_nullIcon_defaultIconSet() {
        val entry = PublicKeyCredentialEntry(
            mContext, USERNAME, mPendingIntent, BEGIN_OPTION
        )

        assertThat(
            equals(
                entry.icon,
                Icon.createWithResource(mContext, R.drawable.ic_passkey)
            )
        ).isTrue()
    }

    @Test
    fun constructor_nullTypeDisplayName_defaultDisplayNameSet() {
        val entry = PublicKeyCredentialEntry(
            mContext, USERNAME, mPendingIntent, BEGIN_OPTION
        )
        assertThat(entry.typeDisplayName).isEqualTo(
            mContext.getString(
                R.string.androidx_credentials_TYPE_PUBLIC_KEY_CREDENTIAL
            )
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun fromSlice_success() {
        val originalEntry = constructWithAllParams()
        val slice = PublicKeyCredentialEntry.toSlice(originalEntry)

        assertNotNull(slice)
        val entry = fromSlice(slice!!)

        assertNotNull(entry)
        entry?.let {
            assertEntryWithRequiredParams(entry)
        }
    }

    private fun constructWithRequiredParamsOnly(): PublicKeyCredentialEntry {
        return PublicKeyCredentialEntry(
            mContext,
            USERNAME,
            mPendingIntent,
            BEGIN_OPTION
        )
    }

    private fun constructWithAllParams(): PublicKeyCredentialEntry {
        return PublicKeyCredentialEntry(
            mContext,
            USERNAME,
            mPendingIntent,
            BEGIN_OPTION,
            DISPLAYNAME,
            Instant.ofEpochMilli(LAST_USED_TIME),
            ICON,
            IS_AUTO_SELECT_ALLOWED
        )
    }

    private fun assertEntryWithRequiredParams(entry: PublicKeyCredentialEntry) {
        assertThat(USERNAME == entry.username)
        assertThat(mPendingIntent).isEqualTo(entry.pendingIntent)
    }

    private fun assertEntryWithAllParams(entry: PublicKeyCredentialEntry) {
        assertThat(USERNAME == entry.username)
        assertThat(DISPLAYNAME == entry.displayName)
        assertThat(TYPE_DISPLAY_NAME == entry.typeDisplayName)
        assertThat(ICON).isEqualTo(entry.icon)
        assertThat(Instant.ofEpochMilli(LAST_USED_TIME)).isEqualTo(entry.lastUsedTime)
        assertThat(IS_AUTO_SELECT_ALLOWED).isEqualTo(entry.isAutoSelectAllowed)
        assertThat(mPendingIntent).isEqualTo(entry.pendingIntent)
    }

    companion object {
        private val BEGIN_OPTION: BeginGetPublicKeyCredentialOption =
            BeginGetPublicKeyCredentialOption(Bundle(), "id",
                "{\"key1\":{\"key2\":{\"key3\":\"value3\"}}}")
        private val USERNAME: CharSequence = "title"
        private val DISPLAYNAME: CharSequence = "subtitle"
        private val TYPE_DISPLAY_NAME: CharSequence = "Password"
        private const val LAST_USED_TIME: Long = 10L
        private val ICON = Icon.createWithBitmap(Bitmap.createBitmap(
            100, 100, Bitmap.Config.ARGB_8888))
        private const val IS_AUTO_SELECT_ALLOWED = true
    }
}
