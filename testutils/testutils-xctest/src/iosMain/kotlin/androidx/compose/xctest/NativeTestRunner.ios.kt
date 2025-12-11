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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package androidx.compose.xctest

import kotlin.native.internal.test.TestCase
import kotlin.native.internal.test.TestSuite
import kotlinx.cinterop.Arena
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.cstr
import platform.Foundation.NSClassFromString
import platform.Foundation.NSCocoaErrorDomain
import platform.Foundation.NSError
import platform.Foundation.NSInvocation
import platform.Foundation.NSMethodSignature
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSString
import platform.Foundation.NSStringFromSelector
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSValidationErrorMinimum
import platform.Foundation.dataUsingEncoding
import platform.UniformTypeIdentifiers.UTTypeSourceCode
import platform.XCTest.XCTAttachment
import platform.XCTest.XCTIssue
import platform.XCTest.XCTIssueTypeAssertionFailure
import platform.XCTest.XCTIssueTypeUncaughtException
import platform.XCTest.XCTSourceCodeContext
import platform.XCTest.XCTSourceCodeLocation
import platform.XCTest.XCTestCase
import platform.XCTest.XCTestCaseMeta
import platform.XCTest.XCTestSuite
import platform.XCTest.skipImplementation
import platform.objc.class_addMethod
import platform.objc.imp_implementationWithBlock
import platform.objc.objc_allocateClassPair
import platform.objc.objc_registerClassPair
import platform.objc.object_setClass

/**
 * An XCTest equivalent of the K/N TestCase.
 *
 * Wraps the [TestCase] that runs it with a special bridge method created by adding it to a class.
 * The idea is to make XCTest invoke them by the created invocation and show the selector as a test name.
 * This selector is created as `class.method` that is than naturally represented in XCTest reports including XCode.
 */
internal class XCTestCaseWrapper(val testCase: TestCase) : XCTestCase(dummyInvocation()) {
    // Sets XCTest to continue running after failure to match Kotlin Test
    override fun continueAfterFailure(): Boolean = true

    val ignored = testCase.ignored || testCase.suite.ignored

    private val fullTestName = testCase.fullName

    init {
        // Set custom test name
        val newClass = NSClassFromString(testCase.suite.name)
            ?: objc_allocateClassPair(XCTestCaseWrapper.`class`(), testCase.suite.name, 0UL)!!.also {
                objc_registerClassPair(it)
            }

        object_setClass(this, newClass)

        val selector = NSSelectorFromString(testCase.name)
        createRunMethod(selector, ignored)
        setInvocation(methodSignatureForSelector(selector)?.let { signature ->
            @Suppress("CAST_NEVER_SUCCEEDS")
            val invocation = NSInvocation.invocationWithMethodSignature(signature as NSMethodSignature)
            invocation.setSelector(selector)
            invocation.setTarget(this)
            invocation
        })
    }

    /**
     * Creates and adds method to the metaclass with implementation block
     * that gets an XCTestCase instance as self to be run.
     */
    private fun createRunMethod(selector: COpaquePointer?, isIgnored: Boolean) {
        val imp = if (isIgnored) {
            // A special implementation that makes current test execution to skip a test case
            skipImplementation()
        } else {
            imp_implementationWithBlock(::run)
        }
        val result = class_addMethod(
            cls = this.`class`(),
            name = selector,
            imp = imp,
            types = "v@:" // Obj-C type encodings: v (returns void), @ (id self), : (SEL sel)
        )
        check(result) {
            "Internal error: was unable to add method with selector $selector"
        }
    }

