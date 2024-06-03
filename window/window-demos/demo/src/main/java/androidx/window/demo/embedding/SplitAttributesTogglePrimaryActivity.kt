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

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.WindowSdkExtensions
import androidx.window.core.ExperimentalWindowApi
import androidx.window.demo.R
import androidx.window.embedding.ActivityStack
import androidx.window.embedding.SplitInfo
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalWindowApi::class)
class SplitAttributesTogglePrimaryActivity :
    SplitAttributesToggleMainActivity(), View.OnClickListener {

    private lateinit var secondaryActivityIntent: Intent
    private var activityStacks: Set<ActivityStack> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding.rootSplitActivityLayout.setBackgroundColor(Color.parseColor("#e8f5e9"))

        val isRuntimeApiSupported = WindowSdkExtensions.getInstance().extensionVersion >= 3

        secondaryActivityIntent = Intent(this, SplitAttributesToggleSecondaryActivity::class.java)

        if (intent.getBooleanExtra(EXTRA_LAUNCH_SECONDARY, false)) {
            startActivity(secondaryActivityIntent)
            // Remove the extra in case the secondary activity is started again when the primary
            // activity is relaunched.
            intent.removeExtra(EXTRA_LAUNCH_SECONDARY)
        }

        // Enable to finish secondary ActivityStacks for primary Activity.
        viewBinding.finishSecondaryActivitiesDivider.visibility = View.VISIBLE
        val finishSecondaryActivitiesButton =
            viewBinding.finishSecondaryActivitiesButton.apply {
                visibility = View.VISIBLE
                if (!isRuntimeApiSupported) {
                    isEnabled = false
                } else {
                    setOnClickListener(this@SplitAttributesTogglePrimaryActivity)
                }
            }

        // Animation background
        if (WindowSdkExtensions.getInstance().extensionVersion >= 5) {
            val animationBackgroundDropdown = viewBinding.animationBackgroundDropdown
            animationBackgroundDropdown.visibility = View.VISIBLE
            viewBinding.animationBackgroundDivider.visibility = View.VISIBLE
            viewBinding.animationBackgroundTextView.visibility = View.VISIBLE
            animationBackgroundDropdown.adapter =
                ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    DemoActivityEmbeddingController.ANIMATION_BACKGROUND_TEXTS
                )
            animationBackgroundDropdown.onItemSelectedListener = this
        }

        lifecycleScope.launch {
            // The block passed to repeatOnLifecycle is executed when the lifecycle
            // is at least STARTED and is cancelled when the lifecycle is STOPPED.
            // It automatically restarts the block when the lifecycle is STARTED again.
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                splitController
                    .splitInfoList(this@SplitAttributesTogglePrimaryActivity)
                    .onEach { updateUiFromRules() }
                    .collect { splitInfoList ->
                        finishSecondaryActivitiesButton.isEnabled = splitInfoList.isNotEmpty()
                        activityStacks =
                            splitInfoList.mapTo(mutableSetOf()) { splitInfo ->
                                splitInfo.getTheOtherActivityStack(
                                    this@SplitAttributesTogglePrimaryActivity
                                )
                            }
                    }
            }
        }
    }

    private fun SplitInfo.getTheOtherActivityStack(activity: Activity): ActivityStack =
        if (activity in primaryActivityStack) {
            secondaryActivityStack
        } else {
            primaryActivityStack
        }

    override fun onClick(button: View) {
        super.onClick(button)
        when (button.id) {
            R.id.finish_secondary_activities_button -> {
                applyRules()
                activityEmbeddingController.finishActivityStacks(activityStacks)
            }
        }
    }
}
