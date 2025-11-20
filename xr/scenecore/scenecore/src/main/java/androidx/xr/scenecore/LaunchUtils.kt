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

@file:JvmName("LaunchUtils")

package androidx.xr.scenecore

import android.app.Activity
import android.os.Bundle
import androidx.xr.runtime.Session

/**
 * Returns a [Bundle] with the necessary entries to request that a new [Activity] be launched
 * directly into Full Space Mode.
 *
 * Use [Activity.startActivity] with the returned bundle to launch an Activity directly into Full
 * Space Mode. Use bundles returned by [android.app.ActivityOptions.toBundle] or
 * [androidx.core.app.ActivityOptionsCompat.toBundle] as arguments to this function to preserve the
 * existing options.
 *
 * The provided bundle must have the [android.content.Intent.FLAG_ACTIVITY_NEW_TASK] set.
 *
 * The launch request will not be honored if it is not started from a focused Activity context or if
 * the [androidx.xr.runtime.manifest.PROPERTY_XR_ACTIVITY_START_MODE] manifest property is set to a
 * value other than [androidx.xr.runtime.manifest.XR_ACTIVITY_START_MODE_UNDEFINED] for the activity
 * being launched.
 *
 * @param session the session from which to access the XR runtime resources.
 * @param bundle the input bundle to copy its values from.
 * @return a new bundle with values from the input bundle and the Full Space Mode launch
 *   configuration set.
 */
public fun createBundleForFullSpaceModeLaunch(session: Session, bundle: Bundle): Bundle =
    session.sceneRuntime.setFullSpaceMode(bundle)

/**
 * Returns a [Bundle] with the necessary entries to request that a new [Activity] be launched
 * directly into Full Space Mode while inheriting the environment from the launching activity.
 *
 * Use [Activity.startActivity] with the returned bundle to launch an Activity directly into Full
 * Space Mode while inheriting the existing environment. Use bundles returned by
 * [android.app.ActivityOptions.toBundle] or [androidx.core.app.ActivityOptionsCompat.toBundle] as
 * arguments to this function to preserve the existing options.
 *
 * When launched, the Activity will be in Full Space Mode and will inherit the environment from the
 * launching activity. If the inherited environment needs to be animated, the launching activity has
 * to continue updating the environment even after the activity is put into the stopped state.
 *
 * The provided bundle must have the [android.content.Intent.FLAG_ACTIVITY_NEW_TASK] set.
 *
 * The launch request will not be honored if it is not started from a focused Activity context or if
 * the [androidx.xr.runtime.manifest.PROPERTY_XR_ACTIVITY_START_MODE] manifest property is set to a
 * value other than [androidx.xr.runtime.manifest.XR_ACTIVITY_START_MODE_UNDEFINED] for the activity
 * being launched.
 *
 * For security reasons, [Z-testing](https://en.wikipedia.org/wiki/Z-order) for the new activity is
 * disabled, and the activity is always drawn on top of the inherited environment. Because Z-testing
 * is disabled, the activity should not spatialize itself.
 *
 * @param session the session from which to access the XR runtime resources.
 * @param bundle the input bundle to copy its values from.
 * @return a new bundle with values from the input bundle and the Full Space Mode with Environment
 *   Inheritance launch configuration set.
 */
public fun createBundleForFullSpaceModeLaunchWithEnvironmentInherited(
    session: Session,
    bundle: Bundle,
): Bundle = session.sceneRuntime.setFullSpaceModeWithEnvironmentInherited(bundle)
