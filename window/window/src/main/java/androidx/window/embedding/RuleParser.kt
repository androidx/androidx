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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.content.res.XmlResourceParser
import androidx.annotation.XmlRes
import androidx.window.R
import androidx.window.embedding.DividerAttributes.Companion.COLOR_SYSTEM_DEFAULT
import androidx.window.embedding.DividerAttributes.Companion.DRAG_RANGE_VALUE_UNSPECIFIED
import androidx.window.embedding.DividerAttributes.Companion.TYPE_VALUE_FIXED
import androidx.window.embedding.DividerAttributes.Companion.WIDTH_SYSTEM_DEFAULT
import androidx.window.embedding.DividerAttributes.Companion.validateXmlDividerAttributes
import androidx.window.embedding.EmbeddingAspectRatio.Companion.buildAspectRatioFromValue
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.LOCALE
import androidx.window.embedding.SplitRule.FinishBehavior.Companion.ALWAYS
import androidx.window.embedding.SplitRule.FinishBehavior.Companion.NEVER
import androidx.window.embedding.SplitRule.FinishBehavior.Companion.getFinishBehaviorFromValue
import org.xmlpull.v1.XmlPullParser

/** Parses the static rules defined in XML. */
internal object RuleParser {

    internal fun parseRules(
        context: Context,
        @XmlRes staticRuleResourceId: Int,
    ): Set<EmbeddingRule>? {
        val resources = context.resources
        val parser: XmlResourceParser
        try {
            parser = resources.getXml(staticRuleResourceId)
        } catch (e: Resources.NotFoundException) {
            // Can't find the XML defining the split config
            return null
        }

        val rules = HashSet<EmbeddingRule>()

        val depth = parser.depth
        var type = parser.next()
        var lastSplitPairRule: SplitPairRule? = null
        var lastSplitPlaceholderRule: SplitPlaceholderRule? = null
        var lastActivityRule: ActivityRule? = null
        while (
            type != XmlPullParser.END_DOCUMENT &&
                (type != XmlPullParser.END_TAG || parser.depth > depth)
        ) {
            if (parser.eventType != XmlPullParser.START_TAG || "split-config" == parser.name) {
                type = parser.next()
                continue
            }
            when (parser.name) {
                "SplitPairRule" -> {
                    val splitConfig = parseSplitPairRule(context, parser)
                    lastSplitPairRule = splitConfig
                    rules.addRuleWithDuplicatedTagCheck(lastSplitPairRule)
                    lastSplitPlaceholderRule = null
                    lastActivityRule = null
                }
                "SplitPlaceholderRule" -> {
                    val placeholderConfig = parseSplitPlaceholderRule(context, parser)
                    lastSplitPlaceholderRule = placeholderConfig
                    rules.addRuleWithDuplicatedTagCheck(lastSplitPlaceholderRule)
                    lastActivityRule = null
                    lastSplitPairRule = null
                }
                "SplitPairFilter" -> {
                    if (lastSplitPairRule == null) {
                        throw IllegalArgumentException(
                            "Found orphaned SplitPairFilter outside of SplitPairRule"
                        )
                    }
                    val splitFilter = parseSplitPairFilter(context, parser)
                    rules.remove(lastSplitPairRule)
                    lastSplitPairRule += splitFilter
                    rules.addRuleWithDuplicatedTagCheck(lastSplitPairRule)
                }
                "ActivityRule" -> {
                    val activityConfig = parseActivityRule(context, parser)
                    rules.addRuleWithDuplicatedTagCheck(activityConfig)
                    lastSplitPairRule = null
                    lastSplitPlaceholderRule = null
                    lastActivityRule = activityConfig
                }
                "ActivityFilter" -> {
                    if (lastActivityRule == null && lastSplitPlaceholderRule == null) {
                        throw IllegalArgumentException("Found orphaned ActivityFilter")
                    }
                    val activityFilter = parseActivityFilter(context, parser)
                    if (lastActivityRule != null) {
                        rules.remove(lastActivityRule)
                        lastActivityRule += activityFilter
                        rules.addRuleWithDuplicatedTagCheck(lastActivityRule)
                    } else if (lastSplitPlaceholderRule != null) {
                        rules.remove(lastSplitPlaceholderRule)
                        lastSplitPlaceholderRule += activityFilter
                        rules.addRuleWithDuplicatedTagCheck(lastSplitPlaceholderRule)
                    }
                }
                "DividerAttributes" -> {
                    if (lastSplitPairRule == null && lastSplitPlaceholderRule == null) {
                        throw IllegalArgumentException("Found orphaned DividerAttributes")
                    }
                    val dividerAttributes = parseDividerAttributes(context, parser)
                    if (lastSplitPairRule != null) {
                        rules.remove(lastSplitPairRule)
                        val splitAttributes =
                            SplitAttributes.Builder(lastSplitPairRule.defaultSplitAttributes)
                                .setDividerAttributes(dividerAttributes)
                                .build()
                        lastSplitPairRule =
                            SplitPairRule.Builder(lastSplitPairRule)
                                .setDefaultSplitAttributes(splitAttributes)
                                .build()
                        rules.addRuleWithDuplicatedTagCheck(lastSplitPairRule)
                    } else if (lastSplitPlaceholderRule != null) {
                        rules.remove(lastSplitPlaceholderRule)
                        val splitAttributes =
                            SplitAttributes.Builder(lastSplitPlaceholderRule.defaultSplitAttributes)
                                .setDividerAttributes(dividerAttributes)
                                .build()
                        lastSplitPlaceholderRule =
                            SplitPlaceholderRule.Builder(lastSplitPlaceholderRule)
                                .setDefaultSplitAttributes(splitAttributes)
                                .build()
                        rules.addRuleWithDuplicatedTagCheck(lastSplitPlaceholderRule)
                    }
                }
            }
            type = parser.next()
        }
        return rules
    }

