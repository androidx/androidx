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

package android.arch.lifecycle

import android.arch.lifecycle.model.AdapterClass
import android.arch.lifecycle.model.EventMethod
import android.arch.lifecycle.model.EventMethodCall
import android.arch.lifecycle.model.LifecycleObserverInfo
import com.google.auto.common.MoreTypes
import com.google.common.collect.HashMultimap
import java.util.LinkedList
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.type.NoType
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic


private fun superObservers(world: Map<TypeElement, LifecycleObserverInfo>,
                           observer: LifecycleObserverInfo): List<LifecycleObserverInfo> {
    val stack = LinkedList<TypeMirror>()
    stack += observer.type.interfaces.reversed()
    stack += observer.type.superclass
    val result = mutableListOf<LifecycleObserverInfo>()
    while (stack.isNotEmpty()) {
        val typeMirror = stack.removeLast()
        if (typeMirror is NoType) {
            continue
        }
        val type = MoreTypes.asTypeElement(typeMirror)
        val currentObserver = world[type]
        if (currentObserver != null) {
            result.add(currentObserver)
        } else {
            stack += type.interfaces.reversed()
            stack += type.superclass
        }
    }
    return result
}

private fun mergeAndVerifyMethods(processingEnv: ProcessingEnvironment,
                                  type: TypeElement,
                                  classMethods: List<EventMethod>,
                                  parentMethods: List<EventMethod>): List<EventMethod> {
    // need to update parent methods like that because:
    // 1. visibility can be expanded
    // 2. we want to preserve order
    val updatedParentMethods = parentMethods.map { parentMethod ->
        val overrideMethod = classMethods.find { (method) ->
            processingEnv.elementUtils.overrides(method, parentMethod.method, type)
        }
        if (overrideMethod != null) {
            if (overrideMethod.onLifecycleEvent != parentMethod.onLifecycleEvent) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR,
                        ErrorMessages.INVALID_STATE_OVERRIDE_METHOD, overrideMethod.method)
            }
            overrideMethod
        } else {
            parentMethod
        }
    }
    return updatedParentMethods + classMethods.filterNot { updatedParentMethods.contains(it) }
}

fun flattenObservers(processingEnv: ProcessingEnvironment,
                     world: Map<TypeElement, LifecycleObserverInfo>): List<LifecycleObserverInfo> {
    val flattened: MutableMap<LifecycleObserverInfo, LifecycleObserverInfo> = mutableMapOf()
    val superObservers = world.mapValues { superObservers(world, it.value) }

    fun traverse(observer: LifecycleObserverInfo) {
        if (observer in flattened) {
            return
        }
        val observers = superObservers[observer.type]!!
        if (observers.isEmpty()) {
            flattened[observer] = observer
            return
        }
        observers.filter { it !in flattened }.forEach(::traverse)
        val methods = observers
                .map(flattened::get)
                .fold(emptyList<EventMethod>()) { list, parentObserver ->
                    mergeAndVerifyMethods(processingEnv, observer.type, parentObserver!!.methods, list)
                }

        flattened[observer] = LifecycleObserverInfo(observer.type,
                mergeAndVerifyMethods(processingEnv, observer.type, observer.methods, methods))
    }

    world.values.forEach(::traverse)
    return flattened.values.toList()
}

fun transformToOutput(processingEnv: ProcessingEnvironment,
                      world: Map<TypeElement, LifecycleObserverInfo>): List<AdapterClass> {
    val flatObservers = flattenObservers(processingEnv, world)
    val syntheticMethods = HashMultimap.create<TypeElement, EventMethodCall>()
    val adapterCalls = flatObservers.map { (type, methods) ->
        val calls = methods.map { eventMethod ->
            val executable = eventMethod.method
            if (type.getPackageQName() != eventMethod.packageName()
                    && (executable.isPackagePrivate() || executable.isProtected())) {
                EventMethodCall(eventMethod, eventMethod.type)
            } else {
                EventMethodCall(eventMethod)
            }
        }
        calls.filter { it.syntheticAccess != null }.forEach { eventMethod ->
            syntheticMethods.put(eventMethod.method.type, eventMethod)
        }
        type to calls
    }.toMap()

    return adapterCalls.map { (type, calls) ->
        val methods = syntheticMethods.get(type) ?: setOf()
        val synthetic = methods.map { eventMethod -> eventMethod!!.method.method }.toSet()
        AdapterClass(type, calls, synthetic)
    }
}
