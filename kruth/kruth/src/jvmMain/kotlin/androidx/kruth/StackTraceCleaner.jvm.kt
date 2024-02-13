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

package androidx.kruth

import androidx.kruth.StackFrameType.Companion.createStreakReplacementFrame
import com.google.common.base.MoreObjects.firstNonNull
import com.google.common.collect.ImmutableList
import java.lang.Thread.currentThread

private const val CLEANER_LINK: String = "https://goo.gl/aH3UyP"

/**
 * Returns true if stack trace cleaning is explicitly disabled in a system property. This switch
 * is intended to be used when attempting to debug the frameworks which are collapsed or filtered
 * out of stack traces by the cleaner.
 */
private fun isStackTraceCleaningDisabled(): Boolean {
    // Reading system properties might be forbidden.
    return try {
        System.getProperty("com.google.common.truth.disable_stack_trace_cleaning").toBoolean()
    } catch (e: SecurityException) {
        // Hope for the best.
        false
        // TODO(cpovirk): Log a warning? Or is that likely to trigger other violations?
    }
}

private val SUBJECT_CLASS: Set<String> = setOf(Subject::class.java.canonicalName!!)
private val STANDARD_SUBJECT_BUILDER_CLASS: Set<String> = setOf(
    StandardSubjectBuilder::class.java.canonicalName!!
)
private val JUNIT_INFRASTRUCTURE_CLASSES: Set<String> = setOf(
    "org.junit.runner.Runner",
    "org.junit.runners.model.Statement"
)

/**
 * Utility that cleans stack traces to remove noise from common frameworks.
 *
 * @constructor A new instance is instantiated for each throwable to be cleaned. This is so that
 * helper methods can make use of instance variables describing the state of the cleaning process.
 */
internal class StackTraceCleaner(private val throwable: Throwable) {
    private val cleanedStackTrace = mutableListOf<StackTraceElementWrapper>()
    private var lastStackFrameElementWrapper: StackTraceElementWrapper? = null
    private var currentStreakType: StackFrameType? = null
    private var currentStreakLength = 0

    // TODO(b/135924708): Add this to the test runners so that we clean all stack traces, not just
    //  those of exceptions originating in Truth.
    /** Cleans the stack trace on [throwable], replacing the trace that was originally on it. */
    fun clean(seenThrowables: MutableSet<Throwable>) {
        // Stack trace cleaning can be disabled using a system property.
        if (isStackTraceCleaningDisabled()) {
            return
        }

        // TODO(cpovirk): Consider wrapping this whole method in a try-catch in case there are any
        //  bugs. It would be a shame for us to fail to report the "real" assertion failure because
        //  we're instead reporting a bug in Truth's cosmetic stack cleaning.

        // Prevent infinite recursion if there is a reference cycle between Throwables.
        if (seenThrowables.contains(throwable)) {
            return
        }
        seenThrowables.add(throwable)

        val stackFrames: Array<StackTraceElement> = throwable.stackTrace

        var stackIndex: Int = stackFrames.lastIndex
        while (stackIndex >= 0 && !stackFrames[stackIndex].isTruthEntrance()) {
            // Find first frame that enters Truth's world, and remove all above.
            stackIndex--
        }
        stackIndex += 1

        var endIndex = 0
        while (endIndex <= stackFrames.lastIndex &&
            !stackFrames[endIndex].isJUnitInfrastructure()
        ) {
            // Find last frame of setup frames, and remove from there down.
            endIndex++
        }

        /*
         * If the stack trace would be empty, the error was probably thrown from "JUnit infrastructure"
         * frames. Keep those frames around (though much of JUnit itself and related startup frames will
         * still be removed by the remainder of this method) so that the user sees a useful stack.
         */
        if (stackIndex >= endIndex) {
            endIndex = stackFrames.size
        }

        while (stackIndex < endIndex) {
            val stackTraceElementWrapper = StackTraceElementWrapper(stackFrames[stackIndex])
            stackIndex++

            // Always keep frames that might be useful.
            if (stackTraceElementWrapper.stackFrameType == StackFrameType.NEVER_REMOVE) {
                endStreak()
                cleanedStackTrace.add(stackTraceElementWrapper)
                continue
            }

            // Otherwise, process the current frame for collapsing
            addToStreak(stackTraceElementWrapper)

            lastStackFrameElementWrapper = stackTraceElementWrapper
        }

        // Close out the streak on the bottom of the stack.
        endStreak()

        // Filter out testing framework and reflective calls from the bottom of the stack
        val iterator = cleanedStackTrace.listIterator(cleanedStackTrace.size)
        while (iterator.hasPrevious()) {
            val stackTraceElementWrapper = iterator.previous()
            if (stackTraceElementWrapper.stackFrameType == StackFrameType.TEST_FRAMEWORK ||
                stackTraceElementWrapper.stackFrameType == StackFrameType.REFLECTION
            ) {
                iterator.remove()
            } else {
                break
            }
        }

        // Replace the stack trace on the Throwable with the cleaned one.
        val result = Array<StackTraceElement>(cleanedStackTrace.size) { i ->
            cleanedStackTrace[i].stackTraceElement
        }
        throwable.setStackTrace(result)

        // Recurse on any related Throwables that are attached to this one
        throwable.cause?.also {
            StackTraceCleaner(it).clean(seenThrowables)
        }
        for (suppressed in getSuppressed(throwable)) {
            StackTraceCleaner(suppressed).clean(seenThrowables)
        }
    }

