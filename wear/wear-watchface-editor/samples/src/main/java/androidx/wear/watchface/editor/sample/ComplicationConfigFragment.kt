/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface.editor.sample

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.support.wearable.watchface.Constants
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.wear.complications.data.ComplicationData
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.LayerMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.style.Layer
import androidx.wear.widget.SwipeDismissFrameLayout
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * This fragment lets the user select a non-background complication to configure.
 */
internal class ComplicationConfigFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ConfigView(
            requireContext(),
            activity as WatchFaceConfigActivity
        ).apply {
            isSwipeable = true
            addCallback(
                object : SwipeDismissFrameLayout.Callback() {
                    override fun onDismissed(layout: SwipeDismissFrameLayout) {
                        parentFragmentManager.popBackStackImmediate()
                    }
                }
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.PROVIDER_CHOOSER_REQUEST_CODE &&
            resultCode == Activity.RESULT_OK
        ) {
            // Exit the configuration flow.
            activity?.finish()
        }
    }
}

/**
 * Configuration view for watch faces with multiple complications.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressWarnings(
    "ViewConstructor", // Internal view, not intended for use by tools.
    "ClickableViewAccessibility" // performClick would be ambiguous.
)
internal class ConfigView(
    context: Context,

    val watchFaceConfigActivity: WatchFaceConfigActivity
) : SwipeDismissFrameLayout(context) {

    private lateinit var previewComplicationData: Map<Int, ComplicationData>

    init {
        watchFaceConfigActivity.coroutineScope.launch {
            previewComplicationData =
                watchFaceConfigActivity.editorSession.getComplicationsPreviewData()
            setWillNotDraw(false)
        }
    }

    /**
     * Event info class to hold the position and type of the event.
     * We will use this structure to cache the details of the last
     * motion event
     */
    data class EventInfo(
        var eventPositionX: Int,
        var eventPositionY: Int,
        var eventType: Int
    )

    private var selectedComplicationId: Int? = null
    private val drawRect = Rect()
    private var lastEventInfo: EventInfo? = null
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        @SuppressWarnings("SyntheticAccessor")
        override fun onDown(e: MotionEvent): Boolean {
            lastEventInfo =
                EventInfo(
                    e.x.toInt(),
                    e.y.toInt(),
                    MotionEvent.ACTION_DOWN
                )
            return true
        }

        @SuppressWarnings("SyntheticAccessor")
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return if (onTap(e.x.toInt(), e.y.toInt())) {
                invalidate()
                true
            } else {
                super.onSingleTapUp(e)
            }
        }
    }
    private val gestureDetector = GestureDetector(context, gestureListener)

    /**
     * Called when the user taps on the view.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @SuppressWarnings("unused")
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun onTap(
        tapX: Int,
        tapY: Int
    ): Boolean {
        // Check if the user tapped on any of the complications, but with the supplied calendar.
        // This is to support freezing of animated complications while the user selects one to
        // configure.
        val complicationId =
            watchFaceConfigActivity.editorSession.getComplicationIdAt(tapX, tapY) ?: return false

        // Silently do nothing if the complication is fixed. Note the user is given a visual clue
        // that the complication is not editable in [Complication.drawOutline] so this is OK.
        val complicationState =
            watchFaceConfigActivity.editorSession.complicationsState[complicationId]!!
        if (complicationState.fixedComplicationProvider) {
            return true
        }

        // Briefly highlight the complication.
        selectedComplicationId = complicationId
        invalidate()

        watchFaceConfigActivity.handler.postDelayed(
            {
                selectedComplicationId = null
                invalidate()
            },
            100
        )
        watchFaceConfigActivity.coroutineScope.launch {
            watchFaceConfigActivity.fragmentController.showComplicationConfig(complicationId)
            // Redraw after the complication provider chooser has run.
            invalidate()
        }

        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }
        // This is to handle strange gestureDetector behavior logged here: http://b/153992719
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            lastEventInfo?.let {
                if ((it.eventType == MotionEvent.ACTION_DOWN) &&
                    (abs(it.eventPositionX - event.rawX.toInt()) < 1) &&
                    (abs(it.eventPositionY - event.rawY.toInt()) < 1)
                ) {
                    onTap(event.rawX.toInt(), event.rawY.toInt())
                }
            }
            lastEventInfo = null
        }
        return super.onTouchEvent(event)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        drawRect.set(0, 0, width, height)
    }

    override fun onDraw(canvas: Canvas) {
        val editingSession = watchFaceConfigActivity.editorSession
        val bitmap = editingSession.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                mapOf(
                    Layer.BASE to LayerMode.DRAW,
                    Layer.COMPLICATIONS to LayerMode.DRAW_OUTLINED,
                    Layer.COMPLICATIONS_OVERLAY to LayerMode.DRAW
                ),
                selectedComplicationId,
                Color.RED
            ),
            editingSession.previewReferenceTimeMillis,
            previewComplicationData
        )
        canvas.drawBitmap(bitmap, drawRect, drawRect, null)
    }
}
