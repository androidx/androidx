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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import androidx.wear.complications.data.ComplicationData
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.RenderParameters.HighlightLayer
import androidx.wear.watchface.style.WatchFaceLayer
import androidx.wear.widget.SwipeDismissFrameLayout
import kotlinx.coroutines.launch

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

    private val watchFaceConfigActivity: WatchFaceConfigActivity
) : SwipeDismissFrameLayout(context) {

    private lateinit var previewComplicationData: Map<Int, ComplicationData>
    private val drawRect = Rect()

    // One invisible button per complication.
    private val complicationButtons =
        watchFaceConfigActivity.editorSession.complicationsState.mapValues { stateEntry ->
            // TODO(alexclarke): This button is a Rect which makes the tap animation look bad.
            Button(context).apply {
                // Make the button transparent unless tapped upon.
                setBackgroundResource(
                    TypedValue().apply {
                        context.theme.resolveAttribute(
                            android.R.attr.selectableItemBackground,
                            this,
                            true
                        )
                    }.resourceId
                )
                setOnClickListener { onComplicationButtonClicked(stateEntry.key) }
                addView(this)
            }
        }

    init {
        watchFaceConfigActivity.coroutineScope.launch {
            previewComplicationData =
                watchFaceConfigActivity.editorSession.getComplicationsPreviewData()
            setWillNotDraw(false)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        for ((id, view) in complicationButtons) {
            val rect = watchFaceConfigActivity.editorSession.complicationsState[id]!!.bounds
            view.layout(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom
            )
        }
    }

    private fun onComplicationButtonClicked(complicationId: Int) {
        watchFaceConfigActivity.coroutineScope.launch {
            watchFaceConfigActivity.fragmentController.showComplicationConfig(complicationId)
            // Redraw after the complication provider chooser has run.
            invalidate()
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        drawRect.set(0, 0, width, height)
    }

    override fun onDraw(canvas: Canvas) {
        val editingSession = watchFaceConfigActivity.editorSession
        val bitmap = editingSession.renderWatchFaceToBitmap(
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                HighlightLayer(
                    RenderParameters.HighlightedElement.AllComplications,
                    Color.RED, // Red complication highlight.
                    Color.argb(128, 0, 0, 0) // Darken everything else.
                )
            ),
            editingSession.previewReferenceTimeMillis,
            previewComplicationData
        )
        canvas.drawBitmap(bitmap, drawRect, drawRect, null)
    }
}
