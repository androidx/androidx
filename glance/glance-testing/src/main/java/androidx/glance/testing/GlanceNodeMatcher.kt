/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.glance.testing

/**
 * Wrapper for matcher lambdas and related details
 *
 * @param description a string explaining to the developer what conditions were being tested.
 * @param matcher a lambda performing the actual logic of matching on the GlanceNode
 */
class GlanceNodeMatcher<R>(
    internal val description: String,
    private val matcher: (GlanceNode<R>) -> Boolean
) {
    /**
     * Returns whether the given node is matched by this matcher.
     */
    fun matches(node: GlanceNode<R>): Boolean {
        return matcher(node)
    }

    /**
     * Returns whether at least one of the given nodes is matched by this matcher.
     */
    fun matchesAny(nodes: Iterable<GlanceNode<R>>): Boolean {
        return nodes.any(matcher)
    }

    /**
     * Returns whether the given node is matched by this and the [other] matcher.
     *
     * @param other matcher that should also match in addition to current matcher
     */
    infix fun and(other: GlanceNodeMatcher<R>): GlanceNodeMatcher<R> {
        return GlanceNodeMatcher("($description) && (${other.description})") {
            matcher(it) && other.matches(it)
        }
    }

    /**
     * Returns whether the given node is matched by this or the [other] matcher.
     *
     * @param other matcher that can be tested to match if the current matcher doesn't.
     */
    infix fun or(other: GlanceNodeMatcher<R>): GlanceNodeMatcher<R> {
        return GlanceNodeMatcher("($description) || (${other.description})") {
            matcher(it) || other.matches(it)
        }
    }

    /**
     * Returns whether the given node does not match the matcher.
     */
    operator fun not(): GlanceNodeMatcher<R> {
        return GlanceNodeMatcher(("NOT ($description)")) {
            !matcher(it)
        }
    }
}
