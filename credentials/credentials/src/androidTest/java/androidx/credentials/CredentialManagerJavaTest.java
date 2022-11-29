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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.CreateCredentialException;
import androidx.credentials.exceptions.CreateCredentialUnknownException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.GetCredentialUnknownException;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

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
    public void testCreateCredentialAsyc_successCallbackThrows() {
        AtomicReference<CreateCredentialException> loadedResult = new AtomicReference<>();
        mCredentialManager.executeCreateCredentialAsync(
                new CreatePasswordRequest("test-user-id", "test-password"),
                null,
                null,
                Runnable::run,
                new CredentialManagerCallback<CreateCredentialResponse,
                        CreateCredentialException>() {
                    @Override
                    public void onError(@NonNull CreateCredentialException e) {
                        loadedResult.set(e);
                    }

                    @Override
                    public void onResult(@NonNull CreateCredentialResponse result) {}
            });
        assertThat(loadedResult.get().getType()).isEqualTo(
                CreateCredentialUnknownException.TYPE_CREATE_CREDENTIAL_UNKNOWN_EXCEPTION);
        // TODO("Add manifest tests and separate tests for pre and post U API Levels")
    }

    @Test
    public void testGetCredentialAsyc_successCallbackThrows() {
        AtomicReference<GetCredentialException> loadedResult = new AtomicReference<>();
        mCredentialManager.executeGetCredentialAsync(
                new GetCredentialRequest.Builder()
                        .addGetCredentialOption(new GetPasswordOption())
                        .build(),
                null,
                null,
                Runnable::run,
                new CredentialManagerCallback<GetCredentialResponse,
                    GetCredentialException>() {
                @Override
                public void onError(@NonNull GetCredentialException e) {
                    loadedResult.set(e);
                }

                @Override
                public void onResult(@NonNull GetCredentialResponse result) {}
            });
        assertThat(loadedResult.get().getType()).isEqualTo(
                GetCredentialUnknownException.TYPE_GET_CREDENTIAL_UNKNOWN_EXCEPTION);
        // TODO("Add manifest tests and separate tests for pre and post U API Levels")
    }

    @Test
    public void testClearCredentialSessionAsync_throws() {
        assertThrows(UnsupportedOperationException.class,
                () -> mCredentialManager.clearCredentialStateAsync(
                    new ClearCredentialStateRequest(),
                    null,
                    Runnable::run,
                    new CredentialManagerCallback<Void,
                            ClearCredentialException>() {
                        @Override
                        public void onError(@NonNull ClearCredentialException e) {}

                        @Override
                        public void onResult(@NonNull Void result) {}
                    })
        );
        // TODO(Add manifest tests and separate tests for pre and post U API Levels")
    }
}
