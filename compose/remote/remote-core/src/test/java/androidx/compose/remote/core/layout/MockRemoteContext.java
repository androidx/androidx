/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.core.layout;

import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.Platform;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.operations.FloatExpression;
import androidx.compose.remote.core.operations.PathData;
import androidx.compose.remote.core.operations.ShaderData;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.operations.utilities.DataMap;
import androidx.compose.remote.core.operations.utilities.IntFloatMap;
import androidx.compose.remote.core.operations.utilities.IntIntMap;
import androidx.compose.remote.core.operations.utilities.IntMap;
import androidx.compose.remote.core.types.LongConstant;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;

public class MockRemoteContext extends RemoteContext {
    private boolean mHideString = true;
    public StringBuilder stringBuilder = new StringBuilder();
    public IntFloatMap floatCache = new IntFloatMap();
    public IntIntMap integerCache = new IntIntMap();
    public int[] colorCache = new int[200];
    public HashMap<Integer, String> stringCache = new HashMap<>(200);
    public IntMap<DataMap> dataMapCache = new IntMap<>();
    public final IntMap<Object> mObjectMap = new IntMap<>();
    public final IntMap<float[]> mPathDataMap = new IntMap<>();
    public final IntIntMap mPathWindingMap = new IntIntMap();
    public HashMap<String, Integer> varNamesMap = new HashMap<>(200);