    private fun HashSet<EmbeddingRule>.addRuleWithDuplicatedTagCheck(rule: EmbeddingRule) {
        val tag = rule.tag
        forEach { addedRule ->
            if (tag != null && tag == addedRule.tag) {
                throw IllegalArgumentException(
                    "Duplicated tag: $tag for $rule. " +
                        "The tag must be unique in XML rule definition."
                )
            }
        }
        add(rule)
    }

    private fun parseSplitPairRule(context: Context, parser: XmlResourceParser): SplitPairRule =
        context.theme.obtainStyledAttributes(parser, R.styleable.SplitPairRule, 0, 0).let {
            typedArray ->
            val tag = typedArray.getString(R.styleable.SplitPairRule_tag)
            val ratio = typedArray.getFloat(R.styleable.SplitPairRule_splitRatio, 0.5f)
            val minWidthDp =
                typedArray.getInteger(
                    R.styleable.SplitPairRule_splitMinWidthDp,
                    SplitRule.SPLIT_MIN_DIMENSION_DP_DEFAULT
                )
            val minHeightDp =
                typedArray.getInteger(
                    R.styleable.SplitPairRule_splitMinHeightDp,
                    SplitRule.SPLIT_MIN_DIMENSION_DP_DEFAULT
                )
            val minSmallestWidthDp =
                typedArray.getInteger(
                    R.styleable.SplitPairRule_splitMinSmallestWidthDp,
                    SplitRule.SPLIT_MIN_DIMENSION_DP_DEFAULT
                )
            val maxAspectRatioInPortrait =
                typedArray.getFloat(
                    R.styleable.SplitPairRule_splitMaxAspectRatioInPortrait,
                    SplitRule.SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT.value
                )
            val maxAspectRatioInLandscape =
                typedArray.getFloat(
                    R.styleable.SplitPairRule_splitMaxAspectRatioInLandscape,
                    SplitRule.SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT.value
                )
            val layoutDir =
                typedArray.getInt(R.styleable.SplitPairRule_splitLayoutDirection, LOCALE.value)
            val finishPrimaryWithSecondary =
                typedArray.getInt(R.styleable.SplitPairRule_finishPrimaryWithSecondary, NEVER.value)
            val finishSecondaryWithPrimary =
                typedArray.getInt(
                    R.styleable.SplitPairRule_finishSecondaryWithPrimary,
                    ALWAYS.value
                )
            val clearTop = typedArray.getBoolean(R.styleable.SplitPairRule_clearTop, false)
            val animationBackgroundColor =
                typedArray.getColor(R.styleable.SplitPairRule_animationBackgroundColor, 0)
            typedArray.recycle()

            val defaultAttrs =
                SplitAttributes.Builder()
                    .setSplitType(SplitAttributes.SplitType.buildSplitTypeFromValue(ratio))
                    .setLayoutDirection(
                        SplitAttributes.LayoutDirection.getLayoutDirectionFromValue(layoutDir)
                    )
                    .setAnimationBackground(
                        EmbeddingAnimationBackground.buildFromValue(animationBackgroundColor)
                    )
                    .build()

            SplitPairRule.Builder(emptySet())
                .setTag(tag)
                .setMinWidthDp(minWidthDp)
                .setMinHeightDp(minHeightDp)
                .setMinSmallestWidthDp(minSmallestWidthDp)
                .setMaxAspectRatioInPortrait(buildAspectRatioFromValue(maxAspectRatioInPortrait))
                .setMaxAspectRatioInLandscape(buildAspectRatioFromValue(maxAspectRatioInLandscape))
                .setFinishPrimaryWithSecondary(
                    getFinishBehaviorFromValue(finishPrimaryWithSecondary)
                )
                .setFinishSecondaryWithPrimary(
                    getFinishBehaviorFromValue(finishSecondaryWithPrimary)
                )
                .setClearTop(clearTop)
                .setDefaultSplitAttributes(defaultAttrs)
                .build()
        }

