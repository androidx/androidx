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
import androidx.compose.remote.creation.modifiers.RecordingModifier;

/**
 * Helper class to register default procedural components into the JSON parser.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultComponentParsers {
    private DefaultComponentParsers() {}

    /**
     * Register default procedural component parsers.
     *
     * @param p the JSON parser instance to register into
     */
    public static void register(RemoteComposeJsonParser p) {
        p.registerComponentParser("column", (component, modifier, writer, parser) -> {
            writer.startColumn(modifier,
                    parser.getHorizontalAlign(component, "start"),
                    parser.getVerticalAlign(component, "top"));
            parser.parseChildren(component.optJSONArray("children"));
            writer.endColumn();
        });
        p.registerComponentParser("row", (component, modifier, writer, parser) -> {
            writer.startRow(modifier,
                    parser.getHorizontalAlign(component, "start"),
                    parser.getVerticalAlign(component, "top"));
            parser.parseChildren(component.optJSONArray("children"));
            writer.endRow();
        });
        p.registerComponentParser("box", (component, modifier, writer, parser) -> {
            org.json.JSONArray children = component.optJSONArray("children");
            if (children == null || children.length() == 0) {
                writer.box(modifier,
                        parser.getHorizontalAlign(component, "center"),
                        parser.getVerticalAlign(component, "center"));
            } else {
                writer.startBox(modifier,
                        parser.getHorizontalAlign(component, "start"),
                        parser.getVerticalAlign(component, "top"));
                parser.parseChildren(children);
                writer.endBox();
            }
        });
        p.registerComponentParser("resources", (component, modifier, writer, parser) -> {
            if (parser.isInFirstPass()) {
                parser.parseResourcesOrdered(component);
            }
        });
        p.registerComponentParser("variable", (component, modifier, writer, parser) -> {
            if (parser.isInFirstPass()) {
                parser.parseCommand(component);
            } else {
                String name = component.optString("name");
                if (!parser.mVariables.containsKey(name)) {
                    parser.parseCommand(component);
                }
            }
        });
        p.registerComponentParser("flow", (component, modifier, writer, parser) -> {
            writer.startFlow(modifier,
                    parser.getHorizontalAlign(component, "start"),
                    parser.getVerticalAlign(component, "top"),
                    component.optInt("maxColumns", Integer.MAX_VALUE),
                    component.optInt("maxLines", Integer.MAX_VALUE));
            parser.parseChildren(component.optJSONArray("children"));
            writer.endFlow();
        });
        p.registerComponentParser("collapsibleColumn", (component, modifier, writer, parser) -> {
            writer.startCollapsibleColumn(modifier,
                    parser.getHorizontalAlign(component, "center"),
                    parser.getVerticalAlign(component, "center"));
            parser.parseChildren(component.optJSONArray("children"));
            writer.endCollapsibleColumn();
        });
        p.registerComponentParser("collapsibleRow", (component, modifier, writer, parser) -> {
            writer.startCollapsibleRow(modifier,
                    parser.getHorizontalAlign(component, "center"),
                    parser.getVerticalAlign(component, "center"));
            parser.parseChildren(component.optJSONArray("children"));
            writer.endCollapsibleRow();
        });
        p.registerComponentParser("fitBox", (component, modifier, writer, parser) -> {
            writer.startFitBox(modifier,
                    parser.getHorizontalAlign(component, "center"),
                    parser.getVerticalAlign(component, "center"));
            parser.parseChildren(component.optJSONArray("children"));
            writer.endFitBox();
        });
        p.registerComponentParser("text", (component, modifier, writer, parser) -> {
            parser.parseText(component, modifier);
        });
        p.registerComponentParser("spacer", (component, modifier, writer, parser) -> {
            if (modifier.getList().isEmpty()) {
                modifier.horizontalWeight(1.0f);
            }
            writer.startBox(modifier, 0, 0);
            writer.endBox();
        });
        p.registerComponentParser("canvas", (component, modifier, writer, parser) -> {
            parser.parseCanvas(component, modifier);
        });
        p.registerComponentParser("bitmap", (component, modifier, writer, parser) -> {
            writer.addBitmap(component.get("id"));
        });
        p.registerComponentParser("global", (component, modifier, writer, parser) -> {
            if (parser.isInFirstPass()) {
                parser.beginGlobal();
                org.json.JSONArray children = component.optJSONArray("children");
                if (children != null) {
                    for (int i = 0; i < children.length(); i++) {
                        org.json.JSONObject child = parser.normalizeComponent(
                                children.getJSONObject(i));
                        String type = child.optString("type");
                        String typeLower = type.toLowerCase();
                        if (typeLower.equals("resources") || typeLower.equals("variable")
                                || typeLower.equals("global") || typeLower.equals("definepattern")
                                || typeLower.equals("referencedoperations")) {
                            parser.parseComponent(child);
                        }
                    }
                }
                parser.endGlobal();
            } else {
                parser.parseChildren(component.optJSONArray("children"));
            }
        });
        p.registerComponentParser("definepattern", (component, modifier, writer, parser) -> {
            if (parser.isInFirstPass()) {
                String name = component.getString("name");
                if (parser.hasComponentParser(name)) {
                    throw new org.json.JSONException(
                            "Cannot override pre-existing component: " + name);
                }
                org.json.JSONArray params = component.optJSONArray("parameters");
                int paramCount = params != null ? params.length() : 0;
                String[] paramNames = new String[paramCount];
                int[] paramIds = new int[paramCount];

                java.util.Map<String, Float> savedVariables =
                        new java.util.HashMap<>(parser.mVariables);

                if (params != null) {
                    for (int i = 0; i < paramCount; i++) {
                        String paramName = params.getString(i);
                        paramNames[i] = paramName;
                        int paramId = writer.definePatternParameter(paramName);
                        paramIds[i] = paramId;
                        parser.mVariables.put(paramName,
                                androidx.compose.remote.core.operations.Utils.asNan(paramId));
                    }
                }

                int patternId = writer.definePattern(name, paramIds);

                // Parse locals if specified
                org.json.JSONArray locals = component.optJSONArray("locals");
                if (locals != null) {
                    for (int i = 0; i < locals.length(); i++) {
                        String localName = locals.getString(i);
                        int localId = writer.nextLocalId();
                        parser.mVariables.put(localName, (float) localId);
                    }
                }

                // Parse components inside body
                parser.parseChildren(component.optJSONArray("children"));

                writer.endPatternDefine();

                parser.mVariables.clear();
                parser.mVariables.putAll(savedVariables);

                // Dynamic component registration
                final String[] finalParamNames = paramNames;
                final int finalPatternId = patternId;

                parser.registerComponentParser(name,
                        (callComponent, callModifier, callWriter, callParser) -> {
                        int[] argIds = new int[finalParamNames.length];
                        if (callComponent.has("arguments")) {
                            org.json.JSONArray args = callComponent.getJSONArray("arguments");
                            for (int i = 0; i < argIds.length; i++) {
                                if (i < args.length()) {
                                    argIds[i] = callParser.resolveTextId(args.get(i));
                                } else {
                                    argIds[i] = 0;
                                }
                            }
                        } else {
                            for (int i = 0; i < finalParamNames.length; i++) {
                                String pName = finalParamNames[i];
                                if (callComponent.has(pName)) {
                                    argIds[i] = callParser.resolveTextId(callComponent.get(pName));
                                } else {
                                    argIds[i] = 0;
                                }
                            }
                        }

                        callWriter.patternInflation(finalPatternId, argIds);
                        callParser.parseChildren(callComponent.optJSONArray("children"));
                        callWriter.endPatternInflation();
                    });

                // Dynamic modifier registration
                parser.registerModifierParser(name, (mod, key, recordingModifier, callParser) -> {
                    org.json.JSONArray args = mod.getJSONArray(key);
                    int[] argIds = new int[args.length()];
                    for (int i = 0; i < args.length(); i++) {
                        argIds[i] = callParser.resolveTextId(args.get(i));
                    }
                    recordingModifier.then(new androidx.compose.remote.creation.modifiers
                             .MacroCallModifier(finalPatternId, argIds));
                });
            }
        });
        p.registerComponentParser("patternargument", (component, modifier, writer, parser) -> {
            int index = component.getInt("parameterIndex");
            writer.addPatternArgument(index);
        });
        p.registerComponentParser("patternforeach", (component, modifier, writer, parser) -> {
            int collectionId = parser.resolveTextId(component.get("collection"));
            String localItemName = component.getString("localItem");
            int localItemId = writer.nextLocalId();

            float prevVal = parser.mVariables.getOrDefault(localItemName, Float.NaN);
            parser.mVariables.put(localItemName, (float) localItemId);

            writer.addPatternForEach(collectionId, localItemId);
            parser.parseChildren(component.optJSONArray("children"));
            writer.endPatternForEach();

            if (Float.isNaN(prevVal)) {
                parser.mVariables.remove(localItemName);
            } else {
                parser.mVariables.put(localItemName, prevVal);
            }
        });
        p.registerComponentParser("patternblock", (component, modifier, writer, parser) -> {
            int index = component.getInt("parameterIndex");
            writer.addPatternBlock(index);
            parser.parseChildren(component.optJSONArray("children"));
            writer.endPatternBlock();
        });
        p.registerComponentParser("patterninflation", (component, modifier, writer, parser) -> {
            String name = component.getString("pattern");
            int patternId = writer.textCreateId(name);
            org.json.JSONArray args = component.getJSONArray("arguments");
            int[] argIds = new int[args.length()];
            for (int i = 0; i < args.length(); i++) {
                argIds[i] = parser.resolveTextId(args.get(i));
            }
            writer.patternInflation(patternId, argIds);
            parser.parseChildren(component.optJSONArray("children"));
            writer.endPatternInflation();
        });
        p.registerComponentParser("referencedoperations", (component, modifier, writer, parser) -> {
            String name = component.getString("name");
            if (parser.isInFirstPass()) {
                int id = writer.nextId();
                writer.startReferencedOperations(id);
                parser.parseChildren(component.optJSONArray("children"));
                org.json.JSONArray modifiers = component.optJSONArray("modifiers");
                if (modifiers != null) {
                    RecordingModifier refModifier = parser.parseModifiers(modifiers);
                    for (RecordingModifier.Element m : refModifier.getList()) {
                        m.write(writer);
                    }
                }
                writer.endReferencedOperations();
                parser.mVariables.put(name, (float) id);
            } else {
                if (!parser.mVariables.containsKey(name)) {
                    int id = writer.nextId();
                    writer.startReferencedOperations(id);
                    parser.parseChildren(component.optJSONArray("children"));
                    org.json.JSONArray modifiers = component.optJSONArray("modifiers");
                    if (modifiers != null) {
                        RecordingModifier refModifier = parser.parseModifiers(modifiers);
                        for (RecordingModifier.Element m : refModifier.getList()) {
                            m.write(writer);
                        }
                    }
                    writer.endReferencedOperations();
                    parser.mVariables.put(name, (float) id);
                }
            }
        });
        p.registerComponentParser("modifier", (component, modifier, writer, parser) -> {
            for (RecordingModifier.Element m : modifier.getList()) {
                m.write(writer);
            }
        });
        p.registerComponentParser("include", (component, modifier, writer, parser) -> {
            int refId = parser.resolveTextId(component.get("value"));
            writer.addIncludeReferencedOperations(refId);
        });
    }
}