    @SuppressWarnings("unchecked")
    public MockRemoteContext() {
        super();
        mPaintContext = new PaintContext(this) {
            @Override
            public void drawBitmap(int imageId, int srcLeft, int srcTop, int srcRight,
                    int srcBottom, int dstLeft, int dstTop, int dstRight, int dstBottom, int cdId) {
                stringBuilder.append("drawBitmap <").append(imageId).append(">\n");
            }

            @Override
            public void drawBitmap(int id, float left, float top, float right, float bottom) {
                stringBuilder.append("drawBitmap (").append(id).append(", ").append(left).append(
                        ", ").append(top).append(", ").append(right).append(", ").append(
                        bottom).append(")\n");
            }

            @Override
            public void scale(float scaleX, float scaleY) {
                stringBuilder.append("scale (").append(scaleX).append(", ").append(scaleY).append(
                        ")\n");
            }

            @Override
            public void translate(float translateX, float translateY) {
                stringBuilder.append("translate (").append(translateX).append(", ").append(
                        translateY).append(")\n");
            }

            @Override
            public void drawArc(float left, float top, float right, float bottom, float startAngle,
                    float sweepAngle) {
                stringBuilder.append("drawArc(").append(left).append(", ").append(top).append(
                        ", ").append(right).append(", ").append(bottom).append(", ").append(
                        startAngle).append(", ").append(sweepAngle).append(")\n");
            }

            @Override
            public void drawSector(float left, float top, float right, float bottom,
                    float startAngle, float sweepAngle) {
                stringBuilder.append("drawSector(").append(left).append(", ").append(top).append(
                        ", ").append(right).append(", ").append(bottom).append(", ").append(
                        startAngle).append(", ").append(sweepAngle).append(")\n");
            }

            @Override
            public void drawCircle(float centerX, float centerY, float radius) {
                stringBuilder.append("drawCircle(").append(centerX).append(", ").append(
                        centerY).append(", ").append(radius).append(")\n");
            }

            @Override
            public void drawLine(float x1, float y1, float x2, float y2) {
                stringBuilder.append("drawLine(").append(x1).append(", ").append(y1).append(
                        ", ").append(x2).append(", ").append(y2).append(")\n");
            }

            @Override
            public void drawOval(float left, float top, float right, float bottom) {
                stringBuilder.append("drawOval(").append(left).append(", ").append(top).append(
                        ", ").append(right).append(", ").append(bottom).append(")\n");
            }

            @Override
            public void drawPath(int id, float start, float end) {
                stringBuilder.append("drawPath(").append(id).append(", ").append(start).append(
                        ", ").append(end).append(")\n");
            }

            @Override
            public void drawRect(float left, float top, float right, float bottom) {
                stringBuilder.append("drawRect(").append(left).append(", ").append(top).append(
                        ", ").append(right).append(", ").append(bottom).append(")\n");
            }

            @Override
            public void savePaint() {
                stringBuilder.append("savePaint\n");
            }

            @Override
            public void restorePaint() {
                stringBuilder.append("restorePaint\n");
            }

            @Override
            public void replacePaint(@NonNull PaintBundle paint) {
                stringBuilder.append("replacePaint\n");
            }

            @Override
            public void drawRoundRect(float left, float top, float right, float bottom,
                    float radiusX, float radiusY) {
                stringBuilder.append("drawRoundRect(").append(left).append(", ").append(top).append(
                        ", ").append(right).append(", ").append(bottom).append(", ").append(
                        radiusX).append(", ").append(radiusY).append(")\n");
            }

            @Override
            public void drawTextOnPath(int textId, int pathId, float hOffset, float vOffset) {
                stringBuilder.append("drawTextOnPath(").append(textId).append(", ").append(
                        pathId).append(", ").append(hOffset).append(", ").append(vOffset).append(
                        ")\n");
            }

            @Override
            public void getTextBounds(int textId, int start, int end, int flags,
                    float @NonNull [] bounds) {
                bounds[0] = 0f;
                bounds[1] = 0f;
                bounds[2] = 100f;
                bounds[3] = 10f;
                stringBuilder.append("getTextBounds(").append(textId).append(", ").append(
                        start).append(", ").append(end).append(")\n");
            }

            @Override
            public Platform.ComputedTextLayout layoutComplexText(int textId, int start, int end,
                    int alignment, int overflow, int maxLines, float maxWidth, int flags) {
                stringBuilder.append("layoutComplexText(").append(textId).append(", ").append(
                        start).append(", ").append(end).append(")\n");
                return null;
            }

            @Override
            public void drawTextRun(int textId, int start, int end, int contextStart,
                    int contextEnd, float x, float y, boolean rtl) {
                stringBuilder.append("drawTextRun(").append(textId).append(", ").append(
                        start).append(", ").append(end).append(", ").append(contextStart).append(
                        ", ").append(contextEnd).append(", ").append(x).append(", ").append(
                        y).append(")\n");
            }

            @Override
            public void drawComplexText(Platform.ComputedTextLayout computedTextLayout) {
                throw new UnsupportedOperationException("Not yet implemented");
            }

            @Override
            public void drawTweenPath(int path1Id, int path2Id, float tween, float start,
                    float stop) {
                stringBuilder.append("drawTweenPath(").append(path1Id).append(", ").append(
                        path2Id).append(", ").append(tween).append(", ").append(start).append(
                        ", ").append(stop).append(")\n");
            }

            @Override
            public void tweenPath(int out, int path1, int path2, float tween) {
                stringBuilder.append("tweenPath(").append(out).append(", ").append(path1).append(
                        ", ").append(path2).append(", ").append(tween).append(")\n");
            }

            @Override
            public void combinePath(int out, int path1, int path2, byte operation) {
                stringBuilder.append("combinePath(").append(out).append(", ").append(path1).append(
                        ", ").append(path2).append(", ").append(operation).append(")\n");
            }

            @Override
            public void applyPaint(@NonNull PaintBundle paintData) {
                stringBuilder.append("paintData(").append(paintData).append(")\n");
            }

            @Override
            public void matrixScale(float scaleX, float scaleY, float centerX, float centerY) {
                if (Float.isNaN(centerX)) {
                    stringBuilder.append("scale(").append(scaleX).append(", ").append(
                            scaleY).append(")\n");
                } else {
                    stringBuilder.append("scale(").append(scaleX).append(", ").append(
                            scaleY).append(", ").append(centerX).append(", ").append(
                            centerY).append(")\n");
                }
            }

            @Override
            public void matrixTranslate(float translateX, float translateY) {
                stringBuilder.append("translate(").append(translateX).append(", ").append(
                        translateY).append(")\n");
            }

            @Override
            public void matrixSkew(float skewX, float skewY) {
                stringBuilder.append("skew(").append(skewX).append(", ").append(skewY).append(
                        ")\n");
            }

            @Override
            public void matrixRotate(float rotate, float pivotX, float pivotY) {
                if (Float.isNaN(pivotX)) {
                    stringBuilder.append("rotate(").append(rotate).append(")\n");
                } else {
                    stringBuilder.append("rotate(").append(rotate).append(", ").append(
                            pivotX).append(", ").append(pivotY).append(")\n");
                }
            }

            @Override
            public void matrixSave() {
                stringBuilder.append("matrixSave()\n");
            }

            @Override
            public void matrixRestore() {
                stringBuilder.append("matrixRestore()\n");
            }

            @Override
            public void clipRect(float left, float top, float right, float bottom) {
                stringBuilder.append("clipRect(").append(left).append(", ").append(top).append(
                        ", ").append(right).append(", ").append(bottom).append(")\n");
            }

            @Override
            public void clipPath(int pathId, int regionOp) {
                stringBuilder.append("clipPath(").append(pathId).append(")\n");
            }

            @Override
            public void reset() {
            }

            @Override
            public void startGraphicsLayer(int w, int h) {
                stringBuilder.append("startGraphicsLayer(").append(w).append(" x ").append(
                        h).append(")\n");
            }

            @Override
            public void setGraphicsLayer(@NonNull HashMap<Integer, Object> attributes) {
                stringBuilder.append("setGraphicsLayer()\n");
            }

            @Override
            public void endGraphicsLayer() {
                stringBuilder.append("endGraphicsLayer()\n");
            }

            @Override
            public String getText(int textID) {
                stringBuilder.append("getText(").append(textID).append(")\n");
                return "<TEST" + textID + ">";
            }

            @Override
            public void roundedClipRect(float width, float height, float topStart, float topEnd,
                    float bottomStart, float bottomEnd) {
                stringBuilder.append("rounded clipRect\n");
            }

            @Override
            public void log(@NonNull String content) {
                stringBuilder.append("log: ").append(content).append("\n");
            }

            @Override
            public void matrixFromPath(int pathId, float fraction, float vOffset, int flags) {
                stringBuilder.append("matrixFromPath(").append(pathId).append(", ").append(
                        fraction).append(", ").append(vOffset).append(", ").append(flags).append(
                        ")");
            }

            @Override
            public void drawToBitmap(int bitmapId, int mode, int color) {
                stringBuilder.append("drawToBitmap(").append(bitmapId).append(")\n");
            }
        };
        mVariableSupport = new ArrayList[400];
    }

