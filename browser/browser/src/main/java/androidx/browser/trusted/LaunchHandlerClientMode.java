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

package androidx.browser.trusted;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents launch handler client mode type value of a Launch Handler API
 * https://developer.mozilla.org/en-US/docs/Web/API/Launch_Handler_API
 */
public final class LaunchHandlerClientMode {

    private LaunchHandlerClientMode() {
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
            AUTO,
            NAVIGATE_EXISTING,
            FOCUS_EXISTING,
            NAVIGATE_NEW,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ClientMode {
    }

    /**
     * The user agent's default launch routing behavior is used.
     */
    public static final int AUTO = 0;

    /**
     * If an existing web app client is open it is brought to focus and navigated to the launch's
     * target URL. If there are no existing web app clients the navigate-new behavior is used
     * instead.
     */
    public static final int NAVIGATE_EXISTING = 1;

    /**
     * If an existing web app client is open it is brought to focus but not navigated to the
     * launch's target URL, instead the target URL is communicated via LaunchParams.
     * If there are no existing web app clients the navigate-new behavior is used instead.
     */
    public static final int FOCUS_EXISTING = 2;

    /**
     * A new web app client is created to load the launch's target URL.
     */
    public static final int NAVIGATE_NEW = 3;
}
