/*
 * Copyright 2026 The Android Open Source Project
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

import android.webkit.WebView;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * This is the encapsulation of a JavaScript execution world in which to inject JavaScript.
 *
 * <p>Worlds are tied to the WebView they are registered to. Using a world with a WebView that it
 * is not registered to will throw an error.
 * See {@link WebViewCompat#getExecutionWorld(WebView, String)} for creating an instance.
 */
public class JavaScriptExecutionWorld {
    /** Name for the default page world. */
    public static final String PAGE_WORLD_NAME = "";
    private final String mName;
    private final WebView mWebView;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public JavaScriptExecutionWorld(@NonNull String name, @NonNull WebView webView) {
        mName = name;
        mWebView = webView;
    }

    /**
     * @return the name of the execution world.
     */
    @NonNull String getName() {
        return mName;
    }

    /**
     * @throws IllegalArgumentException if the webview passed in is not the same
     *                                  as the webview this world is registered to.
     */
    void checkWebviewRegistration(WebView view) {
        if (mWebView != view) {
            throw new IllegalArgumentException("The world is not associated with the webview");
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        // Null check.
        if (o == null) {
            return false;
        }
        // Self check.
        if (this == o) {
            return true;
        }
        if (o instanceof JavaScriptExecutionWorld) {
            JavaScriptExecutionWorld that = (JavaScriptExecutionWorld) o;
            return Objects.equals(mName, that.mName)
                    && Objects.equals(mWebView, that.mWebView);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mName, mWebView);
    }
}
