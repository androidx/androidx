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

package androidx.window.embedding

import android.app.Activity
import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.window.SafeWindowExtensionsProvider
import androidx.window.core.ConsumerAdapter
import androidx.window.core.ExtensionsUtil
import androidx.window.extensions.WindowExtensions
import androidx.window.extensions.core.util.function.Consumer
import androidx.window.extensions.core.util.function.Function
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import androidx.window.extensions.embedding.ActivityRule
import androidx.window.extensions.embedding.ActivityStack
import androidx.window.extensions.embedding.SplitAttributes
import androidx.window.extensions.embedding.SplitAttributes.SplitType
import androidx.window.extensions.embedding.SplitInfo
import androidx.window.extensions.embedding.SplitPairRule
import androidx.window.extensions.embedding.SplitPlaceholderRule
import androidx.window.reflection.ReflectionUtils.doesReturn
import androidx.window.reflection.ReflectionUtils.isPublic
import androidx.window.reflection.ReflectionUtils.validateReflection
import androidx.window.reflection.WindowExtensionsConstants.ACTIVITY_EMBEDDING_COMPONENT_CLASS

/**
 * Reflection Guard for [ActivityEmbeddingComponent].
 * This will go through the [ActivityEmbeddingComponent]'s method by reflection and
 * check each method's name and signature to see if the interface is what we required.
 */
