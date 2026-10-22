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

package androidx.xr.scenecore.openxr.smoketest

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Vector3
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Phone-side controller for [SceneCorePanelActivity]: spawns panels on the connected XR glasses,
 * lists them, and lets the selected one be moved, recolored, resized or removed through
 * [SceneCorePanelStateHolder]. It also creates ARCore anchors on the glasses, attaches panels to
 * them, and shows what the glasses report about the Session and the anchors in
 * [SceneCoreAnchorList].
 *
 * [SceneCorePanelActivity] declares `requiredDisplayCategory=XR_PROJECTED`, so it can only be
 * started with activity options that target the projected display.
 */
@SuppressLint("NewApi", "SetTextI18n")
@OptIn(ExperimentalProjectedApi::class)
class SceneCoreOpenXrLauncherActivity : Activity() {

    private val scene = SceneCorePanelStateHolder.scene
    private val status = SceneCorePanelStateHolder.status
    private val scope = MainScope()
    private var nextId = 1
    private var nextAnchorId = 1
    private var selectedId: Int? = null

    private lateinit var x: Slider
    private lateinit var y: Slider
    private lateinit var z: Slider
    private lateinit var red: Slider
    private lateinit var green: Slider
    private lateinit var blue: Slider
    private lateinit var width: Slider
    private lateinit var height: Slider
    private lateinit var parentingDelay: Slider
    private lateinit var swatch: View
    private lateinit var list: LinearLayout
    private lateinit var positionHeader: TextView
    private lateinit var statusView: TextView
    private lateinit var anchorList: SceneCoreAnchorList
    private lateinit var eventLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val defaultPosition = SceneCorePanelSpec.DEFAULT_POSITION
        x = Slider("X", -3f, 3f, defaultPosition.x)
        y = Slider("Y", -3f, 3f, defaultPosition.y)
        // Relative to an anchor, a panel can also sit at or behind it.
        z = Slider("Z", -3f, 10f, defaultPosition.z)
        red = Slider("R", 0f, 255f, Color.red(DEFAULT_COLOR).toFloat(), decimals = 0)
        green = Slider("G", 0f, 255f, Color.green(DEFAULT_COLOR).toFloat(), decimals = 0)
        blue = Slider("B", 0f, 255f, Color.blue(DEFAULT_COLOR).toFloat(), decimals = 0)
        // Resizing re-creates the panel on the glasses, so only commit once the finger lifts.
        width = Slider("W", 100f, 2000f, DEFAULT_SIZE.width.toFloat(), 0, commitOnRelease = true)
        height = Slider("H", 100f, 2000f, DEFAULT_SIZE.height.toFloat(), 0, commitOnRelease = true)
        parentingDelay =
            Slider(
                "ms",
                0f,
                MAX_ANCHOR_PARENTING_DELAY_MILLIS,
                SceneCorePanelStateHolder.anchorParentingDelayMillis.value.toFloat(),
                decimals = 0,
                commitOnRelease = true,
                onChange = {
                    SceneCorePanelStateHolder.anchorParentingDelayMillis.value = it.roundToLong()
                },
            )
        swatch = View(this).apply { setBackgroundColor(DEFAULT_COLOR) }
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        positionHeader = header("Position (m)")
        statusView = TextView(this).apply { textSize = TEXT_SIZE_SP }
        anchorList = SceneCoreAnchorList(this, onDetach = ::detachAnchor, onRemove = ::removeAnchor)
        eventLog =
            TextView(this).apply {
                textSize = DETAIL_TEXT_SIZE_SP
                typeface = Typeface.MONOSPACE
            }

