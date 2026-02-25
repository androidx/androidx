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

package androidx.mediarouter.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.RestrictTo;

@RestrictTo(LIBRARY)
public class MediaRouterFlags {
    static final String NAMESPACE = "com.android.media.flags";
    static final String ENABLE_ROUTE_VISIBILITY_CONTROL_API = "enable_route_visibility_control_api";
    static final String ENABLE_SUGGESTED_DEVICE_API = "enable_suggested_device_api";

    private MediaRouterFlags() {}
}