    /**
     * Clear debug results
     */
    public void clearResults() {
        stringBuilder.setLength(0);
    }

    /**
     * Get debug results
     * @return
     */
    public String getTestResults() {
        return stringBuilder.toString();
    }

    @Override
    public float getAnimationTime() {
        return 1f;
    }

    public void setHideString(boolean h) {
        mHideString = h;
    }

    @Override
    public void loadPathData(int instanceId, int winding, float @NonNull [] floatPath) {
        if (winding == 0) {
            stringBuilder.append("loadPathData(").append(instanceId).append(")=").append(
                    pathString(floatPath)).append("\n");
        } else {
            stringBuilder.append("loadPathData(").append(instanceId).append(")= [").append(
                    winding).append("]").append(pathString(floatPath)).append("\n");
        }
        mPathDataMap.put(instanceId, floatPath);
        mPathWindingMap.put(instanceId, winding);
    }

    @Override
    public float[] getPathData(int instanceId) {
        stringBuilder.append("getPathData(").append(instanceId).append(")= \n");
        return mPathDataMap.get(instanceId);
    }

    @Override
    public void loadVariableName(@NonNull String varName, int varId, int varType) {
        varNamesMap.put(varName, varId);
        stringBuilder.append("loadVariableName(").append(varName).append(")= [").append(
                varId).append("] ").append(varType).append("\n");
    }

    @Override
    public void loadColor(int id, int color) {
        colorCache[id] = color;
        stringBuilder.append("loadColor([").append(id).append("])= ").append(
                Utils.colorInt(color)).append("\n");
    }

    @Override
    public void setNamedColorOverride(@NonNull String colorName, int color) {
        stringBuilder.append("setNamedColorOverride([").append(colorName).append("])= ").append(
                Utils.colorInt(color)).append("\n");
    }

    @Override
    public void setNamedLong(@NonNull String name, long value) {
        LongConstant lc = (LongConstant) mObjectMap.get(varNamesMap.get(name));
        if (lc != null) {
            lc.setValue(value);
        }
        stringBuilder.append("setNamedLong([").append(name).append("])= ").append(value).append(
                "\n");
    }

    @Override
    public void setNamedStringOverride(@NonNull String stringName, @NonNull String value) {
        stringBuilder.append("setNamedStringOverride([").append(stringName).append("])= ").append(
                value).append("\n");
    }

    @Override
    public void clearNamedStringOverride(@NonNull String stringName) {
        stringBuilder.append("clearNamedStringOverride([").append(stringName).append("])\n");
    }

    @Override
    public void setNamedBooleanOverride(@NonNull String booleanName, boolean value) {
        stringBuilder.append("setNamedBooleanOverride([").append(booleanName).append("])= ").append(
                value).append("\n");
    }

