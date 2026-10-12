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

package androidx.webkit.internal;

import androidx.webkit.Page;
import androidx.webkit.PageState;
import androidx.webkit.WebViewFeature;

import org.chromium.support_lib_boundary.WebViewPageStateBoundaryInterface;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.InvocationHandler;

/**
 * CopiedPageState acts as a fake WebViewPageStateBoundaryInterface to be used in {@link PageState},
 * it copies values from the Page object passed by the old NavigationListener
 * callbacks when {@link WebViewFeature#NAVIGATION_LISTENER_NAVIGATION_STATE} is not
 * supported. This means it's not threadsafe like a real {@link PageState}, it exists for easy
 * backwards compatibility.
 */
public class CopiedPageState implements WebViewPageStateBoundaryInterface {

    private final /*Page*/ InvocationHandler mPage;
    private final String mUrl;

    /*package*/ CopiedPageState(@NonNull /*Page*/ InvocationHandler invocationHandler) {
        Page page = Page.forInvocationHandler(invocationHandler);
        mPage = invocationHandler;
        mUrl = page.getUrl();
    }

    @Override
    public @NonNull String getUrl() {
        return mUrl;
    }

    @Override
    public @NonNull /*Page*/ InvocationHandler getPage() {
        return mPage;
    }
}
