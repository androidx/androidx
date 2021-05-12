/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.lifecycle.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * Lint check for detecting calls to the suspend `repeatOnLifecycle` APIs in wrong lifecycle
 * methods of [androidx.fragment.app.Fragment] or [androidx.core.app.ComponentActivity].
 */
class RepeatOnLifecycleDetector : Detector(), SourceCodeScanner {

    companion object {
        val ISSUE = Issue.create(
            id = "RepeatOnLifecycleWrongUsage",
            briefDescription = "Wrong usage of repeatOnLifecycle.",
            explanation = """The repeatOnLifecycle APIs should be used when the View is created, \
                that is in the `onCreate` lifecycle method for Activities, or `onViewCreated` in \
                case you're using Fragments.""",
            category = Category.CORRECTNESS,
            severity = Severity.ERROR,
            implementation = Implementation(
                RepeatOnLifecycleDetector::class.java, Scope.JAVA_FILE_SCOPE
            ),
            androidSpecific = true
        )
    }

    private val lifecycleMethods = setOf("onStart", "onResume")

    override fun applicableSuperClasses(): List<String>? = listOf(FRAGMENT_CLASS, ACTIVITY_CLASS)

    override fun visitClass(context: JavaContext, declaration: UClass) {
        if (!isKotlin(context.psiFile)) return // Check only Kotlin files

        val visitedMethods = mutableSetOf<PsiMethod>()
        declaration.methods.forEach { method ->
            if (lifecycleMethods.contains(method.name)) {
                val visitor = RecursiveMethodVisitor(
                    context, declaration.name, method, visitedMethods
                )
                method.uastBody?.accept(visitor)
            }
        }
    }
}

/**
 * A UAST Visitor that recursively explores all method calls within an Activity or Fragment
 * lifecycle method to check for wrong method calls to repeatOnLifecycle.
 *
 * @param context The context of the lint request.
 * @param originClassName The name of the class being checked.
 * @param lifecycleMethod The originating lifecycle method.
 */
private class RecursiveMethodVisitor(
    private val context: JavaContext,
    private val originClassName: String?,
    private val lifecycleMethod: PsiMethod,
    private val visitedMethods: MutableSet<PsiMethod>
) : AbstractUastVisitor() {

    override fun visitCallExpression(node: UCallExpression): Boolean {
        val psiMethod = node.resolve() ?: return super.visitCallExpression(node)
        if (visitedMethods.contains(psiMethod)) {
            return super.visitCallExpression(node)
        }
        // Don't add UNSAFE_METHOD to the list of visitedMethods
        if (psiMethod.name != UNSAFE_METHOD.name) {
            visitedMethods.add(psiMethod)
        }
        // Check current method and report if there's a wrong repeatOnLifecycle usage
        if (!checkMethodCall(psiMethod, node)) {
            val uastNode = context.uastContext.getMethod(psiMethod)
            uastNode.uastBody?.accept(this)
        }
        return super.visitCallExpression(node)
    }

    /**
     * Checks if the current method call is not correct.
     *
     * Returns `true` and report the appropriate lint issue if an error is found, otherwise return
     * `false`.
     *
     * @param psiMethod The resolved [PsiMethod] of the call to check.
     * @param expression Original expression.
     * @return `true` if a lint error was found and reported, `false` otherwise.
     */
    private fun checkMethodCall(psiMethod: PsiMethod, expression: UCallExpression): Boolean {
        val method = Method(psiMethod.containingClass?.qualifiedName, psiMethod.name)
        return if (method == UNSAFE_METHOD) {
            context.report(
                RepeatOnLifecycleDetector.ISSUE,
                context.getLocation(expression),
                "Wrong usage of ${method.name} from $originClassName.${lifecycleMethod.name}."
            )
            true
        } else {
            false
        }
    }
}

internal data class Method(val cls: String?, val name: String)

private val UNSAFE_METHOD = Method(
    "androidx.lifecycle.RepeatOnLifecycleKt", "repeatOnLifecycle"
)

private const val FRAGMENT_CLASS = "androidx.fragment.app.Fragment"
private const val ACTIVITY_CLASS = "androidx.core.app.ComponentActivity"
