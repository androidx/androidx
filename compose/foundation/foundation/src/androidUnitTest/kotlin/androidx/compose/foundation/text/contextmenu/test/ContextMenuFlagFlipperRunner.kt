/*
 * Copyright 2025 The Android Open Source Project
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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.text.contextmenu.test

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.internal.checkPreconditionNotNull
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runner.Runner
import org.junit.runner.manipulation.Filter
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.Suite
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.InitializationError
import org.junit.runners.model.MultipleFailureException
import org.junit.runners.model.Statement

// KEEP THIS FILE UP TO DATE WITH
// compose/foundation/foundation/src/androidInstrumentedTest/kotlin/androidx/compose/foundation/text/contextmenu/test/ContextMenuFlagFlipperRunner.kt

/**
 * Annotation that tells junit to **NOT** run the test or test class with
 * [ComposeFoundationFlags.isNewContextMenuEnabled] set to [suppressedFlagValue].
 *
 * The test class must be [RunWith] [ContextMenuFlagFlipperRunner] for this to take effect.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class ContextMenuFlagSuppress(val suppressedFlagValue: Boolean)

/**
 * Runner that runs all tests in the test class twice, once with
 * [ComposeFoundationFlags.isNewContextMenuEnabled] set to `true` and once with it set to `false`.
 * Use the [ContextMenuFlagSuppress] annotation on a test class or test method to customize which
 * flag is used.
 *
 * Note: Android Studio tray icons to start specific methods will not match the methods when using
 * this runner due to the string appended to the test name.
 */
class ContextMenuFlagFlipperRunner(clazz: Class<*>) : Suite(clazz, buildRunners(clazz)) {
    companion object {
        private fun buildRunners(clazz: Class<*>): List<Runner> =
            try {
                val annotation = clazz.getAnnotation(ContextMenuFlagSuppress::class.java)
                listOf(true, false)
                    .filter { annotation?.suppressedFlagValue != it }
                    .map { TestClassRunnerForFlagValue(clazz, flagValue = it) }
            } catch (e: InitializationError) {
                val message = "Failed to initialize child runners for ContextMenuFlagFlipperRunner"
                throw RuntimeException(message, e)
            } catch (e: Exception) {
                val message =
                    "Unexpected error building child runners for ContextMenuFlagFlipperRunner"
                throw RuntimeException(message, e)
            }
    }
}

private class TestClassRunnerForFlagValue(clazz: Class<*>, private val flagValue: Boolean) :
    BlockJUnit4ClassRunner(clazz) {
    private var initialFlagValue: Boolean? = null

    init {
        filter(ContextMenuFlagSuppressFilter())
    }

    override fun getName(): String = "[isNewContextMenuEnabled=$flagValue]"

    override fun testName(method: FrameworkMethod): String = method.name + name

    override fun withBefores(
        method: FrameworkMethod,
        target: Any?,
        statement: Statement,
    ): Statement =
        RunBefore(statement = super.withBefores(method, target, statement)) {
            initialFlagValue = ComposeFoundationFlags.isNewContextMenuEnabled
            ComposeFoundationFlags.isNewContextMenuEnabled = flagValue
        }

    override fun withAfters(
        method: FrameworkMethod,
        target: Any?,
        statement: Statement,
    ): Statement =
        RunAfter(statement = super.withAfters(method, target, statement)) {
            ComposeFoundationFlags.isNewContextMenuEnabled =
                checkPreconditionNotNull(initialFlagValue)
        }

    private inner class ContextMenuFlagSuppressFilter() : Filter() {
        override fun shouldRun(description: Description): Boolean =
            description.testFunctionPasses() && description.testClassPasses()

        private fun Description.testFunctionPasses(): Boolean =
            getAnnotation(ContextMenuFlagSuppress::class.java).annotationPasses()

        private fun Description.testClassPasses(): Boolean =
            this.testClass.getAnnotation(ContextMenuFlagSuppress::class.java).annotationPasses()

        private fun ContextMenuFlagSuppress?.annotationPasses(): Boolean =
            this == null || suppressedFlagValue != flagValue

        override fun describe(): String {
            return "skip tests annotated with ContextMenuFlagSuppress if necessary"
        }
    }
}

/**
 * Akin to [RunBefores][org.junit.internal.runners.statements.RunBefores] but runs a block directly
 * instead of annotated methods on a class.
 */
private class RunBefore(private val statement: Statement, private val beforeBlock: () -> Unit) :
    Statement() {
    override fun evaluate() {
        beforeBlock()
        statement.evaluate()
    }
}

/**
 * Akin to [RunAfters][org.junit.internal.runners.statements.RunAfters] but runs a block directly
 * instead of annotated methods on a class.
 */
private class RunAfter(private val statement: Statement, private val afterBlock: () -> Unit) :
    Statement() {
    override fun evaluate() {
        val errors = mutableListOf<Throwable>()
        try {
            statement.evaluate()
        } catch (e: Throwable) {
            errors.add(e)
        } finally {
            try {
                afterBlock()
            } catch (e: Throwable) {
                errors.add(e)
            }
        }
        MultipleFailureException.assertEmpty(errors)
    }
}
