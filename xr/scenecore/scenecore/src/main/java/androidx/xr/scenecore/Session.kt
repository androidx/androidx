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

@file:Suppress("BanConcurrentHashMap", "Deprecation")

package androidx.xr.scenecore

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.util.Log
import androidx.xr.scenecore.impl.JxrPlatformAdapterAxr
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * The Session provides the primary interface to SceneCore functionality for the application. Each
 * spatialized Activity must create and hold an instance of Session.
 *
 * Once created, the application can use the Session interfaces to create spatialized entities, such
 * as Widget panels and geometric models, set the background environment, and anchor content to the
 * real world.
 */
// TODO: Make this class thread safe.
public class Session(
    public val activity: Activity,
    public val platformAdapter: JxrPlatformAdapter,
    public val spatialEnvironment: SpatialEnvironment,
) {
    internal val entityManager by lazy { EntityManager() }

    /**
     * The ActivitySpace is a special entity that represents the space in which the application is
     * launched. It is the default parent of all entities in the scene.
     *
     * The ActivitySpace is created automatically when the Session is created.
     */
    public val activitySpace: ActivitySpace = ActivitySpace.create(platformAdapter, entityManager)

    /** The SpatialUser contains information about the user. */
    public val spatialUser: SpatialUser = SpatialUser.create(platformAdapter)

    /**
     * A spatialized PanelEntity associated with the "main window" for the Activity. When in
     * HomeSpace mode, this is the application's "main window".
     *
     * If called multiple times, this will return the same PanelEntity.
     */
    public val mainPanelEntity: PanelEntity =
        PanelEntity.createMainPanelEntity(platformAdapter, entityManager)

    /**
     * The PerceptionSpace represents the origin of the space in which the ARCore for XR API
     * provides tracking info. The transformations provided by the PerceptionSpace are only valid
     * for the call frame, as the transformation can be changed by the system at any time.
     */
    public val perceptionSpace: PerceptionSpace = PerceptionSpace.create(platformAdapter)

    // TODO: 378706624 - Remove this method once we have a better way to handle the root entity.
    public val activitySpaceRoot: Entity by lazy {
        entityManager.getEntityForRtEntity(platformAdapter.activitySpaceRootImpl)!!
    }

    public companion object {
        private const val TAG = "Session"
        private val activitySessionMap = ConcurrentHashMap<Activity, Session>()

        // TODO: b/323060217 - Move the platformAdapter behind a loader class that loads it in.
        /**
         * Creates a session and pairs it with an Activity and its lifecycle. If a session is
         * already paired with an Activity, return that Session instead of creating a new one.
         *
         * For our Alpha release, we just directly instantiate the Android XR PlatformAdapter.
         */
        // TODO(b/326748782): Change the returned Session here to be nullable or asynchronous.
        @JvmStatic
        @JvmOverloads
        public fun create(
            activity: Activity,
            platformAdapter: JxrPlatformAdapter? = null
        ): Session {
            return activitySessionMap.computeIfAbsent(activity) {
                Log.i(TAG, "Creating session for activity $activity")
                val session =
                    when (platformAdapter) {
                        null -> {
                            val platformAdapterImpl =
                                JxrPlatformAdapterAxr.create(
                                    activity,
                                    Executors.newSingleThreadScheduledExecutor(
                                        object : ThreadFactory {
                                            override fun newThread(r: Runnable): Thread {
                                                return Thread(r, "JXRCoreSession")
                                            }
                                        }
                                    ),
                                )
                            Session(
                                activity,
                                platformAdapterImpl,
                                SpatialEnvironment(platformAdapterImpl)
                            )
                        }
                        else ->
                            Session(activity, platformAdapter, SpatialEnvironment(platformAdapter))
                    }
                activity.registerActivityLifecycleCallbacks(
                    object : ActivityLifecycleCallbacks {
                        override fun onActivityCreated(
                            activity: Activity,
                            savedInstanceState: Bundle?
                        ) {}

                        override fun onActivityStarted(activity: Activity) {}

                        override fun onActivityResumed(activity: Activity) {
                            session.platformAdapter.startRenderer()
                        }

                        override fun onActivityPaused(activity: Activity) {
                            session.platformAdapter.stopRenderer()
                        }

                        override fun onActivityStopped(activity: Activity) {}

                        override fun onActivitySaveInstanceState(
                            activity: Activity,
                            outState: Bundle
                        ) {}

                        override fun onActivityDestroyed(activity: Activity) {
                            activitySessionMap.remove(activity)
                            session.entityManager.clear()
                            session.platformAdapter.dispose()
                        }
                    }
                )
                session
            }
        }
    }
}
