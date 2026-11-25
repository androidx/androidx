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

package androidx.compose.remote.core.operations.layout.modifiers;

import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.ClipPath;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Clips the content of a component to an arbitrary path.
 *
 * <p>The path is referenced by id and must be defined earlier in the document (for instance with a
 * {@code DATA_PATH} or {@code PATH_EXPRESSION} operation). Its coordinates are interpreted in the
 * local space of this modifier, i.e. relative to the component's content box after any padding
 * modifiers that precede it in the chain, exactly like the drawing operations of a canvas
 * component. They are not scaled by the document density behaviour.
 *
 * <p>The clip applies to everything painted after this modifier: subsequent modifiers (including
 * click ripples) and the component's content. Like {@link RoundedClipRectModifierOperation}, the
 * clip is intersected with the current clip.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ClipPathModifierOperation extends DecoratorModifierOperation {
    public static final String CLASS_NAME = "ClipPathModifierOperation";
    private static final int OP_CODE = Operations.MODIFIER_CLIP_PATH;

    private final int mPathId;

    public ClipPathModifierOperation(int pathId) {
        mPathId = pathId;
    }

    /** Returns the id of the path used to clip the component. */
    public int getPathId() {
        return mPathId;
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        context.clipPath(mPathId, ClipPath.INTERSECT);
    }

    @Override
    public void layout(
            @NonNull RemoteContext context,
            @NonNull Component component,
            float width,
            float height) {}

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(indent, "CLIP_PATH = [" + mPathId + "]");
    }

    @NonNull
    @Override
    public String toString() {
        return CLASS_NAME + "(" + mPathId + ")";
    }

    @Override
    @NonNull
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mPathId);
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
     * Write the operation to the buffer
     *
     * @param buffer the WireBuffer
     * @param pathId the id of the path to clip with
     */
    public static void apply(@NonNull WireBuffer buffer, int pathId) {
        buffer.start(OP_CODE);
        buffer.writeInt(pathId);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int pathId = buffer.readId();
        operations.add(new ClipPathModifierOperation(pathId));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Modifier Operations", OP_CODE, CLASS_NAME)
                .addedVersion(7)
                .experimental(true)
                .additionalDocumentation("modifier_clip_path")
                .description(
                        "Clip the component's content to the given path. The path coordinates are"
                                + " relative to the component's content box (after any preceding"
                                + " padding), like canvas drawing operations, and are not scaled by"
                                + " density. Subsequent modifiers and the component's content are"
                                + " clipped.")
                .field(INT, "pathId", "The id of the path to clip with");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addTags(SerializeTags.MODIFIER).addType(CLASS_NAME).add("pathId", mPathId);
    }
}
