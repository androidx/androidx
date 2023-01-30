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

package androidx.appactions.interaction.capabilities.core.testing;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.ActionExecutor;
import androidx.appactions.interaction.capabilities.core.ConfirmationOutput;
import androidx.appactions.interaction.capabilities.core.ExecutionResult;
import androidx.appactions.interaction.capabilities.core.impl.CallbackInternal;
import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal;
import androidx.appactions.interaction.capabilities.core.impl.TouchEventCallback;
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures;
import androidx.appactions.interaction.capabilities.core.task.OnDialogFinishListener;
import androidx.appactions.interaction.capabilities.core.task.OnInitListener;
import androidx.appactions.interaction.capabilities.core.task.OnReadyToConfirmListener;
import androidx.appactions.interaction.capabilities.core.testing.spec.SettableFutureWrapper;
import androidx.appactions.interaction.proto.FulfillmentResponse;
import androidx.appactions.interaction.proto.TouchEventMetadata;

import com.google.auto.value.AutoOneOf;
import com.google.auto.value.AutoValue;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class TestingUtils {

    public static final long CB_TIMEOUT = 1000L;

    private TestingUtils() {}

    public static CallbackInternal buildActionCallback(SettableFutureWrapper<Boolean> future) {
        return new CallbackInternal() {
            @Override
            public void onSuccess(FulfillmentResponse response) {
                future.set(true);
            }

            @Override
            public void onError(ErrorStatusInternal error) {
                future.set(false);
            }
        };
    }

    public static CallbackInternal buildActionCallbackWithFulfillmentResponse(
            SettableFutureWrapper<FulfillmentResponse> future) {
        return new CallbackInternal() {
            @Override
            public void onSuccess(FulfillmentResponse response) {
                future.set(response);
            }

            @Override
            public void onError(ErrorStatusInternal error) {
                future.setException(
                        new IllegalStateException(
                                String.format(
                                        "expected FulfillmentResponse, but got ErrorStatus=%s "
                                                + "instead",
                                        error)));
            }
        };
    }

    public static CallbackInternal buildErrorActionCallback(
            SettableFutureWrapper<ErrorStatusInternal> future) {
        return new CallbackInternal() {
            @Override
            public void onSuccess(FulfillmentResponse response) {
                future.setException(
                        new IllegalStateException(
                                "expected ErrorStatus, but got FulfillmentResponse instead"));
            }

            @Override
            public void onError(ErrorStatusInternal error) {
                future.set(error);
            }
        };
    }

    public static <T> Optional<OnInitListener<T>> buildOnInitListener(
            SettableFutureWrapper<T> updaterFuture) {
        return Optional.of(
                new OnInitListener<T>() {
                    @NonNull
                    @Override
                    public ListenableFuture<Void> onInit(T taskUpdater) {
                        updaterFuture.set(taskUpdater);
                        return Futures.immediateVoidFuture();
                    }
                });
    }

    public static <ArgumentT, ConfirmationT>
            Optional<OnReadyToConfirmListener<ArgumentT, ConfirmationT>>
                    buildOnReadyToConfirmListener(SettableFutureWrapper<ArgumentT> future) {
        return Optional.of(
                (finalArgs) -> {
                    future.set(finalArgs);
                    return Futures.immediateFuture(
                            ConfirmationOutput.<ConfirmationT>getDefaultInstanceWithConfirmation());
                });
    }

    public static <ArgumentT, OutputT>
            OnDialogFinishListener<ArgumentT, OutputT> buildOnFinishListener(
                    SettableFutureWrapper<ArgumentT> future) {
        return (finalArgs) -> {
            future.set(finalArgs);
            return Futures.immediateFuture(ExecutionResult.<OutputT>getDefaultInstanceWithOutput());
        };
    }

    public static TouchEventCallback buildTouchEventCallback(
            SettableFutureWrapper<FulfillmentResponse> future) {
        return new TouchEventCallback() {
            @Override
            public void onSuccess(
                    @NonNull FulfillmentResponse fulfillmentResponse,
                    @NonNull TouchEventMetadata touchEventMetadata) {
                future.set(fulfillmentResponse);
            }

            @Override
            public void onError(@NonNull ErrorStatusInternal errorStatus) {}
        };
    }

    @AutoValue
    public abstract static class TouchEventSuccessResult {
        public static TouchEventSuccessResult create(
                FulfillmentResponse fulfillmentResponse, TouchEventMetadata touchEventMetadata) {
            return new AutoValue_TestingUtils_TouchEventSuccessResult(
                    fulfillmentResponse, touchEventMetadata);
        }

        public abstract FulfillmentResponse fulfillmentResponse();

        public abstract TouchEventMetadata touchEventMetadata();
    }

    @AutoOneOf(TouchEventResult.Kind.class)
    public abstract static class TouchEventResult {
        public static TouchEventResult of(TouchEventSuccessResult result) {
            return AutoOneOf_TestingUtils_TouchEventResult.success(result);
        }

        public static TouchEventResult of(ErrorStatusInternal error) {
            return AutoOneOf_TestingUtils_TouchEventResult.error(error);
        }

        public abstract Kind getKind();

        public abstract TouchEventSuccessResult success();

        public abstract ErrorStatusInternal error();

        public enum Kind {
            SUCCESS,
            ERROR
        }
    }

    public static class ReusableTouchEventCallback implements TouchEventCallback {

        AtomicReference<TouchEventResult> mResultRef = new AtomicReference<>();

        @Override
        public void onSuccess(
                @NonNull FulfillmentResponse fulfillmentResponse,
                @NonNull TouchEventMetadata touchEventMetadata) {
            mResultRef.set(
                    TouchEventResult.of(
                            TouchEventSuccessResult.create(
                                    fulfillmentResponse, touchEventMetadata)));
        }

        @Override
        public void onError(@NonNull ErrorStatusInternal errorStatus) {
            mResultRef.set(TouchEventResult.of(errorStatus));
        }

        public TouchEventResult getLastResult() {
            return mResultRef.get();
        }
    }

    public static <ArgumentT, OutputT>
            ActionExecutor<ArgumentT, OutputT> createFakeActionExecutor() {
        return (args) ->
                Futures.immediateFuture(ExecutionResult.<OutputT>getDefaultInstanceWithOutput());
    }
}
