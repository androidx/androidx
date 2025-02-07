/*
 * Copyright 2024 The Android Open Source Project
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

@file:JvmName("SessionExt")
@file:Suppress("BanConcurrentHashMap")

package androidx.xr.scenecore

import android.app.Activity
import android.os.Bundle
import androidx.xr.scenecore.JxrPlatformAdapter.Entity as RtEntity
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialCapabilities as RtSpatialCapabilities
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executor
import java.util.function.Consumer

private val spatialCapabilitiesListeners:
    ConcurrentMap<Consumer<SpatialCapabilities>, Consumer<RtSpatialCapabilities>> =
    ConcurrentHashMap()

/**
 * Returns the current [SpatialCapabilities] of the Session. The set of capabilities can change
 * within a session. The returned object will not update if the capabilities change; this method
 * should be called again to get the latest set of capabilities.
 */
public fun Session.getSpatialCapabilities(): SpatialCapabilities =
    this.platformAdapter.spatialCapabilities.toSpatialCapabilities()

/**
 * Adds the given [Consumer] as a listener to be invoked when this Session's current
 * [SpatialCapabilities] change. [Consumer#accept(SpatialCapabilities)] will be invoked on the main
 * thread.
 */
public fun Session.addSpatialCapabilitiesChangedListener(
    listener: Consumer<SpatialCapabilities>
): Unit = addSpatialCapabilitiesChangedListener(HandlerExecutor.mainThreadExecutor, listener)

/**
 * Adds the given [Consumer] as a listener to be invoked when this Session's current
 * [SpatialCapabilities] change. [Consumer#accept(SpatialCapabilities)] will be invoked on the given
 * callbackExecutor, or the main thread if the callbackExecutor is null (default).
 */
public fun Session.addSpatialCapabilitiesChangedListener(
    callbackExecutor: Executor,
    listener: Consumer<SpatialCapabilities>,
): Unit {
    // wrap the client's listener in a callback that receives & converts the platformAdapter
    // SpatialCapabilities type.
    val rtListener: Consumer<RtSpatialCapabilities> =
        Consumer<RtSpatialCapabilities> { rtCaps: RtSpatialCapabilities ->
            listener.accept(rtCaps.toSpatialCapabilities())
        }
    spatialCapabilitiesListeners.compute(
        listener,
        { _, _ ->
            platformAdapter.addSpatialCapabilitiesChangedListener(callbackExecutor, rtListener)
            rtListener
        },
    )
}

/**
 * Releases the given [Consumer] from receiving updates when the Session's [SpatialCapabilities]
 * change.
 */
@Suppress("PairedRegistration") // The corresponding remove method does not accept an Executor
public fun Session.removeSpatialCapabilitiesChangedListener(
    listener: Consumer<SpatialCapabilities>
): Unit {
    spatialCapabilitiesListeners.computeIfPresent(
        listener,
        { _, rtListener ->
            platformAdapter.removeSpatialCapabilitiesChangedListener(rtListener)
            null
        },
    )
}

/**
 * Sets the full space mode flag to the given [android.os.Bundle].
 *
 * The [android.os.Bundle] then could be used to launch an [android.app.Activity] with requesting to
 * enter full space mode through [android.app.Activity.startActivity]. If there's a bundle used for
 * customizing how the [android.app.Activity] should be started by
 * [android.app.ActivityOptions.toBundle] or [androidx.core.app.ActivityOptionsCompat.toBundle],
 * it's suggested to use the bundle to call this method.
 *
 * The flag will be ignored when no [android.content.Intent.FLAG_ACTIVITY_NEW_TASK] is set in the
 * bundle, or it is not started from a focused Activity context.
 *
 * This flag is also ignored when the [android.window.PROPERTY_XR_ACTIVITY_START_MODE] property is
 * set to a value other than [XR_ACTIVITY_START_MODE_UNDEFINED] in the AndroidManifest.xml file for
 * the activity being launched.
 *
 * @param bundle the input bundle to set with the full space mode flag.
 * @return the input bundle with the full space mode flag set.
 */
public fun Session.setFullSpaceMode(bundle: Bundle): Bundle =
    platformAdapter.setFullSpaceMode(bundle)

/**
 * Sets the inherit full space mode environvment flag to the given [android.os.Bundle].
 *
 * The [android.os.Bundle] then could be used to launch an [android.app.Activity] with requesting to
 * enter full space mode while inherit the existing environment through
 * [android.app.Activity.startActivity]. If there's a bundle used for customizing how the
 * [android.app.Activity] should be started by [android.app.ActivityOptions.toBundle] or
 * [androidx.core.app.ActivityOptionsCompat.toBundle], it's suggested to use the bundle to call this
 * method.
 *
 * When launched, the activity will be in full space mode and also inherits the environment from the
 * launching activity. If the inherited environment needs to be animated, the launching activity has
 * to continue updating the environment even after the activity is put into the stopped state.
 *
 * The flag will be ignored when no [android.content.Intent.FLAG_ACTIVITY_NEW_TASK] is set in the
 * intent, or it is not started from a focused Activity context.
 *
 * The flag will also be ignored when there is no environment to inherit or the activity has its own
 * environment set already.
 *
 * This flag is ignored too when the [android.window.PROPERTY_XR_ACTIVITY_START_MODE] property is
 * set to a value other than [XR_ACTIVITY_START_MODE_UNDEFINED] in the AndroidManifest.xml file for
 * the activity being launched.
 *
 * For security reasons, Z testing for the new activity is disabled, and the activity is always
 * drawn on top of the inherited environment. Because Z testing is disabled, the activity should not
 * spatialize itself.
 *
 * @param bundle the input bundle to set with the inherit full space mode environment flag.
 * @return the input bundle with the inherit full space mode flag set.
 */
public fun Session.setFullSpaceModeWithEnvironmentInherited(bundle: Bundle): Bundle =
    platformAdapter.setFullSpaceModeWithEnvironmentInherited(bundle)

/**
 * Sets a preferred main panel aspect ratio for home space mode.
 *
 * The ratio is only applied to the activity. If the activity launches another activity in the same
 * task, the ratio is not applied to the new activity. Also, while the activity is in full space
 * mode, the preference is temporarily removed.
 *
 * If the activity's current aspect ratio differs from the preferredRatio, the panel is
 * automatically resized. This resizing preserves the panel's area. To avoid runtime resizing,
 * consider specifying the desired aspect ratio in your AndroidManifest.xml. This ensures your
 * activity launches with the preferred aspect ratio from the start.
 *
 * @param activity the activity to set the preference.
 * @param preferredRatio the aspect ratio determined by taking the panel's width over its height. A
 *   value <= 0.0f means there are no preferences.
 */
public fun Session.setPreferredAspectRatio(activity: Activity, preferredRatio: Float): Unit =
    platformAdapter.setPreferredAspectRatio(activity, preferredRatio)

/**
 * Returns all [Entity]s of the given type or its subtypes.
 *
 * @param type the type of [Entity] to return.
 * @return a list of all [Entity]s of the given type.
 */
public fun <T : Entity> Session.getEntitiesOfType(type: Class<out T>): List<T> =
    entityManager.getEntitiesOfType(type)

internal fun Session.getEntityForRtEntity(entity: RtEntity): Entity? {
    return entityManager.getEntityForRtEntity(entity)
}
