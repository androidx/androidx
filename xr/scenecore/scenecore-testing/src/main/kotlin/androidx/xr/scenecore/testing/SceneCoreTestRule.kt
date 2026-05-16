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

package androidx.xr.scenecore.testing

import androidx.xr.scenecore.Component
import androidx.xr.scenecore.Entity
import org.junit.rules.ExternalResource

/**
 * A JUnit Rule for establishing a test environment for SceneCore applications.
 *
 * This rule provides fake implementations and test accessors for SceneCore components, allowing you
 * to configure and verify the internal runtime states of SceneCore's public API objects like
 * [Entity] and [Component].
 *
 * To enable the test rule to manage the lifecycle and state of SceneCore objects, you must register
 * the created instance with the rule after its creation using the [createTester] method provided by
 * this rule. These methods return test-specific accessors that allow inspection and manipulation of
 * the underlying fake instance.
 *
 * Example usage:
 * ```kotlin
 * @Rule @JvmField val testRule = SceneCoreTestRule()
 *
 * // Inside a test method:
 * // Create a SceneCore Entity
 * val entity = PanelEntity.create(...)
 * // Register it with the rule and get the test accessor
 * val entityTester = testRule.createTester<PanelEntityTester>(entity)
 * // Use entityTester to interact with the entity's state
 *
 * // Create a SceneCore Component
 * val component = MovableComponent.create(...)
 * // Register it and get the test accessor
 * val componentTester = testRule.createTester<MovableComponentTester>(component)
 * // Use componentTester to interact with the component's state
 * ```
 */
public class SceneCoreTestRule : ExternalResource() {

    // TODO: b/512223711 - Add testers for specific entities/components in follow-up PRs.
    @PublishedApi
    internal fun resolveTesterInternal(entity: Entity): Any? {
        return if (entity::class == Entity::class) {
            EntityTester.create(entity)
        } else {
            when (entity) {
                else -> null
            }
        }
    }

    // TODO: b/512223711 - Add testers for specific entities/components in follow-up PRs.
    @PublishedApi
    internal fun resolveTesterInternal(component: Component): Any? {
        return when (component) {
            else -> null
        }
    }

    /**
     * Creates a specific test data accessor for the given [Entity].
     *
     * In the test environment, entities have corresponding underlying fake data. This function
     * provides a convenient, type-safe way to access the specific tester associated with an
     * entity's subclass, allowing for verification or manipulation in tests.
     *
     * Example usage:
     * ```
     * val entityTester = rule.createTester<PanelEntityTester>(myPanelEntity)
     * ```
     *
     * @param T The expected type of the entity tester (e.g., `PanelEntityTester`).
     * @param entity The [Entity] instance for which to retrieve the test data accessor.
     * @return A tester instance of type [T].
     * @throws IllegalArgumentException If the underlying tester created for the entity does not
     *   match the expected type [T].
     */
    public inline fun <reified T : Any> createTester(entity: Entity): T {
        val tester =
            resolveTesterInternal(entity)
                ?: throw IllegalArgumentException(
                    "Unsupported entity type: ${entity::class.simpleName}"
                )

        return tester as? T
            ?: throw IllegalArgumentException(
                "Expected tester of type ${T::class.simpleName}, but actual entity created a ${tester::class.simpleName}"
            )
    }

    /**
     * Creates a specific test data accessor for the given [Component].
     *
     * In the test environment, components have corresponding underlying fake data. This function
     * provides a convenient, type-safe way to access the specific tester associated with a
     * component's subclass, allowing for verification or manipulation in tests.
     *
     * Example usage:
     * ```
     * val componentTester = rule.createTester<BoundsComponentTester>(myBoundsComponent)
     * ```
     *
     * @param T The expected type of the component tester (e.g., `BoundsComponentTester`).
     * @param component The [Component] instance for which to retrieve the test data accessor.
     * @return A tester instance of type [T].
     * @throws IllegalArgumentException If the underlying tester created for the component does not
     *   match the expected type [T], or if no tester is found.
     */
    public inline fun <reified T : Any> createTester(component: Component): T {
        val tester =
            resolveTesterInternal(component)
                ?: throw IllegalArgumentException(
                    "Unsupported component type: ${component::class.simpleName}"
                )

        return tester as? T
            ?: throw IllegalArgumentException(
                "Expected tester of type ${T::class.simpleName}, but actual component created a ${tester::class.simpleName}"
            )
    }
}
