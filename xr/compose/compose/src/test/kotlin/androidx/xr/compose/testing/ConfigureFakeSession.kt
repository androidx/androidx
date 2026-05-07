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

@file:Suppress("DEPRECATION")

package androidx.xr.compose.testing

import android.app.Activity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.lifecycle.LifecycleOwner
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.compose.platform.contentView
import androidx.xr.runtime.Session
import androidx.xr.scenecore.runtime.RenderingEntityFactory
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.testing.FakeRenderingRuntime
import androidx.xr.scenecore.testing.FakeSceneRuntimeFactory
import com.google.errorprone.annotations.CanIgnoreReturnValue

private object SubspaceAndroidComposeTestRuleConstants {
    const val DEFAULT_DP_PER_METER = 2000f
}

/**
 * Configures and overrides the fake [Session] prior to accessing any internal JXR APIs or starting
 * spatial compositions.
 *
 * This function intercepts and wraps the underlying fake runtimes ([SceneRuntime],
 * [RenderingRuntime], and [PerceptionRuntime]), allowing tests to spy on, mock, or modify
 * individual entities and runtime operations.
 *
 * ```
 * composeTestRule.configureFakeSession(
 *   sceneRuntime = {
 *     object : SceneRuntime by it {
 *       override fun createPanelEntity(...) {
 *         // Here you can see what arguments are passed or wrap the entity itself
 *         val basePanel = it.createPanelEntity(...)
 *         return object : SpatialPanel by basePanel {
 *           override fun setAlpha(...) { ... }
 *         }
 *       }
 *     }
 *   }
 * )
 *
 * composeTestRule.setContent { ... }
 * ```
 *
 * @param sceneRuntime A factory lambda that receives the original fake [SceneRuntime] and returns a
 *   custom or wrapped implementation. Defaults to returning the original instance unmodified.
 * @param renderingRuntime A factory lambda that receives the original fake [RenderingRuntime] and
 *   returns a custom or wrapped implementation. Defaults to returning the original instance
 *   unmodified.
 * @param perceptionRuntime A factory lambda that receives the original fake [PerceptionRuntime] and
 *   returns a custom or wrapped implementation. Defaults to returning the original instance
 *   unmodified.
 * @param defaultDpPerMeter The baseline scaling factor mapping density-independent pixels (dp) to
 *   meters within the fake spatial environment. Must be a positive, non-zero float. Defaults to
 *   [SubspaceAndroidComposeTestRuleConstants.DEFAULT_DP_PER_METER].
 * @return The configured fake [Session] instance, fully attached to the underlying activity.
 * @receiver The [AndroidComposeTestRule] used to orchestrate the current test and attach the fake
 *   session.
 * @see androidx.xr.compose.subspace.SpatialGltfModelTest
 */
@CanIgnoreReturnValue
internal fun AndroidComposeTestRule<*, *>.configureFakeSession(
    sceneRuntime: (SceneRuntime) -> SceneRuntime = { it },
    renderingRuntime: (RenderingRuntime) -> RenderingRuntime = { it },
    perceptionRuntime: (PerceptionRuntime) -> PerceptionRuntime = { it },
    defaultDpPerMeter: Float = SubspaceAndroidComposeTestRuleConstants.DEFAULT_DP_PER_METER,
): Session =
    activity.configureFakeSession(
        sceneRuntime,
        renderingRuntime,
        perceptionRuntime,
        defaultDpPerMeter,
    )

/**
 * Configures and overrides the fake [Session] directly on the underlying [Activity].
 *
 * This function acts as the lower-level entry point to intercept and wrap the underlying fake
 * runtimes ([SceneRuntime], [RenderingRuntime], and [PerceptionRuntime]), allowing tests to spy on,
 * mock, or modify individual entities and runtime operations before spatial features are activated.
 *
 * ```
 * activity.configureFakeSession(
 *   sceneRuntime = {
 *     object : SceneRuntime by it {
 *       override fun createPanelEntity(...) {
 *         // Here you can see what arguments are passed or wrap the entity itself
 *         val basePanel = it.createPanelEntity(...)
 *         return object : SpatialPanel by basePanel {
 *           override fun setAlpha(...) { ... }
 *         }
 *       }
 *     }
 *   }
 * )
 *
 * composeTestRule.setContent { ... }
 * ```
 *
 * @param sceneRuntime A factory lambda that receives the original fake [SceneRuntime] and returns a
 *   custom or wrapped implementation. Defaults to returning the original instance unmodified.
 * @param renderingRuntime A factory lambda that receives the original fake [RenderingRuntime] and
 *   returns a custom or wrapped implementation. Defaults to returning the original instance
 *   unmodified.
 * @param perceptionRuntime A factory lambda that receives the original fake [PerceptionRuntime] and
 *   returns a custom or wrapped implementation. Defaults to returning the original instance
 *   unmodified.
 * @param defaultDpPerMeter The baseline scaling factor mapping density-independent pixels (dp) to
 *   meters. Must be greater than zero. Defaults to
 *   [SubspaceAndroidComposeTestRuleConstants.DEFAULT_DP_PER_METER].
 * @return The configured fake [Session] instance, securely cached and linked to the activity.
 * @receiver The hosting [Activity] that manages the lifecycle of the spatial runtime session.
 * @see androidx.xr.compose.subspace.SpatialGltfModelTest
 */
@CanIgnoreReturnValue
@Suppress("DEPRECATION")
@SuppressWarnings("RestrictTo")
// TODO: b/494305963 Remove references to arcore-testing Fakes
internal fun Activity.configureFakeSession(
    sceneRuntime: (SceneRuntime) -> SceneRuntime = { it },
    renderingRuntime: (RenderingRuntime) -> RenderingRuntime = { it },
    perceptionRuntime: (PerceptionRuntime) -> PerceptionRuntime = { it },
    defaultDpPerMeter: Float = SubspaceAndroidComposeTestRuleConstants.DEFAULT_DP_PER_METER,
): Session {
    val originalSceneRuntime =
        FakeSceneRuntimeFactory().create(this).apply { deviceDpPerMeter = defaultDpPerMeter }
    val wrappedSceneRuntime = sceneRuntime(originalSceneRuntime)
    val sceneRuntime: SceneRuntime =
        if (wrappedSceneRuntime is RenderingEntityFactory) {
            wrappedSceneRuntime
        } else {
            object :
                SceneRuntime by wrappedSceneRuntime,
                RenderingEntityFactory by originalSceneRuntime {}
        }

    return Session(
            this,
            runtimes =
                listOf(
                    sceneRuntime,
                    renderingRuntime(FakeRenderingRuntime(sceneRuntime)),
                    perceptionRuntime(
                        androidx.xr.arcore.testing
                            .FakePerceptionRuntimeFactory()
                            .createRuntime(this)
                    ),
                ),
            lifecycleOwner = this as LifecycleOwner,
        )
        .also { session = it }
}

/**
 * The XR [Session] for the current [Activity].
 *
 * This will be null until the value is set or `LocalSession.current` is accessed in compose, after
 * which the value will be non-null and return the current [Session]. Setting this value after
 * calling `setContent` will not change the Session that is used for that content block. Setting the
 * value to null will indicate that the default test Session should be used.
 */
private var Activity.session: Session?
    get() = contentView.getTag(androidx.xr.compose.R.id.compose_xr_session) as? Session
    set(value) {
        contentView.setTag(androidx.xr.compose.R.id.compose_xr_session, value)
    }
