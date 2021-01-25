/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.solver.prepared.binderprovider

import androidx.room.compiler.processing.XType
import androidx.room.parser.ParsedQuery
import androidx.room.solver.prepared.binder.PreparedQueryResultBinder

/**
 * Interface for for providing the appropriate [PreparedQueryResultBinder] given a query and a
 * method's return type.
 */
interface PreparedQueryResultBinderProvider {
    /**
     * Check whether the [XType] can be handled by the [PreparedQueryResultBinder] provided
     * by this provider.
     */
    fun matches(declared: XType): Boolean

    /**
     * Provides a [PreparedQueryResultBinder]
     */
    fun provide(declared: XType, query: ParsedQuery): PreparedQueryResultBinder
}