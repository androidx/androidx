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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.util.LayoutDirection
import android.util.Log
import android.util.Pair as AndroidPair
import android.view.WindowMetrics as AndroidWindowMetrics
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.core.Bounds
import androidx.window.core.ExperimentalWindowApi
import androidx.window.core.PredicateAdapter
import androidx.window.embedding.DividerAttributes.DragRange.Companion.DRAG_RANGE_SYSTEM_DEFAULT
import androidx.window.embedding.DividerAttributes.DragRange.SplitRatioDragRange
import androidx.window.embedding.DividerAttributes.DraggableDividerAttributes
import androidx.window.embedding.DividerAttributes.FixedDividerAttributes
import androidx.window.embedding.EmbeddingConfiguration.DimAreaBehavior.Companion.ON_ACTIVITY_STACK
import androidx.window.embedding.OverlayController.Companion.OVERLAY_FEATURE_VERSION
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.BOTTOM_TO_TOP
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.LEFT_TO_RIGHT
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.LOCALE
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.RIGHT_TO_LEFT
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.TOP_TO_BOTTOM
import androidx.window.embedding.SplitAttributes.SplitType
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_EQUAL
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_EXPAND
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_HINGE
import androidx.window.embedding.SplitAttributes.SplitType.Companion.ratio
import androidx.window.extensions.embedding.ActivityRule as OEMActivityRule
import androidx.window.extensions.embedding.ActivityRule.Builder as ActivityRuleBuilder
import androidx.window.extensions.embedding.ActivityStack as OEMActivityStack
import androidx.window.extensions.embedding.AnimationBackground as OEMEmbeddingAnimationBackground
import androidx.window.extensions.embedding.DividerAttributes as OEMDividerAttributes
import androidx.window.extensions.embedding.DividerAttributes.RATIO_SYSTEM_DEFAULT
import androidx.window.extensions.embedding.EmbeddingRule as OEMEmbeddingRule
import androidx.window.extensions.embedding.ParentContainerInfo as OEMParentContainerInfo
import androidx.window.extensions.embedding.SplitAttributes as OEMSplitAttributes
import androidx.window.extensions.embedding.SplitAttributes.SplitType as OEMSplitType
import androidx.window.extensions.embedding.SplitAttributes.SplitType.RatioSplitType
import androidx.window.extensions.embedding.SplitAttributesCalculatorParams as OEMSplitAttributesCalculatorParams
import androidx.window.extensions.embedding.SplitInfo as OEMSplitInfo
import androidx.window.extensions.embedding.SplitPairRule as OEMSplitPairRule
import androidx.window.extensions.embedding.SplitPairRule.Builder as SplitPairRuleBuilder
import androidx.window.extensions.embedding.SplitPairRule.FINISH_ADJACENT
import androidx.window.extensions.embedding.SplitPairRule.FINISH_ALWAYS
import androidx.window.extensions.embedding.SplitPairRule.FINISH_NEVER
import androidx.window.extensions.embedding.SplitPinRule as OEMSplitPinRule
import androidx.window.extensions.embedding.SplitPinRule.Builder as SplitPinRuleBuilder
import androidx.window.extensions.embedding.SplitPlaceholderRule as OEMSplitPlaceholderRule
import androidx.window.extensions.embedding.SplitPlaceholderRule.Builder as SplitPlaceholderRuleBuilder
import androidx.window.extensions.embedding.WindowAttributes as OEMWindowAttributes
import androidx.window.extensions.embedding.WindowAttributes
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.layout.adapter.extensions.ExtensionsWindowLayoutInfoAdapter
import androidx.window.layout.util.DensityCompatHelper
import androidx.window.reflection.JFunction2
import androidx.window.reflection.Predicate2

/** Adapter class that translates data classes between Extension and Jetpack interfaces. */
internal class EmbeddingAdapter(private val predicateAdapter: PredicateAdapter) {
    private val extensionVersion
        get() = WindowSdkExtensions.getInstance().extensionVersion

