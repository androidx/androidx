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

import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

/**
 * Combines with {@link GetPublicKeyCredentialOptionPrivilegedJavaTest} for full tests.
 */
@RunWith(Parameterized.class)
public class GetPublicKeyCredentialOptionPrivilegedFailureInputsJavaTest {
    private String mRequestJson;
    private String mRp;
    private String mClientDataHash;

    private String mNullRequestJson;
    private String mNullRp;
    private String mNullClientDataHash;

    public GetPublicKeyCredentialOptionPrivilegedFailureInputsJavaTest(String requestJson,
            String rp, String clientDataHash, String mNullRequestJson, String mNullRp,
            String mNullClientDataHash) {
        this.mRequestJson = requestJson;
        this.mRp = rp;
        this.mClientDataHash = clientDataHash;
        this.mNullRequestJson = mNullRequestJson;
        this.mNullRp = mNullRp;
        this.mNullClientDataHash = mNullClientDataHash;
    }

    @Parameterized.Parameters
    public static Iterable<String[]> failureCases() {
        // Allows checking improper formations for builder and normal construction.
        // Handles both null and empty cases.
        // For successful cases, see the non parameterized privileged tests.
        return Arrays.asList(new String[][] {
                { "{\"hi\":21}", "rp", "", null, "rp", "hash"},
                { "", "rp", "clientDataHash", "{\"hi\":21}", null, "hash"},
                { "{\"hi\":21}", "", "clientDataHash", "{\"hi\":21}", "rp", null}
        });
    }

    @Test
    public void constructor_emptyInput_throwsIllegalArgumentException() {
        assertThrows("If at least one arg empty, should throw IllegalArgumentException",
                IllegalArgumentException.class,
                () -> new GetPublicKeyCredentialOptionPrivileged(this.mRequestJson, this.mRp,
                        this.mClientDataHash)
        );
    }

    @Test
    public void builder_build_emptyInput_IllegalArgumentException() {
        GetPublicKeyCredentialOptionPrivileged.Builder builder =
                new GetPublicKeyCredentialOptionPrivileged.Builder(mRequestJson, mRp,
                        mClientDataHash);
        assertThrows("If at least one arg empty to builder, should throw "
                        + "IllegalArgumentException",
                IllegalArgumentException.class,
                () -> builder.build()
        );
    }

    @Test
    public void constructor_nullInput_throwsNullPointerException() {
        convertAPIIssueToProperNull();
        assertThrows(
                "If at least one arg null, should throw NullPointerException",
                NullPointerException.class,
                () -> new GetPublicKeyCredentialOptionPrivileged(this.mNullRequestJson,
                        this.mNullRp,
                        this.mNullClientDataHash)
        );
    }

    @Test
    public void builder_build_nullInput_throwsNullPointerException() {
        convertAPIIssueToProperNull();
        assertThrows(
                "If at least one arg null to builder, should throw NullPointerException",
                NullPointerException.class,
                () -> new GetPublicKeyCredentialOptionPrivileged.Builder(mNullRequestJson,
                        mNullRp, mNullClientDataHash).build()
        );
    }

    // Certain API levels have parameterized tests that automatically convert null to a string
    // 'null' causing test failures. Until Parameterized tests fixes this bug, this is the
    // workaround. Note this is *not* always the case but only for certain API levels (we have
    // recorded 21, 22, and 23 as such levels).
    private void convertAPIIssueToProperNull() {
        if (mNullRequestJson != null && mNullRequestJson.equals("null")) {
            mNullRequestJson = null;
        }
        if (mNullRp != null && mNullRp.equals("null")) {
            mNullRp = null;
        }
        if (mNullClientDataHash != null && mNullClientDataHash.equals("null")) {
            mNullClientDataHash = null;
        }
    }
}
