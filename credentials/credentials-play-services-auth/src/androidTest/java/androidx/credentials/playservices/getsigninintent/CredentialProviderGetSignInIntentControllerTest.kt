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

package androidx.credentials.playservices.getsigninintent

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.playservices.TestCredentialsActivity
import androidx.credentials.playservices.controllers.GetSignInIntent.CredentialProviderGetSignInIntentController.Companion.getInstance
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@Suppress("deprecation")
@RequiresApi(api = Build.VERSION_CODES.O)
class CredentialProviderGetSignInIntentControllerTest {

    @Test
    fun convertRequestToPlayServices_success() {
        val serverClientId: String = "server_client_id"
        val activityScenario = ActivityScenario.launch(
            TestCredentialsActivity::class.java
        )
        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            val actual: GetSignInIntentRequest = getInstance(activity!!)
                .convertRequestToPlayServices(
                    GetCredentialRequest(
                        listOf(
                            GetSignInWithGoogleOption.Builder(serverClientId).build()
                        )
                    )
                )
            assertThat(
                actual.serverClientId
            ).isEqualTo(serverClientId)
        }
    }

    @Test
    fun convertRequestToPlayServices_moreThanOneOption_failure() {
        val serverClientId: String = "server_client_id"
        val activityScenario = ActivityScenario.launch(
            TestCredentialsActivity::class.java
        )
        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            assertThrows(GetCredentialUnsupportedException::class.java) {
                getInstance(activity!!)
                    .convertRequestToPlayServices(
                        GetCredentialRequest(
                            listOf(
                                GetPasswordOption(),
                                GetSignInWithGoogleOption.Builder(serverClientId).build()
                            )
                        )
                    )
            }
        }
    }
}
