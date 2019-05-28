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
import androidx.lifecycle.lint.stubs.COROUTINES_STUB
import androidx.lifecycle.lint.stubs.LIFECYCLE_STUB
import androidx.lifecycle.lint.stubs.VIEW_STUB
import com.android.tools.lint.checks.infrastructure.TestFiles.kt
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.util.Properties

@RunWith(JUnit4::class)
class LifecycleWhenChecksTest {

    private var sdkDir: File? = null

    @Before
    fun setup() {
        val stream = LifecycleWhenChecksTest::class.java.classLoader.getResourceAsStream("sdk.prop")
        val properties = Properties()
        properties.load(stream)
        sdkDir = File(properties["sdk.dir"] as String)
    }

    private fun check(body: String): TestLintResult {
        return TestLintTask.lint()
            .files(VIEW_STUB, LIFECYCLE_STUB, COROUTINES_STUB, kt(template(body)))
            .sdkHome(sdkDir!!)
            .issues(ISSUE)
            .run()
    }

    private val TEMPLATE = """
        package foo

        import androidx.lifecycle.Lifecycle
        import androidx.lifecycle.whenStarted
        import android.view.FooView
        import kotlinx.coroutines.GlobalScope

        suspend fun foo(lifecycle: Lifecycle, view: FooView) {
            lifecycle.whenStarted {
                %s
            }
        }

        suspend fun suspendingFun() {
        }

        suspend fun suspendWithTryCatch() {
            try {
                suspendingFun()
            } finally {
                FooView().foo()
            }
        }

        fun accessView(view: FooView) {
            cycle()
            view.foo()
        }

        fun cycle() {
            accessView(FooView())
        }

        suspend fun suspendingWithCycle() {
            try {
                suspendingFun()
                suspendingWithCycle()
            } finally {
                FooView().foo()
            }
        }
    """.trimIndent()

    private val TEMPLATE_SIZE_BEFOFE_BODY = TEMPLATE.substringBefore("%s").lines().size

    private fun template(body: String) = TEMPLATE.format(body)

    fun error(
        lineNumber: Int,
        customExpression: String = "view.foo()",
        additionalMessage: String = ""
    ): String {
        val l = TEMPLATE_SIZE_BEFOFE_BODY - 1 + lineNumber

        val trimmed = customExpression.trimStart().length
        val indent = " ".repeat(customExpression.length - trimmed)
        val highlight = indent + "~".repeat(trimmed)

        fun multiLine(s: String) = s.lines().joinToString("\n|")

        val error = errorMessage("whenStarted").replace("`", "")
        val primary = """
            src/foo/test.kt:$l: Error: $error [${ISSUE.id}]
                $customExpression
                $highlight
        """.trimIndent()

        val message = if (additionalMessage.isEmpty()) {
            primary
        } else {
            """
                |${multiLine(primary)}
                |    $additionalMessage
            """.trimMargin()
        }

        return """
            |${multiLine(message)}
            |1 errors, 0 warnings
            """.trimMargin()
    }

    @Test
    fun accessViewInFinally() {
        val input = """
            try {
                suspendingFun()
            } finally {
                view.foo()
            }
        """.trimIndent()

        check(input.trimIndent()).expect(error(4))
    }

    @Test
    fun accessViewInFinallyInLifecycleCheck() {
        val input = """
            try {
                suspendingFun()
            } finally {
                if (lifecycle.isAtLeast(Lifecycle.State.STARTED)) {
                    view.foo()
                }
            }
        """.trimIndent()
        check(input.trimIndent()).expectClean()
    }

    @Test
    fun accessViewInFinallyAfterLifecycleCheck() {
        val input = """
            try {
                suspendingFun()
            } finally {
                if (lifecycle.isAtLeast(Lifecycle.State.STARTED)) {
                } else {
                    view.foo()
                }
            }
        """.trimIndent()
        check(input.trimIndent()).expect(error(6, "    view.foo()"))
    }

    @Test
    fun accessViewInFinallyWithLifecycleCheckInterrupted() {
        // it is ok, because suspendingFun in if - check will throw if scope was cancelled,
        // so view.foo() won't be executed
        val input = """
            try {
                suspendingFun()
            } finally {
                if (lifecycle.isAtLeast(Lifecycle.State.STARTED)) {
                    suspendingFun()
                    view.foo()
                }
            }
        """.trimIndent()
        check(input.trimIndent()).expectClean()
    }

    @Test
    fun tryInLifecycleCheck() {
        val input = """
            try {
                suspendingFun()
            } finally {
                if (lifecycle.isAtLeast(Lifecycle.State.STARTED)) {
                    try {
                        suspendingFun()
                    } finally {
                        view.foo()
                    }
                    view.foo()
                }
            }
        """.trimIndent()
        check(input.trimIndent()).expect(error(8, "        view.foo()"))
            .expectErrorCount(1)
    }

