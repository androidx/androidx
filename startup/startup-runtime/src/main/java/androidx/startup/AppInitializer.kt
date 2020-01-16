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

package androidx.startup

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.VisibleForTesting

/**
 * An [AppInitializer] can be used to initialize all discovered [ComponentInitializer]s.
 * <br/>
 * The discovery mechanism is via `<meta-data>` entries in the merged `AndroidManifest.xml`.
 */
class AppInitializer {
    companion object {

        /**
         * Discovers an initializes all available [ComponentInitializer] classes based on the merged
         * manifest `<meta-data>` entries in the `AndroidManifest.xml`.
         * @param context The Application context
         */
        @JvmName("initialize")
        fun initialize(context: Context) {
            val components = discoverComponents(context)
            initialize(context, components)
        }

        /**
         * Initializes a [List] of [ComponentInitializer] class types.
         *
         * @param context The Application context
         * @param components The [List] of [Class]es that represent all discovered
         * [ComponentInitializer]s
         */
        @JvmName("initialize")
        fun initialize(context: Context, components: List<Class<*>>) {
            initialize(context, components, mutableSetOf(), mutableSetOf())
        }

        @VisibleForTesting
        internal fun initialize(
            context: Context,
            components: List<Class<*>>,
            initializing: MutableSet<Class<*>>,
            initialized: MutableSet<Class<*>>
        ) {
            for (component in components) {
                check(component !in initializing) {
                    "Cannot initialize $component. Cycle detected."
                }
                if (component !in initialized) {
                    initializing.add(component)
                    val instance = component.newInstance()
                    check(instance is ComponentInitializer<*>) {
                        "Component $component is not a subtype of ComponentInitializer."
                    }
                    val dependencies = instance.dependencies().filterNot {
                        it in initialized
                    }
                    if (dependencies.isNotEmpty()) {
                        initialize(context, dependencies, initializing, initialized)
                    }
                    StartupLogger.i { "Initializing $component" }
                    instance.create(context)
                    StartupLogger.i { "Initialized $component" }
                    initializing.remove(component)
                    initialized.add(component)
                }
            }
        }

        @VisibleForTesting
        internal fun discoverComponents(context: Context): List<Class<*>> {
            val metadata = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            ).metaData

            val startup = context.getString(R.string.androidx_startup)
            return if (metadata != null && metadata.size() > 0) {
                val components = mutableListOf<Class<*>>()
                metadata.keySet().forEach { key ->
                    val value = metadata.getString(key, null)
                    if (startup == value) {
                        try {
                            val clazz = Class.forName(key)
                            StartupLogger.i { "Discovered ($clazz)" }
                            components.add(clazz)
                        } catch (throwable: Throwable) {
                            val message = "Cannot find ComponentInitializer ($key)"
                            StartupLogger.e(message, throwable)
                            throw StartupException(message, throwable)
                        }
                    }
                }
                components
            } else {
                emptyList()
            }
        }
    }
}
