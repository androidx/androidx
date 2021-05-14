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

package androidx.room.compiler.processing

/**
 * Helper class to collect all methods of an [XTypeElement] to implement
 * [XTypeElement.getAllMethods].
 */
private class MethodCollector(
    val target: XTypeElement
) {
    // group methods by name for fast overrides check
    private val selectionByName = mutableMapOf<String, MutableList<XMethodElement>>()

    // we keep a duplicate list to preserve declaration order, makes the generated code match
    // user code
    private val selection = mutableListOf<XMethodElement>()

    fun collect() {
        val selection = target.getDeclaredMethods().forEach(::addToSelection)

        target.superType
            ?.typeElement
            ?.getAllMethods()
            ?.forEach(::addIfNotOverridden)
        target.getSuperInterfaceElements().forEach {
            it.getAllMethods().forEach {
                if (!it.isStatic()) {
                    addIfNotOverridden(it)
                }
            }
        }
        return selection
    }

    fun getResult(): List<XMethodElement> {
        return selection
    }

    private fun addIfNotOverridden(candidate: XMethodElement) {
        if (!target.canAccessSuperMethod(candidate)) {
            return
        }
        val overridden = selectionByName[candidate.name]?.any { existing ->
            existing.overrides(candidate, target)
        } ?: false
        if (!overridden) {
            addToSelection(candidate.copyTo(target))
        }
    }

    private fun addToSelection(method: XMethodElement) {
        selectionByName.getOrPut(method.name) {
            mutableListOf()
        }.add(method)
        selection.add(method)
    }

    private fun XTypeElement.canAccessSuperMethod(other: XMethodElement): Boolean {
        if (other.isPublic() || other.isProtected()) {
            return true
        }
        if (other.isPrivate()) {
            return false
        }
        // check package
        return packageName == other.enclosingElement.className.packageName()
    }
}

internal fun XTypeElement.collectAllMethods(): List<XMethodElement> {
    val collector = MethodCollector(this)
    collector.collect()
    return collector.getResult()
}