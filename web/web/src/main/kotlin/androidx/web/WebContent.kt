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
import androidx.annotation.UiThread
import java.util.function.BiConsumer
import java.util.function.Function
import org.chromium.support_lib_boundary.WebContentBoundaryInterface
import org.chromium.support_lib_boundary.WebContentConfig
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil

/** Creates and configures a [WebContent] instance. */
@JvmSynthetic
@RequiresFeature(
    name = WebFeature.WEB_CONTENT,
    enforcement = "androidx.web.WebFeature#isFeatureSupported",
)
@Suppress("MissingJvmstatic")
@NonNull
public fun WebContent(
    @NonNull context: Context,
    block: WebContent.Builder.() -> Unit = {},
): WebContent {
    return WebContent.Builder(context).apply(block).build()
}

/**
 * [WebContent] can [attach] and [detach] [WebContentView]s to outlive [Activity] lifetimes.
 *
 * To detach the active view so that its context can be safely garbage collected, call [detach]. To
 * permanently destroy the engine and all associated resources, call [close].
 *
 * This may only be used when [WebFeature.WEB_CONTENT] feature checks pass.
 */
public interface WebContent : AutoCloseable {
    /** Builder for [WebContent]. */
    @Suppress("EmptyBuilder")
    public class Builder {
        private val context: Context

        /**
         * Creates a new [Builder] to create [WebContent].
         *
         * @param context The context to use.
         */
        @RequiresFeature(
            name = WebFeature.WEB_CONTENT,
            enforcement = "androidx.web.WebFeature#isFeatureSupported",
        )
        public constructor(@NonNull context: Context) {
            if (!WebFeature.isFeatureSupported(WebFeature.WEB_CONTENT)) {
                throw WebFeature.getUnsupportedOperationException()
            }
            this.context = context.applicationContext
        }

        private fun transfer(chromiumConfig: BiConsumer<@WebContentConfig Int, Any>) {
            // Transfer config fields to Chromium.
        }

        /** Builds a [WebContent] instance. */
        @NonNull
        public fun build(): WebContent {
            val factory = WebGlueCommunicator.factory
            val contentHandler = factory.buildWebContent(::transfer)
            val contentBoundary =
                BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                    WebContentBoundaryInterface::class.java,
                    contentHandler,
                )!!

            return WebContentImpl(contentBoundary, context)
        }
    }

    /**
     * Construct a [WebContentView] that will be bound to this [WebContent]. This allows the
     * underlying [WebContentView] engine to outlive its lifetime. Previously bound [WebContentView]
     * instances are destroyed when this method is called.
     *
     * The provided context _must_ be used to construct the WebContentView.
     *
     * [detach] must be called when this [WebContentView] is no longer in use to prevent context
     * leaks. When the content is permanently retired, call [close].
     *
     * @param context The context to use to construct the View.
     * @param factory A function that creates a new [WebContentView].
     * @return A wrapped factory that automatically attaches this [WebContent] state.
     * @throws IllegalArgumentException if [context] is not an [Activity] context.
     */
    @UiThread
    @NonNull
    public fun <T : WebContentView> attach(
        @NonNull context: Context,
        @NonNull factory: Function<Context, T>,
    ): T

    /**
     * Detaches this [WebContent] from its current [WebContentView]. Any callbacks triggered will
     * return a [DetachedWebContentView] in this state.
     */
    @UiThread public fun detach()

    /**
     * Permanently closes and destroys the underlying [WebContentView] engine associated with this
     * [WebContent]. This method must be called from the main thread. Once closed, this [WebContent]
     * instance can no longer be used.
     */
    @UiThread override fun close()
}

internal class WebContentImpl(
    private val boundaryInterface: WebContentBoundaryInterface,
    private val applicationContext: Context,
) : WebContent {

    private var isDetached: Boolean = true
    private var isDestroyed: Boolean = false
    private var currentView: WebContentView? = null
    private var savedScrollX: Int = 0
    private var savedScrollY: Int = 0

    private fun unwrapActivity(context: Context): Activity? {
        var ctx: Context? = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    override fun <T : WebContentView> attach(context: Context, factory: Function<Context, T>): T {
        check(!isDestroyed) { "Cannot attach to a destroyed WebContent." }
        require(unwrapActivity(context) != null) {
            "WebContent must be attached with an Activity context."
        }
        return internalAttach(context, factory::apply)
    }

    override fun detach() {
        if (isDetached || isDestroyed) return
        internalAttach(applicationContext, ::DetachedWebContentView)
    }

    override fun close() {
        if (isDestroyed) return
        boundaryInterface.destroy()
        isDestroyed = true
        currentView = null
    }

    internal fun <T : WebContentView> internalAttach(context: Context, factory: (Context) -> T): T {
        currentView?.let { view ->
            if (view !is DetachedWebContentView) {
                check(!view.isAttachedToWindow && view.parent == null) {
                    "Previous WebContentView must be detached from the view hierarchy before attaching or detaching WebContent."
                }
                savedScrollX = view.scrollX
                savedScrollY = view.scrollY
            }
        }

        val view = boundaryInterface.executeViewFactory(context, factory::invoke)

        isDetached = view is DetachedWebContentView
        currentView = view

        // The scroll position is saved in WebView in framework source
        // so the Chromium layer cannot persist these values.
        if (!isDetached && (savedScrollX != 0 || savedScrollY != 0)) {
            view.scrollTo(savedScrollX, savedScrollY)
        }

        return view
    }
}
