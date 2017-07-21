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

import android.arch.lifecycle.model.LifecycleObserverInfo
import android.arch.lifecycle.model.StateMethod
import com.google.auto.common.MoreTypes
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
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
                                  classMethods: List<StateMethod>,
                                  parentMethods: List<StateMethod>): List<StateMethod> {
    return parentMethods + classMethods.filter { (method, onLifecycleEvent) ->
        val baseMethod = parentMethods.find { m ->
            method.simpleName == m.method.simpleName
                    && method.parameters.size == m.method.parameters.size
        }
        if (baseMethod != null
                && baseMethod.onLifecycleEvent != onLifecycleEvent) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR,
                    ErrorMessages.INVALID_STATE_OVERRIDE_METHOD, method)
        }
        baseMethod == null
    }

}

fun transformToOutput(processingEnv: ProcessingEnvironment,
                      world: Map<TypeElement, LifecycleObserverInfo>): List<LifecycleObserverInfo> {
    val superObservers = world.mapValues { superObservers(world, it.value) }
    val packagePrivateMethods = world.mapValues { observer ->
        if (observer.value.type.kind.isInterface) {
            emptyList()
        } else {
            observer.value.methods.filter {
                it.method.isPackagePrivate() || it.method.isProtected()
            }.map { it.method }
        }
    }

    val ppMethodsToType = packagePrivateMethods.entries.fold(
            mapOf<ExecutableElement, TypeElement>(), { map, entry ->
        map + entry.value.associate { it to entry.key }
    })

    world.values.forEach {
        val observers = superObservers[it.type]!!
        val currentPackage = it.type.getPackageQName()
        observers.filter { superObserver ->
            superObserver.type.getPackageQName() != currentPackage
                    && packagePrivateMethods[superObserver.type]!!.isNotEmpty()
        }.forEach { it.syntheticMethods.addAll(packagePrivateMethods[it.type]!!) }
    }


    val flattened: MutableMap<LifecycleObserverInfo, LifecycleObserverInfo> = mutableMapOf()
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
        val currentPackage = observer.type.getPackageQName()
        val methods = observers
                .fold(emptyList<StateMethod>(), { list, parentObserver ->
                    mergeAndVerifyMethods(processingEnv, parentObserver.methods, list)
                })
                .map {
                    val packageName = ppMethodsToType[it.method]?.getPackageQName()
                    if (packageName == null || packageName == currentPackage) {
                        it
                    } else {
                        StateMethod(it.method, it.onLifecycleEvent, ppMethodsToType[it.method])
                    }
                }

        flattened[observer] = LifecycleObserverInfo(observer.type,
                mergeAndVerifyMethods(processingEnv, observer.methods, methods),
                observer.syntheticMethods)
    }

    world.values.forEach(::traverse)
    return flattened.values.toList()
}
