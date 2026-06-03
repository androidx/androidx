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

package androidx.compose.ui.autofill

import android.credentials.CredentialOption
import android.credentials.GetCredentialException
import android.credentials.GetCredentialRequest
import android.credentials.GetCredentialResponse
import android.os.Bundle
import android.os.OutcomeReceiver
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CredentialRequestData
import androidx.compose.ui.semantics.credentialRequest
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class AndroidCredentialViewStructureTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    private val height = 200.dp
    private val width = 200.dp

    @Test
    fun populateViewStructure_credentialRequestPopulated() {
        val emptyOption =
            CredentialOption.Builder(
                    "com.example.credential.TYPE", // type
                    Bundle(),
                    Bundle(),
                )
                .build()

        val platformRequest =
            GetCredentialRequest.Builder(Bundle()).addCredentialOption(emptyOption).build()
        val callback =
            object : OutcomeReceiver<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {}

                override fun onError(error: GetCredentialException) {}
            }
        val expectedData = CredentialRequestData(platformRequest, callback)

        var autofillManager: AndroidAutofillManager? = null

        rule.setContent {
            autofillManager = LocalAutofillManager.current as AndroidAutofillManager
            Box(
                Modifier.testTag("loginNode")
                    .semantics { credentialRequest = expectedData }
                    .size(height, width)
            )
        }

        rule.waitForIdle()

        val fakeRootStructure = FakeViewStructure()

        // Act.
        rule.runOnIdle { autofillManager!!.populateViewStructure(fakeRootStructure) }

        // Assert.
        assertThat(fakeRootStructure.children).hasSize(1)
        val child = fakeRootStructure.children[0]
        assertThat(child.credentialRequest).isSameInstanceAs(platformRequest)
        assertThat(child.credentialCallback).isSameInstanceAs(callback)
    }
}
