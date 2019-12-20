/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.textclassifier;

import androidx.annotation.RestrictTo;
import androidx.core.app.RemoteActionCompat;
import androidx.textclassifier.TextClassifier.EntityType;

import java.util.List;

/**
 * Returns actions for a specified entity type.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
interface MatchMaker {
    /**
     * Returns an ordered list of actions for the specified entityType. Clients should expect
     * that the actions will be ordered based on how important the matchmaker thinks the action
     * is to the current task.
     */
    List<RemoteActionCompat> getActions(@EntityType String entityType, CharSequence text);
}
