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

package androidx.webkit;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;

/**
 * AndroidX side JsReplyProxy class. An instance will be given by {@link
 * androidx.webkit.WebViewCompat.WebMessageListener#onPostMessage()}. See also {@link
 * androidx.webkit.WebViewCompat#addWebMessageListener()}.
 *
 * TODO(ctzsm): unhide
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class JsReplyProxy {
    /**
     * Post a String message to the injected JavaScript object which send this {@link
     * JsReplyProxy}.
     *
     * @param message Message send from app to the corresponding JavaScript object.
     */
    @RequiresFeature(name = WebViewFeature.WEB_MESSAGE_LISTENER,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public abstract void postMessage(@NonNull String message);

    /**
     * This class cannot be created by applications.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public JsReplyProxy() {}
}
