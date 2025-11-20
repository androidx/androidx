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
package androidx.compose.remote.core.operations.layout;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.TextData;
import androidx.compose.remote.core.operations.layout.modifiers.ModifierOperation;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Vector;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ListActionsOperation extends PaintOperation
        implements Container, ModifierOperation, DecoratorComponent {

    @NonNull String mOperationName;
    protected float mWidth = 0;
    protected float mHeight = 0;

    private final float[] mLocationInWindow = new float[2];

    public ListActionsOperation(@NonNull String operationName) {
        mOperationName = operationName;
    }

    public @NonNull ArrayList<Operation> mList = new ArrayList<>();

    @NonNull
    @Override
    public ArrayList<Operation> getList() {
        return mList;
    }

    @Override
    public String toString() {
        return mOperationName;
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        for (Operation op : mList) {
            if (op instanceof TextData) {
                op.apply(context);
                context.incrementOpCount();
            }
        }
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return (indent != null ? indent : "") + toString();
    }

    @Override
    public void paint(@NonNull PaintContext context) {}

    @Override
    public void layout(
            @NonNull RemoteContext context,
            @NonNull Component component,
            float width,
            float height) {
        mWidth = width;
        mHeight = height;
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(indent, mOperationName);
        for (Operation o : mList) {
            if (o instanceof ActionOperation) {
                ((ActionOperation) o).serializeToString(indent + 1, serializer);
            }
        }
    }

    /**
     * Execute the list of actions
     *
     * @param context the RemoteContext
     * @param document the current document
     * @param component the component we belong to
     * @param x the x touch down coordinate
     * @param y the y touch down coordinate
     * @param force if true, will apply the actions even if the component is not visible / not
     *     containing the touch down coordinates
     * @return true if we applied the actions, false otherwise
     */
    public boolean applyActions(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            float x,
            float y,
            boolean force) {
        if (!force && !component.isVisible()) {
            return false;
        }
        if (!force && !component.contains(x, y)) {
            return false;
        }
        mLocationInWindow[0] = 0f;
        mLocationInWindow[1] = 0f;
        component.getLocationInWindow(mLocationInWindow);
        for (Operation o : mList) {
            if (o instanceof ActionOperation) {
                ((ActionOperation) o).runAction(context, document, component, x, y);
            }
        }
        return true;
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        // TODO: Pass in the list once all operations implement Serializable
        serializer.add("actions", new Vector<>());
    }
}
