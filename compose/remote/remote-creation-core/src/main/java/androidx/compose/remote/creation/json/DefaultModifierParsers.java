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
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation;
import androidx.compose.remote.core.semantics.CoreSemantics;
import androidx.compose.remote.creation.actions.Action;
import androidx.compose.remote.creation.actions.HostAction;
import androidx.compose.remote.creation.actions.ValueFloatChange;
import androidx.compose.remote.creation.actions.ValueFloatExpressionChange;
import androidx.compose.remote.creation.actions.ValueIntegerChange;
import androidx.compose.remote.creation.actions.ValueIntegerExpressionChange;
import androidx.compose.remote.creation.actions.ValueStringChange;
import androidx.compose.remote.creation.dsl.RcFloat;
import androidx.compose.remote.creation.dsl.VerticalScrollRcFloatModifier;
import androidx.compose.remote.creation.modifiers.ClickActionModifier;
import androidx.compose.remote.creation.modifiers.ComponentLayoutComputeModifier;
import androidx.compose.remote.creation.modifiers.GraphicsLayerModifier;
import androidx.compose.remote.creation.modifiers.IncludeReferencedOperationsModifier;
import androidx.compose.remote.creation.modifiers.MacroCallModifier;
import androidx.compose.remote.creation.modifiers.MarqueeModifier;
import androidx.compose.remote.creation.modifiers.RippleModifier;
import androidx.compose.remote.creation.modifiers.SemanticsModifier;
import androidx.compose.remote.creation.modifiers.TouchActionModifier;
import androidx.compose.remote.creation.modifiers.VisibilityModifier;
import androidx.compose.remote.creation.modifiers.ZIndexModifier;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
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
                if (pa.length() == 2) {
                    float h = (float) pa.getDouble(0);
                    float v = (float) pa.getDouble(1);
                    recordingModifier.padding(h, v, h, v);
                } else if (pa.length() >= 4) {
                    recordingModifier.padding(
                            (float) pa.getDouble(0),
                            (float) pa.getDouble(1),
                            (float) pa.getDouble(2),
                            (float) pa.getDouble(3)
                    );
                } else if (pa.length() == 1) {
                    recordingModifier.padding((float) pa.getDouble(0));
                }
            } else {
                recordingModifier.padding(parser.parseFloat(paddingVal));
            }
        });
        p.registerModifierParser("fillmaxwidth", (mod, key, recordingModifier, parser) -> {
            Object val = mod.get(key);
            if (val == null || val == org.json.JSONObject.NULL) {
                recordingModifier.fillMaxWidth();
            } else {
                recordingModifier.fillMaxWidth(parser.parseFloat(val));
            }
        });
        p.registerModifierParser("fillmaxheight", (mod, key, recordingModifier, parser) -> {
            Object val = mod.get(key);
            if (val == null || val == org.json.JSONObject.NULL) {
                recordingModifier.fillMaxHeight();
            } else {
                recordingModifier.fillMaxHeight(parser.parseFloat(val));
            }
        });
        p.registerModifierParser("fillmaxsize", (mod, key, recordingModifier, parser) -> {
            Object val = mod.get(key);
            if (val == null || val == org.json.JSONObject.NULL) {
                recordingModifier.fillMaxSize();
            } else {
                recordingModifier.fillMaxSize(parser.parseFloat(val));
            }
        });
        p.registerModifierParser("width", (mod, key, recordingModifier, parser) -> {
            recordingModifier.width(parser.parseFloat(mod.get(key)));
        });
        p.registerModifierParser("height", (mod, key, recordingModifier, parser) -> {
            recordingModifier.height(parser.parseFloat(mod.get(key)));
        });
        p.registerModifierParser("size", (mod, key, recordingModifier, parser) -> {
            Object val = mod.get(key);
            if (val instanceof JSONArray) {
                JSONArray sa = (JSONArray) val;
                recordingModifier.width((float) sa.getDouble(0));
                recordingModifier.height((float) sa.getDouble(1));
            } else {
                float sizeVal = parser.parseFloat(val);
                recordingModifier.width(sizeVal);
                recordingModifier.height(sizeVal);
            }
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
        p.registerModifierParser("requiredwidthin", (mod, key, recordingModifier, parser) -> {
            JSONArray rwi = mod.getJSONArray(key);
            recordingModifier.requiredWidthIn(
                    parser.parseFloat(rwi.get(0)),
                    parser.parseFloat(rwi.get(1))
            );
        });
        p.registerModifierParser("requiredheightin", (mod, key, recordingModifier, parser) -> {
            JSONArray rhi = mod.getJSONArray(key);
            recordingModifier.requiredHeightIn(
                    parser.parseFloat(rhi.get(0)),
                    parser.parseFloat(rhi.get(1))
            );
        });
        p.registerModifierParser("dimensionconstraints", (mod, key, recordingModifier, parser) -> {
            JSONObject dc = mod.getJSONObject(key);
            int type = dc.optInt("type", 0);
            float min = parser.parseFloat(dc.get("min"));
            float max = parser.parseFloat(dc.get("max"));
            recordingModifier.then(
                    new androidx.compose.remote.creation.modifiers.WidthInModifier(
                            type, min, max));
        });
        p.registerModifierParser("clip", (mod, key, recordingModifier, parser) -> {
            recordingModifier.clip(parser.parseShape(mod.get(key)));
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
        p.registerModifierParser("multiclick", (mod, key, recordingModifier, parser) -> {
            JSONObject obj = mod.getJSONObject(key);
            int clickType = obj.optInt("clickType", 0);
            recordingModifier.then(new ClickActionModifier(
                    parseActions(obj.get("actions"), parser), clickType));
        });
        p.registerModifierParser("ontouchdown", (mod, key, recordingModifier, parser) -> {
            recordingModifier.then(new TouchActionModifier(
                    TouchActionModifier.DOWN, parseActions(mod.get(key), parser)));
        });
        p.registerModifierParser("touchdown", (mod, key, recordingModifier, parser) -> {
            recordingModifier.then(new TouchActionModifier(
                    TouchActionModifier.DOWN, parseActions(mod.get(key), parser)));
        });
        p.registerModifierParser("ontouchup", (mod, key, recordingModifier, parser) -> {
            recordingModifier.then(new TouchActionModifier(
                    TouchActionModifier.UP, parseActions(mod.get(key), parser)));
        });
        p.registerModifierParser("touchup", (mod, key, recordingModifier, parser) -> {
            recordingModifier.then(new TouchActionModifier(
                    TouchActionModifier.UP, parseActions(mod.get(key), parser)));
        });
        p.registerModifierParser("ontouchcancel", (mod, key, recordingModifier, parser) -> {
            recordingModifier.then(new TouchActionModifier(
                    TouchActionModifier.CANCEL, parseActions(mod.get(key), parser)));
        });
        p.registerModifierParser("touchcancel", (mod, key, recordingModifier, parser) -> {
            recordingModifier.then(new TouchActionModifier(
                    TouchActionModifier.CANCEL, parseActions(mod.get(key), parser)));
        });
        p.registerModifierParser("drawwithcontent",
                (mod, key, recordingModifier, parser) -> {
                    recordingModifier.drawWithContent();
                });
        p.registerModifierParser("layoutcompute",
                (mod, key, recordingModifier, parser) -> {
                    int computeType = mod.getJSONObject(key).optInt("type", 0);
                    recordingModifier.then(
                            new ComponentLayoutComputeModifier(
                                    computeType, changes -> {}));
                });
        p.registerModifierParser("spacedby", (mod, key, recordingModifier, parser) -> {
            recordingModifier.spacedBy(parser.parseFloat(mod.get(key)));
        });
        p.registerModifierParser("animationspec", (mod, key, recordingModifier, parser) -> {
            recordingModifier.animationSpec(mod.getInt(key));
        });
        p.registerModifierParser("alignbybaseline",
                (mod, key, recordingModifier, parser) -> {
                    recordingModifier.alignByBaseline();
                });
        p.registerModifierParser("fillparentmaxwidth",
                (mod, key, recordingModifier, parser) -> {
                    recordingModifier.fillParentMaxWidth(parser.parseFloat(mod.get(key)));
                });
        p.registerModifierParser("fillparentmaxheight",
                (mod, key, recordingModifier, parser) -> {
                    recordingModifier.fillParentMaxHeight(parser.parseFloat(mod.get(key)));
                });
        p.registerModifierParser("fillparentmaxsize",
                (mod, key, recordingModifier, parser) -> {
                    recordingModifier.fillParentMaxSize(parser.parseFloat(mod.get(key)));
                });
        p.registerModifierParser("graphicslayer",
                (mod, key, recordingModifier, parser) -> {
                    JSONObject gObj = mod.getJSONObject(key);
                    GraphicsLayerModifier gMod = new GraphicsLayerModifier();
                    Iterator<String> keys = gObj.keys();
                    while (keys.hasNext()) {
                        String k = keys.next();
                        int attrId = -1;
                        switch (k.toLowerCase()) {
                            case "scalex": attrId = 0; break;
                            case "scaley": attrId = 1; break;
                            case "rotationz": attrId = 4; break;
                            case "translationx": attrId = 7; break;
                            case "translationy": attrId = 8; break;
                            case "alpha": attrId = 11; break;
                        }
                        if (attrId != -1) {
                            gMod.setFloatAttribute(attrId,
                                    parser.parseFloat(gObj.get(k)));
                        }
                    }
                    recordingModifier.then(gMod);
                });
        p.registerModifierParser("marquee", (mod, key, recordingModifier, parser) -> {
            JSONObject mObj = mod.getJSONObject(key);
            int iterations = mObj.optInt("iterations", Integer.MAX_VALUE);
            int animationMode = mObj.optInt("animationMode", 0);
            float repeatDelay = (float) mObj.optDouble("repeatDelayMillis", 1200);
            float initialDelay = (float) mObj.optDouble("initialDelayMillis", 1200);
            float spacing = (float) mObj.optDouble("spacing", 0);
            float velocity = (float) mObj.optDouble("velocity", 0);
            recordingModifier.then(new MarqueeModifier(iterations, animationMode,
                    repeatDelay, initialDelay, spacing, velocity));
        });
        p.registerModifierParser("ripple", (mod, key, recordingModifier, parser) -> {
            recordingModifier.then(new RippleModifier());
        });
        p.registerModifierParser("semantics", (mod, key, recordingModifier, parser) -> {
            JSONObject sObj = mod.getJSONObject(key);
            CoreSemantics semantics = new CoreSemantics();
            if (sObj.has("contentDescription")) {
                semantics.mContentDescriptionId =
                        parser.resolveTextId(sObj.get("contentDescription"));
            }
            if (sObj.has("text")) {
                semantics.mTextId = parser.resolveTextId(sObj.get("text"));
            }
            if (sObj.has("stateDescription")) {
                semantics.mStateDescriptionId =
                        parser.resolveTextId(sObj.get("stateDescription"));
            }
            semantics.mEnabled = sObj.optBoolean("enabled", true);
            semantics.mClickable = sObj.optBoolean("clickable", false);
            recordingModifier.then(new SemanticsModifier(semantics));
        });
        p.registerModifierParser("visibility", (mod, key, recordingModifier, parser) -> {
            int valId = parser.resolveTextId(mod.get(key));
            recordingModifier.then(new VisibilityModifier(valId));
        });
        p.registerModifierParser("zindex", (mod, key, recordingModifier, parser) -> {
            float z = parser.parseFloat(mod.get(key));
            recordingModifier.then(new ZIndexModifier(z));
        });
        p.registerModifierParser("offset", (mod, key, recordingModifier, parser) -> {
            Object val = mod.get(key);
            if (val instanceof JSONObject) {
                JSONObject obj = (JSONObject) val;
                float x = parser.parseFloat(obj.opt("x"));
                float y = parser.parseFloat(obj.opt("y"));
                recordingModifier.offset(x, y);
            } else if (val instanceof JSONArray) {
                JSONArray arr = (JSONArray) val;
                float x = parser.parseFloat(arr.get(0));
                float y = parser.parseFloat(arr.get(1));
                recordingModifier.offset(x, y);
            }
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
        String type = obj.getString("type").toLowerCase();
        int targetId = obj.has("targetId") ? parser.resolveTextId(obj.get("targetId"))
                : (obj.has("target") ? parser.resolveTextId(obj.get("target")) : -1);
        switch (type) {
            case "hostaction":
            case "hostnamedaction":
            case "hostmetadataaction": {
                if (obj.has("name")) {
                    String name = obj.getString("name");
                    if (obj.has("value")) {
                        int actionType = obj.optInt("actionType",
                                HostNamedActionOperation.STRING_TYPE);
                        int valueId = parser.resolveTextId(obj.get("value"));
                        return new HostAction(name, actionType, valueId);
                    } else {
                        return new HostAction(name);
                    }
                } else {
                    int actionId = obj.getInt("actionId");
                    if (obj.has("metadataId")) {
                        int metadataId = obj.getInt("metadataId");
                        return new HostAction(actionId, metadataId);
                    } else {
                        return new HostAction(actionId);
                    }
                }
            }
            case "valuefloatexpressionchange": {
                Object valObj = obj.has("value") ? obj.get("value") : obj.get("expression");
                float valNan = parser.parseFloat(valObj);
                int valId = androidx.compose.remote.core.operations.Utils.idFromNan(valNan);
                return new ValueFloatExpressionChange(targetId, valId);
            }
            case "valuefloatchange": {
                float val = parser.parseFloat(obj.get("value"));
                return new ValueFloatChange(targetId, val);
            }
            case "valueintegerchange": {
                int val = obj.getInt("value");
                return new ValueIntegerChange(targetId, val);
            }
            case "valueintegerexpressionchange": {
                Object targetObj = obj.has("targetId") ? obj.get("targetId") : obj.get("target");
                long intTargetId = parser.resolveIntegerVariable(targetObj);
                Object valObj = obj.has("value") ? obj.get("value") : obj.get("expression");
                long exprId;
                if (valObj instanceof String) {
                    exprId = parser.getExpressionParser().parseIntegerExpression((String) valObj);
                } else {
                    exprId = obj.getLong("value");
                }
                return new ValueIntegerExpressionChange(intTargetId, exprId);
            }
            case "valuestringchange": {
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
