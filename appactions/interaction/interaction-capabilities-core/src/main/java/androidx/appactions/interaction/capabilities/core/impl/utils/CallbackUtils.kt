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
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException
import androidx.appactions.interaction.capabilities.core.impl.exceptions.InvalidRequestException
import kotlin.reflect.KClass

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
internal fun <T : Throwable> Throwable.isCausedBy(clazz: KClass<T>): Boolean {
    if (clazz.isInstance(this)) {
        return true
    }
    return this.cause?.isCausedBy(clazz) == true
}

internal fun Throwable.toErrorStatusInternal(): ErrorStatusInternal {
    return when {
        this.isCausedBy(
            ExternalException::class
        ) -> ErrorStatusInternal.EXTERNAL_EXCEPTION
        this.isCausedBy(
            StructConversionException::class
        ) -> ErrorStatusInternal.STRUCT_CONVERSION_FAILURE
        this.isCausedBy(
            InvalidRequestException::class
        ) -> ErrorStatusInternal.INVALID_REQUEST
        else -> ErrorStatusInternal.CANCELLED
    }
}