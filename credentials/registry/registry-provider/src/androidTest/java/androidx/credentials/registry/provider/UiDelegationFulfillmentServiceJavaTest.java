/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.registry.provider;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;

import androidx.core.os.OutcomeReceiverCompat;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.provider.ProviderGetCredentialRequest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class UiDelegationFulfillmentServiceJavaTest {
    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();

    private static class TestJavaFulfillmentService extends UiDelegationFulfillmentService {
        final AtomicReference<ProviderGetCredentialRequest> mLastRequest =
                new AtomicReference<>();
        final AtomicReference<OutcomeReceiverCompat<GetCredentialResponse, GetCredentialException>>
                mLastCallback = new AtomicReference<>();

        TestJavaFulfillmentService(Context context) {
            attachBaseContext(context);
        }

        @Override
        public void onGetCredentialRequest(
                @NonNull ProviderGetCredentialRequest request,
                @NonNull OutcomeReceiverCompat<GetCredentialResponse,
                        GetCredentialException> callback) {
            mLastRequest.set(request);
            mLastCallback.set(callback);
        }
    }

    @Test
    public void onBind_nullIntent_returnsNull() {
        TestJavaFulfillmentService service = new TestJavaFulfillmentService(mContext);
        assertThat(service.onBind(null)).isNull();
    }

    @Test
    public void onBind_emptyIntent_returnsNull() {
        TestJavaFulfillmentService service = new TestJavaFulfillmentService(mContext);
        Intent intent = new Intent("androidx.credentials.action.GET_CREDENTIAL_SERVICE");
        assertThat(service.onBind(intent)).isNull();
    }

    @Test
    public void onGetCredentialRequest_parametersReceived() {
        TestJavaFulfillmentService service = new TestJavaFulfillmentService(mContext);
        ProviderGetCredentialRequest request = new ProviderGetCredentialRequest(
                Collections.emptyList(), TestUtilsKt.getTestCallingAppInfo("https://example.com"));
        OutcomeReceiverCompat<GetCredentialResponse, GetCredentialException> callback =
                new OutcomeReceiverCompat<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {}

                    @Override
                    public void onError(@NonNull GetCredentialException error) {}
                };

        service.onGetCredentialRequest(request, callback);

        assertThat(service.mLastRequest.get()).isSameInstanceAs(request);
        assertThat(service.mLastCallback.get()).isSameInstanceAs(callback);
    }
}
