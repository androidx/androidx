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

import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;

import org.jspecify.annotations.NonNull;

/**
 * A handle for a {@link HeaderInterceptor} registration returned by
 * {@link Profile#addHeaderInterceptor(RequestMatcher, HeaderInterceptor)} or
 * {@link WebViewCompat#addHeaderInterceptor(android.webkit.WebView, RequestMatcher, HeaderInterceptor)}
 * that can be used to unregister the {@link HeaderInterceptor}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class HeaderInterceptorHandle {
    private final @NonNull Runnable mUnregisterRunnable;

    /**
     * Wraps a {@link Runnable} from the support library glue layer that unregisters the associated
     * {@link HeaderInterceptor}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public HeaderInterceptorHandle(@NonNull Runnable unregisterRunnable) {
        mUnregisterRunnable = unregisterRunnable;
    }

    /**
     * Unregisters the {@link HeaderInterceptor} associated with this handle.
     *
     * <p>See {@link HeaderInterceptor} for details on when registration and unregistration changes
     * take effect. Calling this method more than once has no effect.
     */
    @UiThread
    public void unregister() {
        mUnregisterRunnable.run();
    }
}
