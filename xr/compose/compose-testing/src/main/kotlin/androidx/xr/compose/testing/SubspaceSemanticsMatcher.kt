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
 * @sample androidx.xr.compose.testing.samples.advancedSubspaceSemanticsMatchers
 */
public class SubspaceSemanticsMatcher(
    public val description: String,
    private val matcher: (SubspaceSemanticsInfo) -> Boolean,
) {

    public companion object {
        /**
         * Builds a predicate that tests whether the value of the given [key] is equal to
         * [expectedValue].
         *
         * **Nullability & Edge Cases:** If the [key] is not present in a node's semantics
         * configuration, the comparison evaluates whether `null == expectedValue`. Therefore,
         * passing `null` as the [expectedValue] successfully matches nodes where the key is
         * entirely omitted, as well as nodes where the key is present but has a value of `null`.
         *
         * @param key The [SemanticsPropertyKey] to look up in the node's configuration.
         * @param expectedValue The expected property value to verify against.
         * @return A [SubspaceSemanticsMatcher] for the specified key-value condition.
         * @sample androidx.xr.compose.testing.samples.advancedSubspaceSemanticsMatchers
         */
        public fun <T> expectValue(
            key: SemanticsPropertyKey<T>,
            expectedValue: T,
        ): SubspaceSemanticsMatcher {
            return SubspaceSemanticsMatcher("${key.name} = '$expectedValue'") {
                it.semanticsConfiguration?.getOrElseNullable(key) { null } == expectedValue
            }
        }

        /**
         * Builds a predicate that tests whether the given [key] is defined in semantics.
         *
         * **Edge Cases:** This check simply verifies the pre-existence of the [key] mapping in the
         * node's configuration regardless of its underlying assigned value.
         *
         * @param key The [SemanticsPropertyKey] to inspect.
         * @return A [SubspaceSemanticsMatcher] verifying the presence of the key.
         * @sample androidx.xr.compose.testing.samples.advancedSubspaceSemanticsMatchers
         */
        public fun <T> keyIsDefined(key: SemanticsPropertyKey<T>): SubspaceSemanticsMatcher {
            return SubspaceSemanticsMatcher("${key.name} is defined") {
                it.semanticsConfiguration?.contains(key) ?: false
            }
        }

        /**
         * Builds a predicate that tests whether the given [key] is NOT defined in semantics.
         *
         * **Edge Cases:** This check validates that the [key] mapping is completely absent from the
         * node's configuration.
         *
         * @param key The [SemanticsPropertyKey] to inspect.
         * @return A [SubspaceSemanticsMatcher] verifying the absence of the key.
         * @sample androidx.xr.compose.testing.samples.advancedSubspaceSemanticsMatchers
         */
        public fun <T> keyNotDefined(key: SemanticsPropertyKey<T>): SubspaceSemanticsMatcher {
            return SubspaceSemanticsMatcher("${key.name} is NOT defined") {
                it.semanticsConfiguration?.let { key !in it } ?: true
            }
        }
    }

    /**
     * Returns whether the given [node] is matched by this matcher.
     *
     * @param node The target [SubspaceSemanticsInfo] to evaluate.
     * @return `true` if the [node] satisfies the predicate; `false` otherwise.
     */
    public fun matches(node: SubspaceSemanticsInfo): Boolean {
        return matcher(node)
    }

    /**
     * Returns whether at least one of the given [nodes] is matched by this matcher.
     *
     * **Edge Cases:** If the provided [nodes] iterable is empty, this evaluation returns `false`.
     *
     * @param nodes An iterable collection of [SubspaceSemanticsInfo] instances to evaluate.
     * @return `true` if at least one node satisfies the predicate; `false` otherwise.
     */
    public fun matchesAny(nodes: Iterable<SubspaceSemanticsInfo>): Boolean {
        return nodes.any(matcher)
    }

    /**
     * Combines this matcher with [other] using a short-circuiting logical AND.
     *
     * The resulting matcher evaluates to `true` only if both this and the [other] matcher succeed
     * on a given node.
     *
     * @param other The second [SubspaceSemanticsMatcher] to satisfy.
     * @return A combined [SubspaceSemanticsMatcher].
     * @sample androidx.xr.compose.testing.samples.advancedSubspaceSemanticsMatchers
     */
    public infix fun and(other: SubspaceSemanticsMatcher): SubspaceSemanticsMatcher {
        return SubspaceSemanticsMatcher("($description) && (${other.description})") {
            matcher(it) && other.matches(it)
        }
    }

    /**
     * Combines this matcher with [other] using a short-circuiting logical OR.
     *
     * The resulting matcher evaluates to `true` if either this or the [other] matcher succeeds on a
     * given node.
     *
     * @param other The alternative [SubspaceSemanticsMatcher] to satisfy.
     * @return A combined [SubspaceSemanticsMatcher].
     * @sample androidx.xr.compose.testing.samples.advancedSubspaceSemanticsMatchers
     */
    public infix fun or(other: SubspaceSemanticsMatcher): SubspaceSemanticsMatcher {
        return SubspaceSemanticsMatcher("($description) || (${other.description})") {
            matcher(it) || other.matches(it)
        }
    }

    /**
     * Inverts the evaluation logic of this matcher using a logical NOT.
     *
     * Evaluates to `true` if the underlying matcher evaluates to `false`.
     *
     * @return A negated [SubspaceSemanticsMatcher].
     * @sample androidx.xr.compose.testing.samples.advancedSubspaceSemanticsMatchers
     */
    public operator fun not(): SubspaceSemanticsMatcher {
        return SubspaceSemanticsMatcher("NOT ($description)") { !matcher(it) }
    }
}