    private fun parseSplitPlaceholderRule(
        context: Context,
        parser: XmlResourceParser
    ): SplitPlaceholderRule =
        context.theme.obtainStyledAttributes(parser, R.styleable.SplitPlaceholderRule, 0, 0).let {
            typedArray ->
            val tag = typedArray.getString(R.styleable.SplitPlaceholderRule_tag)
            val placeholderActivityIntentName =
                typedArray.getString(R.styleable.SplitPlaceholderRule_placeholderActivityName)
            val stickyPlaceholder =
                typedArray.getBoolean(R.styleable.SplitPlaceholderRule_stickyPlaceholder, false)
            val finishPrimaryWithPlaceholder =
                typedArray.getInt(
                    R.styleable.SplitPlaceholderRule_finishPrimaryWithPlaceholder,
                    ALWAYS.value
                )
            if (finishPrimaryWithPlaceholder == NEVER.value) {
                throw IllegalArgumentException(
                    "Never is not a valid configuration for Placeholder activities. " +
                        "Please use FINISH_ALWAYS or FINISH_ADJACENT instead or refer to the " +
                        "current API"
                )
            }
            val ratio = typedArray.getFloat(R.styleable.SplitPlaceholderRule_splitRatio, 0.5f)
            val minWidthDp =
                typedArray.getInteger(
                    R.styleable.SplitPlaceholderRule_splitMinWidthDp,
                    SplitRule.SPLIT_MIN_DIMENSION_DP_DEFAULT
                )
            val minHeightDp =
                typedArray.getInteger(
                    R.styleable.SplitPlaceholderRule_splitMinHeightDp,
                    SplitRule.SPLIT_MIN_DIMENSION_DP_DEFAULT
                )
            val minSmallestWidthDp =
                typedArray.getInteger(
                    R.styleable.SplitPlaceholderRule_splitMinSmallestWidthDp,
                    SplitRule.SPLIT_MIN_DIMENSION_DP_DEFAULT
                )
            val maxAspectRatioInPortrait =
                typedArray.getFloat(
                    R.styleable.SplitPlaceholderRule_splitMaxAspectRatioInPortrait,
                    SplitRule.SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT.value
                )
            val maxAspectRatioInLandscape =
                typedArray.getFloat(
                    R.styleable.SplitPlaceholderRule_splitMaxAspectRatioInLandscape,
                    SplitRule.SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT.value
                )
            val layoutDir =
                typedArray.getInt(
                    R.styleable.SplitPlaceholderRule_splitLayoutDirection,
                    LOCALE.value
                )
            val animationBackgroundColor =
                typedArray.getColor(R.styleable.SplitPlaceholderRule_animationBackgroundColor, 0)
            typedArray.recycle()

            val defaultAttrs =
                SplitAttributes.Builder()
                    .setSplitType(SplitAttributes.SplitType.buildSplitTypeFromValue(ratio))
                    .setLayoutDirection(
                        SplitAttributes.LayoutDirection.getLayoutDirectionFromValue(layoutDir)
                    )
                    .setAnimationBackground(
                        EmbeddingAnimationBackground.buildFromValue(animationBackgroundColor)
                    )
                    .build()
            val packageName = context.applicationContext.packageName
            val placeholderActivityClassName =
                buildClassName(packageName, placeholderActivityIntentName)

            SplitPlaceholderRule.Builder(
                    emptySet(),
                    Intent().setComponent(placeholderActivityClassName)
                )
                .setTag(tag)
                .setMinWidthDp(minWidthDp)
                .setMinHeightDp(minHeightDp)
                .setMinSmallestWidthDp(minSmallestWidthDp)
                .setMaxAspectRatioInPortrait(buildAspectRatioFromValue(maxAspectRatioInPortrait))
                .setMaxAspectRatioInLandscape(buildAspectRatioFromValue(maxAspectRatioInLandscape))
                .setSticky(stickyPlaceholder)
                .setFinishPrimaryWithPlaceholder(
                    getFinishBehaviorFromValue(finishPrimaryWithPlaceholder)
                )
                .setDefaultSplitAttributes(defaultAttrs)
                .build()
        }

