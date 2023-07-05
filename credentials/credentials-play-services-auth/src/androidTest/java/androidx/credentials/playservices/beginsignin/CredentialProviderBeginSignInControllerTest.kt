/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.credentials.playservices.beginsignin

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.playservices.TestCredentialsActivity
import androidx.credentials.playservices.TestCredentialsFragmentActivity
import androidx.credentials.playservices.controllers.BeginSignIn.CredentialProviderBeginSignInController.Companion.getInstance
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SmallTest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@SmallTest
@Suppress("deprecation")
@RequiresApi(api = Build.VERSION_CODES.O)
class CredentialProviderBeginSignInControllerTest(val useFragmentActivity: Boolean) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = listOf(true, false)
    }

    private fun launchTestActivity(callback: (activity: Activity) -> Unit) {
        if (useFragmentActivity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            var activityScenario =
                            ActivityScenario.launch(
                                    androidx.credentials.playservices
                                            .TestCredentialsFragmentActivity::class.java)
            activityScenario.onActivity { activity: Activity ->
                callback.invoke(activity)
            }
        } else {
            var activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)
            activityScenario.onActivity { activity: Activity ->
                callback.invoke(activity)
            }
        }
    }

    @Test
    fun convertRequestToPlayServices_setPasswordOptionRequestAndFalseAutoSelect_success() {
        launchTestActivity { activity: Activity ->
            val actualResponse = getInstance(activity)
                .convertRequestToPlayServices(
                    GetCredentialRequest(
                        listOf(
                            GetPasswordOption()
                        )
                    )
                )
            assertThat(
                actualResponse.passwordRequestOptions.isSupported
            ).isTrue()
            assertThat(actualResponse.isAutoSelectEnabled).isFalse()
        }
    }

    @Test
    fun convertRequestToPlayServices_setPasswordOptionRequestAndTrueAutoSelect_success() {
        launchTestActivity { activity: Activity ->
            val actualResponse = getInstance(activity)
                .convertRequestToPlayServices(
                    GetCredentialRequest(
                        listOf(
                            GetPasswordOption(isAutoSelectAllowed = true)
                        )
                    )
                )
            assertThat(
                actualResponse.passwordRequestOptions.isSupported
            ).isTrue()
            assertThat(actualResponse.isAutoSelectEnabled).isTrue()
        }
    }

    @Test
    fun convertRequestToPlayServices_setGoogleIdOptionRequest_success() {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId("server_client_id")
            .setNonce("nonce")
            .setFilterByAuthorizedAccounts(true)
            .setRequestVerifiedPhoneNumber(false)
            .associateLinkedAccounts("link_service_id", listOf("a", "b", "c"))
            .setAutoSelectEnabled(true)
            .build()

        launchTestActivity { activity: Activity ->
            val actualRequest = getInstance(activity)
                .convertRequestToPlayServices(
                    GetCredentialRequest(
                        listOf(
                            option
                        )
                    )
                )
            assertThat(
                actualRequest.googleIdTokenRequestOptions.isSupported
            ).isTrue()
            assertThat(actualRequest.isAutoSelectEnabled).isTrue()
            val actualOption = actualRequest.googleIdTokenRequestOptions
            assertThat(actualOption.serverClientId).isEqualTo(option.serverClientId)
            assertThat(actualOption.nonce).isEqualTo(option.nonce)
            assertThat(actualOption.filterByAuthorizedAccounts())
                .isEqualTo(option.filterByAuthorizedAccounts)
            assertThat(actualOption.requestVerifiedPhoneNumber())
                .isEqualTo(option.requestVerifiedPhoneNumber)
            assertThat(actualOption.linkedServiceId).isEqualTo(option.linkedServiceId)
            assertThat(actualOption.idTokenDepositionScopes)
                .isEqualTo(option.idTokenDepositionScopes)
        }
    }

    @Test
    fun duplicateGetInstance_shouldBeEqual() {
        launchTestActivity { activity: Activity ->

            val firstInstance = getInstance(activity)
            val secondInstance = getInstance(activity)
            assertThat(firstInstance).isEqualTo(secondInstance)
        }
    }
}