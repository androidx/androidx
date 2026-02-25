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

package androidx.webkit.internal;

import androidx.webkit.Page;
import androidx.webkit.WebNavigationClient;

import org.chromium.support_lib_boundary.WebViewPageBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.InvocationHandler;
import java.util.Objects;

/**
 * Adapter for {@link WebViewPageBoundaryInterface} objects.
 *
 * <p>These objects are isomorphic, and should be obtained through
 * {@link #forInvocationHandler(InvocationHandler)}.
 */
@WebNavigationClient.ExperimentalNavigationCallback
public class PageImpl implements Page {
    @SuppressWarnings({"UnusedVariable", "FieldCanBeLocal"})
    private final WebViewPageBoundaryInterface mPageBoundaryInterface;

    /**
     * Factory method that returns the PageImpl associated with the given invocationHandler.
     */
    public static @NonNull Page forInvocationHandler(@NonNull InvocationHandler invocationHandler) {
        WebViewPageBoundaryInterface boundaryInterface =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        WebViewPageBoundaryInterface.class, invocationHandler);
        assert boundaryInterface != null;
        return (Page) Objects.requireNonNull(
                boundaryInterface.getOrCreatePeer(() -> new PageImpl(boundaryInterface)));
    }

    private PageImpl(@NonNull WebViewPageBoundaryInterface impl) {
        mPageBoundaryInterface = impl;
    }

    @Override
    public @NonNull String getUrl() {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.PAGE_GET_URL;
        if (feature.isSupportedByWebView()) {
            return mPageBoundaryInterface.getUrl();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }
}
