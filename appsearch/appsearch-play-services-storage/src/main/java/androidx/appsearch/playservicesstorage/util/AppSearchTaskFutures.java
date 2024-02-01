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

package androidx.appsearch.playservicesstorage.util;

import static androidx.appsearch.app.AppSearchResult.RESULT_NOT_FOUND;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.util.LogUtil;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.util.Function;

import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/** Utilities for converting {@link Task} to {@link ListenableFuture}. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppSearchTaskFutures {
    private static final String TAG = "AppSearchTaskFutures";
    private AppSearchTaskFutures() {}

    /**
     * Converts a {@link Task} to a {@link ListenableFuture} to more easily interact with other
     * androidx apis.
     * <p>Note: Calling {@link java.util.concurrent.Future#cancel(boolean)} on the returned result
     * is a no-op since {@link Task} has no equivalent method.
     *
     * @param task The {@link Task} that needs to be converted to {@link ListenableFuture}.
     * @param valueMapper The transformation function to apply to the task's result.
     * @param executor The {@link Executor} to execute task's onCompleteListener logic.
     */
    @NonNull
    public static <GmsType, JetpackType> ListenableFuture<JetpackType> toListenableFuture(
            @NonNull Task<GmsType> task,
            @NonNull Function<GmsType, JetpackType> valueMapper,
            @NonNull Executor executor) {
        return CallbackToFutureAdapter.getFuture(
                completer -> task.addOnCompleteListener(
                        executor,
                        completedTask -> {
                            if (completedTask.isCanceled()) {
                                completer.setCancelled();
                            } else if (completedTask.isSuccessful()) {
                                JetpackType jetpackType = valueMapper.apply(
                                        completedTask.getResult());
                                completer.set(jetpackType);
                            } else {
                                Exception exception = task.getException();
                                completer.setException(toJetpackException(exception));
                            }
                        }));
    }

    /**
     * Converts the given Exception to AppSearchException if from PlayServicesAppSearch otherwise
     * just returns it.
     */
    @NonNull
    private static Exception toJetpackException(@NonNull Exception exception) {
        if (exception instanceof com.google.android.gms.appsearch.exceptions.AppSearchException) {
            com.google.android.gms.appsearch.exceptions.AppSearchException
                    gmsException =
                    (com.google.android.gms.appsearch.exceptions.AppSearchException) exception;
            if (gmsException.getResultCode() == RESULT_NOT_FOUND && LogUtil.DEBUG) {
                // Log for traceability. NOT_FOUND is logged at VERBOSE because this
                // error can occur during the regular operation of the system
                // (b/183550974). Everything else is indicative of an actual
                // problem and is logged at WARN.
                Log.v(TAG, "Failed to call PlayServicesAppSearch: "
                        + exception);
            }

            return new AppSearchException(
                    gmsException.getResultCode(),
                    gmsException.getMessage());
        }

        Log.w(TAG, "Failed to call PlayServicesAppSearch", exception);
        return exception;
    }
}
