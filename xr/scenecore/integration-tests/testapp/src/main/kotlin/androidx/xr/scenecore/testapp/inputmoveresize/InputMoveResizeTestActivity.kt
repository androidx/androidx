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

package androidx.xr.scenecore.testapp.inputmoveresize

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.InteractableComponent
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeEvent
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.createSession
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.concurrent.Executors
import java.util.function.Consumer

@SuppressLint("SetTextI18n", "RestrictedApi")
class InputMoveResizeTestActivity : AppCompatActivity() {
    private var session: Session? = null
    private val executor by lazy { Executors.newSingleThreadExecutor() }
    private var interactablePanelActive = false
    private var movablePanelActive = false
    private var resizablePanelActive = false
    private var mainPanelMovableActive = false
    private var mainPanelResizableActive = false

    private val moveListener =
        object : EntityMoveListener {
            override fun onMoveStart(
                entity: Entity,
                initialInputRay: Ray,
                initialPose: Pose,
                initialScale: Float,
                initialParent: Entity,
            ) {
                Log.i(TAG, "$entity $initialInputRay $initialPose $initialScale $initialParent")
            }

            override fun onMoveUpdate(
                entity: Entity,
                currentInputRay: Ray,
                currentPose: Pose,
                currentScale: Float,
            ) {
                Log.i(TAG, "$entity $currentInputRay$currentPose $currentScale")
                updatePoseAndScale(entity, currentPose, currentScale)
            }

            override fun onMoveEnd(
                entity: Entity,
                finalInputRay: Ray,
                finalPose: Pose,
                finalScale: Float,
                updatedParent: Entity?,
            ) {
                Log.i(TAG, "$entity $finalInputRay $finalPose $finalScale $updatedParent")
                updatePoseAndScale(entity, finalPose, finalScale)
            }
        }

    private val resizeListener =
        Consumer<ResizeEvent> { resizeEvent: ResizeEvent ->
            Log.i(
                TAG,
                "ResizeEvent(entity: ${resizeEvent.entity}, resizeState: ${resizeEvent.resizeState}, newSize: ${resizeEvent.newSize}",
            )
            if (resizeEvent.resizeState == ResizeEvent.ResizeState.END) {
                (resizeEvent.entity as PanelEntity).size = resizeEvent.newSize.to2d()
            }
        }

    companion object {
        private const val TAG = "InputMoveResizeTest"
    }

    private fun updatePoseAndScale(entity: Entity, pose: Pose, scale: Float) {
        Log.i(TAG, "$entity $pose $scale")
        entity.setPose(pose)
        entity.setScale(scale)
    }

    private fun updateTextInPanel(text: String, panel: View) {
        val textView = panel.findViewById<TextView>(R.id.textView)
        textView.text = text
    }

    private fun createPanelEntityWithText(text: String, panel: View): PanelEntity {
        updateTextInPanel(text, panel)
        val switch = panel.findViewById<MaterialSwitch>(R.id.switch1)
        val switchText = "$text Switch"
        switch.text = switchText

        val panelEntity = PanelEntity.create(session!!, panel, IntSize2d(640, 480), "panel")
        panelEntity.setPose(Pose(Vector3(0f, -0.5f, 0.5f)))
        panelEntity.parent = session!!.scene.activitySpace
        return panelEntity
    }

    private fun changeTextAndBGColor(textView: TextView) {
        val backgroundColor = (Math.random() * 0xffffff).toInt()
        textView.setBackgroundColor((backgroundColor + 0xff000000).toInt())
        textView.setTextColor((0xffffffff - backgroundColor).toInt())
    }

