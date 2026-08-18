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

package androidx.compose.remote.integration.demos.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.utilities.IntMap
import androidx.compose.remote.player.core.platform.AndroidCustomContext

/**
 * Platform-specific support manager delegating custom component creation, property configuration,
 * measurements, touch handling, and drawing pipelines to registered [CustomComponentDelegate]
 * instances.
 */
@SuppressLint("RestrictedApiAndroidX")
public class AndroidCustomContextImpl(
    initialDelegates: Map<String, CustomComponentDelegate> = emptyMap()
) : AndroidCustomContext {

    // Registered component delegates matching config names
    private val delegates = HashMap<String, CustomComponentDelegate>()

    private val activeDelegates = IntMap<CustomComponentDelegate>()

    private var context: Context? = null

    private var canvas: Canvas? = null

    private var remoteContext: RemoteContext? = null

    init {
        delegates.putAll(initialDelegates)
    }

    /** Registers a [CustomComponentDelegate] matching the configuration name. */
    public fun registerDelegate(name: String, delegate: CustomComponentDelegate) {
        delegates[name] = delegate
    }

    override fun setRemoteContext(remoteContext: RemoteContext?) {
        this.remoteContext = remoteContext
    }

    public fun getRemoteContext(): RemoteContext? {
        return remoteContext
    }

    /** Sets the active Android Context for component instantiation. */
    override fun setContext(context: Context) {
        this.context = context
    }

    /** Sets the active Android Canvas for custom component drawing. */
    override fun setCanvas(canvas: Canvas?) {
        this.canvas = canvas
    }

    /** Creates a custom component matching the requested configuration type. */
    override fun createCustom(id: Int, config: String) {
        val currentContext = context ?: return

        var delegate: CustomComponentDelegate? = null
        for (name in delegates.keys) {
            if (name.equals(config, ignoreCase = true)) {
                delegate = delegates[name]
                break
            }
        }

        if (delegate != null) {
            activeDelegates.put(id, delegate)
            delegate.create(id, currentContext)
        }
    }

    /** Sets a String property configuration on the target component. */
    override fun configureCustom(id: Int, type: Int, value: String) {
        activeDelegates.get(id)?.configure(id, type, value)
    }

    /** Sets an integer property configuration on the target component. */
    override fun configureCustom(id: Int, type: Int, value: Int) {
        activeDelegates.get(id)?.configure(id, type, value)
    }

    /** Sets a float property configuration on the target component. */
    override fun configureCustom(id: Int, type: Int, value: Float) {
        activeDelegates.get(id)?.configure(id, type, value)
    }

    /** Runs Measurement Pass on the custom component. */
    override fun measureCustom(id: Int, bounds: FloatArray) {
        val currentContext = context ?: return
        activeDelegates.get(id)?.measure(id, currentContext, bounds)
    }

    /** Runs Layout Positioning Pass on the custom component. */
    override fun layoutCustom(id: Int, bounds: FloatArray) {
        activeDelegates.get(id)?.layout(id, bounds)
    }

    /** Dispatches touch events to the target custom component. */
    override fun touchCustom(id: Int, type: Int, x: Float, y: Float): Boolean {
        return activeDelegates.get(id)?.onTouch(id, type, x, y) ?: false
    }

    /** Renders the custom component onto the player Canvas. */
    override fun drawCustom(id: Int) {
        val currentCanvas = canvas ?: return
        activeDelegates.get(id)?.draw(id, currentCanvas)
    }

    /**
     * Common delegate interface for custom components in RemoteCompose.
     *
     * This allows both View-less direct Canvas renderers and View-backed adapters to participate in
     * component creation, property configuration, measurement, layout, touch handling, and drawing.
     */
    public interface CustomComponentDelegate {

        /**
         * Initializes a custom component instance.
         *
         * @param id unique component identifier
         * @param context active Android Context
         */
        public fun create(id: Int, context: Context) {}

        /**
         * Configures a String property on the custom component.
         *
         * @param id unique component identifier
         * @param type property identifier constant
         * @param value String value to set
         */
        public fun configure(id: Int, type: Int, value: String) {}

        /**
         * Configures an integer property on the custom component.
         *
         * @param id unique component identifier
         * @param type property identifier constant
         * @param value integer value to set
         */
        public fun configure(id: Int, type: Int, value: Int) {}

        /**
         * Configures a float property on the custom component.
         *
         * @param id unique component identifier
         * @param type property identifier constant
         * @param value float value to set
         */
        public fun configure(id: Int, type: Int, value: Float) {}

        /**
         * Invokes measurement on the custom component.
         *
         * Input bounds: [minWidth, maxWidth, minHeight, maxHeight]. Output dimensions:
         * [outWidth, outHeight] should be written back to bounds[2], bounds[3].
         *
         * @param id unique component identifier
         * @param context active Android Context
         * @param bounds float array containing measurement constraints and output dimensions
         */
        public fun measure(id: Int, context: Context, bounds: FloatArray) {}

        /**
         * Informs the custom component of its final layout coordinates and dimensions.
         *
         * @param id unique component identifier
         * @param bounds float array containing [x, y, width, height]
         */
        public fun layout(id: Int, bounds: FloatArray) {}

        /**
         * Informs the custom component of touch events.
         *
         * @param id unique component identifier
         * @param type touch event type (e.g.
         *   [androidx.compose.remote.core.CustomContext.TOUCH_DOWN])
         * @param x local x coordinate
         * @param y local y coordinate
         * @return true if the event was handled
         */
        public fun onTouch(id: Int, type: Int, x: Float, y: Float): Boolean = false

        /**
         * Renders the custom component onto the player Canvas.
         *
         * @param id unique component identifier
         * @param canvas active Canvas to draw into
         */
        public fun draw(id: Int, canvas: Canvas) {}
    }
}
