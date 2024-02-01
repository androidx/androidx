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

package androidx.lifecycle.testing.lint

import androidx.compose.lint.Name
import androidx.compose.lint.Package
import androidx.compose.lint.isInPackageName
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.getParameterForArgument
import org.jetbrains.uast.tryResolve
import org.jetbrains.uast.visitor.AbstractUastVisitor

/**
 * [Detector] that checks TestLifecycleOwner usage when run from within a coroutine
 */
class TestLifecycleOwnerInCoroutineDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames() = listOf(RunTest.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!(method.isInPackageName(RunTestPackageName)))
            return

        // The RunTest lambda
        val testBody = node.valueArguments.find {
            node.getParameterForArgument(it)?.name == "testBody"
        } ?: return

        var setsCurrentState = false

        testBody.accept(object : AbstractUastVisitor() {
            override fun visitBinaryExpression(node: UBinaryExpression): Boolean {
                val resolvedLeft = node.leftOperand.tryResolve() as? PsiField ?: return false
                val resolvedRight = node.rightOperand.tryResolve() as? PsiField ?: return false
                setsCurrentState = resolvedLeft.name == CurrentState.shortName &&
                    isLifecycleState(resolvedRight.name)
                return setsCurrentState
            }
        })

        if (setsCurrentState) {
            context.report(
                ISSUE,
                node,
                context.getNameLocation(node),
                "Incorrect use of currentState property inside of Coroutine, please use " +
                    "the suspending setCurrentState() function."
            )
        }
    }

    internal fun isLifecycleState(state: String): Boolean {
        return listOf("INITIALIZED", "DESTROYED", "CREATED", "STARTED", "RESUMED").contains(state)
    }

    companion object {
        val ISSUE = Issue.create(
            id = "TestLifecycleOwnerInCoroutine",
            briefDescription = "Use the suspending function setCurrentState(), rather than " +
                "directly accessing the currentState property.",
            explanation = """When using TestLifecycleOwner, one of the main use cases is to change \
                the currentState property. Under the hood, we do this using runBlocking to keep \
                it thread-safe. However, when using TestLifecycleOwner from the context of a \
                coroutine (like runTest), this will cause the setter to hang, since coroutines \
                should remain asynchronous.""",
            category = Category.CORRECTNESS,
            severity = Severity.ERROR,
            implementation = Implementation(
                TestLifecycleOwnerInCoroutineDetector::class.java,
                EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
            ),
            androidSpecific = true
        )
    }
}

private val RunTestPackageName = Package("kotlinx.coroutines.test")
private val RunTest = Name(RunTestPackageName, "runTest")
private val LifecyclePackageName = Package("androidx.lifecycle")
private val CurrentState = Name(LifecyclePackageName, "currentState")
