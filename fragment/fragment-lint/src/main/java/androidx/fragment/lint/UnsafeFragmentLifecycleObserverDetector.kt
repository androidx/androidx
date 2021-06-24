/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.fragment.lint

import androidx.fragment.lint.UnsafeFragmentLifecycleObserverDetector.Issues.BACK_PRESSED_ISSUE
import androidx.fragment.lint.UnsafeFragmentLifecycleObserverDetector.Issues.LIVEDATA_ISSUE
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UastFacade
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * Lint check for detecting calls to lifecycle aware components with a
 * [androidx.fragment.app.Fragment] instance as the [androidx.lifecycle.LifecycleOwner] while
 * inside the [androidx.fragment.app.Fragment]'s [androidx.fragment.app.Fragment.onCreateView],
 * [androidx.fragment.app.Fragment.onViewCreated],
 * [androidx.fragment.app.Fragment.onActivityCreated], or
 * [androidx.fragment.app.Fragment.onViewStateRestored].
 */
class UnsafeFragmentLifecycleObserverDetector : Detector(), SourceCodeScanner {

    companion object Issues {
        val LIVEDATA_ISSUE = Issue.create(
            id = "FragmentLiveDataObserve",
            briefDescription = "Use getViewLifecycleOwner() as the LifecycleOwner instead of " +
                "a Fragment instance when observing a LiveData object.",
            explanation = """When observing a LiveData object from a fragment's onCreateView, \
                onViewCreated, onActivityCreated, or onViewStateRestored method \
                getViewLifecycleOwner() should be used as the LifecycleOwner rather than the \
                Fragment instance. The Fragment lifecycle can result in the Fragment being \
                active longer than its view. This can lead to unexpected behavior from \
                LiveData objects being observed longer than the Fragment's view is active.""",
            category = Category.CORRECTNESS,
            severity = Severity.ERROR,
            implementation = Implementation(
                UnsafeFragmentLifecycleObserverDetector::class.java, Scope.JAVA_FILE_SCOPE
            ),
            androidSpecific = true
        )

        val BACK_PRESSED_ISSUE = Issue.create(
            id = "FragmentBackPressedCallback",
            briefDescription = "Use getViewLifecycleOwner() as the LifecycleOwner instead of " +
                "a Fragment instance.",
            explanation = """The Fragment lifecycle can result in a Fragment being active \
                longer than its view. This can lead to unexpected behavior from lifecycle aware \
                objects remaining active longer than the Fragment's view. To solve this issue, \
                getViewLifecycleOwner() should be used as a LifecycleOwner rather than the \
                Fragment instance once it is safe to access the view lifecycle in a \
                Fragment's onCreateView, onViewCreated, onActivityCreated, or \
                onViewStateRestored methods.""",
            category = Category.CORRECTNESS,
            severity = Severity.ERROR,
            implementation = Implementation(
                UnsafeFragmentLifecycleObserverDetector::class.java, Scope.JAVA_FILE_SCOPE
            ),
            androidSpecific = true
        )
    }

    private val lifecycleMethods = setOf(
        "onCreateView", "onViewCreated", "onActivityCreated",
        "onViewStateRestored"
    )

    override fun applicableSuperClasses(): List<String>? = listOf(FRAGMENT_CLASS)

    override fun visitClass(context: JavaContext, declaration: UClass) {
        declaration.methods.forEach {
            if (lifecycleMethods.contains(it.name)) {
                val visitor = RecursiveMethodVisitor(context, declaration.name, it.name)
                it.uastBody?.accept(visitor)
            }
        }
    }
}

/**
 * A UAST Visitor that recursively explores all method calls within a
 * [androidx.fragment.app.Fragment] lifecycle method to check for an unsafe method call
 * ([UNSAFE_METHODS]) with a [androidx.fragment.app.Fragment] instance as the lifecycle owner.
 *
 * @param context The context of the lint request.
 * @param originFragmentName The name of the Fragment class being checked.
 * @param lifecycleMethod The name of the originating Fragment lifecycle method.
 */
