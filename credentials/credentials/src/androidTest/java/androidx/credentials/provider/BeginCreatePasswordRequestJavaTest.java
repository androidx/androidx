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

package androidx.credentials.provider;

import static com.google.common.truth.Truth.assertThat;

import android.content.pm.SigningInfo;
import android.os.Bundle;
import android.service.credentials.CallingAppInfo;

import androidx.core.os.BuildCompat;
import androidx.credentials.TestUtilsKt;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BeginCreatePasswordRequestJavaTest {
    @Test
    public void constructor_success() {
        if (BuildCompat.isAtLeastU()) {
            new BeginCreatePasswordCredentialRequest(
                    new CallingAppInfo("sample_package_name",
                            new SigningInfo()),
                    new Bundle());
        }
    }

    @Test
    public void getter_callingAppInfo() {
        if (BuildCompat.isAtLeastU()) {
            Bundle expectedCandidateQueryBundle = new Bundle();
            expectedCandidateQueryBundle.putString("key", "value");
            String expectedPackageName = "sample_package_name";
            SigningInfo expectedSigningInfo = new SigningInfo();
            CallingAppInfo expectedCallingAppInfo = new CallingAppInfo(expectedPackageName,
                    expectedSigningInfo);

            BeginCreatePasswordCredentialRequest request =
                    new BeginCreatePasswordCredentialRequest(expectedCallingAppInfo,
                            expectedCandidateQueryBundle);

            assertThat(request.getCallingAppInfo().getPackageName()).isEqualTo(expectedPackageName);
            assertThat(request.getCallingAppInfo().getSigningInfo()).isEqualTo(expectedSigningInfo);
            TestUtilsKt.equals(request.getCandidateQueryData(), expectedCandidateQueryBundle);
        }
    }

    // TODO ("Add framework conversion, createFrom tests")
}
