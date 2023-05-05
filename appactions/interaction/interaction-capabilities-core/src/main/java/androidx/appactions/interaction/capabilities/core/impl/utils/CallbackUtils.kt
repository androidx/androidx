/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.impl.utils

import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal
import androidx.appactions.interaction.capabilities.core.impl.exceptions.ExternalException
import androidx.appactions.interaction.capabilities.core.impl.exceptions.InvalidRequestException
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException
import androidx.appactions.interaction.capabilities.core.impl.task.exceptions.DisambigStateException
import androidx.appactions.interaction.capabilities.core.impl.task.exceptions.InvalidResolverException
import kotlin.reflect.KClass

private const val LOG_TAG = "CallbackUtils"

/** invoke an externally implemented method, wrapping any exceptions with ExternalException.
 */
internal fun <T> invokeExternalBlock(description: String, block: () -> T): T {
    try {
        return block()
    } catch (t: Throwable) {
        throw ExternalException("exception occurred during '$description'", t)
    }
}

/** invoke an externally implemented suspend method, wrapping any exceptions with
 * ExternalException.
 */
internal suspend fun <T> invokeExternalSuspendBlock(
    description: String,
    block: suspend () -> T
): T {
    try {
        return block()
    } catch (t: Throwable) {
        throw ExternalException("exception occurred during '$description'", t)
    }
}

/** Determines whether or not this exception is caused by some type, directly or indirectly. */
private fun <T : Throwable> Throwable.isCausedBy(clazz: KClass<T>): Boolean {
    if (clazz.isInstance(this)) {
        return true
    }
    return this.cause?.isCausedBy(clazz) == true
}

/**
 * Returns an ErrorStatusInternal corresponding to this Throwable.
 */
private fun Throwable.toErrorStatusInternal(): ErrorStatusInternal {
    return when {
        this.isCausedBy(
            ExternalException::class
        ) -> ErrorStatusInternal.EXTERNAL_EXCEPTION
        this.isCausedBy(
            StructConversionException::class
        ) -> ErrorStatusInternal.STRUCT_CONVERSION_FAILURE
        this.isCausedBy(
            InvalidResolverException::class
        ) -> ErrorStatusInternal.INVALID_RESOLVER
        this.isCausedBy(
            DisambigStateException::class
        ) -> ErrorStatusInternal.UNCHANGED_DISAMBIG_STATE
        this.isCausedBy(
            InvalidRequestException::class
        ) -> ErrorStatusInternal.INVALID_REQUEST
        else -> ErrorStatusInternal.CANCELLED
    }
}

/**
 * Handles an exception encountered during request proessing (one-shot or multi-turn).
 * Includes reporting an ErrorStatusInternal to some callback based on the exception.
 */
internal fun handleExceptionFromRequestProcessing(
    t: Throwable,
    errorCallback: (ErrorStatusInternal) -> Unit
) {
    LoggerInternal.log(
        CapabilityLogger.LogLevel.ERROR,
        LOG_TAG,
        "exception encountered during request processing, this exception.",
        t
    )
    errorCallback.invoke(t.toErrorStatusInternal())
    if (!t.isCausedBy(InvalidRequestException::class)) {
        LoggerInternal.log(
            CapabilityLogger.LogLevel.ERROR,
            LOG_TAG,
            "Rethrowing exception because it is not caused by InvalidRequestException.",
            t
        )
        throw t
    }
}