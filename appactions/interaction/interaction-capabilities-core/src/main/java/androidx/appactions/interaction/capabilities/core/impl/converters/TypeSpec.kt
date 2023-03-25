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

package androidx.appactions.interaction.capabilities.core.impl.converters

import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException
import androidx.appactions.interaction.protobuf.Struct

/**
 * TypeSpec is used to convert between native objects in capabilities/values and Struct proto.
 */
interface TypeSpec<T> {
    /* Given the object, returns its identifier, which can be null. */
    fun getIdentifier(obj: T): String?

    /** Converts a object into a Struct proto. */
    fun toStruct(obj: T): Struct

    /**
     * Converts a Struct into object.
     *
     * @throws StructConversionException if the Struct is malformed.
     */
    @Throws(StructConversionException::class)
    fun fromStruct(struct: Struct): T
}
