/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.data.client.response

import androidx.health.data.client.changes.Change

/**
 * Response to clients fetching changes.
 *
 * If [changesTokenExpired] is true, clients need to generate a new one. This typically happens when
 * clients have not synced changelog for extended period of time.
 *
 * @property changes List of observed changes from Android Health Platform.
 * @property nextChangesToken Changes-token to keep for future calls.
 * @property hasMore Whether there are more changes available to be fetched again.
 * @property changesTokenExpired Whether requested Changes-Token has expired.
 *
 * @see [androidx.health.data.client.HealthDataClient.getChanges]
 */
class ChangesResponse
internal constructor(
    public val changes: List<Change>,
    public val nextChangesToken: String,
    public val hasMore: Boolean,
    public val changesTokenExpired: Boolean,
)
