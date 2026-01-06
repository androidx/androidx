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

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RcProfiles;
import androidx.compose.remote.creation.ExperimentalRemoteCreationApi;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

public class RcPlatformProfiles {
    /**
     * Profile for Glance Widgets for Platform 16.
     * <p>
     * This will be moved to the glance module when creation APIs are public, before
     * stable APIs.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @ExperimentalRemoteCreationApi
    public static final @NonNull Profile WIDGETS_V6 =
            new Profile(6, 0, new AndroidxRcPlatformServices(),
                    (creationDisplayInfo, profile, callback) ->
                            new WidgetsProfileWriterV6(creationDisplayInfo, null, profile));

    /**
     * A profile for creating Remote Compose UIs for use with the embedded AndroidX Player.
     *
     * <p>It uses the {@link RemoteComposeWriterAndroid} to serialize the UI tree.
     */
    public static final @NonNull Profile ANDROIDX = new Profile(CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX, new AndroidxRcPlatformServices(),
            (creationDisplayInfo, profile, callback) ->
                    new RemoteComposeWriterAndroid(
                            creationDisplayInfo, null, profile, callback));

    /**
     * Profile for Wear OS widgets.
     * <p>
     * This will be moved to the glance:wear:wear module when creation APIs are public, before
     * stable APIs.
     */
    @ExperimentalRemoteCreationApi
    public static final @NonNull Profile WEAR_WIDGETS = new Profile(CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_WEAR_WIDGETS, new AndroidxRcPlatformServices(),
            (creationDisplayInfo, profile, callback) ->
                    new RemoteComposeWriterAndroid(
                            creationDisplayInfo, null, profile, callback));
}
