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

import android.app.appsearch.GenericDocument;
import android.os.Bundle;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/**
 * A request to execute an app function.
 *
 * <p>The {@link ExecuteAppFunctionRequest#getParameters()} contains the parameters for the function
 * to be executed in a GenericDocument. Structured classes defined in the AppFunction SDK can be
 * converted into GenericDocuments.
 *
 * <p>The {@link ExecuteAppFunctionRequest#getExtras()} provides any extra metadata for the request.
 * Structured APIs can be exposed in the SDK by packing and unpacking this Bundle.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class ExecuteAppFunctionRequest {
    /** Returns the package name of the app that hosts the function. */
    @NonNull
    public String getTargetPackageName() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the unique string identifier of the app function to be executed.
     *
     * <p>When there is a package change or the device starts up, the metadata of available
     * functions is indexed by AppSearch. AppSearch stores the indexed information as {@code
     * AppFunctionStaticMetadata} document.
     *
     * <p>The ID can be obtained by querying the {@code AppFunctionStaticMetadata} documents from
     * AppSearch.
     *
     * <p>If the {@code functionId} provided is invalid, the caller will get an invalid argument
     * response.
     */
    @NonNull
    public String getFunctionIdentifier() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the function parameters. The key is the parameter name, and the value is the
     * parameter value.
     *
     * <p>The {@link GenericDocument} may have missing parameters. Developers are advised to
     * implement defensive handling measures.
     *
     * <p>Similar to {@link #getFunctionIdentifier()} the parameters required by a function can be
     * obtained by querying AppSearch for the corresponding {@code AppFunctionStaticMetadata}. This
     * metadata will contain enough information for the caller to resolve the required parameters
     * either using information from the metadata itself or using the AppFunction SDK for function
     * callers.
     */
    @NonNull
    public GenericDocument getParameters() {
        throw new RuntimeException("Stub!");
    }

    /** Returns the additional data relevant to this function execution. */
    @NonNull
    public Bundle getExtras() {
        throw new RuntimeException("Stub!");
    }

    /** Builder for {@link ExecuteAppFunctionRequest}. */
    public static final class Builder {
        public Builder(@NonNull String targetPackageName, @NonNull String functionIdentifier) {
            throw new RuntimeException("Stub!");
        }

        /** Sets the additional data relevant to this function execution. */
        @NonNull
        public Builder setExtras(@NonNull Bundle extras) {
            throw new RuntimeException("Stub!");
        }

        /** Sets the function parameters. */
        @NonNull
        public Builder setParameters(@NonNull GenericDocument parameters) {
            throw new RuntimeException("Stub!");
        }

        /** Builds the {@link ExecuteAppFunctionRequest}. */
        @NonNull
        public ExecuteAppFunctionRequest build() {
            throw new RuntimeException("Stub!");
        }
    }
}
