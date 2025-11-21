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
import org.jetbrains.uast.UBinaryExpression
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

                if (node.methodName !in RELEVANT_METHOD_NAMES) return

                val method = node.resolve() ?: return
                val containingClass =
                    (node.receiver?.getExpressionType() as? PsiClassType)?.resolve()
                        ?: method.containingClass
                        ?: return
                val methodName = method.name
                val paramSig =
                    method.parameterList.parameters.joinToString(",") { it.type.canonicalText }
                val keyExact = if (paramSig.isEmpty()) "$methodName()" else "$methodName($paramSig)"

                val replacement = findReplacement(containingClass, keyExact, methodName) ?: return

                // Optional fix
                val fix =
                    replacement.recommendedReplacement?.let { replacementMethod ->
                        fix()
                            .replace()
                            .with(replacementMethod)
                            .reformat(true)
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

            /** Iteratively search the REPLACEMENTS map through the class hierarchy */
            private fun findReplacement(
                psiClass: PsiClass,
                keyExact: String,
                keyWildcard: String,
            ): Replacement? {
                val queue = ArrayDeque<PsiClass>()
                queue.add(psiClass)
                val visited = mutableSetOf<PsiClass>()
                visited.add(psiClass)

                while (queue.isNotEmpty()) {
                    val current = queue.removeFirst()

                    val fqn = current.qualifiedName
                    if (fqn != null) {
                        REPLACEMENTS["$fqn#$keyExact"]?.let {
                            return it
                        }
                        REPLACEMENTS["$fqn#$keyWildcard"]?.let {
                            return it
                        }
                    }

                    current.supers.forEach { superClass ->
                        if (visited.add(superClass)) {
                            queue.add(superClass)
                        }
                    }
                }
                return null
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
    fun PsiClass.isInstanceOf(qualifiedName: String): Boolean {
        val queue = ArrayDeque<PsiClass>(listOf(this))
        val visited = mutableSetOf<PsiClass>(this)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current.qualifiedName == qualifiedName) {
                return true
            }
            current.supers.forEach { superClass ->
                if (visited.add(superClass)) {
                    queue.add(superClass)
                }
            }
        }
        return false
    }

    companion object {
        private const val CONFIGURATION = "org.gradle.api.artifacts.Configuration"
        private const val CONFIGURABLE_FILE_COLLECTION =
            "org.gradle.api.file.ConfigurableFileCollection"

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

        val CONFIGURATION_CACHE_BROAD_INPUTS =
            Issue.create(
                "GradleConfigurationCacheInputs",
                "Avoid using APIs that capture too much of the environment in the" +
                    "configuration cache",
                """
                See https://docs.gradle.org/current/userguide/configuration_cache_requirements.html
                for more details.
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
        private val REPLACEMENTS: Map<String, Replacement> =
            mapOf(
                // DomainObjectCollection
                "org.gradle.api.DomainObjectCollection#all" to
                    Replacement("configureEach", EAGER_CONFIGURATION_ISSUE),
                "org.gradle.api.DomainObjectCollection#whenObjectAdded" to
                    Replacement("configureEach", EAGER_CONFIGURATION_ISSUE),

                // TaskContainer
                "org.gradle.api.tasks.TaskContainer#any" to
                    Replacement(null, EAGER_CONFIGURATION_ISSUE),
                "org.gradle.api.tasks.TaskContainer#create" to
                    Replacement("register", EAGER_CONFIGURATION_ISSUE),
                "org.gradle.api.tasks.TaskContainer#findByName" to
                    Replacement(null, EAGER_CONFIGURATION_ISSUE),
                "org.gradle.api.tasks.TaskContainer#findByPath" to
                    Replacement(null, EAGER_CONFIGURATION_ISSUE),
                "org.gradle.api.tasks.TaskContainer#forEach" to
                    Replacement(null, EAGER_CONFIGURATION_ISSUE),
                "org.gradle.api.tasks.TaskContainer#iterator" to
                    Replacement(null, EAGER_CONFIGURATION_ISSUE),
                "org.gradle.api.tasks.TaskContainer#getByPath" to
                    Replacement(null, EAGER_CONFIGURATION_ISSUE),
                "org.gradle.api.tasks.TaskContainer#getByName" to
                    Replacement("named", EAGER_CONFIGURATION_ISSUE),
                "org.gradle.api.tasks.TaskContainer#groupBy" to
                    Replacement(null, EAGER_CONFIGURATION_ISSUE),
                "org.gradle.api.tasks.TaskContainer#map" to
                    Replacement(null, EAGER_CONFIGURATION_ISSUE),
                "org.gradle.api.tasks.TaskContainer#mapNotNull" to
                    Replacement(null, EAGER_CONFIGURATION_ISSUE),
                "org.gradle.api.tasks.TaskContainer#replace" to
                    Replacement(null, EAGER_CONFIGURATION_ISSUE),
                "org.gradle.api.tasks.TaskContainer#remove" to
                    Replacement(null, EAGER_CONFIGURATION_ISSUE),
                "org.gradle.api.tasks.TaskContainer#whenTaskAdded" to
                    Replacement("configureEach", EAGER_CONFIGURATION_ISSUE),

                // ConfigurationContainer
                "org.gradle.api.artifacts.ConfigurationContainer#create" to
                    Replacement("register", EAGER_CONFIGURATION_ISSUE),
                "org.gradle.api.artifacts.ConfigurationContainer#maybeCreate" to
                    Replacement("register", EAGER_CONFIGURATION_ISSUE),

                // Project
                "org.gradle.api.Project#evaluationDependsOn" to
                    Replacement(null, PROJECT_ISOLATION_ISSUE),
                "org.gradle.api.Project#evaluationDependsOnChildren" to
                    Replacement(null, PROJECT_ISOLATION_ISSUE),
                "org.gradle.api.Project#findProject" to Replacement(null, PROJECT_ISOLATION_ISSUE),
                "org.gradle.api.Project#findProperty" to
                    Replacement("providers.gradleProperty", PROJECT_ISOLATION_ISSUE),
                "org.gradle.api.Project#hasProperty" to
                    Replacement("providers.gradleProperty", PROJECT_ISOLATION_ISSUE),
                "org.gradle.api.Project#property" to
                    Replacement("providers.gradleProperty", PROJECT_ISOLATION_ISSUE),
                "org.gradle.api.Project#getParent" to Replacement(null, PROJECT_ISOLATION_ISSUE),
                "org.gradle.api.Project#getProperties" to
                    Replacement("providers.gradleProperty", PROJECT_ISOLATION_ISSUE),
                "org.gradle.api.Project#getRootProject" to
                    Replacement("isolated.rootProject", PROJECT_ISOLATION_ISSUE),

                // Task
                "org.gradle.api.Task#mustRunAfter" to Replacement(null, PERFORMANCE_ISSUE),
                "org.gradle.api.Task#setMustRunAfter" to Replacement(null, PERFORMANCE_ISSUE),
                "org.gradle.api.Task#setShouldRunAfter" to Replacement(null, PERFORMANCE_ISSUE),
                "org.gradle.api.Task#shouldRunAfter" to Replacement(null, PERFORMANCE_ISSUE),

                // TaskCollection
                "org.gradle.api.tasks.TaskCollection#getAt" to
                    Replacement("named", EAGER_CONFIGURATION_ISSUE),
                "org.gradle.api.tasks.TaskCollection#matching" to
                    Replacement(null, EAGER_CONFIGURATION_ISSUE),

                // TaskProvider
                "org.gradle.api.tasks.TaskProvider#get" to
                    Replacement(null, EAGER_CONFIGURATION_ISSUE),

                // NamedDomainObjectCollection
                "org.gradle.api.NamedDomainObjectCollection#findAll" to
                    Replacement(null, EAGER_CONFIGURATION_ISSUE),

                // Provider
                "org.gradle.api.provider.Provider#toString" to
                    Replacement("get", TO_STRING_ON_PROVIDER_ISSUE),

                // java.lang.System
                "java.lang.System#getenv()" to Replacement(null, CONFIGURATION_CACHE_BROAD_INPUTS),
                "java.lang.System#getProperties" to
                    Replacement("getProperty", CONFIGURATION_CACHE_BROAD_INPUTS),
            )
        private val RELEVANT_METHOD_NAMES =
            REPLACEMENTS.keys.map { it.substringAfter('#').substringBefore('(') }.toSet()
    }
}

private data class Replacement(val recommendedReplacement: String?, val issue: Issue)
