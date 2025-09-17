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
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Impulse Event To trigger an impulse event. set the startAt time to the
 * context.getAnimationTime() Impluse Operation. This operation execute a list of actions once and
 * the impluseProcess is executed for a fixed duration
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ImpulseOperation extends PaintOperation implements VariableSupport, Container {
    private static final int OP_CODE = Operations.IMPULSE_START;
    private static final String CLASS_NAME = "ImpulseOperation";
    private float mDuration;
    private float mStartAt;
    private float mOutDuration;
    private float mOutStartAt;
    private boolean mInitialPass = true;
    @NonNull public ArrayList<Operation> mList = new ArrayList<>();

    int mIndexVariableId;

    private ImpulseProcess mProcess;

    /**
     * Constructor for a Impulse Operation
     *
     * @param duration the duration of the impluse
     * @param startAt the start time of the impluse
     */
    public ImpulseOperation(float duration, float startAt) {
        mDuration = duration;
        mStartAt = startAt;
        mOutStartAt = startAt;
        mOutDuration = duration;
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (mProcess == null) {
            System.out.println(".....");
            Operation last = mList.get(mList.size() - 1);
            if (last instanceof ImpulseProcess) {
                mProcess = (ImpulseProcess) last;
                mList.remove(last);
            }
        }
        if (Float.isNaN(mStartAt)) {
            context.listensTo(Utils.idFromNan(mStartAt), this);
        }
        if (Float.isNaN(mDuration)) {
            context.listensTo(Utils.idFromNan(mDuration), this);
        }
        for (Operation operation : mList) {
            if (operation instanceof VariableSupport) {
                VariableSupport variableSupport = (VariableSupport) operation;
                variableSupport.registerListening(context);
            }
        }
        if (mProcess != null) {
            mProcess.registerListening(context);
        }
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {

        mOutDuration =
                Float.isNaN(mDuration) ? context.getFloat(Utils.idFromNan(mDuration)) : mDuration;

        mOutStartAt =
                Float.isNaN(mStartAt) ? context.getFloat(Utils.idFromNan(mStartAt)) : mStartAt;
        if (mProcess != null) {
            mProcess.updateVariables(context);
        }
    }

    @NonNull
    @Override
    public ArrayList<Operation> getList() {
        return mList;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mDuration, mStartAt);
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("ImpulseOperation\n");
        for (Operation operation : mList) {
            builder.append(operation);
            builder.append("  startAt: ");
            builder.append(mStartAt);
            builder.append(" duration: ");
            builder.append(mDuration);
            builder.append("\n");
        }
        return builder.toString();
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return (indent != null ? indent : "") + toString();
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        RemoteContext remoteContext = context.getContext();

        if (remoteContext.getAnimationTime() < mOutStartAt + mOutDuration) {
            if (mInitialPass) {
                for (Operation op : mList) {
                    if (op instanceof VariableSupport && op.isDirty()) {
                        ((VariableSupport) op).updateVariables(context.getContext());
                    }
                    remoteContext.incrementOpCount();
                    op.apply(context.getContext());
                }
                mInitialPass = false;
            } else {
                remoteContext.incrementOpCount();
                if (mProcess != null) {
                    mProcess.paint(context);
                }
            }
        } else {
            mInitialPass = true;
        }
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return CLASS_NAME;
    }

    /**
     * Write the operation on the buffer
     *
     * @param buffer
     * @param duration
     * @param startAt
     */
    public static void apply(@NonNull WireBuffer buffer, float duration, float startAt) {
        buffer.start(OP_CODE);
        buffer.writeFloat(duration);
        buffer.writeFloat(startAt);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        float duration = buffer.readFloat();
        float startAt = buffer.readFloat();

        operations.add(new ImpulseOperation(duration, startAt));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Operations", OP_CODE, name())
                .description(
                        "Impulse Operation. This operation execute a list of action for a fixed"
                                + " duration")
                .field(DocumentedOperation.FLOAT, "duration", "How long to last")
                .field(DocumentedOperation.FLOAT, "startAt", "value step");
    }

    /**
     * Calculate and estimate of the number of iterations
     *
     * @return number of loops or 10 if based on variables
     */
    public int estimateIterations() {
        if (Float.isNaN(mDuration)) {
            return 10; // this is a generic estmate if the values are variables;
        }
        return (int) (mDuration * 60);
    }

    /**
     * set the impulse process. This gets executed for the duration of the impulse
     *
     * @param impulseProcess process to be executed every time
     */
    public void setProcess(@NonNull ImpulseProcess impulseProcess) {
        mProcess = impulseProcess;
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("duration", mDuration, mOutDuration)
                .add("startAt", mStartAt, mOutStartAt);
    }
}
