/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.annotation.RequiresApi;
import androidx.test.filters.SdkSuppress;

import kotlin.Unit;

import org.junit.Test;

import java.util.Collections;

@SdkSuppress(minSdkVersion = 35, codeName = "VanillaIceCream")
public class PendingGetCredentialRequestJavaTest {
    @Test
    @RequiresApi(35)
    public void constructor_setAndGetRequestThroughViewTag() {
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .setCredentialOptions(Collections.singletonList(new GetPasswordOption()))
                .build();
        PendingGetCredentialRequest pendingGetCredentialRequest =
                new PendingGetCredentialRequest(request,
                        (response) -> Unit.INSTANCE);

        assertThat(pendingGetCredentialRequest.getRequest())
                .isSameInstanceAs(request);
    }
}
