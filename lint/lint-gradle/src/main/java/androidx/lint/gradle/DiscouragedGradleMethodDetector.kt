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

package androidx.lint.gradle

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression

/**
 * Checks for usages of
 * [eager APIs](https://docs.gradle.org/current/userguide/task_configuration_avoidance.html) and
 * [project isolation unsafe APIs](https://docs.gradle.org/nightly/userguide/isolated_projects.html)
 */
class DiscouragedGradleMethodDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UCallExpression::class.java, UExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                checkForConfigurationToConfigurableFileCollection(node)
                val methodName = node.methodName
                val potentialReplacements = REPLACEMENTS[methodName] ?: return
                val containingClass = (node.receiverType as? PsiClassType)?.resolve() ?: return
                // Check that the called method is from the expected class (or a child class) and
                // not an unrelated method with the same name).
                potentialReplacements.forEach { (containingClassName, replacement) ->
                    if (!containingClass.isInstanceOf(containingClassName)) return@forEach

                    val fix =
                        replacement.recommendedReplacement?.let {
                            fix()
                                .replace()
                                .with(it)
                                .reformat(true)
                                // Don't auto-fix from the command line because the replacement
                                // methods
                                // don't
                                // have the same return types, so the fixed code likely won't
                                // compile.
                                .autoFix(robot = false, independent = false)
                                .build()
                        }
                    val message =
                        replacement.recommendedReplacement?.let { "Use $it instead of $methodName" }
                            ?: "Avoid using method $methodName"

                    val incident =
                        Incident(context)
                            .issue(replacement.issue)
                            .location(context.getNameLocation(node))
                            .message(message)
                            .fix(fix)
                            .scope(node)
                    context.report(incident)
                }
            }

            private fun checkForConfigurationToConfigurableFileCollection(node: UCallExpression) {
                if (node.methodName != "from") return
                val containingClass = (node.receiverType as? PsiClassType)?.resolve() ?: return
                // Check that the called method is from the expected class (or a child class) and
                // not an unrelated method with the same name).
                if (!containingClass.isInstanceOf(CONFIGURABLE_FILE_COLLECTION)) return
                val hasConfigurationParameter =
                    node.valueArguments.any { parameter ->
                        val parameterType =
                            (parameter.getExpressionType() as? PsiClassType)?.resolve()
                                ?: return@any false
                        parameterType.isInstanceOf(CONFIGURATION)
                    }
                if (!hasConfigurationParameter) return
                val incident =
                    Incident(context)
                        .issue(EAGER_CONFIGURATION_ISSUE)
                        .location(context.getNameLocation(node))
                        .message(
                            "Passing Configuration to ConfigurableFileCollection.from " +
                                "results in eager resolution of this configuration. Instead use " +
                                "project.files(configuration) or " +
                                "configuration.incoming.artifactView {}.files to wrap the " +
                                "configuration making it lazy."
                        )
                        .scope(node)
                context.report(incident)
            }

            /** Check for implicit calls to Provider.toString(). */
            override fun visitExpression(node: UExpression) {
                val parent = node.sourcePsi?.parent ?: return
                // Check if the node is part of a Kotlin formatted string.
                if (parent is KtStringTemplateEntry) {
                    val type = node.getExpressionType() ?: return
                    // Check if type is Provider
                    if (
                        type is PsiClassType &&
                            type.resolve()?.isInstanceOf("org.gradle.api.provider.Provider") == true
                    ) {
                        // Use `Provider.get()` to not call `toString()` directly on the Provider.
                        val nodeWithGet = node.asSourceString() + ".get()"
                        // Curly braces are required for string templates more complex than a simple
                        // reference, which the replacement will be. Check if the original template
                        // already has braces, and add them if not.
                        val replacement =
                            if (parent is KtSimpleNameStringTemplateEntry) {
                                "{$nodeWithGet}"
                            } else {
                                nodeWithGet
                            }
                        val fix =
                            fix()
                                .replace()
                                .with(replacement)
                                .reformat(true)
                                // Allow applying the fix from the command line
                                .autoFix(robot = true, independent = true)
                                .build()

                        val incident =
                            Incident(context)
                                .issue(TO_STRING_ON_PROVIDER_ISSUE)
                                .location(context.getNameLocation(node))
                                .message("Implicit usage of toString on a Provider")
                                .scope(node)
                                .fix(fix)

                        context.report(incident)
                    }
                }
            }
        }

    /** Checks if the class is [qualifiedName] or has [qualifiedName] as a super type. */
    fun PsiClass.isInstanceOf(qualifiedName: String): Boolean =
        // Recursion will stop when this hits Object, which has no [supers]
        qualifiedName == this.qualifiedName || supers.any { it.isInstanceOf(qualifiedName) }

    companion object {
        private const val CONFIGURATION = "org.gradle.api.artifacts.Configuration"
        private const val CONFIGURATION_CONTAINER =
            "org.gradle.api.artifacts.ConfigurationContainer"
        private const val CONFIGURABLE_FILE_COLLECTION =
            "org.gradle.api.file.ConfigurableFileCollection"
        private const val PROJECT = "org.gradle.api.Project"
        private const val TASK = "org.gradle.api.Task"
        private const val TASK_CONTAINER = "org.gradle.api.tasks.TaskContainer"
        private const val TASK_PROVIDER = "org.gradle.api.tasks.TaskProvider"
        private const val DOMAIN_OBJECT_COLLECTION = "org.gradle.api.DomainObjectCollection"
        private const val TASK_COLLECTION = "org.gradle.api.tasks.TaskCollection"
        private const val NAMED_DOMAIN_OBJECT_COLLECTION =
            "org.gradle.api.NamedDomainObjectCollection"
        private const val PROVIDER = "org.gradle.api.provider.Provider"

        val EAGER_CONFIGURATION_ISSUE =
            Issue.create(
                "EagerGradleConfiguration",
                "Avoid using eager task APIs",
                """
                Lazy APIs defer creating and configuring objects until they are needed instead of
                doing unnecessary work in the configuration phase.
                See https://docs.gradle.org/current/userguide/task_configuration_avoidance.html for
                more details.
            """,
                Category.CORRECTNESS,
                5,
                Severity.ERROR,
                Implementation(DiscouragedGradleMethodDetector::class.java, Scope.JAVA_FILE_SCOPE),
            )

        val PROJECT_ISOLATION_ISSUE =
            Issue.create(
                "GradleProjectIsolation",
                "Avoid using APIs that are not project isolation safe",
                """
                Using APIs that reach out cross projects makes it not safe for Gradle project
                isolation.
                See https://docs.gradle.org/nightly/userguide/isolated_projects.html for
                more details.
            """,
                Category.CORRECTNESS,
                5,
                Severity.ERROR,
                Implementation(DiscouragedGradleMethodDetector::class.java, Scope.JAVA_FILE_SCOPE),
            )

        val TO_STRING_ON_PROVIDER_ISSUE =
            Issue.create(
                "GradleLikelyBug",
                "Use of this API is likely a bug",
                """
                    Calling Provider.toString() will return you a generic hash of the instance of this provider.
                    You most likely want to call Provider.get() method to get the actual value instead of the
                    provider.
                    """,
                Category.CORRECTNESS,
                5,
                Severity.ERROR,
                Implementation(DiscouragedGradleMethodDetector::class.java, Scope.JAVA_FILE_SCOPE),
            )

        val PERFORMANCE_ISSUE =
            Issue.create(
                "GradlePerformance",
                "Use of this API is expensive",
                """
                    Calling Task.mustRunAfter and Task.shouldRunAfter is expensive as it causes Gradle to traverse
                    the task graph a second time in order to re-order tasks and fix these constraints.
                    """,
                Category.CORRECTNESS,
                5,
                Severity.ERROR,
                Implementation(DiscouragedGradleMethodDetector::class.java, Scope.JAVA_FILE_SCOPE),
            )

        // A map from eager method name to the containing class of the method and the name of the
        // replacement method, if there is a direct equivalent.
        private val REPLACEMENTS =
            mapOf(
                "all" to
                    mapOf(
                        DOMAIN_OBJECT_COLLECTION to
                            Replacement("configureEach", EAGER_CONFIGURATION_ISSUE)
                    ),
                "any" to mapOf(TASK_CONTAINER to Replacement(null, EAGER_CONFIGURATION_ISSUE)),
                "create" to
                    mapOf(
                        TASK_CONTAINER to Replacement("register", EAGER_CONFIGURATION_ISSUE),
                        CONFIGURATION_CONTAINER to
                            Replacement("register", EAGER_CONFIGURATION_ISSUE),
                    ),
                "evaluationDependsOn" to
                    mapOf(PROJECT to Replacement(null, PROJECT_ISOLATION_ISSUE)),
                "evaluationDependsOnChildren" to
                    mapOf(PROJECT to Replacement(null, PROJECT_ISOLATION_ISSUE)),
                "findAll" to
                    mapOf(
                        NAMED_DOMAIN_OBJECT_COLLECTION to
                            Replacement(null, EAGER_CONFIGURATION_ISSUE)
                    ),
                "findByName" to
                    mapOf(TASK_CONTAINER to Replacement(null, EAGER_CONFIGURATION_ISSUE)),
                "findByPath" to
                    mapOf(TASK_CONTAINER to Replacement(null, EAGER_CONFIGURATION_ISSUE)),
                "findProject" to mapOf(PROJECT to Replacement(null, PROJECT_ISOLATION_ISSUE)),
                "findProperty" to
                    mapOf(
                        PROJECT to Replacement("providers.gradleProperty", PROJECT_ISOLATION_ISSUE)
                    ),
                "forEach" to mapOf(TASK_CONTAINER to Replacement(null, EAGER_CONFIGURATION_ISSUE)),
                "hasProperty" to
                    mapOf(
                        PROJECT to Replacement("providers.gradleProperty", PROJECT_ISOLATION_ISSUE)
                    ),
                "property" to
                    mapOf(
                        PROJECT to Replacement("providers.gradleProperty", PROJECT_ISOLATION_ISSUE)
                    ),
                "iterator" to mapOf(TASK_CONTAINER to Replacement(null, EAGER_CONFIGURATION_ISSUE)),
                "get" to mapOf(TASK_PROVIDER to Replacement(null, EAGER_CONFIGURATION_ISSUE)),
                "getAt" to
                    mapOf(TASK_COLLECTION to Replacement("named", EAGER_CONFIGURATION_ISSUE)),
                "getByPath" to
                    mapOf(TASK_CONTAINER to Replacement(null, EAGER_CONFIGURATION_ISSUE)),
                "getByName" to
                    mapOf(TASK_CONTAINER to Replacement("named", EAGER_CONFIGURATION_ISSUE)),
                "getParent" to mapOf(PROJECT to Replacement(null, PROJECT_ISOLATION_ISSUE)),
                "getProperties" to
                    mapOf(
                        PROJECT to Replacement("providers.gradleProperty", PROJECT_ISOLATION_ISSUE)
                    ),
                "getRootProject" to
                    mapOf(PROJECT to Replacement("isolated.rootProject", PROJECT_ISOLATION_ISSUE)),
                "groupBy" to mapOf(TASK_CONTAINER to Replacement(null, EAGER_CONFIGURATION_ISSUE)),
                "matching" to
                    mapOf(TASK_COLLECTION to Replacement(null, EAGER_CONFIGURATION_ISSUE)),
                "map" to mapOf(TASK_CONTAINER to Replacement(null, EAGER_CONFIGURATION_ISSUE)),
                "mapNotNull" to
                    mapOf(TASK_CONTAINER to Replacement(null, EAGER_CONFIGURATION_ISSUE)),
                "maybeCreate" to
                    mapOf(
                        CONFIGURATION_CONTAINER to
                            Replacement("register", EAGER_CONFIGURATION_ISSUE)
                    ),
                "mustRunAfter" to mapOf(TASK to Replacement(null, PERFORMANCE_ISSUE)),
                "replace" to mapOf(TASK_CONTAINER to Replacement(null, EAGER_CONFIGURATION_ISSUE)),
                "remove" to mapOf(TASK_CONTAINER to Replacement(null, EAGER_CONFIGURATION_ISSUE)),
                "setMustRunAfter" to mapOf(TASK to Replacement(null, PERFORMANCE_ISSUE)),
                "setShouldRunAfter" to mapOf(TASK to Replacement(null, PERFORMANCE_ISSUE)),
                "shouldRunAfter" to mapOf(TASK to Replacement(null, PERFORMANCE_ISSUE)),
                "toString" to mapOf(PROVIDER to Replacement("get", TO_STRING_ON_PROVIDER_ISSUE)),
                "whenTaskAdded" to
                    mapOf(
                        TASK_CONTAINER to Replacement("configureEach", EAGER_CONFIGURATION_ISSUE)
                    ),
                "whenObjectAdded" to
                    mapOf(
                        DOMAIN_OBJECT_COLLECTION to
                            Replacement("configureEach", EAGER_CONFIGURATION_ISSUE)
                    ),
            )
    }
}

private data class Replacement(val recommendedReplacement: String?, val issue: Issue)
