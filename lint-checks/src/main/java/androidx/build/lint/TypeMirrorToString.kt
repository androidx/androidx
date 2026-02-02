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

package androidx.build.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClass
import org.jetbrains.uast.UCallExpression

class TypeMirrorToString : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return TypeMirrorHandler(context)
    }

    private inner class TypeMirrorHandler(private val context: JavaContext) : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            if (node.methodName != "toString") return
            val method = node.resolve() ?: return
            val containingClass = method.containingClass ?: return
            if (containingClass.isInstanceOf("javax.lang.model.type.TypeMirror")) {
                // Instead of calling `receiver.toString()`, call `TypeName.get(receiver).toString`
                val fix =
                    node.receiver?.asSourceString()?.let { receiver ->
                        fix()
                            .replace()
                            .name("Use TypeName.toString")
                            .text(receiver)
                            .with("com.squareup.javapoet.TypeName.get($receiver)")
                            .reformat(true)
                            .shortenNames()
                            .autoFix()
                            .build()
                    }

                val incident =
                    Incident(context)
                        .fix(fix)
                        .issue(ISSUE)
                        .location(context.getLocation(node))
                        .message("TypeMirror.toString includes annotations")
                        .scope(node)
                context.report(incident)
            }
        }

        /** Checks if the class is [qualifiedName] or has [qualifiedName] as a super type. */
        private fun PsiClass.isInstanceOf(qualifiedName: String): Boolean =
            // Recursion will stop when this hits Object, which has no [supers]
            qualifiedName == this.qualifiedName || supers.any { it.isInstanceOf(qualifiedName) }
    }

    companion object {
        val ISSUE =
            Issue.create(
                "TypeMirrorToString",
                "Avoid using TypeMirror.toString",
                """
                    This method includes type-use annotations in the string, which can lead to bugs
                    when comparing the type string against unannotated type string.

                    If you need a type string that includes annotations, you can suppress this lint.
                """,
                Category.CORRECTNESS,
                5,
                Severity.ERROR,
                Implementation(TypeMirrorToString::class.java, Scope.JAVA_FILE_SCOPE)
            )
    }
}
