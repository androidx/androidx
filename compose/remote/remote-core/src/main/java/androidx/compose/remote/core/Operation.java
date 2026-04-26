/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.compose.remote.core;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.layout.Container;
import androidx.compose.remote.core.operations.layout.ContainerEnd;
import androidx.compose.remote.core.operations.loom.ExpansionContext;
import androidx.compose.remote.core.operations.loom.LoomManager;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;

/** Base interface for RemoteCompose operations */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class Operation {

    private static final boolean ENABLE_DIRTY_FLAG_OPTIMIZATION = true;

    /**
     * Recursively write an operation and its children to a buffer
     *
     * @param op the operation to write
     * @param buffer the target buffer
     */
    public static void writeRecursive(@NonNull Operation op, @NonNull WireBuffer buffer) {
        op.write(buffer);
        if (op instanceof Container) {
            for (Operation child : ((Container) op).getList()) {
                writeRecursive(child, buffer);
            }
            ContainerEnd.apply(buffer);
        }
    }

    /** add the operation to the buffer */
    public abstract void write(@NonNull WireBuffer buffer);

    /**
     * paint an operation
     *
     * @param context the paint context used to paint the operation
     */
    public abstract void apply(@NonNull RemoteContext context);

    /**
     * Materialize the operation into the results list during expansion. The default implementation
     * adds the operation to the result list. Macro-specific operations should override this to
     * provide custom behavior.
     *
     * @param context the expansion context
     * @param result the list to add materialized operations to
     * @param loomManager the macro manager
     */
    public void materialize(
            @NonNull ExpansionContext context,
            @NonNull ArrayList<Operation> result,
            @NonNull LoomManager loomManager) {
        result.add(this);
        if (this instanceof Container) {
            Container container = (Container) this;
            context.expandRecursive(container.getList(), result, loomManager);
            container.getList().clear();
            result.add(new ContainerEnd());
        }
    }

    /** Debug utility to display an operation + indentation */
    @NonNull
    public abstract String deepToString(@NonNull String indent);

    private boolean mDirty = true;

    /** Mark the operation as "dirty" to indicate it will need to be re-executed. */
    public void markDirty() {
        mDirty = true;
    }

    /** Mark the operation as "not dirty" */
    public void markNotDirty() {
        if (ENABLE_DIRTY_FLAG_OPTIMIZATION) {
            mDirty = false;
        }
    }

    /**
     * Returns true if the operation is marked as "dirty"
     *
     * @return true if dirty
     */
    public boolean isDirty() {
        if (ENABLE_DIRTY_FLAG_OPTIMIZATION) {
            return mDirty;
        }
        return true;
    }
}
