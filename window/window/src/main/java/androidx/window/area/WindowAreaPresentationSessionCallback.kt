/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.window.area

import android.content.Context
import android.view.View
import androidx.window.core.ExperimentalWindowApi

/**
 * A callback to notify about the lifecycle of a window area presentation session.
 *
 * @see WindowAreaController.presentContentOnWindowArea
 */
@ExperimentalWindowApi
interface WindowAreaPresentationSessionCallback {

    /**
     * Notifies about a start of a presentation session. Provides a reference to
     * [WindowAreaSessionPresenter] to allow an application to customize a presentation when the
     * session starts. The [Context] provided from the [WindowAreaSessionPresenter] should be used
     * to inflate or make any UI decisions around the presentation [View] that should be shown in
     * that area.
     */
    fun onSessionStarted(session: WindowAreaSessionPresenter)

    /**
     * Notifies about an end of a presentation session. The presentation and any app-provided
     * content in the window area is removed.
     *
     * @param t [Throwable] to provide information on if the session was ended due to an error.
     * This will only occur if a session is attempted to be enabled when it is not available, but
     * can be expanded to alert for more errors in the future.
     */
    fun onSessionEnded(t: Throwable?)

    /**
     * Notifies about changes in visibility of a container that can hold the app content to show
     * in the window area. Notification of the container being visible is guaranteed to occur after
     * [onSessionStarted] has been called. The container being no longer visible is guaranteed to
     * occur before [onSessionEnded].
     *
     * If content was never presented, then this method will never be called.
     */
    fun onContainerVisibilityChanged(isVisible: Boolean)
}
