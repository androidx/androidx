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
import android.content.res.Configuration
import android.view.ViewGroup
import androidx.annotation.RequiresFeature
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
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
internal constructor(
    private val boundaryInterface: WebContentBoundaryInterface,
    private val useMutableContextWrapper: Boolean = true,
) : AutoCloseable {

    internal fun getInvocationHandler(): InvocationHandler =
        Proxy.getInvocationHandler(boundaryInterface)

    private var isDetached: Boolean = true
    private var isDestroyed: Boolean = false
    private var currentView: WebContentView? = null
    private var currentContext: Context? = null

    /** Returns whether this [WebContent] is currently attached to a [WebContentView]. */
    @InternalWebApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun isAttached(): Boolean = !isDetached && !isDestroyed

    /**
     * Transfers this [WebContent] to a new [WebContentView], like [attach] but first removing the
     * attached view from the view hierarchy, so the new view can take over in the same frame.
     */
    @InternalWebApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @UiThread
    public fun <T : WebContentView> transfer(context: Context, factory: Function<Context, T>): T {
        checkCanAttach(context)
        val previousView = if (isAttached()) currentView else null
        if (previousView != null) {
            // We need to remove the previous views focus so that the View system
            // doesn't search for something new to focus on.
            // We also need to remove the previous view before attaching to the
            // new one to avoid Chromium complaining at us.
            previousView.clearFocus()
            (previousView.parent as? ViewGroup)?.removeView(previousView)
        }
        return internalAttach(context, factory::apply)
    }

    /**
     * Like [detach], but does nothing if this [WebContent] is no longer attached to [view], e.g.
     * after it was [transfer]red to another view.
     */
    @InternalWebApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @UiThread
    public fun detach(view: WebContentView) {
        if (currentView === view) detach()
    }

    private fun unwrapActivity(context: Context): Activity? {
        var ctx: Context? = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private fun checkCanAttach(context: Context) {
        check(!isDestroyed) { "Cannot attach to a destroyed WebContent." }
        require(unwrapActivity(context) != null) {
            "WebContent must be attached with an Activity context."
        }
    }

    /**
     * Constructs a [WebContentView] bound to this [WebContent]. This allows the underlying web
     * engine, including its state, settings, and clients (such as [android.webkit.WebViewClient]
     * and [android.webkit.WebChromeClient]), to outlive the view's lifetime. Previously bound
     * [WebContentView] instances are destroyed when this method is called.
     *
     * JavaScript interfaces added with [android.webkit.WebView.addJavascriptInterface] belong to
     * this [WebContent] rather than the view, so pages loaded or reloaded in a newly attached
     * [WebContentView] can still use them.
     *
     * The provided context _must_ be used to construct the [WebContentView], and any previously
     * attached [WebContentView] must be removed from the view hierarchy before calling this method.
     *
     * [detach] must be called when this [WebContentView] is no longer in use to prevent context
     * leaks. When the content is permanently retired, call [close].
     *
     * @param context The context to use to construct the View.
     * @param factory A function that creates a new [WebContentView].
     * @return The [WebContentView] created by `factory` and attached to this [WebContent].
     * @throws IllegalArgumentException if `context` is not an [Activity] context.
     * @throws IllegalStateException if a previously bound [WebContentView] is still in the view
     *   hierarchy, if the returned view uses a different context, or if this [WebContent] is
     *   closed.
     */
    @UiThread
    public fun <T : WebContentView> attach(context: Context, factory: Function<Context, T>): T {
        checkCanAttach(context)
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
        val detachedContext =
            currentView!!.context.applicationContext.createConfigurationContext(Configuration())
        internalAttach(detachedContext, ::DetachedWebContentView)
    }

    /**
     * Permanently closes and destroys the underlying [WebContentView] engine associated with this
     * [WebContent]. This method must be called from the main thread. Once closed, this [WebContent]
     * instance can no longer be used.
     */
    @UiThread
    override fun close() {
        if (isDestroyed) return
        try {
            boundaryInterface.destroy()
        } finally {
            isDestroyed = true
            currentView = null
            currentContext = null
        }
    }

    internal fun <T : WebContentView> internalAttach(context: Context, factory: (Context) -> T): T {
        currentView?.let { view ->
            if (view !is DetachedWebContentView) {
                check(!view.isAttachedToWindow && view.parent == null) {
                    "Previous WebContentView must be detached from the view hierarchy before attaching or detaching WebContent."
                }
            }
        }

        val nextContext = getContext(context)
        val nextView =
            try {
                boundaryInterface.executeViewFactory(nextContext, factory::invoke)
            } catch (t: Throwable) {
                cleanUpContext(nextContext)
                throw t
            }

        currentView?.transferViewState(nextView)
        val previousContext = currentContext
        currentView = nextView
        currentContext = nextContext
        isDetached = nextView is DetachedWebContentView
        // Clean up the previous context only after executeViewFactory() finishes, so Chromium's
        // detach/adopt teardown still observes the original Activity (e.g. when unregistering
        // ComponentCallbacks).
        cleanUpContext(previousContext)
        return nextView
    }

    private fun getContext(context: Context): Context {
        return if (useMutableContextWrapper && unwrapActivity(context) != null) {
            WebContentContextWrapper(context)
        } else {
            context
        }
    }

    private fun cleanUpContext(context: Context?) {
        (context as? WebContentContextWrapper)?.cleanUp()
    }

    /** Builder for [WebContent]. */
    @Suppress("EmptyBuilder")
    public class Builder {
        /**
         * Wraps attached [Activity] contexts in a [WebContentContextWrapper] whose base is reset to
         * an application-backed configuration context once the view is detached or transferred.
         * Enabled by default as defense-in-depth against Chromium or framework objects retaining a
         * detached view or context; leak tests can disable this to verify Chromium itself releases
         * the [Activity]. We will eventually remove this behaviour once we're confident that
         * Chromium has no leaks.
         */
        @VisibleForTesting internal var useMutableContextWrapper: Boolean = true

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
        public fun build(): WebContent {
            val contentBoundary =
                WebGlueCommunicator.factory
                    .buildWebContent(::transfer)
                    .asProxy<WebContentBoundaryInterface>()

            return WebContent(contentBoundary, useMutableContextWrapper)
        }
    }
}