    private val api1Impl = VendorApiLevel1Impl(predicateAdapter)
    private val api2Impl = VendorApiLevel2Impl()
    private val api3Impl = VendorApiLevel3Impl()
    @OptIn(ExperimentalWindowApi::class) var embeddingConfiguration: EmbeddingConfiguration? = null

    fun translate(splitInfoList: List<OEMSplitInfo>): List<SplitInfo> {
        return splitInfoList.map(this::translate)
    }

    private fun translate(splitInfo: OEMSplitInfo): SplitInfo {
        return when (extensionVersion) {
            1 -> api1Impl.translateCompat(splitInfo)
            2 -> api2Impl.translateCompat(splitInfo)
            in 3..4 -> api3Impl.translateCompat(splitInfo)
            else ->
                SplitInfo(
                    translate(splitInfo.primaryActivityStack),
                    translate(splitInfo.secondaryActivityStack),
                    translate(splitInfo.splitAttributes),
                    splitInfo.splitInfoToken,
                )
        }
    }

    internal fun translate(activityStack: OEMActivityStack): ActivityStack =
        when (extensionVersion) {
            in 1..4 -> api1Impl.translateCompat(activityStack)
            else ->
                ActivityStack(
                    activityStack.activities,
                    activityStack.isEmpty,
                    activityStack.activityStackToken,
                )
        }

    internal fun translate(activityStacks: List<OEMActivityStack>): List<ActivityStack> =
        activityStacks.map(this::translate)

    internal fun translate(splitAttributes: OEMSplitAttributes): SplitAttributes {
        val builder =
            SplitAttributes.Builder()
                .setSplitType(
                    when (val splitType = splitAttributes.splitType) {
                        is OEMSplitType.HingeSplitType -> SPLIT_TYPE_HINGE
                        is OEMSplitType.ExpandContainersSplitType -> SPLIT_TYPE_EXPAND
                        is RatioSplitType -> ratio(splitType.ratio)
                        else -> throw IllegalArgumentException("Unknown split type: $splitType")
                    }
                )
                .setLayoutDirection(
                    when (val layoutDirection = splitAttributes.layoutDirection) {
                        OEMSplitAttributes.LayoutDirection.LEFT_TO_RIGHT -> LEFT_TO_RIGHT
                        OEMSplitAttributes.LayoutDirection.RIGHT_TO_LEFT -> RIGHT_TO_LEFT
                        OEMSplitAttributes.LayoutDirection.LOCALE -> LOCALE
                        OEMSplitAttributes.LayoutDirection.TOP_TO_BOTTOM -> TOP_TO_BOTTOM
                        OEMSplitAttributes.LayoutDirection.BOTTOM_TO_TOP -> BOTTOM_TO_TOP
                        else ->
                            throw IllegalArgumentException(
                                "Unknown layout direction: $layoutDirection"
                            )
                    }
                )
        if (extensionVersion >= 5) {
            val animationBackground = splitAttributes.animationBackground
            builder.setAnimationBackground(
                if (animationBackground is OEMEmbeddingAnimationBackground.ColorBackground) {
                    EmbeddingAnimationBackground.createColorBackground(animationBackground.color)
                } else {
                    EmbeddingAnimationBackground.DEFAULT
                }
            )
        }
        if (extensionVersion >= 6) {
            builder.setDividerAttributes(
                translateDividerAttributes(splitAttributes.dividerAttributes)
            )
        }
        return builder.build()
    }

