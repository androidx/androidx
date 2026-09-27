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

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CompanionOperation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.creation.CreationDisplayInfo;
import androidx.compose.remote.creation.RemoteComposeWriter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

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
 *   <li>how the documents are compressed (see {@link #withCompression})
 * </ul>
 *
 * A subclass of RemoteComposeWriter can be provided by the profile, allowing additional validation
 * (e.g. validating parameters for a specific functionality) Additional features will likely be
 * represented via Profile in the future (set of valid host actions, etc.)
 */
public class Profile {
    int mApiLevel;
    int mOperationsProfiles;
    @NonNull
    RcPlatformServices mPlatform;
    @NonNull
    RemoteComposeWriterFactory mFactory;
    int mCompression = Header.COMPRESSION_NONE;

    @NonNull
    SupportedOperationsProvider mSupportedOperationsProvider = () -> {
        Operations.UniqueIntMap<CompanionOperation> operations = Operations.getOperations(
                mApiLevel, mOperationsProfiles);

        if (operations == null) {
            throw new IllegalStateException("No supported operations defined");
        }
        return operations.keySet();
    };

    /**
     * Profile constructor
     *
     * @param apiLevel          the api level used by this profile
     * @param operationProfiles the operation profiles bitmask (specifying valid set of operations)
     * @param platform          a platform services implementation
     * @param factory           a valid factory returning a RemoteComposeWriter
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Profile(
            int apiLevel,
            int operationProfiles,
            @NonNull RcPlatformServices platform,
            @NonNull RemoteComposeWriterFactory factory) {
        mApiLevel = apiLevel;
        mOperationsProfiles = operationProfiles;
        mPlatform = platform;
        mFactory = factory;
    }

    /**
     * Profile constructor
     *
     * @param apiLevel            the api level used by this profile
     * @param operationProfiles   the operation profiles bitmask (specifying valid set of
     *                            operations)
     * @param platform            a platform services implementation
     * @param supportedOperationsProvider supplier of supported operations
     * @param factory             a valid factory returning a RemoteComposeWriter
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Profile(
            int apiLevel,
            int operationProfiles,
            @NonNull RcPlatformServices platform,
            @NonNull SupportedOperationsProvider supportedOperationsProvider,
            @NonNull RemoteComposeWriterFactory factory) {
        mApiLevel = apiLevel;
        mOperationsProfiles = operationProfiles;
        mPlatform = platform;
        mFactory = factory;
        mSupportedOperationsProvider = supportedOperationsProvider;
    }

    /**
     * Returns a valid RemoteComposeWriter that can be used to create a document
     *
     * @param creationDisplayInfo original size of the document
     * @param writerCallback the callback for writer out of band data
     * @return a valid RemoteComposeWriter
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull RemoteComposeWriter create(@NonNull CreationDisplayInfo creationDisplayInfo,
            @Nullable Object writerCallback) {
        return mFactory.create(creationDisplayInfo, this, writerCallback);
    }

    /**
     * Returns a copy of this profile that compresses the documents it creates. Everything after the
     * {@link Header} is compressed, so the header can still be peeked, and players decompress the
     * documents transparently. This profile is left unchanged.
     *
     * <p>Writers built with a {@link Header#COMPRESS} tag use that value instead.
     *
     * @param compression {@link Header#COMPRESSION_DEFLATE}, or {@link Header#COMPRESSION_NONE}
     * @return a new profile, identical to this one except for its compression
     * @throws IllegalArgumentException if the compression is unknown, or this profile's API level
     *     is below 8
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Profile withCompression(int compression) {
        RemoteComposeWriter.checkCompression(mApiLevel, compression);
        Profile profile =
                new Profile(
                        mApiLevel,
                        mOperationsProfiles,
                        mPlatform,
                        mSupportedOperationsProvider,
                        mFactory);
        profile.mCompression = compression;
        return profile;
    }

    /**
     * Returns how the documents created with this profile are compressed.
     *
     * @return a {@link Header#COMPRESS} value, {@link Header#COMPRESSION_NONE} by default
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public int getCompression() {
        return mCompression;
    }

    /**
     * Returns the API level for the operations associated with this profile
     *
     * @return the current API level used
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public int getApiLevel() {
        return mApiLevel;
    }

    /**
     * Returns the bitmask of profiles bit masks that this Profile supports
     *
     * @return a bitmask of operation profiles
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public int getOperationsProfiles() {
        return mOperationsProfiles;
    }

    /**
     * Returns the Platform Services implementation associated with this Profile
     *
     * @return the platform
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull RcPlatformServices getPlatform() {
        return mPlatform;
    }

    /**
     * Returns a ProfileFactory, that is used to create a RemoteComposeWriter instance
     *
     * @return a ProfileFactory
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull RemoteComposeWriterFactory getProfileFactory() {
        return mFactory;
    }

    /**
     * Returns the set of valid operations for this Profile. Null if this profile doesn't do any
     * validation of operations.
     *
     * @return a set of operations
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull Set<Integer> getSupportedOperations() {
        return mSupportedOperationsProvider.getSupportedOperations();
    }

    /**
     * Interface for providing a set of supported operations.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface SupportedOperationsProvider {
        /**
         * Returns the set of supported operations.
         */
        @NonNull Set<Integer> getSupportedOperations();
    }
}
