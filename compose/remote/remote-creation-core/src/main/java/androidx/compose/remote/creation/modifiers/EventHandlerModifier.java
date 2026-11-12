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
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.actions.Action;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Encapsulate actions executed when a matching event is dispatched to the component. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EventHandlerModifier implements RecordingModifier.Element {
    @NonNull
    ArrayList<Action> mList = new ArrayList<>();

    final int mType;
    final int mFilter;
    final int mFlags;
    final int @Nullable [] mDataIds;
    final float @Nullable [] mCondition;

    /**
     * Create an event handler modifier.
     *
     * @param type      type identifying compatible events
     * @param filter    filter metadata required to match the event
     * @param flags     routing specific status flags returned on success
     * @param dataIds   optional mapping of input payload indices to target float variables
     * @param condition optional RPN condition used to conditionally trigger actions
     * @param actions   list of actions to execute when the event matches
     */
    public EventHandlerModifier(
            int type,
            int filter,
            int flags,
            int @Nullable [] dataIds,
            float @Nullable [] condition,
            @NonNull List<Action> actions) {
        mType = type;
        mFilter = filter;
        mFlags = flags;
        mDataIds = dataIds;
        mCondition = condition;
        mList.addAll(actions);
    }

    @Override
    public void write(@NonNull RemoteComposeWriter writer) {
        writer.addEventHandlerModifierOperation(mType, mFilter, mFlags, mDataIds, mCondition);
        for (Action m : mList) {
            m.write(writer);
        }
        writer.addContainerEnd();
    }

    public @NonNull List<Action> getActions() {
        return mList;
    }
}
