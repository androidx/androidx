/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.credentials

import android.content.Context
import android.credentials.Credential
import android.os.OutcomeReceiver
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.credentials.internal.FrameworkImplHelper.Companion.convertGetRequestToFrameworkClass
import androidx.credentials.internal.FrameworkImplHelper.Companion.convertGetResponseToJetpackClass
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Test

@SdkSuppress(minSdkVersion = 35, codeName = "VanillaIceCream")
class CredentialManagerViewHandlerTest {
    private val mContext: Context = ApplicationProvider.getApplicationContext()

    companion object {
        private val GET_CRED_PASSWORD_REQ =
            GetCredentialRequest.Builder().setCredentialOptions(listOf(GetPasswordOption())).build()
        private val GET_CRED_PASSWORD_FRAMEWORK_REQ =
            convertGetRequestToFrameworkClass(GET_CRED_PASSWORD_REQ)
    }

    @Test
    @RequiresApi(35)
    fun setPendingCredentialRequest_frameworkAttrSetSuccessfully() {
        val editText = EditText(mContext)
        val pendingGetCredentialRequest =
            PendingGetCredentialRequest(GET_CRED_PASSWORD_REQ) { _: GetCredentialResponse? -> }

        editText.pendingGetCredentialRequest = pendingGetCredentialRequest

        equals(editText.pendingCredentialRequest!!, GET_CRED_PASSWORD_FRAMEWORK_REQ)
        assertThat(editText.pendingCredentialCallback).isInstanceOf(OutcomeReceiver::class.java)
    }

    @Test
    @RequiresApi(35)
    @Throws(InterruptedException::class)
    fun setPendingCredentialRequest_callbackInvokedSuccessfully() {
        val latch1 = CountDownLatch(1)
        val getCredentialResponse = AtomicReference<GetCredentialResponse>()
        val editText = EditText(mContext)

        editText.pendingGetCredentialRequest =
            PendingGetCredentialRequest(GET_CRED_PASSWORD_REQ) { response ->
                getCredentialResponse.set(response)
                latch1.countDown()
            }

        equals(editText.pendingCredentialRequest!!, GET_CRED_PASSWORD_FRAMEWORK_REQ)
        assertThat(editText.pendingCredentialCallback).isInstanceOf(OutcomeReceiver::class.java)

        val passwordCredential = PasswordCredential("id", "password")
        val frameworkPasswordResponse =
            android.credentials.GetCredentialResponse(
                Credential(passwordCredential.type, passwordCredential.data)
            )

        editText.pendingCredentialCallback!!.onResult(frameworkPasswordResponse)
        latch1.await(50L, TimeUnit.MILLISECONDS)

        assertThat(getCredentialResponse.get()).isNotNull()
        val expectedGetCredentialResponse =
            convertGetResponseToJetpackClass(frameworkPasswordResponse)
        equals(expectedGetCredentialResponse, getCredentialResponse.get())
    }
}
