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

package androidx.compose.runtime

import kotlin.jvm.JvmField

@ExperimentalComposeApi
public object ComposeRuntimeFlags {
    /**
     * A feature flag that can be used to disable detecting nested movable content.
     *
     * The way movable is detected was changed to ensure that movable content that is no longer
     * used, but was nested in other unused movable content, is made a candidate for moving to avoid
     * state being lost. However, this is a change in behavior may have indirectly been relied on by
     * an application. This flags allows detecting if any regressions are caused by this change in
     * behavior and provides a temporary work-around.
     *
     * This feature flag will eventually be depreciated and removed. All applications should be
     * updated to ensure they are compatible with the new behavior.
     */
    @JvmField
    @Suppress("MutableBareField")
    public var isMovingNestedMovableContentEnabled: Boolean = true

    /**
     * A feature flag that can be used to enable movable content usage tracking.
     *
     * With this feature flag enabled the usage of movable content instances is tracked to avoid
     * deferring the insert of content that is not currently in use. When movable content is
     * inserted (e.g. a movable content lambda is called for the first time, instead of being
     * recomposed) the insert is deferred until after all other composition has completed. This
     * allows detecting when instances of the movable content is removed from elsewhere in any
     * composition and can be moved to the location of the new call. However, when movable content
     * is not used this deferral will never end up finding removed content that matches so
     * deferring, in this case, is unnecessary.
     */
    @JvmField
    @Suppress("MutableBareField")
    public var isMovableContentUsageTrackingEnabled: Boolean = false
}
