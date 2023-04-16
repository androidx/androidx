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

package androidx.credentials.provider;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.pm.SigningInfo;
import android.os.Bundle;

import androidx.core.os.BuildCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BeginCreateCustomCredentialRequestJavaTest {

    @Test
    public void constructor_success() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }

        new BeginCreateCustomCredentialRequest("type", Bundle.EMPTY, null);
    }

    @Test
    public void constructor_nullTypeBundle_throws() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }

        // TODO(b/275416815) - parameterize to account for all individually
        assertThrows("Expected null list to throw NPE",
                NullPointerException.class,
                () -> new BeginCreateCustomCredentialRequest(null, null,
                        new CallingAppInfo(
                        "package", new SigningInfo()))
        );
    }

    @Test
    public void getter_type() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        String expectedType = "ironman";

        BeginCreateCustomCredentialRequest beginCreateCustomCredentialRequest =
                new BeginCreateCustomCredentialRequest(expectedType, Bundle.EMPTY, null);
        String actualType = beginCreateCustomCredentialRequest.getType();

        assertThat(actualType).isEqualTo(expectedType);
    }

    @Test
    public void getter_bundle() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        String expectedKey = "query";
        String expectedValue = "data";
        Bundle expectedBundle = new Bundle();
        expectedBundle.putString(expectedKey, expectedValue);

        BeginCreateCustomCredentialRequest beginCreateCustomCredentialRequest =
                new BeginCreateCustomCredentialRequest("type", expectedBundle, null);
        Bundle actualBundle = beginCreateCustomCredentialRequest.getCandidateQueryData();

        assertThat(actualBundle.getString(expectedKey)).isEqualTo(expectedValue);
    }
}
