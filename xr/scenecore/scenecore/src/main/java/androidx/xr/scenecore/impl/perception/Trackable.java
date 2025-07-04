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

package androidx.xr.scenecore.impl.perception;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A Trackable is something that can be tracked in the real world (e.g. a plane). An anchor can be
 * attached to a trackable in order to keep track of a point relative to that trackable.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface Trackable {

    /**
     * Creates an anchor on the trackable for a point in the specified reference space.
     *
     * @param pose Specifies the pose relative to the center point of the trackable.
     * @param timeNs The monotonic time retrieved using the SystemClock's uptimeMillis() or
     *     uptimeNanos() at which to get the pose, in nanoseconds. If time is null or negative, the
     *     current time will be used.
     */
    @Nullable Anchor createAnchor(
            @NonNull Pose pose, @SuppressWarnings("AutoBoxing") @Nullable Long timeNs);

    /** Returns all anchors attached to this trackable. */
    @NonNull List<Anchor> getAnchors();
}
