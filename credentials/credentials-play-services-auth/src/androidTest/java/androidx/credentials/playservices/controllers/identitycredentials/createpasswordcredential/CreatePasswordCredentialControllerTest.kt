/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.credentials.playservices.controllers.identitycredentials.createpasswordcredential

import android.content.Context
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.playservices.TestCredentialsActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class CreatePasswordCredentialControllerTest {
    val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun convertRequestToPlayServices_createPasswordRequest_success() {
        val id = "name"
        val password = "password"
        val request = CreatePasswordRequest(id = id, password = password)

        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)

        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            val controller = CreatePasswordCredentialController(context)
            val convertedRequest = controller.convertRequestToPlayServices(request)
            assertThat(convertedRequest.origin).isEqualTo(request.origin)
            assertThat(
                    convertedRequest.credentialData.getString("androidx.credentials.BUNDLE_KEY_ID")
                )
                .isEqualTo(id)
            assertThat(
                    convertedRequest.credentialData.getString(
                        "androidx.credentials.BUNDLE_KEY_PASSWORD"
                    )
                )
                .isEqualTo(password)
        }
    }
}
