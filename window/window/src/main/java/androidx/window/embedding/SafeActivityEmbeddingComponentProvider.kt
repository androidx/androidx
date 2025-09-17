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
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.os.IBinder
import android.view.WindowMetrics
import androidx.annotation.VisibleForTesting
import androidx.window.SafeWindowExtensionsProvider
import androidx.window.WindowSdkExtensions
import androidx.window.core.ConsumerAdapter
import androidx.window.extensions.WindowExtensions
import androidx.window.extensions.core.util.function.Consumer
import androidx.window.extensions.core.util.function.Function
import androidx.window.extensions.core.util.function.Predicate
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import androidx.window.extensions.embedding.ActivityRule
import androidx.window.extensions.embedding.ActivityStack
import androidx.window.extensions.embedding.ActivityStackAttributes
import androidx.window.extensions.embedding.ActivityStackAttributesCalculatorParams
import androidx.window.extensions.embedding.AnimationBackground
import androidx.window.extensions.embedding.AnimationParams
import androidx.window.extensions.embedding.DividerAttributes
import androidx.window.extensions.embedding.EmbeddedActivityWindowInfo
import androidx.window.extensions.embedding.ParentContainerInfo
import androidx.window.extensions.embedding.SplitAttributes
import androidx.window.extensions.embedding.SplitAttributes.SplitType
import androidx.window.extensions.embedding.SplitAttributesCalculatorParams
import androidx.window.extensions.embedding.SplitInfo
import androidx.window.extensions.embedding.SplitPairRule
import androidx.window.extensions.embedding.SplitPinRule
import androidx.window.extensions.embedding.SplitPlaceholderRule
import androidx.window.extensions.embedding.SplitRule
import androidx.window.extensions.embedding.WindowAttributes
import androidx.window.extensions.layout.WindowLayoutInfo
import androidx.window.reflection.ReflectionUtils.doesReturn
import androidx.window.reflection.ReflectionUtils.isPublic
import androidx.window.reflection.ReflectionUtils.validateReflection
import androidx.window.reflection.WindowExtensionsConstants.ACTIVITY_EMBEDDING_COMPONENT_CLASS
import java.util.concurrent.Executor

/**
 * Reflection Guard for [ActivityEmbeddingComponent]. This will go through the
 * [ActivityEmbeddingComponent]'s method by reflection and check each method's name and signature to
 * see if the interface is what we required.
 */
