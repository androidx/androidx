/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.lint.Names.Ui.Platform.LocalConfiguration
import androidx.compose.lint.Names.Ui.Platform.LocalResources
import androidx.compose.lint.Package
import androidx.compose.lint.isInPackageName
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.UastLintUtils.Companion.tryResolveUDeclaration
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.isUastChildOf
import org.jetbrains.uast.matchesQualified
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.tryResolve

/**
 * Detector that warns for calls to LocalContext.current.resources and
 * LocalContext.current.resources.configuration - changes to the configuration object will not cause
 * these to recompose, so callers of these APIs will not be notified when it changes. For resources
 * this is important because APIs such as Resources.getString() can return new values when the
 * configuration changes. LocalResources.current and LocalConfiguration.current should be used
 * instead.
 */
class LocalContextResourcesConfigurationReadDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(UQualifiedReferenceExpression::class.java)

    /**
     * List of `LocalContext.current.resources` calls (this includes 'sub calls' inside a larger
     * `LocalContext.current.resources.configuration` call
     */
    private val fullyQualifiedResourceCalls = mutableListOf<UQualifiedReferenceExpression>()

    /** List of `LocalContext.current.resources.configuration` calls */
    private val fullyQualifiedConfigurationCalls = mutableListOf<UQualifiedReferenceExpression>()

    /** List of calls to Context#resources */
    private val contextResourcesCalls = mutableListOf<UExpression>()

    /**
     * List of `context` references that are used to call resources.configuration
     *
     * E.g. for:
     *
     * val context = LocalContext.current val resources = context.resources val configuration =
     * resources.configuration
     *
     * We will add the `context` in `context.resources` to this list
     */
    private val contextsReferencedFromResourcesConfigurationCall = mutableListOf<UExpression>()

    override fun afterCheckFile(context: Context) {
        // Will always be JavaContext when we are checking a Kotlin source file
        if (context is JavaContext) {
            fullyQualifiedResourceCalls.forEach { resourcesCall ->
                // Make sure that this resources call is not inside a configuration call, and that
                // this resources call does not contain the context call that is used by a later
                // resources call
                val isInsideConfigurationCall =
                    fullyQualifiedConfigurationCalls.any { configurationCall ->
                        resourcesCall.isUastChildOf(configurationCall)
                    }
                val containsContextCallCalledByResourcesCall =
                    contextsReferencedFromResourcesConfigurationCall.any { call ->
                        call.isUastChildOf(resourcesCall)
                    }
                if (!isInsideConfigurationCall && !containsContextCallCalledByResourcesCall) {
                    context.report(
                        LocalContextResourcesRead,
                        resourcesCall,
                        context.getNameLocation(resourcesCall),
                        "Reading Resources using $LocalContextCurrentResources",
                        LintFix.create()
                            .replace()
                            .name("Replace with $LocalResourcesCurrent")
                            .all()
                            .with(LocalResourcesCurrent)
                            .imports(LocalResources.javaFqn)
                            .autoFix()
                            .build()
                    )
                }
            }
            contextResourcesCalls.forEach { resourcesCall ->
                // Only report if we didn't report a resources.configuration error that came
                // from the same context within this context.resources call
                if (
                    contextsReferencedFromResourcesConfigurationCall.none { call ->
                        call.isUastChildOf(resourcesCall)
                    }
                ) {
                    context.report(
                        LocalContextResourcesRead,
                        resourcesCall,
                        context.getNameLocation(resourcesCall),
                        "Reading Resources using $LocalContextCurrentResources"
                    )
                }
            }
        }
        fullyQualifiedResourceCalls.clear()
        fullyQualifiedConfigurationCalls.clear()
        contextResourcesCalls.clear()
        contextsReferencedFromResourcesConfigurationCall.clear()
    }

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) {
                // Fast path for whole configuration string - note the later logic would catch
                // this (and variants that use method calls such as getResources() instead), but
                // we want a fast path so we can suggest a replacement.
                if (node.matchesQualified(LocalContextCurrentResourcesConfiguration)) {
                    fullyQualifiedConfigurationCalls += node
                    context.report(
                        LocalContextConfigurationRead,
                        node,
                        context.getNameLocation(node),
                        "Reading Configuration using $LocalContextCurrentResourcesConfiguration",
                        LintFix.create()
                            .replace()
                            .name("Replace with $LocalConfigurationCurrent")
                            .all()
                            .with(LocalConfigurationCurrent)
                            .imports(LocalConfiguration.javaFqn)
                            .autoFix()
                            .build()
                    )
                    return
                }

                // Check for the whole resources string. We need to delay reporting until after we
                // analyze the file, as we could find the resources string inside the configuration
                // string as well, and in that case we only want to report the configuration error.
                // So after we check the file, we only report the resource string if it was not
                // inside a configuration string.
                if (node.matchesQualified(LocalContextCurrentResources)) {
                    fullyQualifiedResourceCalls += node
                    return
                }

                // Simple logic to try and match a few specific cases (there are many cases that
                // this won't warn for) where the chain is split up
                // E.g. val context = LocalContext.current, val resources = context.resources,
                // val configuration = resources.configuration
                // A future improvement would be to catch receiver scope cases, such as
                // `with(LocalContext.current.resources) { configuration... }`, but this is more
                // complicated and error prone

                // See if this is a resources.configuration call or a context.resources call
                val selector = node.selector.skipParenthesizedExprDown()
                val configurationCall = selector.isCallToGetConfiguration()
                val resourcesCall = selector.isCallToGetResources()
                if (!configurationCall && !resourcesCall) return

                // Either the expression with resources when the selector is resources.configuration
                // or the expression with context when the selector is context.resources
                val parent = node.receiver.skipParenthesizedExprDown()

                val contextExpression =
                    if (resourcesCall) {
                        parent
                    } else {
                        findContextExpressionFromResourcesConfigurationExpression(parent) ?: return
                    }

                // Try and find out where this context came from
                val contextSource =
                    when (contextExpression) {
                        // Still part of a qualified expression, e.g. LocalContext.current
                        is UQualifiedReferenceExpression -> contextExpression
                        // Possible reference to a variable, e.g. val context =
                        // LocalContext.current,
                        // and this USimpleNameReferenceExpression is `context`
                        is USimpleNameReferenceExpression -> {
                            // If it is a property such as val context = LocalContext.current, find
                            // the initializer
                            val initializer =
                                (contextExpression.tryResolveUDeclaration() as? UVariable)
                                    ?.uastInitializer ?: return
                            if (initializer !is UQualifiedReferenceExpression) return
                            initializer
                        }
                        else -> return
                    }

                if (contextSource.matchesQualified("LocalContext.current")) {
                    // We can be here from two cases, either we were analyzing a call to
                    // context.resources, or a call to resources.configuration. Since calls to
                    // resources.configuration imply a previous call to context.resources, we only
                    // want to report the resources.configuration error in such a case, and not
                    // the context.resources error. To do that, we need to track the
                    // context used for the resources.configuration call, so that we only
                    // report an error for context.resources calls when there is no
                    // resources.configuration error we are reporting that referenced the same
                    // context
                    if (configurationCall) {
                        contextsReferencedFromResourcesConfigurationCall.add(contextExpression)
                        context.report(
                            LocalContextConfigurationRead,
                            node,
                            context.getNameLocation(node),
                            "Reading Configuration using $LocalContextCurrentResourcesConfiguration"
                        )
                    } else {
                        // context.resources call, so add to list of context.resources calls to
                        // report after we analyze the file, to avoid double reporting as mentioned
                        // above
                        contextResourcesCalls.add(node)
                    }
                }
            }
        }

    /**
     * Given a resources.configuration expression, try and find the context expression that the
     * resources comes from.
     */
    private fun findContextExpressionFromResourcesConfigurationExpression(
        resources: UExpression
    ): UExpression? {
        return when (resources) {
            // Still part of a qualified expression, e.g. context.resources
            is UQualifiedReferenceExpression -> {
                if (!resources.isCallToGetResources()) return null
                // Return the receiver, e.g. `context` in the case of
                // `context.resources`
                resources.receiver.skipParenthesizedExprDown()
            }
            // Possible reference to a variable, e.g. val resources = context.resources,
            // and this USimpleNameReferenceExpression is `resources`
            is USimpleNameReferenceExpression -> {
                // If it is a property such as val resources = context.resources, find
                // the initializer
                val initializer =
                    (resources.tryResolveUDeclaration() as? UVariable)?.uastInitializer
                        ?: return null
                if (initializer !is UQualifiedReferenceExpression) return null
                if (!initializer.isCallToGetResources()) return null
                // Return the receiver, e.g. `context` in the case of
                // `context.resources`
                initializer.receiver.skipParenthesizedExprDown()
            }
            else -> null
        }
    }

    private fun UElement.isCallToGetConfiguration(): Boolean {
        val resolved = tryResolve() as? PsiMethod ?: return false
        return resolved.name == "getConfiguration" && resolved.isInPackageName(ResPackage)
    }

    private fun UElement.isCallToGetResources(): Boolean {
        val resolved = tryResolve() as? PsiMethod ?: return false
        return resolved.name == "getResources" && resolved.isInPackageName(ContentPackage)
    }

    companion object {
        private const val LocalContextCurrentResources = "LocalContext.current.resources"
        private const val LocalContextCurrentResourcesConfiguration =
            "LocalContext.current.resources.configuration"
        private const val LocalResourcesCurrent = "LocalResources.current"
        private const val LocalConfigurationCurrent = "LocalConfiguration.current"
        private val ContentPackage = Package("android.content")
        private val ResPackage = Package("android.content.res")

        val LocalContextConfigurationRead =
            Issue.create(
                "LocalContextConfigurationRead",
                "Reading Configuration using $LocalContextCurrentResourcesConfiguration",
                "Changes to the Configuration object will not cause LocalContext reads to be " +
                    "invalidated, so you may end up with stale values when the Configuration " +
                    "changes. Instead, use $LocalConfigurationCurrent to retrieve the " +
                    "Configuration - this will recompose callers when the Configuration object " +
                    "changes.",
                Category.CORRECTNESS,
                3,
                Severity.ERROR,
                Implementation(
                    LocalContextResourcesConfigurationReadDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
            )

        val LocalContextResourcesRead =
            Issue.create(
                "LocalContextResourcesRead",
                "Reading Resources using $LocalContextCurrentResources",
                "Changes to the Configuration object will not cause " +
                    "$LocalContextCurrentResources reads to be invalidated, so calls to APIs such" +
                    "as Resources.getString() will not be updated when the Configuration " +
                    "changes. Instead, use $LocalResourcesCurrent to retrieve the Resources - " +
                    "this will invalidate callers when the Configuration changes, to ensure that " +
                    "these calls reflect the latest values.",
                Category.CORRECTNESS,
                3,
                Severity.WARNING,
                Implementation(
                    LocalContextResourcesConfigurationReadDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
            )
    }
}
