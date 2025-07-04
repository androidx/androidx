/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.graphics

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayerV23
import androidx.compose.ui.graphics.layer.GraphicsLayerV29
import androidx.compose.ui.graphics.layer.GraphicsViewLayer
import androidx.compose.ui.graphics.layer.view.DrawChildContainer
import androidx.compose.ui.graphics.layer.view.ViewLayerContainer
import androidx.compose.ui.graphics.shadow.ShadowContext

/**
 * Create a new [GraphicsContext] with the provided [ViewGroup] to contain [View] based layers.
 *
 * @param layerContainer [ViewGroup] used to contain [View] based layers that are created by the
 *   returned [GraphicsContext]
 */
fun GraphicsContext(layerContainer: ViewGroup): GraphicsContext =
    AndroidGraphicsContext(layerContainer)

private class AndroidGraphicsContext(private val ownerView: ViewGroup) : GraphicsContext {

    private val lock = Any()
    private var viewLayerContainer: DrawChildContainer? = null
    private var componentCallbackRegistered = false
    private var shadowCache: ShadowContext? = null

    private val componentCallback: ComponentCallbacks2

    init {
        componentCallback =
            object : ComponentCallbacks2 {
                override fun onConfigurationChanged(newConfig: Configuration) {
                    // NO-OP
                }

                @Suppress("OVERRIDE_DEPRECATION") // b/407491706
                override fun onLowMemory() {
                    // NO-OP
                }

                override fun onTrimMemory(level: Int) {
                    // See CacheManager.cpp. HWUI releases graphics resources whenever the trim
                    // memory callback exceed the level of TRIM_MEMORY_BACKGROUND so do the same
                    // here to release shadow dependencies
                    if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
                        clearShadowCache()
                    }
                }
            }
        if (ownerView.isAttachedToWindow) {
            registerComponentCallback(ownerView.context)
        }
        ownerView.addOnAttachStateChangeListener(
            object : OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    // If the View is attached to the window again, re-add the component
                    // callbacks
                    registerComponentCallback(v.context)
                }

                override fun onViewDetachedFromWindow(v: View) {
                    // When the View is detached from the window, remove the component callbacks
                    // used to listen to trim memory signals
                    unregisterComponentCallback(v.context)
                    clearShadowCache()
                }
            }
        )
    }

    private fun clearShadowCache() {
        shadowCache?.clearCache()
        shadowCache = null
    }

    private fun registerComponentCallback(context: Context) {
        if (!componentCallbackRegistered) {
            context.applicationContext.registerComponentCallbacks(componentCallback)
            componentCallbackRegistered = true
        }
    }

    private fun unregisterComponentCallback(context: Context) {
        if (componentCallbackRegistered) {
            context.applicationContext.unregisterComponentCallbacks(componentCallback)
            componentCallbackRegistered = false
        }
    }

    override fun createGraphicsLayer(): GraphicsLayer {
        synchronized(lock) {
            val ownerId = getUniqueDrawingId(ownerView)
            val layerImpl =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    GraphicsLayerV29(ownerId)
                } else if (
                    isRenderNodeCompatible && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ) {
                    try {
                        GraphicsLayerV23(ownerView, ownerId)
                    } catch (_: Throwable) {
                        // If we ever failed to create an instance of the RenderNode stub
                        // based
                        // GraphicsLayer, always fallback to creation of View based layers
                        // as it is
                        // unlikely that subsequent attempts to create a GraphicsLayer with
                        // RenderNode
                        // stubs would be successful.
                        isRenderNodeCompatible = false
                        GraphicsViewLayer(obtainViewLayerContainer(ownerView), ownerId)
                    }
                } else {
                    GraphicsViewLayer(obtainViewLayerContainer(ownerView), ownerId)
                }
            val layer = GraphicsLayer(layerImpl)
            return layer
        }
    }

    override val shadowContext: ShadowContext
        get() = shadowCache ?: ShadowContext().also { shadowCache = it }

    override fun releaseGraphicsLayer(layer: GraphicsLayer) {
        synchronized(lock) { layer.release() }
    }

    private fun obtainViewLayerContainer(ownerView: ViewGroup): DrawChildContainer {
        var container = viewLayerContainer
        if (container == null) {
            val context = ownerView.context

            container = ViewLayerContainer(context)
            ownerView.addView(container)
            viewLayerContainer = container
        }
        return container
    }

    private fun getUniqueDrawingId(view: View): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            UniqueDrawingIdApi29.getUniqueDrawingId(view)
        } else {
            -1
        }

    internal companion object {
        var isRenderNodeCompatible: Boolean = true
    }

    @RequiresApi(29)
    private object UniqueDrawingIdApi29 {
        @JvmStatic fun getUniqueDrawingId(view: View) = view.uniqueDrawingId
    }
}
