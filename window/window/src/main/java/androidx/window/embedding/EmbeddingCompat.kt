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
import android.app.ActivityOptions
import android.content.Context
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.util.Consumer as JetpackConsumer
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.core.BuildConfig
import androidx.window.core.ConsumerAdapter
import androidx.window.core.ExperimentalWindowApi
import androidx.window.core.VerificationMode
import androidx.window.embedding.EmbeddingInterfaceCompat.EmbeddingCallbackInterface
import androidx.window.embedding.SplitController.SplitSupportStatus.Companion.SPLIT_AVAILABLE
import androidx.window.extensions.WindowExtensionsProvider
import androidx.window.extensions.core.util.function.Consumer
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import androidx.window.extensions.embedding.ActivityStack as OEMActivityStack
import androidx.window.extensions.embedding.ActivityStackAttributes
import androidx.window.extensions.embedding.SplitInfo as OEMSplitInfo
import java.lang.reflect.Proxy
import java.util.concurrent.Executor

/**
 * Adapter implementation for different historical versions of activity embedding OEM interface in
 * [ActivityEmbeddingComponent]. Only supports the single current version in this implementation.
 */
internal class EmbeddingCompat(
    private val embeddingExtension: ActivityEmbeddingComponent,
    private val adapter: EmbeddingAdapter,
    private val consumerAdapter: ConsumerAdapter,
    private val applicationContext: Context
) : EmbeddingInterfaceCompat {

    private val windowSdkExtensions = WindowSdkExtensions.getInstance()

    private var isCustomSplitAttributeCalculatorSet: Boolean = false

    private var overlayController: OverlayControllerImpl? =
        if (windowSdkExtensions.extensionVersion >= 5) {
            OverlayControllerImpl(embeddingExtension, adapter)
        } else {
            null
        }

    override fun setRules(rules: Set<EmbeddingRule>) {
        var hasSplitRule = false
        for (rule in rules) {
            if (rule is SplitRule) {
                hasSplitRule = true
                break
            }
        }
        if (hasSplitRule &&
            SplitController.getInstance(applicationContext).splitSupportStatus != SPLIT_AVAILABLE
        ) {
            if (BuildConfig.verificationMode == VerificationMode.LOG) {
                Log.w(
                    TAG, "Cannot set SplitRule because ActivityEmbedding Split is not " +
                        "supported or PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED is not set."
                )
            }
            return
        }

        val r = adapter.translate(applicationContext, rules)
        embeddingExtension.setEmbeddingRules(r)
    }

    override fun setEmbeddingCallback(embeddingCallback: EmbeddingCallbackInterface) {
        when (windowSdkExtensions.extensionVersion) {
            1 -> {
                consumerAdapter.addConsumer(
                    embeddingExtension,
                    List::class,
                    "setSplitInfoCallback"
                ) { values ->
                    val splitInfoList = values.filterIsInstance<OEMSplitInfo>()
                    embeddingCallback.onSplitInfoChanged(adapter.translate(splitInfoList))
                }
            }
            in 2..4 -> {
                registerSplitInfoCallback(embeddingCallback)
            }
            5 -> {
                registerSplitInfoCallback(embeddingCallback)

                // Register ActivityStack callback
                val activityStackCallback = Consumer<List<OEMActivityStack>> { activityStacks ->
                    embeddingCallback.onActivityStackChanged(adapter.translate(activityStacks))
                }
                embeddingExtension.registerActivityStackCallback(
                    Runnable::run,
                    activityStackCallback
                )
            }
        }
    }

    private fun registerSplitInfoCallback(embeddingCallback: EmbeddingCallbackInterface) {
        val splitInfoCallback = Consumer<List<OEMSplitInfo>> { splitInfoList ->
            embeddingCallback.onSplitInfoChanged(adapter.translate(splitInfoList))
        }
        embeddingExtension.setSplitInfoCallback(splitInfoCallback)
    }

    override fun isActivityEmbedded(activity: Activity): Boolean {
        return embeddingExtension.isActivityEmbedded(activity)
    }

    @RequiresWindowSdkExtension(5)
    @OptIn(ExperimentalWindowApi::class)
    override fun pinTopActivityStack(taskId: Int, splitPinRule: SplitPinRule): Boolean {
        windowSdkExtensions.requireExtensionVersion(5)
        return embeddingExtension.pinTopActivityStack(
            taskId,
            adapter.translateSplitPinRule(
                applicationContext,
                splitPinRule
            )
        )
    }

    @RequiresWindowSdkExtension(5)
    override fun unpinTopActivityStack(taskId: Int) {
        windowSdkExtensions.requireExtensionVersion(5)
        return embeddingExtension.unpinTopActivityStack(taskId)
    }

    @RequiresWindowSdkExtension(2)
    override fun setSplitAttributesCalculator(
        calculator: (SplitAttributesCalculatorParams) -> SplitAttributes
    ) {
        windowSdkExtensions.requireExtensionVersion(2)

        embeddingExtension.setSplitAttributesCalculator(
            adapter.translateSplitAttributesCalculator(calculator)
        )
        isCustomSplitAttributeCalculatorSet = true;
    }

    @RequiresWindowSdkExtension(2)
    override fun clearSplitAttributesCalculator() {
        windowSdkExtensions.requireExtensionVersion(2)

        embeddingExtension.clearSplitAttributesCalculator()
        isCustomSplitAttributeCalculatorSet = false
        setDefaultSplitAttributeCalculatorIfNeeded()
    }

    @RequiresWindowSdkExtension(5)
    override fun finishActivityStacks(activityStacks: Set<ActivityStack>) {
        // This API requires version 5 because the implementation needs ActivityStack#getToken,
        // which is targeting vendor API level 5.
        windowSdkExtensions.requireExtensionVersion(5)

        val stackTokens = activityStacks.mapTo(mutableSetOf()) { it.token }
        embeddingExtension.finishActivityStacks(stackTokens)
    }

    @OptIn(ExperimentalWindowApi::class)
    @RequiresWindowSdkExtension(5)
    override fun setEmbeddingConfiguration(embeddingConfig: EmbeddingConfiguration) {
        windowSdkExtensions.requireExtensionVersion(5)
        adapter.embeddingConfiguration = embeddingConfig
        setDefaultSplitAttributeCalculatorIfNeeded()

        embeddingExtension.invalidateTopVisibleSplitAttributes()
    }

    @OptIn(ExperimentalWindowApi::class)
    private fun setDefaultSplitAttributeCalculatorIfNeeded() {
        // Setting a default SplitAttributeCalculator if the EmbeddingConfiguration is set,
        // in order to ensure the dimArea in the SplitAttribute is up-to-date.
        if (windowSdkExtensions.extensionVersion >= 5 && !isCustomSplitAttributeCalculatorSet &&
            adapter.embeddingConfiguration != null) {
            embeddingExtension.setSplitAttributesCalculator { params ->
                adapter.translateSplitAttributes(adapter.translate(params.defaultSplitAttributes))
            }
        }
    }

    @RequiresWindowSdkExtension(3)
    override fun invalidateVisibleActivityStacks() {
        windowSdkExtensions.requireExtensionVersion(3)

        embeddingExtension.invalidateVisibleActivityStacks()
    }

    /**
     * Updates top [activityStacks][ActivityStack] layouts, which will trigger [SplitAttributes]
     * calculator and [ActivityStackAttributes] calculator if set.
     */
    private fun ActivityEmbeddingComponent.invalidateVisibleActivityStacks() {
        // Note that this API also updates overlay container regardless of its naming.
        invalidateTopVisibleSplitAttributes()
    }

    @RequiresWindowSdkExtension(3)
    override fun updateSplitAttributes(
        splitInfo: SplitInfo,
        splitAttributes: SplitAttributes
    ) {
        windowSdkExtensions.requireExtensionVersion(3)

        embeddingExtension.updateSplitAttributes(
            splitInfo.token,
            adapter.translateSplitAttributes(splitAttributes)
        )
    }

    @RequiresWindowSdkExtension(5)
    override fun setLaunchingActivityStack(
        options: ActivityOptions,
        token: IBinder
    ): ActivityOptions {
        // This API requires version 5 because the implementation needs ActivityStack#getToken,
        // which is targeting vendor API level 5.
        windowSdkExtensions.requireExtensionVersion(5)

        return embeddingExtension.setLaunchingActivityStack(options, token)
    }

    @RequiresWindowSdkExtension(5)
    override fun setOverlayCreateParams(
        options: Bundle,
        overlayCreateParams: OverlayCreateParams
    ): Bundle = options.apply {
        ActivityEmbeddingOptionsImpl.setOverlayCreateParams(options, overlayCreateParams)
    }

    @RequiresWindowSdkExtension(5)
    override fun setOverlayAttributesCalculator(
        calculator: (OverlayAttributesCalculatorParams) -> OverlayAttributes
    ) {
        windowSdkExtensions.requireExtensionVersion(5)

        overlayController!!.overlayAttributesCalculator = calculator
    }

    @RequiresWindowSdkExtension(5)
    override fun clearOverlayAttributesCalculator() {
        windowSdkExtensions.requireExtensionVersion(5)

        overlayController!!.overlayAttributesCalculator = null
    }

    @RequiresWindowSdkExtension(5)
    override fun updateOverlayAttributes(overlayTag: String, overlayAttributes: OverlayAttributes) {
        windowSdkExtensions.requireExtensionVersion(5)

        overlayController!!.updateOverlayAttributes(overlayTag, overlayAttributes)
    }

    @RequiresWindowSdkExtension(5)
    override fun addOverlayInfoCallback(
        overlayTag: String,
        executor: Executor,
        overlayInfoCallback: JetpackConsumer<OverlayInfo>,
    ) {
        overlayController?.addOverlayInfoCallback(
            overlayTag,
            executor,
            overlayInfoCallback,
        ) ?: apply {
            Log.w(TAG, "overlayInfo is not supported on device less than version 5")

            overlayInfoCallback.accept(
                OverlayInfo(
                    overlayTag,
                    currentOverlayAttributes = null,
                    activityStack = null,
                )
            )
        }
    }

    @RequiresWindowSdkExtension(5)
    override fun removeOverlayInfoCallback(overlayInfoCallback: JetpackConsumer<OverlayInfo>) {
        overlayController?.removeOverlayInfoCallback(overlayInfoCallback)
    }

    companion object {
        const val DEBUG = true
        private const val TAG = "EmbeddingCompat"

        fun isEmbeddingAvailable(): Boolean {
            return try {
                EmbeddingCompat::class.java.classLoader?.let { loader ->
                    SafeActivityEmbeddingComponentProvider(
                        loader,
                        ConsumerAdapter(loader),
                        WindowExtensionsProvider.getWindowExtensions(),
                    ).activityEmbeddingComponent != null
                } ?: false
            } catch (e: NoClassDefFoundError) {
                if (DEBUG) {
                    Log.d(TAG, "Embedding extension version not found")
                }
                false
            } catch (e: UnsupportedOperationException) {
                if (DEBUG) {
                    Log.d(TAG, "Stub Extension")
                }
                false
            }
        }

        fun embeddingComponent(): ActivityEmbeddingComponent {
            return if (isEmbeddingAvailable()) {
                EmbeddingCompat::class.java.classLoader?.let { loader ->
                    SafeActivityEmbeddingComponentProvider(
                        loader,
                        ConsumerAdapter(loader),
                        WindowExtensionsProvider.getWindowExtensions(),
                    ).activityEmbeddingComponent
                } ?: emptyActivityEmbeddingProxy()
            } else {
                emptyActivityEmbeddingProxy()
            }
        }

        private fun emptyActivityEmbeddingProxy(): ActivityEmbeddingComponent {
            return Proxy.newProxyInstance(
                EmbeddingCompat::class.java.classLoader,
                arrayOf(ActivityEmbeddingComponent::class.java)
            ) { _, _, _ -> } as ActivityEmbeddingComponent
        }
    }
}
