/*
 * Copyright 2023 The Android Open Source Project
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

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.WindowSdkExtensions
import androidx.window.core.ExperimentalWindowApi
import androidx.window.demo.R
import androidx.window.demo.databinding.ActivitySplitAttributesToggleSecondaryActivityBinding
import androidx.window.demo.embedding.SplitDeviceStateActivityBase.Companion.SUFFIX_AND_HORIZONTAL_LAYOUT_IN_TABLETOP
import androidx.window.demo.embedding.SplitDeviceStateActivityBase.Companion.TAG_SHOW_FULLSCREEN_IN_PORTRAIT
import androidx.window.embedding.ActivityEmbeddingController
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_EXPAND
import androidx.window.embedding.SplitInfo
import androidx.window.embedding.SplitPlaceholderRule
import androidx.window.embedding.SplitRule
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalWindowApi::class)
open class SplitAttributesToggleSecondaryActivity : SplitAttributesToggleActivityBase(),
    View.OnClickListener, AdapterView.OnItemSelectedListener {

    protected lateinit var viewBinding: ActivitySplitAttributesToggleSecondaryActivityBinding

    private var lastSplitInfo: SplitInfo? = null

    private val demoActivityEmbeddingController = DemoActivityEmbeddingController.getInstance()
    private lateinit var activityEmbeddingController: ActivityEmbeddingController
    private var splitAttributesUpdatesSupported: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySplitAttributesToggleSecondaryActivityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        val fullscreenToggleButton = viewBinding.fullscreenToggleButton

        viewBinding.rootSplitActivityLayout.setBackgroundColor(Color.parseColor("#fff3e0"))

        // Disable fullscreen mode in case it's enabled by other activities.
        demoActivityEmbeddingController.shouldExpandSecondaryContainer.set(false)

        activityEmbeddingController = ActivityEmbeddingController.getInstance(this)
        val splitAttributesCustomizationEnabled = demoActivityEmbeddingController
            .splitAttributesCustomizationEnabled.get()
        splitAttributesUpdatesSupported = WindowSdkExtensions.getInstance().extensionVersion >= 3 &&
            !splitAttributesCustomizationEnabled

        fullscreenToggleButton.apply {
            // Disable the toggle fullscreen feature if the device doesn't support runtime
            // SplitAttributes API or split attributes customization is enabled
            if (!splitAttributesUpdatesSupported) {
                isEnabled = false
            } else {
                setOnClickListener(this@SplitAttributesToggleSecondaryActivity)
            }
        }

        if (demoActivityEmbeddingController.splitAttributesCustomizationEnabled.get()) {
            val splitTypeSpinner = viewBinding.splitTypeSpinner
            splitTypeSpinner.visibility = View.VISIBLE
            splitTypeSpinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                CUSTOMIZED_SPLIT_TYPES_TEXT,
            )
            splitTypeSpinner.onItemSelectedListener = this

            val layoutDirectionSpinner = viewBinding.layoutDirectionSpinner
            layoutDirectionSpinner.visibility = View.VISIBLE
            layoutDirectionSpinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                CUSTOMIZED_LAYOUT_DIRECTIONS_TEXT,
            )
            layoutDirectionSpinner.onItemSelectedListener = this
        }

        lifecycleScope.launch {
            // The block passed to repeatOnLifecycle is executed when the lifecycle
            // is at least STARTED and is cancelled when the lifecycle is STOPPED.
            // It automatically restarts the block when the lifecycle is STARTED again.
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                splitController.splitInfoList(this@SplitAttributesToggleSecondaryActivity)
                    .onEach {
                        updateWarningMessages()
                        updateSpinnerFromDemoController()
                    }
                    .collect { splitInfoList ->
                        // Empty split info means this activity doesn't participate any split rule:
                        // neither the secondary activity of the split pair rule nor the placeholder
                        // activity of placeholder rule.
                        fullscreenToggleButton.isEnabled = splitAttributesUpdatesSupported &&
                            splitInfoList.isNotEmpty()

                        lastSplitInfo = if (splitInfoList.isEmpty()) {
                            null
                        } else {
                            splitInfoList.last()
                        }
                    }
            }
        }
    }

    private fun updateSpinnerFromDemoController() {
        viewBinding.splitTypeSpinner.setSelection(
            CUSTOMIZED_SPLIT_TYPES_VALUE.indexOf(
                demoActivityEmbeddingController.customizedSplitType
            )
        )
        viewBinding.layoutDirectionSpinner.setSelection(
            CUSTOMIZED_LAYOUT_DIRECTIONS_VALUE.indexOf(
                demoActivityEmbeddingController.customizedLayoutDirection
            )
        )
    }

    private fun updateWarningMessages() {
        val warningMessages = StringBuilder().apply {
            if (!splitAttributesUpdatesSupported) {
                append("Toggling fullscreen mode is not supported on this device!\n")
            }
            val splitPlaceholderRule = getSplitRule<SplitPlaceholderRule>()
            if (splitPlaceholderRule?.isSticky == false) {
                append("Placeholder activity may show again " +
                    "when clicking \"TOGGLE FULLSCREEN MODE\". " +
                    "Clear the placeholder rule and launch Activity again " +
                    "to remove placeholder rule.\n")
            }
        }
        viewBinding.warningMessageTextView.text = warningMessages
    }

    override fun onClick(button: View) {
        when (button.id) {
            R.id.fullscreen_toggle_button -> {
                val splitRule = getSplitRule<SplitRule>() ?: return
                // Toggle the fullscreen mode and trigger SplitAttributes calculation if
                // the splitAttributes is customized.
                if (splitRule.tag?.contains(TAG_SHOW_FULLSCREEN_IN_PORTRAIT +
                    SUFFIX_AND_HORIZONTAL_LAYOUT_IN_TABLETOP) == true
                ) {
                    val enableFullscreenMode = DemoActivityEmbeddingController.getInstance()
                        .shouldExpandSecondaryContainer
                    enableFullscreenMode.set(!enableFullscreenMode.get())
                    splitController.invalidateTopVisibleSplitAttributes()
                } else {
                    // Update the top splitInfo if single default split Attributes is used.
                    splitController.updateSplitAttributes(
                        splitInfo = lastSplitInfo ?: return,
                        splitAttributes = if (
                            lastSplitInfo!!.splitAttributes.splitType == SPLIT_TYPE_EXPAND
                        ) {
                            splitRule.defaultSplitAttributes
                        } else {
                            EXPAND_ATTRS
                        }
                    )
                }
            }
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (parent?.id) {
            R.id.split_type_spinner ->
                demoActivityEmbeddingController.customizedSplitType =
                    CUSTOMIZED_SPLIT_TYPES_VALUE[position]
            R.id.layout_direction_spinner ->
                demoActivityEmbeddingController.customizedLayoutDirection =
                    CUSTOMIZED_LAYOUT_DIRECTIONS_VALUE[position]
        }
        splitController.invalidateTopVisibleSplitAttributes()
    }

    override fun onNothingSelected(view: AdapterView<*>?) {
        // Auto-generated method stub
    }
}
