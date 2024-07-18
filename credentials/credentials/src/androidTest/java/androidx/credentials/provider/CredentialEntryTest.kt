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

package androidx.credentials.provider

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.credentials.PasswordCredential.Companion.TYPE_PASSWORD_CREDENTIAL
import androidx.credentials.PublicKeyCredential.Companion.TYPE_PUBLIC_KEY_CREDENTIAL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 28)
@SmallTest
class CredentialEntryTest {
    private val mContext = ApplicationProvider.getApplicationContext<Context>()
    private val mIntent = Intent()
    private val mPendingIntent = PendingIntent.getActivity(mContext, 0, mIntent,
        PendingIntent.FLAG_IMMUTABLE)

    companion object {
        private val BEGIN_OPTION_CUSTOM: BeginGetCredentialOption = BeginGetCustomCredentialOption(
            "id", "custom", Bundle()
        )
        private val BEGIN_OPTION_PASSWORD: BeginGetPasswordOption = BeginGetPasswordOption(
            emptySet(), Bundle.EMPTY, "id"
        )
        private val BEGIN_OPTION_PUBLIC_KEY: BeginGetPublicKeyCredentialOption =
            BeginGetPublicKeyCredentialOption(
                Bundle.EMPTY, "id", "{\"key1\":{\"key2\":{\"key3\":\"value3\"}}}"
        )
    }

    @Test
    fun createFrom_passwordCredential() {
        val entry = PasswordCredentialEntry(
            mContext,
            "username",
            mPendingIntent,
            BEGIN_OPTION_PASSWORD
        )
        assertNotNull(entry)

        val slice = PasswordCredentialEntry.toSlice(entry)
        assertNotNull(slice)

        val result = CredentialEntry.createFrom(slice!!)
        assertThat(result).isNotNull()
        assertThat(result!!.type).isEqualTo(TYPE_PASSWORD_CREDENTIAL)
    }

    @Test
    fun createFrom_publicKeyCredential() {
        val entry = PublicKeyCredentialEntry(
            mContext,
            "username",
            mPendingIntent,
            BEGIN_OPTION_PUBLIC_KEY
        )
        assertNotNull(entry)

        val slice = PublicKeyCredentialEntry.toSlice(entry)
        assertNotNull(slice)

        val result = CredentialEntry.createFrom(slice!!)
        assertNotNull(result)
        assertThat(result!!.type).isEqualTo(TYPE_PUBLIC_KEY_CREDENTIAL)
    }

    @Test
    fun createFrom_customCredential() {
        val entry = CustomCredentialEntry(
            mContext,
            "title",
            mPendingIntent,
            BEGIN_OPTION_CUSTOM
        )
        val slice = CustomCredentialEntry.toSlice(entry)
        assertNotNull(slice)

        val result = CredentialEntry.createFrom(slice!!)
        assertThat(result).isNotNull()
        assertThat(result!!.type).isEqualTo("custom")
    }
}
