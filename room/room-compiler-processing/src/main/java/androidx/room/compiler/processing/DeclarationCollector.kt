/*
 * Copyright 2021 The Android Open Source Project
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
 * see [XTypeElement.getAllFieldsIncludingPrivateSupers]
 */
internal fun collectFieldsIncludingPrivateSupers(
    xTypeElement: XTypeElement
): Sequence<XFieldElement> {
    return sequence {
        val existingFieldNames = mutableSetOf<String>()
        suspend fun SequenceScope<XFieldElement>.yieldAllFields(type: XTypeElement) {
            // yield all fields declared directly on this type
            type.getDeclaredFields().forEach {
                if (existingFieldNames.add(it.name)) {
                    if (type == xTypeElement) {
                        yield(it)
                    } else {
                        yield(it.copyTo(xTypeElement))
                    }
                }
            }
            // visit all declared fields on super types
            type.superType?.typeElement?.let { parent ->
                yieldAllFields(parent)
            }
        }
        yieldAllFields(xTypeElement)
    }
}

/**
 * see [XTypeElement.getAllMethods]
 */
internal fun collectAllMethods(
    xTypeElement: XTypeElement
): Sequence<XMethodElement> {
    return sequence {
        // group methods by name for faster override checks
        val methodsByName = mutableMapOf<String, LinkedHashSet<XMethodElement>>()
        val visitedInterfaces = mutableSetOf<XTypeElement>()
        fun collectAllMethodsByName(type: XTypeElement) {
            // First, visit all super interface methods.
            type.getSuperInterfaceElements().forEach {
                // Skip if we've already visited the methods in this interface.
                if (visitedInterfaces.add(it)) {
                    collectAllMethodsByName(it)
                }
            }
            // Next, visit all super class methods.
            type.superType?.typeElement?.let {
                collectAllMethodsByName(it)
            }
            // Finally, visit all methods declared in this type.
            if (type == xTypeElement) {
                type.getDeclaredMethods().forEach {
                    methodsByName.getOrPut(it.name) { linkedSetOf() }.add(it)
                }
            } else {
                type.getDeclaredMethods()
                    .filter { it.isAccessibleFrom(type.packageName) }
                    .filterNot { it.isStaticInterfaceMethod() }
                    .map { it.copyTo(xTypeElement) }
                    .forEach { methodsByName.getOrPut(it.name) { linkedSetOf() }.add(it) }
            }
        }
        collectAllMethodsByName(xTypeElement)

        // Yield all non-overridden methods
        methodsByName.values.forEach { methodSet ->
            if (methodSet.size == 1) {
                // There's only one method with this name, so it can't be overridden
                yield(methodSet.single())
            } else {
                // There are multiple methods with the same name, so we must check for overridden
                // methods. The order of the methods should guarantee that any potentially
                // overridden method comes first in the list, so we only need to check each method
                // against subsequent methods.
                val methods = methodSet.toList()
                val overridden = mutableSetOf<XMethodElement>()
                methods.forEachIndexed { i, methodOne ->
                    methods.subList(i + 1, methods.size).forEach { methodTwo ->
                        if (methodTwo.overrides(methodOne, xTypeElement)) {
                            overridden.add(methodOne)
                            // Once we've added methodOne, we can break out of this inner loop since
                            // additional checks would only try to add methodOne again.
                            return@forEachIndexed
                        }
                    }
                }
                methods.filterNot { overridden.contains(it) }.forEach { yield(it) }
            }
        }
    }
}

private fun XMethodElement.isAccessibleFrom(packageName: String): Boolean {
    if (isPublic() || isProtected()) {
        return true
    }
    if (isPrivate()) {
        return false
    }
    // check package
    return packageName == enclosingElement.className.packageName()
}

private fun XMethodElement.isStaticInterfaceMethod(): Boolean {
    return isStatic() && (enclosingElement as? XTypeElement)?.isInterface() == true
}
