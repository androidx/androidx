/*
 * Copyright 2022 The Android Open Source Project
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
import android.widget.CompoundButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.core.ExperimentalWindowApi
import androidx.window.demo.R
import androidx.window.demo.databinding.ActivitySplitDeviceStateLayoutBinding
import androidx.window.embedding.EmbeddingRule
import androidx.window.embedding.RuleController
import androidx.window.embedding.SplitAttributes
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_EQUAL
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_EXPAND
import androidx.window.embedding.SplitController
import androidx.window.embedding.SplitController.SplitSupportStatus.Companion.SPLIT_AVAILABLE
import androidx.window.embedding.SplitInfo
import androidx.window.embedding.SplitPairFilter
import androidx.window.embedding.SplitPairRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalWindowApi::class)
open class SplitDeviceStateActivityBase : AppCompatActivity(), View.OnClickListener,
    RadioGroup.OnCheckedChangeListener, CompoundButton.OnCheckedChangeListener,
    AdapterView.OnItemSelectedListener {

    private lateinit var splitController: SplitController
    private lateinit var ruleController: RuleController

    private lateinit var splitPairRule: SplitPairRule
    private var shouldReverseContainerPosition = false
    private var shouldShowHorizontalInTabletop = false
    private var shouldShowFullscreenInBookMode = false

    private lateinit var viewBinding: ActivitySplitDeviceStateLayoutBinding
    private lateinit var activityA: ComponentName
    private lateinit var activityB: ComponentName

    /** The last selected split rule id. */
    private var lastCheckedRuleId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySplitDeviceStateLayoutBinding.inflate(layoutInflater)
        splitController = SplitController.getInstance(this)
        if (splitController.splitSupportStatus != SPLIT_AVAILABLE) {
            Toast.makeText(
                this, R.string.toast_split_not_support,
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }
        ruleController = RuleController.getInstance(this)

        setContentView(viewBinding.root)

        activityA = ComponentName(this, SplitDeviceStateActivityA::class.java.name)
        activityB = ComponentName(this, SplitDeviceStateActivityB::class.java.name)

        val radioGroup = viewBinding.splitAttributesOptionsRadioGroup
        if (componentName == activityA) {
            // Set to the first option
            radioGroup.check(R.id.use_default_split_attributes)
            onCheckedChanged(radioGroup, radioGroup.checkedRadioButtonId)
            radioGroup.setOnCheckedChangeListener(this)
        } else {
            // Only update split pair rule on the primary Activity. The secondary Activity can only
            // finish itself to prevent confusing users. We only apply the rule when the Activity is
            // launched from the primary.
            viewBinding.chooseLayoutTextView.visibility = View.GONE
            radioGroup.visibility = View.GONE
            viewBinding.launchActivityToSide.text = "Finish this Activity"
        }

        viewBinding.showHorizontalLayoutInTabletopCheckBox.setOnCheckedChangeListener(this)
        viewBinding.showFullscreenInBookModeCheckBox.setOnCheckedChangeListener(this)
        viewBinding.swapPrimarySecondaryPositionCheckBox.setOnCheckedChangeListener(this)
        viewBinding.launchActivityToSide.setOnClickListener(this)

        val isCallbackSupported = splitController.isSplitAttributesCalculatorSupported()
        if (!isCallbackSupported) {
            // Disable the radioButtons that use SplitAttributesCalculator
            viewBinding.showFullscreenInPortraitRadioButton.isEnabled = false
            viewBinding.showHorizontalLayoutInTabletopRadioButton.isEnabled = false
            viewBinding.showDifferentLayoutWithSizeRadioButton.isEnabled = false
            viewBinding.splitByHingeWhenSeparatingRadioButton.isEnabled = false
            hideAllSubCheckBoxes()
            // Add the error message to notify the SplitAttributesCalculator is not available.
            viewBinding.errorMessageTextView.text = "SplitAttributesCalculator is not supported!"
        }

        lifecycleScope.launch {
            // The block passed to repeatOnLifecycle is executed when the lifecycle
            // is at least STARTED and is cancelled when the lifecycle is STOPPED.
            // It automatically restarts the block when the lifecycle is STARTED again.
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                splitController.splitInfoList(this@SplitDeviceStateActivityBase)
                    .collect { newSplitInfos ->
                        updateSplitAttributesText(newSplitInfos)
                        updateRadioGroupAndCheckBoxFromRule()
                    }
            }
        }
    }

    override fun onClick(button: View) {
        if (button.id != R.id.launch_activity_to_side) {
            return
        }
        when (componentName) {
            activityA -> {
                startActivity(Intent(this, SplitDeviceStateActivityB::class.java))
            }
            activityB -> finish()
        }
    }

    override fun onCheckedChanged(c: CompoundButton, isChecked: Boolean) {
        when (c.id) {
            R.id.swap_primary_secondary_position_check_box -> {
                shouldReverseContainerPosition = isChecked
                updateSplitPairRuleWithRadioButtonId(
                    viewBinding.splitAttributesOptionsRadioGroup.checkedRadioButtonId
                )
            }
            R.id.show_horizontal_layout_in_tabletop_check_box -> {
                shouldShowHorizontalInTabletop = isChecked
                updateSplitPairRuleWithRadioButtonId(
                    R.id.show_fullscreen_in_portrait_radio_button
                )
            }
            R.id.show_fullscreen_in_book_mode_check_box -> {
                shouldShowFullscreenInBookMode = isChecked
                updateSplitPairRuleWithRadioButtonId(
                    R.id.show_different_layout_with_size_radio_button
                )
            }
        }
    }

    override fun onCheckedChanged(group: RadioGroup, id: Int) {
        updateCheckboxWithRadioButton(id)
        updateSplitPairRuleWithRadioButtonId(id)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        updateSplitPairRuleWithRadioButtonId(lastCheckedRuleId)
    }

    override fun onNothingSelected(view: AdapterView<*>?) {
        // Auto-generated method stub
    }

    private fun updateCheckboxWithRadioButton(id: Int) {
        when (id) {
            R.id.show_fullscreen_in_portrait_radio_button -> {
                showCheckBox(R.id.show_horizontal_layout_in_tabletop_check_box)
                hideCheckBox(R.id.show_fullscreen_in_book_mode_check_box)
            }
            R.id.show_different_layout_with_size_radio_button -> {
                hideCheckBox(R.id.show_horizontal_layout_in_tabletop_check_box)
                showCheckBox(R.id.show_fullscreen_in_book_mode_check_box)
            }
            else -> hideAllSubCheckBoxes()
        }
        // Disable the checkbox because this won't be applied if users want to use the default rule
        // behavior.
        viewBinding.swapPrimarySecondaryPositionCheckBox.isEnabled =
            id != R.id.use_default_split_attributes
    }

    private fun hideAllSubCheckBoxes() {
        hideCheckBox(R.id.show_horizontal_layout_in_tabletop_check_box)
        hideCheckBox(R.id.show_fullscreen_in_book_mode_check_box)
    }

    /** Show check box with [id] and also hides other check boxes. */
    private fun showCheckBox(id: Int) {
        when (id) {
            R.id.show_horizontal_layout_in_tabletop_check_box -> {
                viewBinding.showFullscreenInPortraitDividerTop.visibility = View.VISIBLE
                viewBinding.showHorizontalLayoutInTabletopCheckBox.visibility = View.VISIBLE
                viewBinding.showFullscreenInPortraitDividerBottom.visibility = View.VISIBLE
            }
            R.id.show_fullscreen_in_book_mode_check_box -> {
                viewBinding.showDifferentLayoutWithSizeDividerTop.visibility = View.VISIBLE
                viewBinding.showFullscreenInBookModeCheckBox.visibility = View.VISIBLE
                viewBinding.showDifferentLayoutWithSizeDividerBottom.visibility = View.VISIBLE
            }
        }
    }

    private fun hideCheckBox(id: Int) {
        when (id) {
            R.id.show_horizontal_layout_in_tabletop_check_box -> {
                viewBinding.showFullscreenInPortraitDividerTop.visibility = View.GONE
                viewBinding.showHorizontalLayoutInTabletopCheckBox.visibility = View.GONE
                viewBinding.showFullscreenInPortraitDividerBottom.visibility = View.GONE
                shouldShowHorizontalInTabletop = false
            }
            R.id.show_fullscreen_in_book_mode_check_box -> {
                viewBinding.showDifferentLayoutWithSizeDividerTop.visibility = View.GONE
                viewBinding.showFullscreenInBookModeCheckBox.visibility = View.GONE
                viewBinding.showDifferentLayoutWithSizeDividerBottom.visibility = View.GONE
                shouldShowFullscreenInBookMode = false
            }
        }
    }

    private fun updateSplitPairRuleWithRadioButtonId(id: Int) {
        lastCheckedRuleId = id
        ruleController.clearRules()

        val splitPairFilters = HashSet<SplitPairFilter>()
        val splitPairFilter = SplitPairFilter(
            activityA,
            activityB,
            secondaryActivityIntentAction = null
        )
        splitPairFilters.add(splitPairFilter)
        val defaultSplitAttributes = SplitAttributes.Builder()
            .setSplitType(SPLIT_TYPE_EQUAL)
            .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
            .build()
        // Use the tag to control the rule how to change split attributes with the current state
        var tag = when (id) {
            R.id.use_default_split_attributes -> TAG_USE_DEFAULT_SPLIT_ATTRIBUTES
            R.id.show_fullscreen_in_portrait_radio_button -> {
                if (shouldShowHorizontalInTabletop) {
                    TAG_SHOW_FULLSCREEN_IN_PORTRAIT + SUFFIX_AND_HORIZONTAL_LAYOUT_IN_TABLETOP
                } else {
                    TAG_SHOW_FULLSCREEN_IN_PORTRAIT
                }
            }
            R.id.show_horizontal_layout_in_tabletop_radio_button -> {
                if (shouldReverseContainerPosition) {
                    TAG_SHOW_HORIZONTAL_LAYOUT_IN_TABLETOP + SUFFIX_REVERSED
                } else {
                    TAG_SHOW_HORIZONTAL_LAYOUT_IN_TABLETOP
                }
            }
            R.id.show_different_layout_with_size_radio_button -> {
                if (shouldShowFullscreenInBookMode) {
                    TAG_SHOW_DIFFERENT_LAYOUT_WITH_SIZE + SUFFIX_AND_FULLSCREEN_IN_BOOK_MODE
                } else {
                    TAG_SHOW_DIFFERENT_LAYOUT_WITH_SIZE
                }
            }
            R.id.split_by_hinge_when_separating_radio_button ->
                TAG_SHOW_LAYOUT_FOLLOWING_HINGE_WHEN_SEPARATING
            else -> null
        }
        if (shouldReverseContainerPosition) {
            tag += SUFFIX_REVERSED
        }

        splitPairRule = SplitPairRule.Builder(splitPairFilters)
            .setTag(tag)
            .setMinWidthDp(DEFAULT_MINIMUM_WIDTH_DP)
            .setMinSmallestWidthDp(DEFAULT_MINIMUM_WIDTH_DP)
            .setDefaultSplitAttributes(defaultSplitAttributes)
            .build()
        ruleController.addRule(splitPairRule)
    }

    private suspend fun updateSplitAttributesText(newSplitInfos: List<SplitInfo>) {
        var splitAttributes: SplitAttributes = SplitAttributes.Builder()
            .setSplitType(SPLIT_TYPE_EXPAND)
            .build()
        var suggestToFinishItself = false
        val isCallbackSupported = splitController.isSplitAttributesCalculatorSupported()
        // Traverse SplitInfos from the end because last SplitInfo has the highest z-order.
        for (info in newSplitInfos.reversed()) {
            if (info.contains(this@SplitDeviceStateActivityBase)) {
                splitAttributes = info.splitAttributes
                if (componentName == activityB &&
                    splitAttributes.splitType == SPLIT_TYPE_EXPAND
                ) {
                    // We don't put any functionality on activity B. Suggest users to finish the
                    // activity if it fills the host task.
                    suggestToFinishItself = true
                }
                break
            }
        }
        withContext(Dispatchers.Main) {
            viewBinding.activityPairSplitAttributesTextView.text =
                resources.getString(R.string.current_split_attributes) + splitAttributes
            if (!isCallbackSupported) {
                // Don't update the error message if the callback is not supported.
                return@withContext
            }
            viewBinding.errorMessageTextView.text =
                if (suggestToFinishItself) {
                    "Please finish the activity to try other split configurations."
                } else {
                    ""
                }
        }
    }

    private fun updateRadioGroupAndCheckBoxFromRule() {
        val splitPairRule = ruleController.getRules().firstOrNull { rule ->
            isRuleForSplitActivityA(rule)
        } ?: return
        val tag = splitPairRule.tag
        viewBinding.splitAttributesOptionsRadioGroup.check(
            when (tag?.substringBefore(SUFFIX_REVERSED)) {
                TAG_USE_DEFAULT_SPLIT_ATTRIBUTES -> R.id.use_default_split_attributes
                TAG_SHOW_FULLSCREEN_IN_PORTRAIT,
                TAG_SHOW_FULLSCREEN_IN_PORTRAIT + SUFFIX_AND_HORIZONTAL_LAYOUT_IN_TABLETOP ->
                    R.id.show_fullscreen_in_portrait_radio_button
                TAG_SHOW_HORIZONTAL_LAYOUT_IN_TABLETOP,
                TAG_SHOW_HORIZONTAL_LAYOUT_IN_TABLETOP + SUFFIX_REVERSED ->
                    R.id.show_horizontal_layout_in_tabletop_radio_button
                TAG_SHOW_DIFFERENT_LAYOUT_WITH_SIZE,
                TAG_SHOW_DIFFERENT_LAYOUT_WITH_SIZE + SUFFIX_AND_FULLSCREEN_IN_BOOK_MODE ->
                    R.id.show_different_layout_with_size_radio_button
                TAG_SHOW_LAYOUT_FOLLOWING_HINGE_WHEN_SEPARATING ->
                    R.id.split_by_hinge_when_separating_radio_button
                else -> 0
            }
        )
        if (tag?.contains(TAG_SHOW_FULLSCREEN_IN_PORTRAIT) == true) {
            showCheckBox(R.id.show_horizontal_layout_in_tabletop_check_box)
            viewBinding.showHorizontalLayoutInTabletopCheckBox.isChecked =
                tag.contains(SUFFIX_AND_HORIZONTAL_LAYOUT_IN_TABLETOP)
        } else if (tag?.contains(TAG_SHOW_DIFFERENT_LAYOUT_WITH_SIZE) == true) {
            showCheckBox(R.id.swap_primary_secondary_position_check_box)
            viewBinding.showFullscreenInBookModeCheckBox.isChecked =
                tag.contains(SUFFIX_AND_FULLSCREEN_IN_BOOK_MODE)
        }

        viewBinding.swapPrimarySecondaryPositionCheckBox.isChecked =
            tag?.contains(SUFFIX_REVERSED) ?: false
    }

    private fun isRuleForSplitActivityA(rule: EmbeddingRule): Boolean {
        if (rule !is SplitPairRule) {
            return false
        }
        rule.filters.forEach { filter ->
            if (filter.primaryActivityName == activityA &&
                filter.secondaryActivityName == activityB
            ) {
                return true
            }
        }
        return false
    }

    companion object {
        const val TAG_USE_DEFAULT_SPLIT_ATTRIBUTES = "use_default_split_attributes"
        const val TAG_SHOW_FULLSCREEN_IN_PORTRAIT = "show_fullscreen_in_portrait"
        const val TAG_SHOW_HORIZONTAL_LAYOUT_IN_TABLETOP = "show_horizontal_layout_in_tabletop"
        const val TAG_SHOW_DIFFERENT_LAYOUT_WITH_SIZE = "show_different_layout_with_size"
        const val TAG_SHOW_LAYOUT_FOLLOWING_HINGE_WHEN_SEPARATING = "show_layout_following_hinge"
        const val SUFFIX_REVERSED = "_reversed"
        const val SUFFIX_AND_HORIZONTAL_LAYOUT_IN_TABLETOP = "_and_horizontal_layout_in_tabletop"
        const val SUFFIX_AND_FULLSCREEN_IN_BOOK_MODE = "_and_fullscreen_in_book_mode"

        /**
         * The default minimum dimension for large screen devices.
         *
         * It is also the default value of [SplitPairRule.minWidthDp] and
         * [SplitPairRule.minSmallestWidthDp] if the properties are not specified in static rule
         * XML format.
         */
        const val DEFAULT_MINIMUM_WIDTH_DP = 600
    }
}