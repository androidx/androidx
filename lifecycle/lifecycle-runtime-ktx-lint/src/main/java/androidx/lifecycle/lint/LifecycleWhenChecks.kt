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

package androidx.lifecycle.lint

import androidx.lifecycle.lint.LifecycleWhenChecks.Companion.ISSUE
import androidx.lifecycle.lint.LifecycleWhenVisitor.SearchState.DONT_SEARCH
import androidx.lifecycle.lint.LifecycleWhenVisitor.SearchState.FOUND
import androidx.lifecycle.lint.LifecycleWhenVisitor.SearchState.SEARCH
import com.android.SdkConstants
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.asJava.elements.KtLightModifierList
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIfExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UTryExpression
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.tryResolve
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.jetbrains.uast.visitor.UastVisitor
import java.util.ArrayDeque

// both old and new ones
private val CONTINUATION_NAMES = setOf("kotlin.coroutines.Continuation<? super kotlin.Unit>",
    "kotlin.coroutines.experimental.Continuation<? super kotlin.Unit>")

internal fun errorMessage(whenMethodName: String) =
    "Unsafe View access from finally/catch block inside of `Lifecycle.$whenMethodName` scope"

internal const val SECONDARY_ERROR_MESSAGE = "Internal View access"

internal val APPLICABLE_METHOD_NAMES = listOf("whenCreated", "whenStarted", "whenResumed")

class LifecycleWhenChecks : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = APPLICABLE_METHOD_NAMES

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val valueArguments = node.valueArguments
        if (valueArguments.size != 1 || !method.isLifecycleWhenExtension(context)) {
            return
        }
        (valueArguments[0] as? ULambdaExpression)?.body
            ?.accept(LifecycleWhenVisitor(context, method.name))
    }

    companion object {
        val ISSUE = Issue.create(
            id = "UnsafeLifecycleWhenUsage",
            briefDescription = "Unsafe UI operation in finally/catch of " +
                    "Lifecycle.whenStarted of similar method",
            explanation = """If the `Lifecycle` is destroyed within the block of \
                    `Lifecycle.whenStarted` or any similar `Lifecycle.when` method is suspended, \
                    the block will be cancelled, which will also cancel any child coroutine \
                    launched inside the block. As as a result, If you have a try finally block \
                    in your code, the finally might run after the Lifecycle moves outside \
                    the desired state. It is recommended to check the `Lifecycle.isAtLeast` \
                    before accessing UI in finally block. Similarly, \
                    if you have a catch statement that might catch `CancellationException`, \
                    you should check the `Lifecycle.isAtLeast` before accessing the UI. See \
                    documentation of `Lifecycle.whenStateAtLeast` for more details""",
            category = Category.CORRECTNESS,
            severity = Severity.ERROR,
            implementation = Implementation(LifecycleWhenChecks::class.java, Scope.JAVA_FILE_SCOPE),
            androidSpecific = true
        )
    }
}

internal class LifecycleWhenVisitor(
    private val context: JavaContext,
    private val whenMethodName: String
) : AbstractUastVisitor() {
    enum class SearchState { DONT_SEARCH, SEARCH, FOUND }

    data class State(val checkUIAccess: Boolean, val suspendCallSearch: SearchState)

    fun State.foundSuspendCall() = suspendCallSearch == FOUND

    private val states = ArrayDeque<State>()

    init {
        states.push(State(checkUIAccess = false, suspendCallSearch = DONT_SEARCH))
    }

    private val currentState: State get() = states.first
    private val recursiveHelper = RecursiveVisitHelper()

    fun withNewState(state: State, block: () -> Unit): State {
        states.push(state)
        block()
        val lastState = states.pop()
        // inner scope found suspend call and current state is looking for it => propagate it up
        if (currentState.suspendCallSearch == SEARCH && lastState.foundSuspendCall()) {
            updateSuspendCallSearch(FOUND)
        }
        return lastState
    }

    fun withNewState(suspendCallSearch: SearchState, block: () -> Unit): State {
        return withNewState(State(currentState.checkUIAccess, suspendCallSearch), block)
    }

    fun withNewState(checkUIAccess: Boolean, block: () -> Unit): State {
        return withNewState(State(checkUIAccess, currentState.suspendCallSearch), block)
    }

    override fun visitTryExpression(node: UTryExpression): Boolean {
        val stateAfterTry = withNewState(SEARCH) { node.tryClause.accept(this) }
        val checkView = currentState.checkUIAccess || stateAfterTry.foundSuspendCall()
        // TODO: support catch
        withNewState(checkView) { node.finallyClause?.accept(this) }
        return true
    }

    fun updateSuspendCallSearch(newState: SearchState) {
        val previous = states.pop()
        states.push(State(previous.checkUIAccess, newState))
    }

    override fun visitCallExpression(node: UCallExpression): Boolean {
        val psiMethod = node.resolve() ?: return super.visitCallExpression(node)

        if (psiMethod.isSuspend()) {
            updateSuspendCallSearch(FOUND)
            // go inside and check it doesn't access
            recursiveHelper.visitIfNeeded(psiMethod, this)
        }

        if (currentState.checkUIAccess) {
            checkUiAccess(context, node, whenMethodName)
        }
        return super.visitCallExpression(node)
    }

    override fun visitLambdaExpression(node: ULambdaExpression): Boolean {
        // we probably should actually look at contracts,
        // because only `callsInPlace` lambdas inherit coroutine scope. But contracts aren't stable
        // yet =(
        // if lambda is suspending it means something else defined its scope
        return node.isSuspendLambda() || super.visitLambdaExpression(node)
    }

    // ignore classes defined inline
    override fun visitClass(node: UClass) = true

    // ignore fun defined inline
    override fun visitDeclaration(node: UDeclaration) = true

    override fun visitIfExpression(node: UIfExpression): Boolean {
        if (!currentState.checkUIAccess) return false
        val method = node.condition.tryResolve() as? PsiMethod ?: return false
        if (method.isLifecycleIsAtLeastMethod(context)) {
            withNewState(checkUIAccess = false) { node.thenExpression?.accept(this) }
            node.elseExpression?.accept(this)
            return true
        }
        return false
    }
}