    @RequiresWindowSdkExtension(OVERLAY_FEATURE_VERSION)
    @OptIn(ExperimentalWindowApi::class)
    @SuppressLint("NewApi", "ClassVerificationFailure")
    internal fun translate(
        parentContainerInfo: OEMParentContainerInfo,
    ): ParentContainerInfo {
        val configuration = parentContainerInfo.configuration
        val density =
            DensityCompatHelper.getInstance()
                .density(parentContainerInfo.configuration, parentContainerInfo.windowMetrics)
        val windowMetrics =
            WindowMetricsCalculator.translateWindowMetrics(
                parentContainerInfo.windowMetrics,
                density
            )

        return ParentContainerInfo(
            Bounds(windowMetrics.bounds),
            ExtensionsWindowLayoutInfoAdapter.translate(
                windowMetrics,
                parentContainerInfo.windowLayoutInfo
            ),
            windowMetrics.getWindowInsets(),
            configuration,
            density
        )
    }

    fun translateSplitAttributesCalculator(
        calculator: (SplitAttributesCalculatorParams) -> SplitAttributes
    ): JFunction2<OEMSplitAttributesCalculatorParams, OEMSplitAttributes> =
        JFunction2 { oemParams ->
            translateSplitAttributes(calculator.invoke(translate(oemParams)))
        }

    @SuppressLint("NewApi")
    fun translate(
        params: OEMSplitAttributesCalculatorParams,
    ): SplitAttributesCalculatorParams = let {
        val taskWindowMetrics = params.parentWindowMetrics
        val taskConfiguration = params.parentConfiguration
        val windowLayoutInfo = params.parentWindowLayoutInfo
        val defaultSplitAttributes = params.defaultSplitAttributes
        val areDefaultConstraintsSatisfied = params.areDefaultConstraintsSatisfied()
        val splitRuleTag = params.splitRuleTag
        val density =
            DensityCompatHelper.getInstance().density(taskConfiguration, taskWindowMetrics)
        val windowMetrics =
            WindowMetricsCalculator.translateWindowMetrics(taskWindowMetrics, density)
        SplitAttributesCalculatorParams(
            windowMetrics,
            taskConfiguration,
            ExtensionsWindowLayoutInfoAdapter.translate(windowMetrics, windowLayoutInfo),
            translate(defaultSplitAttributes),
            areDefaultConstraintsSatisfied,
            splitRuleTag,
        )
    }

    private fun translateSplitPairRule(
        context: Context,
        rule: SplitPairRule,
        predicateClass: Class<*>
    ): OEMSplitPairRule {
        if (extensionVersion < 2) {
            return api1Impl.translateSplitPairRuleCompat(context, rule, predicateClass)
        } else {
            val activitiesPairPredicate =
                Predicate2<AndroidPair<Activity, Activity>> { activitiesPair ->
                    rule.filters.any { filter ->
                        filter.matchesActivityPair(activitiesPair.first, activitiesPair.second)
                    }
                }
            val activityIntentPredicate =
                Predicate2<AndroidPair<Activity, Intent>> { activityIntentPair ->
                    rule.filters.any { filter ->
                        filter.matchesActivityIntentPair(
                            activityIntentPair.first,
                            activityIntentPair.second
                        )
                    }
                }
            val windowMetricsPredicate =
                Predicate2<AndroidWindowMetrics> { windowMetrics ->
                    rule.checkParentMetrics(context, windowMetrics)
                }
            val tag = rule.tag
            val builder =
                SplitPairRuleBuilder(
                        activitiesPairPredicate,
                        activityIntentPredicate,
                        windowMetricsPredicate,
                    )
                    .setDefaultSplitAttributes(
                        translateSplitAttributes(rule.defaultSplitAttributes)
                    )
                    .setFinishPrimaryWithSecondary(
                        translateFinishBehavior(rule.finishPrimaryWithSecondary)
                    )
                    .setFinishSecondaryWithPrimary(
                        translateFinishBehavior(rule.finishSecondaryWithPrimary)
                    )
                    .setShouldClearTop(rule.clearTop)

            if (tag != null) {
                builder.setTag(tag)
            }
            return builder.build()
        }
    }

