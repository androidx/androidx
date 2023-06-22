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

package androidx.credentials.playservices.createpassword

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.annotation.DoNotInline
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.playservices.TestCredentialsActivity
import androidx.credentials.playservices.TestUtils.Companion.equals
import androidx.credentials.playservices.controllers.CreatePassword.CredentialProviderCreatePasswordController.Companion.getInstance
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@SmallTest
class CredentialProviderCreatePasswordControllerTest(val useFragmentActivity: Boolean) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters() = listOf(true, false)
    }

    @DoNotInline
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
    fun convertResponseToCredentialManager_unitInput_success() {
        val expectedResponseType = CreatePasswordResponse().type
        launchTestActivity { activity: Activity ->

            val actualResponse = getInstance(activity)
                .convertResponseToCredentialManager(Unit)

            assertThat(actualResponse.type)
                .isEqualTo(expectedResponseType)
            assertThat(equals(actualResponse.data, Bundle.EMPTY)).isTrue()
        }
    }

    @Test
    fun convertRequestToPlayServices_createPasswordRequest_success() {
        val expectedId = "LM"
        val expectedPassword = "SodaButton"
        launchTestActivity { activity: Activity ->

            val actualRequest = getInstance(activity)
                .convertRequestToPlayServices(CreatePasswordRequest(
                        expectedId, expectedPassword)).signInPassword

            assertThat(actualRequest.password)
                .isEqualTo(expectedPassword)
            assertThat(actualRequest.id).isEqualTo(expectedId)
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