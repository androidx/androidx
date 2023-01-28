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

package androidx.credentials;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class GetCredentialRequestJavaTest {
    @Test
    public void constructor_emptyCredentialOptions_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new GetCredentialRequest(new ArrayList<>()));

    }

    @Test
    public void constructor() {
        boolean expectedIsAutoSelectAllowed = true;
        ArrayList<CredentialOption> expectedCredentialOptions = new ArrayList<>();
        expectedCredentialOptions.add(new GetPasswordOption());
        expectedCredentialOptions.add(new GetPublicKeyCredentialOption("json"));

        GetCredentialRequest request = new GetCredentialRequest(expectedCredentialOptions,
                expectedIsAutoSelectAllowed);

        assertThat(request.isAutoSelectAllowed()).isEqualTo(expectedIsAutoSelectAllowed);
        assertThat(request.getCredentialOptions()).hasSize(expectedCredentialOptions.size());
        for (int i = 0; i < expectedCredentialOptions.size(); i++) {
            assertThat(request.getCredentialOptions().get(i)).isEqualTo(
                    expectedCredentialOptions.get(i));
        }
    }

    @Test
    public void constructor_defaultAutoSelect() {
        ArrayList<CredentialOption> options = new ArrayList<>();
        options.add(new GetPasswordOption());

        GetCredentialRequest request = new GetCredentialRequest(options);

        assertThat(request.isAutoSelectAllowed()).isFalse();
    }

    @Test
    public void builder_addCredentialOption() {
        boolean expectedIsAutoSelectAllowed = true;
        ArrayList<CredentialOption> expectedCredentialOptions = new ArrayList<>();
        expectedCredentialOptions.add(new GetPasswordOption());
        expectedCredentialOptions.add(new GetPublicKeyCredentialOption("json"));

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(expectedCredentialOptions.get(0))
                .addCredentialOption(expectedCredentialOptions.get(1))
                .setAutoSelectAllowed(expectedIsAutoSelectAllowed)
                .build();

        assertThat(request.isAutoSelectAllowed()).isEqualTo(expectedIsAutoSelectAllowed);
        assertThat(request.getCredentialOptions()).hasSize(expectedCredentialOptions.size());
        for (int i = 0; i < expectedCredentialOptions.size(); i++) {
            assertThat(request.getCredentialOptions().get(i)).isEqualTo(
                    expectedCredentialOptions.get(i));
        }
    }

    @Test
    public void builder_setCredentialOptions() {
        boolean expectedIsAutoSelectAllowed = true;
        ArrayList<CredentialOption> expectedCredentialOptions = new ArrayList<>();
        expectedCredentialOptions.add(new GetPasswordOption());
        expectedCredentialOptions.add(new GetPublicKeyCredentialOption("json"));

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .setCredentialOptions(expectedCredentialOptions)
                .setAutoSelectAllowed(expectedIsAutoSelectAllowed)
                .build();

        assertThat(request.isAutoSelectAllowed()).isEqualTo(expectedIsAutoSelectAllowed);
        assertThat(request.getCredentialOptions()).hasSize(expectedCredentialOptions.size());
        for (int i = 0; i < expectedCredentialOptions.size(); i++) {
            assertThat(request.getCredentialOptions().get(i)).isEqualTo(
                    expectedCredentialOptions.get(i));
        }
    }

    @Test
    public void builder_defaultAutoSelect() {
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(new GetPasswordOption())
                .build();

        assertThat(request.isAutoSelectAllowed()).isFalse();
    }
}
