/*
 * Copyright 2020 The Android Open Source Project
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

/**
 * This interface represents the return result from {@link WebViewCompat#addDocumentStartJavaScript(
 * android.webkit.WebView, String, java.util.Set)}. Call {@link ScriptHandler#remove()} when the
 * corresponding JavaScript script should be removed.
 *
 * @see WebViewCompat#addDocumentStartJavaScript(android.webkit.WebView, String, java.util.Set)
 */
public interface ScriptHandler {
    /**
     * Removes the corresponding script, it will take effect from next page load.
     */
    @RequiresFeature(name = WebViewFeature.DOCUMENT_START_SCRIPT,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    void remove();
}
