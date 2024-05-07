/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.savedstate

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.savedstate.SavedStateRegistry.AutoRecreated

internal class Recreator(private val owner: SavedStateRegistryOwner) : LifecycleEventObserver {

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event != Lifecycle.Event.ON_CREATE) {
            throw AssertionError("Next event must be ON_CREATE")
        }
        source.lifecycle.removeObserver(this)

        val registry = owner.savedStateRegistry
        val savedState = registry.consumeRestoredStateForKey(COMPONENT_KEY) ?: return
        val classes =
            savedState.read {
                return@read getStringListOrElse(CLASSES_KEY) {
                    error(
                        "SavedState with restored state for the component " +
                            "\"$COMPONENT_KEY\" must contain list of strings by the key " +
                            "\"$CLASSES_KEY\""
                    )
                }
            }
        for (className: String in classes) {
            reflectiveNew(className)
        }
    }

    private fun reflectiveNew(className: String) {
        val clazz: Class<out AutoRecreated> =
            try {
                Class.forName(className, false, Recreator::class.java.classLoader)
                    .asSubclass(AutoRecreated::class.java)
            } catch (e: ClassNotFoundException) {
                throw RuntimeException("Class $className wasn't found", e)
            }
        val constructor =
            try {
                clazz.getDeclaredConstructor()
            } catch (e: NoSuchMethodException) {
                throw IllegalStateException(
                    "Class ${clazz.simpleName} must have " +
                        "default constructor in order to be automatically recreated",
                    e
                )
            }
        constructor.isAccessible = true
        val newInstance: AutoRecreated =
            try {
                constructor.newInstance()
            } catch (e: Exception) {
                throw RuntimeException("Failed to instantiate $className", e)
            }
        newInstance.onRecreated(owner)
    }

    internal class SavedStateProvider(registry: SavedStateRegistry) :
        SavedStateRegistry.SavedStateProvider {

        private val classes: MutableSet<String> = mutableSetOf()

        init {
            registry.registerSavedStateProvider(COMPONENT_KEY, this)
        }

        override fun saveState(): SavedState = savedState {
            putStringList(CLASSES_KEY, classes.toList())
        }

        fun add(className: String) {
            classes.add(className)
        }
    }

    companion object {
        const val CLASSES_KEY = "classes_to_restore"
        const val COMPONENT_KEY = "androidx.savedstate.Restarter"
    }
}
