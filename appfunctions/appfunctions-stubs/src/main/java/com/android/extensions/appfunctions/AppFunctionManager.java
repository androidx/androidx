/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.extensions.appfunctions;

import android.content.Context;
import android.os.CancellationSignal;
import android.os.OutcomeReceiver;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * Provides access to app functions.
 *
 * <p>An app function is a piece of functionality that apps expose to the system for cross-app
 * orchestration.
 *
 * <p>**Building App Functions:**
 *
 * <p>Most developers should build app functions through the AppFunctions SDK. This SDK library
 * offers a more convenient and type-safe way to build app functions. The SDK provides predefined
 * function schemas for common use cases and associated data classes for function parameters and
 * return values. Apps only have to implement the provided interfaces. Internally, the SDK converts
 * these data classes into {@link ExecuteAppFunctionRequest#getParameters()} and {@link
 * ExecuteAppFunctionResponse#getResultDocument()}.
 *
 * <p>**Discovering App Functions:**
 *
 * <p>When there is a package change or the device starts up, the metadata of available functions is
 * indexed on-device by AppSearch. AppSearch stores the indexed information as an
 * {@code AppFunctionStaticMetadata} document. This document contains the {@code functionIdentifier}
 * and the schema information that the app function implements. This allows other apps and the app
 * itself to discover these functions using the AppSearch search APIs. Visibility to this metadata
 * document is based on the packages that have visibility to the app providing the app functions.
 * AppFunction SDK provides a convenient way to achieve this and is the preferred method.
 *
 * <p>**Executing App Functions:**
 *
 * <p>To execute an app function, the caller app can retrieve the {@code functionIdentifier} from
 * the {@code AppFunctionStaticMetadata} document and use it to build an {@link
 * ExecuteAppFunctionRequest}. Then, invoke {@link #executeAppFunction} with the request to execute
 * the app function. Callers need the {@code android.permission.EXECUTE_APP_FUNCTIONS} or {@code
 * android.permission.EXECUTE_APP_FUNCTIONS_TRUSTED} permission to execute app functions from other
 * apps. An app can always execute its own app functions and doesn't need these permissions.
 * AppFunction SDK provides a convenient way to achieve this and is the preferred method.
 *
 * <p>**Example:**
 *
 * <p>An assistant app is trying to fulfill the user request "Save XYZ into my note". The assistant
 * app should first list all available app functions as {@code AppFunctionStaticMetadata} documents
 * from AppSearch. Then, it should identify an app function that implements the {@code CreateNote}
 * schema. Finally, the assistant app can invoke {@link #executeAppFunction} with the {@code
 * functionIdentifier} of the chosen function.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class AppFunctionManager {
    /**
     * The default state of the app function. Call {@link #setAppFunctionEnabled} with this to reset
     * enabled state to the default value.
     */
    public static final int APP_FUNCTION_STATE_DEFAULT = 0;

    /**
     * The app function is enabled. To enable an app function, call {@link #setAppFunctionEnabled}
     * with this value.
     */
    public static final int APP_FUNCTION_STATE_ENABLED = 1;

    /**
     * The app function is disabled. To disable an app function, call {@link #setAppFunctionEnabled}
     * with this value.
     */
    public static final int APP_FUNCTION_STATE_DISABLED = 2;

    /**
     * The enabled state of the app function.
     */
    @IntDef(
            value = {
                    APP_FUNCTION_STATE_DEFAULT,
                    APP_FUNCTION_STATE_ENABLED,
                    APP_FUNCTION_STATE_DISABLED
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface EnabledState {
    }

    /**
     * Creates an instance.
     *
     * @param context A {@link Context}.
     */
    public AppFunctionManager(@NonNull Context context) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Executes the app function.
     *
     * <p>Proxies request and response to the underlying
     * {@link AppFunctionManager#executeAppFunction}, converting the request and
     * response in the appropriate type required by the function.
     *
     * <p>See {@link AppFunctionManager#executeAppFunction} for the
     * documented behaviour of this method.
     */
    public void executeAppFunction(
            @NonNull ExecuteAppFunctionRequest sidecarRequest,
            @NonNull Executor executor,
            @NonNull CancellationSignal cancellationSignal,
            @NonNull
            OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException>
                    callback) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns a boolean through a callback, indicating whether the app function is enabled.
     *
     * <p>See {@link AppFunctionManager#isAppFunctionEnabled} for the documented behaviour of
     * this method.
     */
    public void isAppFunctionEnabled(
            @NonNull String functionIdentifier,
            @NonNull String targetPackage,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Boolean, Exception> callback) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns a boolean through a callback, indicating whether the app function is enabled.
     *
     * <p>See {@link AppFunctionManager#isAppFunctionEnabled} for the documented behaviour of
     * this method.
     */
    public void isAppFunctionEnabled(
            @NonNull String functionIdentifier,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Boolean, Exception> callback) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Sets the enabled state of the app function owned by the calling package.
     *
     * <p>See {@link AppFunctionManager#isAppFunctionEnabled} for the documented behaviour of
     * this method.
     */
    public void setAppFunctionEnabled(
            @NonNull String functionIdentifier,
            @EnabledState int newEnabledState,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<Void, Exception> callback) {
        throw new RuntimeException("Stub!");
    }
}
