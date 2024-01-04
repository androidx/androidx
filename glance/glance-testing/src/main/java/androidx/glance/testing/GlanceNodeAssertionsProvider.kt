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
 * Provides an entry point into testing exposing methods to find glance nodes
 */
// Equivalent to "androidx.compose.ui.test.SemanticsNodeInteractionsProvider" from compose.
interface GlanceNodeAssertionsProvider<R, T : GlanceNode<R>> {
    /**
     * Finds a Glance node that matches the given condition.
     *
     * Any subsequent operation on its result will expect exactly one element found and will throw
     * [AssertionError] if none or more than one element is found.
     *
     * @param matcher Matcher used for filtering
     */
    fun onNode(matcher: GlanceNodeMatcher<R>): GlanceNodeAssertion<R, T>

    /**
     * Finds all Glance nodes that matches the given condition.
     *
     * @param matcher Matcher used for filtering
     */
    fun onAllNodes(matcher: GlanceNodeMatcher<R>): GlanceNodeAssertionCollection<R, T>
}
