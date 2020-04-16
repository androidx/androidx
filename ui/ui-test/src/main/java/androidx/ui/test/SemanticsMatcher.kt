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

package androidx.ui.test

import androidx.ui.core.semantics.SemanticsNode
import androidx.ui.semantics.SemanticsPropertyKey

/**
 * Wrapper for semantics matcher lambdas that allows to build string explaining to the developer
 * what conditions were being tested.
 */
class SemanticsMatcher(
    val description: String,
    private val selector: (Iterable<SemanticsNode>) -> Iterable<SemanticsNode>
) {

    companion object {
        /**
         * Predicate that matches anything.
         */
        val any: SemanticsMatcher = SemanticsMatcher("Any") { nodes ->
            nodes
        }

        /**
         * Builds a predicate that tests whether the value of the given [key] is equal to
         * [expectedValue].
         */
        fun <T> expectValue(key: SemanticsPropertyKey<T>, expectedValue: T): SemanticsMatcher {
            return fromCondition("${key.name} = '$expectedValue'") {
                config.getOrElseNullable(key) { null } == expectedValue
            }
        }

        /**
         * Builds a predicate that tests whether the given [key] is defined in semantics.
         */
        fun <T> keyIsDefined(key: SemanticsPropertyKey<T>): SemanticsMatcher {
            return fromCondition("${key.name} is defined") {
                key in config
            }
        }

        /**
         * Builds a predicate that tests whether the given [key] is NOT defined in semantics.
         */
        fun <T> keyNotDefined(key: SemanticsPropertyKey<T>): SemanticsMatcher {
            return fromCondition("${key.name} is NOT defined") {
                key !in config
            }
        }

        /**
         * Creates a matcher that will match using a provided boolean selector.
         *
         * @param description Description of the condition being performed (will be displayed to the
         * developer when this matcher fails).
         * @param selector The filter lambda to use to build the matcher.
         */
        fun fromCondition(
            description: String,
            selector: SemanticsNode.() -> Boolean
        ): SemanticsMatcher {
            return SemanticsMatcher(description) { nodes ->
                nodes.filter { selector(it) }
            }
        }
    }

    /**
     * Returns whether the given node is matched by this matcher.
     */
    fun matches(node: SemanticsNode): Boolean {
        return selector(listOf(node)).count() == 1
    }

    /**
     * Returns whether at least one of the given nodes is matched by this matcher.
     */
    fun matchesAny(nodes: Iterable<SemanticsNode>): Boolean {
        return selector(nodes).count() >= 1
    }

    /**
     * From the given nodes, returns all the nodes that got matched by this matcher.
     */
    fun match(nodes: Iterable<SemanticsNode>): Iterable<SemanticsNode> {
        return selector(nodes)
    }

    infix fun and(other: SemanticsMatcher): SemanticsMatcher {
        val desc = "($description) && (${other.description})"
        return SemanticsMatcher(desc) { nodes ->
            selector(nodes).intersect(other.selector(nodes))
        }
    }

    infix fun or(other: SemanticsMatcher): SemanticsMatcher {
        val desc = "($description) || (${other.description})"
        return SemanticsMatcher(desc) { nodes ->
            selector(nodes).union(other.selector(nodes))
        }
    }

    operator fun not(): SemanticsMatcher {
        val desc = "NOT ($description)"
        return SemanticsMatcher(desc) { nodes ->
            nodes.subtract(selector(nodes))
        }
    }
}