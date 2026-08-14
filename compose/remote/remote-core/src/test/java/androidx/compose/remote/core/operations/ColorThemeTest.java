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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RcProfiles;
import androidx.compose.remote.core.RemoteClock;
import androidx.compose.remote.core.RemoteComposeBuffer;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.operations.utilities.DataMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

public class ColorThemeTest {

    private static class TestRemoteContext extends RemoteContext {
        private final HashMap<Integer, Object> mObjects = new HashMap<>();
        private final HashMap<Integer, Integer> mColors = new HashMap<>();

        TestRemoteContext() {
            super();
            setPaintContext(new TestPaintContext(this));
        }

        @Override
        public void loadPathData(int instanceId, int winding, float @NonNull [] floatPath) {}

        @Override
        public float @Nullable [] getPathData(int instanceId) {
            return null;
        }

        @Override
        public void loadVariableName(@NonNull String varName, int varId, int varType) {}

        @Override
        public void loadColor(int id, int color) {
            mColors.put(id, color);
            mRemoteComposeState.cacheData(id, (Object) color);
        }

        @Override
        public int getColor(int id) {
            Integer c = mColors.get(id);
            if (c != null) {
                return c;
            }
            return mRemoteComposeState.getColor(id);
        }

        @Override
        public void setNamedColorOverride(@NonNull String colorName, int color) {}

        @Override
        public void setNamedStringOverride(@NonNull String stringName, @NonNull String value) {}

        @Override
        public void clearNamedStringOverride(@NonNull String stringName) {}

        @Override
        public void setNamedBooleanOverride(@NonNull String booleanName, boolean value) {}

        @Override
        public void clearNamedBooleanOverride(@NonNull String booleanName) {}

        @Override
        public void setNamedIntegerOverride(@NonNull String integerName, int value) {}

        @Override
        public void clearNamedIntegerOverride(@NonNull String integerName) {}

        @Override
        public void setNamedFloatOverride(@NonNull String floatName, float value) {}

        @Override
        public void clearNamedFloatOverride(@NonNull String floatName) {}

        @Override
        public void setNamedLong(@NonNull String name, long value) {}

        @Override
        public void setNamedDataOverride(@NonNull String dataName, @NonNull Object value) {}

        @Override
        public void clearNamedDataOverride(@NonNull String dataName) {}

        @Override
        public void addCollection(int id, @NonNull ArrayAccess collection) {}

        @Override
        public void putDataMap(int id, @NonNull DataMap map) {}

        @Override
        public @Nullable DataMap getDataMap(int id) {
            return null;
        }

        @Override
        public void runAction(int id, @NonNull String metadata) {}

        @Override
        public void runNamedAction(int textId, @Nullable Object value) {}

        @Override
        public void putObject(int key, @NonNull Object command) {
            mObjects.put(key, command);
        }

        @Override
        public @Nullable Object getObject(int key) {
            return mObjects.get(key);
        }

        @Override
        public void hapticEffect(int type) {}

        @Override
        public void loadSound(int soundId, byte @NonNull [] data) {}

        @Override
        public void playSound(int soundId) {}

        @Override
        public void loadBitmap(
                int imageId,
                short encoding,
                short type,
                int width,
                int height,
                byte @NonNull [] bitmap) {}

        @Override
        public void loadText(int id, @NonNull String text) {}

        @Override
        public @Nullable String getText(int id) {
            return null;
        }

        @Override
        public void loadFloat(int id, float value) {}

        @Override
        public void overrideFloat(int id, float value) {}

        @Override
        public void loadInteger(int id, int value) {}

        @Override
        public void overrideInteger(int id, int value) {}

        @Override
        public void overrideText(int id, int valueId) {}

        @Override
        public void loadAnimatedFloat(int id, @NonNull FloatExpression animatedFloat) {}

        @Override
        public void loadShader(int id, @NonNull ShaderData value) {}

        @Override
        public float getFloat(int id) {
            return 0f;
        }

        @Override
        public int getInteger(int id) {
            return 0;
        }

        @Override
        public long getLong(int id) {
            return 0L;
        }

        @Override
        public void listensTo(int id, @NonNull VariableSupport variableSupport) {}

        @Override
        public int updateOps() {
            return 0;
        }

        @Override
        public @Nullable ShaderData getShader(int id) {
            return null;
        }

        @Override
        public void addClickArea(
                int id,
                int contentDescriptionId,
                float left,
                float top,
                float right,
                float bottom,
                int metadataId) {}
    }

    private static class TestPaintContext extends PaintContext {
        TestPaintContext(@NonNull RemoteContext context) {
            super(context);
        }

        @Override
        public void drawBitmap(
                int imageId,
                int srcLeft,
                int srcTop,
                int srcRight,
                int srcBottom,
                int dstLeft,
                int dstTop,
                int dstRight,
                int dstBottom,
                int cdId) {}

