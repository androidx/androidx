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

package androidx.webkit;

import androidx.annotation.AnyThread;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents modifications to the HTTP headers of a network request intercepted by a
 * {@link HeaderInterceptor}.
 */
@AnyThread
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class HeaderUpdates {
    private final @NonNull Map<@NonNull String, @Nullable String> mHeaderUpdates;

    private HeaderUpdates(@NonNull Map<@NonNull String, @Nullable String> headerUpdates) {
        Map<String, String> copy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        copy.putAll(headerUpdates);
        mHeaderUpdates = Collections.unmodifiableMap(copy);
    }

    /**
     * Returns whether this {@link HeaderUpdates} contains an update (either setting a value via
     * {@link Builder#setHeader(String, String)} or marking for removal via
     * {@link Builder#setHeaderRemoved(String)}) for the given header name.
     *
     * <p>Header names are matched case-insensitively.
     *
     * @param name The HTTP header name to look up.
     * @return {@code true} if this {@link HeaderUpdates} sets or removes {@code name}, or
     *     {@code false} if {@code name} is left unmodified.
     */
    public boolean hasHeader(@NonNull String name) {
        return mHeaderUpdates.containsKey(name);
    }

    /**
     * Returns the header value to set for the given header name, or {@code null} if the header is
     * marked for removal or is not present in this {@link HeaderUpdates}.
     *
     * <p>Header names are matched case-insensitively. Use {@link #hasHeader(String)} or
     * {@link #isHeaderRemoved(String)} to distinguish between a header marked for removal and a
     * header with no update.
     *
     * @param name The HTTP header name to look up.
     * @return The header value set for {@code name}, or {@code null} if {@code name} will be
     *     removed or is not modified by this {@link HeaderUpdates}.
     */
    public @Nullable String getHeader(@NonNull String name) {
        return mHeaderUpdates.get(name);
    }

    /**
     * Returns whether the given header name is marked for removal from the request.
     *
     * <p>Header names are matched case-insensitively.
     *
     * @param name The HTTP header name to look up.
     * @return {@code true} if {@code name} is marked for removal via
     *     {@link Builder#setHeaderRemoved(String)}, or {@code false} otherwise.
     */
    public boolean isHeaderRemoved(@NonNull String name) {
        return mHeaderUpdates.containsKey(name) && mHeaderUpdates.get(name) == null;
    }

    /**
     * Returns the header updates map for use with the support library glue layer, where non-null
     * values represent headers to set and {@code null} values represent headers to remove.
     *
     * <p>This method is for internal library use only.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @NonNull Map<@NonNull String, @Nullable String> toBoundaryMap() {
        return mHeaderUpdates;
    }

    /**
     * Builder for {@link HeaderUpdates}.
     */
    @AnyThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final class Builder {
        private final @NonNull Map<@NonNull String, @Nullable String> mHeaderUpdates =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        /**
         * Constructs a new {@link Builder} for {@link HeaderUpdates}.
         */
        public Builder() {
        }

        /**
         * Sets the given header on the request, replacing any previous value.
         *
         * <p>Header names are matched case-insensitively. If {@link #setHeader} or
         * {@link #setHeaderRemoved} is called multiple times for the same header name, only the
         * last call will take effect.
         *
         * @param name  The HTTP header name to set.
         * @param value The HTTP header value to set.
         * @return This {@link Builder} instance for method chaining.
         */
        public @NonNull Builder setHeader(@NonNull String name, @NonNull String value) {
            putHeaderUpdate(name, value);
            return this;
        }

        /**
         * Marks the given header to be removed from the request.
         *
         * <p>Header names are matched case-insensitively. If {@link #setHeader} or
         * {@link #setHeaderRemoved} is called multiple times for the same header name, only the
         * last call will take effect.
         *
         * @param name The HTTP header name to remove from the request.
         * @return This {@link Builder} instance for method chaining.
         */
        public @NonNull Builder setHeaderRemoved(@NonNull String name) {
            putHeaderUpdate(name, null);
            return this;
        }

        /**
         * Builds the {@link HeaderUpdates}.
         *
         * @return A new immutable {@link HeaderUpdates} instance.
         */
        public @NonNull HeaderUpdates build() {
            return new HeaderUpdates(mHeaderUpdates);
        }

        private void putHeaderUpdate(@NonNull String name, @Nullable String value) {
            // TreeMap#put updates the value in-place but retains the first key's casing when
            // CASE_INSENSITIVE_ORDER compares equal; remove first so the latest casing wins.
            mHeaderUpdates.remove(name);
            mHeaderUpdates.put(name, value);
        }
    }
}
