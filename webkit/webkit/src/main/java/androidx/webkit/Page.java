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

package androidx.webkit;

import androidx.annotation.RequiresFeature;

import org.jspecify.annotations.NonNull;

/**
 * This class serves solely as a key for Page-associated data.
 * The instance itself functions as the key/identifier through {@link Object#equals(Object)} and
 * {@link Object#hashCode()}.
 * <p>
 * In the following circumstances, multiple navigations may result in the same Page:
 * <ul>
 *     <li>Same-document navigations when
 *     {@link WebViewFeature#NAVIGATION_LISTENER_NON_NULL_PAGE_FOR_SAME_DOCUMENT_NAVIGATIONS} is
 *     supported.</li>
 *     <li>Back or forward navigations that result in the user returning to a previously loaded page
 *     when {@link WebSettingsCompat#setBackForwardCacheEnabled} is enabled.</li>
 * </ul>
 */
@WebNavigationClient.ExperimentalNavigationCallback
public interface Page {
    /**
     * Returns the URL associated with this page instance.
     */
    @RequiresFeature(name = WebViewFeature.PAGE_GET_URL,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @NonNull
    String getUrl();
}
