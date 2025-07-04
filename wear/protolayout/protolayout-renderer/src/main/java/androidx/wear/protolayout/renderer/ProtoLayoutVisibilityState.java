/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.renderer;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** The visibility state of the layout. */
@IntDef({
    ProtoLayoutVisibilityState.VISIBILITY_STATE_FULLY_VISIBLE,
    ProtoLayoutVisibilityState.VISIBILITY_STATE_PARTIALLY_VISIBLE,
    ProtoLayoutVisibilityState.VISIBILITY_STATE_INVISIBLE,
})
@RestrictTo(Scope.LIBRARY)
@Retention(RetentionPolicy.SOURCE)
public @interface ProtoLayoutVisibilityState {
    /** Fully visible and on-screen. */
    int VISIBILITY_STATE_FULLY_VISIBLE = 0;

    /** The layout is either entering or leaving the screen. */
    int VISIBILITY_STATE_PARTIALLY_VISIBLE = 1;

    /** The layout is off screen, or covered up by a foreground activity. */
    int VISIBILITY_STATE_INVISIBLE = 2;
}
