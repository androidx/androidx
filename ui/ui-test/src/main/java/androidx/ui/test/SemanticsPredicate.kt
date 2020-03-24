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
 * Wrapper for semantics predicate that allows to build string explaining to the developer what
 * conditions were being performed.
 */
class SemanticsPredicate(
    val description: String,
    val condition: SemanticsNode.() -> Boolean
) {
    companion object {
        /**
         * Predicate that matches anything.
         */
        val any: SemanticsPredicate = SemanticsPredicate("Any") { true }

        /**
         * Builds a predicate that tests whether the value of the given [key] is equal to
         * [expectedValue].
         */
        fun <T> expectValue(key: SemanticsPropertyKey<T>, expectedValue: T): SemanticsPredicate {
            return SemanticsPredicate("${key.name} = '$expectedValue'") {
                config.getOrElseNullable(key) { null } == expectedValue
            }
        }

        /**
         * Builds a predicate that tests whether the given [key] is defined in semantics.
         */
        fun <T> keyIsDefined(key: SemanticsPropertyKey<T>): SemanticsPredicate {
            return SemanticsPredicate("${key.name} is defined") {
                key in this.config
            }
        }

        /**
         * Builds a predicate that tests whether the given [key] is NOT defined in semantics.
         */
        fun <T> keyNotDefined(key: SemanticsPropertyKey<T>): SemanticsPredicate {
            return SemanticsPredicate("${key.name} is NOT defined") {
                key !in this.config
            }
        }
    }

    infix fun and(other: SemanticsPredicate): SemanticsPredicate {
        val desc = "($description) && (${other.description})"
        return SemanticsPredicate(desc) {
            condition(this) && other.condition(this)
        }
    }
    infix fun or(other: SemanticsPredicate): SemanticsPredicate {
        val desc = "($description) || (${other.description})"
        return SemanticsPredicate(desc) {
            condition(this) || other.condition(this)
        }
    }
    operator fun not(): SemanticsPredicate {
        val desc = "NOT ($description)"
        return SemanticsPredicate(desc) { !condition(this) }
    }
}