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
import static org.junit.Assert.assertTrue;

import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.operations.utilities.DataMap;
import androidx.compose.remote.core.types.LongConstant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import java.util.HashMap;

public class TimeAttributeTest {

    private static class TestRemoteContext extends RemoteContext {
        private final HashMap<Integer, Object> mObjects = new HashMap<>();

        TestRemoteContext() {
            super();
        }

        @Override
        public void setPaintContext(@NonNull PaintContext paintContext) {
            super.setPaintContext(paintContext);
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
        public void loadColor(int id, int color) {}

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
        public int getColor(int id) {
            return 0;
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
        float mLastWakeIn = Float.NaN;

        TestPaintContext(@NonNull RemoteContext context) {
            super(context);
        }

        @Override
        public void wakeIn(float seconds) {
            super.wakeIn(seconds);
            mLastWakeIn = seconds;
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
        public void getTextBounds(
                int textId, int start, int end, int flags, float @NonNull [] bounds) {}

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
                int textId,
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
                int path1Id, int path2Id, float tween, float start, float end) {}

        @Override
        public void tweenPath(int out, int path1, int path2, float tween) {}

        @Override
        public void combinePath(int out, int path1, int path2, byte operation) {}

        @Override
        public void applyPaint(@NonNull PaintBundle mPaintData) {}

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
        public void startGraphicsLayer(int w, int h) {}

        @Override
        public void setGraphicsLayer(@NonNull HashMap<Integer, Object> attributes) {}

        @Override
        public void endGraphicsLayer() {}

        @Override
        public @Nullable String getText(int id) {
            return null;
        }

        @Override
        public void matrixFromPath(int pathId, float fraction, float vOffset, int flags) {}

        @Override
        public void drawToBitmap(int bitmapId, int mode, int color) {}
    }

    @Test
    public void paint_timeFromNowSec_callsWakeIn1s_noNeedsRepaint() {
        TestRemoteContext remoteContext = new TestRemoteContext();
        TestPaintContext paintContext = new TestPaintContext(remoteContext);
        remoteContext.setPaintContext(paintContext);

        TimeAttribute attr =
                new TimeAttribute(
                        1, 2, (short) TimeAttribute.TIME_FROM_NOW_SEC, null);
        attr.paint(paintContext);

        assertEquals(1f, paintContext.mLastWakeIn, 0.0f);
        assertFalse(paintContext.doesNeedsRepaint());
    }

    @Test
    public void paint_timeFromArgSec_callsWakeIn1s_noNeedsRepaint() {
        TestRemoteContext remoteContext = new TestRemoteContext();
        TestPaintContext paintContext = new TestPaintContext(remoteContext);
        remoteContext.setPaintContext(paintContext);

        remoteContext.putObject(10, new LongConstant(10, 1000L));
        TimeAttribute attr =
                new TimeAttribute(
                        1, 2, (short) TimeAttribute.TIME_FROM_ARG_SEC, new int[] {10});
        attr.paint(paintContext);

        assertEquals(1f, paintContext.mLastWakeIn, 0.0f);
        assertFalse(paintContext.doesNeedsRepaint());
    }

    @Test
    public void paint_timeFromLoadSec_callsWakeIn_noNeedsRepaint() {
        TestRemoteContext remoteContext = new TestRemoteContext();
        TestPaintContext paintContext = new TestPaintContext(remoteContext);
        remoteContext.setPaintContext(paintContext);

        TimeAttribute attr =
                new TimeAttribute(
                        1, 2, (short) TimeAttribute.TIME_FROM_LOAD_SEC, null);
        attr.paint(paintContext);

        assertTrue(paintContext.mLastWakeIn > 0f && paintContext.mLastWakeIn <= 1f);
        assertFalse(paintContext.doesNeedsRepaint());
    }

    @Test
    public void paint_timeFromNowMin_callsWakeInToNextMinute_noNeedsRepaint() {
        TestRemoteContext remoteContext = new TestRemoteContext();
        TestPaintContext paintContext = new TestPaintContext(remoteContext);
        remoteContext.setPaintContext(paintContext);

        TimeAttribute attr =
                new TimeAttribute(
                        1, 2, (short) TimeAttribute.TIME_FROM_NOW_MIN, null);
        attr.paint(paintContext);

        assertEquals(60f, paintContext.mLastWakeIn, 0.0f);
        assertFalse(paintContext.doesNeedsRepaint());
    }

    @Test
    public void paint_timeFromNowMin_withOffset_wakesInFractionOfMinute() {
        TestRemoteContext remoteContext = new TestRemoteContext();
        TestPaintContext paintContext = new TestPaintContext(remoteContext);
        remoteContext.setPaintContext(paintContext);

        long nowMillis = paintContext.getClock().snapshot(null).getMillis();
        // 45 seconds into the future -> delta = 45s, fractional minute = 0.75,
        // remaining is 15 seconds
        remoteContext.putObject(2, new LongConstant(2, nowMillis + 45000L));
        TimeAttribute attr =
                new TimeAttribute(
                        1, 2, (short) TimeAttribute.TIME_FROM_NOW_MIN, null);
        attr.paint(paintContext);

        assertEquals(15f, paintContext.mLastWakeIn, 0.1f);
        assertFalse(paintContext.doesNeedsRepaint());
    }

    @Test
    public void paint_timeFromArgMin_doesNotCallWakeInOrNeedsRepaint() {
        TestRemoteContext remoteContext = new TestRemoteContext();
        TestPaintContext paintContext = new TestPaintContext(remoteContext);
        remoteContext.setPaintContext(paintContext);

        remoteContext.putObject(10, new LongConstant(10, 1000L));
        TimeAttribute attr =
                new TimeAttribute(
                        1, 2, (short) TimeAttribute.TIME_FROM_ARG_MIN, new int[] {10});
        attr.paint(paintContext);

        assertTrue(Float.isNaN(paintContext.mLastWakeIn));
        assertFalse(paintContext.doesNeedsRepaint());
    }

    @Test
    public void paint_timeFromNowHr_doesNotCallWakeInOrNeedsRepaint() {
        TestRemoteContext remoteContext = new TestRemoteContext();
        TestPaintContext paintContext = new TestPaintContext(remoteContext);
        remoteContext.setPaintContext(paintContext);

        TimeAttribute attr =
                new TimeAttribute(
                        1, 2, (short) TimeAttribute.TIME_FROM_NOW_HR, null);
        attr.paint(paintContext);

        assertTrue(Float.isNaN(paintContext.mLastWakeIn));
        assertFalse(paintContext.doesNeedsRepaint());
    }
}
