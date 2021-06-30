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
 * Helper method to collect declarations from a type element and its parents.
 *
 * To be able to benefit from multi level caching (e.g. each type element caching its important
 * fields / declarations), parents are not visited recursively. Instead, callers are expected to
 * call the right methods when traversing immediate parents / interfaces.
 */
private fun <T> collectDeclarations(
    /**
     * The root element
     */
    target: XTypeElement,
    /**
     * Elements that will be added without verification. These are usually inherited from the
     * root element.
     */
    initialList: List<T>,
    /**
     * Returns a list of possible elements that can be included in the final list.
     * The receiver is either a super class or interface.
     */
    getCandidateDeclarations: XTypeElement.() -> Iterable<T>,
    /**
     * Returns a partitan key for each element to optimize override checks etc.
     */
    getPartitionKey: T.() -> String,
    /**
     * Returns true if this item should be added to the list, false otherwise.
     */
    isAcceptable: (candidate: T, existing: List<T>) -> Boolean,
    /**
     * Copies the element to the root element
     */
    copyToTarget: (T) -> T,
    /**
     * If true, parent class will be visited.
     */
    visitParent: Boolean,
    /**
     * If true, parent interfaces will be visited.
     */
    visitInterfaces: Boolean
): Sequence<T> {
    val parents = sequence {
        if (visitParent) {
            target.superType?.typeElement?.let {
                yield(it)
            }
        }
        if (visitInterfaces) {
            yieldAll(target.getSuperInterfaceElements())
        }
    }
    return sequence {
        // group members by name for faster override checks
        val selectionByName = mutableMapOf<String, MutableList<T>>()
        suspend fun SequenceScope<T>.addToSelection(item: T) {
            val key = getPartitionKey(item)
            selectionByName.getOrPut(key) {
                mutableListOf()
            }.add(item)
            yield(item)
        }

        suspend fun SequenceScope<T>.maybeAddToSelection(candidate: T) {
            val partitionKey = candidate.getPartitionKey()
            val existing = selectionByName[partitionKey] ?: emptyList()
            if (isAcceptable(candidate, existing)) {
                addToSelection(copyToTarget(candidate))
            }
        }

        // yield everything in the root list
        initialList.forEach {
            addToSelection(it)
        }
        // now start yielding it from parents
        parents.flatMap { it.getCandidateDeclarations() }.forEach {
            maybeAddToSelection(copyToTarget(it))
        }
    }
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

/**
 * see [XTypeElement.getAllFieldsIncludingPrivateSupers]
 */
internal fun collectFieldsIncludingPrivateSupers(
    xTypeElement: XTypeElement
): Sequence<XFieldElement> {
    return collectDeclarations(
        target = xTypeElement,
        visitParent = true,
        visitInterfaces = false,
        getPartitionKey = XFieldElement::name,
        initialList = xTypeElement.getDeclaredFields(),
        copyToTarget = {
            it.copyTo(xTypeElement)
        },
        isAcceptable = { _, existing ->
            existing.isEmpty()
        },
        getCandidateDeclarations = XTypeElement::getAllFieldsIncludingPrivateSupers
    )
}

/**
 * see [XTypeElement.getAllMethods]
 */
internal fun collectAllMethods(
    xTypeElement: XTypeElement
): Sequence<XMethodElement> {
    return collectDeclarations(
        target = xTypeElement,
        visitParent = true,
        visitInterfaces = true,
        getPartitionKey = XMethodElement::name,
        initialList = xTypeElement.getDeclaredMethods(),
        copyToTarget = {
            it.copyTo(xTypeElement)
        },
        isAcceptable = { candidate, existing ->
            when {
                // my method, accept all
                candidate.enclosingElement == xTypeElement -> true
                // cannot access, reject
                !xTypeElement.canAccessSuperMethod(candidate) -> false
                // static in an interface
                candidate.isStatic() &&
                    (candidate.enclosingElement as? XTypeElement)?.isInterface() == true ->
                    false
                // accept if not overridden
                else -> existing.none { it.overrides(candidate, xTypeElement) }
            }
        },
        getCandidateDeclarations = XTypeElement::getAllMethods,
    )
}