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
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement

/**
 * Checks for usages of [eager APIs](https://docs.gradle.org/current/userguide/task_configuration_avoidance.html).
 */
class EagerTaskConfigurationDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(
        UCallExpression::class.java
    )

    override fun createUastHandler(context: JavaContext): UElementHandler = object :
        UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            val (containingClassName, replacementMethod) = REPLACEMENTS[node.methodName] ?: return
            val method = node.resolve() ?: return
            val containingClass = method.containingClass ?: return
            // Check that the called method is from the expected class (or a child class) and not an
            // unrelated method with the same name).
            if (
                containingClass.qualifiedName != containingClassName &&
                    containingClass.supers.none { it.qualifiedName == containingClassName }
            ) return

            val fix = fix()
                .replace()
                .with(replacementMethod)
                .reformat(true)
                // Don't auto-fix from the command line because the replacement methods don't have
                // the same return types, so the fixed code likely won't compile.
                .autoFix(robot = false, independent = false)
                .build()
            val incident = Incident(context)
                .issue(ISSUE)
                .location(context.getNameLocation(node))
                .message("Use $replacementMethod instead of ${method.name}")
                .fix(fix)
                .scope(node)
            context.report(incident)
        }
    }

    companion object {
        private const val TASK_CONTAINER = "org.gradle.api.tasks.TaskContainer"
        private const val DOMAIN_OBJECT_COLLECTION = "org.gradle.api.DomainObjectCollection"
        private const val TASK_COLLECTION = "org.gradle.api.tasks.TaskCollection"

        // A map from eager method name to the containing class of the method and the name of the
        // replacement method.
        private val REPLACEMENTS = mapOf(
            "create" to Pair(TASK_CONTAINER, "register"),
            "getByName" to Pair(TASK_CONTAINER, "named"),
            "all" to Pair(DOMAIN_OBJECT_COLLECTION, "configureEach"),
            "whenTaskAdded" to Pair(TASK_CONTAINER, "configureEach"),
            "whenObjectAdded" to Pair(DOMAIN_OBJECT_COLLECTION, "configureEach"),
            "getAt" to Pair(TASK_COLLECTION, "named"),
        )

        val ISSUE = Issue.create(
            "EagerGradleTaskConfiguration",
            "Avoid using eager task APIs",
            """
                Lazy APIs defer task configuration until the task is needed instead of doing
                unnecessary work in the configuration phase.
                See https://docs.gradle.org/current/userguide/task_configuration_avoidance.html for
                more details.
            """,
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(
                EagerTaskConfigurationDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