    @Test
    fun tryWithNonSuspendLambda() {
        val input = """
            try {
                "".apply {
                    suspendingFun()
                }
            } finally {
                view.foo()
            }
        """.trimIndent()

        check(input.trimIndent()).expect(error(6))
    }

    @Test
    fun tryWithSuspendLambda() {
        val input = """
            try {
                GlobalScope.launch {
                    suspendingFun()
                }
            } finally {
                view.foo()
            }
        """.trimIndent()
        check(input.trimIndent()).expectClean()
    }

    @Test
    fun suspendLambdaWithTry() {
        // some weird stuff is going, but it is not our business
        val input = """
            GlobalScope.launch {
                try {
                    suspendingFun()
                } finally {
                    view.foo()
                }
            }
        """.trimIndent()
        check(input.trimIndent()).expectClean()
    }

    @Test
    fun nonSuspendLambdaWithTry() {
        // some weird stuff is going, but it is not our business
        val input = """
            "".apply {
                try {
                    suspendingFun()
                } finally {
                    view.foo()
                }
            }
        """.trimIndent()
        check(input.trimIndent()).expect(error(5, "    view.foo()"))
    }

    @Test
    fun visitResolvedMethod() {
        val input = "suspendWithTryCatch()"
        check(input).expect(error(12, "    FooView().foo()"))
    }

    @Test
    fun visitResolvedMethodWithCycle() {
        val input = "suspendingWithCycle()"
        check(input).expect(error(30, "    FooView().foo()"))
    }

    @Test
    fun finallyWithWhenFinally() {
        val input = """
            try {
                suspendingFun()
            } finally {
                lifecycle.whenStarted {
                    view.foo()
                }
            }
        """.trimIndent()
        check(input).expectClean()
    }

    @Test
    fun tryInTrySuspendAfter() {
        val input = """
            try {
                try { } finally {}
                suspendingFun()
            } finally {
                view.foo()
            }
        """.trimIndent()
        check(input).expect(error(5))
    }

    @Test
    fun tryInTrySuspendBefore() {
        val input = """
            try {
                suspendingFun()
                try { } finally {}
            } finally {
                view.foo()
            }
        """.trimIndent()
        check(input).expect(error(5))
    }

    @Test
    fun tryInTrySuspendInInnerSuspend() {
        val input = """
            try {
                try {
                    suspendingFun()
                } finally {
                }
            } finally {
                view.foo()
            }
        """.trimIndent()
        check(input).expect(error(7))
    }

    @Test
    fun tryInTrySuspendInInnerFinally() {
        val input = """
            try {
                try {
                } finally {
                    suspendingFun()
                }
            } finally {
                view.foo()
            }
        """.trimIndent()
        check(input).expect(error(7))
    }

    @Test
    fun failingTryOkTry() {
        val input = """
            try {
                suspendingFun()
            } finally {
                view.foo()
            }
            try{
                view.foo()
            } finally {
                view.foo()
            }
        """.trimIndent()
        check(input).expect(error(4)).expectErrorCount(1)
    }

    @Test
    fun tryInFinallySuspendInOuterTry() {
        val input = """
            try {
                suspendingFun()
            } finally {
                try {
                } finally {
                    view.foo()
                }
            }
        """.trimIndent()
        check(input).expect(error(6, "    view.foo()")).expectErrorCount(1)
    }

    @Test
    fun accessViewInTryInFinallySuspendInOuter() {
        val input = """
            try {
                suspendingFun()
            } finally {
                try {
                    view.foo()
                } finally {
                }
            }
        """.trimIndent()
        check(input).expect(error(5, "    view.foo()")).expectErrorCount(1)
    }

    @Test
    fun failingTrySuspendFunOkTry() {
        val input = """
            try {
                suspendingFun()
            } finally {
                view.foo()
            }
            suspendingFun()
            try{
                view.foo()
            } finally {
                view.foo()
            }
        """.trimIndent()
        check(input).expect(error(4)).expectErrorCount(1)
    }

    @Test
    fun unrelatedClassDeclaration() {
        val input = """
            try {
                class Boom {
                    fun another() {
                        suspendingFun()
                    }
                }
            } finally {
                view.foo()
            }
        """.trimIndent()
        check(input).expectClean()
    }

    @Test
    fun unrelatedFunDeclaration() {
        val input = """
            try {
                suspend fun another() {
                    suspendingFun()
                }
            } finally {
                view.foo()
            }
        """.trimIndent()
        check(input).expectClean()
    }

    @Test
    fun viewAccessInFunction() {
        val input = """
            try {
                suspendingFun()
            } finally {
                accessView(view)
            }
        """.trimIndent()
        check(input).expect(
            error(
                4, "accessView(view)",
                "src/foo/test.kt:31: Internal View access"
            )
        )
    }
}