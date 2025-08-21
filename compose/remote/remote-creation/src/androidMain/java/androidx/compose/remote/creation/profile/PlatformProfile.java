/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.platform.AndroidxPlatformServices;

import org.jspecify.annotations.NonNull;

public class PlatformProfile {
    // Platform profile
    public static final @NonNull Profile WIDGETS_V6 =
            new Profile(6, 0, new AndroidxPlatformServices(), WidgetsProfileWriterV6::new);

    // Default AndroidX profile
    public static final @NonNull Profile ANDROIDX =
            new Profile(
                    CoreDocument.DOCUMENT_API_LEVEL,
                    Operations.PROFILE_ANDROIDX,
                    new AndroidxPlatformServices(),
                    (width, height, contentDescription, profile) ->
                            new RemoteComposeWriterAndroid(
                                    width,
                                    height,
                                    contentDescription,
                                    CoreDocument.DOCUMENT_API_LEVEL,
                                    Operations.PROFILE_ANDROIDX,
                                    profile.getPlatform()));
}
