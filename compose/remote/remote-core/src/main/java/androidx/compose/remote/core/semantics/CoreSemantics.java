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
package androidx.compose.remote.core.semantics;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Implementation of the most common semantics used in typical Android apps. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class CoreSemantics extends Operation implements AccessibilityModifier {
    public int mContentDescriptionId = 0;
    public @Nullable Role mRole = null;
    public int mTextId = 0;
    public int mStateDescriptionId = 0;
    public boolean mEnabled = true;
    public @NonNull Mode mMode = Mode.SET;
    public boolean mClickable = false;

    @Override
    public int getOpCode() {
        return Operations.ACCESSIBILITY_SEMANTICS;
    }

    @Nullable
    @Override
    public Role getRole() {
        return mRole;
    }

    @Override
    public @NonNull Mode getMode() {
        return mMode;
    }

    /**
     * Applies the semantics to a WireBuffer.
     * @param buffer WireBuffer to apply the semantics to
     * @param contentDescriptionId content description id
     * @param role role
     * @param textId text id
     * @param stateDescriptionId state description id
     * @param mode mode
     * @param enabled enabled
     * @param clickable clickable
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int contentDescriptionId,
            byte role,
            int textId,
            int stateDescriptionId,
            int mode,
            boolean enabled,
            boolean clickable) {

        buffer.start(Operations.ACCESSIBILITY_SEMANTICS);
        buffer.writeInt(contentDescriptionId);
        buffer.writeByte(role);
        buffer.writeInt(textId);
        buffer.writeInt(stateDescriptionId);
        buffer.writeByte(mode);
        buffer.writeBoolean(enabled);
        buffer.writeBoolean(clickable);
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        // TODO this should write its start
        buffer.writeInt(mContentDescriptionId);
        buffer.writeByte((mRole != null) ? mRole.ordinal() : -1);
        buffer.writeInt(mTextId);
        buffer.writeInt(mStateDescriptionId);
        buffer.writeByte(mMode.ordinal());
        buffer.writeBoolean(mEnabled);
        buffer.writeBoolean(mClickable);
    }

    private void read(WireBuffer buffer) {
        mContentDescriptionId = buffer.readInt();
        mRole = Role.fromInt(buffer.readByte());
        mTextId = buffer.readInt();
        mStateDescriptionId = buffer.readInt();
        mMode = Mode.values()[buffer.readByte()];
        mEnabled = buffer.readBoolean();
        mClickable = buffer.readBoolean();
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        // Handled via touch helper
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SEMANTICS");
        if (mMode != Mode.SET) {
            builder.append(" ");
            builder.append(mMode);
        }
        if (mRole != null) {
            builder.append(" ");
            builder.append(mRole);
        }
        if (mContentDescriptionId > 0) {
            builder.append(" contentDescription=");
            builder.append(mContentDescriptionId);
        }
        if (mTextId > 0) {
            builder.append(" text=");
            builder.append(mTextId);
        }
        if (mStateDescriptionId > 0) {
            builder.append(" stateDescription=");
            builder.append(mStateDescriptionId);
        }
        if (!mEnabled) {
            builder.append(" disabled");
        }
        if (mClickable) {
            builder.append(" clickable");
        }
        return builder.toString();
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + this;
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(indent, "SEMANTICS" + " = " + this);
    }

    /**
     * Reads a CoreSemantics object from a WireBuffer and adds it to a list of operations.
     *
     * <p>This method reads the data required to construct a CoreSemantics object from the provided
     * WireBuffer. After reading and constructing the CoreSemantics object, it is added to the
     * provided list of operations.
     *
     * @param buffer The WireBuffer to read data from.
     * @param operations The list of operations to which the read CoreSemantics object will be
     *     added.
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        CoreSemantics semantics = new CoreSemantics();

        semantics.read(buffer);

        operations.add(semantics);
    }

    @Override
    public @NonNull Integer getContentDescriptionId() {
        return mContentDescriptionId != 0 ? mContentDescriptionId : null;
    }

    public @Nullable Integer getStateDescriptionId() {
        return mStateDescriptionId != 0 ? mStateDescriptionId : null;
    }

    @Override
    public @Nullable Integer getTextId() {
        return mTextId != 0 ? mTextId : null;
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addTags(SerializeTags.MODIFIER, SerializeTags.A11Y)
                .addType("CoreSemantics")
                .add("contentDescriptionId", mContentDescriptionId)
                .add("role", mRole)
                .add("textId", mTextId)
                .add("stateDescriptionId", mStateDescriptionId)
                .add("enabled", mEnabled)
                .add("mode", mMode)
                .add("clickable", mClickable);
    }
}
