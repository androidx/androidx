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

package androidx.credentials.registry.provider

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * Defines the delegation mode for a credential entry.
 *
 * The delegation type determines whether the provider handles user interactions and UI during
 * credential fulfillment, or if that responsibility is delegated to Credential Manager.
 *
 * Delegating fulfillment to Credential Manager does not mean the entire user flow is completely
 * silent to the user. Rather, it means the specific step where the provider generates the
 * credential(s) occurs silently in the background without displaying provider-owned UI. While
 * Credential Manager waits for silent credential generation, it displays a loading UI indicator
 * with a timeout. Therefore, providers must generate and return their credential responses in a
 * timely manner to ensure a good user experience.
 *
 * If the user chooses a credential set containing mixed delegation types (for example, some
 * credentials specify [DelegationType.FULL] while others specify [DelegationType.NONE]), Credential
 * Manager falls back to standard interactive UI fulfillment unless all selected credentials in the
 * set support [DelegationType.FULL] delegation.
 */
public object DelegationType {
    /**
     * No delegation. The provider retains full control over authentication and UI when the entry is
     * selected.
     *
     * When selected by the user, Credential Manager launches the provider's fulfillment activity to
     * perform any necessary user interaction or authentication. Multi-provider aggregation is not
     * supported in this mode.
     */
    public const val NONE: Int = 0

    /**
     * Full delegation. The provider delegates UI and interaction handling to Credential Manager.
     *
     * When a delegated entry is selected, Credential Manager will invoke the provider's background
     * fulfillment service to handle the generation of credentials silently without launching
     * provider UI. Providers configuring this mode MUST implement and declare a background
     * fulfillment service.
     *
     * Delegating UI handling to Credential Manager enables seamless multi-provider aggregation and
     * allows users to select and share credentials through a single-tap experience, reducing
     * friction and user drop-off.
     */
    public const val FULL: Int = 1
}

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
@RestrictTo(RestrictTo.Scope.LIBRARY)
@IntDef(DelegationType.NONE, DelegationType.FULL)
@Retention(AnnotationRetention.SOURCE)
public annotation class DelegationTypeAnnotation
