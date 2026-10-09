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
import kotlinx.coroutines.flow.update

/**
 * Phone-side controller for [SceneCorePanelActivity]: spawns panels on the connected XR glasses,
 * lists them, and lets the selected one be moved, recolored, resized or removed through
 * [SceneCorePanelStateHolder].
 *
 * [SceneCorePanelActivity] declares `requiredDisplayCategory=XR_PROJECTED`, so it can only be
 * started with activity options that target the projected display.
 */
@SuppressLint("NewApi", "SetTextI18n")
@OptIn(ExperimentalProjectedApi::class)
class SceneCoreOpenXrLauncherActivity : Activity() {

    private val panels = SceneCorePanelStateHolder.panels
    private var nextId = 1
    private var selectedId: Int? = null

    private lateinit var x: Slider
    private lateinit var y: Slider
    private lateinit var z: Slider
    private lateinit var red: Slider
    private lateinit var green: Slider
    private lateinit var blue: Slider
    private lateinit var width: Slider
    private lateinit var height: Slider
    private lateinit var swatch: View
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        x = Slider("X", -3f, 3f, DEFAULT_POSITION.x)
        y = Slider("Y", -3f, 3f, DEFAULT_POSITION.y)
        z = Slider("Z", 0.1f, 10f, DEFAULT_POSITION.z)
        red = Slider("R", 0f, 255f, Color.red(DEFAULT_COLOR).toFloat(), decimals = 0)
        green = Slider("G", 0f, 255f, Color.green(DEFAULT_COLOR).toFloat(), decimals = 0)
        blue = Slider("B", 0f, 255f, Color.blue(DEFAULT_COLOR).toFloat(), decimals = 0)
        // Resizing re-creates the panel on the glasses, so only commit once the finger lifts.
        width = Slider("W", 100f, 2000f, DEFAULT_SIZE.width.toFloat(), 0, commitOnRelease = true)
        height = Slider("H", 100f, 2000f, DEFAULT_SIZE.height.toFloat(), 0, commitOnRelease = true)
        swatch = View(this).apply { setBackgroundColor(DEFAULT_COLOR) }
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val controls =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
            }
        controls.addView(header("Position (m)"))
        listOf(x, y, z).forEach { controls.addView(it.view) }
        controls.addView(header("Color"))
        listOf(red, green, blue).forEach { controls.addView(it.view) }
        controls.addView(swatch, LinearLayout.LayoutParams(MATCH_PARENT, dp(48)))
        controls.addView(header("Dimensions (px)"))
        listOf(width, height).forEach { controls.addView(it.view) }
        controls.addView(
            Button(this).apply {
                text = "Spawn panel"
                textSize = TEXT_SIZE_SP
                setOnClickListener { spawn() }
            },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(16) },
        )
        controls.addView(header("Panels"))
        controls.addView(list)

        setContentView(
            ScrollView(this).apply {
                // Keep the controls clear of the status and navigation bars (edge-to-edge window).
                fitsSystemWindows = true
                addView(controls)
            }
        )
        refreshList()
    }

    /** Adds a default panel, shows it on the glasses and selects it (which resets the sliders). */
    private fun spawn() {
        val spec = SceneCorePanelSpec(nextId++, DEFAULT_POSITION, DEFAULT_COLOR, DEFAULT_SIZE)
        panels.update { it + spec }
        // Idempotent thanks to launchMode=singleTop: a running panel activity just stays up.
        val options = ProjectedContext.createProjectedActivityOptions(this)
        val intent =
            Intent(this, SceneCorePanelActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent, options.toBundle())
        Log.i(TAG, "Spawned panel ${spec.id} on the projected display.")
        select(spec.id)
    }

    private fun remove(id: Int) {
        panels.update { list -> list.filterNot { it.id == id } }
        if (selectedId == id) selectedId = null
        refreshList()
    }

    /** Selects panel [id] and loads its position, color and size into the sliders. */
    private fun select(id: Int) {
        val spec = panels.value.firstOrNull { it.id == id } ?: return
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

    /** Pushes the slider values to the selected panel, if any. */
    private fun onSliderChanged() {
        val spec = spec(selectedId ?: NO_PANEL)
        swatch.setBackgroundColor(spec.color)
        if (spec.id == NO_PANEL) return
        panels.update { list -> list.map { if (it.id == spec.id) spec else it } }
    }

    private fun spec(id: Int) =
        SceneCorePanelSpec(
            id,
            Vector3(x.value, y.value, z.value),
            Color.rgb(red.value.roundToInt(), green.value.roundToInt(), blue.value.roundToInt()),
            IntSize2d(width.value.roundToInt(), height.value.roundToInt()),
        )

    private fun refreshList() {
        list.removeAllViews()
        if (panels.value.isEmpty()) {
            list.addView(
                TextView(this).apply {
                    text = "No panel yet"
                    textSize = TEXT_SIZE_SP
                }
            )
        }
        panels.value.forEach { spec ->
            val row =
                LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    minimumHeight = dp(56)
                    setPadding(dp(8), 0, 0, 0)
                    if (spec.id == selectedId) setBackgroundColor(SELECTED_ROW_COLOR)
                    setOnClickListener { select(spec.id) }
                }
            row.addView(
                TextView(this).apply {
                    text = "Panel ${spec.id}"
                    textSize = TEXT_SIZE_SP
                },
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f),
            )
            row.addView(
                Button(this).apply {
                    text = "Remove"
                    textSize = TEXT_SIZE_SP
                    setOnClickListener { remove(spec.id) }
                }
            )
            list.addView(row)
        }
    }

    private fun header(label: String): View =
        TextView(this).apply {
            text = label
            textSize = HEADER_TEXT_SIZE_SP
            setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(16), 0, dp(4))
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /** A labeled [SeekBar] whose progress is mapped linearly onto the [min], [max] range. */
    private inner class Slider(
        label: String,
        private val min: Float,
        private val max: Float,
        initial: Float,
        private val decimals: Int = 2,
        private val commitOnRelease: Boolean = false,
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
                            if (fromUser && !commitOnRelease) onSliderChanged()
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {}

                        override fun onStopTrackingTouch(seekBar: SeekBar) {
                            if (commitOnRelease) onSliderChanged()
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
        const val TEXT_SIZE_SP = 18f
        const val HEADER_TEXT_SIZE_SP = 22f
        const val SELECTED_ROW_COLOR = 0xFFE3F2FD.toInt()
        const val DEFAULT_COLOR = 0xFF0D47A1.toInt()
        val DEFAULT_POSITION = Vector3(0f, 0f, 0.9f)
        val DEFAULT_SIZE = IntSize2d(1000, 1000)
    }
}
