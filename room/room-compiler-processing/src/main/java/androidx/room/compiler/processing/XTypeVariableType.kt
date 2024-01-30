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

package androidx.room.compiler.processing

/**
 * Represents a type variable.
 *
 * @see [javax.lang.model.type.TypeVariable]
 * @see [com.google.devtools.ksp.symbol.KSTypeParameter]
 */
interface XTypeVariableType : XType {
    /**
     * The upper bounds of the type variable.
     *
     * Note that this model differs a bit from the Javac model where
     * [javax.lang.model.type.TypeVariable] always has a single `upperBound` which may end up
     * resolving to an [javax.lang.model.type.IntersectionType]. Instead, this model is closer to
     * the KSP model where the type variable may return multiple upper bounds when an intersection
     * type exists rather than representing the intersection type as a unique type in the model.
     */
    val upperBounds: List<XType>
}
