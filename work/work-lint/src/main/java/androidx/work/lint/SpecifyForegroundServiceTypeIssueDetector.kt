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

import com.android.SdkConstants.ANDROID_URI
import com.android.SdkConstants.ATTR_NAME
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.w3c.dom.Element
import java.util.EnumSet

/**
 * Detects usage of `ForegroundInfo` with the `foregroundServiceType` and ensure that the service
 * type is defined in the `AndroidManifest.xml`.
 */
class SpecifyForegroundServiceTypeIssueDetector : Detector(), SourceCodeScanner, XmlScanner {

    private val knownServiceTypes = mutableSetOf<String>()

    companion object {
        val ISSUE = Issue.create(
            id = "SpecifyForegroundServiceType",
            briefDescription = "Specify foreground service type",
            explanation = """
                When using the setForegroundAsync() API, the application must override <service /> \
                entry for `SystemForegroundService` to include the foreground service type in the \
                 `AndroidManifest.xml` file.
            """,
            androidSpecific = true,
            category = Category.CORRECTNESS,
            severity = Severity.FATAL,
            implementation = Implementation(
                SpecifyForegroundServiceTypeIssueDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.MANIFEST)
            )
        )

        private val SERVICE_TYPE_MAPPING = mapOf(
            1 to "dataSync",
            2 to "mediaPlayback",
            4 to "phoneCall",
            8 to "location",
            16 to "connectedDevice",
            32 to "mediaProjection"
        )
    }

    override fun getApplicableConstructorTypes() = listOf("androidx.work.ForegroundInfo")

    override fun getApplicableElements() = listOf("service")

    override fun visitElement(context: XmlContext, element: Element) {
        val name = element.getAttributeNS(ANDROID_URI, ATTR_NAME)
        if ("androidx.work.impl.foreground.SystemForegroundService" == name) {
            val serviceTypes = element.getAttributeNS(ANDROID_URI, "foregroundServiceType")
            if (serviceTypes != null) {
                knownServiceTypes += serviceTypes.split("|")
            }
        }
    }

    override fun visitConstructor(
        context: JavaContext,
        node: UCallExpression,
        constructor: PsiMethod
    ) {
        if (node.valueArgumentCount > 2) {
            val type = node.valueArguments[2].evaluate()
            if (type != null && type is Int && type > 0) {
                for ((mask, name) in SERVICE_TYPE_MAPPING) {
                    if (type and mask > 0) {
                        if (name !in knownServiceTypes) {
                            context.report(
                                issue = ISSUE,
                                location = context.getLocation(node),
                                message = "Missing $name foregroundServiceType in " +
                                    "the AndroidManifest.xml"
                            )
                        }
                    }
                }
            }
        }
    }
}
