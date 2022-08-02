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

package androidx.window.sample.embedding

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.ActivityFilter
import androidx.window.embedding.EmbeddingRule
import androidx.window.embedding.SplitController
import androidx.window.embedding.SplitInfo
import androidx.window.embedding.SplitPairFilter
import androidx.window.embedding.SplitPairRule
import androidx.window.embedding.SplitPlaceholderRule
import androidx.window.embedding.SplitRule
import androidx.window.sample.R
import androidx.window.sample.databinding.ActivitySplitPipActivityLayoutBinding
import androidx.window.sample.util.PictureInPictureUtil.setPictureInPictureParams
import androidx.window.sample.util.PictureInPictureUtil.startPictureInPicture

/**
 * Sample showcase of split activity rules with picture-in-picture. Allows the user to select some
 * split and PiP configuration options with checkboxes and launch activities with those options
 * applied.
 */
@OptIn(ExperimentalWindowApi::class)
abstract class SplitPipActivityBase : AppCompatActivity(), CompoundButton.OnCheckedChangeListener,
    View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    lateinit var splitController: SplitController
    lateinit var viewBinding: ActivitySplitPipActivityLayoutBinding
    lateinit var componentNameA: ComponentName
    lateinit var componentNameB: ComponentName
    lateinit var componentNameNotPip: ComponentName
    lateinit var componentNamePlaceholder: ComponentName
    private val splitChangeListener = SplitStateChangeListener()
    private val splitRatio = 0.5f
    private var enterPipOnUserLeave = false
    private var autoEnterPip = false

    /** In the process of updating checkboxes based on split rule. */
    private var updatingConfigs = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySplitPipActivityLayoutBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        componentNameA = ComponentName(packageName, SplitPipActivityA::class.java.name)
        componentNameB = ComponentName(packageName, SplitPipActivityB::class.java.name)
        componentNameNotPip = ComponentName(packageName, SplitPipActivityNoPip::class.java.name)
        componentNamePlaceholder = ComponentName(packageName,
            SplitPipActivityPlaceholder::class.java.name)

        splitController = SplitController.getInstance()

        // Buttons for split rules of the main activity.
        viewBinding.splitMainCheckBox.setOnCheckedChangeListener(this)
        viewBinding.finishPrimaryWithSecondaryCheckBox.setOnCheckedChangeListener(this)
        viewBinding.finishSecondaryWithPrimaryCheckBox.setOnCheckedChangeListener(this)

        // Buttons for split rules of the secondary activity.
        viewBinding.launchBButton.setOnClickListener(this)
        viewBinding.usePlaceHolderCheckBox.setOnCheckedChangeListener(this)
        viewBinding.useStickyPlaceHolderCheckBox.setOnCheckedChangeListener(this)

        // Buttons for launching an activity that doesn't support PiP
        viewBinding.launchNoPipButton.setOnClickListener(this)

        // Buttons for PiP options.
        viewBinding.enterPipButton.setOnClickListener(this)
        viewBinding.supportPipRadioGroup.setOnCheckedChangeListener(this)
    }

    /** Called on checkbox changed. */
    override fun onCheckedChanged(button: CompoundButton, isChecked: Boolean) {
        if (button.id == R.id.split_main_check_box) {
            if (isChecked) {
                viewBinding.finishPrimaryWithSecondaryCheckBox.isEnabled = true
                viewBinding.finishSecondaryWithPrimaryCheckBox.isEnabled = true
            } else {
                viewBinding.finishPrimaryWithSecondaryCheckBox.isEnabled = false
                viewBinding.finishPrimaryWithSecondaryCheckBox.isChecked = false
                viewBinding.finishSecondaryWithPrimaryCheckBox.isEnabled = false
                viewBinding.finishSecondaryWithPrimaryCheckBox.isChecked = false
            }
        }
        if (button.id == R.id.use_place_holder_check_box) {
            if (isChecked) {
                viewBinding.useStickyPlaceHolderCheckBox.isEnabled = true
            } else {
                viewBinding.useStickyPlaceHolderCheckBox.isEnabled = false
                viewBinding.useStickyPlaceHolderCheckBox.isChecked = false
            }
        }
        if (!updatingConfigs) {
            updateSplitRules()
        }
    }

    /** Called on button clicked. */
    override fun onClick(button: View) {
        when (button.id) {
            R.id.launch_b_button -> {
                startActivity(Intent(this, SplitPipActivityB::class.java))
                return
            }
            R.id.launch_no_pip_button -> {
                startActivity(Intent(this, SplitPipActivityNoPip::class.java))
                return
            }
            R.id.enter_pip_button -> {
                startPictureInPicture(this, autoEnterPip)
            }
        }
    }

    /** Called on RatioGroup (PiP options) changed. */
    override fun onCheckedChanged(group: RadioGroup, id: Int) {
        when (id) {
            R.id.support_pip_not_enter_on_exit -> {
                enterPipOnUserLeave = false
                autoEnterPip = false
            }
            R.id.support_pip_enter_on_user_leave -> {
                enterPipOnUserLeave = true
                autoEnterPip = false
            }
            R.id.support_pip_auto_enter -> {
                enterPipOnUserLeave = false
                autoEnterPip = true
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    Toast.makeText(this, "auto enter PiP not supported", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
        setPictureInPictureParams(this, autoEnterPip)
    }

    /** Enters PiP if enterPipOnUserLeave checkbox is checked. */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (enterPipOnUserLeave) {
            startPictureInPicture(this, autoEnterPip)
        }
    }

    /** Updates the checkboxes states after the split rules are changed by other activity. */
    internal fun updateCheckboxes() {
        updatingConfigs = true

        val curRules = splitController.getSplitRules()
        val splitRule = curRules.firstOrNull { isRuleForSplit(it) }
        val placeholderRule = curRules.firstOrNull { isRuleForPlaceholder(it) }

        if (splitRule != null && splitRule is SplitPairRule) {
            viewBinding.splitMainCheckBox.isChecked = true
            viewBinding.finishPrimaryWithSecondaryCheckBox.isEnabled = true
            viewBinding.finishPrimaryWithSecondaryCheckBox.isChecked =
                splitRule.finishPrimaryWithSecondary == SplitRule.FINISH_ALWAYS
            viewBinding.finishSecondaryWithPrimaryCheckBox.isEnabled = true
            viewBinding.finishSecondaryWithPrimaryCheckBox.isChecked =
                splitRule.finishSecondaryWithPrimary == SplitRule.FINISH_ALWAYS
        } else {
            viewBinding.splitMainCheckBox.isChecked = false
            viewBinding.finishPrimaryWithSecondaryCheckBox.isEnabled = false
            viewBinding.finishPrimaryWithSecondaryCheckBox.isChecked = false
            viewBinding.finishSecondaryWithPrimaryCheckBox.isEnabled = false
            viewBinding.finishSecondaryWithPrimaryCheckBox.isChecked = false
        }

        if (placeholderRule != null && placeholderRule is SplitPlaceholderRule) {
            viewBinding.usePlaceHolderCheckBox.isChecked = true
            viewBinding.useStickyPlaceHolderCheckBox.isEnabled = true
            viewBinding.useStickyPlaceHolderCheckBox.isChecked = placeholderRule.isSticky
        } else {
            viewBinding.usePlaceHolderCheckBox.isChecked = false
            viewBinding.useStickyPlaceHolderCheckBox.isEnabled = false
            viewBinding.useStickyPlaceHolderCheckBox.isChecked = false
        }

        updatingConfigs = false
    }

    /** Whether the given rule is for splitting activity A and others. */
    private fun isRuleForSplit(rule: EmbeddingRule): Boolean {
        if (rule !is SplitPairRule) {
            return false
        }
        for (filter in rule.filters) {
            if (filter.primaryActivityName.className == SplitPipActivityA::class.java.name) {
                return true
            }
        }
        return false
    }

    /** Whether the given rule is for launching placeholder with activity B. */
    private fun isRuleForPlaceholder(rule: EmbeddingRule): Boolean {
        if (rule !is SplitPlaceholderRule) {
            return false
        }
        for (filter in rule.filters) {
            if (filter.componentName.className == SplitPipActivityB::class.java.name) {
                return true
            }
        }
        return false
    }

    /** Updates the split rules based on the current selection on checkboxes. */
    private fun updateSplitRules() {
        splitController.clearRegisteredRules()

        if (viewBinding.splitMainCheckBox.isChecked) {
            val pairFilters = HashSet<SplitPairFilter>()
            pairFilters.add(SplitPairFilter(componentNameA, componentNameB, null))
            pairFilters.add(SplitPairFilter(componentNameA, componentNameNotPip, null))
            val finishAWithB = viewBinding.finishPrimaryWithSecondaryCheckBox.isChecked
            val finishBWithA = viewBinding.finishSecondaryWithPrimaryCheckBox.isChecked
            val rule = SplitPairRule.Builder(pairFilters, 0, 0)
                .setFinishPrimaryWithSecondary(
                    if (finishAWithB) SplitRule.FINISH_ALWAYS else SplitRule.FINISH_NEVER)
                .setFinishSecondaryWithPrimary(
                    if (finishBWithA) SplitRule.FINISH_ALWAYS else SplitRule.FINISH_NEVER)
                .setClearTop(true)
                .setSplitRatio(splitRatio)
                .build()
            splitController.registerRule(rule)
        }

        if (viewBinding.usePlaceHolderCheckBox.isChecked) {
            val activityFilters = HashSet<ActivityFilter>()
            activityFilters.add(ActivityFilter(componentNameB, null))
            val intent = Intent().setComponent(componentNamePlaceholder)
            val isSticky = viewBinding.useStickyPlaceHolderCheckBox.isChecked
            val rule = SplitPlaceholderRule.Builder(activityFilters, intent, 0, 0)
                .setSticky(isSticky)
                .setFinishPrimaryWithPlaceholder(SplitRule.FINISH_ADJACENT)
                .setSplitRatio(splitRatio)
                .build()
            splitController.registerRule(rule)
        }
    }

    override fun onStart() {
        super.onStart()
        splitController.addSplitListener(
            this,
            ContextCompat.getMainExecutor(this),
            splitChangeListener
        )
    }

    override fun onStop() {
        super.onStop()
        splitController.removeSplitListener(splitChangeListener)
    }

    /** Updates the embedding status when receives callback from the extension. */
    inner class SplitStateChangeListener : Consumer<List<SplitInfo>> {
        override fun accept(newSplitInfos: List<SplitInfo>) {
            var isInSplit = false
            for (info in newSplitInfos) {
                if (info.contains(this@SplitPipActivityBase) && info.splitRatio > 0) {
                    isInSplit = true
                    break
                }
            }

            runOnUiThread {
                viewBinding.activityEmbeddedStatusTextView.visibility =
                    if (isInSplit) View.VISIBLE else View.GONE

                updateCheckboxes()
            }
        }
    }
}