        @Override
        public void scale(float scaleX, float scaleY) {}

        @Override
        public void translate(float translateX, float translateY) {}

        @Override
        public void drawArc(
                float left,
                float top,
                float right,
                float bottom,
                float startAngle,
                float sweepAngle) {}

        @Override
        public void drawSector(
                float left,
                float top,
                float right,
                float bottom,
                float startAngle,
                float sweepAngle) {}

        @Override
        public void drawBitmap(int id, float left, float top, float right, float bottom) {}

        @Override
        public void drawCircle(float centerX, float centerY, float radius) {}

        @Override
        public void drawLine(float x1, float y1, float x2, float y2) {}

        @Override
        public void drawOval(float left, float top, float right, float bottom) {}

        @Override
        public void drawPath(int id, float start, float end) {}

        @Override
        public void drawRect(float left, float top, float right, float bottom) {}

        @Override
        public void savePaint() {}

        @Override
        public void restorePaint() {}

        @Override
        public void replacePaint(@NonNull PaintBundle paintBundle) {}

        @Override
        public void drawRoundRect(
                float left,
                float top,
                float right,
                float bottom,
                float radiusX,
                float radiusY) {}

        @Override
        public void drawTextOnPath(int textId, int pathId, float hOffset, float vOffset) {}

        @Override
        public void getTextBounds(int textId, int start, int end, int flags, float[] bounds) {}

        @Override
        public RcPlatformServices.@Nullable ComputedTextLayout layoutComplexText(
                int textId,
                int start,
                int end,
                int alignment,
                int overflow,
                int maxLines,
                float maxWidth,
                float maxHeight,
                float letterSpacing,
                float lineHeightAdd,
                float lineHeightMultiplier,
                int lineBreakStrategy,
                int hyphenationFrequency,
                int justificationMode,
                boolean useUnderline,
                boolean strikethrough,
                int flags) {
            return null;
        }

        @Override
        public void drawTextRun(
                int textID,
                int start,
                int end,
                int contextStart,
                int contextEnd,
                float x,
                float y,
                boolean rtl) {}

        @Override
        public void drawComplexText(
                RcPlatformServices.@Nullable ComputedTextLayout computedTextLayout) {}

        @Override
        public void drawTweenPath(
                int path1Id,
                int path2Id,
                float tween,
                float start,
                float stop) {}

        @Override
        public void tweenPath(int path1Id, int path2Id, int path3Id, float tween) {}

        @Override
        public void combinePath(int outPathId, int path1Id, int path2Id, byte operation) {}

        @Override
        public void applyPaint(@NonNull PaintBundle paintBundle) {}

        @Override
        public void matrixScale(float scaleX, float scaleY, float centerX, float centerY) {}

        @Override
        public void matrixTranslate(float translateX, float translateY) {}

        @Override
        public void matrixSkew(float skewX, float skewY) {}

        @Override
        public void matrixRotate(float rotate, float pivotX, float pivotY) {}

        @Override
        public void matrixSave() {}

        @Override
        public void matrixRestore() {}

        @Override
        public void clipRect(float left, float top, float right, float bottom) {}

        @Override
        public void clipPath(int pathId, int regionOp) {}

        @Override
        public void roundedClipRect(
                float width,
                float height,
                float topStart,
                float topEnd,
                float bottomStart,
                float bottomEnd) {}

        @Override
        public void reset() {}

        @Override
        public void startGraphicsLayer(int id, int flag) {}

        @Override
        public void setGraphicsLayer(@NonNull HashMap<Integer, Object> map) {}

        @Override
        public void endGraphicsLayer() {}

        @Override
        public @Nullable String getText(int id) {
            return null;
        }

        @Override
        public void matrixFromPath(int pathId, float progress, float distance, int flags) {}

        @Override
        public void drawToBitmap(int bitmapId, int mode, int color) {}
    }

    @Test
    public void apply_lightTheme_loadsLightModeColorAndClearsDirty() {
        ColorTheme theme = new ColorTheme(10, 42, (short) 1, (short) 2, 0x111111, 0x222222);
        theme.mLightMode = 0xFFAABBCC;
        theme.mDarkMode = 0xFF334455;
        theme.markDirty();
        assertTrue(theme.isDirty());

        TestRemoteContext context = new TestRemoteContext();
        context.setPaintTheme(Theme.LIGHT);

        theme.apply(context);

        assertEquals(0xFFAABBCC, context.getColor(10));
        assertFalse(theme.isDirty());
    }

