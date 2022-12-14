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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.playservices.TestCredentialsActivity
import androidx.credentials.playservices.controllers.BeginSignIn.CredentialProviderBeginSignInController.Companion.getInstance
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@Suppress("deprecation")
@RequiresApi(api = Build.VERSION_CODES.O)
class CredentialProviderBeginSignInControllerTest {
    @Test
    fun convertRequestToPlayServices_setPasswordOptionRequestAndFalseAutoSelect_success() {
        val activityScenario = ActivityScenario.launch(
            TestCredentialsActivity::class.java
        )
        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            val actualResponse = getInstance(activity!!)
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
        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            val actualResponse = getInstance(activity!!)
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