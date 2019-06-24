/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.browser.trusted.splashscreens;

import androidx.browser.customtabs.CustomTabsSession;

/**
 * These constants are the categories the providers add to the intent filter of
 * CustomTabService implementation to declare the support of a particular version of splash
 * screens. The are also passed by the client as the value for the key
 * {@link SplashScreenParamKey#VERSION} when launching a Trusted Web Activity.
 */
public final class SplashScreenVersion {
    /**
     * The splash screen is transferred via {@link CustomTabsSession#receiveFile},
     * and then used by Trusted Web Activity when it is launched.
     *
     * The passed image is shown in a full-screen ImageView.
     * The following parameters are supported:
     * - {@link SplashScreenParamKey#BACKGROUND_COLOR},
     * - {@link SplashScreenParamKey#SCALE_TYPE},
     * - {@link SplashScreenParamKey#IMAGE_TRANSFORMATION_MATRIX}
     * - {@link SplashScreenParamKey#FADE_OUT_DURATION_MS}.
     */
    public static final String V1 =
            "androidx.browser.trusted.category.TrustedWebActivitySplashScreensV1";

    private SplashScreenVersion() {}
}
