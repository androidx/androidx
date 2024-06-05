/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.embedding

import android.app.Activity
import android.os.Bundle
import androidx.core.util.Consumer
import androidx.window.RequiresWindowSdkExtension
import androidx.window.embedding.OverlayController.Companion.OVERLAY_FEATURE_VERSION
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import java.util.concurrent.Executor

/**
 * Adapter interface for different historical versions of activity embedding OEM interface in
 * [ActivityEmbeddingComponent].
 */
internal interface EmbeddingInterfaceCompat {

    fun setRules(rules: Set<EmbeddingRule>)

    fun setEmbeddingCallback(embeddingCallback: EmbeddingCallbackInterface)

    interface EmbeddingCallbackInterface {
        fun onSplitInfoChanged(splitInfo: List<SplitInfo>)

        fun onActivityStackChanged(activityStacks: List<ActivityStack>)
    }

    fun isActivityEmbedded(activity: Activity): Boolean

    @RequiresWindowSdkExtension(5)
    fun pinTopActivityStack(taskId: Int, splitPinRule: SplitPinRule): Boolean

    @RequiresWindowSdkExtension(5) fun unpinTopActivityStack(taskId: Int)

    @RequiresWindowSdkExtension(2)
    fun setSplitAttributesCalculator(
        calculator: (SplitAttributesCalculatorParams) -> SplitAttributes
    )

    @RequiresWindowSdkExtension(2) fun clearSplitAttributesCalculator()

    @RequiresWindowSdkExtension(5)
    fun setLaunchingActivityStack(options: Bundle, activityStack: ActivityStack): Bundle

    @RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
    fun setOverlayCreateParams(options: Bundle, overlayCreateParams: OverlayCreateParams): Bundle

    @RequiresWindowSdkExtension(5) fun finishActivityStacks(activityStacks: Set<ActivityStack>)

    @RequiresWindowSdkExtension(5)
    fun setEmbeddingConfiguration(embeddingConfig: EmbeddingConfiguration)

    @RequiresWindowSdkExtension(3) fun invalidateVisibleActivityStacks()

    @RequiresWindowSdkExtension(3)
    fun updateSplitAttributes(splitInfo: SplitInfo, splitAttributes: SplitAttributes)

    @RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
    fun setOverlayAttributesCalculator(
        calculator: (OverlayAttributesCalculatorParams) -> OverlayAttributes
    )

    @RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION) fun clearOverlayAttributesCalculator()

    @RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
    fun updateOverlayAttributes(overlayTag: String, overlayAttributes: OverlayAttributes)

    @RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
    fun addOverlayInfoCallback(
        overlayTag: String,
        executor: Executor,
        overlayInfoCallback: Consumer<OverlayInfo>,
    )

    @RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
    fun removeOverlayInfoCallback(overlayInfoCallback: Consumer<OverlayInfo>)

    @RequiresWindowSdkExtension(6)
    fun addEmbeddedActivityWindowInfoCallbackForActivity(
        activity: Activity,
        callback: Consumer<EmbeddedActivityWindowInfo>
    )

    @RequiresWindowSdkExtension(6)
    fun removeEmbeddedActivityWindowInfoCallbackForActivity(
        callback: Consumer<EmbeddedActivityWindowInfo>
    )
}
