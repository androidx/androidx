/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.creation.profile;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Platform;
import androidx.compose.remote.creation.RemoteComposeWriter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Represent a RemoteCompose profile
 *
 * <p>A profile can be set when creating documents and will allow to validate the creation. A
 * profile encapsulates:
 *
 * <ul>
 *   <li>api level for the operations - the operation profiles used (i.e., this plus the api level
 *       defines the set of valid operations)
 *   <li>a platform services implementation
 *   <li>a RemoteComposeWriter instance
 * </ul>
 *
 * A subclass of RemoteComposeWriter can be provided by the profile, allowing additional validation
 * (e.g. validating parameters for a specific functionality) Additional features will likely be
 * represented via Profile in the future (set of valid host actions, etc.)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Profile {
    int mApiLevel;
    int mOperationsProfiles;
    @NonNull Platform mPlatform;
    @NonNull ProfileFactory mFactory;

    @Nullable Supplier<Set<Integer>> mSupportedOperations;

    /**
     * Profile constructor
     *
     * @param apiLevel the api level used by this profile
     * @param operationProfiles the operation profiles bitmask (specifying valid set of operations)
     * @param platform a platform services implementation
     * @param factory a valid factory returning a RemoteComposeWriter
     */
    public Profile(
            int apiLevel,
            int operationProfiles,
            @NonNull Platform platform,
            @NonNull ProfileFactory factory) {
        mApiLevel = apiLevel;
        mOperationsProfiles = operationProfiles;
        mPlatform = platform;
        mFactory = factory;
    }

    /**
     * Profile constructor
     *
     * @param apiLevel the api level used by this profile
     * @param operationProfiles the operation profiles bitmask (specifying valid set of operations)
     * @param platform a platform services implementation
     * @param supportedOperations supplier of supported operations
     * @param factory a valid factory returning a RemoteComposeWriter
     */
    public Profile(
            int apiLevel,
            int operationProfiles,
            @NonNull Platform platform,
            @NonNull Supplier<Set<Integer>> supportedOperations,
            @NonNull ProfileFactory factory) {
        mApiLevel = apiLevel;
        mOperationsProfiles = operationProfiles;
        mPlatform = platform;
        mFactory = factory;
        mSupportedOperations = supportedOperations;
    }

    /**
     * Returns a valid RemoteComposeWriter that can be used to create a document
     *
     * @param width original width of the document
     * @param height original height of the document
     * @param description content description
     * @return a valid RemoteComposeWriter
     */
    public @NonNull RemoteComposeWriter create(int width, int height, @NonNull String description) {
        return mFactory.create(width, height, description, this);
    }

    /**
     * Returns the API level for the operations associated with this profile
     *
     * @return the current API level used
     */
    public int getApiLevel() {
        return mApiLevel;
    }

    /**
     * Returns the bitmask of profiles bit masks that this Profile supports
     *
     * @return a bitmask of operation profiles
     */
    public int getOperationsProfiles() {
        return mOperationsProfiles;
    }

    /**
     * Returns the Platform Services implementation associated with this Profile
     *
     * @return the platform
     */
    public @NonNull Platform getPlatform() {
        return mPlatform;
    }

    /**
     * Returns a ProfileFactory, that is used to create a RemoteComposeWriter instance
     *
     * @return a ProfileFactory
     */
    public @NonNull ProfileFactory getProfileFactory() {
        return mFactory;
    }

    /**
     * Returns the set of valid operations for this Profile. Null if this profile doesn't do any
     * validation of operations.
     *
     * @return a set of operations
     */
    @RequiresApi(24)
    public @Nullable Set<Integer> getSupportedOperations() {
        if (mSupportedOperations == null) return null;

        return mSupportedOperations.get();
    }
}
