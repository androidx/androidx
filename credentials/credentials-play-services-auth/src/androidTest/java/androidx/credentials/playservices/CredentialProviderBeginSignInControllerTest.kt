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

package androidx.credentials.playservices

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.playservices.TestUtils.Companion.clearFragmentManager
import androidx.credentials.playservices.controllers.BeginSignIn.CredentialProviderBeginSignInController
import androidx.credentials.playservices.controllers.CreatePassword.CredentialProviderCreatePasswordController
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@Suppress("deprecation")
@RequiresApi(api = Build.VERSION_CODES.O)
class CredentialProviderBeginSignInControllerTest {
    @Test
    fun getInstance_createBrandNewFragment_constructSuccess() {
        val activityScenario = ActivityScenario.launch(
            TestCredentialsActivity::class.java
        )
        activityScenario.onActivity { activity: TestCredentialsActivity ->
            val reusedFragmentManager = activity.fragmentManager

            clearFragmentManager(
                reusedFragmentManager
            )

            assertThat(
                reusedFragmentManager.fragments[0].tag ==
                    TestUtils.EXPECTED_LIFECYCLE_TAG
            )
            assertThat(reusedFragmentManager.fragments.size)
                .isEqualTo(1)

            val actualBeginSignInController =
                CredentialProviderBeginSignInController.getInstance(reusedFragmentManager)

            assertThat(actualBeginSignInController).isNotNull()
            assertThat(reusedFragmentManager.fragments.size)
                .isEqualTo(1)
        }
    }

    @Test
    fun getInstance_createDifferentFragment_replaceWithNewFragmentSuccess() {
        val activityScenario = ActivityScenario.launch(
            TestCredentialsActivity::class.java
        )
        activityScenario.onActivity { activity: TestCredentialsActivity ->
            val reusedFragmentManager = activity.fragmentManager

            clearFragmentManager(
                reusedFragmentManager
            )

            assertThat(
                reusedFragmentManager.fragments[0].tag ==
                    TestUtils.EXPECTED_LIFECYCLE_TAG
            )
            assertThat(reusedFragmentManager.fragments.size)
                .isEqualTo(1)

            val oldFragment =
                CredentialProviderCreatePasswordController.getInstance(reusedFragmentManager)

            assertThat(oldFragment).isNotNull()
            assertThat(reusedFragmentManager.fragments.size)
                .isEqualTo(1)

            val newFragment =
                CredentialProviderBeginSignInController.getInstance(reusedFragmentManager)

            assertThat(newFragment).isNotNull()
            assertThat(newFragment).isNotSameInstanceAs(oldFragment)
            assertThat(reusedFragmentManager.fragments.size)
                .isEqualTo(1)
        }
    }

    @Test
    fun getInstance_createFragment_replaceAttemptGivesBackSameFragmentSuccess() {
        val activityScenario = ActivityScenario.launch(
            TestCredentialsActivity::class.java
        )
        activityScenario.onActivity { activity: TestCredentialsActivity ->
            val reusedFragmentManager = activity.fragmentManager

            clearFragmentManager(
                reusedFragmentManager
            )

            assertThat(
                reusedFragmentManager.fragments[0].tag ==
                    TestUtils.EXPECTED_LIFECYCLE_TAG
            )
            assertThat(reusedFragmentManager.fragments.size)
                .isEqualTo(1)

            val expectedBeginSignInController =
                CredentialProviderBeginSignInController.getInstance(reusedFragmentManager)

            assertThat(expectedBeginSignInController).isNotNull()
            assertThat(reusedFragmentManager.fragments.size)
                .isEqualTo(1)

            val actualBeginSignInController =
                CredentialProviderBeginSignInController.getInstance(reusedFragmentManager)

            assertThat(actualBeginSignInController).isNotNull()
            assertThat(actualBeginSignInController)
                .isSameInstanceAs(expectedBeginSignInController)
            assertThat(reusedFragmentManager.fragments.size)
                .isEqualTo(1)
        }
    }

    @Test
    fun invokePlayServices_success() {
        // TODO(" Requires mocking inner Identity call. ")
    }

    @Test
    fun convertResponseToCredentialManager_signInCredentialPasswordInput_success() {
        val activityScenario = ActivityScenario.launch(
            TestCredentialsActivity::class.java
        )
        // TODO add back val expectedId = "id"
        // TODO add back val expectedPassword = "password"
        // TODO add back val expectedType = PasswordCredential.TYPE_PASSWORD_CREDENTIAL
        activityScenario.onActivity { activity: TestCredentialsActivity ->
            val beginSignInController =
                CredentialProviderBeginSignInController
                    .getInstance(activity.fragmentManager)
            beginSignInController.callback =
                object :
                    CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                    override fun onResult(result: GetCredentialResponse) {}
                    override fun onError(e: GetCredentialException) {}
                }
            beginSignInController.executor =
                Executor { obj: Runnable -> obj.run() }

            /**
             * TODO uncomment once SignInCredential testable solution found outside of Auth 20.3.0
            val actualResponse = beginSignInController
                .convertResponseToCredentialManager(
                    SignInCredential(
                        expectedId, null, null,
                        null, null, expectedPassword,
                        null, null, null
                    )
                ).credential

            assertThat(actualResponse.type).isEqualTo(expectedType)
            assertThat((actualResponse as PasswordCredential).password)
                .isEqualTo(expectedPassword)
            assertThat(actualResponse.id)
                .isEqualTo(expectedId)
            */
        }
    }

    @Test
    fun convertRequestToPlayServices_setPasswordOptionRequestAndFalseAutoSelect_success() {
        val activityScenario = ActivityScenario.launch(
            TestCredentialsActivity::class.java
        )
        activityScenario.onActivity { activity: TestCredentialsActivity ->

            val actualResponse = CredentialProviderBeginSignInController
                .getInstance(activity.fragmentManager)
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
        val activityScenario = ActivityScenario.launch(
            TestCredentialsActivity::class.java
        )
        activityScenario.onActivity { activity: TestCredentialsActivity ->

            val actualResponse = CredentialProviderBeginSignInController
                .getInstance(activity.fragmentManager)
                .convertRequestToPlayServices(
                    GetCredentialRequest(
                        listOf(
                            GetPasswordOption()
                        ), true
                    )
                )

            assertThat(
                actualResponse.passwordRequestOptions.isSupported
            ).isTrue()
            assertThat(actualResponse.isAutoSelectEnabled).isTrue()
        }
    }
}