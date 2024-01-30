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

package androidx.credentials;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@SdkSuppress(minSdkVersion = 34)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class PrepareGetCredentialResponseJavaTest {

    @Test
    public void constructor_throwsNPEWhenMissingInput() {
        Assert.assertThrows(
                "Expected null pointer when missing input",
                NullPointerException.class,
                () -> {
                    new PrepareGetCredentialResponse.Builder()
                        .setFrameworkResponse(null)
                        .build();
                });
    }

    @Test
    public void test_hasCredentialResults() {
        // Construct the test class.
        PrepareGetCredentialResponse response = new PrepareGetCredentialResponse.TestBuilder()
                .setCredentialTypeDelegate((val) ->
                    val.equals("password") || val.equals("otherValid"))
                .build();

        // Verify the response.
        assertThat(response.hasCredentialResults("password")).isTrue();
        assertThat(response.hasCredentialResults("otherValid")).isTrue();
        assertThat(response.hasCredentialResults("other")).isFalse();
        assertThat(response.hasAuthenticationResults()).isFalse();
        assertThat(response.hasRemoteResults()).isFalse();
    }

    @Test
    public void test_hasAuthenticationResults() {
        // Construct the test class.
        PrepareGetCredentialResponse response = new PrepareGetCredentialResponse.TestBuilder()
                .setHasAuthResultsDelegate(() -> true)
                .build();

        // Verify the response.
        assertThat(response.hasCredentialResults("password")).isFalse();
        assertThat(response.hasCredentialResults("otherValid")).isFalse();
        assertThat(response.hasCredentialResults("other")).isFalse();
        assertThat(response.hasAuthenticationResults()).isTrue();
        assertThat(response.hasRemoteResults()).isFalse();
    }

    @Test
    public void test_hasRemoteResults() {
        // Construct the test class.
        PrepareGetCredentialResponse response = new PrepareGetCredentialResponse.TestBuilder()
                .setHasRemoteResultsDelegate(() -> true)
                .build();

        // Verify the response.
        assertThat(response.hasCredentialResults("password")).isFalse();
        assertThat(response.hasCredentialResults("otherValid")).isFalse();
        assertThat(response.hasCredentialResults("other")).isFalse();
        assertThat(response.hasAuthenticationResults()).isFalse();
        assertThat(response.hasRemoteResults()).isTrue();
    }
}
