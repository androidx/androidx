/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.ongoing;

import androidx.versionedparcelable.VersionedParcelable;

/**
 * Abstract class to represent An Ongoing activity status or part of it.
 * <p>
 * Parts are used to create complex statuses, that may contain several timers, placeholders for
 * text, etc.
 * They may also be used to convey information to the system about this Ongoing Activity.
 */
public abstract class StatusPart implements VersionedParcelable, TimeDependentText {
}
