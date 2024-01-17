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

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@RequiresApi(34)
@SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
class PendingIntentHandlerTest {
    @Test
    fun test_createCredentialException() {
        if (Build.VERSION.SDK_INT >= 34) {
            return
        }

        val intent = Intent()
        val initialException = CreateCredentialInterruptedException("message")

        PendingIntentHandler.setCreateCredentialException(intent, initialException)

        val finalException = intent.getCreateCredentialException()
        assertThat(finalException).isNotNull()
        assertThat(finalException).isEqualTo(initialException)
    }

    @Test()
    fun test_createCredentialException_throwsWhenEmptyIntent() {
        if (Build.VERSION.SDK_INT >= 34) {
            return
        }

        val intent = Intent()
        assertThat(intent.getCreateCredentialException()).isNull()
    }

    @Test
    fun test_credentialException() {
        if (Build.VERSION.SDK_INT >= 34) {
            return
        }

        val intent = Intent()
        val initialException = GetCredentialInterruptedException("message")

        PendingIntentHandler.setGetCredentialException(intent, initialException)

        val finalException = intent.getGetCredentialException()
        assertThat(finalException).isNotNull()
        assertThat(finalException).isEqualTo(initialException)
    }

    @Test
    fun test_credentialException_throwsWhenEmptyIntent() {
        if (Build.VERSION.SDK_INT >= 34) {
            return
        }

        val intent = Intent()
        assertThat(intent.getGetCredentialException()).isNull()
    }

    @Test
    fun test_beginGetResponse() {
        if (Build.VERSION.SDK_INT >= 34) {
            return
        }

        val intent = Intent()
        val initialResponse = BeginGetCredentialResponse.Builder().build()

        PendingIntentHandler.setBeginGetCredentialResponse(intent, initialResponse)

        val finalResponse = intent.getBeginGetResponse()
        assertThat(finalResponse).isNotNull()
        assertThat(finalResponse).isEqualTo(initialResponse)
    }

    @Test
    fun test_beginGetResponse_throwsWhenEmptyIntent() {
        if (Build.VERSION.SDK_INT >= 34) {
            return
        }

        val intent = Intent()
        assertThat(intent.getBeginGetResponse()).isNull()
    }

    @Test
    fun test_credentialResponse() {
        if (Build.VERSION.SDK_INT >= 34) {
            return
        }

        val intent = Intent()
        val credential = PasswordCredential("a", "b")
        val initialResponse = GetCredentialResponse(credential)

        PendingIntentHandler.setGetCredentialResponse(intent, initialResponse)

        val finalResponse = intent.getGetCredentialResponse()
        assertThat(finalResponse).isNotNull()
        assertThat(finalResponse).isEqualTo(initialResponse)
    }

    @Test
    fun test_credentialResponse_throwsWhenEmptyIntent() {
        if (Build.VERSION.SDK_INT >= 34) {
            return
        }

        val intent = Intent()
        assertThat(intent.getGetCredentialResponse()).isNull()
    }

    @Test
    fun test_createCredentialCredentialResponse() {
        if (Build.VERSION.SDK_INT >= 34) {
            return
        }

        val intent = Intent()
        val initialResponse = CreatePasswordResponse()

        PendingIntentHandler.setCreateCredentialResponse(intent, initialResponse)

        val finalResponse = intent.getCreateCredentialCredentialResponse()
        assertThat(finalResponse).isNotNull()
        assertThat(finalResponse).isEqualTo(initialResponse)
    }

    @Test
    fun test_createCredentialCredentialResponse_throwsWhenEmptyIntent() {
        if (Build.VERSION.SDK_INT >= 34) {
            return
        }

        val intent = Intent()
        val r = intent.getCreateCredentialCredentialResponse()
        assertThat(r).isNull()
    }
}
