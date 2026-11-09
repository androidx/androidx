/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.adaptive.core.slots

import androidx.annotation.RestrictTo

/**
 * Single slot or capability invariant evaluated against a built template instance [T].
 *
 * @param T Template type validated by this requirement.
 * @property description Human-readable description of what the template needs, included in the
 *   failure message when [predicate] returns `false`.
 * @property predicate Validation predicate returning `true` when [T] satisfies this requirement.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Requirement<T>(
    public val description: String,
    public val predicate: (T) -> Boolean,
)

/**
 * Composite validation rule checking all template-level slot requirements once a template DSL block
 * completes.
 *
 * Evaluates every [Requirement] and throws a single [IllegalArgumentException] listing all unmet
 * requirements together so callers can fix missing slots in one pass.
 *
 * @param T Template type validated by this rule.
 * @property templateName Display name of the template used in error messages.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TemplateRule<T>(
    public val templateName: String,
    private vararg val requirements: Requirement<T>,
) {
    /**
     * Validates [template] against all configured requirements.
     *
     * @throws IllegalArgumentException if one or more requirements are not met.
     */
    public fun check(template: T) {
        require(requirements.all { it.predicate(template) }) {
            val unmet = requirements.filterNot { it.predicate(template) }
            "$templateName needs ${unmet.joinToString("; ") { it.description }}."
        }
    }
}
