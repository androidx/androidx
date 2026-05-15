/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.testing

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.util.fastAny
import androidx.xr.compose.subspace.node.SubspaceSemanticsInfo

/**
 * Verifies the 3D semantic node's content description for accessibility services.
 *
 * Note that 2D UI content rendered inside a Subspace panel (like text or buttons) maintains a
 * separate 2D semantics tree.
 *
 * @param value The exact or partial description string expected on the spatial node.
 * @param substring If true, matches any node whose description contains [value]. Defaults to false.
 * @param ignoreCase If true, case differences are ignored during comparison. Defaults to false.
 * @return a matcher validating the presence of the content description.
 */
public fun hasContentDescription(
    value: String,
    substring: Boolean = false,
    ignoreCase: Boolean = false,
): SubspaceSemanticsMatcher {
    return if (substring) {
        SubspaceSemanticsMatcher(
            "${SemanticsProperties.ContentDescription.name} contains '$value' " +
                "(ignoreCase: $ignoreCase)"
        ) {
            it.semanticsConfiguration.getOrNull(SemanticsProperties.ContentDescription)?.any { item
                ->
                item.contains(value, ignoreCase)
            } ?: false
        }
    } else {
        SubspaceSemanticsMatcher(
            "${SemanticsProperties.ContentDescription.name} = '$value' (ignoreCase: $ignoreCase)"
        ) {
            it.semanticsConfiguration.getOrNull(SemanticsProperties.ContentDescription)?.any { item
                ->
                item.equals(value, ignoreCase)
            } ?: false
        }
    }
}

/**
 * Verifies the 3D semantic node's test tag.
 *
 * This is the primary mechanism for addressing spatial containers, anchors, and 3D panels in
 * Compose for XR tests.
 *
 * @param testTag The string identifier assigned to the spatial element.
 * @return a matcher for the specified [testTag].
 */
public fun hasTestTag(testTag: String): SubspaceSemanticsMatcher =
    SubspaceSemanticsMatcher.expectValue(SemanticsProperties.TestTag, testTag)

/**
 * Verifies that the node is the root of the 3D semantics hierarchy.
 *
 * @return a matcher that matches the root semantics node.
 */
public fun isRoot(): SubspaceSemanticsMatcher = SubspaceSemanticsMatcher("isRoot") { it.isRoot }

/**
 * Verifies the direct parent of a 3D semantic node.
 *
 * This is indispensable for nested spatial compositions, such as a `PlanarEmbeddedSubspace`
 * composed inside a `SpatialPanel`, to confirm exact container anchoring.
 *
 * @param matcher The semantic check to evaluate against the parent node.
 * @return a matcher validating the parent's properties.
 */
public fun hasParent(matcher: SubspaceSemanticsMatcher): SubspaceSemanticsMatcher {
    return SubspaceSemanticsMatcher("hasParentThat(${matcher.description})") {
        it.parentInfo?.run { matcher.matches(this) } ?: false
    }
}

/**
 * Verifies whether any immediate child of the 3D semantic node satisfies the given criteria.
 *
 * Useful when testing spatial composite layouts or multiple anchored floating panels grouped under
 * a common parent semantic container.
 *
 * @param matcher The semantic criteria for the expected child.
 * @return a matcher validating the node's children.
 */
public fun hasAnyChild(matcher: SubspaceSemanticsMatcher): SubspaceSemanticsMatcher {
    return SubspaceSemanticsMatcher("hasAnyChildThat(${matcher.description})") {
        matcher.matchesAny(it.childrenInfo)
    }
}

/**
 * Verifies whether the 3D semantic node has any sibling satisfying the given condition.
 *
 * A sibling is defined as any other spatial node sharing the exact same parent container.
 *
 * @param matcher The semantic criteria to evaluate against siblings.
 * @return a matcher validating the node's siblings.
 */
public fun hasAnySibling(matcher: SubspaceSemanticsMatcher): SubspaceSemanticsMatcher {
    return SubspaceSemanticsMatcher("hasAnySiblingThat(${matcher.description})") {
        val node = it
        it.parentInfo?.run {
            matcher.matchesAny(
                this.childrenInfo.filter { child -> child.semanticsId != node.semanticsId }
            )
        } ?: false
    }
}

/**
 * Verifies whether any ancestor in the path to the 3D semantics root satisfies the given matcher.
 *
 * Example: For the following spatial semantic tree:
 * ```
 * |-X
 * |-A
 *   |-B
 *     |-C1
 *     |-C2
 * ```
 *
 * In case of C1, we would check the matcher against A and B.
 *
 * @param matcher The semantic criteria to evaluate against all ancestors up to the root.
 * @return a matcher validating the presence of the specified ancestor.
 */
public fun hasAnyAncestor(matcher: SubspaceSemanticsMatcher): SubspaceSemanticsMatcher {
    return SubspaceSemanticsMatcher("hasAnyAncestorThat(${matcher.description})") {
        matcher.matchesAny(it.ancestors)
    }
}

/**
 * Verifies whether any descendant of the 3D semantic node satisfies the given condition.
 *
 * Recursively descends the 3D semantic structure to locate deeply nested elements.
 *
 * Example usage:
 * ```
 * |-X
 * |-A
 *   |-B
 *     |-C1
 *     |-C2
 * ```
 *
 * In case of A, we would check the matcher against B, C1 and C2.
 *
 * @param matcher The semantic criteria for the expected descendant.
 * @return a matcher validating the presence of the specified descendant.
 */
public fun hasAnyDescendant(matcher: SubspaceSemanticsMatcher): SubspaceSemanticsMatcher {
    fun checkIfSubtreeMatches(
        matcher: SubspaceSemanticsMatcher,
        node: SubspaceSemanticsInfo,
    ): Boolean {
        if (matcher.matchesAny(node.childrenInfo)) {
            return true
        }

        return node.childrenInfo.fastAny { checkIfSubtreeMatches(matcher, it) }
    }

    return SubspaceSemanticsMatcher("hasAnyDescendantThat(${matcher.description})") {
        checkIfSubtreeMatches(matcher, it)
    }
}

private val SubspaceSemanticsInfo.ancestors: Iterable<SubspaceSemanticsInfo>
    get() =
        object : Iterable<SubspaceSemanticsInfo> {
            override fun iterator(): Iterator<SubspaceSemanticsInfo> {
                return object : Iterator<SubspaceSemanticsInfo> {
                    var next = parentInfo

                    override fun hasNext(): Boolean {
                        return next != null
                    }

                    override fun next(): SubspaceSemanticsInfo {
                        return next!!.also { next = it.parentInfo }
                    }
                }
            }
        }
