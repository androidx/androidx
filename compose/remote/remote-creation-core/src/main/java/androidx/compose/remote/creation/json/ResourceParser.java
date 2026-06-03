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
import androidx.compose.remote.core.operations.NamedVariable;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.creation.RemoteComposeWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Layout resources parsing engine (colors, paths/vectors, variables, matrices).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ResourceParser {
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
            parseResourceByKey(resources, "matrices");
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
                        return;
                    } else if (s.equals("height")) {
                        float val = mWriter.addComponentHeightValue();
                        mWriter.setFloatName(Utils.idFromNan(val), name);
                        mParser.mVariables.put(name, val);
                        return;
                    } else if (s.equals("fontSize")) {
                        float val = Utils.asNan(RemoteContext.ID_FONT_SIZE);
                        mWriter.setFloatName(Utils.idFromNan(val), name);
                        mParser.mVariables.put(name, val);
                        return;
                    }
                }
                float val = mParser.parseFloat(value);
                if (Float.isNaN(val)) {
                    mWriter.setFloatName(Utils.idFromNan(val), name);
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
                            return;
                        }
                    }
                    id = mWriter.addNamedColor(name, mParser.parseColor(value));
                    mParser.mColors.put(name, id);
                });
                break;
            case "paths":
                parseOrderedResource(resources, key, (obj, name, value) -> {
                    int id;
                    if (value instanceof JSONArray) {
                        JSONArray arr = (JSONArray) value;
                        List<Float> path = new ArrayList<>();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject op = arr.getJSONObject(i);
                            String opType = op.getString("type");
                            switch (opType) {
                                case "moveTo":
                                    path.add(Utils.asNan(10)); // MOVE
                                    path.add((float) op.getDouble("x"));
                                    path.add((float) op.getDouble("y"));
                                    break;
                                case "lineTo":
                                    path.add(Utils.asNan(11)); // LINE
                                    path.add((float) op.getDouble("x"));
                                    path.add((float) op.getDouble("y"));
                                    break;
                                case "quadTo":
                                    path.add(Utils.asNan(12)); // QUAD
                                    path.add((float) op.getDouble("x1"));
                                    path.add((float) op.getDouble("y1"));
                                    path.add((float) op.getDouble("x2"));
                                    path.add((float) op.getDouble("y2"));
                                    break;
                                case "cubicTo":
                                    path.add(Utils.asNan(13)); // CUBIC
                                    path.add((float) op.getDouble("x1"));
                                    path.add((float) op.getDouble("y1"));
                                    path.add((float) op.getDouble("x2"));
                                    path.add((float) op.getDouble("y2"));
                                    path.add((float) op.getDouble("x3"));
                                    path.add((float) op.getDouble("y3"));
                                    break;
                                case "close":
                                    path.add(Utils.asNan(14)); // CLOSE
                                    break;
                            }
                        }
                        float[] pathData = new float[path.size()];
                        for (int i = 0; i < path.size(); i++) pathData[i] = path.get(i);
                        id = mWriter.addPathData(pathData);
                    } else {
                        id = mWriter.addPathString((String) value);
                    }
                    mParser.mPaths.put(name, id);
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
                        if (vo.has("type") && vo.getString("type").equals("textFromFloat")) {
                            float val = mParser.parseFloat(vo.get("value"));
                            int after = vo.optInt("after", 3);
                            int before = vo.optInt("before", 0);
                            int flags = vo.optInt("flags", 0);
                            int textId = mWriter.createTextFromFloat(val, before, after, flags);
                            mParser.mVariables.put(name, (float) textId);
                            if (named) {
                                mWriter.setStringName(textId, name);
                            }
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
                            return;
                        } else if (s.equals("height")) {
                            float val = mWriter.addComponentHeightValue();
                            if (named) {
                                mWriter.setFloatName(Utils.idFromNan(val), name);
                            }
                            mParser.mVariables.put(name, val);
                            return;
                        } else if (s.equals("fontSize")) {
                            float val = Utils.asNan(RemoteContext.ID_FONT_SIZE);
                            if (named) {
                                mWriter.setFloatName(Utils.idFromNan(val), name);
                            }
                            mParser.mVariables.put(name, val);
                            return;
                        }
                    }
                    float val = mParser.parseFloat(valObj);
                    if (!Float.isNaN(val)) {
                        if (named) {
                            mParser.mVariables.put(name, mWriter.addNamedFloat(name, val));
                        } else {
                            mParser.mVariables.put(name, mWriter.addFloatConstant(val));
                        }
                    } else {
                        if (named) {
                            mWriter.setFloatName(Utils.idFromNan(val), name);
                        }
                        mParser.mVariables.put(name, val);
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
        }
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
        if (key.equalsIgnoreCase("matrices")) return "matrices";
        if (key.equalsIgnoreCase("floatArrays")) return "floatArrays";
        return key;
    }
}
