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
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayerV23
import androidx.compose.ui.graphics.layer.GraphicsLayerV29
import androidx.compose.ui.graphics.layer.GraphicsViewLayer
import androidx.compose.ui.graphics.layer.LayerManager
import androidx.compose.ui.graphics.layer.view.DrawChildContainer
import androidx.compose.ui.graphics.layer.view.ViewLayerContainer
import java.util.Locale

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
    private val layerManager: LayerManager?
    private var viewLayerContainer: DrawChildContainer? = null
    private var componentCallbackRegistered = false
    private var predrawListenerRegistered = false

    private val componentCallback: ComponentCallbacks2?

    init {
        // Register the component callbacks when the GraphicsContext is created
        if (enableLayerPersistence) {
            layerManager = LayerManager(CanvasHolder())
            componentCallback =
                object : ComponentCallbacks2 {
                    override fun onConfigurationChanged(newConfig: Configuration) {
                        // NO-OP
                    }

                    @Suppress("OVERRIDE_DEPRECATION")
                    override fun onLowMemory() {
                        // NO-OP
                    }

                    override fun onTrimMemory(level: Int) {
                        // See CacheManager.cpp. HWUI releases graphics resources whenever the trim
                        // memory
                        // callback exceed the level of TRIM_MEMORY_BACKGROUND so do the same here
                        // to
                        // release and recreate the internal ImageReader used to increment the ref
                        // count
                        // of internal RenderNodes
                        // Some devices skip straight to TRIM_COMPLETE so ensure we persist layers
                        // if
                        // we receive any trim memory callback that exceeds TRIM_MEMORY_BACKGROUND
                        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
                            // HardwareRenderer instances would be discarded by HWUI so we need to
                            // discard
                            // the existing underlying ImageReader instance and do a placeholder
                            // render
                            // to increment the refcount of any outstanding layers again the next
                            // time the
                            // content is drawn
                            if (!predrawListenerRegistered) {
                                layerManager.destroy()
                                ownerView.viewTreeObserver.addOnPreDrawListener(
                                    object : ViewTreeObserver.OnPreDrawListener {
                                        override fun onPreDraw(): Boolean {
                                            layerManager.updateLayerPersistence()
                                            ownerView.viewTreeObserver.removeOnPreDrawListener(this)
                                            predrawListenerRegistered = false
                                            return true
                                        }
                                    }
                                )
                                predrawListenerRegistered = true
                            }
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
                        layerManager.destroy()
                    }
                }
            )
        } else {
            layerManager = null
            componentCallback = null
        }
    }

    fun isLayerManagerInitialized(): Boolean = layerManager?.hasImageReader() ?: false

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
            val layer = GraphicsLayer(layerImpl, layerManager)
            if (enableLayerPersistence) {
                layerManager?.persist(layer)
            }
            return layer
        }
    }

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
        var isRenderNodeCompatible: Boolean = supportsV23RenderNode(Build.MANUFACTURER)

        const val enableLayerPersistence = false
    }

    @RequiresApi(29)
    private object UniqueDrawingIdApi29 {
        @JvmStatic fun getUniqueDrawingId(view: View) = view.uniqueDrawingId
    }
}

internal fun GraphicsContext.isLayerManagerInitialized(): Boolean =
    (this as AndroidGraphicsContext).isLayerManagerInitialized()

internal val isLayerPersistenceEnabled: Boolean
    get() = AndroidGraphicsContext.enableLayerPersistence

internal fun supportsV23RenderNode(manufacturer: String): Boolean =
    // See b/371012452. Some Android devices don't support the reflective stub implementation
    // of RenderNode. More specifically the cast of android.graphics.Canvas to
    // android.view.DisplayListCanvas fails when trying to draw the layer itself.
    // In these cases we should fallback to using the View based layer implementation instead
    !manufacturer.lowercase(Locale.ENGLISH).contains("vivo")
