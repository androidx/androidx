/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.WebViewRendererImpl;

import org.chromium.support_lib_boundary.WebViewRendererClientBoundaryInterface;
import org.chromium.support_lib_boundary.util.Features;

import java.lang.reflect.InvocationHandler;

/**
 * Used to receive callbacks on {@link WebView} renderer events.
 *
 * WebViewRendererClient instances may be set or retrieved via {@link
 * WebViewCompat#setWebViewRendererClient(WebView,WebViewRendererClient)} and {@link
 * WebViewCompat#getWebViewRendererClient(WebView)}.
 *
 * Instances may be attached to multiple WebViews, and thus a single renderer event may cause
 * a callback to be called multiple times with different WebView parameters.
 */
public abstract class WebViewRendererClient implements WebViewRendererClientBoundaryInterface {
    private static final String[] sSupportedFeatures = new String[] {
            // TODO(tobiasjs) remove dev suffix when ready.
            Features.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE + Features.DEV_SUFFIX,
    };

    /**
     * Returns the list of features this client supports. This feature list should always be a
     * subset of the Features declared in WebViewFeature.
     *
     * @hide
     */
    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public final String[] getSupportedFeatures() {
        return sSupportedFeatures;
    }

    /**
     * Called when the renderer currently associated with {@code view} becomes unresponsive as a
     * result of a long running blocking task such as the execution of JavaScript.
     *
     * <p>If a WebView fails to process an input event, or successfully navigate to a new URL within
     * a reasonable time frame, the renderer is considered to be unresponsive, and this callback
     * will be called.
     *
     * <p>This callback will continue to be called at regular intervals as long as the renderer
     * remains unresponsive. If the renderer becomes responsive again, {@link
     * WebViewRendererClient#onRendererResponsive} will be called once, and this method will not
     * subsequently be called unless another period of unresponsiveness is detected.
     *
     * <p>No action is taken by WebView as a result of this method call. Applications may
     * choose to terminate the associated renderer via the object that is passed to this callback,
     * if in multiprocess mode, however this must be accompanied by correctly handling
     * {@link android.webkit.WebViewClient#onRenderProcessGone} for this WebView, and all other
     * WebViews associated with the same renderer. Failure to do so will result in application
     * termination.
     *
     * @param view The {@link android.webkit.WebView} for which unresponsiveness was detected.
     * @param renderer The {@link WebViewRenderer} that has become unresponsive, or {@code null} if
     * WebView is running in single process mode.
     */
    public abstract void onRendererUnresponsive(
            @NonNull WebView view, @Nullable WebViewRenderer renderer);

    /**
     * Called once when an unresponsive renderer currently associated with {@code view} becomes
     * responsive.
     *
     * <p>After a WebView renderer becomes unresponsive, which is notified to the application by
     * {@link WebViewRendererClient#onRendererUnresponsive}, it is possible for the blocking
     * renderer task to complete, returning the renderer to a responsive state. In that case,
     * this method is called once to indicate responsiveness.
     *
     * <p>No action is taken by WebView as a result of this method call.
     *
     * @param view The {@link android.webkit.WebView} for which responsiveness was detected.
     *
     * @param renderer The {@link WebViewRenderer} that has become responsive, or {@code null} if
     * WebView is running in single process mode.
     */
    public abstract void onRendererResponsive(
            @NonNull WebView view, @Nullable WebViewRenderer renderer);

    /**
     * Invoked by chromium with arguments that need to be wrapped by support library adapter
     * objects. Applications are not meant to override this. instead they should override the
     * non-final method that this method calls with adapted arguments.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public final void onRendererUnresponsive(
            @NonNull WebView view, @NonNull /* WebViewRenderer */ InvocationHandler renderer) {
        onRendererUnresponsive(view, WebViewRendererImpl.forInvocationHandler(renderer));
    }

    /**
     * Invoked by chromium with arguments that need to be wrapped by support library adapter
     * objects. Applications are not meant to override this. instead they should override the
     * non-final method that this method calls with adapted arguments.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public final void onRendererResponsive(
            @NonNull WebView view, @NonNull /* WebViewRenderer */ InvocationHandler renderer) {
        onRendererResponsive(view, WebViewRendererImpl.forInvocationHandler(renderer));
    }
}
