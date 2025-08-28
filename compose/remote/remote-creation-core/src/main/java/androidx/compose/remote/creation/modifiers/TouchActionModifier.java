/*
 * Copyright (C) 2024 The Android Open Source Project
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

import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.actions.Action;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/** Encapsulate actions */
public class TouchActionModifier implements RecordingModifier.Element {
    @NonNull
    ArrayList<Action> mList = new ArrayList<>();

    public static final int DOWN = 0;
    public static final int UP = 1;
    public static final int CANCEL = 2;

    int mType = DOWN;

    public TouchActionModifier(int type, @NonNull List<Action> actions) {
        mList.addAll(actions);
        mType = type;
    }

    @Override
    public void write(@NonNull RemoteComposeWriter writer) {
        if (mType == DOWN) {
            writer.addTouchDownModifierOperation();
        } else if (mType == UP) {
            writer.addTouchUpModifierOperation();
        } else {
            writer.addTouchCancelModifierOperation();
        }
        for (Action m : mList) {
            m.write(writer);
        }
        writer.addContainerEnd();

    }

    public @NonNull List<Action> getActions() {
        return mList;
    }
}
