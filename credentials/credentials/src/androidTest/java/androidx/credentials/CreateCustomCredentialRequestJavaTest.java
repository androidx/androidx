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

import android.os.Bundle;

import org.junit.Test;

public class CreateCustomCredentialRequestJavaTest {
    @Test
    public void constructor_nullType_throws() {
        assertThrows("Expected null type to throw NPE",
                NullPointerException.class,
                () -> new CreateCustomCredentialRequest(null, new Bundle(), new Bundle(), false,
                        new CreateCredentialRequest.DisplayInfo("userId"))
        );
    }

    @Test
    public void constructor_nullCredentialData_throws() {
        assertThrows(
                NullPointerException.class,
                () -> new CreateCustomCredentialRequest("T", null, new Bundle(), true,
                        new CreateCredentialRequest.DisplayInfo("userId"))
        );
    }

    @Test
    public void constructor_nullCandidateQueryData_throws() {
        assertThrows(
                NullPointerException.class,
                () -> new CreateCustomCredentialRequest("T", new Bundle(), null, true,
                        new CreateCredentialRequest.DisplayInfo("userId"))
        );
    }

    @Test
    public void constructor_emptyType_throws() {
        assertThrows("Expected empty type to throw IAE",
                IllegalArgumentException.class,
                () -> new CreateCustomCredentialRequest("", new Bundle(), new Bundle(), false,
                        new CreateCredentialRequest.DisplayInfo("userId"))
        );
    }

    @Test
    public void constructor_nonEmptyTypeNonNullBundle_success() {
        new CreateCustomCredentialRequest("T", new Bundle(), new Bundle(), true,
                new CreateCredentialRequest.DisplayInfo("userId"));
    }

    @Test
    public void getter() {
        String expectedType = "TYPE";
        Bundle expectedCredentialDataBundle = new Bundle();
        expectedCredentialDataBundle.putString("Test", "Test");
        Bundle expectedCandidateQueryDataBundle = new Bundle();
        expectedCandidateQueryDataBundle.putBoolean("key", true);
        CreateCredentialRequest.DisplayInfo expectedDisplayInfo =
                new CreateCredentialRequest.DisplayInfo("userId");
        boolean expectedSystemProvider = true;

        CreateCustomCredentialRequest request = new CreateCustomCredentialRequest(expectedType,
                expectedCredentialDataBundle,
                expectedCandidateQueryDataBundle,
                expectedSystemProvider,
                expectedDisplayInfo);

        assertThat(request.getType()).isEqualTo(expectedType);
        assertThat(TestUtilsKt.equals(request.getCredentialData(), expectedCredentialDataBundle))
                .isTrue();
        assertThat(TestUtilsKt.equals(request.getCandidateQueryData(),
                expectedCandidateQueryDataBundle)).isTrue();
        assertThat(request.isSystemProviderRequired()).isEqualTo(expectedSystemProvider);
        assertThat(request.getDisplayInfo$credentials_debug()).isEqualTo(expectedDisplayInfo);
    }

    @Test
    public void constructionWithNullRequestDisplayInfo_throws() {
        assertThrows(
                NullPointerException.class, () -> new CreateCustomCredentialRequest("type",
                        new Bundle(), new Bundle(), false, /* requestDisplayInfo= */null));
    }
}
