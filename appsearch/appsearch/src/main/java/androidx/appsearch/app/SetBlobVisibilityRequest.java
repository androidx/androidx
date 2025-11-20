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

package androidx.appsearch.app;

import android.annotation.SuppressLint;

import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Request to configure the visibility settings of blobs in AppSearch.
 *
 * <p>Used with {@link AppSearchSession#setBlobVisibilityAsync} to specify visibility and display
 * properties for blob namespaces. You can control which blob namespaces are displayed on system UI
 * surfaces and which are accessible based on specific visibility configurations.
 *
 * @see AppSearchSession#openBlobForWriteAsync
 * @see GlobalSearchSession#openBlobForReadAsync
 */
@FlaggedApi(Flags.FLAG_ENABLE_BLOB_STORE)
public class SetBlobVisibilityRequest {

    private final Set<String> mNamespacesNotDisplayedBySystem;
    private final Map<String, Set<SchemaVisibilityConfig>> mNamespacesVisibleToConfigs;

    SetBlobVisibilityRequest(@NonNull Set<String> namespacesNotDisplayedBySystem,
            @NonNull Map<String, Set<SchemaVisibilityConfig>> namespacesVisibleToConfigs) {
        mNamespacesNotDisplayedBySystem =
                Preconditions.checkNotNull(namespacesNotDisplayedBySystem);
        mNamespacesVisibleToConfigs = Preconditions.checkNotNull(namespacesVisibleToConfigs);
    }

    /**
     * Returns all the blob namespaces that are opted out of being displayed and visible on any
     * system UI surface.
     */
    public @NonNull Set<String> getNamespacesNotDisplayedBySystem() {
        return Collections.unmodifiableSet(mNamespacesNotDisplayedBySystem);
    }

    /**
     * Returns a mapping of blob namespaces to the set of {@link SchemaVisibilityConfig}s that
     * have access to that namespace.
     *
     * <p> All conditions in a single {@link SchemaVisibilityConfig} are "AND" relationship. A
     * caller must match all conditions to have the access. All {@link SchemaVisibilityConfig}s
     * in the set of a blob namespace are "OR" relationship. A caller could have access if they
     * matches any {@link SchemaVisibilityConfig} in the set.
     *
     * <p> This method provides the set of {@link SchemaVisibilityConfig} for all blob namespaces.
     *
     * @see Builder#addNamespaceVisibleToConfig
     */
    public @NonNull Map<String, Set<SchemaVisibilityConfig>> getNamespacesVisibleToConfigs() {
        return Collections.unmodifiableMap(mNamespacesVisibleToConfigs);
    }

    /** Builder for {@link SetBlobVisibilityRequest} objects. */
    public static final class Builder {

        private final ArrayMap<String, Set<SchemaVisibilityConfig>> mNamespacesVisibleToConfigs =
                new ArrayMap<>();
        private final ArraySet<String> mNamespacesNotDisplayedBySystem = new ArraySet<>();

        /**
         * Sets whether or not blobs in the specified {@code namespace} will be displayed
         * on any system UI surface.
         *
         * <p>This setting applies to the provided {@code namespace} only, all other
         * {@code namespace}s that are not included here will be reverted to the default displayed
         * setting.
         *
         * <p>If this method is not called, the default behavior allows blobs to be displayed
         * on system UI surfaces.
         *
         * @param namespace The name of the namespace to configure visibility for.
         * @param displayed If {@code false}, blobs in this namespace will not appear on system
         *                  UI surfaces.
         */
        // Merged list available from getBlobNamespacesNotDisplayedBySystem
        @CanIgnoreReturnValue
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder setNamespaceDisplayedBySystem(
                @NonNull String namespace, boolean displayed) {
            Preconditions.checkNotNull(namespace);
            if (displayed) {
                mNamespacesNotDisplayedBySystem.remove(namespace);
            } else {
                mNamespacesNotDisplayedBySystem.add(namespace);
            }
            return this;
        }

        /**
         * Specifies that blobs within the given {@code namespace} can be accessed by the caller
         * if they meet the requirements defined in {@link SchemaVisibilityConfig}.
         *
         * <p>The requirements in each {@link SchemaVisibilityConfig} have an "AND" relationship,
         * meaning that all conditions within a configuration must be met for access. For instance,
         * the caller may need specific permissions and belong to a specific package.
         *
         * <p>Repeated calls to this method can add multiple {@link SchemaVisibilityConfig}s to a
         * namespace. The caller will have access if they match any of the configurations added,
         * so the configurations form an "OR" relationship.
         *
         * @param namespace The blob namespace to set visibility for.
         * @param visibilityConfig The config hold specifying visibility settings.
         */
        // Merged list available from getNamespacesVisibleToConfigs
        @CanIgnoreReturnValue
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder addNamespaceVisibleToConfig(@NonNull String namespace,
                @NonNull SchemaVisibilityConfig visibilityConfig) {
            Preconditions.checkNotNull(namespace);
            Preconditions.checkNotNull(visibilityConfig);
            Set<SchemaVisibilityConfig> visibleToConfigs =
                    mNamespacesVisibleToConfigs.get(namespace);
            if (visibleToConfigs == null) {
                visibleToConfigs = new ArraySet<>();
                mNamespacesVisibleToConfigs.put(namespace, visibleToConfigs);
            }
            visibleToConfigs.add(visibilityConfig);
            return this;
        }

        /**
         * Clears all visibility configurations for the specified blob {@code namespace}.
         *
         * <p>After calling this method, the specified namespace will have no visibility
         * configurations, meaning it will only be accessible by default rules.
         *
         * @param namespace The blob namespace for which visibility config should be cleared.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder clearNamespaceVisibleToConfigs(@NonNull String namespace) {
            Preconditions.checkNotNull(namespace);
            mNamespacesVisibleToConfigs.remove(namespace);
            return this;
        }


        /**  Builds a new {@link SetBlobVisibilityRequest} object.         */
        public @NonNull SetBlobVisibilityRequest build() {
            return new SetBlobVisibilityRequest(
                    new ArraySet<>(mNamespacesNotDisplayedBySystem),
                    new ArrayMap<>(mNamespacesVisibleToConfigs));
        }
    }
}