private const val DISPATCHER_CLASS_NAME = "androidx.lifecycle.PausingDispatcherKt"
private const val LIFECYCLE_CLASS_NAME = "androidx.lifecycle.Lifecycle"

private fun PsiMethod.isLifecycleWhenExtension(context: JavaContext): Boolean {
    return name in APPLICABLE_METHOD_NAMES &&
            context.evaluator.isMemberInClass(this, DISPATCHER_CLASS_NAME) &&
            context.evaluator.isStatic(this)
}

private fun PsiMethod.isLifecycleIsAtLeastMethod(context: JavaContext): Boolean {
    return name == "isAtLeast" && context.evaluator.isMemberInClass(this, LIFECYCLE_CLASS_NAME)
}

// TODO: find a better way!
private fun ULambdaExpression.isSuspendLambda(): Boolean {
    val expressionClass = getExpressionType() as? PsiClassType ?: return false
    val params = expressionClass.parameters
    // suspend functions are FunctionN<*, Continuation, Obj>
    if (params.size < 2) {
        return false
    }
    val superBound = (params[params.size - 2] as? PsiWildcardType)?.superBound as? PsiClassType
    return if (superBound != null) {
        superBound.canonicalText in CONTINUATION_NAMES
    } else {
        false
    }
}

private fun PsiMethod.isSuspend(): Boolean {
    val modifiers = modifierList as? KtLightModifierList<*>
    return modifiers?.kotlinOrigin?.hasModifier(KtTokens.SUSPEND_KEYWORD) ?: false
}

fun checkUiAccess(context: JavaContext, node: UCallExpression, whenMethodName: String) {
    val checkVisitor = CheckAccessUiVisitor(context)
    node.accept(checkVisitor)
    checkVisitor.uiAccessNode?.let { accessNode ->
        val mainLocation = context.getLocation(node)
        if (accessNode != node) {
            mainLocation.withSecondary(context.getLocation(accessNode), SECONDARY_ERROR_MESSAGE)
        }
        context.report(ISSUE, mainLocation, errorMessage(whenMethodName))
    }
}

internal class CheckAccessUiVisitor(private val context: JavaContext) : AbstractUastVisitor() {
    var uiAccessNode: UCallExpression? = null
    private val recursiveHelper = RecursiveVisitHelper()

    override fun visitElement(node: UElement) = uiAccessNode != null

    override fun visitCallExpression(node: UCallExpression): Boolean {
        val receiverClass = PsiTypesUtil.getPsiClass(node.receiverType)
        if (context.evaluator.extendsClass(receiverClass, SdkConstants.CLASS_VIEW, false)) {
            uiAccessNode = node
            return true
        }
        recursiveHelper.visitIfNeeded(node.resolve(), this)
        return super.visitCallExpression(node)
    }

    // ignore classes defined inline
    override fun visitClass(node: UClass) = true

    // ignore fun defined inline
    override fun visitDeclaration(node: UDeclaration) = true

    // issue here, that we ignore calls like .let { } that calls lambda inplace
    override fun visitLambdaExpression(node: ULambdaExpression) = true
}

class RecursiveVisitHelper {
    private val maxInspectionDepth = 3
    private val visitedMethods = mutableSetOf<UMethod>()
    private var depth = 0

    fun visitIfNeeded(psiMethod: PsiMethod?, visitor: UastVisitor) {
        val method = psiMethod?.toUElement() as? UMethod
        if (method != null && method !in visitedMethods) {
            visitedMethods.add(method)
            if (depth < maxInspectionDepth) {
                depth++
                method.uastBody?.accept(visitor)
                depth--
            }
        }
    }
}