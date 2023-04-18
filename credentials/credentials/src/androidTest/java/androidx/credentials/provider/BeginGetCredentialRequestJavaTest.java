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
import android.service.credentials.CallingAppInfo;

import androidx.core.os.BuildCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

@SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BeginGetCredentialRequestJavaTest {

    @Test
    public void constructor_success() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }

        new BeginGetCredentialRequest(Collections.emptyList(), null);
    }

    @Test
    public void constructor_nullList_throws() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }

        assertThrows("Expected null list to throw NPE",
                NullPointerException.class,
                () -> new BeginGetCredentialRequest(null,
                        new CallingAppInfo("tom.cruise.security",
                                new SigningInfo()))
        );
    }

    @Test
    public void getter_beginGetCredentialOptions() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        String expectedKey = "query";
        String expectedValue = "data";
        Bundle expectedBundle = new Bundle();
        expectedBundle.putString(expectedKey, expectedValue);
        String expectedId = "key";
        String expectedType = "mach-10";
        int expectedBeginGetCredentialOptionsSize = 1;

        BeginGetCredentialRequest beginGetCredentialRequest =
                new BeginGetCredentialRequest(Collections.singletonList(
                        new BeginGetCustomCredentialOption(expectedId, expectedType,
                                expectedBundle)),
                        null);
        List<BeginGetCredentialOption> actualBeginGetCredentialOptionList =
                beginGetCredentialRequest.getBeginGetCredentialOptions();
        int actualBeginGetCredentialOptionsSize = actualBeginGetCredentialOptionList.size();
        assertThat(actualBeginGetCredentialOptionsSize)
                .isEqualTo(expectedBeginGetCredentialOptionsSize);
        String actualBundleValue =
                actualBeginGetCredentialOptionList.get(0).getCandidateQueryData()
                        .getString(expectedKey);
        String actualId = actualBeginGetCredentialOptionList.get(0).getId();
        String actualType = actualBeginGetCredentialOptionList.get(0).getType();

        assertThat(actualBundleValue).isEqualTo(expectedValue);
        assertThat(actualId).isEqualTo(expectedId);
        assertThat(actualType).isEqualTo(expectedType);
    }

    @Test
    public void getter_nullCallingAppInfo() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        CallingAppInfo expectedCallingAppInfo = null;

        BeginGetCredentialRequest beginGetCredentialRequest =
                new BeginGetCredentialRequest(Collections.emptyList(),
                        expectedCallingAppInfo);
        CallingAppInfo actualCallingAppInfo = beginGetCredentialRequest.getCallingAppInfo();

        assertThat(actualCallingAppInfo).isEqualTo(expectedCallingAppInfo);
    }

    @Test
    public void getter_nonNullCallingAppInfo() {
        if (!BuildCompat.isAtLeastU()) {
            return;
        }
        String expectedPackageName = "john.wick.four.credentials";
        CallingAppInfo expectedCallingAppInfo = new CallingAppInfo(expectedPackageName,
                new SigningInfo());

        BeginGetCredentialRequest beginGetCredentialRequest =
                new BeginGetCredentialRequest(Collections.emptyList(),
                        expectedCallingAppInfo);
        CallingAppInfo actualCallingAppInfo = beginGetCredentialRequest.getCallingAppInfo();
        String actualPackageName = actualCallingAppInfo.getPackageName();

        assertThat(actualPackageName).isEqualTo(expectedPackageName);
    }
}
