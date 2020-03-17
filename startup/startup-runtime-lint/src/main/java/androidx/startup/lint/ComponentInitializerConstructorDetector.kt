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

package androidx.startup.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.lang.java.JavaLanguage
import org.jetbrains.uast.UClass
import java.util.EnumSet

/**
 * A [Detector] which ensures that every `ComponentInitializer` has a no argument constructor.
 */
class ComponentInitializerConstructorDetector : Detector(), SourceCodeScanner {
    companion object {
        private const val DESCRIPTION = "Missing ComponentInitializer no-arg constructor"
        val ISSUE = Issue.create(
            id = "EnsureComponentInitializerNoArgConstr",
            briefDescription = DESCRIPTION,
            explanation = """
                Every `ComponentInitializer` must have a no argument constructor.
            """,
            androidSpecific = true,
            category = Category.CORRECTNESS,
            severity = Severity.FATAL,
            implementation = Implementation(
                ComponentInitializerConstructorDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE)
            )
        )
    }

    override fun applicableSuperClasses() = listOf("androidx.startup.ComponentInitializer")

    override fun visitClass(context: JavaContext, declaration: UClass) {
        val name = declaration.qualifiedName

        if (name == "androidx.startup.ComponentInitializer") {
            // This is the component initializer itself.
            return
        }

        if (declaration.isInterface) {
            return
        }

        // isJava() is available in the latest Lint APIs but not the LINT_MIN API
        val isJava = declaration.javaPsi.language == JavaLanguage.INSTANCE

        if (isJava && declaration.constructors.isEmpty()) {
            // Java classes have a default no-arg constructor
            return
        }

        for (constructor in declaration.constructors) {
            if (!constructor.hasParameters()) {
                // Found a no argument constructor.
                return
            }
        }

        // Did not find any no-arg constructors
        val location = context.getLocation(declaration.javaPsi)
        context.report(
            issue = ISSUE,
            location = location,
            message = DESCRIPTION
        )
    }
}
