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

package androidx.compose.remote.core.operations;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Limits;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.events.Event;
import androidx.compose.remote.core.events.EventHandler;
import androidx.compose.remote.core.events.EventManager;
import androidx.compose.remote.core.operations.layout.ActionOperation;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.Container;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Handles dispatched events by mapping payloads and running nested actions conditionally. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EventActionOperation extends Operation implements EventHandler, Container,
        ComponentData {

    private static final int OP_CODE = Operations.EVENT_ACTION;
    private static final int VERSION = 0;

    /** The serializable class name. */
    public static final String CLASS_NAME = "EventActionOperation";

    final int mType;
    final int mFilter;
    final int mFlags;
    final int @Nullable [] mDataIds;
    final float @Nullable [] mCondition;
    @NonNull private final ArrayList<Operation> mOperations = new ArrayList<>();

    /**
     * Constructs a new EventActionOperation.
     *
     * @param type type identifying compatible events
     * @param filter filter metadata required to match onEvent
     * @param flags routing specific status flags returned on success
     * @param dataIds optional mapping of input payload indices to target float variables
     * @param condition optional RPN condition used to conditionally trigger actions
     */
    public EventActionOperation(
            int type,
            int filter,
            int flags,
            int @Nullable [] dataIds,
            float @Nullable [] condition) {
        mType = type;
        mFilter = filter;
        mFlags = flags;
        mDataIds = dataIds;
        mCondition = condition;
    }

    @Override
    public boolean matchesEvent(int eventType) {
        return eventType == mType;
    }

    @Override
    public int onEvent(
            @NonNull RemoteContext context, @NonNull CoreDocument document, @NonNull Event event) {

        if (event.getMetadata() != mFilter) {
            return EventManager.STATUS_UNHANDLED;
        }

        if (mDataIds != null) {
            float[] data = event.getData();
            if (data != null) {
                int copyLength = Math.min(data.length, mDataIds.length);
                for (int i = 0; i < copyLength; i++) {
                    // Ignore fields which the user has not set.
                    if (mDataIds[i] != 0) {
                        context.overrideFloat(mDataIds[i], data[i]);
                    }
                }
            }
        }

        // Calculate the condition to determine whether we should run the actions.
        boolean condition = true;
        if (mCondition != null) {
            // Create a temporary FloatExpression instance (id = -1 for inline evaluation)
            FloatExpression expression = new FloatExpression(-1, mCondition, null);
            // evaluate(context) automatically updates variable listeners & evaluates the RPN stack
            float result = expression.evaluate(context);
            condition = (result != 0f); // 1.0f represents true, 0.0f false
        }
        if (condition) {
            for (Operation operation : mOperations) {
                Component component = context.mLastComponent;
                if (operation instanceof ActionOperation && component != null) {
                    // TODO(b/556845784): Component, X and Y are not used by runAction.
                    ((ActionOperation) operation).runAction(context, document, component, 0, 0);
                }
            }
            return mFlags;
        }

        return EventManager.STATUS_UNHANDLED;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mType, mFilter, mFlags, mDataIds, mCondition);
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        if (context.getMode() == RemoteContext.ContextMode.DATA) {
            context.getEventManager().registerHandler(this);
        }
    }

    @Override
    public @NonNull ArrayList<Operation> getList() {
        return mOperations;
    }

    @Override
    public @NonNull String deepToString(@NonNull String indent) {
        return indent + this;
    }

    /**
     * Serializes EventActionOperation attributes to the provided buffer.
     *
     * @param buffer the target wire buffer
     * @param type the event type
     * @param filter the filter metadata
     * @param flags the routing flags
     * @param dataIds optional target payload data mapping IDs
     * @param condition optional conditional float expression RPN stream
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int type,
            int filter,
            int flags,
            int @Nullable [] dataIds,
            float @Nullable [] condition) {
        buffer.start(OP_CODE);
        buffer.writeInt(VERSION);
        buffer.writeInt(type);
        buffer.writeInt(filter);

        // Common flags.
        boolean isConditional = condition != null && condition.length > 0;
        boolean hasData = dataIds != null && dataIds.length > 0;
        buffer.writeShort(CommonFlagsUtil.pack(isConditional, hasData));

        // Event specific flags.
        buffer.writeShort(flags);

        if (hasData) {
            int dataLen = dataIds.length;
            buffer.writeInt(dataLen);
            for (int data : dataIds) {
                buffer.writeInt(data);
            }
        }

        if (isConditional) {
            int len = condition.length;
            if (len > Limits.MAX_EXPRESSION_SIZE) {
                throw new RuntimeException(
                        "Condition expression passed to EventHandler is too long");
            }
            buffer.writeInt(len);
            for (float v : condition) {
                buffer.writeFloat(v);
            }
        }
    }

    /**
     * Deserializes and inflates EventActionOperation from the provided buffer.
     *
     * @param buffer the source wire buffer
     * @param operations target collection to append the parsed operation
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int version = buffer.readInt();
        if (version != 0) {
            throw new RuntimeException("Unsupported EventActionOperation version: " + version);
        }
        int type = buffer.readInt();
        int filter = buffer.readInt();
        int commonFlags = buffer.readShort();
        int flags = buffer.readShort();

        int[] dataIds = null;
        if (CommonFlagsUtil.hasData(commonFlags)) {
            int dataLen = buffer.readInt();
            dataIds = new int[dataLen];
            for (int i = 0; i < dataLen; i++) {
                dataIds[i] = buffer.readInt();
            }
        }

        float[] condition = null;
        if (CommonFlagsUtil.isConditional(commonFlags)) {
            int len = buffer.readInt();
            if (len > Limits.MAX_EXPRESSION_SIZE) {
                throw new RuntimeException("Float expression too long");
            }
            condition = new float[len];
            for (int i = 0; i < condition.length; i++) {
                condition[i] = buffer.readNanId();
            }
        }
        operations.add(new EventActionOperation(type, filter, flags, dataIds, condition));
    }

    /** Utility for packing and unpacking standard 16-bit common communication flags. */
    private static class CommonFlagsUtil {
        // Flag definitions within the 16-bit common space:
        private static final int FLAG_NONE = 0;
        private static final int FLAG_UNCONDITIONAL = 1; // Skip condition evaluation (always true)
        private static final int FLAG_NO_DATA = 1 << 1; // Skip event data (no variable write)

        private CommonFlagsUtil() {}

        /** Packs common boolean options into a 16-bit bitmask. */
        public static int pack(boolean isConditional, boolean hasData) {
            int flags = FLAG_NONE;
            if (!isConditional) flags |= FLAG_UNCONDITIONAL;
            if (!hasData) flags |= FLAG_NO_DATA;
            return flags;
        }

        /** Returns true if the condition should be evaluated. */
        public static boolean isConditional(int commonFlags) {
            return (commonFlags & FLAG_UNCONDITIONAL) == 0;
        }

        /** Returns true if payload data IDs are present. */
        public static boolean hasData(int commonFlags) {
            return (commonFlags & FLAG_NO_DATA) == 0;
        }
    }
}
