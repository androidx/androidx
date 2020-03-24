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

package androidx.ui.savedinstancestate

import androidx.compose.Composer
import androidx.compose.staticAmbientOf

/**
 * Allows components to save and restore their state using the saved instance state mechanism.
 */
interface UiSavedStateRegistry {
    /**
     * Returns the restored value for the given key.
     * Once being restored the value is cleared, so you can't restore the same key twice.
     *
     * @param key Key used to save the value
     */
    fun consumeRestored(key: String): Any?

    /**
     * Registers the value provider.
     *
     * The same [key] cannot be registered twice, if you need to update the provider call
     * [unregisterProvider] first and then register again.
     *
     * @param key Key to use for storing the value
     * @param valueProvider Provides the current value, to be executed when [performSave]
     * will be triggered to collect all the registered values
     */
    fun registerProvider(key: String, valueProvider: () -> Any?)

    /**
     * Unregisters the value provider previously registered via [registerProvider].
     *
     * @param key Key of the value which shouldn't be saved anymore
     */
    fun unregisterProvider(key: String)

    /**
     * Returns true if the value can be saved using this Registry.
     * The default implementation will return true if this value can be stored in Bundle.
     *
     * @param value The value which we want to save using this Registry
     */
    fun canBeSaved(value: Any): Boolean

    /**
     * Executes all the registered value providers and combines these values into a key-value map.
     */
    fun performSave(): Map<String, Any>
}

/**
 * Creates [UiSavedStateRegistry].
 *
 * @param restoredValues The map of the restored values
 * @param canBeSaved Function which returns true if the given value can be saved by the registry
 */
fun UiSavedStateRegistry(
    restoredValues: Map<String, Any>?,
    canBeSaved: (Any) -> Boolean
): UiSavedStateRegistry = UiSavedStateRegistryImpl(restoredValues, canBeSaved)

/**
 * Ambient with a current [UiSavedStateRegistry] instance.
 */
val UiSavedStateRegistryAmbient = staticAmbientOf<UiSavedStateRegistry?> { null }

private class UiSavedStateRegistryImpl(
    restored: Map<String, Any>?,
    private val canBeSaved: (Any) -> Boolean
) : UiSavedStateRegistry {

    private val restored: MutableMap<String, Any> = restored?.toMutableMap() ?: mutableMapOf()
    private val valueProviders = mutableMapOf<String, () -> Any?>()

    override fun canBeSaved(value: Any): Boolean = canBeSaved.invoke(value)

    override fun consumeRestored(key: String): Any? = restored.remove(key)

    override fun registerProvider(key: String, valueProvider: () -> Any?) {
        require(key.isNotBlank()) { "Registered key is empty or blank" }
        require(!valueProviders.contains(key)) {
            "Key $key was already registered. Please call " +
                    "unregister before registering again"
        }
        @Suppress("UNCHECKED_CAST")
        valueProviders[key] = valueProvider
    }

    override fun unregisterProvider(key: String) {
        require(valueProviders.contains(key)) {
            "Key $key wasn't registered, but unregister " +
                    "requested"
        }
        valueProviders.remove(key)
    }

    override fun performSave(): Map<String, Any> {
        val map = restored.toMutableMap()
        valueProviders.forEach {
            val value = it.value()
            if (value != null) {
                check(canBeSaved(value))
                map[it.key] = value
            }
        }
        return map
    }
}

// NOTE(lmr): This API is no longer needed in any way by the compiler, but we still need this API
// to be here to support versions of Android Studio that are still looking for it. Without it,
// valid composable code will look broken in the IDE. Remove this after we have left some time to
// get all versions of Studio upgraded.
// b/152059242
@Deprecated(
    "This property should not be called directly. It is only used by the compiler.",
    replaceWith = ReplaceWith("currentComposer")
)
internal val composer: Composer<*>
    get() = error(
        "This property should not be called directly. It is only used by the compiler."
    )
