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
import androidx.compose.remote.creation.dsl.RcFloat;
import androidx.compose.remote.creation.dsl.VerticalScrollRcFloatModifier;

import org.json.JSONArray;
import org.json.JSONObject;

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
        p.registerModifierParser("verticalscroll", (mod, key, recordingModifier, parser) -> {
            RcFloat positionRc = new RcFloat(parser.getWriter(), parser.parseFloat(mod.get(key)));
            recordingModifier.then(new VerticalScrollRcFloatModifier(positionRc));
        });
        p.registerModifierParser("horizontalscroll", (mod, key, recordingModifier, parser) -> {
            recordingModifier.horizontalScroll(parser.parseFloat(mod.get(key)));
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
    }
}
