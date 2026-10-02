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

package androidx.web

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.NonNull
import androidx.annotation.RequiresFeature
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.web.WebGlueCommunicator.asProxy
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.function.BiConsumer
import java.util.function.Function
import org.chromium.support_lib_boundary.web.WebContentBoundaryInterface
import org.chromium.support_lib_boundary.web.WebContentConfig

/** Creates and configures a [WebContent] instance. */
@JvmSynthetic
@RequiresFeature(
    name = WebFeature.WEB_CONTENT,
    enforcement = "androidx.web.WebFeature#isFeatureSupported",
)
@Suppress("MissingJvmstatic")
@NonNull
public fun WebContent(block: WebContent.Builder.() -> Unit = {}): WebContent {
    return WebContent.Builder().apply(block).build()
}

/**
 * [WebContent] can [attach] and [detach] [WebContentView]s to outlive [Activity] lifetimes.
 *
 * To detach the active view so that its context can be safely garbage collected, call [detach]. To
 * permanently destroy the engine and all associated resources, call [close].
 *
 * This may only be used when [WebFeature.WEB_CONTENT] feature checks pass.
 */
public class WebContent
internal constructor(private val boundaryInterface: WebContentBoundaryInterface) : AutoCloseable {

    internal fun getInvocationHandler(): InvocationHandler =
        Proxy.getInvocationHandler(boundaryInterface)

    private var isDetached: Boolean = true
    private var isDestroyed: Boolean = false

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var currentView: WebContentView? = null
        private set

    private var currentViewListener: ((WebContentView?) -> Unit)? = null

    private val attachedView: WebContentView?
        get() = currentView?.takeUnless { isDetached }

    /** Sets a listener to be notified when the attached [WebContentView] changes. */
    @UiThread
    @InternalWebApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun setCurrentViewListener(listener: ((WebContentView?) -> Unit)?) {
        if (currentViewListener === listener) return
        val previousListener = currentViewListener
        currentViewListener = listener
        previousListener?.invoke(null)
        listener?.invoke(attachedView)
    }

    private fun unwrapActivity(context: Context): Activity? {
        var ctx: Context? = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    /**
     * Constructs a [WebContentView] bound to this [WebContent]. This allows the underlying web
     * engine, including its state, settings, and clients (such as [android.webkit.WebViewClient]
     * and [android.webkit.WebChromeClient]), to outlive the view's lifetime. Previously bound
     * [WebContentView] instances are destroyed when this method is called.
     *
     * The provided context _must_ be used to construct the [WebContentView], and any previously
     * attached [WebContentView] must be removed from the view hierarchy before calling this method.
     *
     * [detach] must be called when this [WebContentView] is no longer in use to prevent context
     * leaks. When the content is permanently retired, call [close].
     *
     * @param context The context to use to construct the View.
     * @param factory A function that creates a new [WebContentView].
     * @return A wrapped factory that automatically attaches this [WebContent] state.
     * @throws IllegalArgumentException if [context] is not an [Activity] context.
     * @throws IllegalStateException if a previously bound [WebContentView] is still in the view
     *   hierarchy, if the returned view uses a different context, or if this [WebContent] is
     *   closed.
     */
    @UiThread
    @NonNull
    public fun <T : WebContentView> attach(
        @NonNull context: Context,
        @NonNull factory: Function<Context, T>,
    ): T {
        check(!isDestroyed) { "Cannot attach to a destroyed WebContent." }
        require(unwrapActivity(context) != null) {
            "WebContent must be attached with an Activity context."
        }
        return internalAttach(context, factory::apply)
    }

    /**
     * Detaches this [WebContent] from its current [WebContentView]. Any callbacks triggered will
     * return a [DetachedWebContentView] in this state.
     *
     * The currently attached [WebContentView] must be removed from the view hierarchy before this
     * method is called.
     *
     * @throws IllegalStateException if the current [WebContentView] is still in the view hierarchy.
     */
    @UiThread
    public fun detach() {
        if (isDetached || isDestroyed) return
        internalAttach(currentView!!.context.applicationContext, ::DetachedWebContentView)
    }

    /**
     * Permanently closes and destroys the underlying [WebContentView] engine associated with this
     * [WebContent]. This method must be called from the main thread. Once closed, this [WebContent]
     * instance can no longer be used.
     */
    @UiThread
    override fun close() {
        if (isDestroyed) return
        boundaryInterface.destroy()
        isDestroyed = true
        currentView = null
        currentViewListener?.invoke(null)
        currentViewListener = null
    }

    internal fun <T : WebContentView> internalAttach(context: Context, factory: (Context) -> T): T {
        currentView?.let { view ->
            if (view !is DetachedWebContentView) {
                check(!view.isAttachedToWindow && view.parent == null) {
                    "Previous WebContentView must be detached from the view hierarchy before attaching or detaching WebContent."
                }
            }
        }

        return boundaryInterface.executeViewFactory(context, factory::invoke).also { nextView ->
            currentView?.transferViewState(nextView)
            currentView = nextView
            isDetached = nextView is DetachedWebContentView
            currentViewListener?.invoke(attachedView)
        }
    }

    /** Builder for [WebContent]. */
    @Suppress("EmptyBuilder")
    public class Builder {
        /** Creates a new [Builder] to create [WebContent]. */
        @RequiresFeature(
            name = WebFeature.WEB_CONTENT,
            enforcement = "androidx.web.WebFeature#isFeatureSupported",
        )
        public constructor() {
            WebFeature.checkSupported(WebFeature.WEB_CONTENT)
        }

        private fun transfer(chromiumConfig: BiConsumer<@WebContentConfig Int, Any>) {
            // Transfer config fields to Chromium.
        }

        /** Builds a [WebContent] instance. */
        @NonNull
        public fun build(): WebContent {
            val contentBoundary =
                WebGlueCommunicator.factory
                    .buildWebContent(::transfer)
                    .asProxy<WebContentBoundaryInterface>()

            return WebContent(contentBoundary)
        }
    }
}
