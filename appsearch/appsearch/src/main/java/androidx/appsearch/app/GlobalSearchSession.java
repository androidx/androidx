/*
 * Copyright 2020 The Android Open Source Project
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
// @exportToFramework:skipFile()
package androidx.appsearch.app;

import androidx.annotation.RequiresFeature;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;

import java.io.Closeable;
import java.util.Set;

/**
 * Provides a connection to all AppSearch databases the querying application has been
 * granted access to.
 *
 * <p>In addition to the querying methods available in {@link ReadOnlyGlobalSearchSession}, this
 * interface may support write operations such as {@link #reportSystemUsageAsync}.
 *
 * <p>All implementations of this interface must be thread safe.
 *
 * @see AppSearchSession
 */
public interface GlobalSearchSession extends ReadOnlyGlobalSearchSession, Closeable {
    /**
     * Opens a batch of AppSearch Blobs for reading.
     *
     * <p>See {@link AppSearchSession#openBlobForReadAsync} for a general description when a blob
     * is open for read.
     *
     * <p class="caution">
     * The returned {@link OpenBlobForReadResponse} must be closed after use to avoid
     * resource leaks. Failing to close it will result in system file descriptor exhaustion.
     * </p>
     *
     * @param handles The {@link AppSearchBlobHandle}s that identifies the blobs.
     * @return a response containing the readable file descriptors.
     *
     * @see GenericDocument.Builder#setPropertyBlobHandle
     *
     */
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.SCHEMA_BLOB_HANDLE)
    default @NonNull ListenableFuture<OpenBlobForReadResponse> openBlobForReadAsync(
            @NonNull Set<AppSearchBlobHandle> handles) {
        throw new UnsupportedOperationException(Features.SCHEMA_BLOB_HANDLE
                + " is not available on this AppSearch implementation.");
    }

    /**
     * Reports that a particular document has been used from a system surface.
     *
     * <p>See {@link AppSearchSession#reportUsageAsync} for a general description of document usage,
     * as well as an API that can be used by the app itself.
     *
     * <p>Usage reported via this method is accounted separately from usage reported via
     * {@link AppSearchSession#reportUsageAsync} and may be accessed using the constants
     * {@link SearchSpec#RANKING_STRATEGY_SYSTEM_USAGE_COUNT} and
     * {@link SearchSpec#RANKING_STRATEGY_SYSTEM_USAGE_LAST_USED_TIMESTAMP}.
     *
     * @return The pending result of performing this operation which resolves to {@code null} on
     *     success. The pending result will be completed with an
     *     {@link androidx.appsearch.exceptions.AppSearchException} with a code of
     *     {@link AppSearchResult#RESULT_SECURITY_ERROR} if this API is invoked by an app which
     *     is not part of the system.
     */
    @NonNull ListenableFuture<Void> reportSystemUsageAsync(
            @NonNull ReportSystemUsageRequest request);

    /** Closes the {@link GlobalSearchSession}. */
    @Override
    void close();
}