    @Test
    public void apply_darkTheme_loadsDarkModeColorAndClearsDirty() {
        ColorTheme theme = new ColorTheme(10, 42, (short) 1, (short) 2, 0x111111, 0x222222);
        theme.mLightMode = 0xFFAABBCC;
        theme.mDarkMode = 0xFF334455;
        theme.markDirty();
        assertTrue(theme.isDirty());

        TestRemoteContext context = new TestRemoteContext();
        context.setPaintTheme(Theme.DARK);

        theme.apply(context);

        assertEquals(0xFF334455, context.getColor(10));
        assertFalse(theme.isDirty());
    }

    @Test
    public void setTheme_switchesThemeColors() {
        ColorTheme theme = new ColorTheme(10, 42, (short) 1, (short) 2, 0x111111, 0x222222);
        theme.mLightMode = 0xFFAAAAAA;
        theme.mDarkMode = 0xFF222222;

        TestRemoteContext context = new TestRemoteContext();

        theme.setTheme(context, Theme.LIGHT);
        assertEquals(0xFFAAAAAA, context.getColor(10));

        theme.setTheme(context, Theme.DARK);
        assertEquals(0xFF222222, context.getColor(10));
    }

    @Test
    public void coreDocument_getThemedColors_resolvesColorGroupName_whenTextDataIsBefore() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        buffer.addHeader(
                new short[] {Header.DOC_PROFILES},
                new Object[] {RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL});
        buffer.addText(42, "android");
        buffer.addThemedColor(10, 42, (short) 1, (short) 2, 0x111111, 0x222222);

        CoreDocument doc = new CoreDocument(RemoteClock.SYSTEM);
        doc.initFromBuffer(buffer);

        ArrayList<ColorTheme> list = doc.getThemedColors();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("android", list.get(0).mColorGroupName);
    }

    @Test
    public void coreDocument_getThemedColors_resolvesColorGroupName_whenTextDataIsAfter() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        buffer.addHeader(
                new short[] {Header.DOC_PROFILES},
                new Object[] {RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL});
        buffer.addThemedColor(10, 42, (short) 1, (short) 2, 0x111111, 0x222222);
        buffer.addText(42, "android");

        CoreDocument doc = new CoreDocument(RemoteClock.SYSTEM);
        doc.initFromBuffer(buffer);

        ArrayList<ColorTheme> list = doc.getThemedColors();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("android", list.get(0).mColorGroupName);
    }

    @Test
    public void coreDocument_paint_appliesDirtyColorTheme_onColdStart() {
        RemoteComposeBuffer buffer = new RemoteComposeBuffer();
        buffer.addHeader(
                new short[] {Header.DOC_PROFILES},
                new Object[] {RcProfiles.PROFILE_ANDROIDX | RcProfiles.PROFILE_EXPERIMENTAL});
        int fallbackLight = 0xFFFF00FF; // Magenta fallback
        int fallbackDark = 0xFF00FF00;  // Green fallback
        int mappedLight = 0xFFE0E0E0;
        int mappedDark = 0xFF121212;

        buffer.addText(42, "android");
        buffer.addThemedColor(10, 42, (short) 1, (short) 2, fallbackLight, fallbackDark);

        CoreDocument doc = new CoreDocument(RemoteClock.SYSTEM);
        doc.initFromBuffer(buffer);

        TestRemoteContext context = new TestRemoteContext();
        doc.initializeContext(context);
        doc.applyDataOperations(context);

        // Verify initial applyDataOperations loaded the fallback color
        assertEquals(fallbackLight, context.getColor(10));

        // Simulate ThemeSupport mapping colors and marking dirty
        ArrayList<ColorTheme> themedColors = doc.getThemedColors();
        assertNotNull(themedColors);
        assertEquals(1, themedColors.size());
        ColorTheme colorTheme = themedColors.get(0);
        colorTheme.mLightMode = mappedLight;
        colorTheme.mDarkMode = mappedDark;
        colorTheme.markDirty();

        // Paint with Theme.LIGHT on cold start
        doc.paint(context, Theme.LIGHT);

        // Verify the resolved light color is applied, not the fallback
        assertEquals(mappedLight, context.getColor(10));
    }

    @Test
    public void write_read_roundTrip() {
        WireBuffer wireBuffer = new WireBuffer(128);
        ColorTheme.apply(
                wireBuffer,
                7,
                42,
                (short) 1,
                (short) 2,
                0x123456,
                0x654321);

        wireBuffer.setIndex(0);
        wireBuffer.readByte(); // opcode

        ArrayList<Operation> ops = new ArrayList<>();
        ColorTheme.read(wireBuffer, ops);

        assertEquals(1, ops.size());
        ColorTheme theme = (ColorTheme) ops.get(0);
        assertEquals(7, theme.mId);
        assertEquals(42, theme.mColorGroupId);
        assertEquals((short) 1, theme.mLightModeIndex);
        assertEquals((short) 2, theme.mDarkModeIndex);
        assertEquals(0x123456, theme.mLightModeFallback);
        assertEquals(0x654321, theme.mDarkModeFallback);
    }
}
