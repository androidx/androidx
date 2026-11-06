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

package androidx.compose.remote.creation.compose.layout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.compose.remote.core.CustomContext
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.utilities.IntMap
import androidx.compose.remote.player.core.platform.AndroidComponentSupport
import androidx.compose.remote.player.core.platform.AndroidCustomContext
import androidx.compose.remote.player.view.RemoteComposePlayer

/**
 * Platform-specific support manager delegating Native Android View instantiations, configurations,
 * measurements, and drawing pipelines under the player hierarchy.
 */
@SuppressLint("RestrictedApiAndroidX")
public class AndroidCustomContextImpl(
    private var player: RemoteComposePlayer? = null,
    initialDelegates: Map<String, AndroidComponentSupport> = emptyMap(),
) : AndroidCustomContext {

    // Registered component support delegates matching config names
    private val delegates = HashMap<String, AndroidComponentSupport>()

    private val customViews = IntMap<View>()

    private val customDelegates = IntMap<AndroidComponentSupport>()

    private var context: Context? = null

    private var canvas: Canvas? = null

    private var remoteContext: RemoteContext? = null

    private var lastDownTime: Long = 0L

    init {
        delegates.putAll(initialDelegates)
    }

    public fun registerDelegate(name: String, delegate: AndroidComponentSupport) {
        delegates[name] = delegate
    }

    override fun setRemoteContext(remoteContext: RemoteContext?) {
        this.remoteContext = remoteContext
    }

    public fun getRemoteContext(): RemoteContext? {
        return remoteContext
    }

    /** Sets the active Android Context for view instantiation. */
    override fun setContext(context: Context) {
        this.context = context
    }

    /** Sets the active Android Canvas for custom view drawing. */
    override fun setCanvas(canvas: Canvas?) {
        this.canvas = canvas
    }

    /** Creates a native view matching the requested configuration type. */
    override fun createCustom(id: Int, config: String) {
        val currentContext = context ?: return

        var delegate: AndroidComponentSupport? = null
        for (name in delegates.keys) {
            if (name.equals(config, ignoreCase = true)) {
                delegate = delegates[name]
                break
            }
        }

        if (delegate != null) {
            val view = delegate.createView(currentContext)
            customViews.put(id, view)
            customDelegates.put(id, delegate)
        }
    }

    /** Sets a String property configuration on the target view. */
    override fun configureCustom(id: Int, type: Int, value: String) {
        val view = customViews.get(id)
        val delegate = customDelegates.get(id)
        if (view != null && delegate != null) {
            delegate.configure(view, type, value)
        }
    }

    /** Sets an integer property configuration on the target view. */
    override fun configureCustom(id: Int, type: Int, value: Int) {
        val view = customViews.get(id)
        val delegate = customDelegates.get(id)
        if (view != null && delegate != null) {
            delegate.configure(view, type, value)
        }
    }

    /** Sets a float property configuration on the target view. */
    override fun configureCustom(id: Int, type: Int, value: Float) {
        val view = customViews.get(id)
        val delegate = customDelegates.get(id)
        if (view != null && delegate != null) {
            delegate.configure(view, type, value)
        }
    }

    /** Runs native Measurement Pass on the cached view and returns calculated dimensions. */
    override fun measureCustom(id: Int, bounds: FloatArray) {
        val view = customViews.get(id)
        if (view != null) {
            val minWidth = bounds[0]
            val maxWidth = bounds[1]
            val minHeight = bounds[2]
            val maxHeight = bounds[3]
            val h =
                if (minHeight == maxHeight) {
                    View.MeasureSpec.EXACTLY
                } else {
                    View.MeasureSpec.AT_MOST
                }
            val w =
                if (minWidth == maxWidth) {
                    View.MeasureSpec.EXACTLY
                } else {
                    View.MeasureSpec.AT_MOST
                }
            val widthSpec =
                View.MeasureSpec.makeMeasureSpec(
                    maxWidth.toInt(),
                    if (maxWidth == Float.MAX_VALUE) View.MeasureSpec.UNSPECIFIED else w,
                )
            val heightSpec =
                View.MeasureSpec.makeMeasureSpec(
                    maxHeight.toInt(),
                    if (maxHeight == Float.MAX_VALUE) View.MeasureSpec.UNSPECIFIED else h,
                )

            view.measure(widthSpec, heightSpec)
            bounds[0] = 0f
            bounds[1] = 0f
            bounds[2] = maxOf(minWidth, view.measuredWidth.toFloat())
            bounds[3] = maxOf(minHeight, view.measuredHeight.toFloat())
        }
    }

    /** Runs native Layout Positioning Pass on the cached view. */
    override fun layoutCustom(id: Int, bounds: FloatArray) {
        val view = customViews.get(id)
        if (view != null) {
            val width = bounds[2]
            val height = bounds[3]
            view.layout(0, 0, width.toInt(), height.toInt())
        }
    }

    override fun touchCustom(id: Int, type: Int, x: Float, y: Float): Boolean {
        val view = customViews.get(id) ?: return false
        val now = SystemClock.uptimeMillis()
        val action =
            when (type) {
                CustomContext.TOUCH_DOWN -> {
                    lastDownTime = now
                    MotionEvent.ACTION_DOWN
                }
                CustomContext.TOUCH_DRAG -> MotionEvent.ACTION_MOVE
                CustomContext.TOUCH_UP -> MotionEvent.ACTION_UP
                else -> return false
            }
        // TODO: Plumb downTime and eventTime from the player's MotionEvent through CustomContext
        val event =
            MotionEvent.obtain(
                /* downTime = */ lastDownTime,
                /* eventTime = */ now,
                /* action = */ action,
                /* x = */ x,
                /* y = */ y,
                /* metaState = */ 0,
            )
        return try {
            view.dispatchTouchEvent(event)
        } finally {
            event.recycle()
        }
    }

    /** Renders the native custom view onto the player Canvas. */
    override fun drawCustom(id: Int) {
        val currentCanvas = canvas ?: return
        val view = customViews.get(id) ?: return
        view.draw(currentCanvas)
    }
}
