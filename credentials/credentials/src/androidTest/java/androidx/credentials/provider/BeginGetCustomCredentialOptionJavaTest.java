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

import static org.junit.Assert.assertThrows;

import android.os.Bundle;

import androidx.credentials.TestUtilsKt;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 28)
@SmallTest
public class BeginGetCustomCredentialOptionJavaTest {
    @Test
    public void constructor_success() {
        Bundle expectedBundle = new Bundle();
        expectedBundle.putString("random", "random_value");
        String expectedType = "type";
        String expectedId = "id";

        BeginGetCustomCredentialOption option = new BeginGetCustomCredentialOption(
                expectedId, expectedType, expectedBundle);

        assertThat(option.getType()).isEqualTo(expectedType);
        assertThat(option.getId()).isEqualTo(expectedId);
        assertThat(TestUtilsKt.equals(option.getCandidateQueryData(), expectedBundle)).isTrue();
    }

    @Test
    public void constructor_emptyType_throwsIAE() {

        assertThrows("Expected empty Json to throw error",
                IllegalArgumentException.class,
                () -> new BeginGetCustomCredentialOption(
                        "id",
                        "",
                        new Bundle()
                )
        );
    }

    @Test
    public void constructor_emptyId_throwsIAE() {
        assertThrows("Expected empty Json to throw error",
                IllegalArgumentException.class,
                () -> new BeginGetCustomCredentialOption(
                        "",
                        "type",
                        new Bundle()
                )
        );
    }
}
