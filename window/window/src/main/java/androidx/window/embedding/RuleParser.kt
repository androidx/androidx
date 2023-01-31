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
import android.util.LayoutDirection
import androidx.annotation.XmlRes

import androidx.window.R
import androidx.window.embedding.EmbeddingAspectRatio.Companion.buildAspectRatioFromValue
import androidx.window.embedding.SplitRule.Companion.FINISH_ALWAYS
import androidx.window.embedding.SplitRule.Companion.FINISH_NEVER

import org.xmlpull.v1.XmlPullParser

/**
 * Parses the static rules defined in XML.
 */
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
        while (type != XmlPullParser.END_DOCUMENT &&
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
                    rules.add(lastSplitPairRule)
                    lastSplitPlaceholderRule = null
                    lastActivityRule = null
                }
                "SplitPlaceholderRule" -> {
                    val placeholderConfig = parseSplitPlaceholderRule(context, parser)
                    lastSplitPlaceholderRule = placeholderConfig
                    rules.add(lastSplitPlaceholderRule)
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
                    rules.add(lastSplitPairRule)
                }
                "ActivityRule" -> {
                    val activityConfig = parseActivityRule(context, parser)
                    rules.add(activityConfig)
                    lastSplitPairRule = null
                    lastSplitPlaceholderRule = null
                    lastActivityRule = activityConfig
                }
                "ActivityFilter" -> {
                    if (lastActivityRule == null && lastSplitPlaceholderRule == null) {
                        throw IllegalArgumentException(
                            "Found orphaned ActivityFilter"
                        )
                    }
                    val activityFilter = parseActivityFilter(context, parser)
                    if (lastActivityRule != null) {
                        rules.remove(lastActivityRule)
                        lastActivityRule += activityFilter
                        rules.add(lastActivityRule)
                    } else if (lastSplitPlaceholderRule != null) {
                        rules.remove(lastSplitPlaceholderRule)
                        lastSplitPlaceholderRule += activityFilter
                        rules.add(lastSplitPlaceholderRule)
                    }
                }
            }
            type = parser.next()
        }

        return rules
    }

    private fun parseSplitPairRule(
        context: Context,
        parser: XmlResourceParser
    ): SplitPairRule {
        val ratio: Float
        val minWidthDp: Int
        val minSmallestWidthDp: Int
        val maxAspectRatioInPortrait: Float
        val maxAspectRatioInLandscape: Float
        val layoutDir: Int
        val finishPrimaryWithSecondary: Int
        val finishSecondaryWithPrimary: Int
        val clearTop: Boolean
        context.theme.obtainStyledAttributes(
            parser,
            R.styleable.SplitPairRule,
            0,
            0
        ).apply {
            ratio = getFloat(R.styleable.SplitPairRule_splitRatio, 0.5f)
            minWidthDp = getInteger(
                R.styleable.SplitPairRule_splitMinWidthDp,
                SplitRule.SPLIT_MIN_DIMENSION_DP_DEFAULT
            )
            minSmallestWidthDp = getInteger(
                R.styleable.SplitPairRule_splitMinSmallestWidthDp,
                SplitRule.SPLIT_MIN_DIMENSION_DP_DEFAULT
            )
            maxAspectRatioInPortrait = getFloat(
                R.styleable.SplitPairRule_splitMaxAspectRatioInPortrait,
                SplitRule.SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT.value
            )
            maxAspectRatioInLandscape = getFloat(
                R.styleable.SplitPairRule_splitMaxAspectRatioInLandscape,
                SplitRule.SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT.value
            )
            layoutDir = getInt(
                R.styleable.SplitPairRule_splitLayoutDirection,
                LayoutDirection.LOCALE
            )
            finishPrimaryWithSecondary =
                getInt(R.styleable.SplitPairRule_finishPrimaryWithSecondary, FINISH_NEVER)
            finishSecondaryWithPrimary =
                getInt(R.styleable.SplitPairRule_finishSecondaryWithPrimary, FINISH_ALWAYS)
            clearTop =
                getBoolean(R.styleable.SplitPairRule_clearTop, false)
        }
        return SplitPairRule.Builder(emptySet())
            .setMinWidthDp(minWidthDp)
            .setMinSmallestWidthDp(minSmallestWidthDp)
            .setMaxAspectRatioInPortrait(buildAspectRatioFromValue(maxAspectRatioInPortrait))
            .setMaxAspectRatioInLandscape(buildAspectRatioFromValue(maxAspectRatioInLandscape))
            .setFinishPrimaryWithSecondary(finishPrimaryWithSecondary)
            .setFinishSecondaryWithPrimary(finishSecondaryWithPrimary)
            .setClearTop(clearTop)
            .setSplitRatio(ratio)
            .setLayoutDirection(layoutDir)
            .build()
    }

    private fun parseSplitPlaceholderRule(
        context: Context,
        parser: XmlResourceParser
    ): SplitPlaceholderRule {
        val placeholderActivityIntentName: String?
        val stickyPlaceholder: Boolean
        val finishPrimaryWithPlaceholder: Int
        val ratio: Float
        val minWidthDp: Int
        val minSmallestWidthDp: Int
        val maxAspectRatioInPortrait: Float
        val maxAspectRatioInLandscape: Float
        val layoutDir: Int
        context.theme.obtainStyledAttributes(
            parser,
            R.styleable.SplitPlaceholderRule,
            0,
            0
        ).apply {
            placeholderActivityIntentName = getString(
                R.styleable.SplitPlaceholderRule_placeholderActivityName
            )
            stickyPlaceholder = getBoolean(R.styleable.SplitPlaceholderRule_stickyPlaceholder,
                false)
            finishPrimaryWithPlaceholder =
                getInt(R.styleable.SplitPlaceholderRule_finishPrimaryWithPlaceholder, FINISH_ALWAYS)
            ratio = getFloat(R.styleable.SplitPlaceholderRule_splitRatio, 0.5f)
            minWidthDp = getInteger(
                R.styleable.SplitPlaceholderRule_splitMinWidthDp,
                SplitRule.SPLIT_MIN_DIMENSION_DP_DEFAULT
            )
            minSmallestWidthDp = getInteger(
                R.styleable.SplitPlaceholderRule_splitMinSmallestWidthDp,
                SplitRule.SPLIT_MIN_DIMENSION_DP_DEFAULT
            )
            maxAspectRatioInPortrait = getFloat(
                R.styleable.SplitPlaceholderRule_splitMaxAspectRatioInPortrait,
                SplitRule.SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT.value
            )
            maxAspectRatioInLandscape = getFloat(
                R.styleable.SplitPlaceholderRule_splitMaxAspectRatioInLandscape,
                SplitRule.SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT.value
            )
            layoutDir = getInt(
                R.styleable.SplitPlaceholderRule_splitLayoutDirection,
                LayoutDirection.LOCALE
            )
        }
        if (finishPrimaryWithPlaceholder == FINISH_NEVER) {
                throw IllegalArgumentException(
                    "FINISH_NEVER is not a valid configuration for Placeholder activities. " +
                        "Please use FINISH_ALWAYS or FINISH_ADJACENT instead or refer to the " +
                        "current API")
        }
        val packageName = context.applicationContext.packageName
        val placeholderActivityClassName = buildClassName(
            packageName,
            placeholderActivityIntentName
        )

        return SplitPlaceholderRule.Builder(
            emptySet(),
            Intent().setComponent(placeholderActivityClassName)
        )
            .setMinWidthDp(minWidthDp)
            .setMinSmallestWidthDp(minSmallestWidthDp)
            .setMaxAspectRatioInPortrait(buildAspectRatioFromValue(maxAspectRatioInPortrait))
            .setMaxAspectRatioInLandscape(buildAspectRatioFromValue(maxAspectRatioInLandscape))
            .setSticky(stickyPlaceholder)
            .setFinishPrimaryWithPlaceholder(finishPrimaryWithPlaceholder)
            .setSplitRatio(ratio)
            .setLayoutDirection(layoutDir)
            .build()
    }

    private fun parseSplitPairFilter(
        context: Context,
        parser: XmlResourceParser
    ): SplitPairFilter {
        val primaryActivityName: String?
        val secondaryActivityIntentName: String?
        val secondaryActivityAction: String?
        context.theme.obtainStyledAttributes(
            parser,
            R.styleable.SplitPairFilter,
            0,
            0
        ).apply {
            primaryActivityName = getString(R.styleable.SplitPairFilter_primaryActivityName)
            secondaryActivityIntentName = getString(
                R.styleable.SplitPairFilter_secondaryActivityName
            )
            secondaryActivityAction = getString(
                R.styleable.SplitPairFilter_secondaryActivityAction
            )
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

    private fun parseActivityRule(
        context: Context,
        parser: XmlResourceParser
    ): ActivityRule {
        val alwaysExpand: Boolean
        context.theme.obtainStyledAttributes(
            parser,
            R.styleable.ActivityRule,
            0,
            0
        ).apply {
            alwaysExpand = getBoolean(R.styleable.ActivityRule_alwaysExpand, false)
        }
        return ActivityRule.Builder(emptySet()).setAlwaysExpand(alwaysExpand).build()
    }

    private fun parseActivityFilter(
        context: Context,
        parser: XmlResourceParser
    ): ActivityFilter {
        val activityName: String?
        val activityIntentAction: String?
        context.theme.obtainStyledAttributes(
            parser,
            R.styleable.ActivityFilter,
            0,
            0
        ).apply {
            activityName = getString(R.styleable.ActivityFilter_activityName)
            activityIntentAction = getString(R.styleable.ActivityFilter_activityAction)
        }
        val packageName = context.applicationContext.packageName
        return ActivityFilter(
            buildClassName(packageName, activityName),
            activityIntentAction
        )
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
