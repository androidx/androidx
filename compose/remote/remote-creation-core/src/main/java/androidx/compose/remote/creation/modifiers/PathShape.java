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

package androidx.compose.remote.creation.modifiers;

import androidx.annotation.RestrictTo;

/**
 * Shape defined by a path already added to the document.
 *
 * <p>Used with {@link ClipModifier} to clip a component to an arbitrary path. The path coordinates
 * are interpreted relative to the component's content box, in the same space as its drawing
 * operations.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PathShape extends Shape {
    private final int mPathId;

    /**
     * @param pathId the id of the path, as returned by the path creation methods of the writer
     */
    public PathShape(int pathId) {
        mPathId = pathId;
    }

    /** Returns the id of the path defining this shape. */
    public int getPathId() {
        return mPathId;
    }
}
