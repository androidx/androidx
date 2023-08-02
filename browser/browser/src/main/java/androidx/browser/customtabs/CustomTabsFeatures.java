/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.browser.customtabs;

import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Utilities for Custom Tabs features.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CustomTabsFeatures {
    private CustomTabsFeatures() {}

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @StringDef({ENGAGEMENT_SIGNALS})
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    public @interface CustomTabsFeature {}

    /**
     * This feature covers {@link EngagementSignalsCallback},
     * {@link CustomTabsSession#isEngagementSignalsApiAvailable},
     * {@link CustomTabsSession#setEngagementSignalsCallback} and
     */
    public static final String ENGAGEMENT_SIGNALS = "ENGAGEMENT_SIGNALS";
}