    @Override
    public void clearNamedBooleanOverride(@NonNull String booleanName) {
        stringBuilder.append("clearNamedBooleanOverride([").append(booleanName).append("])\n");
    }

    @Override
    public void setNamedIntegerOverride(@NonNull String integerName, int value) {
        stringBuilder.append("setNamedIntegerOverride([").append(integerName).append("])= ").append(
                value).append("\n");
    }

    @Override
    public void clearNamedIntegerOverride(@NonNull String integerName) {
        stringBuilder.append("clearNamedIntegerOverride([").append(integerName).append("])\n");
    }

    @Override
    public void setNamedFloatOverride(@NonNull String floatName, float value) {
        stringBuilder.append("setNamedFloatOverride([").append(floatName).append("])= ").append(
                value).append("\n");
    }

    @Override
    public void clearNamedFloatOverride(@NonNull String floatName) {
        stringBuilder.append("clearNamedIntegerOverride([").append(floatName).append("])\n");
    }

    @Override
    public void setNamedDataOverride(@NonNull String dataName, @NonNull Object value) {
        stringBuilder.append("setNamedDataOverride([").append(dataName).append("])= ").append(
                value).append("\n");
    }

    @Override
    public void clearNamedDataOverride(@NonNull String dataName) {
        stringBuilder.append("clearNamedDataOverride([").append(dataName).append("])\n");
    }

    @Override
    public void addCollection(int id, @NonNull ArrayAccess collection) {
        Utils.log("add collection **** " + Integer.toHexString(id));
        mRemoteComposeState.addCollection(id, collection);
    }

    @Override
    public void putDataMap(int id, @NonNull DataMap map) {
        dataMapCache.put(id, map);
    }

    @Override
    public DataMap getDataMap(int id) {
        return dataMapCache.get(id);
    }

    public int lastAction = -1;

    @Override
    public void runAction(int id, @NonNull String metadata) {
        lastAction = id;
    }

    @Override
    public void runNamedAction(int textId, Object value) {
    }

    @Override
    public void putObject(int key, @NonNull Object command) {
        mObjectMap.put(key, command);
    }

    @Override
    public Object getObject(int key) {
        return mObjectMap.get(key);
    }

    @Override
    public void hapticEffect(int type) {
        stringBuilder.append("hapticEffect ").append(type).append("\n");
    }

    /**
     * Utility to convert a path of float to a string
     * @param path
     * @return
     */
    public String pathString(float[] path) {
        if (path == null) {
            return "null";
        }
        StringBuilder str = new StringBuilder();
        int last_return = 0;
        for (int i = 0; i < path.length; i++) {
            if (i != 0) {
                str.append(" ");
            }
            if (Float.isNaN(path[i])) {
                if (str.length() - last_return > 65) {
                    str.append("\n");
                    last_return = str.length();
                }
                int id = Utils.idFromNan(path[i]);
                if (id <= PathData.DONE) {
                    switch (id) {
                        case PathData.MOVE:
                            str.append("M");
                            break;
                        case PathData.LINE:
                            str.append("L");
                            break;
                        case PathData.QUADRATIC:
                            str.append("Q");
                            break;
                        case PathData.CONIC:
                            str.append("R");
                            break;
                        case PathData.CUBIC:
                            str.append("C");
                            break;
                        case PathData.CLOSE:
                            str.append("Z");
                            break;
                        case PathData.DONE:
                            str.append(".");
                            break;
                        default:
                            str.append("X");
                            break;
                    }
                } else {
                    str.append("(").append(id).append(")");
                }
            } else {
                str.append(path[i]);
            }
        }
        return str.toString();
    }

    @Override
    public void setTheme(int theme) {
        super.setTheme(theme);
        stringBuilder.append("setTheme(").append(theme).append(")\n");
    }

    @Override
    public void header(int majorVersion, int minorVersion, int patchVersion, int width, int height,
            long capabilities, IntMap<Object> map) {
        loadInteger(ID_WINDOW_WIDTH, width);
        loadInteger(ID_WINDOW_HEIGHT, height);

        stringBuilder.append("header(").append(majorVersion).append(", ").append(
                minorVersion).append(", ").append(patchVersion).append(")");
        stringBuilder.append(" ").append(width).append(" x ").append(height).append(", ").append(
                capabilities).append("\n");
    }

    @Override
    public void loadBitmap(int imageId, short encoding, short type, int width, int height,
            byte @NonNull [] bitmap) {
        stringBuilder.append("loadImage(").append(imageId).append(")\n");
    }

