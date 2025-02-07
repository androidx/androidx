/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.lint

import androidx.compose.lint.Name
import androidx.compose.lint.Package
import androidx.compose.lint.isInPackageName
import androidx.compose.lint.isInvokedWithinComposable
import com.android.SdkConstants
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.UastLintUtils.Companion.tryResolveUDeclaration
import com.android.utils.iterator
import com.android.xml.AndroidManifest
import com.intellij.psi.PsiMember
import java.util.EnumSet
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.matchesQualified
import org.jetbrains.uast.skipParenthesizedExprDown
import org.w3c.dom.Element

/**
 * Detector that warns for calls to Configuration.screenWidthDp/screenHeightDp inside composition,
 * or calls to Configuration.screenWidthDp/screenHeightDp made on a Configuration object that was
 * retrieved using LocalConfiguration.current (since that also comes from composition). Instead
 * LocalWindowInfo.current.containerSize should be used.
 */
class ConfigurationScreenWidthHeightDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UQualifiedReferenceExpression::class.java)

    var isWearProject = false

    override fun beforeCheckEachProject(context: Context) {
        super.beforeCheckEachProject(context)
        isWearProject = isWearProject(context)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) {
                // b/333784604 Ignore wear since this is the recommended API on wear
                if (isWearProject) return
                val resolved = node.resolve() as? PsiMember ?: return
                val name = resolved.name
                if (name != ScreenWidthDp && name != ScreenHeightDp) return

                val containingClass = resolved.containingClass ?: return
                if (
                    containingClass.name != Configuration.shortName ||
                        !containingClass.isInPackageName(Configuration.packageName)
                )
                    return

                // If we are invoking this inside a composable function, report
                if (node.isInvokedWithinComposable()) {
                    report(name, context, node)
                    return
                }

                // Otherwise, check to see if the configuration object was retrieved via
                // LocalConfiguration.current. In which case, this could be replaced with
                // LocalWindowInfo, so we should still warn. For other cases the configuration might
                // come from outside Compose, so it can't be replaced.
                // Simple check to see if the `configuration` receiver is a variable defined as val
                // someVariable = LocalConfiguration.current
                val configurationSource =
                    (node.receiver.skipParenthesizedExprDown().tryResolveUDeclaration()
                            as? UVariable)
                        ?.uastInitializer ?: return
                if (configurationSource.matchesQualified("LocalConfiguration.current")) {
                    report(name, context, node)
                }
            }
        }

    private fun report(referencedFieldName: String, context: JavaContext, node: UElement) {
        context.report(
            ConfigurationScreenWidthHeight,
            node,
            context.getNameLocation(node),
            "Using Configuration.$referencedFieldName instead of LocalWindowInfo.current.containerSize",
        )
    }

    companion object {
        private val ResPackage = Package("android.content.res")
        private val Configuration = Name(ResPackage, "Configuration")
        private const val ScreenWidthDp = "screenWidthDp"
        private const val ScreenHeightDp = "screenHeightDp"

        val ConfigurationScreenWidthHeight =
            Issue.create(
                "ConfigurationScreenWidthHeight",
                "Using Configuration.screenWidthDp/screenHeightDp instead of LocalWindowInfo.current.containerSize",
                "Configuration.screenWidthDp and Configuration.screenHeightDp have different insets behaviour depending on target SDK version, and are rounded to the nearest Dp. This means that using these values in composition to size a layout can result in issues, as these values do not accurately represent the actual available window size. Instead it is recommended to use WindowInfo.containerSize which accurately represents the window size.",
                Category.CORRECTNESS,
                3,
                Severity.WARNING,
                Implementation(
                    ConfigurationScreenWidthHeightDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
            )
    }
}

// TODO: b/386335480 use lint API when available
// This is copied from Lint's WearDetector until it is made public / an equivalent API is provided

private fun isWearProject(context: Context) =
    containsWearFeature(
        if (context.isGlobalAnalysis()) context.mainProject.mergedManifest?.documentElement
        else context.project.mergedManifest?.documentElement
    )

private fun containsWearFeature(manifest: Element?): Boolean {
    if (manifest == null) {
        return false
    }
    for (element in manifest) {
        if (isWearFeature(element)) return true
    }
    return false
}

private fun isWearFeature(element: Element) =
    element.tagName == SdkConstants.TAG_USES_FEATURE &&
        element
            .getAttributeNS(SdkConstants.ANDROID_URI, AndroidManifest.ATTRIBUTE_NAME)
            .equals(FEATURE_WATCH)

private const val FEATURE_WATCH = "android.hardware.type.watch"
