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

import androidx.annotation.RestrictTo;

import org.chromium.support_lib_boundary.WebViewPageBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.InvocationHandler;
import java.util.Objects;

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
public final class Page {

    private final @NonNull WebViewPageBoundaryInterface mPageImpl;

    private Page(@NonNull WebViewPageBoundaryInterface pageImpl) {
        mPageImpl = pageImpl;
    }

    /**
     * Factory method that returns the Page associated with the given invocationHandler.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static @NonNull Page forInvocationHandler(@NonNull InvocationHandler invocationHandler) {
        WebViewPageBoundaryInterface boundaryInterface =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        WebViewPageBoundaryInterface.class, invocationHandler);
        assert boundaryInterface != null;
        return (Page) Objects.requireNonNull(
                boundaryInterface.getOrCreatePeer(() -> new Page(boundaryInterface)));
    }

    /**
     * Returns the URL associated with this page instance.
     */
    public @NonNull String getUrl() {
        return mPageImpl.getUrl();
    }
}
