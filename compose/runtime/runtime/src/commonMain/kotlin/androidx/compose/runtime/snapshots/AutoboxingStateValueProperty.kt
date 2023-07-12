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

package androidx.compose.runtime.snapshots

/**
 * This annotation designates that a property on a [State] class will autobox when it is read from
 * or assigned to. This is helpful for state APIs like [IntState], which define an alternative
 * value property that does not box while maintaining compatibility with the generic
 * [`State<T>`][State] API.
 *
 * Whenever a property that is annotated with `AutoboxingStateValueProperty` is accessed in code,
 * it will be flagged with a warning and will suggest using an alternative, non-boxing property
 * instead.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
annotation class AutoboxingStateValueProperty(
    /**
     * An alternative, non-boxing property that can be used instead of the annotated property.
     * The property indicated in this property should contain the exact same value as the annotated
     * property and should be observed in Compose in the same way, meaning that the designated
     * replacement property can serve as a drop-in replacement to the annotated property.
     *
     * This property name will be used for suggesting quick fixes. It must match the suggested
     * property name exactly, including its case.
     */
    @Suppress("unused") // Used by lint
    val preferredPropertyName: String
)