        val controls =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
            }
        controls.addView(header("Status"))
        controls.addView(statusView)
        controls.addView(positionHeader)
        listOf(x, y, z).forEach { controls.addView(it.view) }
        controls.addView(header("Color"))
        listOf(red, green, blue).forEach { controls.addView(it.view) }
        controls.addView(swatch, LinearLayout.LayoutParams(MATCH_PARENT, dp(48)))
        controls.addView(header("Dimensions (px)"))
        listOf(width, height).forEach { controls.addView(it.view) }
        controls.addView(
            button("Spawn panel") { spawn() },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(16) },
        )
        controls.addView(header("Anchors"))
        controls.addView(caption("New anchors are created at X, Y, Z in the activity space."))
        controls.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(
                    button("Anchor at X, Y, Z") { createAnchor() },
                    LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f),
                )
                addView(
                    button("Pin selected panel") { pinSelectedPanel() },
                    LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f),
                )
            }
        )
        controls.addView(
            CheckBox(this).apply {
                text = "Dispose an AnchorSpace once it reports ERROR"
                textSize = TEXT_SIZE_SP
                isChecked = SceneCorePanelStateHolder.disposeAnchorSpaceOnError.value
                setOnCheckedChangeListener { _, isChecked ->
                    SceneCorePanelStateHolder.disposeAnchorSpaceOnError.value = isChecked
                }
            }
        )
        controls.addView(caption("Wait before attaching panels to a new anchor:"))
        controls.addView(parentingDelay.view)
        controls.addView(anchorList.view)
        controls.addView(header("Panels"))
        controls.addView(
            caption(
                "Tap a panel to select or deselect it. Tap its parent to move it to the next anchor."
            )
        )
        controls.addView(list)
        controls.addView(header("Event log"))
        controls.addView(
            button("Clear log") {
                status.update { it.copy(events = emptyList(), lastError = null) }
            }
        )
        controls.addView(eventLog)

        setContentView(
            ScrollView(this).apply {
                // Keep the controls clear of the status and navigation bars (edge-to-edge window).
                fitsSystemWindows = true
                addView(controls)
            }
        )
        refreshList()
        if (status.value.attachmentNatives == null) {
            val attachmentNatives = SceneCoreDiagnostics.describeAttachmentNatives()
            status.update { it.copy(attachmentNatives = attachmentNatives) }
        }
        scope.launch {
            combine(scene, status) { scene, status -> scene to status }
                .collect { (scene, status) ->
                    releasePanelsOfFailedAnchors(scene, status)
                    refreshStatus(scene, status)
                }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** Adds a default panel, shows it on the glasses and selects it (which resets the sliders). */
    private fun spawn() {
        val spec =
            SceneCorePanelSpec(
                nextId++,
                SceneCorePanelSpec.DEFAULT_POSITION,
                DEFAULT_COLOR,
                DEFAULT_SIZE,
            )
        updatePanels { it + spec }
        showPanelActivity()
        Log.i(TAG, "Spawned panel ${spec.id} on the projected display.")
        select(spec.id)
    }

    /** Starts [SceneCorePanelActivity] on the projected display, which creates the Session. */
    private fun showPanelActivity() {
        // Starting the singleTop panel activity while it runs still pauses it, delivers a new
        // intent and resumes it, which pauses and resumes its Session too. The scene flows already
        // reach a running activity, so only start it when it isn't running.
        if (SceneCorePanelStateHolder.isPanelActivityRunning) return
        val options = ProjectedContext.createProjectedActivityOptions(this)
        val intent =
            Intent(this, SceneCorePanelActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent, options.toBundle())
    }

    private fun remove(id: Int) {
        updatePanels { list -> list.filterNot { it.id == id } }
        if (selectedId == id) selectedId = null
        refreshList()
    }

    /** Selects panel [id] and loads its position, color and size into the sliders. */
    private fun select(id: Int) {
        val spec = scene.value.panels.firstOrNull { it.id == id } ?: return
        selectedId = id
        x.value = spec.position.x
        y.value = spec.position.y
        z.value = spec.position.z
        red.value = Color.red(spec.color).toFloat()
        green.value = Color.green(spec.color).toFloat()
        blue.value = Color.blue(spec.color).toFloat()
        width.value = spec.sizeInPixels.width.toFloat()
        height.value = spec.sizeInPixels.height.toFloat()
        swatch.setBackgroundColor(spec.color)
        refreshList()
    }

    private fun deselect() {
        selectedId = null
        val defaultPosition = SceneCorePanelSpec.DEFAULT_POSITION
        x.value = defaultPosition.x
        y.value = defaultPosition.y
        z.value = defaultPosition.z
        refreshList()
    }

    /** Reloads the sliders from the selected panel, if it still exists, and the panel list. */
    private fun refreshPanels() {
        val selected = selectedId
        if (selected != null && scene.value.panels.any { it.id == selected }) {
            select(selected)
        } else {
            refreshList()
        }
    }

    /** Pushes the slider values to the selected panel, if any. */
    private fun onSliderChanged() {
        val id = selectedId ?: NO_PANEL
        // The sliders don't cover the parent, so keep the panel on its anchor.
        val anchorId = scene.value.panels.firstOrNull { it.id == id }?.anchorId
        val spec = spec(id, anchorId)
        swatch.setBackgroundColor(spec.color)
        if (spec.id == NO_PANEL) return
        updatePanels { list -> list.map { if (it.id == spec.id) spec else it } }
    }

    private fun spec(id: Int, anchorId: Int?) =
        SceneCorePanelSpec(
            id,
            Vector3(x.value, y.value, z.value),
            Color.rgb(red.value.roundToInt(), green.value.roundToInt(), blue.value.roundToInt()),
            IntSize2d(width.value.roundToInt(), height.value.roundToInt()),
            anchorId,
        )

    private fun updatePanels(transform: (List<SceneCorePanelSpec>) -> List<SceneCorePanelSpec>) {
        scene.update { it.copy(panels = transform(it.panels)) }
    }

    /** Creates an anchor on the glasses at the X, Y, Z sliders, in the activity space. */
    private fun createAnchor() {
        val anchor = SceneCoreAnchorSpec(nextAnchorId++, Vector3(x.value, y.value, z.value))
        scene.update { it.copy(anchors = it.anchors + anchor) }
        showPanelActivity()
        Log.i(TAG, "Requested anchor ${anchor.id} at ${anchor.position}.")
    }

    /** Creates an anchor where the selected panel is, and attaches the panel to it in place. */
    private fun pinSelectedPanel() {
        val panel = scene.value.panels.firstOrNull { it.id == selectedId }
        if (panel == null) {
            SceneCorePanelStateHolder.logEvent(TAG, "Select a panel to pin first.")
            return
        }
        if (panel.anchorId != null) {
            SceneCorePanelStateHolder.logEvent(
                TAG,
                "Panel ${panel.id} is already on Anchor ${panel.anchorId}.",
            )
            return
        }
        scene.update { it.pinPanel(panel.id, SceneCoreAnchorSpec(nextAnchorId++, panel.position)) }
        showPanelActivity()
        select(panel.id)
    }

    /** Moves panel [id] to its next parent: the activity space, then each anchor in turn. */
    private fun cycleParent(id: Int) {
        scene.update { it.withNextParent(id, status.value.failedAnchorIds) }
        refreshPanels()
    }

    private fun removeAnchor(id: Int) {
        scene.update { it.withoutAnchor(id) }
        refreshPanels()
    }

    private fun detachAnchor(id: Int) {
        scene.update { it.withDetachRequested(id) }
    }

    /**
     * Panels can't follow an anchor that failed, or whose AnchorSpace was disposed, so this moves
     * them back to the activity space.
     */
    private fun releasePanelsOfFailedAnchors(
        scene: SceneCoreSceneSpec,
        status: SceneCoreStatus,
    ) {
        val failedIds = status.failedAnchorIds
        if (scene.panels.none { it.anchorId in failedIds }) return
        this.scene.update { it.releasePanels(failedIds) }
        refreshPanels()
    }

    private fun refreshList() {
        list.removeAllViews()
        val panels = scene.value.panels
        val selectedAnchorId = panels.firstOrNull { it.id == selectedId }?.anchorId
        positionHeader.text =
            if (selectedAnchorId == null) "Position (m)"
            else "Position relative to Anchor $selectedAnchorId (m)"
        if (panels.isEmpty()) {
            list.addView(
                TextView(this).apply {
                    text = "No panel yet"
                    textSize = TEXT_SIZE_SP
                }
            )
        }
        panels.forEach { spec ->
            val row =
                LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    minimumHeight = dp(56)
                    setPadding(dp(8), 0, 0, 0)
                    if (spec.id == selectedId) setBackgroundColor(SELECTED_ROW_COLOR)
                    setOnClickListener {
                        if (spec.id == selectedId) deselect() else select(spec.id)
                    }
                }
            row.addView(
                TextView(this).apply {
                    text = "Panel ${spec.id}"
                    textSize = TEXT_SIZE_SP
                },
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f),
            )
            row.addView(
                button(spec.anchorId?.let { "Anchor $it" } ?: "Activity space") {
                    cycleParent(spec.id)
                }
            )
            row.addView(button("Remove") { remove(spec.id) })
            list.addView(row)
        }
    }

    private fun refreshStatus(scene: SceneCoreSceneSpec, status: SceneCoreStatus) {
        statusView.text = describeStatus(status)
        anchorList.bind(scene.anchors, status.anchors)
        // Newest first, so that the latest events show without scrolling.
        eventLog.text = status.events.asReversed().joinToString("\n").ifEmpty { "No events yet" }
    }

    private fun describeStatus(status: SceneCoreStatus): String = buildString {
        appendLine(
            status.runtimes?.let { "Runtimes: $it" }
                ?: "No Session: spawn a panel or create an anchor to start one."
        )
        if (status.usesArCoreFakes) {
            appendLine(
                "Warning: the ARCore fakes' anchors have no XrSpace, so AnchorSpaces go to " +
                    "ERROR. Build with -Pxr.scenecore.openxr.useRealArCore=true to use ARCore."
            )
        }
        appendLine("Session configured: ${if (status.isConfigured) "yes" else "no"}")
        appendLine("Attachment natives: ${status.attachmentNatives ?: "not checked"}")
        append("Last error: ${status.lastError ?: "none"}")
    }

    private fun header(label: String): TextView =
        TextView(this).apply {
            text = label
            textSize = HEADER_TEXT_SIZE_SP
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(16), 0, dp(4))
        }

    private fun caption(label: String): View =
        TextView(this).apply {
            text = label
            textSize = DETAIL_TEXT_SIZE_SP
            setPadding(0, dp(4), 0, dp(4))
        }

    /** A labeled [SeekBar] whose progress is mapped linearly onto the [min], [max] range. */
    private inner class Slider(
        label: String,
        private val min: Float,
        private val max: Float,
        initial: Float,
        private val decimals: Int = 2,
        private val commitOnRelease: Boolean = false,
        /** Called with the new value when the user changes it. By default, updates the panel. */
        private val onChange: (Float) -> Unit = { onSliderChanged() },
    ) {
        private val valueView =
            TextView(this@SceneCoreOpenXrLauncherActivity).apply {
                textSize = TEXT_SIZE_SP
                gravity = Gravity.END
                minWidth = dp(72)
            }
        private val bar =
            SeekBar(this@SceneCoreOpenXrLauncherActivity).apply {
                this.max = SLIDER_STEPS
                minimumHeight = dp(56)
                setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar,
                            progress: Int,
                            fromUser: Boolean,
                        ) {
                            valueView.text = String.format(Locale.US, "%.${decimals}f", value)
                            if (fromUser && !commitOnRelease) onChange(value)
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {}

                        override fun onStopTrackingTouch(seekBar: SeekBar) {
                            if (commitOnRelease) onChange(value)
                        }
                    }
                )
            }

        val view: View =
            LinearLayout(this@SceneCoreOpenXrLauncherActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(
                    TextView(context).apply {
                        text = label
                        textSize = TEXT_SIZE_SP
                    },
                    LinearLayout.LayoutParams(dp(32), WRAP_CONTENT),
                )
                addView(bar, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
                addView(valueView)
            }

        var value: Float
            get() = min + (max - min) * bar.progress / SLIDER_STEPS
            set(newValue) {
                bar.progress = ((newValue - min) / (max - min) * SLIDER_STEPS).roundToInt()
            }

        init {
            value = initial
        }
    }

    private companion object {
        const val TAG = "SceneCoreOpenXrLauncher"
        const val NO_PANEL = 0
        const val SLIDER_STEPS = 1000
        const val HEADER_TEXT_SIZE_SP = 22f
        const val SELECTED_ROW_COLOR = 0xFFE3F2FD.toInt()
        const val DEFAULT_COLOR = 0xFF0D47A1.toInt()
        const val MAX_ANCHOR_PARENTING_DELAY_MILLIS = 3000f
        val DEFAULT_SIZE = IntSize2d(1000, 1000)
    }
}

// Shared with SceneCoreAnchorList.

internal const val TEXT_SIZE_SP = 18f
internal const val DETAIL_TEXT_SIZE_SP = 14f

internal fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

internal fun Context.button(label: String, onClick: () -> Unit): Button =
    Button(this).apply {
        text = label
        textSize = TEXT_SIZE_SP
        setOnClickListener { onClick() }
    }