    /**
     * Either adds the given frame to the running streak or closes out the running streak and starts a
     * new one.
     */
    private fun addToStreak(stackTraceElementWrapper: StackTraceElementWrapper) {
        if (stackTraceElementWrapper.stackFrameType != currentStreakType) {
            endStreak()
            currentStreakType = stackTraceElementWrapper.stackFrameType
            currentStreakLength = 1
        } else {
            currentStreakLength++
        }
    }

    /** Ends the current streak, adding a summary frame to the result. Resets the streak counter. */
    private fun endStreak() {
        if (currentStreakLength == 0) {
            return
        }

        if (currentStreakLength == 1) {
            // A single frame isn't a streak. Just include the frame as-is in the result.
            cleanedStackTrace.add(checkNotNull(lastStackFrameElementWrapper))
        } else {
            // Add a single frame to the result summarizing the streak of framework frames
            cleanedStackTrace.add(
                createStreakReplacementFrame(checkNotNull(currentStreakType), currentStreakLength)
            )
        }

        clearStreak()
    }

    /** Resets the streak counter. */
    private fun clearStreak() {
        currentStreakType = null
        currentStreakLength = 0
    }

    private fun StackTraceElement.isTruthEntrance(): Boolean {
        return isFromClassOrClassNestedInside(SUBJECT_CLASS) ||
            /*
             * Don't match classes _nested inside_ StandardSubjectBuilder because that would match
             * Expect's Statement implementation. While we want to strip everything from there
             * _down_, we don't want to strip everything from there _up_ (which would strip the test
             * class itself!).
             *
             * (StandardSubjectBuilder is listed here only for its fail() methods, anyway, so we
             * don't have to worry about nested classes like we do with Subject.)
             */
            isFromClassDirectly(STANDARD_SUBJECT_BUILDER_CLASS)
    }

    private fun StackTraceElement.isJUnitInfrastructure(): Boolean {
        // It's not clear whether looking at nested classes here is useful, harmful, or neutral.
        return isFromClassOrClassNestedInside(JUNIT_INFRASTRUCTURE_CLASSES)
    }

    private fun StackTraceElement.isFromClassOrClassNestedInside(
        recognizedClasses: Set<String>
    ): Boolean {
        var stackClass: Class<*>? = try {
            loadClass(className)
        } catch (e: ClassNotFoundException) {
            return false
        }

        try {
            while (stackClass != null) {
                for (recognizedClass in recognizedClasses) {
                    if (isSubtypeOf(stackClass, recognizedClass)) {
                        return true
                    }
                }
                stackClass = stackClass.enclosingClass
            }
        } catch (e: Error) {
            if (e::class.java.name.equals("com.google.j2objc.ReflectionStrippedError")) {
                /*
                 * We're running under j2objc without reflection. Skip testing the enclosing classes. At
                 * least we tested the class itself against all the recognized classes.
                 *
                 * TODO(cpovirk): The smarter thing might be to guess the name of the enclosing classes by
                 * removing "$Foo" from the end of the name. But this should be good enough for now.
                 */
                return false
            }
            if (e is IncompatibleClassChangeError) {
                // OEM class-loading bug? https://issuetracker.google.com/issues/37045084
                return false
            }
            throw e
        }
        return false
    }

    private fun isSubtypeOf(subclass: Class<*>?, superclass: String): Boolean {
        var clazz = subclass
        while (clazz != null) {
            if (clazz.canonicalName != null && clazz.canonicalName.equals(superclass)) {
                return true
            }

            clazz = clazz.superclass
        }
        return false
    }

    private fun StackTraceElement.isFromClassDirectly(
        recognizedClasses: Set<String>
    ): Boolean {
        val stackClass = try {
            loadClass(className)
        } catch (e: ClassNotFoundException) {
            return false
        }
        for (recognizedClass in recognizedClasses) {
            if (isSubtypeOf(stackClass, recognizedClass)) {
                return true
            }
        }
        return false
    }

