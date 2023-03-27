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

package androidx.credentials.exceptions.domerror;

import static com.google.common.truth.Truth.assertThat;

import androidx.credentials.exceptions.domerrors.QuotaExceededError;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class QuotaExceededDomErrorJavaTest {
    @Test
    public void construct_success() {
        new QuotaExceededError();
    }

    @Test
    public void getter_success() {
        QuotaExceededError error =
                new QuotaExceededError();
        String expectedType = QuotaExceededError
                .TYPE_CREATE_PUBLIC_KEY_CREDENTIAL_QUOTA_EXCEEDED_ERROR;
        assertThat(error.getType()).isEqualTo(expectedType);
    }
}
