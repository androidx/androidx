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

package androidx.credentials.registry.provider.digitalcredentials

import androidx.annotation.RestrictTo

/**
 * A digital credential entry to be registered.
 *
 * This entry contains information that serves two major purposes:
 * 1. Credential metadata for filtering / request matching purpose. When another app makes a
 *    [CredentialManager.getCredential] request, it will specify the specific credential properties
 *    it is looking for. The Credential Manager will use the info stored for the given entry and the
 *    registry matcher you registered to determine whether this entry can fulfill an incoming
 *    request.
 * 2. Display metadata that will be rendered as part of the Credential Manager selector UI. The
 *    selector UI, post filtering, will display to the user their qualified, available credential
 *    candidates, in order for the user to make an informed choice.
 *
 *    All display metadata will be contained in the `entryDisplayData` property.
 *
 * @property id the unique identifier of this credential entry, which can be used to identify the
 *   exact credential that the user has chosen
 * @property entryDisplayData the display properties associated with the given entry
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class DigitalCredentialEntry
constructor(
    public val id: String,
    public val entryDisplayData: Set<EntryDisplayData>,
)
