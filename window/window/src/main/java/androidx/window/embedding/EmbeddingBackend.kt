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
import android.content.Context
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.core.util.Consumer
import androidx.window.RequiresWindowSdkExtension
import androidx.window.embedding.OverlayController.Companion.OVERLAY_FEATURE_VERSION
import java.util.concurrent.Executor

/**  */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface EmbeddingBackend {
    public fun setRules(rules: Set<EmbeddingRule>)

    public fun getRules(): Set<EmbeddingRule>

    public fun addRule(rule: EmbeddingRule)

    public fun removeRule(rule: EmbeddingRule)

    public fun addSplitListenerForActivity(
        activity: Activity,
        executor: Executor,
        callback: Consumer<List<SplitInfo>>,
    )

    public fun removeSplitListenerForActivity(consumer: Consumer<List<SplitInfo>>)

    public val splitSupportStatus: SplitController.SplitSupportStatus

    public fun isActivityEmbedded(activity: Activity): Boolean

    @RequiresWindowSdkExtension(5)
    public fun pinTopActivityStack(taskId: Int, splitPinRule: SplitPinRule): Boolean

    @RequiresWindowSdkExtension(5) public fun unpinTopActivityStack(taskId: Int)

    @RequiresWindowSdkExtension(2)
    public fun setSplitAttributesCalculator(
        calculator: (SplitAttributesCalculatorParams) -> SplitAttributes
    )

    @RequiresWindowSdkExtension(2) public fun clearSplitAttributesCalculator()

    public fun getActivityStack(activity: Activity): ActivityStack?

    @RequiresWindowSdkExtension(5)
    public fun setLaunchingActivityStack(options: Bundle, activityStack: ActivityStack): Bundle

    @RequiresWindowSdkExtension(5)
    public fun setOverlayCreateParams(
        options: Bundle,
        overlayCreateParams: OverlayCreateParams,
    ): Bundle

    @RequiresWindowSdkExtension(5)
    public fun finishActivityStacks(activityStacks: Set<ActivityStack>)

    @RequiresWindowSdkExtension(5)
    public fun setEmbeddingConfiguration(embeddingConfig: EmbeddingConfiguration)

    @RequiresWindowSdkExtension(3) public fun invalidateVisibleActivityStacks()

    @RequiresWindowSdkExtension(3)
    public fun updateSplitAttributes(splitInfo: SplitInfo, splitAttributes: SplitAttributes)

    @RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
    public fun setOverlayAttributesCalculator(
        calculator: (OverlayAttributesCalculatorParams) -> OverlayAttributes
    )

    @RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
    public fun clearOverlayAttributesCalculator()

    @RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
    public fun updateOverlayAttributes(overlayTag: String, overlayAttributes: OverlayAttributes)

    @RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
    public fun addOverlayInfoCallback(
        overlayTag: String,
        executor: Executor,
        overlayInfoCallback: Consumer<OverlayInfo>,
    )

    @RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
    public fun removeOverlayInfoCallback(overlayInfoCallback: Consumer<OverlayInfo>)

    @RequiresWindowSdkExtension(6)
    public fun addEmbeddedActivityWindowInfoCallbackForActivity(
        activity: Activity,
        callback: Consumer<EmbeddedActivityWindowInfo>,
    )

    @RequiresWindowSdkExtension(6)
    public fun removeEmbeddedActivityWindowInfoCallbackForActivity(
        callback: Consumer<EmbeddedActivityWindowInfo>
    )

    public companion object {

        private var decorator: (EmbeddingBackend) -> EmbeddingBackend = { it }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public fun getInstance(context: Context): EmbeddingBackend {
            return decorator(ExtensionEmbeddingBackend.getInstance(context))
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun overrideDecorator(overridingDecorator: EmbeddingBackendDecorator) {
            decorator = overridingDecorator::decorate
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @JvmStatic
        public fun reset() {
            decorator = { it }
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface EmbeddingBackendDecorator {

    /** Returns an instance of [EmbeddingBackend] */
    public fun decorate(embeddingBackend: EmbeddingBackend): EmbeddingBackend
}
