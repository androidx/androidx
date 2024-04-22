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

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayerV23
import androidx.compose.ui.graphics.layer.GraphicsLayerV29
import androidx.compose.ui.graphics.layer.GraphicsViewLayer
import androidx.compose.ui.graphics.layer.LayerManager
import androidx.compose.ui.graphics.layer.view.DrawChildContainer
import androidx.compose.ui.graphics.layer.view.ViewLayerContainer

/**
 * Create a new [GraphicsContext] with the provided [ViewGroup] to contain [View] based layers.
 *
 * @param layerContainer [ViewGroup] used to contain [View] based layers that are created by the
 * returned [GraphicsContext]
 */
fun GraphicsContext(layerContainer: ViewGroup): GraphicsContext =
    AndroidGraphicsContext(layerContainer)

private class AndroidGraphicsContext(private val ownerView: ViewGroup) : GraphicsContext {

    private val lock = Any()
    private val layerManager = LayerManager(CanvasHolder())
    private var viewLayerContainer: DrawChildContainer? = null

    override fun createGraphicsLayer(): GraphicsLayer {
        synchronized(lock) {
            val ownerId = getUniqueDrawingId(ownerView)
            val layerImpl = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                GraphicsLayerV29(ownerId)
            } else if (isRenderNodeCompatible && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    GraphicsLayerV23(ownerView, ownerId)
                } catch (_: Throwable) {
                    // If we ever failed to create an instance of the RenderNode stub based
                    // GraphicsLayer, always fallback to creation of View based layers as it is
                    // unlikely that subsequent attempts to create a GraphicsLayer with RenderNode
                    // stubs would be successful.
                    isRenderNodeCompatible = false
                    GraphicsViewLayer(
                        obtainViewLayerContainer(ownerView),
                        ownerId
                    )
                }
            } else {
                GraphicsViewLayer(
                    obtainViewLayerContainer(ownerView),
                    ownerId
                )
            }
            return GraphicsLayer(layerImpl, layerManager).also { layer ->
                layerManager.persist(layer)
            }
        }
    }

    override fun releaseGraphicsLayer(layer: GraphicsLayer) {
        synchronized(lock) {
            layer.release()
        }
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
        @JvmStatic
        @androidx.annotation.DoNotInline
        fun getUniqueDrawingId(view: View) = view.uniqueDrawingId
    }
}