    @ObjCAction
    private fun run() {
        try {
            testCase.doRun()
        } catch (throwable: Throwable) {
            val stackTrace = throwable.getStackTrace()
            val failedStackLine = stackTrace.first {
                // try to filter out kotlin.Exceptions and kotlin.test.Assertion inits to poin to the failed stack and line
                !it.contains("kfun:kotlin.")
            }
            // Find path and line number to create source location
            val matchResult = Regex("^\\d+ +.* \\((.*):(\\d+):.*\\)$").find(failedStackLine)
            val sourceLocation = if (matchResult != null) {
                val (file, line) = matchResult.destructured
                XCTSourceCodeLocation(file, line.toLong())
            } else {
                // No debug info to get the path. Still have to record location
                XCTSourceCodeLocation(testCase.suite.name, 0L)
            }

            // Make a stacktrace attachment, encoding it as source code.
            // This makes it appear as an attachment in the XCode test results for the failed test.
            @Suppress("CAST_NEVER_SUCCEEDS")
            val stackAsPayload = (stackTrace.joinToString("\n") as? NSString)?.dataUsingEncoding(NSUTF8StringEncoding)
            val stackTraceAttachment = XCTAttachment.attachmentWithUniformTypeIdentifier(
                identifier = UTTypeSourceCode.identifier,
                name = "Kotlin stacktrace (full)",
                payload = stackAsPayload,
                userInfo = null
            )

            val type = when (throwable) {
                is AssertionError -> XCTIssueTypeAssertionFailure
                else -> XCTIssueTypeUncaughtException
            }

            // Finally, create and record an issue with all gathered data
            val issue = XCTIssue(
                type = type,
                compactDescription = "$throwable in $fullTestName",
                detailedDescription = buildString {
                    appendLine("Test '$fullTestName' from '${testCase.suite.name}' failed with $throwable")
                    throwable.cause?.let { appendLine("(caused by ${throwable.cause})") }
                },
                sourceCodeContext = XCTSourceCodeContext(
                    callStackAddresses = throwable.getStackTraceAddresses(),
                    location = sourceLocation
                ),
                // pass the error through the XCTest to the NativeTestObserver
                associatedError = NSErrorWithKotlinException(throwable),
                attachments = listOf(stackTraceAttachment)
            )
            testRun?.recordIssue(issue) ?: error("TestRun for the test $fullTestName not found")
        }
    }

    override fun setUp() {
        if (!ignored) testCase.doBefore()
    }

    override fun tearDown() {
        if (!ignored) testCase.doAfter()
    }

    override fun description(): String = buildString {
        append(fullTestName)
        if (ignored) append("(ignored)")
    }

    override fun name(): String {
        return testCase.name
    }

    companion object : XCTestCaseMeta() {
        /**
         * This method is invoked by the XCTest when it discovered XCTestCase instance
         * that contains test method.
         *
         * This method should not be called with the current idea and assumptions.
         */
        override fun testCaseWithInvocation(invocation: NSInvocation?): XCTestCase {
            error(
                """
                This should not happen by default.
                Got invocation: ${invocation?.description}
                with selector @sel(${NSStringFromSelector(invocation?.selector)})
                """.trimIndent()
            )
        }

        private fun dummyInvocation(): NSInvocation {
            return NSInvocation.invocationWithMethodSignature(
                NSMethodSignature.signatureWithObjCTypes("v@:".cstr.getPointer(Arena())) as NSMethodSignature
            )
        }
    }
}

/**
 * This is a NSError-wrapper of Kotlin exception used to pass it through the XCTIssue
 * to the XCTestObservation protocol implementation [NativeTestObserver].
 * See [NativeTestObserver.testCase] for the usage.
 */
internal class NSErrorWithKotlinException(val kotlinException: Throwable) : NSError(NSCocoaErrorDomain, NSValidationErrorMinimum, null)

/**
 * XCTest equivalent of K/N TestSuite.
 */
internal class XCTestSuiteWrapper(val testSuite: TestSuite) : XCTestSuite(testSuite.name) {
    private val ignoredSuite: Boolean
        get() = testSuite.ignored || testSuite.testCases.all { it.value.ignored }

    override fun setUp() {
        if (!ignoredSuite) testSuite.doBeforeClass()
    }

    override fun tearDown() {
        if (!ignoredSuite) testSuite.doAfterClass()
    }
}
