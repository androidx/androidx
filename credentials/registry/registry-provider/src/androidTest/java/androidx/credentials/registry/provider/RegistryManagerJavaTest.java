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

package androidx.credentials.registry.provider;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.credentials.CredentialManagerCallback;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class RegistryManagerJavaTest {
    private final Context mContext = InstrumentationRegistry.getInstrumentation().getContext();
    private RegistryManager mRegistryManager;

    @Before
    public void setup() {
        mRegistryManager = RegistryManager.create(mContext);
    }

    @Test
    public void registerCredentialsAsync_noOptionalModule_throws() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<RegisterCredentialsException> resultCaptor = new AtomicReference<>();

        mRegistryManager.registerCredentialsAsync(
                new RegisterCredentialsRequest("type", "id", "cred".getBytes(),
                        "matcher".getBytes()) {
                },
                null,
                Runnable::run,
                new CredentialManagerCallback<RegisterCredentialsResponse,
                        RegisterCredentialsException>() {
                    @Override
                    public void onResult(RegisterCredentialsResponse result) {}

                    @Override
                    public void onError(@NonNull RegisterCredentialsException e) {
                        resultCaptor.set(e);
                        latch.countDown();
                    }
                }
        );
        latch.await(100L, TimeUnit.MILLISECONDS);
        assertThat(resultCaptor.get()).isInstanceOf(
                RegisterCredentialsConfigurationException.class);
    }
}
