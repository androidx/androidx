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
            writer.startBox(modifier,
                    parser.getHorizontalAlign(component, "start"),
                    parser.getVerticalAlign(component, "top"));
            parser.parseChildren(component.optJSONArray("children"));
            writer.endBox();
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
                    Integer.MAX_VALUE);
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
                        if (type.equals("resources") || type.equals("variable")
                                || type.equals("global")) {
                            parser.parseComponent(child);
                        }
                    }
                }
                parser.endGlobal();
            } else {
                parser.parseChildren(component.optJSONArray("children"));
            }
        });
    }
}
