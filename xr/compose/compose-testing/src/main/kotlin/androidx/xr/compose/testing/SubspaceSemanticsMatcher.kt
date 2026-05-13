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

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.xr.compose.subspace.node.SubspaceSemanticsInfo

/**
 * Wrapper for semantics matcher lambdas that allows building a description string explaining to the
 * developer what conditions were being tested.
 *
 * This class encapsulates a predicate that evaluates a [SubspaceSemanticsInfo] node to verify
 * whether it matches the expected semantic properties. It is primarily used by the testing
 * framework to locate nodes within a Subspace hierarchy.
 *
 * @param description A human-readable explanation of the condition being tested, used in test
 *   failure messages.
 * @param matcher The predicate function that evaluates a given [SubspaceSemanticsInfo].
 * @sample androidx.xr.compose.testing.samples.subspacePanelRenderedAndInteractive
 * @sample androidx.xr.compose.testing.samples.subspaceNodeMatcherProperties
 */
public class SubspaceSemanticsMatcher(
    internal val description: String,
    private val matcher: (SubspaceSemanticsInfo) -> Boolean,
) {

    internal companion object {
        /**
         * Builds a predicate that tests whether the value of the given [key] is equal to
         * [expectedValue].
         */
        internal fun <T> expectValue(
            key: SemanticsPropertyKey<T>,
            expectedValue: T,
        ): SubspaceSemanticsMatcher {
            return SubspaceSemanticsMatcher("${key.name} = '$expectedValue'") {
                it.semanticsConfiguration.getOrElseNullable(key) { null } == expectedValue
            }
        }

        /** Builds a predicate that tests whether the given [key] is defined in semantics. */
        internal fun <T> keyIsDefined(key: SemanticsPropertyKey<T>): SubspaceSemanticsMatcher {
            return SubspaceSemanticsMatcher("${key.name} is defined") {
                key in it.semanticsConfiguration
            }
        }

        /** Builds a predicate that tests whether the given [key] is NOT defined in semantics. */
        internal fun <T> keyNotDefined(key: SemanticsPropertyKey<T>): SubspaceSemanticsMatcher {
            return SubspaceSemanticsMatcher("${key.name} is NOT defined") {
                key !in it.semanticsConfiguration
            }
        }
    }

    /** Returns whether the given node is matched by this matcher. */
    internal fun matches(node: SubspaceSemanticsInfo): Boolean {
        return matcher(node)
    }

    /** Returns whether at least one of the given nodes is matched by this matcher. */
    internal fun matchesAny(nodes: Iterable<SubspaceSemanticsInfo>): Boolean {
        return nodes.any(matcher)
    }

    internal infix fun and(other: SubspaceSemanticsMatcher): SubspaceSemanticsMatcher {
        return SubspaceSemanticsMatcher("($description) && (${other.description})") {
            matcher(it) && other.matches(it)
        }
    }

    internal infix fun or(other: SubspaceSemanticsMatcher): SubspaceSemanticsMatcher {
        return SubspaceSemanticsMatcher("($description) || (${other.description})") {
            matcher(it) || other.matches(it)
        }
    }

    internal operator fun not(): SubspaceSemanticsMatcher {
        return SubspaceSemanticsMatcher("NOT ($description)") { !matcher(it) }
    }
}