internal class SafeActivityEmbeddingComponentProvider(
    private val loader: ClassLoader,
    private val consumerAdapter: ConsumerAdapter,
    private val windowExtensions: WindowExtensions,
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
        return when (WindowSdkExtensions.getInstance().extensionVersion) {
            1 -> hasValidVendorApiLevel1()
            2 -> hasValidVendorApiLevel2()
            in 3..4 -> hasValidVendorApiLevel3() // No additional API in 4.
            5 -> hasValidVendorApiLevel5()
            6 -> hasValidVendorApiLevel6()
            7 -> hasValidVendorApiLevel7()
            in 8..Int.MAX_VALUE -> hasValidVendorApiLevel8()
            else -> false
        }
    }

    @VisibleForTesting
    internal fun isActivityEmbeddingComponentAccessible(): Boolean =
        safeWindowExtensionsProvider.isWindowExtensionsValid() &&
            isActivityEmbeddingComponentValid()

    /**
     * Vendor API level 1 includes the following methods:
     * - [ActivityEmbeddingComponent.setEmbeddingRules]
     * - [ActivityEmbeddingComponent.isActivityEmbedded]
     * - [ActivityEmbeddingComponent.setSplitInfoCallback] with [java.util.function.Consumer]
     * - [SplitRule.getSplitRatio]
     * - [SplitRule.getLayoutDirection] and following classes:
     * - [ActivityRule]
     * - [ActivityRule.Builder]
     * - [SplitInfo]
     * - [SplitPairRule]
     * - [SplitPairRule.Builder]
     * - [SplitPlaceholderRule]
     * - [SplitPlaceholderRule.Builder]
     */
    @VisibleForTesting
    internal fun hasValidVendorApiLevel1(): Boolean {
        return isMethodSetEmbeddingRulesValid() &&
            isMethodIsActivityEmbeddedValid() &&
            isMethodSetSplitInfoCallbackJavaConsumerValid() &&
            isMethodGetSplitRatioValid() &&
            isMethodGetLayoutDirectionValid() &&
            isClassActivityRuleValid() &&
            isClassActivityRuleBuilderLevel1Valid() &&
            isClassSplitInfoValid() &&
            isClassSplitPairRuleValid() &&
            isClassSplitPairRuleBuilderLevel1Valid() &&
            isClassSplitPlaceholderRuleValid() &&
            isClassSplitPlaceholderRuleBuilderLevel1Valid()
    }

    /**
     * Vendor API level 2 includes the following methods:
     * - [ActivityEmbeddingComponent.setSplitInfoCallback] with [Consumer]
     * - [ActivityEmbeddingComponent.clearSplitInfoCallback]
     * - [ActivityEmbeddingComponent.setSplitAttributesCalculator]
     * - [ActivityEmbeddingComponent.clearSplitAttributesCalculator]
     * - [SplitInfo.getSplitAttributes]
     * - [SplitPlaceholderRule.getFinishPrimaryWithPlaceholder]
     * - [SplitRule.getDefaultSplitAttributes] and following classes:
     * - [ActivityRule.Builder]
     * - [EmbeddingRule]
     * - [SplitAttributes]
     * - [SplitAttributes.SplitType]
     * - [SplitAttributesCalculatorParams]
     * - [SplitPairRule.Builder]
     * - [SplitPlaceholderRule.Builder]
     */
    @VisibleForTesting
    internal fun hasValidVendorApiLevel2(): Boolean {
        return hasValidVendorApiLevel1() &&
            isMethodSetSplitInfoCallbackWindowConsumerValid() &&
            isMethodClearSplitInfoCallbackValid() &&
            isMethodSplitAttributesCalculatorValid() &&
            isMethodGetSplitAttributesValid() &&
            isMethodGetFinishPrimaryWithPlaceholderValid() &&
            isMethodGetDefaultSplitAttributesValid() &&
            isClassActivityRuleBuilderLevel2Valid() &&
            isClassEmbeddingRuleValid() &&
            isClassSplitAttributesValid() &&
            isClassSplitAttributesCalculatorParamsValid() &&
            isClassSplitTypeValid() &&
            isClassSplitPairRuleBuilderLevel2Valid() &&
            isClassSplitPlaceholderRuleBuilderLevel2Valid()
    }

    /**
     * Vendor API level 3 includes the following methods:
     * - [ActivityEmbeddingComponent.updateSplitAttributes]
     * - [ActivityEmbeddingComponent.invalidateTopVisibleSplitAttributes]
     * - [SplitInfo.getToken]
     */
    @VisibleForTesting
    internal fun hasValidVendorApiLevel3(): Boolean =
        hasValidVendorApiLevel2() &&
            isMethodInvalidateTopVisibleSplitAttributesValid() &&
            isMethodUpdateSplitAttributesValid() &&
            isMethodSplitInfoGetTokenValid()

    /**
     * Vendor API level 5 includes the following methods:
     * - [ActivityEmbeddingComponent.registerActivityStackCallback]
     * - [ActivityEmbeddingComponent.unregisterActivityStackCallback]
     * - [ActivityStack.getActivityStackToken]
     * - [ActivityStack.Token.createFromBinder]
     * - [ActivityStack.Token.readFromBundle]
     * - [ActivityStack.Token.toBundle]
     * - [ActivityStack.Token.INVALID_ACTIVITY_STACK_TOKEN]
     * - [AnimationBackground.createColorBackground]
     * - [AnimationBackground.ANIMATION_BACKGROUND_DEFAULT]
     * - [SplitAttributes.getAnimationBackground]
     * - [SplitAttributes.Builder.setAnimationBackground]
     * - [WindowAttributes.getDimAreaBehavior]
     * - [SplitAttributes.getWindowAttributes]
     * - [SplitAttributes.Builder.setWindowAttributes]
     * - [SplitPinRule.isSticky]
     * - [ActivityEmbeddingComponent.pinTopActivityStack]
     * - [ActivityEmbeddingComponent.unpinTopActivityStack]
     * - [ActivityEmbeddingComponent.updateSplitAttributes] with [SplitInfo.Token]
     * - [SplitInfo.getSplitInfoToken] and following classes:
     * - [AnimationBackground]
     * - [ActivityStack.Token]
     * - [WindowAttributes]
     * - [SplitInfo.Token]
     */
    @VisibleForTesting
    internal fun hasValidVendorApiLevel5(): Boolean =
        hasValidVendorApiLevel3() &&
            isActivityStackGetActivityStackTokenValid() &&
            isMethodRegisterActivityStackCallbackValid() &&
            isMethodUnregisterActivityStackCallbackValid() &&
            isMethodPinUnpinTopActivityStackValid() &&
            isMethodUpdateSplitAttributesWithTokenValid() &&
            isMethodGetSplitInfoTokenValid() &&
            isClassAnimationBackgroundValid() &&
            isClassActivityStackTokenValid() &&
            isClassWindowAttributesValid() &&
            isClassSplitInfoTokenValid()

    /**
     * Vendor API level 6 includes the following methods:
     * - [ActivityEmbeddingComponent.clearEmbeddedActivityWindowInfoCallback]
     * - [ActivityEmbeddingComponent.getEmbeddedActivityWindowInfo]
     * - [ActivityEmbeddingComponent.setEmbeddedActivityWindowInfoCallback]
     * - [SplitAttributes.getDividerAttributes]
     * - [SplitAttributes.Builder.setDividerAttributes] and following classes:
     * - [EmbeddedActivityWindowInfo]
     * - [DividerAttributes]
     * - [DividerAttributes.Builder]
     */
    @VisibleForTesting
    internal fun hasValidVendorApiLevel6(): Boolean =
        hasValidVendorApiLevel5() &&
            isMethodGetEmbeddedActivityWindowInfoValid() &&
            isMethodSetEmbeddedActivityWindowInfoCallbackValid() &&
            isMethodClearEmbeddedActivityWindowInfoCallbackValid() &&
            isMethodGetDividerAttributesValid() &&
            isMethodSetDividerAttributesValid() &&
            isClassEmbeddedActivityWindowInfoValid() &&
            isClassDividerAttributesValid() &&
            isClassDividerAttributesBuilderValid()

    /**
     * Vendor API level 7 includes the following methods:
     * - [SplitAttributes.getAnimationParams]
     * - [SplitAttributes.Builder.setAnimationParams]
     * - [DividerAttributes.isDraggingToFullscreenAllowed]
     * - [DividerAttributes.Builder.setDraggingToFullscreenAllowed] and following classes:
     * - [AnimationParams]
     * - [AnimationParams.Builder]
     */
    @VisibleForTesting
    internal fun hasValidVendorApiLevel7(): Boolean =
        hasValidVendorApiLevel6() &&
            isMethodGetAnimationParamsValid() &&
            isMethodSetAnimationParamsValid() &&
            isMethodIsDraggingToFullscreenAllowedValid() &&
            isMethodSetDraggingToFullscreenAllowedValid() &&
            isClassAnimationParamsValid() &&
            isClassAnimationParamsBuilderValid()

    /**
     * Vendor API level 8 includes the following methods:
     * - [EmbeddingConfiguration.Builder.setAutoSaveEmbeddingState]
     */
    @VisibleForTesting
    internal fun hasValidVendorApiLevel8(): Boolean =
        // TODO(b/289875940): adding #isClassEmbeddingConfigurationBuilderApi8Valid() when API
        //                    finalized.
        hasValidVendorApiLevel7()

    /**
     * Overlay features includes the following methods:
     * - [ActivityEmbeddingComponent.clearActivityStackAttributesCalculator]
     * - [ActivityEmbeddingComponent.getActivityStackToken]
     * - [ActivityEmbeddingComponent.getParentContainerInfo]
     * - [ActivityEmbeddingComponent.setActivityStackAttributesCalculator]
     * - [ActivityEmbeddingComponent.updateActivityStackAttributes]
     * - [ActivityStack.getTag] and following classes:
     * - [ParentContainerInfo]
     * - [ActivityStackAttributes]
     * - [ActivityStackAttributes.Builder]
     * - [ActivityStackAttributesCalculatorParams]
     */
    private fun isOverlayFeatureValid(): Boolean =
        isActivityStackGetTagValid() &&
            isMethodGetActivityStackTokenValid() &&
            isClassParentContainerInfoValid() &&
            isMethodGetParentContainerInfoValid() &&
            isMethodSetActivityStackAttributesCalculatorValid() &&
            isMethodClearActivityStackAttributesCalculatorValid() &&
            isMethodUpdateActivityStackAttributesValid() &&
            isClassActivityStackAttributesValid() &&
            isClassActivityStackAttributesBuilderValid() &&
            isClassActivityStackAttributesCalculatorParamsValid()

    private val activityEmbeddingComponentClass: Class<*>
        get() {
            return loader.loadClass(ACTIVITY_EMBEDDING_COMPONENT_CLASS)
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

    /** Vendor API level 1 validation methods */
    private fun isMethodSetEmbeddingRulesValid(): Boolean {
        return validateReflection("ActivityEmbeddingComponent#setEmbeddingRules is not valid") {
            val setEmbeddingRulesMethod =
                activityEmbeddingComponentClass.getMethod("setEmbeddingRules", Set::class.java)
            setEmbeddingRulesMethod.isPublic
        }
    }

    private fun isMethodIsActivityEmbeddedValid(): Boolean {
        return validateReflection("ActivityEmbeddingComponent#isActivityEmbedded is not valid") {
            val isActivityEmbeddedMethod =
                activityEmbeddingComponentClass.getMethod(
                    "isActivityEmbedded",
                    Activity::class.java,
                )
            isActivityEmbeddedMethod.isPublic &&
                isActivityEmbeddedMethod.doesReturn(Boolean::class.java)
        }
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

    private fun isMethodGetSplitRatioValid(): Boolean =
        validateReflection("SplitRule#getSplitRatio is not valid") {
            val splitRuleClass = SplitRule::class.java
            val getSplitRatioMethod = splitRuleClass.getMethod("getSplitRatio")
            getSplitRatioMethod.isPublic && getSplitRatioMethod.doesReturn(Float::class.java)
        }

    private fun isMethodGetLayoutDirectionValid(): Boolean =
        validateReflection("SplitRule#getLayoutDirection is not valid") {
            val splitRuleClass = SplitRule::class.java
            val getLayoutDirectionMethod = splitRuleClass.getMethod("getLayoutDirection")
            getLayoutDirectionMethod.isPublic &&
                getLayoutDirectionMethod.doesReturn(Int::class.java)
        }

    private fun isClassActivityRuleValid(): Boolean =
        validateReflection("Class ActivityRule is not valid") {
            val activityRuleClass = ActivityRule::class.java
            val shouldAlwaysExpandMethod = activityRuleClass.getMethod("shouldAlwaysExpand")
            shouldAlwaysExpandMethod.isPublic &&
                shouldAlwaysExpandMethod.doesReturn(Boolean::class.java)
        }

    private fun isClassActivityRuleBuilderLevel1Valid(): Boolean =
        validateReflection("Class ActivityRule.Builder is not valid") {
            val activityRuleBuilderClass = ActivityRule.Builder::class.java
            val setShouldAlwaysExpandMethod =
                activityRuleBuilderClass.getMethod("setShouldAlwaysExpand", Boolean::class.java)
            setShouldAlwaysExpandMethod.isPublic &&
                setShouldAlwaysExpandMethod.doesReturn(ActivityRule.Builder::class.java)
        }

    private fun isClassSplitInfoValid(): Boolean =
        validateReflection("Class SplitInfo is not valid") {
            val splitInfoClass = SplitInfo::class.java
            val getPrimaryActivityStackMethod = splitInfoClass.getMethod("getPrimaryActivityStack")
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

    private fun isClassSplitPairRuleBuilderLevel1Valid(): Boolean =
        validateReflection("Class SplitPairRule.Builder is not valid") {
            val splitPairRuleBuilderClass = SplitPairRule.Builder::class.java
            val setSplitRatioMethod =
                splitPairRuleBuilderClass.getMethod("setSplitRatio", Float::class.java)
            val setLayoutDirectionMethod =
                splitPairRuleBuilderClass.getMethod("setLayoutDirection", Int::class.java)
            setSplitRatioMethod.isPublic &&
                setSplitRatioMethod.doesReturn(SplitPairRule.Builder::class.java) &&
                setLayoutDirectionMethod.isPublic &&
                setLayoutDirectionMethod.doesReturn(SplitPairRule.Builder::class.java)
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
                isStickyMethod.doesReturn(Boolean::class.java) &&
                getFinishPrimaryWithSecondaryMethod.isPublic &&
                getFinishPrimaryWithSecondaryMethod.doesReturn(Int::class.java)
        }

    private fun isClassSplitPlaceholderRuleBuilderLevel1Valid(): Boolean =
        validateReflection("Class SplitPlaceholderRule.Builder is not valid") {
            val splitPlaceholderRuleBuilderClass = SplitPlaceholderRule.Builder::class.java
            val setSplitRatioMethod =
                splitPlaceholderRuleBuilderClass.getMethod("setSplitRatio", Float::class.java)
            val setLayoutDirectionMethod =
                splitPlaceholderRuleBuilderClass.getMethod("setLayoutDirection", Int::class.java)
            val setStickyMethod =
                splitPlaceholderRuleBuilderClass.getMethod("setSticky", Boolean::class.java)
            val setFinishPrimaryWithSecondaryMethod =
                splitPlaceholderRuleBuilderClass.getMethod(
                    "setFinishPrimaryWithSecondary",
                    Int::class.java,
                )
            setSplitRatioMethod.isPublic &&
                setSplitRatioMethod.doesReturn(SplitPlaceholderRule.Builder::class.java) &&
                setLayoutDirectionMethod.isPublic &&
                setLayoutDirectionMethod.doesReturn(SplitPlaceholderRule.Builder::class.java) &&
                setStickyMethod.isPublic &&
                setStickyMethod.doesReturn(SplitPlaceholderRule.Builder::class.java) &&
                setFinishPrimaryWithSecondaryMethod.isPublic &&
                setFinishPrimaryWithSecondaryMethod.doesReturn(
                    SplitPlaceholderRule.Builder::class.java
                )
        }

    /** Vendor API level 2 validation methods */
    private fun isMethodSetSplitInfoCallbackWindowConsumerValid(): Boolean {
        return validateReflection("ActivityEmbeddingComponent#setSplitInfoCallback is not valid") {
            val setSplitInfoCallbackMethod =
                activityEmbeddingComponentClass.getMethod(
                    "setSplitInfoCallback",
                    Consumer::class.java,
                )
            setSplitInfoCallbackMethod.isPublic
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
            val setSplitAttributesCalculatorMethod =
                activityEmbeddingComponentClass.getMethod(
                    "setSplitAttributesCalculator",
                    Function::class.java,
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

    private fun isMethodGetFinishPrimaryWithPlaceholderValid(): Boolean =
        validateReflection("SplitPlaceholderRule#getFinishPrimaryWithPlaceholder is not valid") {
            val splitPlaceholderRuleClass = SplitPlaceholderRule::class.java
            val getFinishPrimaryWithPlaceholderMethod =
                splitPlaceholderRuleClass.getMethod("getFinishPrimaryWithPlaceholder")
            getFinishPrimaryWithPlaceholderMethod.isPublic &&
                getFinishPrimaryWithPlaceholderMethod.doesReturn(Int::class.java)
        }

    private fun isMethodGetDefaultSplitAttributesValid(): Boolean =
        validateReflection("SplitRule#getDefaultSplitAttributes is not valid") {
            val splitRuleClass = SplitRule::class.java
            val getDefaultSplitAttributesMethod =
                splitRuleClass.getMethod("getDefaultSplitAttributes")
            getDefaultSplitAttributesMethod.isPublic &&
                getDefaultSplitAttributesMethod.doesReturn(SplitAttributes::class.java)
        }

    private fun isClassActivityRuleBuilderLevel2Valid(): Boolean =
        validateReflection("Class ActivityRule.Builder is not valid") {
            val activityRuleBuilderClass = ActivityRule.Builder::class.java
            val activityRuleBuilderConstructor =
                activityRuleBuilderClass.getDeclaredConstructor(
                    Predicate::class.java,
                    Predicate::class.java,
                )
            val setTagMethod = activityRuleBuilderClass.getMethod("setTag", String::class.java)
            activityRuleBuilderConstructor.isPublic &&
                setTagMethod.isPublic &&
                setTagMethod.doesReturn(ActivityRule.Builder::class.java)
        }

    private fun isClassEmbeddingRuleValid(): Boolean =
        validateReflection("Class EmbeddingRule is not valid") {
            val embeddingRuleClass = EmbeddingRule::class.java
            val getTagMethod = embeddingRuleClass.getMethod("getTag")
            getTagMethod.isPublic && getTagMethod.doesReturn(String::class.java)
        }

    private fun isClassSplitAttributesValid(): Boolean =
        validateReflection("Class SplitAttributes is not valid") {
            val splitAttributesClass = SplitAttributes::class.java
            val getLayoutDirectionMethod = splitAttributesClass.getMethod("getLayoutDirection")
            val getSplitTypeMethod = splitAttributesClass.getMethod("getSplitType")
            val splitAttributesBuilderClass = SplitAttributes.Builder::class.java
            val setSplitTypeMethod =
                splitAttributesBuilderClass.getMethod("setSplitType", SplitType::class.java)
            val setLayoutDirectionMethod =
                splitAttributesBuilderClass.getMethod("setLayoutDirection", Int::class.java)
            getLayoutDirectionMethod.isPublic &&
                getLayoutDirectionMethod.doesReturn(Int::class.java) &&
                getSplitTypeMethod.isPublic &&
                getSplitTypeMethod.doesReturn(SplitType::class.java) &&
                setSplitTypeMethod.isPublic &&
                setLayoutDirectionMethod.isPublic
        }

    @Suppress("newApi") // Suppress lint check for WindowMetrics
    private fun isClassSplitAttributesCalculatorParamsValid(): Boolean =
        validateReflection("Class SplitAttributesCalculatorParams is not valid") {
            val splitAttributesCalculatorParamsClass = SplitAttributesCalculatorParams::class.java
            val getParentWindowMetricsMethod =
                splitAttributesCalculatorParamsClass.getMethod("getParentWindowMetrics")
            val getParentConfigurationMethod =
                splitAttributesCalculatorParamsClass.getMethod("getParentConfiguration")
            val getDefaultSplitAttributesMethod =
                splitAttributesCalculatorParamsClass.getMethod("getDefaultSplitAttributes")
            val areDefaultConstraintsSatisfiedMethod =
                splitAttributesCalculatorParamsClass.getMethod("areDefaultConstraintsSatisfied")
            val getParentWindowLayoutInfoMethod =
                splitAttributesCalculatorParamsClass.getMethod("getParentWindowLayoutInfo")
            val getSplitRuleTagMethod =
                splitAttributesCalculatorParamsClass.getMethod("getSplitRuleTag")
            getParentWindowMetricsMethod.isPublic &&
                getParentWindowMetricsMethod.doesReturn(WindowMetrics::class.java) &&
                getParentConfigurationMethod.isPublic &&
                getParentConfigurationMethod.doesReturn(Configuration::class.java) &&
                getDefaultSplitAttributesMethod.isPublic &&
                getDefaultSplitAttributesMethod.doesReturn(SplitAttributes::class.java) &&
                areDefaultConstraintsSatisfiedMethod.isPublic &&
                areDefaultConstraintsSatisfiedMethod.doesReturn(Boolean::class.java) &&
                getParentWindowLayoutInfoMethod.isPublic &&
                getParentWindowLayoutInfoMethod.doesReturn(WindowLayoutInfo::class.java) &&
                getSplitRuleTagMethod.isPublic &&
                getSplitRuleTagMethod.doesReturn(String::class.java)
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
            val getFallbackSplitTypeMethod = hingeSplitTypeClass.getMethod("getFallbackSplitType")
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

    private fun isClassSplitPairRuleBuilderLevel2Valid(): Boolean =
        validateReflection("Class SplitPairRule.Builder is not valid") {
            val splitPairRuleBuilderClass = SplitPairRule.Builder::class.java
            val splitPairRuleBuilderConstructor =
                splitPairRuleBuilderClass.getDeclaredConstructor(
                    Predicate::class.java,
                    Predicate::class.java,
                    Predicate::class.java,
                )
            val setDefaultSplitAttributesMethod =
                splitPairRuleBuilderClass.getMethod(
                    "setDefaultSplitAttributes",
                    SplitAttributes::class.java,
                )
            val setTagMethod = splitPairRuleBuilderClass.getMethod("setTag", String::class.java)
            splitPairRuleBuilderConstructor.isPublic &&
                setDefaultSplitAttributesMethod.isPublic &&
                setDefaultSplitAttributesMethod.doesReturn(SplitPairRule.Builder::class.java) &&
                setTagMethod.isPublic &&
                setTagMethod.doesReturn(SplitPairRule.Builder::class.java)
        }

    private fun isClassSplitPlaceholderRuleBuilderLevel2Valid(): Boolean =
        validateReflection("Class SplitPlaceholderRule.Builder is not valid") {
            val splitPlaceholderRuleBuilderClass = SplitPlaceholderRule.Builder::class.java
            val splitPlaceholderRuleBuilderConstructor =
                splitPlaceholderRuleBuilderClass.getDeclaredConstructor(
                    Intent::class.java,
                    Predicate::class.java,
                    Predicate::class.java,
                    Predicate::class.java,
                )
            val setDefaultSplitAttributesMethod =
                splitPlaceholderRuleBuilderClass.getMethod(
                    "setDefaultSplitAttributes",
                    SplitAttributes::class.java,
                )
            val setFinishPrimaryWithPlaceholderMethod =
                splitPlaceholderRuleBuilderClass.getMethod(
                    "setFinishPrimaryWithPlaceholder",
                    Int::class.java,
                )
            val setTagMethod =
                splitPlaceholderRuleBuilderClass.getMethod("setTag", String::class.java)
            splitPlaceholderRuleBuilderConstructor.isPublic &&
                setDefaultSplitAttributesMethod.isPublic &&
                setDefaultSplitAttributesMethod.doesReturn(
                    SplitPlaceholderRule.Builder::class.java
                ) &&
                setFinishPrimaryWithPlaceholderMethod.isPublic &&
                setFinishPrimaryWithPlaceholderMethod.doesReturn(
                    SplitPlaceholderRule.Builder::class.java
                ) &&
                setTagMethod.isPublic &&
                setTagMethod.doesReturn(SplitPlaceholderRule.Builder::class.java)
        }

    /** Vendor API level 3 validation methods */
    private fun isMethodInvalidateTopVisibleSplitAttributesValid(): Boolean =
        validateReflection("#invalidateTopVisibleSplitAttributes is not valid") {
            val invalidateTopVisibleSplitAttributesMethod =
                activityEmbeddingComponentClass.getMethod("invalidateTopVisibleSplitAttributes")
            invalidateTopVisibleSplitAttributesMethod.isPublic
        }

    private fun isMethodUpdateSplitAttributesValid(): Boolean =
        validateReflection("#updateSplitAttributes is not valid") {
            val updateSplitAttributesMethod =
                activityEmbeddingComponentClass.getMethod(
                    "updateSplitAttributes",
                    IBinder::class.java,
                    SplitAttributes::class.java,
                )
            updateSplitAttributesMethod.isPublic
        }

    private fun isMethodSplitInfoGetTokenValid(): Boolean =
        validateReflection("SplitInfo#getToken is not valid") {
            val splitInfoClass = SplitInfo::class.java
            val getTokenMethod = splitInfoClass.getMethod("getToken")
            getTokenMethod.isPublic && getTokenMethod.doesReturn(IBinder::class.java)
        }

    /** Vendor API level 5 validation methods */
    private fun isActivityStackGetActivityStackTokenValid(): Boolean =
        validateReflection("ActivityStack#getActivityToken is not valid") {
            val activityStackClass = ActivityStack::class.java
            val getActivityStackTokenMethod = activityStackClass.getMethod("getActivityStackToken")

            getActivityStackTokenMethod.isPublic &&
                getActivityStackTokenMethod.doesReturn(ActivityStack.Token::class.java)
        }

    private fun isMethodRegisterActivityStackCallbackValid(): Boolean =
        validateReflection("registerActivityStackCallback is not valid") {
            val registerActivityStackCallbackMethod =
                activityEmbeddingComponentClass.getMethod(
                    "registerActivityStackCallback",
                    Executor::class.java,
                    Consumer::class.java,
                )
            registerActivityStackCallbackMethod.isPublic
        }

    private fun isMethodUnregisterActivityStackCallbackValid(): Boolean =
        validateReflection("unregisterActivityStackCallback is not valid") {
            val unregisterActivityStackCallbackMethod =
                activityEmbeddingComponentClass.getMethod(
                    "unregisterActivityStackCallback",
                    Consumer::class.java,
                )
            unregisterActivityStackCallbackMethod.isPublic
        }

    private fun isMethodPinUnpinTopActivityStackValid(): Boolean =
        validateReflection("#pin(unPin)TopActivityStack is not valid") {
            val splitPinRuleClass = SplitPinRule::class.java
            val isStickyMethod = splitPinRuleClass.getMethod("isSticky")
            val pinTopActivityStackMethod =
                activityEmbeddingComponentClass.getMethod(
                    "pinTopActivityStack",
                    Int::class.java,
                    SplitPinRule::class.java,
                )
            val unpinTopActivityStackMethod =
                activityEmbeddingComponentClass.getMethod("unpinTopActivityStack", Int::class.java)
            isStickyMethod.isPublic &&
                isStickyMethod.doesReturn(Boolean::class.java) &&
                pinTopActivityStackMethod.isPublic &&
                pinTopActivityStackMethod.doesReturn(Boolean::class.java) &&
                unpinTopActivityStackMethod.isPublic
        }

    private fun isMethodUpdateSplitAttributesWithTokenValid(): Boolean =
        validateReflection("updateSplitAttributes is not valid") {
            val updateSplitAttributesMethod =
                activityEmbeddingComponentClass.getMethod(
                    "updateSplitAttributes",
                    SplitInfo.Token::class.java,
                    SplitAttributes::class.java,
                )
            updateSplitAttributesMethod.isPublic
        }

    private fun isMethodGetSplitInfoTokenValid(): Boolean =
        validateReflection("SplitInfo#getSplitInfoToken is not valid") {
            val splitInfoClass = SplitInfo::class.java
            val getSplitInfoToken = splitInfoClass.getMethod("getSplitInfoToken")
            getSplitInfoToken.isPublic && getSplitInfoToken.doesReturn(SplitInfo.Token::class.java)
        }

    private fun isClassAnimationBackgroundValid(): Boolean =
        validateReflection("Class AnimationBackground is not valid") {
            val animationBackgroundClass = AnimationBackground::class.java
            val colorBackgroundClass = AnimationBackground.ColorBackground::class.java
            val createColorBackgroundMethod =
                animationBackgroundClass.getMethod("createColorBackground", Int::class.java)
            val animationBackgroundDefaultField =
                animationBackgroundClass.getDeclaredField("ANIMATION_BACKGROUND_DEFAULT")
            val colorBackgroundGetColor = colorBackgroundClass.getMethod("getColor")

            val splitAttributesClass = SplitAttributes::class.java
            val getAnimationBackgroundMethod =
                splitAttributesClass.getMethod("getAnimationBackground")

            val splitAttributesBuilderClass = SplitAttributes.Builder::class.java
            val setAnimationBackgroundMethod =
                splitAttributesBuilderClass.getMethod(
                    "setAnimationBackground",
                    AnimationBackground::class.java,
                )

            createColorBackgroundMethod.isPublic &&
                createColorBackgroundMethod.doesReturn(colorBackgroundClass) &&
                animationBackgroundDefaultField.isPublic &&
                colorBackgroundGetColor.isPublic &&
                colorBackgroundGetColor.doesReturn(Int::class.java) &&
                getAnimationBackgroundMethod.isPublic &&
                getAnimationBackgroundMethod.doesReturn(animationBackgroundClass) &&
                setAnimationBackgroundMethod.isPublic &&
                setAnimationBackgroundMethod.doesReturn(SplitAttributes.Builder::class.java)
        }

    private fun isClassActivityStackTokenValid(): Boolean =
        validateReflection("Class ActivityStack.Token is not valid") {
            val activityStackTokenClass = ActivityStack.Token::class.java
            val toBundleMethod = activityStackTokenClass.getMethod("toBundle")
            val readFromBundle =
                activityStackTokenClass.getMethod("readFromBundle", Bundle::class.java)
            val createFromBinder =
                activityStackTokenClass.getMethod("createFromBinder", IBinder::class.java)
            val invalidActivityStackTokenField =
                activityStackTokenClass.getDeclaredField("INVALID_ACTIVITY_STACK_TOKEN")

            toBundleMethod.isPublic &&
                toBundleMethod.doesReturn(Bundle::class.java) &&
                readFromBundle.isPublic &&
                readFromBundle.doesReturn(activityStackTokenClass) &&
                createFromBinder.isPublic &&
                createFromBinder.doesReturn(activityStackTokenClass) &&
                invalidActivityStackTokenField.isPublic
        }

    private fun isClassWindowAttributesValid(): Boolean =
        validateReflection("Class WindowAttributes is not valid") {
            val windowAttributesClass = WindowAttributes::class.java
            val getDimAreaBehaviorMethod = windowAttributesClass.getMethod("getDimAreaBehavior")

            val splitAttributesClass = SplitAttributes::class.java
            val getWindowAttributesMethod = splitAttributesClass.getMethod("getWindowAttributes")

            val splitAttributesBuilderClass = SplitAttributes.Builder::class.java
            val setWindowAttributesMethod =
                splitAttributesBuilderClass.getMethod(
                    "setWindowAttributes",
                    WindowAttributes::class.java,
                )

            getDimAreaBehaviorMethod.isPublic &&
                getDimAreaBehaviorMethod.doesReturn(Int::class.java) &&
                getWindowAttributesMethod.isPublic &&
                getWindowAttributesMethod.doesReturn(windowAttributesClass) &&
                setWindowAttributesMethod.isPublic &&
                setWindowAttributesMethod.doesReturn(SplitAttributes.Builder::class.java)
        }

    private fun isClassSplitInfoTokenValid(): Boolean =
        validateReflection("SplitInfo.Token is not valid") {
            val splitInfoTokenClass = SplitInfo.Token::class.java
            val createFromBinder =
                splitInfoTokenClass.getMethod("createFromBinder", IBinder::class.java)

            createFromBinder.isPublic && createFromBinder.doesReturn(splitInfoTokenClass)
        }

    /** Vendor API level 6 validation methods */
    private fun isMethodGetEmbeddedActivityWindowInfoValid(): Boolean =
        validateReflection(
            "ActivityEmbeddingComponent#getEmbeddedActivityWindowInfo is not valid"
        ) {
            val getEmbeddedActivityWindowInfoMethod =
                activityEmbeddingComponentClass.getMethod(
                    "getEmbeddedActivityWindowInfo",
                    Activity::class.java,
                )
            getEmbeddedActivityWindowInfoMethod.isPublic &&
                getEmbeddedActivityWindowInfoMethod.doesReturn(
                    EmbeddedActivityWindowInfo::class.java
                )
        }

    private fun isMethodSetEmbeddedActivityWindowInfoCallbackValid(): Boolean =
        validateReflection(
            "ActivityEmbeddingComponent#setEmbeddedActivityWindowInfoCallback is not valid"
        ) {
            val setEmbeddedActivityWindowInfoCallbackMethod =
                activityEmbeddingComponentClass.getMethod(
                    "setEmbeddedActivityWindowInfoCallback",
                    Executor::class.java,
                    Consumer::class.java,
                )
            setEmbeddedActivityWindowInfoCallbackMethod.isPublic
        }

    private fun isMethodClearEmbeddedActivityWindowInfoCallbackValid(): Boolean =
        validateReflection(
            "ActivityEmbeddingComponent#clearEmbeddedActivityWindowInfoCallback is not valid"
        ) {
            val clearEmbeddedActivityWindowInfoCallbackMethod =
                activityEmbeddingComponentClass.getMethod("clearEmbeddedActivityWindowInfoCallback")
            clearEmbeddedActivityWindowInfoCallbackMethod.isPublic
        }

    private fun isMethodGetDividerAttributesValid(): Boolean =
        validateReflection("SplitAttributes#getDividerAttributes is not valid") {
            val splitAttributesClass = SplitAttributes::class.java
            val getDividerAttributesMethod = splitAttributesClass.getMethod("getDividerAttributes")
            getDividerAttributesMethod.isPublic &&
                getDividerAttributesMethod.doesReturn(DividerAttributes::class.java)
        }

    private fun isMethodSetDividerAttributesValid(): Boolean =
        validateReflection("SplitAttributes#setDividerAttributes is not valid") {
            val splitAttributesBuilderClass = SplitAttributes.Builder::class.java
            val setDividerAttributesMethod =
                splitAttributesBuilderClass.getMethod(
                    "setDividerAttributes",
                    DividerAttributes::class.java,
                )
            setDividerAttributesMethod.isPublic &&
                setDividerAttributesMethod.doesReturn(SplitAttributes.Builder::class.java)
        }

    private fun isClassEmbeddedActivityWindowInfoValid(): Boolean =
        validateReflection("Class EmbeddedActivityWindowInfo is not valid") {
            val embeddedActivityWindowInfoClass = EmbeddedActivityWindowInfo::class.java
            val getActivityMethod = embeddedActivityWindowInfoClass.getMethod("getActivity")
            val isEmbeddedMethod = embeddedActivityWindowInfoClass.getMethod("isEmbedded")
            val getTaskBoundsMethod = embeddedActivityWindowInfoClass.getMethod("getTaskBounds")
            val getActivityStackBoundsMethod =
                embeddedActivityWindowInfoClass.getMethod("getActivityStackBounds")
            getActivityMethod.isPublic &&
                getActivityMethod.doesReturn(Activity::class.java) &&
                isEmbeddedMethod.isPublic &&
                isEmbeddedMethod.doesReturn(Boolean::class.java) &&
                getTaskBoundsMethod.isPublic &&
                getTaskBoundsMethod.doesReturn(Rect::class.java) &&
                getActivityStackBoundsMethod.isPublic &&
                getActivityStackBoundsMethod.doesReturn(Rect::class.java)
        }

    private fun isClassDividerAttributesValid(): Boolean =
        validateReflection("Class DividerAttributes is not valid") {
            val dividerAttributesClass = DividerAttributes::class.java
            val getDividerTypeMethod = dividerAttributesClass.getMethod("getDividerType")
            val getWidthDpMethod = dividerAttributesClass.getMethod("getWidthDp")
            val getPrimaryMinRatioMethod = dividerAttributesClass.getMethod("getPrimaryMinRatio")
            val getPrimaryMaxRatioMethod = dividerAttributesClass.getMethod("getPrimaryMaxRatio")
            val getDividerColorMethod = dividerAttributesClass.getMethod("getDividerColor")
            getDividerTypeMethod.isPublic &&
                getDividerTypeMethod.doesReturn(Int::class.java) &&
                getWidthDpMethod.isPublic &&
                getWidthDpMethod.doesReturn(Int::class.java) &&
                getPrimaryMinRatioMethod.isPublic &&
                getPrimaryMinRatioMethod.doesReturn(Float::class.java) &&
                getPrimaryMaxRatioMethod.isPublic &&
                getPrimaryMaxRatioMethod.doesReturn(Float::class.java) &&
                getDividerColorMethod.isPublic &&
                getDividerColorMethod.doesReturn(Int::class.java)
        }

    private fun isClassDividerAttributesBuilderValid(): Boolean =
        validateReflection("Class DividerAttributes.Builder is not valid") {
            val dividerAttributesBuilderClass = DividerAttributes.Builder::class.java
            val dividerAttributesTypeBuilderConstructor =
                dividerAttributesBuilderClass.getDeclaredConstructor(Int::class.java)
            val dividerAttributesBuilderConstructor =
                dividerAttributesBuilderClass.getDeclaredConstructor(DividerAttributes::class.java)
            val setWidthDpMethod =
                dividerAttributesBuilderClass.getMethod("setWidthDp", Int::class.java)
            val setPrimaryMinRatioMethod =
                dividerAttributesBuilderClass.getMethod("setPrimaryMinRatio", Float::class.java)
            val setPrimaryMaxRatioMethod =
                dividerAttributesBuilderClass.getMethod("setPrimaryMaxRatio", Float::class.java)
            val setDividerColorMethod =
                dividerAttributesBuilderClass.getMethod("setDividerColor", Int::class.java)
            dividerAttributesTypeBuilderConstructor.isPublic &&
                dividerAttributesBuilderConstructor.isPublic &&
                setWidthDpMethod.isPublic &&
                setWidthDpMethod.doesReturn(DividerAttributes.Builder::class.java) &&
                setPrimaryMinRatioMethod.isPublic &&
                setPrimaryMinRatioMethod.doesReturn(DividerAttributes.Builder::class.java) &&
                setPrimaryMaxRatioMethod.isPublic &&
                setPrimaryMaxRatioMethod.doesReturn(DividerAttributes.Builder::class.java) &&
                setDividerColorMethod.isPublic &&
                setDividerColorMethod.doesReturn(DividerAttributes.Builder::class.java)
        }

    /** Vendor API level 7 validation methods */
    private fun isMethodGetAnimationParamsValid(): Boolean =
        validateReflection("SplitAttributes#getAnimationParams is not valid") {
            val splitAttributesClass = SplitAttributes::class.java
            val getAnimationParamsMethod = splitAttributesClass.getMethod("getAnimationParams")
            getAnimationParamsMethod.isPublic &&
                getAnimationParamsMethod.doesReturn(AnimationParams::class.java)
        }

    private fun isMethodSetAnimationParamsValid(): Boolean =
        validateReflection("SplitAttributes#setAnimationParams is not valid") {
            val splitAttributesBuilderClass = SplitAttributes.Builder::class.java
            val setAnimationParamsMethod =
                splitAttributesBuilderClass.getMethod(
                    "setAnimationParams",
                    AnimationParams::class.java,
                )
            setAnimationParamsMethod.isPublic &&
                setAnimationParamsMethod.doesReturn(SplitAttributes.Builder::class.java)
        }

    private fun isMethodIsDraggingToFullscreenAllowedValid(): Boolean =
        validateReflection("DividerAttributes#isDraggingToFullscreenAllowed is not valid") {
            val dividerAttributesClass = DividerAttributes::class.java
            val getDividerTypeMethod =
                dividerAttributesClass.getMethod("isDraggingToFullscreenAllowed")
            getDividerTypeMethod.isPublic && getDividerTypeMethod.doesReturn(Boolean::class.java)
        }

    private fun isMethodSetDraggingToFullscreenAllowedValid(): Boolean =
        validateReflection(
            "DividerAttributes.Builder#setDraggingToFullscreenAllowed is not valid"
        ) {
            val dividerAttributesBuilderClass = DividerAttributes.Builder::class.java
            val setDividerColorMethod =
                dividerAttributesBuilderClass.getMethod(
                    "setDraggingToFullscreenAllowed",
                    Boolean::class.java,
                )
            setDividerColorMethod.isPublic &&
                setDividerColorMethod.doesReturn(DividerAttributes.Builder::class.java)
        }

    private fun isClassAnimationParamsValid(): Boolean =
        validateReflection("Class AnimationParams is not valid") {
            val animationParamsClass = AnimationParams::class.java
            val animationResourcesIdDefaultField =
                animationParamsClass.getDeclaredField("DEFAULT_ANIMATION_RESOURCES_ID")
            val getAnimationBackgroundMethod =
                animationParamsClass.getMethod("getAnimationBackground")
            val getOpenAnimationResIdMethod =
                animationParamsClass.getMethod("getOpenAnimationResId")
            val getCloseAnimationResIdMethod =
                animationParamsClass.getMethod("getCloseAnimationResId")
            val getChangeAnimationResIdMethod =
                animationParamsClass.getMethod("getChangeAnimationResId")
            animationResourcesIdDefaultField.isPublic &&
                getAnimationBackgroundMethod.isPublic &&
                getAnimationBackgroundMethod.doesReturn(AnimationBackground::class.java) &&
                getOpenAnimationResIdMethod.isPublic &&
                getOpenAnimationResIdMethod.doesReturn(Int::class.java) &&
                getCloseAnimationResIdMethod.isPublic &&
                getCloseAnimationResIdMethod.doesReturn(Int::class.java) &&
                getChangeAnimationResIdMethod.isPublic &&
                getChangeAnimationResIdMethod.doesReturn(Int::class.java)
        }

    private fun isClassAnimationParamsBuilderValid(): Boolean =
        validateReflection("Class AnimationParams.Builder is not valid") {
            val animationParamsBuilderClass = AnimationParams.Builder::class.java
            val setAnimationBackgroundMethod =
                animationParamsBuilderClass.getMethod(
                    "setAnimationBackground",
                    AnimationBackground::class.java,
                )
            val setOpenAnimationResIdMethod =
                animationParamsBuilderClass.getMethod("setOpenAnimationResId", Int::class.java)
            val setCloseAnimationResIdMethod =
                animationParamsBuilderClass.getMethod("setCloseAnimationResId", Int::class.java)
            val setChangeAnimationResIdMethod =
                animationParamsBuilderClass.getMethod("setChangeAnimationResId", Int::class.java)
            setAnimationBackgroundMethod.isPublic &&
                setAnimationBackgroundMethod.doesReturn(AnimationParams.Builder::class.java) &&
                setOpenAnimationResIdMethod.isPublic &&
                setOpenAnimationResIdMethod.doesReturn(AnimationParams.Builder::class.java) &&
                setCloseAnimationResIdMethod.isPublic &&
                setCloseAnimationResIdMethod.doesReturn(AnimationParams.Builder::class.java) &&
                setChangeAnimationResIdMethod.isPublic &&
                setChangeAnimationResIdMethod.doesReturn(AnimationParams.Builder::class.java)
        }

    /** Overlay features validation methods */
    private fun isActivityStackGetTagValid(): Boolean =
        validateReflection("ActivityStack#getTag is not valid") {
            val activityStackClass = ActivityStack::class.java
            val getTokenMethod = activityStackClass.getMethod("getTag")

            getTokenMethod.isPublic && getTokenMethod.doesReturn(String::class.java)
        }

    private fun isMethodGetActivityStackTokenValid(): Boolean =
        validateReflection("getActivityStackToken is not valid") {
            val getActivityStackTokenMethod =
                activityEmbeddingComponentClass.getMethod(
                    "getActivityStackToken",
                    String::class.java,
                )
            getActivityStackTokenMethod.isPublic &&
                getActivityStackTokenMethod.doesReturn(ActivityStack.Token::class.java)
        }

    @Suppress("newApi") // Suppress lint check for WindowMetrics
    private fun isClassParentContainerInfoValid(): Boolean =
        validateReflection("ParentContainerInfo is not valid") {
            val parentContainerInfoClass = ParentContainerInfo::class.java
            val getWindowMetricsMethod = parentContainerInfoClass.getMethod("getWindowMetrics")
            val getConfigurationMethod = parentContainerInfoClass.getMethod("getConfiguration")
            val getWindowLayoutInfoMethod =
                parentContainerInfoClass.getMethod("getWindowLayoutInfo")
            getWindowMetricsMethod.isPublic &&
                getWindowMetricsMethod.doesReturn(WindowMetrics::class.java) &&
                getConfigurationMethod.isPublic &&
                getConfigurationMethod.doesReturn(Configuration::class.java) &&
                getWindowLayoutInfoMethod.isPublic &&
                getWindowLayoutInfoMethod.doesReturn(WindowLayoutInfo::class.java)
        }

    private fun isMethodGetParentContainerInfoValid(): Boolean =
        validateReflection("ActivityEmbeddingComponent#getParentContainerInfo is not valid") {
            val getParentContainerInfoMethod =
                activityEmbeddingComponentClass.getMethod(
                    "getParentContainerInfo",
                    ActivityStack.Token::class.java,
                )
            getParentContainerInfoMethod.isPublic &&
                getParentContainerInfoMethod.doesReturn(ParentContainerInfo::class.java)
        }

    private fun isMethodSetActivityStackAttributesCalculatorValid(): Boolean =
        validateReflection("setActivityStackAttributesCalculator is not valid") {
            val setActivityStackAttributesCalculatorMethod =
                activityEmbeddingComponentClass.getMethod(
                    "setActivityStackAttributesCalculator",
                    Function::class.java,
                )
            setActivityStackAttributesCalculatorMethod.isPublic
        }

    private fun isMethodClearActivityStackAttributesCalculatorValid(): Boolean =
        validateReflection("clearActivityStackAttributesCalculator is not valid") {
            val setActivityStackAttributesCalculatorMethod =
                activityEmbeddingComponentClass.getMethod("clearActivityStackAttributesCalculator")
            setActivityStackAttributesCalculatorMethod.isPublic
        }

    private fun isMethodUpdateActivityStackAttributesValid(): Boolean =
        validateReflection("updateActivityStackAttributes is not valid") {
            val updateActivityStackAttributesMethod =
                activityEmbeddingComponentClass.getMethod(
                    "updateActivityStackAttributes",
                    ActivityStack.Token::class.java,
                    ActivityStackAttributes::class.java,
                )
            updateActivityStackAttributesMethod.isPublic
        }

    private fun isClassActivityStackAttributesValid(): Boolean =
        validateReflection("Class ActivityStackAttributes is not valid") {
            val activityStackAttributesClass = ActivityStackAttributes::class.java
            val getRelativeBoundsMethod =
                activityStackAttributesClass.getMethod("getRelativeBounds")
            val getWindowAttributesMethod =
                activityStackAttributesClass.getMethod("getWindowAttributes")
            getRelativeBoundsMethod.isPublic &&
                getRelativeBoundsMethod.doesReturn(Rect::class.java) &&
                getWindowAttributesMethod.isPublic &&
                getWindowAttributesMethod.doesReturn(WindowAttributes::class.java)
        }

    private fun isClassActivityStackAttributesBuilderValid(): Boolean =
        validateReflection("Class ActivityStackAttributes.Builder is not valid") {
            val activityStackAttributesBuilderClass = ActivityStackAttributes.Builder::class.java
            val activityStackAttributesBuilderConstructor =
                activityStackAttributesBuilderClass.getDeclaredConstructor()
            val setRelativeBoundsMethod =
                activityStackAttributesBuilderClass.getMethod("setRelativeBounds", Rect::class.java)
            val setWindowAttributesMethod =
                activityStackAttributesBuilderClass.getMethod(
                    "setWindowAttributes",
                    WindowAttributes::class.java,
                )
            activityStackAttributesBuilderConstructor.isPublic &&
                setRelativeBoundsMethod.isPublic &&
                setRelativeBoundsMethod.doesReturn(ActivityStackAttributes.Builder::class.java) &&
                setWindowAttributesMethod.isPublic &&
                setWindowAttributesMethod.doesReturn(ActivityStackAttributes.Builder::class.java)
        }

    private fun isClassActivityStackAttributesCalculatorParamsValid(): Boolean =
        validateReflection("Class ActivityStackAttributesCalculatorParams is not valid") {
            val activityStackAttributesCalculatorParamsClass =
                ActivityStackAttributesCalculatorParams::class.java
            val getParentContainerInfoMethod =
                activityStackAttributesCalculatorParamsClass.getMethod("getParentContainerInfo")
            val getActivityStackTagMethod =
                activityStackAttributesCalculatorParamsClass.getMethod("getActivityStackTag")
            val getLaunchOptionsMethod =
                activityStackAttributesCalculatorParamsClass.getMethod("getLaunchOptions")
            getParentContainerInfoMethod.isPublic &&
                getParentContainerInfoMethod.doesReturn(ParentContainerInfo::class.java) &&
                getActivityStackTagMethod.isPublic &&
                getActivityStackTagMethod.doesReturn(String::class.java) &&
                getLaunchOptionsMethod.isPublic &&
                getLaunchOptionsMethod.doesReturn(Bundle::class.java)
        }
}
