/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.webkit.internal;

import org.chromium.support_lib_boundary.SupportLibraryInfoBoundaryInterface;
import org.chromium.support_lib_boundary.util.Features;

/**
 * Contains information about the Android Support Library part of the WebView Support Library - this
 * information is passed to the WebView APK code with the first WebView Support Library call.
 */
public class SupportLibraryInfo implements SupportLibraryInfoBoundaryInterface {
    // Features supported by the support library itself (regardless of what the WebView APK
    // supports).
    private static final String[] SUPPORTED_FEATURES =
            new String[] {
                    Features.VISUAL_STATE_CALLBACK
            };

    @Override
    public String[] getSupportedFeatures() {
        return SUPPORTED_FEATURES;
    }
}
