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
import androidx.annotation.RestrictTo;

/**
 * TODO(ctzsm): Complete Javadoc
 * @see WebViewCompat#addDocumentStartJavascript(android.webkit.WebView, String, List)
 *
 * TODO(ctzsm): unhide when ready.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ScriptReferenceCompat {
    /**
     * Removes the corresponding script, it will take effect from next page load.
     */
    @RequiresFeature(name = WebViewFeature.DOCUMENT_START_SCRIPT,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public abstract void remove();

    /**
     * This class cannot be created by applications.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public ScriptReferenceCompat() {}
}
