/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.health.platform.client.error

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.health.platform.client.impl.data.ProtoParcelable
import androidx.health.platform.client.proto.ErrorProto
import java.lang.reflect.Field

/** Data object holding error state for IPC method calls. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class ErrorStatus
constructor(
    @ErrorCode val errorCode: Int,
    internal val errorMessage: String? = null
) : ProtoParcelable<ErrorProto.ErrorStatus>() {

    override val proto: ErrorProto.ErrorStatus by lazy {
        val builder = ErrorProto.ErrorStatus.newBuilder().setCode(errorCode)
        errorMessage?.let(builder::setMessage)
        builder.build()
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun create(
            errorCode: Int,
            errorMessage: String? = null
        ): ErrorStatus {
            return ErrorStatus(safeErrorCode(errorCode), errorMessage)
        }

        @ErrorCode
        fun safeErrorCode(errorCode: Int): Int {
            return ErrorCode::class
                .java
                .declaredFields
                .filter { it.type.isAssignableFrom(Int::class.java) }
                .map { field: Field ->
                    try {
                        return@map field[null] as Int
                    } catch (e: IllegalAccessException) {
                        return@map ErrorCode.INTERNAL_ERROR
                    }
                }
                .firstOrNull { value: Int -> value == errorCode }
                ?: ErrorCode.INTERNAL_ERROR
        }

        @JvmField
        val CREATOR: Parcelable.Creator<ErrorStatus> = newCreator {
            val proto = ErrorProto.ErrorStatus.parseFrom(it)
            create(
                proto.code,
                if (proto.hasMessage()) proto.message else null,
            )
        }
    }
}