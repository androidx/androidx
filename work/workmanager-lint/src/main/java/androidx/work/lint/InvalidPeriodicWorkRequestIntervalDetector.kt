/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage")

package androidx.work.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.uast.UCallExpression
import java.util.EnumSet
import java.util.concurrent.TimeUnit

/**
 * Ensures a valid interval duration for a `PeriodicWorkRequest`.
 */
class InvalidPeriodicWorkRequestIntervalDetector : Detector(), SourceCodeScanner {
    companion object {
        val ISSUE = Issue.create(
            id = "InvalidPeriodicWorkRequestInterval",
            briefDescription = "Invalid interval duration",
            explanation = """
                The interval duration for a `PeriodicWorkRequest` must be at least 15 minutes.
            """,
            androidSpecific = true,
            category = Category.CORRECTNESS,
            severity = Severity.FATAL,
            implementation = Implementation(
                InvalidPeriodicWorkRequestIntervalDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE)
            )
        )
    }

    override fun getApplicableConstructorTypes() = listOf(
        "androidx.work.PeriodicWorkRequest.Builder"
    )

    @Suppress("UNCHECKED_CAST")
    override fun visitConstructor(
        context: JavaContext,
        node: UCallExpression,
        constructor: PsiMethod
    ) {
        if (node.valueArgumentCount >= 2) {
            val type = node.valueArguments[1].getExpressionType()?.canonicalText
            if ("long" == type) {
                val value = node.valueArguments[1].evaluate() as? Long
                // TimeUnit
                val units = node.valueArguments[2].evaluate() as? Pair<ClassId, Name>
                if (value != null && units != null) {
                    val (_, timeUnitType) = units
                    val interval: Long? = when (timeUnitType.identifier) {
                        "NANOSECONDS" -> TimeUnit.MINUTES.convert(value, TimeUnit.NANOSECONDS)
                        "MICROSECONDS" -> TimeUnit.MINUTES.convert(value, TimeUnit.MICROSECONDS)
                        "MILLISECONDS" -> TimeUnit.MINUTES.convert(value, TimeUnit.MILLISECONDS)
                        "SECONDS" -> TimeUnit.MINUTES.convert(value, TimeUnit.SECONDS)
                        "MINUTES" -> value
                        "HOURS" -> TimeUnit.MINUTES.convert(value, TimeUnit.HOURS)
                        "DAYS" -> TimeUnit.MINUTES.convert(value, TimeUnit.DAYS)
                        else -> null
                    }
                    if (interval != null && interval < 15) {
                        context.report(
                            ISSUE,
                            context.getLocation(node),
                            """
                                Interval duration for `PeriodicWorkRequest`s must be at least 15 \
                                minutes.
                            """.trimIndent()
                        )
                    }
                }
            } else if ("java.time.Duration" == type) {
                val source = node.valueArguments[1].asSourceString()
                // Look for the most common Duration specification
                // Example: Duration.ofMinutes(15)
                val regexp = Regex("Duration.of(\\w+)\\((\\d+)\\)")
                val matchResult = regexp.matchEntire(source)
                if (matchResult != null) {
                    val unit = matchResult.groupValues[1]
                    val value = matchResult.groupValues[2].toLong()
                    val interval: Long? = when (unit) {
                        "Nanos" -> TimeUnit.MINUTES.convert(value, TimeUnit.NANOSECONDS)
                        "Millis" -> TimeUnit.MINUTES.convert(value, TimeUnit.MILLISECONDS)
                        "Seconds" -> TimeUnit.MINUTES.convert(value, TimeUnit.SECONDS)
                        "Minutes" -> value
                        "Hours" -> TimeUnit.MINUTES.convert(value, TimeUnit.HOURS)
                        "Days" -> TimeUnit.MINUTES.convert(value, TimeUnit.DAYS)
                        else -> null
                    }
                    if (interval != null && interval < 15) {
                        context.report(
                            ISSUE,
                            context.getLocation(node),
                            """
                                Interval duration for `PeriodicWorkRequest`s must be at least 15 \
                                minutes.
                            """.trimIndent()
                        )
                    }
                }
            }
        }
    }
}
