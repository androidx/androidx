/*
 * Copyright (C) 2026 The Android Open Source Project
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
package androidx.compose.remote.core.operations;

import static androidx.compose.remote.core.documentation.DocumentedOperation.BYTE;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.SerializableToString;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.managers.RcDocLayout;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Operation that maps a host document variable or host action to a named variable or action ID
 * in an embedded {@link RcDocLayout} child document.
 *
 * <ul>
 *   <li>{@link #MAP_FORWARD} (0): copies the host variable {@code localId} to the child document's
 *       named variable {@code remoteName} (identified by text ID {@code remoteTarget}).
 *   <li>{@link #MAP_REVERSE} (1): copies the child document's named variable {@code remoteName}
 *       back to the host variable {@code localId}.
 *   <li>{@link #MAP_BIDIRECTIONAL} (2): synchronizes {@code localId} and {@code remoteName} in
 *       both directions.
 *   <li>{@link #MAP_HOST_ACTION} (3): intercepts {@code hostAction(actionId, metadata)} from the
 *       child document where {@code actionId == remoteTarget} and writes the {@code metadata}
 *       string into the host String variable {@code localId}. Unmapped action IDs are forwarded to
 *       the host's real action handler.
 * </ul>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MapRcDocVariables extends Operation
        implements ComponentData, VariableSupport, SerializableToString, Serializable {
    private static final int OP_CODE = Operations.MAP_RC_DOC_VARIABLES;
    private static final String CLASS_NAME = "MapRcDocVariables";

    /** Forward mapping: host {@code localId} -> child {@code remoteName}. */
    public static final int MAP_FORWARD = 0;

    /** Reverse mapping: child {@code remoteName} -> host {@code localId}. */
    public static final int MAP_REVERSE = 1;

    /** Bidirectional mapping: host {@code localId} <-> child {@code remoteName}. */
    public static final int MAP_BIDIRECTIONAL = 2;

    /**
     * Host action mapping: child {@code hostAction(remoteTarget, metadata)} sets host String
     * variable {@code localId} to {@code metadata}.
     */
    public static final int MAP_HOST_ACTION = 3;

    private final int mDocId;
    private final int mMode;
    private final int mLocalId;
    private final int mRemoteTarget;

    private boolean mInitialSynced = false;
    private float mLastSyncedFloat = Float.NaN;
    private int mLastSyncedInt = Integer.MIN_VALUE;
    private int mLastSyncedColor = 0;
    private @Nullable String mLastSyncedText = null;

    /**
     * Create a new {@link MapRcDocVariables} operation.
     *
     * @param docId the component ID of the target {@link RcDocLayout}
     * @param mode one of {@link #MAP_FORWARD}, {@link #MAP_REVERSE}, {@link #MAP_BIDIRECTIONAL}, or
     *     {@link #MAP_HOST_ACTION}
     * @param localId the variable ID in the host document
     * @param remoteTarget for variable modes (0..2), the text ID of the named variable in the child
     *     document; for {@link #MAP_HOST_ACTION} (3), the child action ID to intercept
     */
    public MapRcDocVariables(int docId, int mode, int localId, int remoteTarget) {
        mDocId = docId;
        mMode = mode;
        mLocalId = localId;
        mRemoteTarget = remoteTarget;
    }

    /** Return the target {@link RcDocLayout} component ID. */
    public int getDocId() {
        return mDocId;
    }

    /** Return the mapping mode. */
    public int getMode() {
        return mMode;
    }

    /** Return the host variable ID. */
    public int getLocalId() {
        return mLocalId;
    }

    /**
     * Return the remote target (text ID of the child variable name for modes 0..2, or child action
     * ID for {@link #MAP_HOST_ACTION}).
     */
    public int getRemoteTarget() {
        return mRemoteTarget;
    }

    /** Reset bidirectional synchronization state when the embedded document is reloaded. */
    public void resetSyncState() {
        mInitialSynced = false;
        mLastSyncedFloat = Float.NaN;
        mLastSyncedInt = Integer.MIN_VALUE;
        mLastSyncedColor = 0;
        mLastSyncedText = null;
    }

    /** Return true if initial bidirectional synchronization has completed. */
    public boolean isInitialSynced() {
        return mInitialSynced;
    }

    /** Set whether initial bidirectional synchronization has completed. */
    public void setInitialSynced(boolean initialSynced) {
        mInitialSynced = initialSynced;
    }

    /** Return the last synced float value for bidirectional mappings. */
    public float getLastSyncedFloat() {
        return mLastSyncedFloat;
    }

    /** Set the last synced float value for bidirectional mappings. */
    public void setLastSyncedFloat(float value) {
        mLastSyncedFloat = value;
    }

    /** Return the last synced integer value for bidirectional mappings. */
    public int getLastSyncedInt() {
        return mLastSyncedInt;
    }

    /** Set the last synced integer value for bidirectional mappings. */
    public void setLastSyncedInt(int value) {
        mLastSyncedInt = value;
    }

    /** Return the last synced color value for bidirectional mappings. */
    public int getLastSyncedColor() {
        return mLastSyncedColor;
    }

    /** Set the last synced color value for bidirectional mappings. */
    public void setLastSyncedColor(int value) {
        mLastSyncedColor = value;
    }

    /** Return the last synced string value for bidirectional mappings. */
    public @Nullable String getLastSyncedText() {
        return mLastSyncedText;
    }

    /** Set the last synced string value for bidirectional mappings. */
    public void setLastSyncedText(@Nullable String value) {
        mLastSyncedText = value;
    }

    private @Nullable RcDocLayout attachToRcDoc(@NonNull RemoteContext context) {
        CoreDocument doc = context.getDocument();
        if (doc != null) {
            Component comp = doc.getComponent(mDocId);
            if (comp instanceof RcDocLayout) {
                RcDocLayout rcDoc = (RcDocLayout) comp;
                rcDoc.addVariableMapping(this);
                return rcDoc;
            }
        }
        return null;
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        RcDocLayout rcDoc = attachToRcDoc(context);
        if (mMode == MAP_FORWARD || mMode == MAP_BIDIRECTIONAL) {
            context.listensTo(mLocalId, this);
            if (rcDoc != null) {
                context.listensTo(mLocalId, rcDoc);
            }
        }
        if (mMode != MAP_HOST_ACTION && mRemoteTarget > 0) {
            context.listensTo(mRemoteTarget, this);
        }
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        RcDocLayout rcDoc = attachToRcDoc(context);
        if (rcDoc != null) {
            rcDoc.needsRepaint();
        }
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        RcDocLayout rcDoc = attachToRcDoc(context);
        if (rcDoc != null && (mMode == MAP_FORWARD || mMode == MAP_BIDIRECTIONAL)) {
            context.listensTo(mLocalId, rcDoc);
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mDocId, mMode, mLocalId, mRemoteTarget);
    }

    /**
     * Write a {@link MapRcDocVariables} operation to the buffer.
     *
     * @param buffer the WireBuffer to write to
     * @param docId the target {@link RcDocLayout} component ID
     * @param mode the mapping mode (0..3)
     * @param localId the host variable ID
     * @param remoteTarget the child variable name text ID (modes 0..2) or action ID (mode 3)
     */
    public static void apply(
            @NonNull WireBuffer buffer, int docId, int mode, int localId, int remoteTarget) {
        buffer.start(OP_CODE);
        buffer.writeInt(docId);
        buffer.writeByte(mode);
        buffer.writeInt(localId);
        buffer.writeInt(remoteTarget);
    }

    /**
     * Read this operation from the buffer and append it to {@code operations}.
     *
     * @param buffer the WireBuffer to read from
     * @param operations the list of operations to append to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int docId = buffer.readId();
        int mode = buffer.readByte();
        int localId = buffer.readId();
        int remoteTarget = mode == MAP_HOST_ACTION ? buffer.readInt() : buffer.readId();
        operations.add(new MapRcDocVariables(docId, mode, localId, remoteTarget));
    }

    /**
     * Populate the documentation with a description of this operation.
     *
     * @param doc the DocumentationBuilder to append to
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Data Operations", OP_CODE, CLASS_NAME)
                .description(
                        "Map a host variable or hostAction to an embedded RcDocLayout document")
                .field(INT, "docId", "The component ID of the target RcDocLayout")
                .field(
                        BYTE,
                        "mode",
                        "Mapping mode (0=FORWARD, 1=REVERSE, 2=BIDIRECTIONAL, 3=HOST_ACTION)")
                .field(INT, "localId", "The variable ID in the host document")
                .field(
                        INT,
                        "remoteTarget",
                        "Text ID of the remote variable name (modes 0..2) or action ID (mode 3)");
    }

    @NonNull
    @Override
    public String toString() {
        return "MapRcDocVariables(docId="
                + mDocId
                + ", mode="
                + mMode
                + ", localId="
                + mLocalId
                + ", remoteTarget="
                + mRemoteTarget
                + ")";
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(
                indent,
                "MAP_RC_DOC_VARIABLES docId="
                        + mDocId
                        + " mode="
                        + mMode
                        + " localId="
                        + mLocalId
                        + " remoteTarget="
                        + mRemoteTarget);
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("docId", mDocId)
                .add("mode", mMode)
                .add("localId", mLocalId)
                .add("remoteTarget", mRemoteTarget);
    }
}
