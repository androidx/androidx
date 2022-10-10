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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CredentialManagerJavaTest {

    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();

    private CredentialManager mCredentialManager;

    @Before
    public void setup() {
        mCredentialManager = CredentialManager.create(mContext);
    }

    @Test
    public void testCreateCredentialAsyc() {
        assertThrows(UnsupportedOperationException.class,
                () -> mCredentialManager.executeCreateCredentialAsync(
                        new CreatePasswordRequest("test-user-id", "test-password"),
                        null,
                        null,
                        Runnable::run,
                        new CredentialManagerCallback<CreateCredentialResponse>() {
                            @Override
                            public void onError(@NonNull CredentialManagerException e) {}

                            @Override
                            public void onResult(CreateCredentialResponse result) {}
                        })
        );
    }

    @Test
    public void testGetCredentialAsyc() {
        assertThrows(UnsupportedOperationException.class,
                () -> mCredentialManager.executeGetCredentialAsync(
                        new GetCredentialRequest.Builder()
                                .addGetCredentialOption(new GetPasswordOption())
                                .build(),
                        null,
                        null,
                        Runnable::run,
                        new CredentialManagerCallback<GetCredentialResponse>() {
                            @Override
                            public void onError(@NonNull CredentialManagerException e) {}

                            @Override
                            public void onResult(GetCredentialResponse result) {}
                        })
        );
    }
}