    /** Setup the main panel to be movable and resizable. */
    private fun setupMainPanel() {
        val mainPanelSystemMovable = findViewById<CheckBox>(R.id.systemMovable)
        mainPanelSystemMovable.isChecked = true
        val mainPanelScaleInZ = findViewById<CheckBox>(R.id.scaleInZ)
        mainPanelScaleInZ.isChecked = true
        var mainPanelMovableComponent = MovableComponent.createSystemMovable(session!!)
        mainPanelMovableComponent.size = session!!.scene.mainPanelEntity.size.to3d()

        fun updateMainPanelMovableComponent() {
            if (mainPanelMovableActive) {
                session!!.scene.mainPanelEntity.removeComponent(mainPanelMovableComponent)
            }
            mainPanelMovableComponent =
                if (mainPanelSystemMovable.isChecked) {
                    MovableComponent.createSystemMovable(session!!, mainPanelScaleInZ.isChecked)
                } else {
                    MovableComponent.createCustomMovable(
                        session!!,
                        mainPanelScaleInZ.isChecked,
                        executor,
                        moveListener,
                    )
                }
        }

        val mainPanelCheckBoxListener =
            CompoundButton.OnCheckedChangeListener { _, _ ->
                updateMainPanelMovableComponent()
                mainPanelMovableActive =
                    session!!.scene.mainPanelEntity.addComponent(mainPanelMovableComponent)
            }

        mainPanelSystemMovable.setOnCheckedChangeListener(mainPanelCheckBoxListener)
        mainPanelScaleInZ.setOnCheckedChangeListener(mainPanelCheckBoxListener)

        val mainPanelMovableSwitch = findViewById<MaterialSwitch>(R.id.movableSwitch)
        mainPanelMovableSwitch.setOnCheckedChangeListener { _, isChecked ->
            mainPanelMovableComponent.size = session!!.scene.mainPanelEntity.size.to3d()
            when (isChecked) {
                true -> {
                    updateMainPanelMovableComponent()
                    mainPanelMovableActive =
                        session!!.scene.mainPanelEntity.addComponent(mainPanelMovableComponent)
                    mainPanelSystemMovable.visibility = View.VISIBLE
                    mainPanelScaleInZ.visibility = View.VISIBLE
                }
                false -> {
                    if (mainPanelMovableActive) {
                        session!!.scene.mainPanelEntity.removeComponent(mainPanelMovableComponent)
                    }
                    mainPanelSystemMovable.visibility = View.GONE
                    mainPanelScaleInZ.visibility = View.GONE
                }
            }
        }

        val mainPanelResizableComponent =
            ResizableComponent.create(
                session!!,
                executor = mainExecutor,
                resizeEventListener = resizeListener,
            )
        mainPanelResizableComponent.affordanceSize = session!!.scene.mainPanelEntity.size.to3d()

        val mainPanelAnyAspectRatioButton = findViewById<RadioButton>(R.id.radioButton1)
        mainPanelAnyAspectRatioButton.text = getString(R.string.any_aspect_ratio_label)
        val mainPanelPortraitAspectRadioButton = findViewById<RadioButton>(R.id.radioButton2)
        mainPanelPortraitAspectRadioButton.text = getString(R.string.portrait_label)
        val mainPanelLandscapeAspectRadioButton = findViewById<RadioButton>(R.id.radioButton3)
        mainPanelLandscapeAspectRadioButton.text = getString(R.string.landscape_label)
        val mainPanelAspectRatioRadioGroup = findViewById<RadioGroup>(R.id.radioGroup1)
        mainPanelAspectRatioRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                // Portrait aspect ratio.
                R.id.radioButton2 -> {
                    val updatedSize =
                        FloatSize2d(
                            session!!.scene.mainPanelEntity.size.height * 0.7f,
                            session!!.scene.mainPanelEntity.size.height,
                        )
                    session!!.scene.mainPanelEntity.size = updatedSize
                    mainPanelResizableComponent.affordanceSize =
                        FloatSize3d(updatedSize.width, updatedSize.height, 1.0f)
                    mainPanelResizableComponent.isFixedAspectRatioEnabled = true
                }
                // Landscape aspect ratio.
                R.id.radioButton3 -> {
                    val updatedSize =
                        FloatSize2d(
                            session!!.scene.mainPanelEntity.size.height / 0.7f,
                            session!!.scene.mainPanelEntity.size.height,
                        )
                    session!!.scene.mainPanelEntity.size = updatedSize
                    mainPanelResizableComponent.affordanceSize =
                        FloatSize3d(updatedSize.width, updatedSize.height, 1.0f)
                    mainPanelResizableComponent.isFixedAspectRatioEnabled = true
                }
                // No preference on the aspect ratio.
                else -> mainPanelResizableComponent.isFixedAspectRatioEnabled = false
            }
        }
        mainPanelResizableComponent.isFixedAspectRatioEnabled = false // no preferences initially

        val mainPanelResizableSwitch = findViewById<MaterialSwitch>(R.id.resizableSwitch)
        mainPanelResizableSwitch.setOnCheckedChangeListener { _, isChecked ->
            mainPanelResizableComponent.affordanceSize = session!!.scene.mainPanelEntity.size.to3d()
            when (isChecked) {
                true ->
                    mainPanelResizableActive =
                        session!!.scene.mainPanelEntity.addComponent(mainPanelResizableComponent)
                false ->
                    if (mainPanelResizableActive) {
                        session!!.scene.mainPanelEntity.removeComponent(mainPanelResizableComponent)
                    }
            }
            mainPanelAspectRatioRadioGroup.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        // Toolbar action
        val toolbar: Toolbar = findViewById(R.id.toolbar_input_move_resize)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { this.finish() }
    }

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input_move_resize)

        // Create session
        session = createSession(this)
        if (session == null) this.finish()

        if (intent.extras != null) {
            findViewById<Toolbar>(R.id.toolbar_input_move_resize).also {
                val toolbarTitle = intent.extras!!.getString("MAIN_PANEL_TITLE", "")
                if (toolbarTitle != "") it.setTitle(toolbarTitle)
            }
        }

        setupMainPanel()

        // Recreate button
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(this@InputMoveResizeTestActivity) }
        }

        // Create the movable spatial panel.
        val movablePanelView = layoutInflater.inflate(R.layout.input_move_resize_panel, null)
        val movablePanelEntity = createPanelEntityWithText("Movable", movablePanelView)
        movablePanelEntity.setPose(Pose(Vector3(-0.8f, 0.2f, 0.1f)))
        movablePanelEntity.parent = session!!.scene.mainPanelEntity
        // Set the movable panel corner radius to 0.
        movablePanelEntity.cornerRadius = 0.0f

        val systemMovableCheckbox = movablePanelView.findViewById<CheckBox>(R.id.systemMovable)
        val scaleInZCheckBox = movablePanelView.findViewById<CheckBox>(R.id.scaleInZ)

        systemMovableCheckbox.isChecked = true
        scaleInZCheckBox.isChecked = true

        var movablePanelComponent = MovableComponent.createSystemMovable(session!!)
        movablePanelComponent.size = movablePanelEntity.size.to3d()
        fun updateMovablePanelComponent() {
            if (movablePanelActive) {
                movablePanelEntity.removeComponent(movablePanelComponent)
            }
            movablePanelComponent =
                if (systemMovableCheckbox.isChecked) {
                    MovableComponent.createSystemMovable(session!!, scaleInZCheckBox.isChecked)
                } else {
                    MovableComponent.createCustomMovable(
                        session!!,
                        scaleInZCheckBox.isChecked,
                        executor,
                        moveListener,
                    )
                }
        }
        val checkBoxListener =
            CompoundButton.OnCheckedChangeListener { _, _ ->
                updateMovablePanelComponent()
                movablePanelActive = movablePanelEntity.addComponent(movablePanelComponent)
            }

        systemMovableCheckbox.setOnCheckedChangeListener(checkBoxListener)
        scaleInZCheckBox.setOnCheckedChangeListener(checkBoxListener)

        val movablePanelSwitch = movablePanelView.findViewById<MaterialSwitch>(R.id.switch1)
        movablePanelSwitch.text = getString(R.string.movable_label)
        movablePanelSwitch.setOnCheckedChangeListener { _, isChecked ->
            movablePanelComponent.size = movablePanelEntity.size.to3d()
            when (isChecked) {
                true -> {
                    updateMovablePanelComponent()
                    movablePanelActive = movablePanelEntity.addComponent(movablePanelComponent)
                    systemMovableCheckbox.visibility = View.VISIBLE
                    scaleInZCheckBox.visibility = View.VISIBLE
                }
                false -> {
                    if (movablePanelActive) {
                        movablePanelEntity.removeComponent(movablePanelComponent)
                    }
                    systemMovableCheckbox.visibility = View.GONE
                    scaleInZCheckBox.visibility = View.GONE
                }
            }
        }

        // Create a spatial panel with all components.
        val everythingPanelView = layoutInflater.inflate(R.layout.input_move_resize_panel, null)
        val everythingPanelEntity = createPanelEntityWithText("Everything", everythingPanelView)
        everythingPanelEntity.parent = movablePanelEntity
        everythingPanelEntity.setPose(Pose(Vector3(0.0f, -0.5f, 0.0f)))
        // Set the everything panel corner radius to 0.
        everythingPanelEntity.cornerRadius = 0.0f

        val everythingPanelSwitch = everythingPanelView.findViewById<MaterialSwitch>(R.id.switch1)
        val everythingPanelInteractableComponent =
            InteractableComponent.create(session!!, executor) {
                Log.i(TAG, "input event $it")
                if (it.action == InputEvent.Action.DOWN) {
                    changeTextAndBGColor(everythingPanelView.findViewById(R.id.textView))
                }
            }
        val everythingPanelMovableComponent = MovableComponent.createSystemMovable(session!!)
        everythingPanelMovableComponent.size = everythingPanelEntity.size.to3d()
        val everythingPanelResizeComponent =
            ResizableComponent.create(
                session!!,
                executor = mainExecutor,
                resizeEventListener = resizeListener,
            )
        everythingPanelResizeComponent.affordanceSize = everythingPanelEntity.size.to3d()
        everythingPanelSwitch.setOnCheckedChangeListener { _, isChecked ->
            everythingPanelMovableComponent.size = everythingPanelEntity.size.to3d()
            everythingPanelResizeComponent.affordanceSize = everythingPanelEntity.size.to3d()
            when (isChecked) {
                true -> {
                    checkNotNull(
                        everythingPanelEntity.addComponent(everythingPanelInteractableComponent)
                    ) {
                        "Component is null"
                    }
                    checkNotNull(
                        everythingPanelEntity.addComponent(everythingPanelMovableComponent)
                    ) {
                        "Component is null"
                    }
                    checkNotNull(
                        everythingPanelEntity.addComponent(everythingPanelResizeComponent)
                    ) {
                        "Component is null"
                    }
                }
                false -> {
                    everythingPanelEntity.removeAllComponents()
                }
            }
        }

        // Create a resizable spatial panel.
        val resizablePanelView = layoutInflater.inflate(R.layout.input_move_resize_panel, null)
        val resizablePanelEntity = createPanelEntityWithText("Resizable", resizablePanelView)
        resizablePanelEntity.setPose(Pose(Vector3(0.9f, 0.2f, -0.1f)))
        resizablePanelEntity.parent = session!!.scene.mainPanelEntity
        // Set the resizable panel corner radius to 0.
        resizablePanelEntity.cornerRadius = 0.0f

        val resizablePanelComponent =
            ResizableComponent.create(
                session!!,
                executor = mainExecutor,
                resizeEventListener = resizeListener,
            )
        resizablePanelComponent.affordanceSize = resizablePanelEntity.size.to3d()

        val anyAspectRatioButton = resizablePanelView.findViewById<RadioButton>(R.id.radioButton1)
        anyAspectRatioButton.text = getString(R.string.any_aspect_ratio_label)
        val portraitAspectRadioButton =
            resizablePanelView.findViewById<RadioButton>(R.id.radioButton2)
        portraitAspectRadioButton.text = getString(R.string.portrait_label)
        val landscapeAspectRadioButton =
            resizablePanelView.findViewById<RadioButton>(R.id.radioButton3)
        landscapeAspectRadioButton.text = getString(R.string.landscape_label)
        val aspectRatioRadioGroup = resizablePanelView.findViewById<RadioGroup>(R.id.radioGroup1)
        aspectRatioRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                // Portrait aspect ratio.
                R.id.radioButton2 -> {
                    val updatedSize =
                        FloatSize2d(
                            resizablePanelEntity.size.height * 0.7f,
                            resizablePanelEntity.size.height,
                        )
                    resizablePanelEntity.size = updatedSize
                    resizablePanelComponent.affordanceSize =
                        FloatSize3d(updatedSize.width, updatedSize.height, 1.0f)
                    resizablePanelComponent.isFixedAspectRatioEnabled = true
                }
                // Landscape aspect ratio.
                R.id.radioButton3 -> {
                    val updatedSize =
                        FloatSize2d(
                            resizablePanelEntity.size.height / 0.7f,
                            resizablePanelEntity.size.height,
                        )
                    resizablePanelEntity.size = updatedSize
                    resizablePanelComponent.affordanceSize =
                        FloatSize3d(updatedSize.width, updatedSize.height, 1.0f)
                    resizablePanelComponent.isFixedAspectRatioEnabled = true
                }
                // No preference on the aspect ratio.
                else -> resizablePanelComponent.isFixedAspectRatioEnabled = false
            }
        }
        resizablePanelComponent.isFixedAspectRatioEnabled = false // no preferences initially

        val resizablePanelSwitch = resizablePanelView.findViewById<MaterialSwitch>(R.id.switch1)
        resizablePanelSwitch.setOnCheckedChangeListener { _, isChecked ->
            resizablePanelComponent.affordanceSize = resizablePanelEntity.size.to3d()
            when (isChecked) {
                true ->
                    resizablePanelActive =
                        resizablePanelEntity.addComponent(resizablePanelComponent)
                false ->
                    if (resizablePanelActive) {
                        resizablePanelEntity.removeComponent(resizablePanelComponent)
                    }
            }
            aspectRatioRadioGroup.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Create a interactable spatial panel.
        val interactablePanelView = layoutInflater.inflate(R.layout.input_move_resize_panel, null)
        val interactablePanelEntity =
            createPanelEntityWithText("Interactable", interactablePanelView)
        interactablePanelEntity.parent = resizablePanelEntity
        interactablePanelEntity.setPose(Pose(Vector3(0f, -0.5f, 0.0f)))
        // Set the interactable panel corner radius to 0.
        interactablePanelEntity.cornerRadius = 0.0f

        val interactablePanelTextView = interactablePanelView.findViewById<TextView>(R.id.textView)
        val interactableComponent =
            InteractableComponent.create(session!!, mainExecutor) {
                Log.i(TAG, "input event $it")
                if (it.action == InputEvent.Action.DOWN) {
                    changeTextAndBGColor(interactablePanelTextView)
                }
            }
        val interactablePanelSwitch =
            interactablePanelView.findViewById<MaterialSwitch>(R.id.switch1)
        interactablePanelSwitch.setOnCheckedChangeListener { _, isChecked ->
            when (isChecked) {
                true ->
                    interactablePanelActive =
                        interactablePanelEntity.addComponent(interactableComponent)
                false ->
                    if (interactablePanelActive) {
                        interactablePanelEntity.removeComponent(interactableComponent)
                    }
            }
        }
    }
}
