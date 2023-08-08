/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.lifecycle

import androidx.annotation.RestrictTo
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException

/**
 * Internal class to handle lifecycle conversion etc.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public object Lifecycling {
    private const val REFLECTIVE_CALLBACK = 1
    private const val GENERATED_CALLBACK = 2
    private val callbackCache: MutableMap<Class<*>, Int> = HashMap()
    private val classToAdapters: MutableMap<Class<*>, List<Constructor<out GeneratedAdapter>>> =
        HashMap()

    @JvmStatic
    @Suppress("DEPRECATION")
    public fun lifecycleEventObserver(`object`: Any): LifecycleEventObserver {
        val isLifecycleEventObserver = `object` is LifecycleEventObserver
        val isDefaultLifecycleObserver = `object` is DefaultLifecycleObserver
        if (isLifecycleEventObserver && isDefaultLifecycleObserver) {
            return DefaultLifecycleObserverAdapter(
                `object` as DefaultLifecycleObserver,
                `object` as LifecycleEventObserver
            )
        }
        if (isDefaultLifecycleObserver) {
            return DefaultLifecycleObserverAdapter(`object` as DefaultLifecycleObserver, null)
        }
        if (isLifecycleEventObserver) {
            return `object` as LifecycleEventObserver
        }
        val klass: Class<*> = `object`.javaClass
        val type = getObserverConstructorType(klass)
        if (type == GENERATED_CALLBACK) {
            val constructors = classToAdapters[klass]!!
            if (constructors.size == 1) {
                val generatedAdapter = createGeneratedAdapter(
                    constructors[0], `object`
                )
                return SingleGeneratedAdapterObserver(generatedAdapter)
            }
            val adapters: Array<GeneratedAdapter> = Array(constructors.size) { i ->
                createGeneratedAdapter(constructors[i], `object`)
            }
            return CompositeGeneratedAdaptersObserver(adapters)
        }
        return ReflectiveGenericLifecycleObserver(`object`)
    }

    private fun createGeneratedAdapter(
        constructor: Constructor<out GeneratedAdapter>,
        `object`: Any
    ): GeneratedAdapter {
        return try {
            constructor.newInstance(`object`)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        } catch (e: InstantiationException) {
            throw RuntimeException(e)
        } catch (e: InvocationTargetException) {
            throw RuntimeException(e)
        }
    }

    @Suppress("DEPRECATION")
    private fun generatedConstructor(klass: Class<*>): Constructor<out GeneratedAdapter>? {
        return try {
            val aPackage = klass.getPackage()
            val name = klass.canonicalName
            val fullPackage = if (aPackage != null) aPackage.name else ""
            val adapterName =
                getAdapterName(
                    if (fullPackage.isEmpty()) name
                    else name.substring(fullPackage.length + 1)
                )
            @Suppress("UNCHECKED_CAST")
            val aClass = Class.forName(
                if (fullPackage.isEmpty()) adapterName else "$fullPackage.$adapterName"
            ) as Class<out GeneratedAdapter>
            val constructor = aClass.getDeclaredConstructor(klass)
            if (!constructor.isAccessible) {
                constructor.isAccessible = true
            }
            constructor
        } catch (e: ClassNotFoundException) {
            null
        } catch (e: NoSuchMethodException) {
            // this should not happen
            throw RuntimeException(e)
        }
    }

    private fun getObserverConstructorType(klass: Class<*>): Int {
        val callbackCache = callbackCache[klass]
        if (callbackCache != null) {
            return callbackCache
        }
        val type = resolveObserverCallbackType(klass)
        this.callbackCache[klass] = type
        return type
    }

    private fun resolveObserverCallbackType(klass: Class<*>): Int {
        // anonymous class bug:35073837
        if (klass.canonicalName == null) {
            return REFLECTIVE_CALLBACK
        }
        val constructor = generatedConstructor(klass)
        if (constructor != null) {
            classToAdapters[klass] = listOf(constructor)
            return GENERATED_CALLBACK
        }
        @Suppress("DEPRECATION")
        val hasLifecycleMethods = ClassesInfoCache.sInstance.hasLifecycleMethods(klass)
        if (hasLifecycleMethods) {
            return REFLECTIVE_CALLBACK
        }
        val superclass = klass.superclass
        var adapterConstructors: MutableList<Constructor<out GeneratedAdapter>>? = null
        if (isLifecycleParent(superclass)) {
            if (getObserverConstructorType(superclass) == REFLECTIVE_CALLBACK) {
                return REFLECTIVE_CALLBACK
            }
            adapterConstructors = ArrayList(
                classToAdapters[superclass]!!
            )
        }
        for (intrface in klass.interfaces) {
            if (!isLifecycleParent(intrface)) {
                continue
            }
            if (getObserverConstructorType(intrface) == REFLECTIVE_CALLBACK) {
                return REFLECTIVE_CALLBACK
            }
            if (adapterConstructors == null) {
                adapterConstructors = ArrayList()
            }
            adapterConstructors.addAll(classToAdapters[intrface]!!)
        }
        if (adapterConstructors != null) {
            classToAdapters[klass] = adapterConstructors
            return GENERATED_CALLBACK
        }
        return REFLECTIVE_CALLBACK
    }

    private fun isLifecycleParent(klass: Class<*>?): Boolean {
        return klass != null && LifecycleObserver::class.java.isAssignableFrom(klass)
    }

    /**
     * Create a name for an adapter class.
     */
    @JvmStatic
    public fun getAdapterName(className: String): String {
        return className.replace(".", "_") + "_LifecycleAdapter"
    }
}
