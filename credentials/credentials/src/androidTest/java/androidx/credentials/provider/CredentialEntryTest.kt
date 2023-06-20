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

import android.app.slice.Slice
import android.app.slice.SliceSpec
import android.net.Uri
import androidx.core.os.BuildCompat
import androidx.credentials.PasswordCredential.Companion.TYPE_PASSWORD_CREDENTIAL
import androidx.credentials.PublicKeyCredential.Companion.TYPE_PUBLIC_KEY_CREDENTIAL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
@RunWith(AndroidJUnit4::class)
@SmallTest
class CredentialEntryTest {

    @Test
    fun createFrom_passwordCredential() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }

        var sliceSpec = SliceSpec(TYPE_PASSWORD_CREDENTIAL, 1)
        var slice = Slice.Builder(Uri.EMPTY, sliceSpec).build()

        var result = CredentialEntry.createFrom(slice)
        assertThat(result).isNotNull()
        assertThat(result!!.type).isEqualTo(TYPE_PASSWORD_CREDENTIAL)
    }

    @Test
    fun createFrom_publicKeyCredential() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }

        var sliceSpec = SliceSpec(TYPE_PUBLIC_KEY_CREDENTIAL, 1)
        var slice = Slice.Builder(Uri.EMPTY, sliceSpec).build()

        var result = CredentialEntry.createFrom(slice)
        assertThat(result).isNotNull()
        assertThat(result!!.type).isEqualTo(TYPE_PUBLIC_KEY_CREDENTIAL)
    }

    @Test
    fun createFrom_customCredential() {
        if (!BuildCompat.isAtLeastU()) {
            return
        }

        var sliceSpec = SliceSpec("custom", 1)
        var slice = Slice.Builder(Uri.EMPTY, sliceSpec).build()

        var result = CredentialEntry.createFrom(slice)
        assertThat(result).isNotNull()
        assertThat(result!!.type).isEqualTo("custom")
    }
}