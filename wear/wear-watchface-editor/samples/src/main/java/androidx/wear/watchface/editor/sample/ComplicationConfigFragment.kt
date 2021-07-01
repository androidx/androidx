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
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import androidx.wear.complications.ComplicationDataSourceInfo
import androidx.wear.complications.data.ComplicationData
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.RenderParameters.HighlightLayer
import androidx.wear.watchface.editor.ChosenComplicationDataSource
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
 * Configuration view for watch faces with multiple complicationSlots.
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

    companion object {
        private const val TAG = "ConfigView"
    }

    private lateinit var previewComplicationData: Map<Int, ComplicationData>
    private val drawRect = Rect()

    // One invisible button per complication.
    private val complicationButtons =
        watchFaceConfigActivity.editorSession.complicationSlotsState.mapValues { stateEntry ->
            // TODO(alexclarke): This button is a Rect which makes the tap animation look bad.
            if (stateEntry.value.fixedComplicationDataSource ||
                !stateEntry.value.isEnabled ||
                stateEntry.key == watchFaceConfigActivity.editorSession.backgroundComplicationSlotId
            ) {
                // Do not create a button for fixed complicationSlots, disabled complicationSlots,
                // or background complicationSlots.
                null
            } else {
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        setOnLongClickListener {
                            TooltipApi26.updateTooltip(it, watchFaceConfigActivity, stateEntry.key)
                            // Do not consume the long click so that the tooltip is shown by the
                            // default handler.
                            false
                        }
                    }
                    addView(this)
                }
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
            val rect = watchFaceConfigActivity.editorSession.complicationSlotsState[id]!!.bounds
            view?.layout(
                rect.left,
                rect.top,
                rect.right,
                rect.bottom
            )
        }
    }

    private fun onComplicationButtonClicked(complicationSlotId: Int) {
        watchFaceConfigActivity.coroutineScope.launch {
            val chosenComplicationDataSource =
                watchFaceConfigActivity.fragmentController.showComplicationConfig(
                    complicationSlotId
                )
            updateUi(chosenComplicationDataSource)
            // Redraw after the complication data source chooser has run.
            invalidate()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private object TooltipApi26 {
        fun updateTooltip(
            button: View,
            watchFaceConfigActivity: WatchFaceConfigActivity,
            complicationSlotId: Int
        ) {
            watchFaceConfigActivity.coroutineScope.launch {
                val dataSourceInfo =
                    watchFaceConfigActivity.editorSession
                        .getComplicationsDataSourceInfo()[complicationSlotId]
                button.tooltipText = getDataSourceInfoToast(dataSourceInfo)
            }
        }

        private fun getDataSourceInfoToast(dataSourceInfo: ComplicationDataSourceInfo?): String =
            dataSourceInfo?.name ?: "Empty complication data source"
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
                    RenderParameters.HighlightedElement.AllComplicationSlots,
                    Color.RED, // Red complication highlight.
                    Color.argb(128, 0, 0, 0) // Darken everything else.
                )
            ),
            editingSession.previewReferenceTimeMillis,
            previewComplicationData
        )
        canvas.drawBitmap(bitmap, drawRect, drawRect, null)
    }

    private fun updateUi(
        @Suppress("UNUSED_PARAMETER")
        chosenComplicationDataSource: ChosenComplicationDataSource?
    ) {
        // The fragment can use the chosen complication to update the UI.
    }
}
