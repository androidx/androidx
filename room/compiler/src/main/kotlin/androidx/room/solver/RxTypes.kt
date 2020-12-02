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

package androidx.room.solver

import androidx.room.ext.RoomRxJava2TypeNames
import androidx.room.ext.RoomRxJava3TypeNames
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.RxJava3TypeNames
import androidx.room.processor.ProcessorErrors
import com.squareup.javapoet.ClassName

internal enum class RxType(
    val version: RxVersion,
    val className: ClassName,
    val factoryMethodName: String? = null,
    val canBeNull: Boolean = false
) {
    // RxJava2 types
    RX2_FLOWABLE(
        version = RxVersion.TWO,
        className = RxJava2TypeNames.FLOWABLE,
        factoryMethodName = RoomRxJava2TypeNames.RX_ROOM_CREATE_FLOWABLE
    ),
    RX2_OBSERVABLE(
        version = RxVersion.TWO,
        className = RxJava2TypeNames.OBSERVABLE,
        factoryMethodName = RoomRxJava2TypeNames.RX_ROOM_CREATE_OBSERVABLE
    ),
    RX2_SINGLE(
        version = RxVersion.TWO,
        className = RxJava2TypeNames.SINGLE
    ),
    RX2_MAYBE(
        version = RxVersion.TWO,
        className = RxJava2TypeNames.MAYBE,
        canBeNull = true
    ),
    RX2_COMPLETABLE(
        version = RxVersion.TWO,
        className = RxJava2TypeNames.COMPLETABLE
    ),
    // RxJava3 types
    RX3_FLOWABLE(
        version = RxVersion.THREE,
        className = RxJava3TypeNames.FLOWABLE,
        factoryMethodName = RoomRxJava3TypeNames.RX_ROOM_CREATE_FLOWABLE
    ),
    RX3_OBSERVABLE(
        version = RxVersion.THREE,
        className = RxJava3TypeNames.OBSERVABLE,
        factoryMethodName = RoomRxJava3TypeNames.RX_ROOM_CREATE_OBSERVABLE
    ),
    RX3_SINGLE(
        version = RxVersion.THREE,
        className = RxJava3TypeNames.SINGLE
    ),
    RX3_MAYBE(
        version = RxVersion.THREE,
        className = RxJava3TypeNames.MAYBE,
        canBeNull = true
    ),
    RX3_COMPLETABLE(
        version = RxVersion.THREE,
        className = RxJava3TypeNames.COMPLETABLE
    );

    fun isSingle() = this == RX2_SINGLE || this == RX3_SINGLE
}

internal enum class RxVersion(
    val rxRoomClassName: ClassName,
    val emptyResultExceptionClassName: ClassName,
    val missingArtifactMessage: String
) {
    TWO(
        rxRoomClassName = RoomRxJava2TypeNames.RX_ROOM,
        emptyResultExceptionClassName = RoomRxJava2TypeNames.RX_EMPTY_RESULT_SET_EXCEPTION,
        missingArtifactMessage = ProcessorErrors.MISSING_ROOM_RXJAVA2_ARTIFACT
    ),
    THREE(
        rxRoomClassName = RoomRxJava3TypeNames.RX_ROOM,
        emptyResultExceptionClassName = RoomRxJava3TypeNames.RX_EMPTY_RESULT_SET_EXCEPTION,
        missingArtifactMessage = ProcessorErrors.MISSING_ROOM_RXJAVA3_ARTIFACT
    );
}