    @OptIn(ExperimentalWindowApi::class)
    fun translateSplitPinRule(context: Context, splitPinRule: SplitPinRule): OEMSplitPinRule {
        WindowSdkExtensions.getInstance().requireExtensionVersion(5)
        val windowMetricsPredicate =
            Predicate2<AndroidWindowMetrics> { windowMetrics ->
                splitPinRule.checkParentMetrics(context, windowMetrics)
            }
        val builder =
            SplitPinRuleBuilder(
                translateSplitAttributes(splitPinRule.defaultSplitAttributes),
                windowMetricsPredicate
            )
        builder.setSticky(splitPinRule.isSticky)
        val tag = splitPinRule.tag
        if (tag != null) {
            builder.setTag(tag)
        }
        return builder.build()
    }

    @OptIn(ExperimentalWindowApi::class)
    fun translateSplitAttributes(splitAttributes: SplitAttributes): OEMSplitAttributes {
        require(extensionVersion >= 2)
        // To workaround the "unused" error in ktlint. It is necessary to translate SplitAttributes
        // from WM Jetpack version to WM extension version.
        val builder =
            OEMSplitAttributes.Builder()
                .setSplitType(translateSplitType(splitAttributes.splitType))
                .setLayoutDirection(
                    when (splitAttributes.layoutDirection) {
                        LOCALE -> OEMSplitAttributes.LayoutDirection.LOCALE
                        LEFT_TO_RIGHT -> OEMSplitAttributes.LayoutDirection.LEFT_TO_RIGHT
                        RIGHT_TO_LEFT -> OEMSplitAttributes.LayoutDirection.RIGHT_TO_LEFT
                        TOP_TO_BOTTOM -> OEMSplitAttributes.LayoutDirection.TOP_TO_BOTTOM
                        BOTTOM_TO_TOP -> OEMSplitAttributes.LayoutDirection.BOTTOM_TO_TOP
                        else ->
                            throw IllegalArgumentException(
                                "Unsupported layoutDirection:" + "$splitAttributes.layoutDirection"
                            )
                    }
                )
        if (extensionVersion >= 5) {
            builder
                .setWindowAttributes(translateWindowAttributes())
                .setAnimationBackground(
                    translateAnimationBackground(splitAttributes.animationBackground)
                )
        }
        if (extensionVersion >= 6) {
            builder.setDividerAttributes(
                translateDividerAttributes(splitAttributes.dividerAttributes)
            )
        }
        return builder.build()
    }

    /** Translates [embeddingConfiguration] from adapter to [WindowAttributes]. */
    @OptIn(ExperimentalWindowApi::class)
    internal fun translateWindowAttributes(): OEMWindowAttributes = let {
        WindowSdkExtensions.getInstance().requireExtensionVersion(5)

        OEMWindowAttributes(
            when (embeddingConfiguration?.dimAreaBehavior) {
                ON_ACTIVITY_STACK -> OEMWindowAttributes.DIM_AREA_ON_ACTIVITY_STACK
                else -> OEMWindowAttributes.DIM_AREA_ON_TASK
            }
        )
    }

    private fun translateSplitType(splitType: SplitType): OEMSplitType {
        require(extensionVersion >= 2)
        return when (splitType) {
            SPLIT_TYPE_HINGE -> OEMSplitType.HingeSplitType(translateSplitType(SPLIT_TYPE_EQUAL))
            SPLIT_TYPE_EXPAND -> OEMSplitType.ExpandContainersSplitType()
            else -> {
                val ratio = splitType.value
                if (ratio > 0.0 && ratio < 1.0) {
                    RatioSplitType(ratio)
                } else {
                    throw IllegalArgumentException(
                        "Unsupported SplitType: $splitType with value:" + " ${splitType.value}"
                    )
                }
            }
        }
    }

