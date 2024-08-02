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

package androidx.core.app

import android.app.Activity

/** Interface for components that can dispatch calls from [Activity.onUserLeaveHint]. */
public interface OnUserLeaveHintProvider {
    /**
     * Add a new listener that will get a callback associated with [Activity.onUserLeaveHint]
     *
     * @param listener The listener that should be called whenever [Activity.onUserLeaveHint] was
     *   called.
     */
    public fun addOnUserLeaveHintListener(listener: Runnable)

    /**
     * Remove a previously added listener. It will not receive any future callbacks.
     *
     * @param listener The listener previously added with [addOnUserLeaveHintListener] that should
     *   be removed.
     */
    public fun removeOnUserLeaveHintListener(listener: Runnable)
}
