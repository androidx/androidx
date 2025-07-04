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

import androidx.savedstate.serialization.SavedStateConfiguration.Builder
import androidx.savedstate.serialization.serializers.MutableStateFlowSerializer
import androidx.savedstate.serialization.serializers.SavedStateSerializer
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.overwriteWith
import kotlinx.serialization.modules.plus

/**
 * Configuration of the current [SavedStateConfiguration] configured with
 * [SavedStateConfiguration.Builder].
 *
 * Can be used via [encodeToSavedState] and [decodeFromSavedState].
 *
 * Standalone configuration object cannot be used outside the encode and decode functions provided
 * by SavedState.
 *
 * Detailed description of each property is available in [SavedStateConfiguration.Builder] class.
 *
 * @see SavedStateConfiguration.Builder
 */
public class SavedStateConfiguration
private constructor(
    public val serializersModule: SerializersModule = DEFAULT_SERIALIZERS_MODULE,
    @ClassDiscriminatorMode.Definition
    public val classDiscriminatorMode: Int = ClassDiscriminatorMode.POLYMORPHIC,
    @get:Suppress("GetterSetterNames") // More idiomatic, matches KTX Serialization naming.
    public val encodeDefaults: Boolean = false,
) {
    /**
     * Builder of the [SavedStateConfiguration] instance provided by `SavedStateConfig { ... }`
     * factory function.
     */
    @Suppress("MissingBuildMethod") // `build()` is internal, only used by the DSL.
    public class Builder internal constructor(configuration: SavedStateConfiguration) {

        /**
         * Module with contextual and polymorphic serializers to be used in the resulting
         * [SavedStateConfiguration] instance.
         *
         * Note that this [SerializersModule] will be combined with an internal [SerializersModule]
         * in the resulting [SavedStateConfiguration] to support built-in types.
         *
         * @see SerializersModule
         * @see Contextual
         * @see Polymorphic
         */
        @get:Suppress("GetterOnBuilder") // KT-3110 (private get with public set).
        @set:Suppress("SetterReturnsThis") // DSL-like builder, no need to return this.
        public var serializersModule: SerializersModule = configuration.serializersModule

        /**
         * Specifies whether default values of Kotlin properties should be encoded. `false` by
         * default. This option does not affect decoding.
         */
        @Suppress("GetterSetterNames") // More idiomatic, matches KTX Serialization naming.
        @get:Suppress("GetterOnBuilder") // KT-3110 (private get with public set).
        @set:Suppress("SetterReturnsThis") // DSL-like builder, no need to return this.
        public var encodeDefaults: Boolean = configuration.encodeDefaults

        /**
         * Defines which classes and objects should have class discriminator added to the output.
         * [ClassDiscriminatorMode.POLYMORPHIC] by default.
         *
         * @see ClassDiscriminatorMode
         */
        @get:Suppress("GetterOnBuilder") // KT-3110 (private get with public set).
        @set:Suppress("SetterReturnsThis") // DSL-like builder, no need to return this.
        @ClassDiscriminatorMode.Definition
        public var classDiscriminatorMode: Int = configuration.classDiscriminatorMode

        internal fun build(): SavedStateConfiguration {
            return SavedStateConfiguration(
                serializersModule = DEFAULT_SERIALIZERS_MODULE.overwriteWith(serializersModule),
                classDiscriminatorMode = classDiscriminatorMode,
                encodeDefaults = encodeDefaults,
            )
        }
    }

    public companion object {
        /**
         * The default instance of [SavedStateConfiguration] with default configuration.
         *
         * This configuration is used by [encodeToSavedState] and [decodeFromSavedState] unless an
         * alternative configuration is explicitly provided.
         *
         * @see encodeToSavedState
         * @see decodeFromSavedState
         */
        @JvmField public val DEFAULT: SavedStateConfiguration = SavedStateConfiguration()
    }
}

/**
 * Creates an instance of [SavedStateConfiguration] configured from the optionally given [from] and
 * adjusted with [builderAction].
 *
 * @sample androidx.savedstate.config
 * @param from An optional initial [SavedStateConfiguration] to start with. Defaults to
 *   [SavedStateConfiguration.DEFAULT].
 * @param builderAction A lambda function to configure the [Builder] for additional customization.
 * @return A new [SavedStateConfiguration] instance configured based on the provided parameters.
 */
@JvmOverloads
public fun SavedStateConfiguration(
    from: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
    builderAction: Builder.() -> Unit,
): SavedStateConfiguration {
    return Builder(from).apply(builderAction).build()
}

internal expect fun getDefaultSerializersModuleOnPlatform(): SerializersModule

private val DEFAULT_SERIALIZERS_MODULE: SerializersModule =
    SerializersModule {
        contextual(SavedStateSerializer)
        contextual(MutableStateFlow::class) { elementSerializers ->
            MutableStateFlowSerializer(elementSerializers.first())
        }
    } + getDefaultSerializersModuleOnPlatform()
