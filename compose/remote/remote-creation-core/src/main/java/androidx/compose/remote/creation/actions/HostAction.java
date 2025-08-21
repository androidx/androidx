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
package androidx.compose.remote.creation.actions;

import androidx.compose.remote.core.operations.layout.modifiers.HostActionMetadataOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HostActionOperation;
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation;
import androidx.compose.remote.creation.RemoteComposeWriter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class HostAction implements Action {
    int mActionId = -1;
    int mValueId = -1;
    int mType = -1;

    @Nullable String mActionName = null;

    public HostAction(int id) {
        mActionId = id;
    }

    public HostAction(int id, int metadataId) {
        mActionId = id;
        mValueId = metadataId;
    }

    public HostAction(@NonNull String name, int type, int valueId) {
        mActionName = name;
        mType = type;
        mValueId = valueId;
    }

    public HostAction(@NonNull String name) {
        mActionName = name;
        mValueId = -1;
    }

    public @Nullable String getActionName() {
        return mActionName;
    }

    @Override
    public void write(@NonNull RemoteComposeWriter writer) {
        if (mActionName == null) {
            if (mValueId != -1) {
                HostActionMetadataOperation.apply(
                        writer.getBuffer().getBuffer(), mActionId, mValueId);
            } else {
                HostActionOperation.apply(writer.getBuffer().getBuffer(), mActionId);
            }
        } else {
            int textId = writer.addText(mActionName);
            HostNamedActionOperation.apply(writer.getBuffer().getBuffer(), textId, mType, mValueId);
        }
    }

    @Override
    public @NonNull String toString() {
        return "HostAction{"
                + "mActionId="
                + mActionId
                + ", mActionName='"
                + mActionName
                + '\''
                + '}';
    }
}
