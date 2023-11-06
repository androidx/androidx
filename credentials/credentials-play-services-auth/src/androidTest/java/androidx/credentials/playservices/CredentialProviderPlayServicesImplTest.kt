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

import androidx.credentials.playservices.TestUtils.Companion.ConnectionResultFailureCases
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
@SmallTest
class CredentialProviderPlayServicesImplTest {

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    fun isAvailableOnDevice_apiSuccess_returnsTrue() {
        val activityScenario = ActivityScenario.launch(
            TestCredentialsActivity::class.java
        )
        activityScenario.onActivity { activity: TestCredentialsActivity ->
            val mock =
                Mockito.mock(GoogleApiAvailability::class.java)
            Mockito.`when`(mock.isGooglePlayServicesAvailable(activity.baseContext))
                .thenReturn(ConnectionResult.SUCCESS)
            val expectedAvailability = true

            val impl =
                CredentialProviderPlayServicesImpl(activity.baseContext)
            impl.googleApiAvailability = mock
            val actualResponse = impl.isAvailableOnDevice()

            Truth.assertThat(actualResponse).isEqualTo(expectedAvailability)
        }
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    fun isAvailableOnDevice_apiNotSuccess_returnsFalse() {
        val activityScenario = ActivityScenario.launch(
            TestCredentialsActivity::class.java
        )
        activityScenario.onActivity { activity: TestCredentialsActivity ->
            for (code in ConnectionResultFailureCases) {
                val mock = Mockito.mock(GoogleApiAvailability::class.java)
                Mockito.`when`(mock.isGooglePlayServicesAvailable(activity.baseContext))
                    .thenReturn(code)
                val expectedAvailability = false

                val impl =
                    CredentialProviderPlayServicesImpl(activity.baseContext)
                impl.googleApiAvailability = mock
                val actualResponse = impl.isAvailableOnDevice()

                Truth.assertThat(actualResponse).isEqualTo(expectedAvailability)
            }
        }
    }
}
