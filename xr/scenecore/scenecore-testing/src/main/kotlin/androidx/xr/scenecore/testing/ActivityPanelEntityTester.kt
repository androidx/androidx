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

import android.app.Activity
import android.content.Intent
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.testing.internal.FakeActivityPanelEntity as InternalFakeActivityPanelEntity

/**
 * A test-only accessor for [ActivityPanelEntity] that enables direct manipulation and inspection of
 * its internal state.
 */
public class ActivityPanelEntityTester
internal constructor(
    private val rtActivityPanelEntity: InternalFakeActivityPanelEntity,
    internal val activityPanelEntity: ActivityPanelEntity,
) {

    internal companion object {
        /**
         * Retrieves a test data accessor for the given [ActivityPanelEntity].
         *
         * This function provides a [ActivityPanelEntityTester] instance, which can be used to
         * inspect and manipulate its underlying data in the test environment.
         *
         * @param activityPanelEntity The entity for which to retrieve the test data accessor.
         * @return A [ActivityPanelEntityTester] instance for the given entity.
         */
        internal fun create(activityPanelEntity: ActivityPanelEntity): ActivityPanelEntityTester {
            @Suppress("DEPRECATION")
            return ActivityPanelEntityTester(
                (activityPanelEntity.rtEntity as FakeEntity).fakeInternal
                    as InternalFakeActivityPanelEntity,
                activityPanelEntity,
            )
        }
    }

    /**
     * The [Intent] that was last used to start an activity in the panel via
     * [ActivityPanelEntity.startActivity].
     */
    public val startActivityIntent: Intent?
        get() = rtActivityPanelEntity.launchIntent

    /**
     * The [Activity] that was last transferred into the panel via
     * [ActivityPanelEntity.transferActivity].
     */
    public val transferredActivity: Activity?
        get() = rtActivityPanelEntity.movedActivity

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ActivityPanelEntityTester

        if (rtActivityPanelEntity != other.rtActivityPanelEntity) return false
        if (activityPanelEntity != other.activityPanelEntity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rtActivityPanelEntity.hashCode()
        result = 31 * result + activityPanelEntity.hashCode()

        return result
    }
}
