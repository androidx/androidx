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

import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.credentials.Credential;
import android.os.OutcomeReceiver;
import android.widget.EditText;

import androidx.annotation.RequiresApi;
import androidx.credentials.internal.FrameworkImplHelper;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;

import kotlin.Unit;

import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@SdkSuppress(minSdkVersion = 35, codeName = "VanillaIceCream")
public class CredentialManagerViewHandlerJavaTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();

    private static final GetCredentialRequest GET_CRED_PASSWORD_REQ =
            new GetCredentialRequest.Builder()
                    .setCredentialOptions(Collections.singletonList(
                            new GetPasswordOption())).build();
    private static final android.credentials.GetCredentialRequest GET_CRED_PASSWORD_FRAMEWORK_REQ =
            FrameworkImplHelper.convertGetRequestToFrameworkClass(GET_CRED_PASSWORD_REQ);

    @Test
    @RequiresApi(35)
    public void setPendingCredentialRequest_frameworkAttrSetSuccessfully() {
        EditText editText = new EditText(mContext);

        PendingGetCredentialRequest pendingGetCredentialRequest = new PendingGetCredentialRequest(
                GET_CRED_PASSWORD_REQ,
                (response) -> Unit.INSTANCE);

        CredentialManagerViewHandler.setPendingGetCredentialRequest(editText,
                pendingGetCredentialRequest);

        assertNotNull(editText.getPendingCredentialRequest());
        TestUtilsKt.equals(editText.getPendingCredentialRequest(),
                GET_CRED_PASSWORD_FRAMEWORK_REQ);
        assertThat(editText.getPendingCredentialCallback()).isInstanceOf(
                OutcomeReceiver.class
        );
    }

    @Test
    @RequiresApi(35)
    public void setPendingCredentialRequest_callbackInvokedSuccessfully()
            throws InterruptedException {
        CountDownLatch latch1 = new CountDownLatch(1);
        AtomicReference<GetCredentialResponse> getCredentialResponse = new AtomicReference<>();
        EditText editText = new EditText(mContext);

        PendingGetCredentialRequest pendingGetCredentialRequest = new PendingGetCredentialRequest(
                GET_CRED_PASSWORD_REQ,
                (response) -> {
                    getCredentialResponse.set(response);
                    latch1.countDown();
                    return Unit.INSTANCE;
                });

        CredentialManagerViewHandler.setPendingGetCredentialRequest(editText,
                pendingGetCredentialRequest);

        assertNotNull(editText.getPendingCredentialRequest());
        TestUtilsKt.equals(editText.getPendingCredentialRequest(), GET_CRED_PASSWORD_FRAMEWORK_REQ);
        assertThat(editText.getPendingCredentialCallback()).isInstanceOf(
                OutcomeReceiver.class
        );

        PasswordCredential passwordCredential = new PasswordCredential("id", "password");
        android.credentials.GetCredentialResponse frameworkPasswordResponse =
                new android.credentials.GetCredentialResponse(new Credential(
                        passwordCredential.getType(), passwordCredential.getData()));
        assertNotNull(editText.getPendingCredentialCallback());
        editText.getPendingCredentialCallback().onResult(frameworkPasswordResponse);
        latch1.await(50L, TimeUnit.MILLISECONDS);

        assertThat(getCredentialResponse.get()).isNotNull();
        GetCredentialResponse expectedGetCredentialResponse = FrameworkImplHelper
                .convertGetResponseToJetpackClass(frameworkPasswordResponse);
        TestUtilsKt.equals(expectedGetCredentialResponse, getCredentialResponse.get());
    }
}
