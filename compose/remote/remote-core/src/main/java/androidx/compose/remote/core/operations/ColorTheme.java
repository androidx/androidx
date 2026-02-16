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

import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.SHORT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * This implement color theme. It supports two colors dark and light modes
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ColorTheme extends Operation implements Serializable, ComponentData {
    private static final int OP_CODE = Operations.COLOR_THEME;
    private static final String CLASS_NAME = "ColorTheme";
    public final int mId;
    public @Nullable String mColorGroupName;
    public int mColorGroupId; // The id of the name of the color group
    public short mDarkModeIndex;
    public short mLightModeIndex;
    public int mDarkModeFallback;
    public int mLightModeFallback;
    private int mCurrentTheme = Theme.UNSPECIFIED;
    public int mDarkMode;
    public int mLightMode;

    public ColorTheme(int id,
            int colorGroupId,
            short lightModeIndex,
            short darkModeIndex,
            int lightModeFallback,
            int darkModeFallback) {
        mId = id;
        mDarkModeIndex = darkModeIndex;
        mLightModeIndex = lightModeIndex;
        mDarkMode = mDarkModeFallback = darkModeFallback;
        mLightMode = mLightModeFallback = lightModeFallback;
        mColorGroupId = colorGroupId;
    }

    /**
     * This is called by the system to set the theme
     *
     * @param theme the theme to set
     */
    public void setTheme(@NonNull RemoteContext context, int theme) {
        if (mId == 44) {
            Utils.logStack(" " + theme, 10);
        } else {
            Utils.log("(" + mId + ") set  " + theme);
        }

        if (mCurrentTheme != theme) {
            Utils.log("(" + mId + ") update  " + theme);
            mCurrentTheme = theme;
            if (Theme.LIGHT == theme) {
                context.loadColor(mId, mLightMode);
            } else {
                context.loadColor(mId, mDarkMode);
            }
        }
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        int theme = context.getPaintTheme();
        if (Theme.LIGHT == theme) {
            context.loadColor(mId, mLightMode);
        } else {
            context.loadColor(mId, mDarkMode);
        }
    }


    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mId, mColorGroupId, mLightModeIndex, mDarkModeIndex, mLightModeFallback,
                mDarkModeFallback);
    }

    @NonNull
    @Override
    public String toString() {
        return "ColorTheme;id=" + mId + ";group=" + mColorGroupName + ";lightId=" + mLightModeIndex
                + ";darkId=" + mDarkModeIndex + ";lightFallback=" + mLightModeFallback
                + ";darkFallback=" + mDarkModeFallback;
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
     * Call to write a ColorExpression object on the buffer
     */
    public static void apply(@NonNull WireBuffer buffer,
            int id,
            int groupId,
            short lightMode,
            short darkMode,
            int lightModeFallback,
            int darkModeFallback) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);
        buffer.writeInt(groupId);
        buffer.writeShort(lightMode);
        buffer.writeShort(darkMode);
        buffer.writeInt(lightModeFallback);
        buffer.writeInt(darkModeFallback);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer     the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.readInt();
        int groupId = buffer.readInt();
        short lightModeIndex = (short) buffer.readShort();
        short darkModeIndex = (short) buffer.readShort();
        int lightModeFallback = buffer.readInt();
        int darkModeFallback = buffer.readInt();
        operations.add(new ColorTheme(id, groupId, lightModeIndex, darkModeIndex, lightModeFallback,
                darkModeFallback));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Paint & Styles Operations", OP_CODE, CLASS_NAME)
                .addedVersion(7)
                .experimental(true)
                .description("Define a color that adapts to the current theme (light/dark)")
                .field(DocumentedOperation.INT, "id", "The ID of the color")
                .field(INT, "groupId", "The ID of the color group name string")
                .field(SHORT, "lightModeIndex", "The ID of the color in the light group")
                .field(SHORT, "darkModeIndex", "The ID of the color in the dark group")
                .field(INT, "lightModeFallback", "32-bit ARGB fallback color for light mode")
                .field(INT, "darkModeFallback", "32-bit ARGB fallback color for dark mode");
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("id", mId);
        serializer.add("group", mColorGroupId);
        serializer.add("lightId", mLightModeIndex);
        serializer.add("darkId", mDarkModeIndex);
        serializer.add("lightFallback", mLightModeFallback);
        serializer.add("darkFallback", mDarkModeFallback);

    }

}