    private fun translateSplitPlaceholderRule(
        context: Context,
        rule: SplitPlaceholderRule,
        predicateClass: Class<*>
    ): OEMSplitPlaceholderRule {
        if (extensionVersion < 2) {
            return api1Impl.translateSplitPlaceholderRuleCompat(context, rule, predicateClass)
        } else {
            val activityPredicate =
                Predicate2<Activity> { activity ->
                    rule.filters.any { filter -> filter.matchesActivity(activity) }
                }
            val intentPredicate =
                Predicate2<Intent> { intent ->
                    rule.filters.any { filter -> filter.matchesIntent(intent) }
                }
            val windowMetricsPredicate =
                Predicate2<AndroidWindowMetrics> { windowMetrics ->
                    rule.checkParentMetrics(context, windowMetrics)
                }
            val tag = rule.tag
            val builder =
                SplitPlaceholderRuleBuilder(
                        rule.placeholderIntent,
                        activityPredicate,
                        intentPredicate,
                        windowMetricsPredicate
                    )
                    .setSticky(rule.isSticky)
                    .setDefaultSplitAttributes(
                        translateSplitAttributes(rule.defaultSplitAttributes)
                    )
                    .setFinishPrimaryWithPlaceholder(
                        translateFinishBehavior(rule.finishPrimaryWithPlaceholder)
                    )
            if (tag != null) {
                builder.setTag(tag)
            }
            return builder.build()
        }
    }

    fun translateFinishBehavior(behavior: SplitRule.FinishBehavior): Int =
        when (behavior) {
            SplitRule.FinishBehavior.NEVER -> FINISH_NEVER
            SplitRule.FinishBehavior.ALWAYS -> FINISH_ALWAYS
            SplitRule.FinishBehavior.ADJACENT -> FINISH_ADJACENT
            else -> throw IllegalArgumentException("Unknown finish behavior:$behavior")
        }

    private fun translateActivityRule(
        rule: ActivityRule,
        predicateClass: Class<*>
    ): OEMActivityRule {
        if (extensionVersion < 2) {
            return api1Impl.translateActivityRuleCompat(rule, predicateClass)
        } else {
            val activityPredicate =
                Predicate2<Activity> { activity ->
                    rule.filters.any { filter -> filter.matchesActivity(activity) }
                }
            val intentPredicate =
                Predicate2<Intent> { intent ->
                    rule.filters.any { filter -> filter.matchesIntent(intent) }
                }
            val builder =
                ActivityRuleBuilder(activityPredicate, intentPredicate)
                    .setShouldAlwaysExpand(rule.alwaysExpand)
            val tag = rule.tag
            if (tag != null) {
                builder.setTag(tag)
            }
            return builder.build()
        }
    }

    fun translate(context: Context, rules: Set<EmbeddingRule>): Set<OEMEmbeddingRule> {
        val predicateClass = predicateAdapter.predicateClassOrNull() ?: return emptySet()
        return rules
            .map { rule ->
                when (rule) {
                    is SplitPairRule -> translateSplitPairRule(context, rule, predicateClass)
                    is SplitPlaceholderRule ->
                        translateSplitPlaceholderRule(context, rule, predicateClass)
                    is ActivityRule -> translateActivityRule(rule, predicateClass)
                    else -> throw IllegalArgumentException("Unsupported rule type")
                }
            }
            .toSet()
    }

    private fun translateAnimationBackground(
        animationBackground: EmbeddingAnimationBackground
    ): OEMEmbeddingAnimationBackground {
        WindowSdkExtensions.getInstance().requireExtensionVersion(5)
        return if (animationBackground is EmbeddingAnimationBackground.ColorBackground) {
            OEMEmbeddingAnimationBackground.createColorBackground(animationBackground.color)
        } else {
            OEMEmbeddingAnimationBackground.ANIMATION_BACKGROUND_DEFAULT
        }
    }

