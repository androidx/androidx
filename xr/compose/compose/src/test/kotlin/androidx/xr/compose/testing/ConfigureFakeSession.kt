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

package androidx.xr.compose.testing

import android.app.Activity
import androidx.annotation.RestrictTo
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.lifecycle.LifecycleOwner
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.compose.platform.contentView
import androidx.xr.runtime.Session
import androidx.xr.scenecore.runtime.RenderingEntityFactory
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider
import androidx.xr.scenecore.testing.FakeRenderingRuntime
import androidx.xr.scenecore.testing.FakeSceneRuntimeFactory
import com.android.extensions.xr.ShadowConfig
import com.google.errorprone.annotations.CanIgnoreReturnValue

private object SubspaceAndroidComposeTestRuleConstants {
    const val DEFAULT_DP_PER_METER = 1151.856f
}

/**
 * This can be called prior to to accessing any JXR APIs to capture the fake runtimes or wrap the
 * fake runtimes with your own behavior.
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
 * @param sceneRuntime possible wrapper factory for the [SceneRuntime].
 * @param renderingRuntime possible wrapper factory for the [RenderingRuntime]
 * @param perceptionRuntime possible wrapper factory from the [PerceptionRuntime]
 * @see androidx.xr.compose.subspace.SpatialGltfModelTest for an example
 */
@CanIgnoreReturnValue
fun AndroidComposeTestRule<*, *>.configureFakeSession(
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
 * This can be called prior to accessing any JXR APIs to capture the fake runtimes or wrap the fake
 * runtimes with your own behavior.
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
 * @param sceneRuntime possible wrapper factory for the [SceneRuntime].
 * @param renderingRuntime possible wrapper factory for the [RenderingRuntime]
 * @param perceptionRuntime possible wrapper factory from the [PerceptionRuntime]
 * @see androidx.xr.compose.subspace.SpatialGltfModelTest for an example
 */
@CanIgnoreReturnValue
fun Activity.configureFakeSession(
    sceneRuntime: (SceneRuntime) -> SceneRuntime = { it },
    renderingRuntime: (RenderingRuntime) -> RenderingRuntime = { it },
    perceptionRuntime: (PerceptionRuntime) -> PerceptionRuntime = { it },
    defaultDpPerMeter: Float = SubspaceAndroidComposeTestRuleConstants.DEFAULT_DP_PER_METER,
): Session {
    // TODO(b/447211302) Remove once direct dependency on XrExtensions in Compose XR is removed.
    ShadowConfig.extract(XrExtensionsProvider.getXrExtensions()!!.config!!)
        .setDefaultDpPerMeter(defaultDpPerMeter)

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
                        FakePerceptionRuntimeFactory().createRuntime(this).apply {
                            lifecycleManager.create()
                        }
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
public var Activity.session: Session?
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    get() = contentView.getTag(androidx.xr.compose.R.id.compose_xr_session) as? Session
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    set(value) {
        contentView.setTag(androidx.xr.compose.R.id.compose_xr_session, value)
    }
