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

import androidx.annotation.AnyThread;
import androidx.annotation.RestrictTo;

import org.chromium.support_lib_boundary.WebViewPageStateBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.InvocationHandler;

/**
 * This class serves as an immutable snapshot of a {@link Page} instance at a certain point in time,
 * captured every time a callback (e.g. {@link NavigationListener#onPageLoadEvent}) is called.
 * <p>
 * Note: Do not use {@link PageState} as a key to connect callbacks, use {@link Page} instead from
 * {@link PageState#getPage()}
 */
@AnyThread
public final class PageState {

    private final @NonNull WebViewPageStateBoundaryInterface mBoundaryInterface;

    /**
     * Factory method that returns the PageState associated with the given invocationHandler.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static @NonNull PageState forInvocationHandler(
            @NonNull InvocationHandler invocationHandler) {
        WebViewPageStateBoundaryInterface boundaryInterface =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                        WebViewPageStateBoundaryInterface.class, invocationHandler);
        assert boundaryInterface != null;
        return new PageState(boundaryInterface);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public PageState(@NonNull WebViewPageStateBoundaryInterface boundaryInterface) {
        mBoundaryInterface = boundaryInterface;
    }

    /**
     * Returns the URL associated with this PageState instance.
     */
    public @NonNull String getUrl() {
        return mBoundaryInterface.getUrl();
    }

    /**
     * Returns the page linked to this PageState snapshot
     */
    public @NonNull Page getPage() {
        return Page.forInvocationHandler(mBoundaryInterface.getPage());
    }

}
