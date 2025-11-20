/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room3.solver

import androidx.room3.compiler.codegen.XClassName
import androidx.room3.compiler.codegen.XMemberName
import androidx.room3.ext.RoomRxJava3MemberNames
import androidx.room3.ext.RoomRxJava3TypeNames
import androidx.room3.ext.RxJava3TypeNames
import androidx.room3.processor.ProcessorErrors

internal enum class RxType(
    val version: RxVersion,
    val className: XClassName,
    val factoryMethodName: XMemberName,
    val canBeNull: Boolean = false,
) {
    RX3_FLOWABLE(
        version = RxVersion.THREE,
        className = RxJava3TypeNames.FLOWABLE,
        factoryMethodName = RoomRxJava3MemberNames.RX_ROOM_CREATE_FLOWABLE,
    ),
    RX3_OBSERVABLE(
        version = RxVersion.THREE,
        className = RxJava3TypeNames.OBSERVABLE,
        factoryMethodName = RoomRxJava3MemberNames.RX_ROOM_CREATE_OBSERVABLE,
    ),
    RX3_SINGLE(
        version = RxVersion.THREE,
        className = RxJava3TypeNames.SINGLE,
        factoryMethodName = RoomRxJava3MemberNames.RX_ROOM_CREATE_SINGLE,
    ),
    RX3_MAYBE(
        version = RxVersion.THREE,
        className = RxJava3TypeNames.MAYBE,
        factoryMethodName = RoomRxJava3MemberNames.RX_ROOM_CREATE_MAYBE,
        canBeNull = true,
    ),
    RX3_COMPLETABLE(
        version = RxVersion.THREE,
        className = RxJava3TypeNames.COMPLETABLE,
        factoryMethodName = RoomRxJava3MemberNames.RX_ROOM_CREATE_COMPLETABLE,
    );

    fun isSingle() = this == RX3_SINGLE
}

internal enum class RxVersion(
    val rxMarkerClassName: XClassName,
    val missingArtifactMessage: String,
) {
    THREE(
        rxMarkerClassName = RoomRxJava3TypeNames.RX3_ROOM_MARKER,
        missingArtifactMessage = ProcessorErrors.MISSING_ROOM_RXJAVA3_ARTIFACT,
    )
}
