/*
 * Copyright (C) 2025 The Android Open Source Project
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
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** WakeIn operation */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WakeIn extends PaintOperation implements VariableSupport, Serializable {
    private static final int OP_CODE = Operations.WAKE_IN;
    private static final String CLASS_NAME = "WakeIn";

    float mWake;
    float mWakeOut;

    String mLastString;

    public static final int ANCHOR_TEXT_RTL = 1;
    public static final int ANCHOR_MONOSPACE_MEASURE = 2;
    public static final int MEASURE_EVERY_TIME = 4;

    public WakeIn(float wake) {
        mWake = wake;
        mWakeOut = wake;
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        mWakeOut = Float.isNaN(mWake) ? context.getFloat(Utils.idFromNan(mWake)) : mWake;
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (Float.isNaN(mWake)) {
            context.listensTo(Utils.idFromNan(mWake), this);
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mWake);
    }

    @NonNull
    @Override
    public String toString() {
        return "DrawTextAnchored [" + Utils.floatToString(mWake, mWakeOut);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        float wake = buffer.readFloat();
        WakeIn op = new WakeIn(wake);
        operations.add(op);
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
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return OP_CODE;
    }

    /**
     * Writes out the operation to the buffer
     *
     * @param buffer the buffer to write to
     * @param wake the value to write
     */
    public static void apply(@NonNull WireBuffer buffer, float wake) {
        buffer.start(OP_CODE);
        buffer.writeFloat(wake);
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Draw Operations", OP_CODE, CLASS_NAME)
                .description("Wake up the render loop after a certain amount of time")
                .field(DocumentedOperation.FLOAT, "wakeSec", "The time in seconds to wake up");
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        context.wakeIn(mWakeOut);
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("wake", mWake);
    }
}
