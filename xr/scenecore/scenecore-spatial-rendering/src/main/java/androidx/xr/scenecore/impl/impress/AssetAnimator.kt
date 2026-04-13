/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore.impl.impress

/**
 * Interface defining the callbacks that are triggered from the Native side when an asset is
 * animated.
 *
 * @UsedByNative("impress/apibindings/asset_animator.cc")
 *
 * TODO(b/440328311): Convert to real annotation to enable code minimization in the rest of
 *   SceneCore.
 */
internal interface AssetAnimator {
    /** Called when the asset is done animating. */
    public fun onComplete()

    /** Called when the asset fails to be animated. */
    public fun onFailure(message: String)

    /** Called when the animation is cancelled, such as during shutdown. */
    public fun onCancelled(message: String)
}
