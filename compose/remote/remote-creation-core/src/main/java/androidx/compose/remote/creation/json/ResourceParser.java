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
package androidx.compose.remote.creation.json;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.BitmapData;
import androidx.compose.remote.core.operations.NamedVariable;
import androidx.compose.remote.core.operations.PathData;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.creation.RemoteComposeWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

/**
 * Layout resources parsing engine (colors, paths/vectors, variables, matrices, bitmaps).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ResourceParser {
    /** The 8 bytes every PNG file starts with. */
    private static final byte[] PNG_SIGNATURE = {
        (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'
    };

    private final RemoteComposeJsonParser mParser;
    private final RemoteComposeWriter mWriter;

    /**
     * Construct a ResourceParser instance.
     *
     * @param parser the parent JSON parser instance
     */
    ResourceParser(@NonNull RemoteComposeJsonParser parser) {
        mParser = parser;
        mWriter = parser.getWriter();
    }

    /**
     * Parse resources from a JSONObject containing resource definitions.
     *
     * @param resources the resources JSONObject to parse
     */
    public void parseResources(@NonNull JSONObject resources) {
        parseResourcesOrdered(resources);
    }

    /**
     * Parse resources in a specific order, resolving globally named resources.
     *
     * @param resources the resources JSONObject containing array definitions
     */
    public void parseResourcesOrdered(@NonNull JSONObject resources) {
        if (resources.has("order")) {
            JSONArray order = resources.getJSONArray("order");
            for (int i = 0; i < order.length(); i++) {
                String key = order.getString(i);
                parseResourceByKey(resources, key);
            }
        } else {
            // Default order
            parseResourceByKey(resources, "v_dims");
            parseResourceByKey(resources, "colors");
            parseResourceByKey(resources, "paths");
            parseResourceByKey(resources, "floatArrays");
            parseResourceByKey(resources, "variables");
            parseResourceByKey(resources, "integers");
            parseResourceByKey(resources, "matrices");
            parseResourceByKey(resources, "bitmaps");
        }
    }

    private void parseResourceByKey(JSONObject resources, String key) throws JSONException {
        if (!resources.has(key)) return;
        if (key.equals("v_dims")) {
            parseOrderedResource(resources, key, (obj, name, value) -> {
                if (value instanceof String) {
                    String s = (String) value;
                    if (s.equals("width")) {
                        float val = mWriter.addComponentWidthValue();
                        mWriter.setFloatName(Utils.idFromNan(val), name);
                        mParser.mVariables.put(name, val);
                        mParser.recordVariable(name, Utils.idFromNan(val));
                        return;
                    } else if (s.equals("height")) {
                        float val = mWriter.addComponentHeightValue();
                        mWriter.setFloatName(Utils.idFromNan(val), name);
                        mParser.mVariables.put(name, val);
                        mParser.recordVariable(name, Utils.idFromNan(val));
                        return;
                    } else if (s.equals("fontSize")) {
                        float val = Utils.asNan(RemoteContext.ID_FONT_SIZE);
                        mWriter.setFloatName(Utils.idFromNan(val), name);
                        mParser.mVariables.put(name, val);
                        mParser.recordVariable(name, RemoteContext.ID_FONT_SIZE);
                        return;
                    }
                }
                float val = mParser.parseFloat(value);
                if (Float.isNaN(val)) {
                    mWriter.setFloatName(Utils.idFromNan(val), name);
                    mParser.recordVariable(name, Utils.idFromNan(val));
                } else {
                    mParser.recordVariable(name, (int) val);
                }
                mParser.mVariables.put(name, val);
            });
            return;
        }
        String type = guessType(key);
        switch (type) {
            case "colors":
                parseOrderedResource(resources, key, (obj, name, value) -> {
                    int id;
                    boolean named = obj == null || obj.optBoolean("export", true);
                    if (value instanceof JSONObject) {
                        JSONObject themed = (JSONObject) value;
                        if (themed.has("light") && themed.has("dark")) {
                            String lightStr = themed.getString("light");
                            String darkStr = themed.getString("dark");
                            short lightId = (short) mParser.parseColor(lightStr);
                            short darkId = (short) mParser.parseColor(darkStr);

                            if ((lightStr.startsWith("$colors.") || lightStr.startsWith("@colors."))
                                    && (darkStr.startsWith("$colors.")
                                    || darkStr.startsWith("@colors."))) {
                                id = mWriter.addThemedColor(lightId, darkId);
                            } else {
                                id = mWriter.addThemedColor(
                                        themed.optString("lightName", null),
                                        mParser.parseColor(lightStr),
                                        themed.optString("darkName", null),
                                        mParser.parseColor(darkStr)
                                );
                            }
                            if (named) {
                                mWriter.setColorName(id, name);
                            }
                            mParser.mColors.put(name, id);
                            mParser.recordVariable(name, id);
                            return;
                        }
                    }
                    id = mWriter.addNamedColor(name, mParser.parseColor(value));
                    mParser.mColors.put(name, id);
                    mParser.recordVariable(name, id);
                });
                break;
            case "paths":
                parseOrderedResource(resources, key, (obj, name, value) -> {
                    int id;
                    if (value instanceof JSONArray) {
                        JSONArray arr = (JSONArray) value;
                        List<Float> path = new ArrayList<>();
                        float cx = 0f;
                        float cy = 0f; // current point
                        float sx = 0f;
                        float sy = 0f; // start of the current subpath, restored by close
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject op = arr.getJSONObject(i);
                            String opType = op.getString("type");
                            switch (opType) {
                                case "moveTo": {
                                    float x = (float) op.getDouble("x");
                                    float y = (float) op.getDouble("y");
                                    path.add(PathData.MOVE_NAN);
                                    path.add(x);
                                    path.add(y);
                                    cx = sx = x;
                                    cy = sy = y;
                                    break;
                                }
                                case "lineTo": {
                                    float x = (float) op.getDouble("x");
                                    float y = (float) op.getDouble("y");
                                    path.add(PathData.LINE_NAN);
                                    path.add(cx);
                                    path.add(cy);
                                    path.add(x);
                                    path.add(y);
                                    cx = x;
                                    cy = y;
                                    break;
                                }
                                case "quadTo": {
                                    float x1 = (float) op.getDouble("x1");
                                    float y1 = (float) op.getDouble("y1");
                                    float x2 = (float) op.getDouble("x2");
                                    float y2 = (float) op.getDouble("y2");
                                    path.add(PathData.QUADRATIC_NAN);
                                    path.add(cx);
                                    path.add(cy);
                                    path.add(x1);
                                    path.add(y1);
                                    path.add(x2);
                                    path.add(y2);
                                    cx = x2;
                                    cy = y2;
                                    break;
                                }
                                case "cubicTo": {
                                    float x1 = (float) op.getDouble("x1");
                                    float y1 = (float) op.getDouble("y1");
                                    float x2 = (float) op.getDouble("x2");
                                    float y2 = (float) op.getDouble("y2");
                                    float x3 = (float) op.getDouble("x3");
                                    float y3 = (float) op.getDouble("y3");
                                    path.add(PathData.CUBIC_NAN);
                                    path.add(cx);
                                    path.add(cy);
                                    path.add(x1);
                                    path.add(y1);
                                    path.add(x2);
                                    path.add(y2);
                                    path.add(x3);
                                    path.add(y3);
                                    cx = x3;
                                    cy = y3;
                                    break;
                                }
                                case "close":
                                    path.add(PathData.CLOSE_NAN);
                                    cx = sx;
                                    cy = sy;
                                    break;
                            }
                        }
                        float[] pathData = new float[path.size()];
                        for (int i = 0; i < path.size(); i++) pathData[i] = path.get(i);
                        id = mWriter.addPathData(pathData);
                    } else {
                        id = mWriter.addPathData(new PathParser((String) value));
                    }
                    mParser.mPaths.put(name, id);
                    mParser.recordVariable(name, id);
                });
                break;
            case "floatArrays":
                parseOrderedResource(resources, key, (obj, name, value) -> {
                    boolean named = obj == null || obj.optBoolean("export", true);
                    JSONArray arr;
                    if (value instanceof JSONObject) {
                        arr = ((JSONObject) value).getJSONArray("value");
                    } else {
                        arr = (JSONArray) value;
                    }
                    float[] data = new float[arr.length()];
                    for (int i = 0; i < arr.length(); i++) {
                        data[i] = (float) arr.getDouble(i);
                    }
                    float id = mWriter.addFloatArray(data);
                    if (named) {
                        mWriter.setNamedVariable(
                                Utils.idFromNan(id),
                                name,
                                NamedVariable.FLOAT_ARRAY_TYPE
                        );
                    }
                    mParser.mVariables.put(name, id);
                    mParser.recordVariable(name, Utils.idFromNan(id));
                });
                break;
            case "integers":
                parseOrderedResource(resources, key, (obj, name, value) -> {
                    boolean named = obj == null || obj.optBoolean("export", true);
                    Object valObj = value;
                    if (value instanceof JSONObject) {
                        JSONObject vo = (JSONObject) value;
                        if (vo.has("export")) {
                            named = vo.optBoolean("export", true);
                        }
                        if (vo.has("value")) {
                            valObj = vo.get("value");
                        }
                    }
                    int intVal = ((Number) valObj).intValue();
                    long intId;
                    if (named) {
                        intId = mWriter.addNamedInt(name, intVal);
                    } else {
                        intId = mWriter.addInteger(intVal);
                    }
                    mParser.mIntegerVariables.put(name, intId);
                    int rawId = (int) (intId & 0xFFFFFFFFL);
                    mParser.mVariables.put(name, Utils.asNan(rawId));
                    mParser.recordVariable(name, rawId);
                });
                break;
            case "variables":
                parseOrderedResource(resources, key, (obj, name, value) -> {
                    boolean named = false;
                    Object valObj = value;
                    if (obj != null) {
                        named = obj.optBoolean("export", false);
                    }
                    if (value instanceof JSONObject) {
                        JSONObject vo = (JSONObject) value;
                        if (vo.has("export")) {
                            named = vo.optBoolean("export", false);
                        }
                        String typeStr = vo.optString("type", "");
                        if (typeStr.equalsIgnoreCase("integer")
                                || typeStr.equalsIgnoreCase("int")) {
                            int intVal = vo.optInt("value", 0);
                            long intId;
                            if (named) {
                                intId = mWriter.addNamedInt(name, intVal);
                            } else {
                                intId = mWriter.addInteger(intVal);
                            }
                            mParser.mIntegerVariables.put(name, intId);
                            int rawId = (int) (intId & 0xFFFFFFFFL);
                            mParser.mVariables.put(name, Utils.asNan(rawId));
                            mParser.recordVariable(name, rawId);
                            return;
                        }
                        if (vo.has("type") && vo.getString("type").equals("textFromFloat")) {
                            float val = mParser.parseFloat(vo.get("value"));
                            short after = RemoteComposeJsonParser.parseDigits(vo, "after", 3);
                            short before = RemoteComposeJsonParser.parseDigits(vo, "before", 0);
                            int flags = vo.optInt("flags", 0);
                            int textId = mWriter.createTextFromFloat(val, before, after, flags);
                            mParser.mVariables.put(name, (float) textId);
                            if (named) {
                                mWriter.setStringName(textId, name);
                            }
                            mParser.recordVariable(name, textId);
                            return;
                        }
                        if (vo.has("value")) {
                            valObj = vo.get("value");
                        }
                    }
                    if (valObj instanceof String) {
                        String s = (String) valObj;
                        if (s.equals("width")) {
                            float val = mWriter.addComponentWidthValue();
                            if (named) {
                                mWriter.setFloatName(Utils.idFromNan(val), name);
                            }
                            mParser.mVariables.put(name, val);
                            mParser.recordVariable(name, Utils.idFromNan(val));
                            return;
                        } else if (s.equals("height")) {
                            float val = mWriter.addComponentHeightValue();
                            if (named) {
                                mWriter.setFloatName(Utils.idFromNan(val), name);
                            }
                            mParser.mVariables.put(name, val);
                            mParser.recordVariable(name, Utils.idFromNan(val));
                            return;
                        } else if (s.equals("fontSize")) {
                            float val = Utils.asNan(RemoteContext.ID_FONT_SIZE);
                            if (named) {
                                mWriter.setFloatName(Utils.idFromNan(val), name);
                            }
                            mParser.mVariables.put(name, val);
                            mParser.recordVariable(name, RemoteContext.ID_FONT_SIZE);
                            return;
                        }
                    }
                    float val = mParser.parseFloat(valObj);
                    if (!Float.isNaN(val)) {
                        if (named) {
                            float fId = mWriter.addNamedFloat(name, val);
                            mParser.mVariables.put(name, fId);
                            mParser.recordVariable(name, Utils.idFromNan(fId));
                        } else {
                            float fId = mWriter.addFloatConstant(val);
                            mParser.mVariables.put(name, fId);
                            mParser.recordVariable(name, Utils.idFromNan(fId));
                        }
                    } else {
                        if (named) {
                            mWriter.setFloatName(Utils.idFromNan(val), name);
                        }
                        mParser.mVariables.put(name, val);
                        mParser.recordVariable(name, Utils.idFromNan(val));
                    }
                });
                break;
            case "matrices":
                parseOrderedResource(resources, key, (obj, name, value) -> {
                    JSONArray expArr = (JSONArray) value;
                    float[] exp = new float[expArr.length()];
                    for (int i = 0; i < expArr.length(); i++) {
                        Object item = expArr.get(i);
                        if (item instanceof String && ((String) item).startsWith("matrix:")) {
                            exp[i] = mParser.parseMatrixOperator(((String) item).substring(7));
                        } else {
                            exp[i] = mParser.parseFloat(item);
                        }
                    }
                    float matrixId = mWriter.matrixExpression(exp);
                    mParser.mMatrices.put(name, matrixId);
                });
                break;
            case "bitmaps":
                parseBitmaps(resources, key);
                break;
        }
    }

    /**
     * Declare the {@code resources.bitmaps} entries: a {@code {"name": source}} map, or a list of
     * {@code {"name": source}}, {@code {"name": ..., "value": source}} or - the shape of the
     * published schema - entries whose source sits next to the name, {@code {"name": "tex",
     * "data": "<base64 PNG>"}}.
     */
    private void parseBitmaps(@NonNull JSONObject resources, @NonNull String key)
            throws JSONException {
        Object bitmaps = resources.get(key);
        if (!(bitmaps instanceof JSONArray)) {
            parseOrderedResource(resources, key, (obj, name, value) -> parseBitmap(name, value));
            return;
        }
        JSONArray entries = (JSONArray) bitmaps;
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            if (!entry.has("name") && entry.length() == 1) {
                String name = entry.keys().next();
                parseBitmap(name, entry.get(name));
            } else if (entry.has("value")) {
                parseBitmap(entry.getString("name"), entry.get("value"));
            } else {
                parseBitmap(entry.getString("name"), entry);
            }
        }
    }

    /**
     * Declare one {@code resources.bitmaps} entry: a PNG image embedded in the document, which
     * {@code texture3D} then refers to by name.
     *
     * <p>The bytes come from inline {@code base64} (or its alias {@code data}, which may also be a
     * {@code data:} URI) or from a {@code file}, resolved against the parser's base directory when
     * relative; a bare string is a file name. The dimensions are read from the PNG header rather
     * than declared, so they cannot disagree with the image.
     */
    private void parseBitmap(@NonNull String name, @NonNull Object value) throws JSONException {
        JSONObject source;
        if (value instanceof String) {
            source = new JSONObject();
            source.put("file", value);
        } else if (value instanceof JSONObject) {
            source = (JSONObject) value;
        } else {
            throw new JSONException("bitmap '" + name + "' needs 'file', 'base64' or 'data'");
        }
        String base64Key = source.has("base64") ? "base64" : source.has("data") ? "data" : null;
        byte[] data;
        if (base64Key != null) {
            String text = source.getString(base64Key);
            int comma = text.indexOf(',');
            if (text.startsWith("data:") && comma >= 0) {
                // A data URI: the payload follows the media type.
                if (!text.substring(0, comma).endsWith(";base64")) {
                    throw new JSONException("bitmap '" + name + "': a data URI must be base64");
                }
                text = text.substring(comma + 1);
            }
            try {
                data = Base64.getMimeDecoder().decode(text);
            } catch (IllegalArgumentException e) {
                throw new JSONException("bitmap '" + name + "': bad base64: " + e.getMessage());
            }
        } else if (source.has("file")) {
            File file = new File(source.getString("file"));
            if (!file.isAbsolute()) {
                file = new File(mParser.mBaseDirectory, file.getPath());
            }
            try {
                data = Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                throw new JSONException("bitmap '" + name + "': cannot read " + file + ": " + e);
            }
        } else {
            throw new JSONException("bitmap '" + name + "' needs 'file', 'base64' or 'data'");
        }
        if (isJpeg(data)) {
            throw new JSONException("bitmap '" + name
                    + "': only PNG is supported, and this is a JPEG; convert it to PNG");
        }
        if (!isPng(data)) {
            throw new JSONException("bitmap '" + name
                    + "': only PNG is supported, and this does not start with a PNG header");
        }
        int width = readInt(data, 16);
        int height = readInt(data, 20);
        if (width < 0 || width > 0xFFFF || height < 0 || height > 0xFFFF) {
            // The wire format packs the type and encoding into the high half of these words.
            throw new JSONException("bitmap '" + name + "': " + width + "x" + height
                    + " does not fit in 16 bits per dimension");
        }
        int id = mWriter.nextId();
        // TYPE_PNG_8888 is the inline PNG type players decode, and what the writer's own bitmap
        // path (RemoteComposeBuffer.storeBitmap) emits; RemoteBitmapDecoder refuses TYPE_PNG
        // with ENCODING_INLINE. This deliberately differs from rcj, which writes TYPE_PNG.
        BitmapData.apply(
                mWriter.getBuffer().getBuffer(),
                id,
                (BitmapData.TYPE_PNG_8888 << 16) | width,
                (BitmapData.ENCODING_INLINE << 16) | height,
                data);
        mParser.mBitmapIds.put(name, id);
    }

    /** Whether {@code data} starts with the PNG signature followed by an IHDR chunk. */
    private static boolean isPng(byte @NonNull [] data) {
        if (data.length < 24) {
            return false;
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (data[i] != PNG_SIGNATURE[i]) {
                return false;
            }
        }
        return data[12] == 'I' && data[13] == 'H' && data[14] == 'D' && data[15] == 'R';
    }

    /** Whether {@code data} starts with a JPEG start-of-image marker. */
    private static boolean isJpeg(byte @NonNull [] data) {
        return data.length >= 3
                && (data[0] & 0xFF) == 0xFF
                && (data[1] & 0xFF) == 0xD8
                && (data[2] & 0xFF) == 0xFF;
    }

    /** The big-endian int at {@code offset}. */
    private static int readInt(byte @NonNull [] data, int offset) {
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    private void parseOrderedResource(JSONObject resources, String key, ResourceHandler handler)
            throws JSONException {
        Object val = resources.get(key);
        if (val instanceof JSONArray) {
            JSONArray arr = (JSONArray) val;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String name;
                Object value;
                JSONObject config = obj;
                if (!obj.has("name") && obj.length() == 1) {
                    name = obj.keys().next();
                    value = obj.get(name);
                    config = value instanceof JSONObject ? (JSONObject) value : null;
                } else {
                    name = obj.getString("name");
                    value = obj.get("value");
                }
                handler.handle(config, name, value);
            }
        } else if (val instanceof JSONObject) {
            JSONObject obj = (JSONObject) val;
            Iterator<String> names = obj.keys();
            while (names.hasNext()) {
                String name = names.next();
                Object value = obj.get(name);
                handler.handle(
                        value instanceof JSONObject ? (JSONObject) value : null,
                        name, value);
            }
        }
    }

    private interface ResourceHandler {
        void handle(JSONObject obj, String name, Object value) throws JSONException;
    }

    private String guessType(String key) {
        if (key.equalsIgnoreCase("colors")) return "colors";
        if (key.equalsIgnoreCase("paths")) return "paths";
        if (key.equalsIgnoreCase("variables")) return "variables";
        if (key.equalsIgnoreCase("integers")) return "integers";
        if (key.equalsIgnoreCase("matrices")) return "matrices";
        if (key.equalsIgnoreCase("floatArrays")) return "floatArrays";
        if (key.equalsIgnoreCase("bitmaps")) return "bitmaps";
        return key;
    }

}
