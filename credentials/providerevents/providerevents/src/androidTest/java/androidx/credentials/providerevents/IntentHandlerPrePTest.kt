/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.providerevents

import android.content.Intent
import android.content.pm.Signature
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(maxSdkVersion = 27)
@RunWith(AndroidJUnit4::class)
@SmallTest
class IntentHandlerPrePTest {

    @Test
    fun retrieveProviderImportCredentialsRequest_success() {
        // 1. Setup
        val intent = Intent()
        val callerSignatures = listOf(byteArrayOf(1), byteArrayOf(2))
        val listSignatures: MutableList<Signature> = mutableListOf()
        for (bytes in callerSignatures) {
            listSignatures.add(Signature(bytes))
        }
        intent.putExtra(EXTRA_SIGNATURE_COUNT, callerSignatures.size)
        callerSignatures.forEachIndexed { index, bytes ->
            intent.putExtra("${EXTRA_SIGNATURE_PREFIX}$index", bytes)
        }

        intent.putExtra(EXTRA_REQUEST_JSON, TEST_REQUEST_JSON)
        intent.putExtra(EXTRA_PACKAGE_NAME, TEST_PACKAGE_NAME)
        intent.putExtra(EXTRA_CRED_ID, "testCredId")
        intent.setData(Uri.EMPTY)

        // 2. Execution
        val result = IntentHandler.retrieveProviderImportCredentialsRequest(intent)

        // 3. Assertion
        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result!!.request.requestJson).isEqualTo(TEST_REQUEST_JSON)
        Truth.assertThat(result.callingAppInfo.packageName).isEqualTo(TEST_PACKAGE_NAME)
        Truth.assertThat(result.callingAppInfo.signingInfoCompat.signingCertificateHistory)
            .isEqualTo(listSignatures)
    }

    private companion object {
        private const val EXTRA_REQUEST_JSON =
            "androidx.credentials.providerevents.extra.IMPORT_CREDENTIALS_REQUEST_JSON"
        private const val EXTRA_PACKAGE_NAME =
            "androidx.credentials.providerevents.extra.CALLING_PACKAGE_NAME"
        private const val EXTRA_CRED_ID = "androidx.credentials.providerevents.extra.CREDENTIAL_ID"
        private const val EXTRA_SIGNATURE_COUNT =
            "androidx.credentials.providerevents.extra.SIGNATURE_COUNT"
        private const val EXTRA_SIGNATURE_PREFIX =
            "androidx.credentials.providerevents.extra.SIGNATURE_"
        private val TEST_REQUEST_JSON =
            "{\"credentialTypes\":[\"basic-auth\"],\"knownExtensions\":[\"shared\"]}"
        private val TEST_PACKAGE_NAME = "com.example.test.app"
    }
}