    private fun parseSplitPairFilter(context: Context, parser: XmlResourceParser): SplitPairFilter {
        val primaryActivityName: String?
        val secondaryActivityIntentName: String?
        val secondaryActivityAction: String?
        context.theme.obtainStyledAttributes(parser, R.styleable.SplitPairFilter, 0, 0).apply {
            primaryActivityName = getString(R.styleable.SplitPairFilter_primaryActivityName)
            secondaryActivityIntentName =
                getString(R.styleable.SplitPairFilter_secondaryActivityName)
            secondaryActivityAction = getString(R.styleable.SplitPairFilter_secondaryActivityAction)
        }
        val packageName = context.applicationContext.packageName
        val primaryActivityClassName = buildClassName(packageName, primaryActivityName)
        val secondaryActivityClassName = buildClassName(packageName, secondaryActivityIntentName)
        return SplitPairFilter(
            primaryActivityClassName,
            secondaryActivityClassName,
            secondaryActivityAction
        )
    }

    private fun parseActivityRule(context: Context, parser: XmlResourceParser): ActivityRule =
        context.theme.obtainStyledAttributes(parser, R.styleable.ActivityRule, 0, 0).let {
            typedArray ->
            val tag = typedArray.getString(R.styleable.ActivityRule_tag)
            val alwaysExpand = typedArray.getBoolean(R.styleable.ActivityRule_alwaysExpand, false)
            typedArray.recycle()

            val builder = ActivityRule.Builder(emptySet()).setAlwaysExpand(alwaysExpand)
            if (tag != null) {
                builder.setTag(tag)
            }
            builder.build()
        }

    private fun parseActivityFilter(context: Context, parser: XmlResourceParser): ActivityFilter {
        val activityName: String?
        val activityIntentAction: String?
        context.theme.obtainStyledAttributes(parser, R.styleable.ActivityFilter, 0, 0).apply {
            activityName = getString(R.styleable.ActivityFilter_activityName)
            activityIntentAction = getString(R.styleable.ActivityFilter_activityAction)
        }
        val packageName = context.applicationContext.packageName
        return ActivityFilter(buildClassName(packageName, activityName), activityIntentAction)
    }

    private fun parseDividerAttributes(
        context: Context,
        parser: XmlResourceParser
    ): DividerAttributes {
        context.theme.obtainStyledAttributes(parser, R.styleable.DividerAttributes, 0, 0).apply {
            val type = getInt(R.styleable.DividerAttributes_dividerType, TYPE_VALUE_FIXED)
            validateXmlDividerAttributes(
                type,
                hasValue(R.styleable.DividerAttributes_dragRangeMinRatio),
                hasValue(R.styleable.DividerAttributes_dragRangeMaxRatio),
            )

            val widthDp = getInt(R.styleable.DividerAttributes_dividerWidthDp, WIDTH_SYSTEM_DEFAULT)
            val color = getColor(R.styleable.DividerAttributes_dividerColor, COLOR_SYSTEM_DEFAULT)
            val dragRangeMinRatio =
                getFloat(
                    R.styleable.DividerAttributes_dragRangeMinRatio,
                    DRAG_RANGE_VALUE_UNSPECIFIED
                )
            val dragRangeMaxRatio =
                getFloat(
                    R.styleable.DividerAttributes_dragRangeMaxRatio,
                    DRAG_RANGE_VALUE_UNSPECIFIED
                )
            return@parseDividerAttributes DividerAttributes.createDividerAttributes(
                type,
                widthDp,
                color,
                dragRangeMinRatio,
                dragRangeMaxRatio,
            )
        }
    }

    private fun buildClassName(pkg: String, clsSeq: CharSequence?): ComponentName {
        if (clsSeq.isNullOrEmpty()) {
            throw IllegalArgumentException("Activity name must not be null")
        }
        val cls = clsSeq.toString()
        val c = cls[0]
        if (c == '.') {
            return ComponentName(pkg, pkg + cls)
        }
        var pkgString = pkg
        var clsString = cls
        val pkgDividerIndex = cls.indexOf('/')
        if (pkgDividerIndex > 0) {
            pkgString = cls.substring(0, pkgDividerIndex)
            clsString = cls.substring(pkgDividerIndex + 1)
        }
        if (clsString != "*" && clsString.indexOf('.') < 0) {
            val b = StringBuilder(pkgString)
            b.append('.')
            b.append(clsString)
            return ComponentName(pkgString, b.toString())
        }
        return ComponentName(pkgString, clsString)
    }
}
