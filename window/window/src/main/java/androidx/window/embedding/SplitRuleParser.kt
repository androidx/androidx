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

import androidx.window.R
import androidx.window.core.ExperimentalWindowApi

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
    ): SplitPairRule {
        val ratio: Float
        val minWidth: Int
        val minSmallestWidth: Int
        val layoutDir: Int
        val finishPrimaryWithSecondary: Boolean
        val finishSecondaryWithPrimary: Boolean
        val clearTop: Boolean
        context.theme.obtainStyledAttributes(
            parser,
            R.styleable.SplitPairRule,
            0,
            0
        ).apply {
            ratio = getFloat(R.styleable.SplitPairRule_splitRatio, 0.0f)
            minWidth = getDimension(R.styleable.SplitPairRule_splitMinWidth, 0.0f).toInt()
            minSmallestWidth = getDimension(R.styleable.SplitPairRule_splitMinSmallestWidth, 0.0f)
                .toInt()
            layoutDir = getInt(
                R.styleable.SplitPairRule_splitLayoutDirection,
                LayoutDirection.LOCALE
            )
            finishPrimaryWithSecondary =
                getBoolean(R.styleable.SplitPairRule_finishPrimaryWithSecondary, false)
            finishSecondaryWithPrimary =
                getBoolean(R.styleable.SplitPairRule_finishSecondaryWithPrimary, true)
            clearTop =
                getBoolean(R.styleable.SplitPairRule_clearTop, false)
        }
        return SplitPairRule(
            emptySet(),
            finishPrimaryWithSecondary,
            finishSecondaryWithPrimary,
            clearTop,
            minWidth,
            minSmallestWidth,
            ratio,
            layoutDir
        )
    }

    private fun parseSplitPlaceholderRule(
        context: Context,
        parser: XmlResourceParser
    ): SplitPlaceholderRule {
        val placeholderActivityIntentName: String?
        val ratio: Float
        val minWidth: Int
        val minSmallestWidth: Int
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
            ratio = getFloat(R.styleable.SplitPlaceholderRule_splitRatio, 0.0f)
            minWidth = getDimension(R.styleable.SplitPlaceholderRule_splitMinWidth, 0.0f).toInt()
            minSmallestWidth = getDimension(
                R.styleable.SplitPlaceholderRule_splitMinSmallestWidth,
                0.0f
            ).toInt()
            layoutDir = getInt(
                R.styleable.SplitPlaceholderRule_splitLayoutDirection,
                LayoutDirection.LOCALE
            )
        }
        val packageName = context.applicationContext.packageName
        val placeholderActivityClassName = buildClassName(
            packageName,
            placeholderActivityIntentName
        )

        return SplitPlaceholderRule(
            emptySet(),
            Intent().setComponent(placeholderActivityClassName),
            minWidth,
            minSmallestWidth,
            ratio,
            layoutDir
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
        return ActivityRule(emptySet(), alwaysExpand)
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
}