private class RecursiveMethodVisitor(
    private val context: JavaContext,
    private val originFragmentName: String?,
    private val lifecycleMethod: String
) : AbstractUastVisitor() {
    private val visitedMethods = mutableSetOf<UCallExpression>()

    override fun visitCallExpression(node: UCallExpression): Boolean {
        if (visitedMethods.contains(node)) {
            return super.visitCallExpression(node)
        }
        val psiMethod = node.resolve() ?: return super.visitCallExpression(node)
        if (!checkCall(node, psiMethod) && node.isInteresting(context)) {
            val uastNode = UastFacade.convertElementWithParent(
                psiMethod,
                UMethod::class.java
            ) as? UMethod
            visitedMethods.add(node)
            uastNode?.uastBody?.accept(this)
            visitedMethods.remove(node)
        }
        return super.visitCallExpression(node)
    }

    /**
     * Checks if the current method call is unsafe.
     *
     * Returns `true` and report the appropriate lint issue if an error is found, otherwise return
     * `false`.
     *
     * @param call The [UCallExpression] to check.
     * @param psiMethod The resolved [PsiMethod] of [call].
     * @return `true` if a lint error was found and reported, `false` otherwise.
     */
    private fun checkCall(call: UCallExpression, psiMethod: PsiMethod): Boolean {
        val method = Method(psiMethod.containingClass?.qualifiedName, psiMethod.name)
        val issue = UNSAFE_METHODS[method] ?: return false
        val argMap = context.evaluator.computeArgumentMapping(call, psiMethod)
        argMap.forEach { (arg, param) ->
            if (arg.getExpressionType().extends(context, FRAGMENT_CLASS) &&
                !arg.getExpressionType().extends(context, DIALOG_FRAGMENT_CLASS) &&
                param.type.extends(context, "androidx.lifecycle.LifecycleOwner")
            ) {
                val argType = PsiTypesUtil.getPsiClass(arg.getExpressionType())
                if (argType == call.getContainingUClass()?.javaPsi) {
                    val methodFix = if (isKotlin(context.psiFile)) {
                        "viewLifecycleOwner"
                    } else {
                        "getViewLifecycleOwner()"
                    }
                    context.report(
                        issue, context.getLocation(arg),
                        "Use $methodFix as the LifecycleOwner.",
                        LintFix.create()
                            .replace()
                            .with(methodFix)
                            .build()
                    )
                } else {
                    context.report(
                        issue, context.getLocation(call),
                        "Unsafe call to ${call.methodName} with Fragment instance as " +
                            "LifecycleOwner from $originFragmentName.$lifecycleMethod."
                    )
                }
                return true
            }
        }
        return false
    }
}

/**
 * Checks if the [UCallExpression] is a call that should be explored. If the call chain
 * will exit the current class without reference to the [androidx.fragment.app.Fragment] instance
 * then the call chain does not need to be explored further.
 *
 * @return Whether this [UCallExpression] is to a call within the Fragment class or has a
 *         reference to the Fragment passed as a parameter.
 */
internal fun UCallExpression.isInteresting(context: JavaContext): Boolean {
    if (PsiTypesUtil.getPsiClass(receiverType) == this.getContainingUClass()?.javaPsi) {
        return true
    }
    if (valueArgumentCount > 0) {
        valueArguments.forEach {
            if (it.getExpressionType().extends(context, FRAGMENT_CLASS)) {
                return true
            }
        }
    }
    return false
}

internal data class Method(val cls: String?, val name: String)

internal val UNSAFE_METHODS = mapOf(
    Method("androidx.lifecycle.LiveData", "observe") to LIVEDATA_ISSUE,
    Method("androidx.lifecycle.LiveDataKt", "observe") to LIVEDATA_ISSUE,
    Method("androidx.activity.OnBackPressedDispatcher", "addCallback") to BACK_PRESSED_ISSUE
)

private const val FRAGMENT_CLASS = "androidx.fragment.app.Fragment"
private const val DIALOG_FRAGMENT_CLASS = "androidx.fragment.app.DialogFragment"