    @RequiresWindowSdkExtension(6)
    fun translateDividerAttributes(dividerAttributes: DividerAttributes): OEMDividerAttributes? {
        WindowSdkExtensions.getInstance().requireExtensionVersion(6)
        if (dividerAttributes === DividerAttributes.NO_DIVIDER) {
            return null
        }
        val builder =
            OEMDividerAttributes.Builder(
                    when (dividerAttributes) {
                        is FixedDividerAttributes -> OEMDividerAttributes.DIVIDER_TYPE_FIXED
                        is DraggableDividerAttributes -> OEMDividerAttributes.DIVIDER_TYPE_DRAGGABLE
                        else ->
                            throw IllegalArgumentException(
                                "Unknown divider attributes $dividerAttributes"
                            )
                    }
                )
                .setDividerColor(dividerAttributes.color)
                .setWidthDp(dividerAttributes.widthDp)

        if (
            dividerAttributes is DraggableDividerAttributes &&
                dividerAttributes.dragRange is SplitRatioDragRange
        ) {
            builder
                .setPrimaryMinRatio(dividerAttributes.dragRange.minRatio)
                .setPrimaryMaxRatio(dividerAttributes.dragRange.maxRatio)
        }
        return builder.build()
    }

    @RequiresWindowSdkExtension(6)
    fun translateDividerAttributes(oemDividerAttributes: OEMDividerAttributes?): DividerAttributes {
        WindowSdkExtensions.getInstance().requireExtensionVersion(6)
        if (oemDividerAttributes == null) {
            return DividerAttributes.NO_DIVIDER
        }
        return when (oemDividerAttributes.dividerType) {
            OEMDividerAttributes.DIVIDER_TYPE_FIXED ->
                FixedDividerAttributes.Builder()
                    .setWidthDp(oemDividerAttributes.widthDp)
                    .setColor(oemDividerAttributes.dividerColor)
                    .build()
            OEMDividerAttributes.DIVIDER_TYPE_DRAGGABLE ->
                DraggableDividerAttributes.Builder()
                    .setWidthDp(oemDividerAttributes.widthDp)
                    .setColor(oemDividerAttributes.dividerColor)
                    .setDragRange(
                        if (
                            oemDividerAttributes.primaryMinRatio == RATIO_SYSTEM_DEFAULT &&
                                oemDividerAttributes.primaryMaxRatio == RATIO_SYSTEM_DEFAULT
                        )
                            DRAG_RANGE_SYSTEM_DEFAULT
                        else
                            SplitRatioDragRange(
                                oemDividerAttributes.primaryMinRatio,
                                oemDividerAttributes.primaryMaxRatio,
                            )
                    )
                    .build()
            // Default to DividerType.FIXED
            else -> {
                Log.w(
                    TAG,
                    "Unknown divider type $oemDividerAttributes.dividerType, default" +
                        " to fixed divider type"
                )
                FixedDividerAttributes.Builder()
                    .setWidthDp(oemDividerAttributes.widthDp)
                    .setColor(oemDividerAttributes.dividerColor)
                    .build()
            }
        }
    }

    /** Provides backward compatibility for Window extensions with API level 3 */
    // Suppress deprecation because this object is to provide backward compatibility.
    @Suppress("DEPRECATION")
    private inner class VendorApiLevel3Impl {
        fun translateCompat(splitInfo: OEMSplitInfo): SplitInfo =
            SplitInfo(
                api1Impl.translateCompat(splitInfo.primaryActivityStack),
                api1Impl.translateCompat(splitInfo.secondaryActivityStack),
                translate(splitInfo.splitAttributes),
                splitInfo.token,
            )
    }

    /** Provides backward compatibility for Window extensions with API level 2 */
    private inner class VendorApiLevel2Impl {
        fun translateCompat(splitInfo: OEMSplitInfo): SplitInfo =
            SplitInfo(
                api1Impl.translateCompat(splitInfo.primaryActivityStack),
                api1Impl.translateCompat(splitInfo.secondaryActivityStack),
                translate(splitInfo.splitAttributes),
            )
    }

