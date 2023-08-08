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

package androidx.window.core

import android.util.Log
import androidx.window.core.VerificationMode.LOG
import androidx.window.core.VerificationMode.QUIET
import androidx.window.core.VerificationMode.STRICT

/**
 * An [Exception] to signal that strict mode has been violated. This class should only be used
 * within the [SpecificationComputer]
 */
internal class WindowStrictModeException(message: String) : Exception(message)

/**
 * The root abstract class to represent a [SpecificationComputer]. A [SpecificationComputer] can be
 * used to validate assumptions about objects and interfaces when working with OEM provided
 * libraries. The [SpecificationComputer] has three verification modes, [STRICT], [LOG], and
 * [QUIET]. For [STRICT] mode, the [SpecificationComputer] will throw an exception when a required
 * condition is not met. For [LOG] mode, the [SpecificationComputer] will create a debug log when a
 * required condition is not met. For [QUIET] mode, value is simply converted to null if the
 * specification is not met.
 */
internal abstract class SpecificationComputer<T : Any> {

    /**
     * Checks if the required condition is met and returns a [SpecificationComputer] based on the
     * result. If the condition is met then the [SpecificationComputer] that is returned will
     * continue to process additional checks. If the condition fails then the
     * [SpecificationComputer] will have different behavior depending on the verification mode. If
     * the verification mode is [STRICT] an [Exception] will be thrown when [compute] is
     * called. If the verification mode is [LOG] a debug log will be written. If the verification
     * mode is [QUIET] then an empty [SpecificationComputer] is returned.
     *
     * Use [require] when an assumption must be true. One key example is when translating from an
     * OEM provided library into a local object you want to verify the attributes. If an attribute
     * does not match expectations then this can raise an exception at the source.
     *
     * One use case for this is our integration tests for devices. We can write good assumptions in
     * a test because we would need external knowledge of the device. One example is a device with a
     * fold. We expect the fold to propagate through the translation layer but we can only assert
     * the presence of a fold if we know the device has a fold. As a substitution we can turn on
     * strict mode and this will throw an exception when translating causing the test to fail.
     *
     * @param message a description of what the condition is testing. This should be user readable
     * @param condition a test that the current value is expected to pass.
     * @return A [SpecificationComputer] that will continue computing if the condition was met or
     * skip subsequent checks if the condition was not met.
     */
    abstract fun require(message: String, condition: T.() -> Boolean): SpecificationComputer<T>

    /**
     * Computes the result of the previous checks and the returned value depends on the
     * [VerificationMode]. For [STRICT] mode an [Exception] may be thrown if the condition was
     * required. For [LOG] mode a debug log is written to the [Logger]. For [QUIET] mode there are
     * no side effects.
     * @return The value if all the checks passed or none of the checks were required, null
     * otherwise.
     * @throws WindowStrictModeException if [STRICT] mode is set and a required check fails.
     */
    abstract fun compute(): T?

    protected fun createMessage(value: Any, message: String): String {
        return "$message value: $value"
    }

    companion object {
        /**
         * Start a specification for the receiver.
         *
         * @param T a non-null value that the specification will run against.
         * @param tag an identifier that will be used when writing debug logs.
         * @param verificationMode determines if the checks should throw, log, or fail silently.
         * @param logger a [Logger] that can be substituted for testing purposes. The default
         * value writes a log to LogCat
         * @return A [SpecificationComputer] that is initially valid and can check other
         * conditions.
         */
        fun <T : Any> T.startSpecification(
            tag: String,
            verificationMode: VerificationMode = BuildConfig.verificationMode,
            logger: Logger = AndroidLogger
        ): SpecificationComputer<T> {
            return ValidSpecification(this, tag, verificationMode, logger)
        }
    }
}

/**
 * A [SpecificationComputer] where the value has passed all previous checks.
 */
private class ValidSpecification<T : Any>(
    val value: T,
    val tag: String,
    val verificationMode: VerificationMode,
    val logger: Logger
) : SpecificationComputer<T>() {

    override fun require(message: String, condition: T.() -> Boolean): SpecificationComputer<T> {
        return if (condition(value)) {
            this
        } else {
            FailedSpecification(
                value = value,
                tag = tag,
                message = message,
                logger = logger,
                verificationMode = verificationMode
            )
        }
    }

    override fun compute(): T {
        return value
    }
}

/**
 * A [SpecificationComputer] that has failed a required check
 */
private class FailedSpecification<T : Any>(
    val value: T,
    val tag: String,
    val message: String,
    val logger: Logger,
    val verificationMode: VerificationMode
) : SpecificationComputer<T>() {

    val exception: WindowStrictModeException =
        WindowStrictModeException(createMessage(value, message)).apply {
            stackTrace = stackTrace.drop(2).toTypedArray()
        }

    override fun require(message: String, condition: T.() -> Boolean): SpecificationComputer<T> {
        return this
    }

    override fun compute(): T? {
        return when (verificationMode) {
            STRICT -> throw exception
            LOG -> {
                logger.debug(tag, createMessage(value, message))
                null
            }
            QUIET -> null
        }
    }
}

internal interface Logger {
    fun debug(tag: String, message: String)
}

internal object AndroidLogger : Logger {
    override fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }
}