    @Override
    public void loadText(int id, @NonNull String text) {
        stringCache.put(id, text);
        String str = "";
        if (!mHideString) {
            if (text.length() < 10) {
                str = "=\"" + text + "\"";
            } else {
                str = "=\"" + text.substring(0, 7) + "...\"";
            }
        }

        stringBuilder.append("loadText(").append(id).append(")").append(str).append("\n");
    }

    @Override
    public String getText(int id) {
        stringBuilder.append("getText[").append(id).append("]= ").append(
                stringCache.get(id)).append("\n");
        return stringCache.get(id);
    }

    @Override
    public void loadFloat(int id, float value) {
        floatCache.put(id, value);
        integerCache.put(id, (int) value);
        if (!mHideString) {
            stringBuilder.append("loadFloat[").append(id).append("]=").append(value).append("\n");
        }
        ArrayList<Object> list = mVariableSupport[id];
        if (list != null) {
            for (Object v : list) {
                VariableSupport vs = (VariableSupport) v;
                vs.markDirty();
            }
        }
    }

    @Override
    public void overrideFloat(int id, float value) {
        stringBuilder.append("overrideFloat(").append(id).append(")").append(value).append("\n");
    }

    @Override
    public void loadInteger(int id, int value) {
        floatCache.put(id, (float) value);
        integerCache.put(id, value);
        if (!mHideString) {
            stringBuilder.append("loadInteger[").append(id).append("]=").append(value).append("\n");
        }
        warnListeners(id);
    }

    private void warnListeners(int id) {
        ArrayList<Object> list = mVariableSupport[id];
        if (list != null) {
            for (Object v : list) {
                VariableSupport vs = (VariableSupport) v;
                vs.markDirty();
            }
        }
    }

    @Override
    public void overrideInteger(int id, int value) {
        stringBuilder.append("overrideInteger(").append(id).append(")").append(value).append("\n");
        integerCache.put(id, value);
        warnListeners(id);
    }

    @Override
    public void overrideText(int id, int valueId) {
        stringBuilder.append("overrideText(").append(id).append(")").append(valueId).append("\n");
    }

    public FloatExpression[] animatedFloatCache = new FloatExpression[200];

    @Override
    public void loadAnimatedFloat(int id, @NonNull FloatExpression animatedFloat) {
        animatedFloatCache[id] = animatedFloat;
        stringBuilder.append("animatedFloat(").append(id).append(")=").append(animatedFloat).append(
                "\n");
    }

    @Override
    public void loadShader(int id, @NonNull ShaderData value) {
        stringBuilder.append("loadShaderData(").append(id).append(")\n");
    }

    @Override
    public float getFloat(int id) {
        return floatCache.get(id);
    }

    @Override
    public int getInteger(int id) {
        return integerCache.get(id);
    }

    @Override
    public long getLong(int id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int getColor(int id) {
        return colorCache[id];
    }

    public ArrayList<Object>[] mVariableSupport;
    public int[] listeners = new int[400];
    public int listenerCount = 0;

    @Override
    public void listensTo(int id, @NonNull VariableSupport variableSupport) {
        if (mVariableSupport[id] == null) {
            mVariableSupport[id] = new ArrayList<>();
        }
        ArrayList<Object> list = mVariableSupport[id];
        list.add(variableSupport);
        listeners[listenerCount++] = id;
    }

    @Override
    public int updateOps() {
        if (true) { // FIXME -- we don't update ops here in the real player
            return 0;
        }
        for (int c = 0; c < listenerCount; c++) {
            ArrayList<Object> list = mVariableSupport[listeners[c]];
            if (list != null) {
                for (Object v : list) {
                    VariableSupport vs = (VariableSupport) v;
                    vs.updateVariables(this);
                }
            }
        }
        return 0; // TODO map out when to update
    }

    @Override
    public ShaderData getShader(int id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void addClickArea(int id, int contentDescription, float left, float top, float right,
            float bottom, int metadataId) {
        stringBuilder.append("clickArea(").append(id).append(", ").append(left).append(", ").append(
                top).append(", ").append(right).append(", ").append(bottom).append(", ").append(
                metadataId).append(")\n");
    }

    @Override
    public void setRootContentBehavior(int scroll, int alignment, int sizing, int mode) {
        stringBuilder.append("rootContentBehavior ").append(scroll).append(", ").append(
                alignment).append(", ").append(sizing).append(", ").append(mode).append("\n");
        super.setRootContentBehavior(scroll, alignment, sizing, mode);
    }
}
