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
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.observer.ObserverCallback;
import androidx.appsearch.observer.ObserverSpec;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;

import java.io.Closeable;
import java.util.Set;
import java.util.concurrent.Executor;

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
    @FlaggedApi(Flags.FLAG_ENABLE_BLOB_STORE)
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

    /**
     * Adds an {@link ObserverCallback} to monitor changes within the databases owned by
     * {@code targetPackageName} if they match the given
     * {@link androidx.appsearch.observer.ObserverSpec}.
     *
     * <p>The observer callback is only triggered for data that changes after it is registered. No
     * notification about existing data is sent as a result of registering an observer. To find out
     * about existing data, you must use the {@link GlobalSearchSession#search} API.
     *
     * <p>If the data owned by {@code targetPackageName} is not visible to you, the registration
     * call will succeed but no notifications will be dispatched. Notifications could start flowing
     * later if {@code targetPackageName} changes its schema visibility settings.
     *
     * <p>If no package matching {@code targetPackageName} exists on the system, the registration
     * call will succeed but no notifications will be dispatched. Notifications could start flowing
     * later if {@code targetPackageName} is installed and starts indexing data.
     *
     * <p>This feature may not be available in all implementations. Check
     * {@link Features#GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK} before calling this method.
     *
     * @param targetPackageName Package whose changes to monitor
     * @param spec            Specification of what types of changes to listen for
     * @param executor        Executor on which to call the {@code observer} callback methods.
     * @param observer        Callback to trigger when a schema or document changes
     * @throws AppSearchException            if an error occurs trying to register the observer
     * @throws UnsupportedOperationException if this feature is not available on this
     *                                       AppSearch implementation.
     */
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK)
    void registerObserverCallback(
            @NonNull String targetPackageName,
            @NonNull ObserverSpec spec,
            @NonNull Executor executor,
            @NonNull ObserverCallback observer) throws AppSearchException;

    /**
     * Removes previously registered {@link ObserverCallback} instances from the system.
     *
     * <p>All instances of {@link ObserverCallback} which are registered to observe
     * {@code targetPackageName} and compare equal to the provided callback using the provided
     * argument's {@link ObserverCallback#equals} will be removed.
     *
     * <p>If no matching observers have been registered, this method has no effect. If multiple
     * matching observers have been registered, all will be removed.
     *
     * <p>This feature may not be available in all implementations. Check
     * {@link Features#GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK} before calling this method.
     *
     * @param targetPackageName Package which the observers to be removed are listening to.
     * @param observer          Callback to unregister.
     * @throws AppSearchException            if an error occurs trying to remove the observer, such
     *                                       as a failure to communicate with the system service
     *                                       in the platform backend. Note that no
     *                                       error will be thrown if the provided observer
     *                                       doesn't match any registered observer.
     * @throws UnsupportedOperationException if this feature is not available on this
     *                                       AppSearch implementation.
     */
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK)
    void unregisterObserverCallback(
            @NonNull String targetPackageName, @NonNull ObserverCallback observer)
            throws AppSearchException;

    /** Closes the {@link GlobalSearchSession}. */
    @Override
    void close();
}
