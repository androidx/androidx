/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.savedstate.serialization

import androidx.annotation.IntDef
import androidx.savedstate.SavedState

/** Represents the key used for class discrimination during serialization and deserialization. */
internal const val CLASS_DISCRIMINATOR_KEY = "type"

/**
 * Defines which classes and objects should have their serial name included in the json as so-called
 * class discriminator.
 *
 * Class discriminator is a [androidx.savedstate.SavedState] field added by `kotlinx.serialization`
 * with a key (`type` by default), and class' serial name as a value (fully qualified name by
 * default).
 *
 * Class discriminator is important for serializing and deserializing
 * [polymorphic class hierarchies](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#sealed-classes).
 * Default [ClassDiscriminatorMode.POLYMORPHIC] mode adds discriminator only to polymorphic classes.
 * This behavior can be changed to match various JSON schemas.
 *
 * @see SavedStateConfiguration.classDiscriminatorMode
 */
public object ClassDiscriminatorMode {

    @IntDef(value = [ALL_OBJECTS, POLYMORPHIC])
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class Definition

    /**
     * Include class discriminators whenever possible.
     *
     * Given that class discriminator is added as a [SavedState] field, adding class discriminator
     * is possible when the resulting [SavedState] is an object — i.e., for Kotlin classes,
     * `object`s, and interfaces. More specifically, discriminator is added to the output of
     * serializers which descriptors have a [kotlinx.serialization.descriptors.SerialKind] of either
     * [kotlinx.serialization.descriptors.StructureKind.CLASS] or
     * [kotlinx.serialization.descriptors.StructureKind.OBJECT].
     */
    public const val ALL_OBJECTS: Int = 1

    /**
     * Include class discriminators for polymorphic classes.
     *
     * Sealed classes, abstract classes, and interfaces are polymorphic classes by definition. Open
     * classes can be polymorphic if they are serializable with
     * [kotlinx.serialization.PolymorphicSerializer] and properly registered in the
     * [kotlinx.serialization.modules.SerializersModule]. See
     * [kotlinx.serialization polymorphism guide](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#sealed-classes)
     * for details.
     *
     * @see kotlinx.serialization.modules.SerializersModule
     * @see kotlinx.serialization.modules.SerializersModuleBuilder
     * @see kotlinx.serialization.modules.PolymorphicModuleBuilder
     */
    public const val POLYMORPHIC: Int = 2
}
