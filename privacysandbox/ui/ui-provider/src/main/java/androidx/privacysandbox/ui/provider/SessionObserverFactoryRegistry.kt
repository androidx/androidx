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

package androidx.privacysandbox.ui.provider

import androidx.privacysandbox.ui.core.SessionObserver
import androidx.privacysandbox.ui.core.SessionObserverFactory

public interface SessionObserverFactoryRegistry {

    public val sessionObserverFactories: List<SessionObserverFactory>

    /**
     * Adds a [SessionObserverFactory] for tracking UI presentation state across UI sessions. This
     * has no effect on already open sessions.
     *
     * For each session that is created for the adapter after this call returns,
     * [SessionObserverFactory.create] will be invoked to allow a new [SessionObserver] instance to
     * be attached to the UI session. This [SessionObserver] will receive UI updates for the
     * lifetime of the session. A separate [SessionObserverFactory.create] call will be made for
     * each UI session.
     */
    public fun addObserverFactory(sessionObserverFactory: SessionObserverFactory)

    /**
     * Removes a [SessionObserverFactory], if it has been previously added with
     * [addObserverFactory].
     *
     * If the [SessionObserverFactory] was not previously added, no action is performed. Any
     * existing [SessionObserver] instances that have been created by the [SessionObserverFactory]
     * will continue to receive updates until their corresponding UI session has been closed. For
     * any subsequent sessions created], no call to [SessionObserverFactory.create] will be made.
     */
    public fun removeObserverFactory(sessionObserverFactory: SessionObserverFactory)
}
