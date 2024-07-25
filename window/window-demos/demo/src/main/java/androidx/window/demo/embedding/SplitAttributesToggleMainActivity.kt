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

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.RadioGroup
import androidx.collection.ArraySet
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.WindowSdkExtensions
import androidx.window.core.ExperimentalWindowApi
import androidx.window.demo.R
import androidx.window.demo.databinding.ActivitySplitAttributesTogglePrimaryActivityBinding
import androidx.window.demo.embedding.SplitDeviceStateActivityBase.Companion.SUFFIX_AND_HORIZONTAL_LAYOUT_IN_TABLETOP
import androidx.window.demo.embedding.SplitDeviceStateActivityBase.Companion.TAG_SHOW_FULLSCREEN_IN_PORTRAIT
import androidx.window.demo.embedding.SplitDeviceStateActivityBase.Companion.TAG_USE_DEFAULT_SPLIT_ATTRIBUTES
import androidx.window.embedding.ActivityEmbeddingController
import androidx.window.embedding.ActivityFilter
import androidx.window.embedding.EmbeddingRule
import androidx.window.embedding.SplitPairFilter
import androidx.window.embedding.SplitPairRule
import androidx.window.embedding.SplitPlaceholderRule
import androidx.window.embedding.SplitRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalWindowApi::class)
open class SplitAttributesToggleMainActivity :
    SplitAttributesToggleActivityBase(),
    View.OnClickListener,
    RadioGroup.OnCheckedChangeListener,
    AdapterView.OnItemSelectedListener,
    CompoundButton.OnCheckedChangeListener {

    protected lateinit var viewBinding: ActivitySplitAttributesTogglePrimaryActivityBinding
    private val pendingRules = ArraySet<EmbeddingRule>()

    internal lateinit var activityEmbeddingController: ActivityEmbeddingController

    private val demoActivityEmbeddingController = DemoActivityEmbeddingController.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivitySplitAttributesTogglePrimaryActivityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val placeholderFoldingAwareAttrsRadioButton =
            viewBinding.placeholderUseFoldingAwareSplitAttributes
        val splitRuleFoldingAwareAttrsRadioButton =
            viewBinding.splitRuleUseFoldingAwareSplitAttributes

        activityEmbeddingController = ActivityEmbeddingController.getInstance(this)

        if (WindowSdkExtensions.getInstance().extensionVersion < 2) {
            placeholderFoldingAwareAttrsRadioButton.isEnabled = false
            viewBinding.placeholderUseCustomizedSplitAttributes.isEnabled = false
            splitRuleFoldingAwareAttrsRadioButton.isEnabled = false
            viewBinding.splitRuleUseCustomizedSplitAttributes.isEnabled = false
        }

        viewBinding.startPrimaryActivityButton.setOnClickListener(this)
        viewBinding.useStickyPlaceholderCheckBox.setOnCheckedChangeListener(this)
        viewBinding.usePlaceholderCheckBox.setOnCheckedChangeListener(this)
        viewBinding.placeholderSplitLayoutOption.setOnCheckedChangeListener(this)
        val placeholderSplitTypeSpinner = viewBinding.placeholderSplitTypeSpinner
        placeholderSplitTypeSpinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                CUSTOMIZED_SPLIT_TYPES_TEXT,
            )
        placeholderSplitTypeSpinner.onItemSelectedListener = this
        val placeholderLayoutDirectionSpinner = viewBinding.placeholderLayoutDirectionSpinner
        placeholderLayoutDirectionSpinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                CUSTOMIZED_LAYOUT_DIRECTIONS_TEXT,
            )
        placeholderLayoutDirectionSpinner.onItemSelectedListener = this

        viewBinding.startActivityPairButton.setOnClickListener(this)
        viewBinding.useSplitRuleCheckBox.setOnCheckedChangeListener(this)
        viewBinding.splitRuleSplitLayoutOption.setOnCheckedChangeListener(this)
        val splitRuleSplitTypeSpinner = viewBinding.splitRuleSplitTypeSpinner
        splitRuleSplitTypeSpinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                CUSTOMIZED_SPLIT_TYPES_TEXT,
            )
        splitRuleSplitTypeSpinner.onItemSelectedListener = this
        val splitRuleLayoutDirectionSpinner = viewBinding.splitRuleLayoutDirectionSpinner
        splitRuleLayoutDirectionSpinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                CUSTOMIZED_LAYOUT_DIRECTIONS_TEXT,
            )
        splitRuleLayoutDirectionSpinner.onItemSelectedListener = this

        lifecycleScope.launch {
            // The block passed to repeatOnLifecycle is executed when the lifecycle
            // is at least STARTED and is cancelled when the lifecycle is STOPPED.
            // It automatically restarts the block when the lifecycle is STARTED again.
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                splitController.splitInfoList(this@SplitAttributesToggleMainActivity).collect {
                    updateUiFromRules()
                }
            }
        }
    }

    open suspend fun updateUiFromRules() {
        withContext(Dispatchers.Main) {
            updateWarningMessages()
            ruleController.getRules().apply {
                val splitPlaceholderRule = getSplitRule<SplitPlaceholderRule>()

                val hasPlaceholderRule = splitPlaceholderRule != null
                val usePlaceholderCheckBox = viewBinding.usePlaceholderCheckBox

                var shouldShowSpinner = false

                usePlaceholderCheckBox.isChecked = hasPlaceholderRule
                onCheckedChanged(usePlaceholderCheckBox, usePlaceholderCheckBox.isChecked)
                viewBinding.useStickyPlaceholderCheckBox.isChecked =
                    splitPlaceholderRule?.isSticky ?: false
                viewBinding.placeholderSplitLayoutOption.check(
                    when (
                        splitPlaceholderRule
                            ?.tag
                            ?.removePrefix(PREFIX_FULLSCREEN_TOGGLE)
                            ?.removePrefix(PREFIX_PLACEHOLDER)
                    ) {
                        TAG_USE_DEFAULT_SPLIT_ATTRIBUTES ->
                            R.id.placeholder_use_default_split_attributes
                        TAG_SHOW_FULLSCREEN_IN_PORTRAIT +
                            SUFFIX_AND_HORIZONTAL_LAYOUT_IN_TABLETOP ->
                            R.id.placeholder_use_folding_aware_split_attributes
                        TAG_CUSTOMIZED_SPLIT_ATTRIBUTES ->
                            R.id.placeholder_use_customized_split_attributes
                        else -> 0
                    }
                )
                if (
                    viewBinding.placeholderSplitLayoutOption.checkedRadioButtonId ==
                        R.id.placeholder_use_customized_split_attributes
                ) {
                    viewBinding.placeholderSplitTypeSpinner.setSelection(
                        CUSTOMIZED_SPLIT_TYPES_VALUE.indexOf(
                            demoActivityEmbeddingController.customizedSplitType
                        )
                    )
                    viewBinding.placeholderSplitTypeSpinner.setSelection(
                        CUSTOMIZED_LAYOUT_DIRECTIONS_VALUE.indexOf(
                            demoActivityEmbeddingController.customizedLayoutDirection
                        )
                    )
                    shouldShowSpinner = true
                }

                val splitPairRule = getSplitRule<SplitPairRule>()
                val useSplitRuleCheckBox = viewBinding.useSplitRuleCheckBox
                useSplitRuleCheckBox.isChecked = splitPairRule != null
                onCheckedChanged(useSplitRuleCheckBox, useSplitRuleCheckBox.isChecked)
                viewBinding.splitRuleSplitLayoutOption.check(
                    when (
                        splitPairRule
                            ?.tag
                            ?.removePrefix(PREFIX_FULLSCREEN_TOGGLE)
                            ?.removePrefix(PREFIX_PLACEHOLDER)
                    ) {
                        TAG_USE_DEFAULT_SPLIT_ATTRIBUTES ->
                            R.id.split_rule_use_single_split_attributes
                        TAG_SHOW_FULLSCREEN_IN_PORTRAIT +
                            SUFFIX_AND_HORIZONTAL_LAYOUT_IN_TABLETOP ->
                            R.id.split_rule_use_folding_aware_split_attributes
                        TAG_CUSTOMIZED_SPLIT_ATTRIBUTES ->
                            R.id.split_rule_use_customized_split_attributes
                        else -> 0
                    }
                )
                if (
                    viewBinding.splitRuleSplitLayoutOption.checkedRadioButtonId ==
                        R.id.split_rule_use_customized_split_attributes
                ) {
                    viewBinding.splitRuleSplitTypeSpinner.setSelection(
                        CUSTOMIZED_SPLIT_TYPES_VALUE.indexOf(
                            demoActivityEmbeddingController.customizedSplitType
                        )
                    )
                    viewBinding.splitRuleLayoutDirectionSpinner.setSelection(
                        CUSTOMIZED_LAYOUT_DIRECTIONS_VALUE.indexOf(
                            demoActivityEmbeddingController.customizedLayoutDirection
                        )
                    )
                    shouldShowSpinner = true
                }
                demoActivityEmbeddingController.splitAttributesCustomizationEnabled.set(
                    shouldShowSpinner
                )
            }
        }
    }

    private suspend fun updateWarningMessages() {
        val warningMessages =
            StringBuilder().apply {
                val apiLevel = WindowSdkExtensions.getInstance().extensionVersion

                if (apiLevel < 2) {
                    append(resources.getString(R.string.split_attributes_calculator_not_supported))
                    append("\n")
                }
                if (apiLevel < 3) {
                    append("Finishing secondary activities is not supported on this device!\n")
                }
                if (
                    viewBinding.finishSecondaryActivitiesButton.isEnabled &&
                        getSplitRule<SplitPlaceholderRule>() != null
                ) {
                    append(resources.getString(R.string.show_placeholder_warning))
                    append("\n")
                }
            }
        withContext(Dispatchers.Main) { viewBinding.warningMessageTextView.text = warningMessages }
    }

    override fun onClick(button: View) {
        // Update the rules to ruleController before starting the Activity.
        applyRules()
        // Set the status to the default.
        demoActivityEmbeddingController.shouldExpandSecondaryContainer.set(false)
        val intent = Intent(this, SplitAttributesTogglePrimaryActivity::class.java)
        when (button.id) {
            R.id.start_primary_activity_button -> startActivity(intent)
            R.id.start_activity_pair_button -> {
                startActivity(intent.putExtra(EXTRA_LAUNCH_SECONDARY, true))
            }
        }
    }

    open fun applyRules() {
        // Only remove rules with tag specified
        ruleController
            .getRules()
            .filter { rule -> rule.tag?.contains(PREFIX_FULLSCREEN_TOGGLE) ?: false }
            .forEach { rule -> ruleController.removeRule(rule) }

        for (rule in pendingRules) {
            ruleController.addRule(rule)
        }
    }

    override fun onCheckedChanged(c: CompoundButton, isChecked: Boolean) {
        val checkBoxId = c.id
        if (isChecked) {
            showSubItems(checkBoxId)
        } else {
            hideSubItems(checkBoxId)
        }
        updatePendingRulesFromUi()
    }

    private fun showSubItems(checkBoxId: Int) {
        when (checkBoxId) {
            R.id.use_placeholder_check_box -> {
                viewBinding.placeholderChooseLayoutTextView.visibility = View.VISIBLE
                viewBinding.placeholderSplitLayoutOption.visibility = View.VISIBLE
                viewBinding.placeholderUseDefaultSplitAttributes.isChecked = true
                viewBinding.useStickyPlaceholderCheckBox.visibility = View.VISIBLE
            }
            R.id.use_split_rule_check_box -> {
                viewBinding.splitRuleChooseLayoutTextView.visibility = View.VISIBLE
                viewBinding.splitRuleSplitLayoutOption.visibility = View.VISIBLE
                viewBinding.splitRuleUseSingleSplitAttributes.isChecked = true
            }
        }
    }

    private fun hideSubItems(checkBoxId: Int) {
        when (checkBoxId) {
            R.id.use_placeholder_check_box -> {
                viewBinding.placeholderChooseLayoutTextView.visibility = View.GONE
                viewBinding.placeholderSplitLayoutOption.visibility = View.GONE
                viewBinding.useStickyPlaceholderCheckBox.visibility = View.GONE
            }
            R.id.use_split_rule_check_box -> {
                viewBinding.splitRuleChooseLayoutTextView.visibility = View.GONE
                viewBinding.splitRuleSplitLayoutOption.visibility = View.GONE
            }
        }
    }

    override fun onCheckedChanged(group: RadioGroup, id: Int) {
        updatePendingRulesFromUi()
        updateSpinnerVisibilitiesIfNeeded(id)
    }

    private fun updatePendingRulesFromUi() {
        pendingRules.clear()
        if (viewBinding.usePlaceholderCheckBox.isChecked) {
            // Create the SplitPlaceholderRule
            val placeholderFilter =
                ActivityFilter(
                    ComponentName(
                        this@SplitAttributesToggleMainActivity,
                        SplitAttributesTogglePrimaryActivity::class.java
                    ),
                    intentAction = null,
                )
            val placeholderIntent =
                Intent(
                    this@SplitAttributesToggleMainActivity,
                    SplitAttributesTogglePlaceholderActivity::class.java
                )
            val splitPlaceholderRule =
                SplitPlaceholderRule.Builder(setOf(placeholderFilter), placeholderIntent)
                    .setFinishPrimaryWithPlaceholder(SplitRule.FinishBehavior.ALWAYS)
                    .setTag(
                        PREFIX_FULLSCREEN_TOGGLE +
                            PREFIX_PLACEHOLDER +
                            when (viewBinding.placeholderSplitLayoutOption.checkedRadioButtonId) {
                                R.id.placeholder_use_default_split_attributes ->
                                    TAG_USE_DEFAULT_SPLIT_ATTRIBUTES
                                R.id.placeholder_use_folding_aware_split_attributes ->
                                    TAG_SHOW_FULLSCREEN_IN_PORTRAIT +
                                        SUFFIX_AND_HORIZONTAL_LAYOUT_IN_TABLETOP
                                R.id.placeholder_use_customized_split_attributes ->
                                    TAG_CUSTOMIZED_SPLIT_ATTRIBUTES
                                else -> null
                            }
                    )
                    .setSticky(viewBinding.useStickyPlaceholderCheckBox.isChecked)
                    .build()
            pendingRules.add(splitPlaceholderRule)
        }
        if (viewBinding.useSplitRuleCheckBox.isChecked) {
            // Create the SplitPairRule
            val splitActivityFilter =
                SplitPairFilter(
                    ComponentName(
                        this@SplitAttributesToggleMainActivity,
                        SplitAttributesTogglePrimaryActivity::class.java
                    ),
                    ComponentName(
                        this@SplitAttributesToggleMainActivity,
                        SplitAttributesToggleSecondaryActivity::class.java
                    ),
                    secondaryActivityIntentAction = null,
                )
            val splitPairRule =
                SplitPairRule.Builder(setOf(splitActivityFilter))
                    .setFinishPrimaryWithSecondary(SplitRule.FinishBehavior.ALWAYS)
                    .setFinishSecondaryWithPrimary(SplitRule.FinishBehavior.ALWAYS)
                    .setTag(
                        PREFIX_FULLSCREEN_TOGGLE +
                            when (viewBinding.splitRuleSplitLayoutOption.checkedRadioButtonId) {
                                R.id.split_rule_use_single_split_attributes ->
                                    TAG_USE_DEFAULT_SPLIT_ATTRIBUTES
                                R.id.split_rule_use_folding_aware_split_attributes ->
                                    TAG_SHOW_FULLSCREEN_IN_PORTRAIT +
                                        SUFFIX_AND_HORIZONTAL_LAYOUT_IN_TABLETOP
                                R.id.split_rule_use_customized_split_attributes ->
                                    TAG_CUSTOMIZED_SPLIT_ATTRIBUTES
                                else -> null
                            }
                    )
                    .build()
            pendingRules.add(splitPairRule)
        }
    }

    private fun updateSpinnerVisibilitiesIfNeeded(id: Int) {
        when (id) {
            R.id.placeholder_use_customized_split_attributes,
            R.id.split_rule_use_customized_split_attributes ->
                updateSpinnerVisibility(id, View.VISIBLE)
            else -> {
                updateSpinnerVisibility(R.id.placeholder_use_customized_split_attributes, View.GONE)
                updateSpinnerVisibility(R.id.split_rule_use_customized_split_attributes, View.GONE)
            }
        }
    }

    private fun updateSpinnerVisibility(id: Int, visibility: Int) {
        demoActivityEmbeddingController.splitAttributesCustomizationEnabled.set(
            visibility == View.VISIBLE
        )
        when (id) {
            R.id.placeholder_use_customized_split_attributes -> {
                viewBinding.placeholderSplitTypeTextView.visibility = visibility
                viewBinding.placeholderSplitTypeSpinner.visibility = visibility
                viewBinding.placeholderLayoutDirectionTextView.visibility = visibility
                viewBinding.placeholderLayoutDirectionSpinner.visibility = visibility
            }
            R.id.split_rule_use_customized_split_attributes -> {
                viewBinding.splitRuleSplitTypeTextView.visibility = visibility
                viewBinding.splitRuleSplitTypeSpinner.visibility = visibility
                viewBinding.splitRuleLayoutDirectionTextView.visibility = visibility
                viewBinding.splitRuleLayoutDirectionSpinner.visibility = visibility
            }
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (parent?.id) {
            R.id.placeholder_split_type_spinner,
            R.id.split_rule_split_type_spinner ->
                demoActivityEmbeddingController.customizedSplitType =
                    CUSTOMIZED_SPLIT_TYPES_VALUE[position]
            R.id.placeholder_layout_direction_spinner,
            R.id.split_rule_layout_direction_spinner ->
                demoActivityEmbeddingController.customizedLayoutDirection =
                    CUSTOMIZED_LAYOUT_DIRECTIONS_VALUE[position]
            R.id.animation_background_dropdown ->
                demoActivityEmbeddingController.animationBackground =
                    DemoActivityEmbeddingController.ANIMATION_BACKGROUND_VALUES[position]
        }
        activityEmbeddingController.invalidateVisibleActivityStacks()
    }

    override fun onNothingSelected(view: AdapterView<*>?) {
        // Auto-generated method stub
    }

    companion object {
        internal const val PREFIX_FULLSCREEN_TOGGLE = "fullscreen_toggle_"
        internal const val PREFIX_PLACEHOLDER = "placeholder_"
        internal const val EXTRA_LAUNCH_SECONDARY = "launch_secondary"
        internal const val TAG_CUSTOMIZED_SPLIT_ATTRIBUTES = "customized_split_attributes"
    }
}
