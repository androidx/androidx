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
package androidx.compose.remote.core.operations;

import static androidx.compose.remote.core.operations.Utils.floatToString;

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

import org.jspecify.annotations.NonNull;

import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DrawTweenPath extends PaintOperation implements VariableSupport {
    private static final int OP_CODE = Operations.DRAW_TWEEN_PATH;
    private static final String CLASS_NAME = "DrawTweenPath";
    float mTween;
    float mStart;
    float mStop;
    float mOutTween;
    float mOutStart;
    float mOutStop;
    int mPath1Id;
    int mPath2Id;

    public DrawTweenPath(int path1Id, int path2Id, float tween, float start, float stop) {
        mOutTween = mTween = tween;
        mOutStart = mStart = start;
        mOutStop = mStop = stop;
        mPath1Id = path1Id;
        mPath2Id = path2Id;
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        mOutTween = Float.isNaN(mTween) ? context.getFloat(Utils.idFromNan(mTween)) : mTween;
        mOutStart = Float.isNaN(mStart) ? context.getFloat(Utils.idFromNan(mStart)) : mStart;
        mOutStop = Float.isNaN(mStop) ? context.getFloat(Utils.idFromNan(mStop)) : mStop;
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (Float.isNaN(mTween)) {
            context.listensTo(Utils.idFromNan(mTween), this);
        }
        if (Float.isNaN(mStart)) {
            context.listensTo(Utils.idFromNan(mStart), this);
        }
        if (Float.isNaN(mStop)) {
            context.listensTo(Utils.idFromNan(mStop), this);
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mPath1Id, mPath2Id, mTween, mStart, mStop);
    }

    @NonNull
    @Override
    public String toString() {
        return "DrawTweenPath "
                + mPath1Id
                + " "
                + mPath2Id
                + " "
                + floatToString(mTween, mOutTween)
                + " "
                + floatToString(mStart, mOutStart)
                + " "
                + "- "
                + floatToString(mStop, mOutStop);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int path1Id = buffer.readInt();
        int path2Id = buffer.readInt();
        float tween = buffer.readFloat();
        float start = buffer.readFloat();
        float stop = buffer.readFloat();
        DrawTweenPath op = new DrawTweenPath(path1Id, path2Id, tween, start, stop);
        operations.add(op);
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return "DrawTweenPath";
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return Operations.DRAW_TWEEN_PATH;
    }

    /**
     * add a draw tween path operation to the buffer
     *
     * @param buffer the buffer to add to
     * @param path1Id the first path
     * @param path2Id the second path
     * @param tween the amount of the tween
     * @param start the start sub range to draw
     * @param stop the end of the sub range to draw
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int path1Id,
            int path2Id,
            float tween,
            float start,
            float stop) {
        buffer.start(OP_CODE);
        buffer.writeInt(path1Id);
        buffer.writeInt(path2Id);
        buffer.writeFloat(tween);
        buffer.writeFloat(start);
        buffer.writeFloat(stop);
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Draw Operations", OP_CODE, CLASS_NAME)
                .description("Draw text along path object")
                .field(DocumentedOperation.INT, "pathId1", "id of path 1 high short is flags")
                .field(DocumentedOperation.INT, "pathId2", "id of path 2 high short is flags")
                .field(DocumentedOperation.FLOAT, "tween", "interpolate between the two paths")
                .field(DocumentedOperation.FLOAT, "start", "trim the start of the path")
                .field(DocumentedOperation.FLOAT, "yOffset", "trim the end of the path");
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        context.drawTweenPath(
                getId(mPath1Id, context), getId(mPath2Id, context), mOutTween, mOutStart, mOutStop);
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("path1Id", mPath1Id)
                .add("path2Id", mPath2Id)
                .add("tween", mTween, mOutTween)
                .add("start", mStart, mOutStart)
                .add("stop", mStop, mOutStop);
    }
}
