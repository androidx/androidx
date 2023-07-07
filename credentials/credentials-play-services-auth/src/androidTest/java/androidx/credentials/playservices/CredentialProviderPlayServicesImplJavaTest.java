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

package androidx.credentials.playservices;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.Build;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CredentialProviderPlayServicesImplJavaTest {

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    public void isAvailableOnDevice_apiSuccess_returnsTrue() {
        if (Build.VERSION.SDK_INT >= 34) {
            return; // Wait until Mockito fixes 'mock' for API 34
        }
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(activity -> {
            GoogleApiAvailability mock = mock(GoogleApiAvailability.class);
            when(mock.isGooglePlayServicesAvailable(activity.getBaseContext()))
                    .thenReturn(ConnectionResult.SUCCESS);
            boolean expectedAvailability = true;

            CredentialProviderPlayServicesImpl impl =
                    new CredentialProviderPlayServicesImpl(activity.getBaseContext());
            impl.setGoogleApiAvailability(mock);
            boolean actualResponse = impl.isAvailableOnDevice();

            assertThat(actualResponse).isEqualTo(expectedAvailability);
        });
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    public void isAvailableOnDevice_apiNotSuccess_returnsFalse() {
        if (Build.VERSION.SDK_INT >= 34) {
            return; // Wait until Mockito fixes 'mock' for API 34
        }
        ActivityScenario<TestCredentialsActivity> activityScenario =
                ActivityScenario.launch(TestCredentialsActivity.class);
        activityScenario.onActivity(activity -> {
            for (int code : TestUtils.Companion.getConnectionResultFailureCases()) {
                GoogleApiAvailability mock = mock(GoogleApiAvailability.class);
                when(mock.isGooglePlayServicesAvailable(activity.getBaseContext()))
                        .thenReturn(code);
                boolean expectedAvailability = false;

                CredentialProviderPlayServicesImpl impl =
                        new CredentialProviderPlayServicesImpl(activity.getBaseContext());
                impl.setGoogleApiAvailability(mock);
                boolean actualResponse = impl.isAvailableOnDevice();

                assertThat(actualResponse).isEqualTo(expectedAvailability);
            }
        });
    }
}

