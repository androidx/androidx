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

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.Platform;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.platform.AndroidxPlatformServices;

import org.jspecify.annotations.NonNull;

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
public class Profile {
    int mApiLevel;
    int mOperationsProfiles;
    Platform mPlatform;
    ProfileFactory mFactory;

    // Platform profile
    public static Profile WIDGETS_V6 =
            new Profile(6, 0, new AndroidxPlatformServices(), WidgetsProfileWriterV6::new);

    // Default AndroidX profile
    public static Profile ANDROIDX =
            new Profile(
                    CoreDocument.DOCUMENT_API_LEVEL,
                    Operations.PROFILE_ANDROIDX,
                    new AndroidxPlatformServices(),
                    RemoteComposeWriter::new);

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
     * Returns a valid RemoteComposeWriter that can be used to create a document
     *
     * @param width original width of the document
     * @param height original height of the document
     * @param description content description
     * @return a valid RemoteComposeWriter
     */
    public RemoteComposeWriter create(int width, int height, @NonNull String description) {
        return mFactory.create(
                width, height, description, mApiLevel, mOperationsProfiles, mPlatform);
    }
}