    /** Provides backward compatibility for [WindowSdkExtensions] version 1 */
    // Suppress deprecation because this object is to provide backward compatibility.
    @Suppress("DEPRECATION")
    private inner class VendorApiLevel1Impl(val predicateAdapter: PredicateAdapter) {
        /** Obtains [SplitAttributes] from [OEMSplitInfo] with [WindowSdkExtensions] version 1 */
        fun getSplitAttributesCompat(splitInfo: OEMSplitInfo): SplitAttributes =
            SplitAttributes.Builder()
                .setSplitType(SplitType.buildSplitTypeFromValue(splitInfo.splitRatio))
                .setLayoutDirection(LOCALE)
                .build()

        fun translateActivityRuleCompat(
            rule: ActivityRule,
            predicateClass: Class<*>
        ): OEMActivityRule =
            ActivityRuleBuilder::class
                .java
                .getConstructor(predicateClass, predicateClass)
                .newInstance(
                    translateActivityPredicates(rule.filters),
                    translateIntentPredicates(rule.filters)
                )
                .setShouldAlwaysExpand(rule.alwaysExpand)
                .build()

        fun translateSplitPlaceholderRuleCompat(
            context: Context,
            rule: SplitPlaceholderRule,
            predicateClass: Class<*>
        ): OEMSplitPlaceholderRule =
            SplitPlaceholderRuleBuilder::class
                .java
                .getConstructor(Intent::class.java, predicateClass, predicateClass, predicateClass)
                .newInstance(
                    rule.placeholderIntent,
                    translateActivityPredicates(rule.filters),
                    translateIntentPredicates(rule.filters),
                    translateParentMetricsPredicate(context, rule)
                )
                .setSticky(rule.isSticky)
                .setFinishPrimaryWithSecondary(
                    translateFinishBehavior(rule.finishPrimaryWithPlaceholder)
                )
                .setDefaultSplitAttributesCompat(rule.defaultSplitAttributes)
                .build()

        private fun SplitPlaceholderRuleBuilder.setDefaultSplitAttributesCompat(
            defaultAttrs: SplitAttributes,
        ): SplitPlaceholderRuleBuilder = apply {
            val (splitRatio, layoutDirection) = translateSplitAttributesCompatInternal(defaultAttrs)
            // #setDefaultAttributes or SplitAttributes ctr weren't supported.
            setSplitRatio(splitRatio)
            setLayoutDirection(layoutDirection)
        }

        fun translateSplitPairRuleCompat(
            context: Context,
            rule: SplitPairRule,
            predicateClass: Class<*>
        ): OEMSplitPairRule =
            SplitPairRuleBuilder::class
                .java
                .getConstructor(
                    predicateClass,
                    predicateClass,
                    predicateClass,
                )
                .newInstance(
                    translateActivityPairPredicates(rule.filters),
                    translateActivityIntentPredicates(rule.filters),
                    translateParentMetricsPredicate(context, rule)
                )
                .setDefaultSplitAttributesCompat(rule.defaultSplitAttributes)
                .setShouldClearTop(rule.clearTop)
                .setFinishPrimaryWithSecondary(
                    translateFinishBehavior(rule.finishPrimaryWithSecondary)
                )
                .setFinishSecondaryWithPrimary(
                    translateFinishBehavior(rule.finishSecondaryWithPrimary)
                )
                .build()

        @SuppressLint("ClassVerificationFailure", "NewApi")
        private fun translateActivityPairPredicates(splitPairFilters: Set<SplitPairFilter>): Any {
            return predicateAdapter.buildPairPredicate(Activity::class, Activity::class) {
                first: Activity,
                second: Activity ->
                splitPairFilters.any { filter -> filter.matchesActivityPair(first, second) }
            }
        }

        @SuppressLint("ClassVerificationFailure", "NewApi")
        private fun translateActivityIntentPredicates(splitPairFilters: Set<SplitPairFilter>): Any {
            return predicateAdapter.buildPairPredicate(Activity::class, Intent::class) {
                first,
                second ->
                splitPairFilters.any { filter -> filter.matchesActivityIntentPair(first, second) }
            }
        }

        private fun SplitPairRuleBuilder.setDefaultSplitAttributesCompat(
            defaultAttrs: SplitAttributes,
        ): SplitPairRuleBuilder = apply {
            val (splitRatio, layoutDirection) = translateSplitAttributesCompatInternal(defaultAttrs)
            setSplitRatio(splitRatio)
            setLayoutDirection(layoutDirection)
        }

        private fun translateSplitAttributesCompatInternal(
            attrs: SplitAttributes
        ): Pair<Float, Int> = // Use a (Float, Integer) pair since SplitAttributes weren't supported
        if (!isSplitAttributesSupported(attrs)) {
                // Fallback to expand the secondary container if the SplitAttributes are not
                // supported.
                Pair(0.0f, LayoutDirection.LOCALE)
            } else {
                Pair(
                    attrs.splitType.value,
                    when (attrs.layoutDirection) {
                        // Legacy LayoutDirection uses LayoutDirection constants in framework APIs.
                        LOCALE -> LayoutDirection.LOCALE
                        LEFT_TO_RIGHT -> LayoutDirection.LTR
                        RIGHT_TO_LEFT -> LayoutDirection.RTL
                        else ->
                            throw IllegalStateException(
                                "Unsupported layout direction must be" +
                                    " covered in @isSplitAttributesSupported!"
                            )
                    }
                )
            }

        /**
         * Returns `true` if `attrs` is compatible with vendor API level 1 and doesn't use the new
         * features introduced in vendor API level 2 or higher.
         */
        private fun isSplitAttributesSupported(attrs: SplitAttributes) =
            attrs.splitType.value in 0.0..1.0 &&
                attrs.splitType.value != 1.0f &&
                attrs.layoutDirection in arrayOf(LEFT_TO_RIGHT, RIGHT_TO_LEFT, LOCALE)

        @SuppressLint("ClassVerificationFailure", "NewApi")
        private fun translateActivityPredicates(activityFilters: Set<ActivityFilter>): Any {
            return predicateAdapter.buildPredicate(Activity::class) { activity ->
                activityFilters.any { filter -> filter.matchesActivity(activity) }
            }
        }

        @SuppressLint("ClassVerificationFailure", "NewApi")
        private fun translateIntentPredicates(activityFilters: Set<ActivityFilter>): Any {
            return predicateAdapter.buildPredicate(Intent::class) { intent ->
                activityFilters.any { filter -> filter.matchesIntent(intent) }
            }
        }

        @SuppressLint("ClassVerificationFailure", "NewApi")
        private fun translateParentMetricsPredicate(context: Context, splitRule: SplitRule): Any =
            predicateAdapter.buildPredicate(AndroidWindowMetrics::class) { windowMetrics ->
                splitRule.checkParentMetrics(context, windowMetrics)
            }

        fun translateCompat(splitInfo: OEMSplitInfo): SplitInfo =
            SplitInfo(
                translateCompat(splitInfo.primaryActivityStack),
                translateCompat(splitInfo.secondaryActivityStack),
                getSplitAttributesCompat(splitInfo),
            )

        fun translateCompat(activityStack: OEMActivityStack): ActivityStack =
            ActivityStack(
                activityStack.activities,
                activityStack.isEmpty,
            )
    }

    internal companion object {
        private val TAG = EmbeddingAdapter::class.simpleName

        /**
         * The default token of [SplitInfo], which provides compatibility for device prior to vendor
         * API level 3
         */
        val INVALID_SPLIT_INFO_TOKEN = Binder()
    }
}