    /**
     * @throws ClassNotFoundException
     */
    // Using plain Class.forName can cause problems.
    /*
     * TODO(cpovirk): Consider avoiding classloading entirely by reading classes with ASM. But that
     * won't work on Android, so we might ultimately need classloading as a fallback. Another
     * possibility is to load classes in a fresh, isolated classloader. However, that requires
     * creating a list of jars to load from, which is fragile and would also require special handling
     * under Android. If we're lucky, this new solution will just work: The classes should already be
     * loaded, anyway, since they appear on the stack, so we just have to hope that we have the right
     * classloader.
     */
    private fun loadClass(name: String): Class<*> {
        val loader: ClassLoader = firstNonNull(
            currentThread().getContextClassLoader(), StackTraceCleaner::class.java.classLoader!!
        )
        return loader.loadClass(name)
    }
}

/**
 * Wrapper around a [StackTraceElement] for calculating and holding the metadata used to clean
 * the stack trace.
 *
 * @constructor Creates a wrapper with the given frame and the given frame type.
 */
internal class StackTraceElementWrapper(
    /** The wrapped [StackTraceElement]. */
    internal val stackTraceElement: StackTraceElement,
    /** The type of this frame. */
    internal val stackFrameType: StackFrameType
) {

    /**
     *  Creates a wrapper with the given frame with frame type inferred from frame's class name.
     */
    constructor(stackTraceElement: StackTraceElement) : this(
        stackTraceElement,
        StackFrameType.forClassName(stackTraceElement.className)
    )
}

/**
 * Enum of the package or class-name based categories of stack frames that might be removed or
 * collapsed by the cleaner.
 *
 * @constructor Each type of stack frame has a name of the summary displayed in the cleaned
 *  trace.
 *
 *  Most also have a set of fully qualified class name prefixes that identify when a
 *  frame belongs to this type.
 */
internal enum class StackFrameType(
    /** Returns the name of this frame type to display in the cleaned trace */
    private val stackFrameName: String,
    private val prefixes: ImmutableList<String> = ImmutableList.of()
) {
    NEVER_REMOVE("N/A"),
    TEST_FRAMEWORK(
        "Testing framework",
        ImmutableList.of(
            "junit",
            "org.junit",
            "androidx.test.internal.runner",
            "com.github.bazel_contrib.contrib_rules_jvm.junit5",
            "com.google.testing.junit",
            "com.google.testing.testsize",
            "com.google.testing.util"
        )
    ),
    REFLECTION(
        "Reflective call",
        ImmutableList.of("java.lang.reflect", "jdk.internal.reflect", "sun.reflect")
    ),
    CONCURRENT_FRAMEWORK(
        "Concurrent framework",
        ImmutableList.of(
            "com.google.tracing.CurrentContext",
            "com.google.common.util.concurrent",
            "java.util.concurrent.ForkJoin"
        )
    );

    /**
     * Returns true if the given frame belongs to this frame type based on the package and/or class
     * name of the frame.
     */
    fun belongsToType(fullyQualifiedClassName: String): Boolean {
        for (prefix in prefixes) {
            // TODO(cpovirk): Should we also check prefix + "$"?
            if (fullyQualifiedClassName == prefix ||
                fullyQualifiedClassName.startsWith("$prefix.")
            ) {
                return true
            }
        }
        return false
    }

    companion object {
        private const val NON_LEAKY_TEST_FQN =
            "androidx.test.internal.runner.junit3.NonLeakyTestSuite\$NonLeakyTest"

        /** Helper method to determine the frame type from the fully qualified class name. */
        internal fun forClassName(fullyQualifiedClassName: String): StackFrameType {
            // Never remove the frames from a test class. These will probably be the frame of a failing
            // assertion.
            // TODO(cpovirk): This is really only for tests in Truth itself, so this doesn't matter
            //  yet, but.... If the Truth tests someday start calling into nested classes, we may
            //  want to add:
            //  || fullyQualifiedClassName.contains("Test$")
            if (fullyQualifiedClassName.endsWith("Test") &&
                fullyQualifiedClassName != NON_LEAKY_TEST_FQN
            ) {
                return NEVER_REMOVE
            }

            for (stackFrameType in StackFrameType.values()) {
                if (stackFrameType.belongsToType(fullyQualifiedClassName)) {
                    return stackFrameType
                }
            }

            return NEVER_REMOVE
        }

        internal fun createStreakReplacementFrame(
            stackFrameType: StackFrameType,
            length: Int
        ) = StackTraceElementWrapper(
            StackTraceElement(
                "[[${stackFrameType.stackFrameName}: $length frames collapsed ($CLEANER_LINK)]]",
                "",
                "",
                0
            ),
            stackFrameType
        )
    }
}