internal class SafeActivityEmbeddingComponentProvider(
    private val loader: ClassLoader,
    private val consumerAdapter: ConsumerAdapter,
    private val windowExtensions: WindowExtensions
) {
    private val safeWindowExtensionsProvider = SafeWindowExtensionsProvider(loader)

    val activityEmbeddingComponent: ActivityEmbeddingComponent?
        get() {
            return if (canUseActivityEmbeddingComponent()) {
                try {
                    windowExtensions.activityEmbeddingComponent
                } catch (e: UnsupportedOperationException) {
                    null
                }
            } else {
                null
            }
        }

    private fun canUseActivityEmbeddingComponent(): Boolean {
        if (!isActivityEmbeddingComponentAccessible()) {
            return false
        }
        // TODO(b/267573854) : update logic to fallback to lower version
        //  if higher version is not matched
        return when (ExtensionsUtil.safeVendorApiLevel) {
            1 -> hasValidVendorApiLevel1()
            in 2..Int.MAX_VALUE -> hasValidVendorApiLevel2()
            // TODO(b/267956499) : add  hasValidVendorApiLevel3
            else -> false
        }
    }

    @VisibleForTesting
    internal fun isActivityEmbeddingComponentAccessible(): Boolean =
        safeWindowExtensionsProvider.isWindowExtensionsValid() &&
            isActivityEmbeddingComponentValid()

    /**
     * [WindowExtensions.VENDOR_API_LEVEL_1] includes the following methods:
     *  - [ActivityEmbeddingComponent.setEmbeddingRules]
     *  - [ActivityEmbeddingComponent.isActivityEmbedded]
     *  - [ActivityEmbeddingComponent.setSplitInfoCallback] with [java.util.function.Consumer]
     * and following classes:
     *  - [ActivityRule]
     *  - [SplitInfo]
     *  - [SplitPairRule]
     *  - [SplitPlaceholderRule]
     */
    @VisibleForTesting
    internal fun hasValidVendorApiLevel1(): Boolean {
        return isMethodSetEmbeddingRulesValid() &&
            isMethodIsActivityEmbeddedValid() &&
            isMethodSetSplitInfoCallbackJavaConsumerValid() &&
            isClassActivityRuleValid() &&
            isClassSplitInfoValid() &&
            isClassSplitPairRuleValid() &&
            isClassSplitPlaceholderRuleValid()
    }

    /**
     * Vendor API level 2 includes the following methods:
     *  - [ActivityEmbeddingComponent.setSplitInfoCallback] with [Consumer]
     *  - [ActivityEmbeddingComponent.clearSplitInfoCallback]
     *  - [ActivityEmbeddingComponent.setSplitAttributesCalculator]
     *  - [ActivityEmbeddingComponent.clearSplitAttributesCalculator]
     *  - [SplitInfo.getSplitAttributes]
     * and following classes:
     *  - [SplitAttributes]
     *  - [SplitAttributes.SplitType]
     */
    @VisibleForTesting
    internal fun hasValidVendorApiLevel2(): Boolean {
        return hasValidVendorApiLevel1() &&
            isMethodSetSplitInfoCallbackWindowConsumerValid() &&
            isMethodClearSplitInfoCallbackValid() &&
            isMethodSplitAttributesCalculatorValid() &&
            isMethodGetSplitAttributesValid() &&
            isClassSplitAttributesValid() &&
            isClassSplitTypeValid()
    }

    private fun isMethodSetEmbeddingRulesValid(): Boolean {
        return validateReflection("ActivityEmbeddingComponent#setEmbeddingRules is not valid") {
            val setEmbeddingRulesMethod = activityEmbeddingComponentClass.getMethod(
                "setEmbeddingRules",
                Set::class.java
            )
            setEmbeddingRulesMethod.isPublic
        }
    }

    private fun isMethodIsActivityEmbeddedValid(): Boolean {
        return validateReflection("ActivityEmbeddingComponent#isActivityEmbedded is not valid") {
            val isActivityEmbeddedMethod = activityEmbeddingComponentClass.getMethod(
                "isActivityEmbedded",
                Activity::class.java
            )
            isActivityEmbeddedMethod.isPublic &&
                isActivityEmbeddedMethod.doesReturn(Boolean::class.java)
        }
    }

    private fun isMethodClearSplitInfoCallbackValid(): Boolean {
        return validateReflection(
            "ActivityEmbeddingComponent#clearSplitInfoCallback is not valid"
        ) {
            val clearSplitInfoCallbackMethod =
                activityEmbeddingComponentClass.getMethod("clearSplitInfoCallback")
            clearSplitInfoCallbackMethod.isPublic
        }
    }

    private fun isMethodSplitAttributesCalculatorValid(): Boolean {
        return validateReflection(
            "ActivityEmbeddingComponent#setSplitAttributesCalculator is not valid"
        ) {
            val setSplitAttributesCalculatorMethod = activityEmbeddingComponentClass.getMethod(
                "setSplitAttributesCalculator",
                Function::class.java
            )
            val clearSplitAttributesCalculatorMethod =
                activityEmbeddingComponentClass.getMethod("clearSplitAttributesCalculator")
            setSplitAttributesCalculatorMethod.isPublic &&
                clearSplitAttributesCalculatorMethod.isPublic
        }
    }

    private fun isMethodGetSplitAttributesValid(): Boolean =
        validateReflection("SplitInfo#getSplitAttributes is not valid") {
            val splitInfoClass = SplitInfo::class.java
            val getSplitAttributesMethod = splitInfoClass.getMethod("getSplitAttributes")
            getSplitAttributesMethod.isPublic &&
                getSplitAttributesMethod.doesReturn(SplitAttributes::class.java)
        }

    private fun isClassSplitAttributesValid(): Boolean =
        validateReflection("Class SplitAttributes is not valid") {
            val splitAttributesClass = SplitAttributes::class.java
            val getLayoutDirectionMethod =
                splitAttributesClass.getMethod("getLayoutDirection")
            val getSplitTypeMethod = splitAttributesClass.getMethod("getSplitType")
            val splitAttributesBuilderClass = SplitAttributes.Builder::class.java
            val setSplitTypeMethod = splitAttributesBuilderClass.getMethod(
                "setSplitType",
                SplitType::class.java
            )
            val setLayoutDirectionMethod = splitAttributesBuilderClass.getMethod(
                "setLayoutDirection",
                Int::class.java
            )
            getLayoutDirectionMethod.isPublic &&
                getLayoutDirectionMethod.doesReturn(Int::class.java) &&
                getSplitTypeMethod.isPublic &&
                getSplitTypeMethod.doesReturn(SplitType::class.java) &&
                setSplitTypeMethod.isPublic && setLayoutDirectionMethod.isPublic
        }

    private fun isClassSplitTypeValid(): Boolean =
        validateReflection("Class SplitAttributes.SplitType is not valid") {
            val ratioSplitTypeClass = SplitType.RatioSplitType::class.java
            val ratioSplitTypeConstructor =
                ratioSplitTypeClass.getDeclaredConstructor(Float::class.java)
            val getRatioMethod = ratioSplitTypeClass.getMethod("getRatio")
            val splitEquallyMethod = ratioSplitTypeClass.getMethod("splitEqually")
            val hingeSplitTypeClass = SplitType.HingeSplitType::class.java
            val hingeSplitTypeConstructor =
                hingeSplitTypeClass.getDeclaredConstructor(SplitType::class.java)
            val getFallbackSplitTypeMethod =
                hingeSplitTypeClass.getMethod("getFallbackSplitType")
            val expandContainersSplitTypeClass = SplitType.ExpandContainersSplitType::class.java
            val expandContainersSplitTypeConstructor =
                expandContainersSplitTypeClass.getDeclaredConstructor()
            ratioSplitTypeConstructor.isPublic &&
                getRatioMethod.isPublic &&
                getRatioMethod.doesReturn(Float::class.java) &&
                hingeSplitTypeConstructor.isPublic &&
                splitEquallyMethod.isPublic &&
                splitEquallyMethod.doesReturn(SplitType.RatioSplitType::class.java) &&
                getFallbackSplitTypeMethod.isPublic &&
                getFallbackSplitTypeMethod.doesReturn(SplitType::class.java) &&
                expandContainersSplitTypeConstructor.isPublic
        }

    private fun isMethodSetSplitInfoCallbackJavaConsumerValid(): Boolean {
        return validateReflection("ActivityEmbeddingComponent#setSplitInfoCallback is not valid") {
            val consumerClass =
                consumerAdapter.consumerClassOrNull() ?: return@validateReflection false
            val setSplitInfoCallbackMethod =
                activityEmbeddingComponentClass.getMethod("setSplitInfoCallback", consumerClass)
            setSplitInfoCallbackMethod.isPublic
        }
    }

    private fun isClassActivityRuleValid(): Boolean =
        validateReflection("Class ActivityRule is not valid") {
            val activityRuleClass = ActivityRule::class.java
            val shouldAlwaysExpandMethod = activityRuleClass.getMethod("shouldAlwaysExpand")
            val activityRuleBuilderClass = ActivityRule.Builder::class.java
            val setShouldAlwaysExpandMethod = activityRuleBuilderClass.getMethod(
                "setShouldAlwaysExpand",
                Boolean::class.java
            )
            shouldAlwaysExpandMethod.isPublic &&
                shouldAlwaysExpandMethod.doesReturn(Boolean::class.java) &&
                setShouldAlwaysExpandMethod.isPublic
        }

    private fun isClassSplitInfoValid(): Boolean =
        validateReflection("Class SplitInfo is not valid") {
            val splitInfoClass = SplitInfo::class.java
            val getPrimaryActivityStackMethod =
                splitInfoClass.getMethod("getPrimaryActivityStack")
            val getSecondaryActivityStackMethod =
                splitInfoClass.getMethod("getSecondaryActivityStack")
            val getSplitRatioMethod = splitInfoClass.getMethod("getSplitRatio")
            getPrimaryActivityStackMethod.isPublic &&
                getPrimaryActivityStackMethod.doesReturn(ActivityStack::class.java) &&
                getSecondaryActivityStackMethod.isPublic &&
                getSecondaryActivityStackMethod.doesReturn(ActivityStack::class.java) &&
                getSplitRatioMethod.isPublic &&
                getSplitRatioMethod.doesReturn(Float::class.java)
        }

    private fun isClassSplitPairRuleValid(): Boolean =
        validateReflection("Class SplitPairRule is not valid") {
            val splitPairRuleClass = SplitPairRule::class.java
            val getFinishPrimaryWithSecondaryMethod =
                splitPairRuleClass.getMethod("getFinishPrimaryWithSecondary")
            val getFinishSecondaryWithPrimaryMethod =
                splitPairRuleClass.getMethod("getFinishSecondaryWithPrimary")
            val shouldClearTopMethod = splitPairRuleClass.getMethod("shouldClearTop")
            getFinishPrimaryWithSecondaryMethod.isPublic &&
                getFinishPrimaryWithSecondaryMethod.doesReturn(Int::class.java) &&
                getFinishSecondaryWithPrimaryMethod.isPublic &&
                getFinishSecondaryWithPrimaryMethod.doesReturn(Int::class.java) &&
                shouldClearTopMethod.isPublic &&
                shouldClearTopMethod.doesReturn(Boolean::class.java)
        }

    private fun isClassSplitPlaceholderRuleValid(): Boolean =
        validateReflection("Class SplitPlaceholderRule is not valid") {
            val splitPlaceholderRuleClass = SplitPlaceholderRule::class.java
            val getPlaceholderIntentMethod =
                splitPlaceholderRuleClass.getMethod("getPlaceholderIntent")
            val isStickyMethod = splitPlaceholderRuleClass.getMethod("isSticky")
            val getFinishPrimaryWithSecondaryMethod =
                splitPlaceholderRuleClass.getMethod("getFinishPrimaryWithSecondary")
            getPlaceholderIntentMethod.isPublic &&
                getPlaceholderIntentMethod.doesReturn(Intent::class.java) &&
                isStickyMethod.isPublic &&
                isStickyMethod.doesReturn(Boolean::class.java)
            getFinishPrimaryWithSecondaryMethod.isPublic &&
                getFinishPrimaryWithSecondaryMethod.doesReturn(Int::class.java)
        }

    private fun isMethodSetSplitInfoCallbackWindowConsumerValid(): Boolean {
        return validateReflection("ActivityEmbeddingComponent#setSplitInfoCallback is not valid") {
            val setSplitInfoCallbackMethod = activityEmbeddingComponentClass.getMethod(
                "setSplitInfoCallback",
                Consumer::class.java
            )
            setSplitInfoCallbackMethod.isPublic
        }
    }

    private fun isActivityEmbeddingComponentValid(): Boolean {
        return validateReflection("WindowExtensions#getActivityEmbeddingComponent is not valid") {
            val extensionsClass = safeWindowExtensionsProvider.windowExtensionsClass
            val getActivityEmbeddingComponentMethod =
                extensionsClass.getMethod("getActivityEmbeddingComponent")
            val activityEmbeddingComponentClass = activityEmbeddingComponentClass
            getActivityEmbeddingComponentMethod.isPublic &&
                getActivityEmbeddingComponentMethod.doesReturn(activityEmbeddingComponentClass)
        }
    }

    private val activityEmbeddingComponentClass: Class<*>
        get() {
            return loader.loadClass(ACTIVITY_EMBEDDING_COMPONENT_CLASS)
        }
}
