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

package androidx.window.demo.embedding

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.WindowSdkExtensions
import androidx.window.demo.R
import androidx.window.demo.common.EdgeToEdgeActivity
import androidx.window.demo.databinding.ActivityOverlayActivityLayoutBinding
import androidx.window.demo.embedding.OverlayActivityBase.OverlayMode.Companion.OVERLAY_MODE_CHANGE_WITH_ORIENTATION
import androidx.window.demo.embedding.OverlayActivityBase.OverlayMode.Companion.OVERLAY_MODE_CUSTOMIZATION
import androidx.window.demo.embedding.OverlayActivityBase.OverlayMode.Companion.OVERLAY_MODE_SIMPLE
import androidx.window.embedding.ActivityEmbeddingController
import androidx.window.embedding.ActivityStack
import androidx.window.embedding.EmbeddingBounds
import androidx.window.embedding.EmbeddingBounds.Alignment.Companion.ALIGN_BOTTOM
import androidx.window.embedding.EmbeddingBounds.Alignment.Companion.ALIGN_LEFT
import androidx.window.embedding.EmbeddingBounds.Alignment.Companion.ALIGN_RIGHT
import androidx.window.embedding.EmbeddingBounds.Alignment.Companion.ALIGN_TOP
import androidx.window.embedding.EmbeddingBounds.Dimension
import androidx.window.embedding.OverlayAttributes
import androidx.window.embedding.OverlayController
import androidx.window.embedding.OverlayCreateParams
import androidx.window.embedding.OverlayInfo
import androidx.window.embedding.SplitController
import androidx.window.embedding.SplitController.SplitSupportStatus.Companion.SPLIT_AVAILABLE
import androidx.window.embedding.setLaunchingActivityStack
import androidx.window.embedding.setOverlayCreateParams
import kotlinx.coroutines.launch

