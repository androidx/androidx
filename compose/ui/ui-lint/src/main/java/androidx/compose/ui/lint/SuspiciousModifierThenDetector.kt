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

@file:Suppress("UnstableApiUsage")

package androidx.compose.ui.lint

import androidx.compose.lint.Names
import androidx.compose.lint.inheritsFrom
import androidx.compose.lint.isInPackageName
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isBelow
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.calls.KtCall
import org.jetbrains.kotlin.analysis.api.calls.KtCallableMemberCall
import org.jetbrains.kotlin.analysis.api.calls.KtCompoundAccessCall
import org.jetbrains.kotlin.analysis.api.calls.KtImplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.calls.singleCallOrNull
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtReceiverParameterSymbol
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * [Detector] that checks calls to Modifier.then to make sure the parameter does not contain a
 * Modifier factory function called with an receiver, as this will cause duplicate modifiers in the
 * chain. E.g. this.then(foo()), will result in this.then(this.then(foo)), as foo() internally will
 * call this.then(FooModifier).
 */
class SuspiciousModifierThenDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf(ThenName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(Names.Ui.PackageName)) return

        val otherModifierArgument = node.valueArguments.firstOrNull() ?: return
        val otherModifierArgumentSource = otherModifierArgument.sourcePsi ?: return

        otherModifierArgument.accept(
            object : AbstractUastVisitor() {
                /**
                 * Visit all calls to look for calls to a Modifier factory with implicit receiver
                 */
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    val hasModifierReceiverType =
                        node.receiverType?.inheritsFrom(Names.Ui.Modifier) == true
                    val usesImplicitThis = node.receiver == null

                    if (!hasModifierReceiverType || !usesImplicitThis) {
                        return false
                    }

                    val ktCallExpression = node.sourcePsi as? KtCallExpression ?: return false
                    // Resolve the implicit `this` to its source, if possible.
                    val implicitReceiver =
                        analyze(ktCallExpression) {
                            getImplicitReceiverValue(ktCallExpression)?.getImplicitReceiverPsi()
                        }

                    // The receiver used by the modifier function is defined within the then() call,
                    // such as then(Modifier.composed { otherModifierFactory() }). We don't know
                    // what
                    // the value of this receiver will be, so we ignore this case.
                    if (implicitReceiver.isBelow(otherModifierArgumentSource)) {
                        return false
                    }

                    context.report(
                        SuspiciousModifierThen,
                        node,
                        context.getNameLocation(node),
                        "Using Modifier.then with a Modifier factory function with an implicit receiver"
                    )

                    // Keep on searching for more errors
                    return false
                }
            }
        )
    }

    companion object {
        val SuspiciousModifierThen =
            Issue.create(
                "SuspiciousModifierThen",
                "Using Modifier.then with a Modifier factory function with an implicit receiver",
                "Calling a Modifier factory function with an implicit receiver inside " +
                    "Modifier.then will result in the receiver (`this`) being added twice to the " +
                    "chain. For example, fun Modifier.myModifier() = this.then(otherModifier()) - " +
                    "the implementation of factory functions such as Modifier.otherModifier() will " +
                    "internally call this.then(...) to chain the provided modifier with their " +
                    "implementation. When you expand this.then(otherModifier()), it becomes: " +
                    "this.then(this.then(OtherModifierImplementation)) - so you can see that `this` " +
                    "is included twice in the chain, which results in modifiers such as padding " +
                    "being applied twice, for example. Instead, you should either remove the then() " +
                    "and directly chain the factory function on the receiver, this.otherModifier(), " +
                    "or add the empty Modifier as the receiver for the factory, such as " +
                    "this.then(Modifier.otherModifier())",
                Category.CORRECTNESS,
                3,
                Severity.ERROR,
                Implementation(
                    SuspiciousModifierThenDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                )
            )
    }
}

private const val ThenName = "then"

// Below functions taken from AnalysisApiLintUtils.kt

/**
 * Returns the PSI for [this], which will be the owning lambda expression or the surrounding class.
 */
private fun KtImplicitReceiverValue.getImplicitReceiverPsi(): PsiElement? {
    return when (val receiverParameterSymbol = this.symbol) {
        // the owning lambda expression
        is KtReceiverParameterSymbol -> receiverParameterSymbol.owningCallableSymbol.psi
        // the class that we are in, calling a method
        is KtClassOrObjectSymbol -> receiverParameterSymbol.psi
        else -> null
    }
}

/**
 * Returns the implicit receiver value of the call-like expression [ktExpression] (can include
 * property accesses, for example).
 */
private fun KtAnalysisSession.getImplicitReceiverValue(
    ktExpression: KtExpression
): KtImplicitReceiverValue? {
    val partiallyAppliedSymbol =
        when (val call = ktExpression.resolveCall()?.singleCallOrNull<KtCall>()) {
            is KtCompoundAccessCall -> call.compoundAccess.operationPartiallyAppliedSymbol
            is KtCallableMemberCall<*, *> -> call.partiallyAppliedSymbol
            else -> null
        } ?: return null

    return partiallyAppliedSymbol.extensionReceiver as? KtImplicitReceiverValue
        ?: partiallyAppliedSymbol.dispatchReceiver as? KtImplicitReceiverValue
}
