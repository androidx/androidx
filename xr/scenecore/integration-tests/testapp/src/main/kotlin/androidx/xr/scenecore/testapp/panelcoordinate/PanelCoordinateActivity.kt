/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore.testapp.panelcoordinate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import java.nio.file.Paths
import kotlinx.coroutines.launch

class PanelCoordinateActivity : AppCompatActivity() {

    private val TAG = "PanelCoordinateActivity"
    private var session: Session? = null

    private lateinit var coordinateTypeRadioGroup: RadioGroup
    private lateinit var pixelCoordinateUi: LinearLayout
    private lateinit var uvCoordinateUi: LinearLayout

    private lateinit var scaleSlider: Slider
    private lateinit var scaleValue: TextView

    private lateinit var xPixelSlider: Slider
    private lateinit var yPixelSlider: Slider
    private lateinit var xPixelValue: TextView
    private lateinit var yPixelValue: TextView

    private lateinit var uSlider: Slider
    private lateinit var vSlider: Slider
    private lateinit var uValue: TextView
    private lateinit var vValue: TextView

    private lateinit var panel: PanelEntity
    private lateinit var xyzModel: GltfModel
    private lateinit var xyzEntity: GltfModelEntity
    private lateinit var panelSizeTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panel_coordinates)

        scaleSlider = findViewById(R.id.scale_slider)
        scaleValue = findViewById(R.id.scale_value)

        coordinateTypeRadioGroup = findViewById(R.id.coordinate_type_radio_group)
        pixelCoordinateUi = findViewById(R.id.pixel_coordinate_ui)
        uvCoordinateUi = findViewById(R.id.uv_coordinate_ui)

        xPixelSlider = findViewById(R.id.x_pixel_slider)
        yPixelSlider = findViewById(R.id.y_pixel_slider)
        xPixelValue = findViewById(R.id.x_pixel_value)
        yPixelValue = findViewById(R.id.y_pixel_value)

        uSlider = findViewById(R.id.u_slider)
        vSlider = findViewById(R.id.v_slider)
        uValue = findViewById(R.id.u_value)
        vValue = findViewById(R.id.v_value)

        setupMainPanelInitialState()
        setupMainPanelListeners()

        lifecycleScope.launch {
            val sessionResult = Session.create(this@PanelCoordinateActivity)
            if (sessionResult is SessionCreateSuccess) {
                session = sessionResult.session
                setupSecondaryPanelAndGltfEntity(session!!)
                session!!.scene.mainPanelEntity.size = FloatSize2d(1.2f, 0.8f)
                session?.scene?.keyEntity = session?.scene?.mainPanelEntity
            } else {
                this@PanelCoordinateActivity.finish()
            }
        }
    }

    private suspend fun setupSecondaryPanelAndGltfEntity(session: Session) {
        val inflater = LayoutInflater.from(this@PanelCoordinateActivity)
        val panelView = inflater.inflate(R.layout.panel_coordinates_secondary_panel, null)
        panelSizeTextView = panelView.findViewById(R.id.size_text_view)

        panel =
            PanelEntity.create(
                session,
                panelView,
                pixelDimensions = IntSize2d(1000, 1000),
                name = "SecondaryPanel",
                pose = Pose(Vector3(0.7f, 0.7f, -0.05f)),
            )
        session.scene.activitySpace.addChild(panel)

        val movable = MovableComponent.createSystemMovable(session, scaleInZ = false)
        val resizable =
            ResizableComponent.create(session) { event ->
                panel.size = FloatSize2d(event.newSize.width, event.newSize.height)
                updateSizeText()
                updateXyzPose()
            }
        panel.addComponent(movable)
        panel.addComponent(resizable)

        xyzModel = GltfModel.create(session, Paths.get("models", "xyzArrows.glb"))
        xyzEntity = GltfModelEntity.create(session, xyzModel)
        panel.addChild(xyzEntity)
        xyzEntity.setScale(0.2f)

        updateSizeText()
        updateXyzPose()
    }

    private fun updateSizeText() {
        panelSizeTextView.text =
            "Pixel Size: ${panel.sizeInPixels.width}x${panel.sizeInPixels.height} \n" +
                "Size in local units: ${panel.size.width}x${panel.size.height} \n" +
                "Scale in local units: ${String.format("%.2f", panel.getScale())}"
    }

    private fun setupMainPanelInitialState() {

        scaleSlider.valueFrom = 0.01f
        scaleSlider.valueTo = 3f
        scaleSlider.value = 1f
        scaleValue.text = "1.0f"

        xPixelSlider.valueFrom = -2000f
        xPixelSlider.valueTo = 2000f
        xPixelSlider.value = 0f
        yPixelSlider.valueFrom = -2000f
        yPixelSlider.valueTo = 2000f
        yPixelSlider.value = 0f
        xPixelValue.text = "0"
        yPixelValue.text = "0"

        uSlider.valueFrom = -2f
        uSlider.valueTo = 2f
        uSlider.value = 0.0f
        vSlider.valueFrom = -2f
        vSlider.valueTo = 2f
        vSlider.value = 0.0f
        uValue.text = "0.0"
        vValue.text = "0.0"

        findViewById<RadioButton>(R.id.pixel_coordinate_radio_button).isChecked = true

        // toolbar
        findViewById<Toolbar>(R.id.top_app_bar).also {
            setSupportActionBar(it)
            it.setNavigationOnClickListener { this@PanelCoordinateActivity.finish() }
        }

        // Recreate button
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(this@PanelCoordinateActivity) }
        }
    }

    private fun setupMainPanelListeners() {

        coordinateTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            pixelCoordinateUi.visibility =
                if (checkedId == R.id.pixel_coordinate_radio_button) View.VISIBLE else View.GONE
            uvCoordinateUi.visibility =
                if (checkedId == R.id.uv_coordinate_radio_button) View.VISIBLE else View.GONE
            updateXyzPose()
        }

        scaleSlider.addOnChangeListener { _, value, _ ->
            scaleValue.text = String.format("%.2f", value)
            if (::panel.isInitialized) {
                panel.setScale(value)
                updateSizeText()
                updateXyzPose()
            }
        }

        xPixelSlider.addOnChangeListener { _, value, _ ->
            xPixelValue.text = String.format("%.2f", value)
            updateXyzPose()
        }

        yPixelSlider.addOnChangeListener { _, value, _ ->
            yPixelValue.text = String.format("%.2f", value)
            updateXyzPose()
        }

        uSlider.addOnChangeListener { _, value, _ ->
            uValue.text = String.format("%.2f", value)
            updateXyzPose()
        }

        vSlider.addOnChangeListener { _, value, _ ->
            vValue.text = String.format("%.2f", value)
            updateXyzPose()
        }
    }

    private fun updateXyzPose() {
        if (!(::panel.isInitialized && ::xyzEntity.isInitialized)) return

        if (coordinateTypeRadioGroup.checkedRadioButtonId == R.id.pixel_coordinate_radio_button) {
            xyzEntity.setPose(
                Pose(
                    panel.transformPixelCoordinatesToLocalPosition(
                        Vector2(xPixelSlider.value, yPixelSlider.value)
                    )
                )
            )
        } else {
            xyzEntity.setPose(
                Pose(
                    panel.transformNormalizedCoordinatesToLocalPosition(
                        Vector2(uSlider.value, vSlider.value)
                    )
                )
            )
        }
    }
}
