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

package androidx.build.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.asJava.classes.isPrivateOrParameterInPrivateMethod
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.uast.UAnonymousClass
import org.jetbrains.uast.UClass

class PrivateConstructorForUtilityClass : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {
        override fun visitClass(node: UClass) {
            if (node.isInterface ||
                node.isEnum ||
                node.hasModifierProperty(PsiModifier.ABSTRACT) ||
                node is UAnonymousClass ||
                // If this is a subclass, then don't flag it.
                node.supers.any { !it.qualifiedName.equals("java.lang.Object") } ||
                // Don't run for Kotlin, for now at least
                node.containingFile.fileType == KotlinFileType.INSTANCE
            ) {
                return
            }
            // If all constructors are already private or if not all methods are static then return
            if ((
                node.constructors.all { it.isPrivateOrParameterInPrivateMethod() } && node
                    .constructors.isNotEmpty()
                ) ||
                node.methods.any { !it.isStatic && !it.isConstructor } ||
                node.methods.none { !it.isConstructor } ||
                node.fields.any { !it.isStatic }
            ) {
                return
            }
            // This is a utility class with a non private constructor
            context.report(
                ISSUE, node,
                context.getNameLocation(node),
                "Utility class with non private constructor",
                null
            )
        }
    }

    companion object {
        val ISSUE = Issue.create(
            "PrivateConstructorForUtilityClass",
            "Utility classes should have a private constructor",
            "Classes which are not intended to be instantiated should be made non-instantiable " +
                "with a private constructor. This includes utility classes (classes with " +
                "only static members), and the main class.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(PrivateConstructorForUtilityClass::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}
