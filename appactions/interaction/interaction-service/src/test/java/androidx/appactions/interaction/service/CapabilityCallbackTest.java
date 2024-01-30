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

package androidx.appactions.interaction.service;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal;
import androidx.appactions.interaction.proto.FulfillmentResponse;
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput;
import androidx.appactions.interaction.proto.ParamValue;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class CapabilityCallbackTest {

    private static final long CB_TIMEOUT = 1000L;
    private static final FulfillmentResponse RESPONSE =
            FulfillmentResponse.newBuilder()
                    .setExecutionOutput(
                            FulfillmentResponse.StructuredOutput.newBuilder()
                                    .addOutputValues(
                                            StructuredOutput.OutputValue.newBuilder()
                                                    .addValues(
                                                            ParamValue.newBuilder()
                                                                    .setStringValue("abcd"))))
                    .build();

    @Test
    public void callbackImpl_callbackSuccess_forwardsToCompleter() throws Exception {
        ListenableFuture<FulfillmentResponse> future =
                CallbackToFutureAdapter.getFuture(
                        completer -> {
                            CapabilityCallback cb = new CapabilityCallback(completer);
                            cb.onSuccess(RESPONSE);
                            return "test future";
                        });

        assertThat(future.get(CB_TIMEOUT, MILLISECONDS)).isEqualTo(RESPONSE);
    }

    @Test
    public void callbackImpl_callbackError_failsCompleter() throws Exception {
        ListenableFuture<FulfillmentResponse> future =
                CallbackToFutureAdapter.getFuture(
                        completer -> {
                            CapabilityCallback cb = new CapabilityCallback(completer);
                            cb.onError(ErrorStatusInternal.CANCELED);
                            return "test future";
                        });

        assertThat(future.isDone()).isTrue();
        ExecutionException e =
                assertThrows(ExecutionException.class, () -> future.get(CB_TIMEOUT, MILLISECONDS));
        assertThat(e.getCause().getMessage()).isEqualTo("Error executing action capability");
    }
}
