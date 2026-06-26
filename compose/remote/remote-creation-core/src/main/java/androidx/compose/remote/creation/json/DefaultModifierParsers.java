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
import androidx.compose.remote.creation.actions.Action;
import androidx.compose.remote.creation.actions.ValueFloatChange;
import androidx.compose.remote.creation.actions.ValueFloatExpressionChange;
import androidx.compose.remote.creation.actions.ValueIntegerChange;
import androidx.compose.remote.creation.actions.ValueIntegerExpressionChange;
import androidx.compose.remote.creation.actions.ValueStringChange;
import androidx.compose.remote.creation.dsl.RcFloat;
import androidx.compose.remote.creation.dsl.VerticalScrollRcFloatModifier;
import androidx.compose.remote.creation.modifiers.ClickActionModifier;
import androidx.compose.remote.creation.modifiers.IncludeReferencedOperationsModifier;
import androidx.compose.remote.creation.modifiers.MacroCallModifier;
import androidx.compose.remote.creation.modifiers.TouchActionModifier;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to register default layout modifiers into the JSON parser.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultModifierParsers {
    private DefaultModifierParsers() {}

    /**
     * Register default modifier parsers.
     *
     * @param p the JSON parser instance to register into
     */
    public static void register(RemoteComposeJsonParser p) {
        p.registerModifierParser("padding", (mod, key, recordingModifier, parser) -> {
            Object paddingVal = mod.get(key);
            if (paddingVal instanceof JSONObject) {
                JSONObject po = (JSONObject) paddingVal;
                recordingModifier.padding(
                        (float) po.optDouble("start", 0),
                        (float) po.optDouble("top", 0),
                        (float) po.optDouble("end", 0),
                        (float) po.optDouble("bottom", 0)
                );
            } else if (paddingVal instanceof JSONArray) {
                JSONArray pa = (JSONArray) paddingVal;
                recordingModifier.padding(
                        (float) pa.getDouble(0),
                        (float) pa.getDouble(1),
                        (float) pa.getDouble(2),
                        (float) pa.getDouble(3)
                );
            } else {
                recordingModifier.padding(parser.parseFloat(paddingVal));
            }
        });
        p.registerModifierParser("fillmaxwidth", (mod, key, recordingModifier, parser) -> {
            recordingModifier.fillMaxWidth(parser.parseFloat(mod.get(key)));
        });
        p.registerModifierParser("fillmaxheight", (mod, key, recordingModifier, parser) -> {
            recordingModifier.fillMaxHeight(parser.parseFloat(mod.get(key)));
        });
        p.registerModifierParser("fillmaxsize", (mod, key, recordingModifier, parser) -> {
            recordingModifier.fillMaxSize(parser.parseFloat(mod.get(key)));
        });
        p.registerModifierParser("width", (mod, key, recordingModifier, parser) -> {
            recordingModifier.width(parser.parseFloat(mod.get(key)));
        });
        p.registerModifierParser("height", (mod, key, recordingModifier, parser) -> {
            recordingModifier.height(parser.parseFloat(mod.get(key)));
        });
        p.registerModifierParser("size", (mod, key, recordingModifier, parser) -> {
            float sizeVal = parser.parseFloat(mod.get(key));
            recordingModifier.width(sizeVal);
            recordingModifier.height(sizeVal);
        });
        p.registerModifierParser("background", (mod, key, recordingModifier, parser) -> {
            String bg = mod.getString(key);
            if (bg.startsWith("$colors.") || bg.startsWith("@colors.")) {
                recordingModifier.backgroundId((short) parser.parseColor(bg));
            } else if (RemoteComposeJsonParser.isVariableRef(bg)) {
                recordingModifier.backgroundId(parser.parseColor(bg));
            } else {
                recordingModifier.background(parser.parseColor(bg));
            }
        });
        p.registerModifierParser("weight", (mod, key, recordingModifier, parser) -> {
            recordingModifier.horizontalWeight((float) mod.getDouble(key));
        });
        p.registerModifierParser("horizontalweight", (mod, key, recordingModifier, parser) -> {
            recordingModifier.horizontalWeight((float) mod.getDouble(key));
        });
        p.registerModifierParser("verticalweight", (mod, key, recordingModifier, parser) -> {
            recordingModifier.verticalWeight((float) mod.getDouble(key));
        });
        p.registerModifierParser("border", (mod, key, recordingModifier, parser) -> {
            JSONObject b = mod.getJSONObject(key);
            float width = (float) b.getDouble("width");
            float corner = (float) b.getDouble("cornerRadius");
            int color = parser.parseColor(b.getString("color"));
            int shape = b.optInt("shape", 0);
            recordingModifier.border(width, corner, color, shape);
        });
        p.registerModifierParser("verticalscroll", (mod, key, recordingModifier, parser) -> {
            Object val = mod.get(key);
            if (val instanceof JSONObject) {
                JSONObject obj = (JSONObject) val;
                float pos = parser.parseFloat(obj.get("position"));
                int notches = obj.optInt("notches", 0);
                recordingModifier.verticalScroll(pos, notches);
            } else {
                RcFloat positionRc = new RcFloat(parser.getWriter(), parser.parseFloat(val));
                recordingModifier.then(new VerticalScrollRcFloatModifier(positionRc));
            }
        });
        p.registerModifierParser("horizontalscroll", (mod, key, recordingModifier, parser) -> {
            Object val = mod.get(key);
            if (val instanceof JSONObject) {
                JSONObject obj = (JSONObject) val;
                float pos = parser.parseFloat(obj.get("position"));
                int notches = obj.optInt("notches", 0);
                recordingModifier.horizontalScroll(pos, notches);
            } else {
                recordingModifier.horizontalScroll(parser.parseFloat(val));
            }
        });
        p.registerModifierParser("collapsiblepriority", (mod, key, recordingModifier, parser) -> {
            Object val = mod.get(key);
            int orientation = 0;
            float priority = 0f;
            if (val instanceof JSONObject) {
                JSONObject obj = (JSONObject) val;
                String orientStr = obj.optString("orientation", "horizontal");
                orientation = orientStr.equalsIgnoreCase("vertical") ? 1 : 0;
                priority = (float) obj.optDouble("priority", 0.0);
            } else if (val instanceof JSONArray) {
                JSONArray arr = (JSONArray) val;
                orientation = arr.getInt(0);
                priority = (float) arr.getDouble(1);
            } else {
                priority = parser.parseFloat(val);
            }
            recordingModifier.collapsiblePriority(orientation, priority);
        });
        p.registerModifierParser("widthin", (mod, key, recordingModifier, parser) -> {
            JSONArray wi = mod.getJSONArray(key);
            recordingModifier.widthIn(
                    parser.parseFloat(wi.get(0)),
                    parser.parseFloat(wi.get(1))
            );
        });
        p.registerModifierParser("heightin", (mod, key, recordingModifier, parser) -> {
            JSONArray hi = mod.getJSONArray(key);
            recordingModifier.heightIn(
                    parser.parseFloat(hi.get(0)),
                    parser.parseFloat(hi.get(1))
            );
        });
        p.registerModifierParser("clip", (mod, key, recordingModifier, parser) -> {
            recordingModifier.clip(parser.parseShape(mod.getJSONObject(key)));
        });
        p.registerModifierParser("id", (mod, key, recordingModifier, parser) -> {
            recordingModifier.componentId(mod.getInt(key));
        });
        p.registerModifierParser("includemacro", (mod, key, recordingModifier, parser) -> {
            org.json.JSONObject callObj = mod.getJSONObject(key);
            String name = callObj.getString("pattern");
            int patternId = parser.getWriter().textCreateId(name);
            JSONArray args = callObj.getJSONArray("arguments");
            int[] argIds = new int[args.length()];
            for (int i = 0; i < args.length(); i++) {
                argIds[i] = parser.resolveTextId(args.get(i));
            }
            recordingModifier.then(new MacroCallModifier(patternId, argIds));
        });
        p.registerModifierParser("include", (mod, key, recordingModifier, parser) -> {
            int styleId = parser.resolveTextId(mod.get(key));
            recordingModifier.then(new IncludeReferencedOperationsModifier(styleId));
        });
        p.registerModifierParser("onclick", (mod, key, recordingModifier, parser) -> {
            recordingModifier.then(new ClickActionModifier(parseActions(mod.get(key), parser)));
        });
        p.registerModifierParser("ontouchdown", (mod, key, recordingModifier, parser) -> {
            recordingModifier.then(new TouchActionModifier(
                    TouchActionModifier.DOWN, parseActions(mod.get(key), parser)));
        });
        p.registerModifierParser("ontouchup", (mod, key, recordingModifier, parser) -> {
            recordingModifier.then(new TouchActionModifier(
                    TouchActionModifier.UP, parseActions(mod.get(key), parser)));
        });
        p.registerModifierParser("ontouchcancel", (mod, key, recordingModifier, parser) -> {
            recordingModifier.then(new TouchActionModifier(
                    TouchActionModifier.CANCEL, parseActions(mod.get(key), parser)));
        });
        p.registerModifierParser("drawwithcontent",
                (mod, key, recordingModifier, parser) -> {
                    recordingModifier.drawWithContent();
                });
    }

    private static List<Action> parseActions(
            Object clickVal, RemoteComposeJsonParser parser) throws JSONException {
        List<Action> actions = new ArrayList<>();
        if (clickVal instanceof JSONArray) {
            JSONArray arr = (JSONArray) clickVal;
            for (int i = 0; i < arr.length(); i++) {
                actions.add(parseAction(arr.getJSONObject(i), parser));
            }
        } else if (clickVal instanceof JSONObject) {
            actions.add(parseAction((JSONObject) clickVal, parser));
        }
        return actions;
    }

    private static Action parseAction(
            JSONObject obj, RemoteComposeJsonParser parser) throws JSONException {
        String type = obj.getString("type");
        int targetId = parser.resolveTextId(obj.get("targetId"));
        switch (type) {
            case "ValueFloatExpressionChange": {
                float valNan = parser.parseFloat(obj.get("value"));
                int valId = androidx.compose.remote.core.operations.Utils.idFromNan(valNan);
                return new ValueFloatExpressionChange(targetId, valId);
            }
            case "ValueFloatChange": {
                float val = parser.parseFloat(obj.get("value"));
                return new ValueFloatChange(targetId, val);
            }
            case "ValueIntegerChange": {
                int val = obj.getInt("value");
                return new ValueIntegerChange(targetId, val);
            }
            case "ValueIntegerExpressionChange": {
                long val = obj.getLong("value");
                return new ValueIntegerExpressionChange(targetId, val);
            }
            case "ValueStringChange": {
                Object valObj = obj.get("value");
                if (valObj instanceof String) {
                    String s = (String) valObj;
                    if (RemoteComposeJsonParser.isVariableRef(s)) {
                        int strId = parser.resolveTextId(s);
                        return new ValueStringChange(targetId, strId);
                    } else {
                        return new ValueStringChange(targetId, s);
                    }
                } else {
                    int strId = parser.resolveTextId(valObj);
                    return new ValueStringChange(targetId, strId);
                }
            }
            default:
                throw new JSONException("Unknown action type: " + type);
        }
    }
}