open class OverlayActivityBase :
    EdgeToEdgeActivity(),
    View.OnClickListener,
    RadioGroup.OnCheckedChangeListener,
    AdapterView.OnItemSelectedListener,
    SeekBar.OnSeekBarChangeListener {

    private val overlayTag = OverlayCreateParams.generateOverlayTag()

    private lateinit var splitController: SplitController

    private lateinit var overlayController: OverlayController

    private val demoActivityEmbeddingController = DemoActivityEmbeddingController.getInstance()

    private val extensionVersion = WindowSdkExtensions.getInstance().extensionVersion

    lateinit var viewBinding: ActivityOverlayActivityLayoutBinding

    private var overlayActivityStack: ActivityStack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityOverlayActivityLayoutBinding.inflate(layoutInflater)
        splitController = SplitController.getInstance(this)
        overlayController = OverlayController.getInstance(this)

        if (
            splitController.splitSupportStatus != SPLIT_AVAILABLE ||
                extensionVersion < OVERLAY_FEATURE_MINIMUM_REQUIRED_VERSION
        ) {
            Toast.makeText(this, R.string.toast_show_overlay_warning, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewBinding.root.setBackgroundColor(Color.parseColor("#fff3e0"))
        setContentView(viewBinding.root)

        viewBinding.buttonUpdateOverlayLayout.setOnClickListener(this)
        viewBinding.buttonLaunchOverlayContainer.setOnClickListener(this)
        viewBinding.buttonLaunchOverlayActivityA.setOnClickListener(this)
        viewBinding.buttonLaunchOverlayActivityB.setOnClickListener(this)
        viewBinding.buttonFinishThisActivity.setOnClickListener(this)

        val radioGroupChooseOverlayLayout = viewBinding.radioGroupChooseOverlayLayout
        radioGroupChooseOverlayLayout.setOnCheckedChangeListener(this)

        viewBinding.spinnerAlignment.apply {
            adapter =
                ArrayAdapter(
                    this@OverlayActivityBase,
                    android.R.layout.simple_spinner_dropdown_item,
                    POSITION_TEXT_ARRAY,
                )
            onItemSelectedListener = this@OverlayActivityBase
        }

        val dimensionAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                DIMENSION_TYPE_TEXT_ARRAY,
            )
        viewBinding.spinnerWidth.apply {
            adapter = dimensionAdapter
            onItemSelectedListener = this@OverlayActivityBase
        }

        viewBinding.spinnerHeight.apply {
            adapter = dimensionAdapter
            onItemSelectedListener = this@OverlayActivityBase
        }

        viewBinding.seekBarHeightInRatio.setOnSeekBarChangeListener(this)
        viewBinding.seekBarWidthInRatio.setOnSeekBarChangeListener(this)

        initializeUi()

        lifecycleScope.launch {
            // The block passed to repeatOnLifecycle is executed when the lifecycle
            // is at least STARTED and is cancelled when the lifecycle is STOPPED.
            // It automatically restarts the block when the lifecycle is STARTED again.
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                overlayController.overlayInfo(overlayTag).collect { overlayInfo ->
                    overlayActivityStack = overlayInfo.activityStack
                    val hasOverlay = overlayActivityStack != null
                    viewBinding.buttonUpdateOverlayLayout.isEnabled =
                        hasOverlay &&
                            demoActivityEmbeddingController.overlayMode.get() !=
                                OVERLAY_MODE_CHANGE_WITH_ORIENTATION.value
                    updateOverlayBoundsText(overlayInfo)
                }
            }
        }
    }

    private fun initializeUi() {
        viewBinding.buttonUpdateOverlayLayout.isEnabled = false
        viewBinding.radioGroupChooseOverlayLayout.check(R.id.radioButton_simple_overlay)
        viewBinding.spinnerAlignment.setSelection(ALIGNMENT_VALUE_ARRAY.indexOf(ALIGN_RIGHT))
        viewBinding.spinnerWidth.apply {
            setSelection(INDEX_DIMENSION_RATIO)
            updateDimensionUi(this)
        }
        viewBinding.spinnerHeight.apply {
            setSelection(INDEX_DIMENSION_RATIO)
            updateDimensionUi(this)
        }
    }

    private fun initializeSeekbar(seekBar: SeekBar) {
        seekBar.progress = 50
        updateRatioText(seekBar)
    }

    private fun updateOverlayBoundsText(overlayInfo: OverlayInfo) {
        viewBinding.textViewOverlayBounds.text =
            resources.getString(R.string.overlay_bounds_text) +
                overlayInfo.currentOverlayAttributes?.bounds.toString()
    }

    override fun onClick(button: View) {
        val overlayAttributes = buildOverlayAttributesFromUi()
        val isCustomizationMode =
            demoActivityEmbeddingController.overlayMode.get() == OVERLAY_MODE_CUSTOMIZATION.value
        when (button.id) {
            R.id.button_launch_overlay_container -> {
                if (isCustomizationMode) {
                    // Also update controller's overlayAttributes because the launch bounds are
                    // determined by calculator, which returns the overlayAttributes from
                    // the controller directly.
                    demoActivityEmbeddingController.overlayAttributes = overlayAttributes
                }
                try {
                    startActivity(
                        Intent().apply {
                            setClassName(
                                "androidx.window.demo2",
                                "androidx.window.demo2.embedding.UntrustedEmbeddingActivity"
                            )
                        },
                        ActivityOptions.makeBasic()
                            .toBundle()
                            .setOverlayCreateParams(
                                this,
                                OverlayCreateParams.Builder()
                                    .setTag(overlayTag)
                                    .setOverlayAttributes(
                                        if (isCustomizationMode) {
                                            overlayAttributes
                                        } else {
                                            DEFAULT_OVERLAY_ATTRIBUTES
                                        }
                                    )
                                    .build()
                            )
                    )
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, R.string.install_samples_2, Toast.LENGTH_LONG).show()
                }
            }
            R.id.button_launch_overlay_activity_a ->
                startActivity(
                    Intent(this, OverlayAssociatedActivityA::class.java).apply {
                        if (viewBinding.checkboxReorderToFront.isChecked) {
                            flags = FLAG_ACTIVITY_REORDER_TO_FRONT
                        }
                    }
                )
            R.id.button_launch_overlay_activity_b ->
                startActivity(
                    Intent(this, OverlayAssociatedActivityB::class.java),
                    overlayActivityStack?.let {
                        if (viewBinding.checkboxLaunchToOverlay.isChecked) {
                            ActivityOptions.makeBasic()
                                .toBundle()
                                .setLaunchingActivityStack(
                                    this,
                                    it,
                                )
                        } else {
                            null
                        }
                    }
                )
            R.id.button_finish_this_activity -> finish()
            R.id.button_update_overlay_layout -> {
                if (isCustomizationMode) {
                    demoActivityEmbeddingController.overlayAttributes = overlayAttributes
                    ActivityEmbeddingController.getInstance(this).invalidateVisibleActivityStacks()
                } else {
                    overlayController.updateOverlayAttributes(overlayTag, overlayAttributes)
                }
            }
        }
    }

    private fun buildOverlayAttributesFromUi(): OverlayAttributes {
        val spinnerPosition = viewBinding.spinnerAlignment
        val spinnerWidth = viewBinding.spinnerWidth
        val spinnerHeight = viewBinding.spinnerHeight

        return OverlayAttributes.Builder()
            .setBounds(
                EmbeddingBounds(
                    ALIGNMENT_VALUE_ARRAY[spinnerPosition.selectedItemPosition],
                    createDimensionFromUi(spinnerWidth),
                    createDimensionFromUi(spinnerHeight),
                )
            )
            .build()
    }

    private fun createDimensionFromUi(spinner: Spinner): Dimension =
        when (val position = spinner.selectedItemPosition) {
            INDEX_DIMENSION_EXPAND -> Dimension.DIMENSION_EXPANDED
            INDEX_DIMENSION_HINGE -> Dimension.DIMENSION_HINGE
            INDEX_DIMENSION_RATIO ->
                Dimension.ratio(
                    if (spinner.isSpinnerWidth()) {
                        viewBinding.seekBarWidthInRatio.progress.toFloat() / 100
                    } else {
                        viewBinding.seekBarHeightInRatio.progress.toFloat() / 100
                    }
                )
            INDEX_DIMENSION_PIXEL ->
                Dimension.pixel(
                    if (spinner.isSpinnerWidth()) {
                        viewBinding.editTextNumberDecimalWidthInPixel.text.toString().toInt()
                    } else {
                        viewBinding.editTextNumberDecimalHeightInPixel.text.toString().toInt()
                    }
                )
            else -> throw IllegalStateException("Unknown spinner index: $position")
        }

    override fun onCheckedChanged(group: RadioGroup, id: Int) {
        demoActivityEmbeddingController.overlayMode.set(
            when (id) {
                R.id.radioButton_simple_overlay -> OVERLAY_MODE_SIMPLE.value
                R.id.radioButton_change_with_orientation ->
                    OVERLAY_MODE_CHANGE_WITH_ORIENTATION.value
                R.id.radioButton_customization -> OVERLAY_MODE_CUSTOMIZATION.value
                else -> throw IllegalArgumentException("Unrecognized id $id")
            }
        )
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (
            parent is Spinner &&
                parent in arrayOf(viewBinding.spinnerWidth, viewBinding.spinnerHeight)
        ) {
            updateDimensionUi(parent)
        }
    }

    private fun updateDimensionUi(spinner: Spinner) {
        val textViewRatio =
            if (spinner.isSpinnerWidth()) {
                viewBinding.textViewWidthInRatio
            } else {
                viewBinding.textViewHeightInRatio
            }
        val seekBarRatio =
            if (spinner.isSpinnerWidth()) {
                viewBinding.seekBarWidthInRatio
            } else {
                viewBinding.seekBarHeightInRatio
            }
        val textViewPixel =
            if (spinner.isSpinnerWidth()) {
                viewBinding.textViewWidthInPixel
            } else {
                viewBinding.textViewHeightInPixel
            }
        val editTextPixel =
            if (spinner.isSpinnerWidth()) {
                viewBinding.editTextNumberDecimalWidthInPixel
            } else {
                viewBinding.editTextNumberDecimalHeightInPixel
            }
        when (spinner.selectedItemPosition) {
            INDEX_DIMENSION_EXPAND,
            INDEX_DIMENSION_HINGE -> {
                textViewRatio.visibility = View.GONE
                seekBarRatio.visibility = View.GONE
                textViewPixel.visibility = View.GONE
                editTextPixel.visibility = View.GONE
            }
            INDEX_DIMENSION_RATIO -> {
                textViewRatio.visibility = View.VISIBLE
                seekBarRatio.visibility = View.VISIBLE
                textViewPixel.visibility = View.GONE
                editTextPixel.visibility = View.GONE
                initializeSeekbar(seekBarRatio)
            }
            INDEX_DIMENSION_PIXEL -> {
                textViewRatio.visibility = View.GONE
                seekBarRatio.visibility = View.GONE
                textViewPixel.visibility = View.VISIBLE
                editTextPixel.visibility = View.VISIBLE
                editTextPixel.text.clear()
            }
        }
    }

    private fun Spinner.isSpinnerWidth() = this == viewBinding.spinnerWidth

    override fun onNothingSelected(view: AdapterView<*>?) {
        // Auto-generated method stub
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        updateRatioText(seekBar)
    }

    private fun updateRatioText(seekBar: SeekBar) {
        if (seekBar.isSeekBarWidthInRatio()) {
            viewBinding.textViewWidthInRatio.text =
                resources.getString(R.string.width_in_ratio) +
                    (seekBar.progress.toFloat() / 100).toString()
        } else {
            viewBinding.textViewHeightInRatio.text =
                resources.getString(R.string.height_in_ratio) +
                    (seekBar.progress.toFloat() / 100).toString()
        }
    }

    private fun SeekBar.isSeekBarWidthInRatio(): Boolean = this == viewBinding.seekBarWidthInRatio

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        // Auto-generated method stub
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        // Auto-generated method stub
    }

    @JvmInline
    internal value class OverlayMode(val value: Int) {
        companion object {
            val OVERLAY_MODE_SIMPLE = OverlayMode(0)
            val OVERLAY_MODE_CHANGE_WITH_ORIENTATION = OverlayMode(1)
            val OVERLAY_MODE_CUSTOMIZATION = OverlayMode(2)
        }
    }

    companion object {
        internal const val OVERLAY_FEATURE_MINIMUM_REQUIRED_VERSION = 8

        internal val DEFAULT_OVERLAY_ATTRIBUTES =
            OverlayAttributes(
                EmbeddingBounds(
                    ALIGN_RIGHT,
                    Dimension.ratio(0.5f),
                    Dimension.ratio(0.8f),
                )
            )

        private val POSITION_TEXT_ARRAY = arrayOf("top", "left", "bottom", "right")
        private val ALIGNMENT_VALUE_ARRAY =
            arrayListOf(
                ALIGN_TOP,
                ALIGN_LEFT,
                ALIGN_BOTTOM,
                ALIGN_RIGHT,
            )

        private val DIMENSION_TYPE_TEXT_ARRAY =
            arrayOf(
                "expand to the task",
                "follow the hinge",
                "dimension in ratio",
                "dimension in pixel",
            )
        private const val INDEX_DIMENSION_EXPAND = 0
        private const val INDEX_DIMENSION_HINGE = 1
        private const val INDEX_DIMENSION_RATIO = 2
        private const val INDEX_DIMENSION_PIXEL = 3
    }
}
