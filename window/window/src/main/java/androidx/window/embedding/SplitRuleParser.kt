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
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.window.R
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.LOCALE
import androidx.window.embedding.SplitRule.Companion.FINISH_ALWAYS
import androidx.window.embedding.SplitRule.Companion.FINISH_NEVER
import org.xmlpull.v1.XmlPullParser

/**
 * Parses the static split rules defined in XML.
 */
@ExperimentalWindowApi
internal class SplitRuleParser {

    internal fun parseSplitRules(context: Context, staticRuleResourceId: Int): Set<EmbeddingRule>? {
        return parseSplitXml(context, staticRuleResourceId)
    }

    private fun parseSplitXml(context: Context, splitResourceId: Int): Set<EmbeddingRule>? {
        val resources = context.resources
        val parser: XmlResourceParser
        try {
            parser = resources.getXml(splitResourceId)
        } catch (e: Resources.NotFoundException) {
            // Can't find the XML defining the split config
            return null
        }

        val splitRuleConfigs = HashSet<EmbeddingRule>()

        val depth = parser.depth
        var type = parser.next()
        var lastSplitPairConfig: SplitPairRule? = null
        var lastSplitPlaceholderConfig: SplitPlaceholderRule? = null
        var lastSplitActivityConfig: ActivityRule? = null
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
                    lastSplitPairConfig = splitConfig
                    splitRuleConfigs.add(lastSplitPairConfig)
                    lastSplitPlaceholderConfig = null
                    lastSplitActivityConfig = null
                }
                "SplitPlaceholderRule" -> {
                    val placeholderConfig = parseSplitPlaceholderRule(context, parser)
                    lastSplitPlaceholderConfig = placeholderConfig
                    splitRuleConfigs.add(lastSplitPlaceholderConfig)
                    lastSplitActivityConfig = null
                    lastSplitPairConfig = null
                }
                "SplitPairFilter" -> {
                    if (lastSplitPairConfig == null) {
                        throw IllegalArgumentException(
                            "Found orphaned SplitPairFilter outside of SplitPairRule"
                        )
                    }
                    val splitFilter = parseSplitPairFilter(context, parser)
                    splitRuleConfigs.remove(lastSplitPairConfig)
                    lastSplitPairConfig += splitFilter
                    splitRuleConfigs.add(lastSplitPairConfig)
                }
                "ActivityRule" -> {
                    val activityConfig = parseSplitActivityRule(context, parser)
                    splitRuleConfigs.add(activityConfig)
                    lastSplitPairConfig = null
                    lastSplitPlaceholderConfig = null
                    lastSplitActivityConfig = activityConfig
                }
                "ActivityFilter" -> {
                    if (lastSplitActivityConfig == null && lastSplitPlaceholderConfig == null) {
                        throw IllegalArgumentException(
                            "Found orphaned ActivityFilter"
                        )
                    }
                    val activityFilter = parseActivityFilter(context, parser)
                    if (lastSplitActivityConfig != null) {
                        splitRuleConfigs.remove(lastSplitActivityConfig)
                        lastSplitActivityConfig += activityFilter
                        splitRuleConfigs.add(lastSplitActivityConfig)
                    } else if (lastSplitPlaceholderConfig != null) {
                        splitRuleConfigs.remove(lastSplitPlaceholderConfig)
                        lastSplitPlaceholderConfig += activityFilter
                        splitRuleConfigs.add(lastSplitPlaceholderConfig)
                    }
                }
            }
            type = parser.next()
        }

        return splitRuleConfigs
    }

    private fun parseSplitPairRule(
        context: Context,
        parser: XmlResourceParser
    ): SplitPairRule =
        context.theme.obtainStyledAttributes(
            parser,
            R.styleable.SplitPairRule,
            0,
            0
        ).let { typedArray ->
            val ratio = typedArray.getFloat(R.styleable.SplitPairRule_splitRatio, 0.5f)
            val minWidth = typedArray.getDimension(
                R.styleable.SplitPairRule_splitMinWidth,
                defaultMinWidth(context.resources)
            ).toInt()
            val minSmallestWidth = typedArray.getDimension(
                R.styleable.SplitPairRule_splitMinSmallestWidth,
                defaultMinWidth(context.resources)
            ).toInt()
            val layoutDir = typedArray.getInt(
                R.styleable.SplitPairRule_splitLayoutDirection,
                LOCALE.value
            )
            val finishPrimaryWithSecondary = typedArray.getInt(
                R.styleable.SplitPairRule_finishPrimaryWithSecondary,
                FINISH_NEVER
            )
            val finishSecondaryWithPrimary = typedArray.getInt(
                R.styleable.SplitPairRule_finishSecondaryWithPrimary,
                FINISH_ALWAYS
            )
            val clearTop = typedArray.getBoolean(R.styleable.SplitPairRule_clearTop, false)
            typedArray.recycle()

            val defaultAttrs = SplitAttributes.buildSplitAttributesFromValue(ratio, layoutDir)

            SplitPairRule(
                emptySet(),
                finishPrimaryWithSecondary,
                finishSecondaryWithPrimary,
                clearTop,
                minWidth,
                minSmallestWidth,
                defaultAttrs,
            )
        }

    // TODO(b/207494880): Support DefaultAttributes in XML format
    private fun buildSplitAttributesFromValue(
        @FloatRange(from = 0.0, to = 1.0, toInclusive = false)
        splitTypeValue: Float,
        @IntRange(from = 0, to = 4)
        layoutDirValue: Int,
    ): SplitAttributes =
        SplitAttributes.Builder()
            // TODO(b/207494880): Support SplitByHinge in XML format
            .setSplitType(
                if (splitTypeValue == 0.0f) {
                    SplitAttributes.SplitType.expandContainers()
                } else {
                    SplitAttributes.SplitType.ratio(splitTypeValue)
                }
            ).setLayoutDirection(
                SplitAttributes.LayoutDirection.getLayoutDirectionFromValue(layoutDirValue)
            ).build()

    private fun parseSplitPlaceholderRule(
        context: Context,
        parser: XmlResourceParser
    ): SplitPlaceholderRule =
        context.theme.obtainStyledAttributes(
            parser,
            R.styleable.SplitPlaceholderRule,
            0,
            0
        ).let { typedArray ->
            val placeholderActivityIntentName = typedArray.getString(
                R.styleable.SplitPlaceholderRule_placeholderActivityName
            )
            val stickyPlaceholder = typedArray.getBoolean(
                R.styleable.SplitPlaceholderRule_stickyPlaceholder,
                false
            )
            val finishPrimaryWithPlaceholder = typedArray.getInt(
                R.styleable.SplitPlaceholderRule_finishPrimaryWithPlaceholder,
                FINISH_ALWAYS
            )
            if (finishPrimaryWithPlaceholder == FINISH_NEVER) {
                throw IllegalArgumentException(
                    "FINISH_NEVER is not a valid configuration for Placeholder activities. " +
                        "Please use FINISH_ALWAYS or FINISH_ADJACENT instead or refer to the " +
                        "current API")
            }
            val ratio = typedArray.getFloat(R.styleable.SplitPlaceholderRule_splitRatio, 0.5f)
            val minWidth = typedArray.getDimension(
                R.styleable.SplitPlaceholderRule_splitMinWidth,
                defaultMinWidth(context.resources)
            ).toInt()
            val minSmallestWidth = typedArray.getDimension(
                R.styleable.SplitPlaceholderRule_splitMinSmallestWidth,
                defaultMinWidth(context.resources)
            ).toInt()
            val layoutDir = typedArray.getInt(
                R.styleable.SplitPlaceholderRule_splitLayoutDirection,
                LOCALE.value
            )
            typedArray.recycle()

            val defaultAttrs = SplitAttributes.buildSplitAttributesFromValue(ratio, layoutDir)
            val packageName = context.applicationContext.packageName
            val placeholderActivityClassName = buildClassName(
                packageName,
                placeholderActivityIntentName
            )

            SplitPlaceholderRule(
                emptySet(),
                Intent().setComponent(placeholderActivityClassName),
                stickyPlaceholder,
                finishPrimaryWithPlaceholder,
                minWidth,
                minSmallestWidth,
                defaultAttrs,
            )
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

    private fun parseSplitActivityRule(
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
        return ActivityRule.Builder(emptySet())
            .setAlwaysExpand(alwaysExpand)
            .build()
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
        if (clsSeq == null || clsSeq.isEmpty()) {
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

    private fun defaultMinWidth(resources: Resources): Float {
        // Get the screen's density scale
        val scale: Float = resources.displayMetrics.density
        // Convert the dps to pixels, based on density scale
        return 600 * scale + 0.5f
    }
}
