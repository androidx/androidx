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
package androidx.compose.remote.creation.json;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.ConditionalOperations;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.NamedVariable;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.managers.TextStyle;
import androidx.compose.remote.core.operations.utilities.MatrixOperations;
import androidx.compose.remote.creation.RcPaint;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.modifiers.CircleShape;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.modifiers.RectShape;
import androidx.compose.remote.creation.modifiers.RoundedRectShape;
import androidx.compose.remote.creation.modifiers.Shape;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Parser for RemoteCompose JSON format.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComposeJsonParser {
    private static final boolean DEBUG = false;
    private final RemoteComposeWriter mWriter;
    final Map<String, Integer> mColors = new HashMap<>();
    final Map<String, Integer> mPaths = new HashMap<>();
    final Map<String, Object> mBitmaps = new HashMap<>();
    final Map<String, Float> mVariables = new HashMap<>();
    final Map<String, Float> mMatrices = new HashMap<>();
    final Map<String, Object> mDeferredVariables = new HashMap<>();
    final Map<String, Float> mEmittedVariables = new HashMap<>();
    boolean mOrderedResources = false;
    int mGlobalNesting = 0;
    private boolean mInFirstPass = false;
    private final List<String> mContextPath = new ArrayList<>();
    private final Map<String, JsonComponentParser> mComponentParsers = new HashMap<>();
    private final Map<String, JsonModifierParser> mModifierParsers = new HashMap<>();
    private final ExpressionParser mExpressionParser;
    private final ResourceParser mResourceParser;

    /**
     * Push a parsing segment onto the context path stack.
     *
     * @param segment the path segment to push (e.g. "root" or "children[0]")
     */
    public void pushContext(@NonNull String segment) {
        mContextPath.add(segment);
    }

    /**
     * Pop the top segment from the context path stack.
     */
    public void popContext() {
        if (!mContextPath.isEmpty()) {
            mContextPath.remove(mContextPath.size() - 1);
        }
    }

    /**
     * Build the context path stack string.
     *
     * @return a formatted string showing the current parsing context hierarchy
     */
    public @NonNull String getContextPathString() {
        if (mContextPath.isEmpty()) {
            return "root";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mContextPath.size(); i++) {
            if (i > 0) {
                sb.append(" -> ");
            }
            sb.append(mContextPath.get(i));
        }
        return sb.toString();
    }

    /**
     * Register a custom procedural component parser.
     *
     * @param type the lower-case type name (e.g. "custom_layout")
     * @param parser the parser implementation
     */
    public void registerComponentParser(@NonNull String type, @NonNull JsonComponentParser parser) {
        mComponentParsers.put(type.toLowerCase(), parser);
    }

    /**
     * Check if a component parser is registered.
     */
    public boolean hasComponentParser(@NonNull String type) {
        return mComponentParsers.containsKey(type.toLowerCase());
    }

    /**
     * Register a custom layout modifier parser.
     *
     * @param key the lower-case modifier key name (e.g. "custom_modifier")
     * @param parser the parser implementation
     */
    public void registerModifierParser(@NonNull String key, @NonNull JsonModifierParser parser) {
        mModifierParsers.put(key.toLowerCase(), parser);
    }

    public RemoteComposeJsonParser(@NonNull RemoteComposeWriter writer) {
        mWriter = writer;
        mExpressionParser = new ExpressionParser(this);
        mResourceParser = new ResourceParser(this);
        DefaultComponentParsers.register(this);
        DefaultModifierParsers.register(this);
    }

    /**
     * Begin a global block in the writer.
     */
    public void beginGlobal() {
        if (mGlobalNesting == 0) {
            mWriter.beginGlobal();
        }
        mGlobalNesting++;
    }

    /**
     * End a global block in the writer.
     */
    public void endGlobal() {
        if (mGlobalNesting > 0) {
            mGlobalNesting--;
            if (mGlobalNesting == 0) {
                mWriter.endGlobal();
            }
        }
    }

    void parseResourcesOrdered(JSONObject resources) throws JSONException {
        mResourceParser.parseResourcesOrdered(resources);
    }

    public @NonNull RemoteComposeWriter getWriter() {
        return mWriter;
    }

    /**
     * Expose whether the parser is currently in the first traversal pass.
     *
     * @return true if in the first pass, false otherwise
     */
    public boolean isInFirstPass() {
        return mInFirstPass;
    }

    /**
     * Expose the RPN mathematical expression parser.
     *
     * @return the ExpressionParser instance
     */
    public @NonNull ExpressionParser getExpressionParser() {
        return mExpressionParser;
    }

    /**
     * Parse only the header tags from the JSON description.
     *
     * @param json the JSON description of the RemoteCompose document
     * @return an array of HTag objects parsed from the header
     * @throws JSONException if JSON parsing fails
     */
    public static RemoteComposeWriter.@NonNull HTag @NonNull [] parseHeaderOnly(
            @NonNull String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (!root.has("header")) {
            return new RemoteComposeWriter.HTag[0];
        }
        JSONObject header = root.getJSONObject("header");
        Iterator<String> keys = header.keys();
        List<RemoteComposeWriter.HTag> tags = new ArrayList<>();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.equals("apiLevel") || key.equals("orderedResources")) {
                continue;
            }
            short tag = parseHeaderTagStatic(key);
            Object value = header.get(key);
            tags.add(RemoteComposeWriter.hTag(tag, value));
        }
        return tags.toArray(new RemoteComposeWriter.HTag[0]);
    }

    /**
     * Parse the API level from the JSON description.
     *
     * @param json the JSON description of the RemoteCompose document
     * @return the parsed API level, defaulting to 7 if not specified
     * @throws JSONException if JSON parsing fails
     */
    public static int parseApiLevel(@NonNull String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (root.has("header")) {
            return root.getJSONObject("header").optInt("apiLevel", 7);
        }
        return 7;
    }

    /**
     * Define a custom float variable by name.
     *
     * @param name the variable identifier string
     * @param value the floating point value to associate with the name
     */
    public void defineVariable(@NonNull String name, float value) {
        mVariables.put(name, value);
    }

    /**
     * Register/define an image/bitmap resource by name.
     *
     * @param name the unique name/key of the bitmap
     * @param bitmap the bitmap instance to associate
     */
    public void defineBitmap(@NonNull String name, @NonNull Object bitmap) {
        mBitmaps.put(name, bitmap);
    }

    /**
     * Parse the full JSON RemoteCompose document into the writer.
     *
     * @param json the JSON description of the RemoteCompose document
     * @throws JSONException if JSON parsing fails
     */
    public void parse(@NonNull String json) throws JSONException {
        try {
            pushContext("root");
            JSONObject root = new JSONObject(json);
            if (root.has("header")) {
                mOrderedResources = root.getJSONObject("header")
                        .optBoolean("orderedResources", false);
            }
            if (root.has("resources")) {
                if (mOrderedResources) {
                    mResourceParser.parseResourcesOrdered(root.getJSONObject("resources"));
                } else {
                    mResourceParser.parseResources(root.getJSONObject("resources"));
                }
            }
            if (root.has("root")) {
                Object r = root.get("root");
                if (r instanceof JSONArray) {
                    JSONArray arr = (JSONArray) r;
                    // First pass: parse resources and variables before root
                    mInFirstPass = true;
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject item = normalizeComponent(arr.getJSONObject(i));
                        String type = item.optString("type");
                        String typeLower = type.toLowerCase();
                        if (typeLower.equals("resources") || typeLower.equals("variable")
                                || typeLower.equals("global") || typeLower.equals("definepattern")
                                || typeLower.equals("referencedoperations")) {
                            parseComponent(item);
                        }
                    }
                    mInFirstPass = false;

                    mWriter.root(() -> {
                        try {
                            // Second pass: parse layout components inside root
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject item = normalizeComponent(arr.getJSONObject(i));
                                String type = item.optString("type");
                                String typeLower = type.toLowerCase();
                                if (!typeLower.equals("resources") && !typeLower.equals("variable")
                                        && !typeLower.equals("definepattern")
                                        && !typeLower.equals("referencedoperations")) {
                                    parseComponent(item);
                                }
                            }
                            while (mGlobalNesting > 0) {
                                endGlobal();
                            }
                        } catch (org.json.JSONException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else {
                    JSONObject component = normalizeComponent((JSONObject) r);
                    String type = component.optString("type");
                    String typeLower = type.toLowerCase();
                    mInFirstPass = true;
                    if (typeLower.equals("resources") || typeLower.equals("variable")
                            || typeLower.equals("global") || typeLower.equals("definepattern")
                            || typeLower.equals("referencedoperations")) {
                        parseComponent(component);
                    }
                    mInFirstPass = false;

                    mWriter.root(() -> {
                        try {
                            if (!typeLower.equals("resources") && !typeLower.equals("variable")
                                    && !typeLower.equals("definepattern")
                                    && !typeLower.equals("referencedoperations")) {
                                parseComponent(component);
                            }
                            while (mGlobalNesting > 0) {
                                endGlobal();
                            }
                        } catch (org.json.JSONException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        } catch (JSONException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Parsing error")) {
                throw e;
            }
            throw new JSONException("Parsing error at ContextPath: " + getContextPathString()
                    + "\nReason: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof JSONException) {
                JSONException cause = (JSONException) e.getCause();
                if (cause.getMessage() != null && cause.getMessage().startsWith("Parsing error")) {
                    throw cause;
                }
                throw new JSONException("Parsing error at ContextPath: " + getContextPathString()
                        + "\nReason: " + cause.getMessage(), cause);
            }
            throw e;
        } finally {
            popContext();
        }
    }

    private static short parseHeaderTagStatic(@NonNull String name) throws JSONException {
        switch (name) {
            case "width": return Header.DOC_WIDTH;
            case "height": return Header.DOC_HEIGHT;
            case "contentDescription": return Header.DOC_CONTENT_DESCRIPTION;
            case "desiredFPS":
            case "fps":
                return Header.DOC_DESIRED_FPS;
            case "profiles": return Header.DOC_PROFILES;
            case "theme": return Header.TEST_COLOR_THEME;
            case "ltResize": return Header.FEATURE_LT_RESIZE;
            case "densityBehavior": return Header.DOC_DENSITY_BEHAVIOR;
            case "featurePaintMeasure": return Header.FEATURE_PAINT_MEASURE;
            default:
                throw new JSONException("Unknown header tag: " + name);
        }
    }



    int getHorizontalAlign(@NonNull JSONObject component, @NonNull String defaultValue) {
        return parseHorizontalAlignment(component.optString("horizontalAlignment", defaultValue));
    }

    int getVerticalAlign(@NonNull JSONObject component, @NonNull String defaultValue) {
        return parseVerticalAlignment(component.optString("verticalAlignment", defaultValue));
    }

    /**
     * Normalize a component to ensure it has the "type" key, resolving shorthands.
     *
     * @param component the component to normalize
     * @return the normalized component
     * @throws JSONException if JSON parsing fails
     */
    public @NonNull JSONObject normalizeComponent(
            @NonNull JSONObject component) throws JSONException {
        if (!component.has("type") && component.length() == 1) {
            String key = component.keys().next();
            Object val = component.get(key);
            JSONObject normalized = new JSONObject();
            normalized.put("type", key);
            if (val instanceof JSONArray) {
                normalized.put("children", val);
            } else if (val instanceof JSONObject) {
                JSONObject obj = (JSONObject) val;
                Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    normalized.put(k, obj.get(k));
                }
            } else {
                if (key.equals("bitmap")) {
                    normalized.put("id", val);
                } else {
                    normalized.put("value", val);
                }
            }
            return normalized;
        }
        return component;
    }

    void parseComponent(@NonNull JSONObject component) throws JSONException {
        try {
            component = normalizeComponent(component);

            if (component.has("resources")) {
                boolean ordered = component.optBoolean("orderedResources", mOrderedResources);
                if (ordered) {
                    mResourceParser.parseResourcesOrdered(component.getJSONObject("resources"));
                } else {
                    mResourceParser.parseResources(component.getJSONObject("resources"));
                }
            }

            RecordingModifier modifier = parseModifiers(component.optJSONArray("modifiers"));
            String type = component.getString("type");
            JsonComponentParser compParser = mComponentParsers.get(type.toLowerCase());
            if (compParser != null) {
                compParser.parse(component, modifier, mWriter, this);
            } else {
                throw new JSONException("Unknown component type: " + type);
            }
        } catch (JSONException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("Parsing error")) {
                throw e;
            }
            JSONException newEx = new JSONException(
                    "Parsing error at ContextPath: " + getContextPathString()
                            + "\nReason: " + e.getMessage());
            newEx.initCause(e);
            throw newEx;
        }
    }

    void parseChildren(@Nullable JSONArray children) throws JSONException {
        if (children == null) return;
        for (int i = 0; i < children.length(); i++) {
            pushContext("children[" + i + "]");
            try {
                parseComponent(children.getJSONObject(i));
            } finally {
                popContext();
            }
        }
    }

    private int parseHorizontalAlignment(String align) {
        switch (align.toLowerCase()) {
            case "start": return 1;
            case "center": return 2;
            case "end": return 3;
            case "spacebetween": return 6;
            case "spaceevenly": return 7;
            case "spacearound": return 8;
            default: return 1;
        }
    }

    private int parseVerticalAlignment(String align) {
        switch (align.toLowerCase()) {
            case "start": return 1;
            case "top": return 4;
            case "center": return 2;
            case "bottom": return 5;
            case "spacebetween": return 6;
            case "spaceevenly": return 7;
            case "spacearound": return 8;
            default: return 4;
        }
    }

    void parseText(@NonNull JSONObject component,
            RecordingModifier modifier) throws JSONException {
        Object value = component.opt("value");
        float textFromFloat = Float.NaN;
        int textFromFloatWhole = 0;
        int textFromFloatDecimal = 0;
        int textFromFloatFlags = 0;
        if (component.has("textFromFloat")) {
            JSONObject obj = component.getJSONObject("textFromFloat");
            textFromFloat = parseFloat(obj.get("value"));
            textFromFloatWhole = obj.optInt("whole", 0);
            textFromFloatDecimal = obj.optInt("decimal", 0);
            textFromFloatFlags = obj.optInt("flags", 0);
        }

        int textId = -1;
        if (value != null) {
            textId = resolveTextId(value);
        } else if (!Float.isNaN(textFromFloat)) {
            textId = mWriter.createTextFromFloat(
                    textFromFloat, textFromFloatWhole, textFromFloatDecimal, textFromFloatFlags);
        }

        int color = 0xFF000000;
        int colorId = -1;
        Object colorObj = component.opt("color");
        if (colorObj instanceof String && (((String) colorObj).startsWith("$colors.")
                || ((String) colorObj).startsWith("@colors."))) {
            colorId = parseColor(colorObj);
        } else if (colorObj != null) {
            color = parseColor(colorObj);
        }

        int maxLines = component.optInt("maxLines", Integer.MAX_VALUE);
        String overflowStr = component.optString("overflow", "clip");
        int overflow = 1;
        if (overflowStr.equalsIgnoreCase("ellipsis")) {
            overflow = 3;
        } else if (overflowStr.equalsIgnoreCase("visible")) {
            overflow = 2;
        } else if (overflowStr.equalsIgnoreCase("start_ellipsis")) {
            overflow = 4;
        } else if (overflowStr.equalsIgnoreCase("middle_ellipsis")) {
            overflow = 5;
        }

        float fontSize = component.has("fontSize")
                ? parseFloat(component.get("fontSize"))
                : TextStyle.DEFAULT_FONT_SIZE;

        float fontWeight = component.has("fontWeight")
                ? parseFloat(component.get("fontWeight"))
                : TextStyle.DEFAULT_FONT_WEIGHT;

        int textStyleId = component.optInt("textStyleId", -1);
        float minFontSize = component.has("minFontSize")
                ? parseFloat(component.get("minFontSize")) : -1f;
        float maxFontSize = component.has("maxFontSize")
                ? parseFloat(component.get("maxFontSize")) : -1f;

        int fontStyle = 0;
        String fontStyleStr = component.optString("fontStyle", null);
        if ("italic".equalsIgnoreCase(fontStyleStr)) {
            fontStyle = 1;
        } else if (component.has("fontStyle")) {
            fontStyle = component.optInt("fontStyle", 0);
        }

        String fontFamily = component.optString("fontFamily", null);
        float letterSpacing = component.has("letterSpacing")
                ? parseFloat(component.get("letterSpacing")) : 0f;
        float lineHeightAdd = component.has("lineHeightAdd")
                ? parseFloat(component.get("lineHeightAdd")) : 0f;
        float lineHeightMultiplier = component.has("lineHeightMultiplier")
                ? parseFloat(component.get("lineHeightMultiplier")) : 1f;

        int lineBreakStrategy = component.optInt("lineBreakStrategy", 0);
        int hyphenationFrequency = component.optInt("hyphenationFrequency", 0);
        int justificationMode = component.optInt("justificationMode", 0);
        boolean underline = component.optBoolean("underline", false);
        boolean strikethrough = component.optBoolean("strikethrough", false);
        boolean autoSize = component.optBoolean("autoSize", false);

        String[] fontAxis = null;
        float[] fontAxisValues = null;
        if (component.has("fontAxis") && component.has("fontAxisValues")) {
            JSONArray axisArr = component.getJSONArray("fontAxis");
            JSONArray valArr = component.getJSONArray("fontAxisValues");
            fontAxis = new String[axisArr.length()];
            fontAxisValues = new float[valArr.length()];
            for (int i = 0; i < axisArr.length(); i++) {
                fontAxis[i] = axisArr.getString(i);
                fontAxisValues[i] = (float) valArr.getDouble(i);
            }
        }

        mWriter.textComponent(
                modifier,
                textId,
                textStyleId,
                color,
                colorId,
                fontSize,
                minFontSize,
                maxFontSize,
                fontStyle,
                fontWeight,
                fontFamily,
                parseTextAlign(component.optString("textAlign", "start")),
                overflow,
                maxLines,
                letterSpacing,
                lineHeightAdd,
                lineHeightMultiplier,
                lineBreakStrategy,
                hyphenationFrequency,
                justificationMode,
                underline,
                strikethrough,
                fontAxis,
                fontAxisValues,
                autoSize,
                0, () -> {});
    }

    private int parseTextAlign(String align) {
        switch (align.toLowerCase()) {
            case "left":
            case "1":
                return 1;
            case "right":
            case "2":
                return 2;
            case "center":
            case "3":
                return 3;
            case "justify":
            case "4":
                return 4;
            case "start":
            case "5":
                return 5;
            case "end":
            case "6":
                return 6;
            default:
                return 1;
        }
    }

    void parseCanvas(@NonNull JSONObject component,
            RecordingModifier modifier) throws JSONException {
        mWriter.startCanvas(modifier);
        parseCommands(component.optJSONArray("commands"));
        mWriter.endCanvas();
    }

    private void parseCommands(@Nullable JSONArray commands) throws JSONException {
        if (commands == null) return;
        for (int i = 0; i < commands.length(); i++) {
            parseCommand(commands.getJSONObject(i));
        }
    }

    void parseCommand(@NonNull JSONObject command) throws JSONException {
        if (!command.has("type") && command.length() == 1) {
            String key = command.keys().next();
            Object val = command.get(key);
            JSONObject normalized = new JSONObject();
            normalized.put("type", key);
            if (val instanceof JSONArray) {
                normalized.put("commands", val);
            } else if (val instanceof JSONObject) {
                JSONObject obj = (JSONObject) val;
                Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    normalized.put(k, obj.get(k));
                }
            } else {
                if (key.equals("drawPath") || key.equals("pathAppendClose")) {
                    normalized.put("path", val);
                } else {
                    normalized.put("value", val);
                }
            }
            command = normalized;
        }
        String type = command.getString("type");
        switch (type) {
            case "paint":
                RcPaint paint = mWriter.getRcPaint();
                JSONArray ops = command.optJSONArray("ops");
                if (ops != null) {
                    for (int i = 0; i < ops.length(); i++) {
                        JSONObject op = ops.getJSONObject(i);
                        Iterator<String> keys = op.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            switch (key) {
                                case "shader":
                                    paint.setShader(op.getInt(key));
                                    break;
                                case "style":
                                    String style = op.getString(key);
                                    if (style.equalsIgnoreCase("fill")) {
                                        paint.setStyle(0);
                                    } else if (style.equalsIgnoreCase("stroke")) {
                                        paint.setStyle(1);
                                    } else if (style.equalsIgnoreCase("fillAndStroke")) {
                                        paint.setStyle(2);
                                    }
                                    break;
                                case "linearGradient": {
                                    JSONObject g = op.getJSONObject("linearGradient");
                                    JSONArray colorsArr = g.getJSONArray("colors");
                                    int[] colors = new int[colorsArr.length()];
                                    int mask = 0;
                                    for (int j = 0; j < colorsArr.length(); j++) {
                                        Object c = colorsArr.get(j);
                                        if (c instanceof String
                                                && (((String) c).startsWith("$colors.")
                                                || ((String) c).startsWith("@colors."))) {
                                            mask |= (1 << j);
                                        }
                                        colors[j] = parseColor(c);
                                    }
                                    JSONArray stopsArr = g.optJSONArray("stops");
                                    float[] stops = null;
                                    if (stopsArr != null) {
                                        stops = new float[stopsArr.length()];
                                        for (int j = 0; j < stopsArr.length(); j++) {
                                            stops[j] = (float) stopsArr.getDouble(j);
                                        }
                                    }
                                    paint.setLinearGradient(
                                            g.has("x1") ? parseFloat(g.get("x1")) : 0f,
                                            g.has("y1") ? parseFloat(g.get("y1")) : 0f,
                                            g.has("x2") ? parseFloat(g.get("x2")) : 0f,
                                            g.has("y2") ? parseFloat(g.get("y2")) : 0f,
                                            colors,
                                            mask,
                                            stops,
                                            g.optInt("tileMode", 0)
                                    );
                                    break;
                                }
                                case "pathEffect": {
                                    JSONArray pe = op.optJSONArray(key);
                                    if (pe == null) {
                                        paint.setPathEffect(null);
                                    } else {
                                        float[] pathEffect = new float[pe.length()];
                                        for (int j = 0; j < pe.length(); j++) {
                                            pathEffect[j] = (float) pe.getDouble(j);
                                        }
                                        paint.setPathEffect(pathEffect);
                                    }
                                    break;
                                }
                                case "color":
                                    String color = op.getString(key);
                                    if (color.startsWith("$colors.")
                                            || color.startsWith("@colors.")) {
                                        paint.setColorId(parseColor(color));
                                    } else {
                                        paint.setColor(parseColor(color));
                                    }
                                    break;
                                case "alpha":
                                    paint.setAlpha(parseFloat(op.get(key)));
                                    break;
                                case "width":
                                    paint.setStrokeWidth(parseFloat(op.get(key)));
                                    break;
                                case "strokeCap": {
                                    String cap = op.getString(key);
                                    if (cap.equalsIgnoreCase("round")) {
                                        paint.setStrokeCap(1);
                                    } else if (cap.equalsIgnoreCase("square")) {
                                        paint.setStrokeCap(2);
                                    } else {
                                        paint.setStrokeCap(0);
                                    }
                                    break;
                                }
                                case "textSize":
                                    paint.setTextSize(parseFloat(op.get(key)));
                                    break;
                                case "sweepGradient": {
                                    JSONObject g = op.getJSONObject("sweepGradient");
                                    JSONArray colorsArr = g.getJSONArray("colors");
                                    int[] colors = new int[colorsArr.length()];
                                    int mask = 0;
                                    for (int j = 0; j < colorsArr.length(); j++) {
                                        Object c = colorsArr.get(j);
                                        if (c instanceof String
                                                && (((String) c).startsWith("$colors.")
                                                || ((String) c).startsWith("@colors."))) {
                                            mask |= (1 << j);
                                        }
                                        colors[j] = parseColor(c);
                                    }
                                    JSONArray stopsArr = g.optJSONArray("stops");
                                    float[] stops = null;
                                    if (stopsArr != null) {
                                        stops = new float[stopsArr.length()];
                                        for (int j = 0; j < stopsArr.length(); j++) {
                                            stops[j] = (float) stopsArr.getDouble(j);
                                        }
                                    }
                                    paint.setSweepGradient(
                                            parseFloat(g.get("centerX")),
                                            parseFloat(g.get("centerY")),
                                            colors,
                                            mask,
                                            stops
                                    );
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    if (command.has("shader")) {
                        paint.setShader(command.getInt("shader"));
                    }
                    if (command.has("color")) {
                        String color = command.getString("color");
                        if (color.startsWith("$colors.") || color.startsWith("@colors.")) {
                            paint.setColorId(parseColor(color));
                        } else {
                            paint.setColor(parseColor(color));
                        }
                    }
                    if (command.has("style")) {
                        String style = command.getString("style");
                        if (style.equalsIgnoreCase("fill")) {
                            paint.setStyle(0);
                        } else if (style.equalsIgnoreCase("stroke")) {
                            paint.setStyle(1);
                        } else if (style.equalsIgnoreCase("fillAndStroke")) {
                            paint.setStyle(2);
                        }
                    }
                    if (command.has("linearGradient")) {
                        JSONObject g = command.getJSONObject("linearGradient");
                        JSONArray colorsArr = g.getJSONArray("colors");
                        int[] colors = new int[colorsArr.length()];
                        int mask = 0;
                        for (int i = 0; i < colorsArr.length(); i++) {
                            Object c = colorsArr.get(i);
                            if (c instanceof String && (((String) c).startsWith("$colors.")
                                    || ((String) c).startsWith("@colors."))) {
                                mask |= (1 << i);
                            }
                            colors[i] = parseColor(c);
                        }
                        JSONArray stopsArr = g.optJSONArray("stops");
                        float[] stops = null;
                        if (stopsArr != null) {
                            stops = new float[stopsArr.length()];
                            for (int i = 0; i < stopsArr.length(); i++) {
                                stops[i] = (float) stopsArr.getDouble(i);
                            }
                        }
                        paint.setLinearGradient(
                                g.has("x1") ? parseFloat(g.get("x1")) : 0f,
                                g.has("y1") ? parseFloat(g.get("y1")) : 0f,
                                g.has("x2") ? parseFloat(g.get("x2")) : 0f,
                                g.has("y2") ? parseFloat(g.get("y2")) : 0f,
                                colors,
                                mask,
                                stops,
                                g.optInt("tileMode", 0)
                        );
                    }
                    if (command.has("pathEffect")) {
                        JSONArray pe = command.optJSONArray("pathEffect");
                        if (pe == null) {
                            paint.setPathEffect(null);
                        } else {
                            float[] pathEffect = new float[pe.length()];
                            for (int i = 0; i < pe.length(); i++) {
                                pathEffect[i] = (float) pe.getDouble(i);
                            }
                            paint.setPathEffect(pathEffect);
                        }
                    }
                    if (command.has("alpha")) {
                        paint.setAlpha(parseFloat(command.get("alpha")));
                    }
                    if (command.has("width")) {
                        paint.setStrokeWidth(parseFloat(command.get("width")));
                    }
                    if (command.has("strokeCap")) {
                        String cap = command.getString("strokeCap");
                        if (cap.equalsIgnoreCase("round")) {
                            paint.setStrokeCap(1);
                        } else if (cap.equalsIgnoreCase("square")) {
                            paint.setStrokeCap(2);
                        } else {
                            paint.setStrokeCap(0);
                        }
                    }
                    if (command.has("textSize")) {
                        paint.setTextSize(parseFloat(command.get("textSize")));
                    }
                    if (command.has("sweepGradient")) {
                        JSONObject g = command.getJSONObject("sweepGradient");
                        JSONArray colorsArr = g.getJSONArray("colors");
                        int[] colors = new int[colorsArr.length()];
                        int mask = 0;
                        for (int i = 0; i < colorsArr.length(); i++) {
                            Object c = colorsArr.get(i);
                            if (c instanceof String && (((String) c).startsWith("$colors.")
                                    || ((String) c).startsWith("@colors."))) {
                                mask |= (1 << i);
                            }
                            colors[i] = parseColor(c);
                        }
                        JSONArray stopsArr = g.optJSONArray("stops");
                        float[] stops = null;
                        if (stopsArr != null) {
                            stops = new float[stopsArr.length()];
                            for (int i = 0; i < stopsArr.length(); i++) {
                                stops[i] = (float) stopsArr.getDouble(i);
                            }
                        }
                        paint.setSweepGradient(
                                parseFloat(g.get("centerX")),
                                parseFloat(g.get("centerY")),
                                colors,
                                mask,
                                stops
                        );
                    }
                }
                paint.commit();
                break;
            case "setColor":
                mWriter.getRcPaint().setColor(parseColor(command.get("color")));
                mWriter.getRcPaint().commit();
                break;
            case "setStyle": {
                String style = command.getString("style");
                if (style.equalsIgnoreCase("fill")) {
                    mWriter.getRcPaint().setStyle(0);
                } else if (style.equalsIgnoreCase("stroke")) {
                    mWriter.getRcPaint().setStyle(1);
                } else if (style.equalsIgnoreCase("fillAndStroke")) {
                    mWriter.getRcPaint().setStyle(2);
                }
                mWriter.getRcPaint().commit();
                break;
            }
            case "setStrokeWidth":
                mWriter.getRcPaint().setStrokeWidth(parseFloat(command.get("width")));
                mWriter.getRcPaint().commit();
                break;
            case "drawRect":
                mWriter.drawRect(
                        parseFloat(command.get("left")),
                        parseFloat(command.get("top")),
                        parseFloat(command.get("right")),
                        parseFloat(command.get("bottom"))
                );
                break;
            case "drawLine":
                mWriter.drawLine(
                        parseFloat(command.get("x1")),
                        parseFloat(command.get("y1")),
                        parseFloat(command.get("x2")),
                        parseFloat(command.get("y2"))
                );
                break;
            case "drawPath":
                mWriter.drawPath(parsePath(command.getString("path")));
                break;
            case "drawCircle":
                mWriter.drawCircle(
                        parseFloat(command.get("cx")),
                        parseFloat(command.get("cy")),
                        parseFloat(command.get("radius"))
                );
                break;
            case "addBitmap": {
                String imageName = command.getString("image");
                Object bitmapObj = mBitmaps.get(imageName);
                if (bitmapObj == null) {
                    throw new JSONException("Bitmap not found: " + imageName);
                }
                int id = mWriter.addBitmap(bitmapObj);
                String varName = command.optString("varName", null);
                if (varName != null) {
                    mVariables.put(varName, (float) id);
                }
                break;
            }
            case "drawOval":
                mWriter.drawOval(
                        parseFloat(command.get("left")),
                        parseFloat(command.get("top")),
                        parseFloat(command.get("right")),
                        parseFloat(command.get("bottom"))
                );
                break;
            case "drawRoundRect":
                mWriter.drawRoundRect(
                        parseFloat(command.get("left")),
                        parseFloat(command.get("top")),
                        parseFloat(command.get("right")),
                        parseFloat(command.get("bottom")),
                        parseFloat(command.get("rx")),
                        parseFloat(command.get("ry"))
                );
                break;
            case "drawArc":
                mWriter.drawArc(
                        parseFloat(command.get("left")),
                        parseFloat(command.get("top")),
                        parseFloat(command.get("right")),
                        parseFloat(command.get("bottom")),
                        parseFloat(command.get("startAngle")),
                        parseFloat(command.get("sweepAngle"))
                );
                break;
            case "drawTextAnchored": {
                Object textObj = command.get("text");
                int textId = resolveTextId(textObj);
                mWriter.drawTextAnchored(
                        textId,
                        parseFloat(command.get("x")),
                        parseFloat(command.get("y")),
                        parseFloat(command.get("panX")),
                        parseFloat(command.get("panY")),
                        command.optInt("flags", 0)
                );
                break;
            }
            case "rotate": {
                if (command.has("pivotX") || command.has("centerX")) {
                    float pivotX = command.has("pivotX") ? parseFloat(command.get("pivotX"))
                            : parseFloat(command.get("centerX"));
                    float pivotY = command.has("pivotY") ? parseFloat(command.get("pivotY"))
                            : parseFloat(command.get("centerY"));
                    mWriter.rotate(parseFloat(command.get("angle")), pivotX, pivotY);
                } else {
                    mWriter.rotate(parseFloat(command.get("angle")));
                }
                break;
            }
            case "translate": {
                mWriter.translate(
                        parseFloat(command.get("dx")),
                        parseFloat(command.get("dy"))
                );
                break;
            }
            case "matrixMultiply": {
                float matrixId = parseFloat(command.get("matrix"));
                short mType = (short) command.optInt("mType", 0);
                JSONArray fromArr = command.getJSONArray("from");
                float[] from = new float[fromArr.length()];
                for (int i = 0; i < fromArr.length(); i++) from[i] = parseFloat(fromArr.get(i));
                JSONArray outArr = command.getJSONArray("out");
                float[] out = new float[outArr.length()];
                for (int i = 0; i < outArr.length(); i++) {
                    String name = outArr.getString(i);
                    float id = mWriter.createNamedVariable(name, NamedVariable.FLOAT_TYPE);
                    out[i] = id;
                    mVariables.put(name, id);
                }
                mWriter.addMatrixMultiply(matrixId, mType, from, out);
                break;
            }
            case "pathCreate": {
                String id = command.optString("id", null);
                float x = parseFloat(command.get("x"));
                float y = parseFloat(command.get("y"));
                int pathId = mWriter.pathCreate(x, y);
                if (id != null) mPaths.put(id, pathId);
                break;
            }
            case "pathAppendLineTo":
                mWriter.pathAppendLineTo(
                        parsePath(command.getString("path")),
                        parseFloat(command.get("x")),
                        parseFloat(command.get("y"))
                );
                break;
            case "pathAppendClose":
                mWriter.pathAppendClose(parsePath(command.getString("path")));
                break;
            case "pathExpression": {
                String pathIdStr = command.getString("id");
                float[] expX = parseFloatExpression(command.get("expressionX"));
                float[] expY = parseFloatExpression(command.get("expressionY"));
                float start = parseFloat(command.get("start"));
                float end = parseFloat(command.get("end"));
                float count = parseFloat(command.get("count"));
                int flags = command.optInt("flags", 0);
                int pathId = mWriter.addPathExpression(expX, expY, start, end, count, flags);
                mPaths.put(pathIdStr, pathId);
                break;
            }
            case "loop": {
                float from = parseFloat(command.get("from"));
                float step = command.has("step") ? parseFloat(command.get("step")) : 1.0f;
                float until = parseFloat(command.get("until"));
                String index = command.optString("index", "i");
                boolean noIndexText = command.optBoolean("noIndexText", false);
                int indexId;
                if (noIndexText) {
                    indexId = mWriter.createID(0);
                } else {
                    indexId = mWriter.textCreateId(index);
                }
                float id = Utils.asNan(indexId);
                mWriter.startLoop(indexId, from, step, until);
                boolean hasPrev = mVariables.containsKey(index);
                float prev = hasPrev ? mVariables.get(index) : Float.NaN;
                mVariables.put(index, id);
                parseCommands(command.getJSONArray("commands"));
                if (hasPrev) {
                    mVariables.put(index, prev);
                } else {
                    mVariables.remove(index);
                }
                mWriter.endLoop();
                break;
            }
            case "impulse": {
                float duration = parseFloat(command.get("duration"));
                float start = parseFloat(command.get("start"));
                final JSONObject finalCommand = command;
                mWriter.impulse(duration, start, () -> {
                    try {
                        parseCommands(finalCommand.getJSONArray("commands"));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
                break;
            }
            case "impulseProcess": {
                final JSONObject finalCommand = command;
                mWriter.impulseProcess(() -> {
                    try {
                        parseCommands(finalCommand.getJSONArray("commands"));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
                break;
            }
            case "createParticles": {
                JSONArray varsArray = command.getJSONArray("variables");
                JSONArray initArray = command.getJSONArray("initialValues");
                int count = command.getInt("count");
                String idStr = command.getString("id");

                float[] variables = new float[varsArray.length()];
                float[][] initialExpressions = new float[initArray.length()][];
                for (int i = 0; i < initArray.length(); i++) {
                    initialExpressions[i] = parseFloatExpression(initArray.get(i));
                }

                float systemIdFloat = mWriter.createParticles(variables, initialExpressions, count);
                mVariables.put(idStr, systemIdFloat);

                for (int i = 0; i < varsArray.length(); i++) {
                    mVariables.put(varsArray.getString(i), variables[i]);
                }
                break;
            }
            case "particlesLoop": {
                float systemIdFloat = parseFloat(command.get("system"));
                float[] restartExpr = command.has("restart")
                        ? parseFloatExpression(command.get("restart")) : null;

                JSONArray eqArray = command.getJSONArray("equations");
                float[][] equations = new float[eqArray.length()][];
                for (int i = 0; i < eqArray.length(); i++) {
                    equations[i] = parseFloatExpression(eqArray.get(i));
                }

                final JSONObject finalCommand = command;
                mWriter.particlesLoop(systemIdFloat, restartExpr, equations, () -> {
                    try {
                        parseCommands(finalCommand.getJSONArray("commands"));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });
                break;
            }
            case "variable": {
                String varName = command.getString("name");
                String varType = command.optString("vtype", "float");
                boolean named = command.optBoolean("export", false);
                if (varType.equals("floatArrays")) {
                    JSONArray arr;
                    Object val = command.get("value");
                    if (val instanceof JSONObject) {
                        arr = ((JSONObject) val).getJSONArray("value");
                    } else {
                        arr = (JSONArray) val;
                    }
                    float[] data = new float[arr.length()];
                    for (int i = 0; i < arr.length(); i++) {
                        data[i] = (float) arr.getDouble(i);
                    }
                    float varVal = mWriter.addFloatArray(data);
                    if (named) {
                        mWriter.setNamedVariable(
                                Utils.idFromNan(varVal),
                                varName,
                                NamedVariable.FLOAT_ARRAY_TYPE
                        );
                    }
                    mVariables.put(varName, varVal);
                } else if (varType.equals("path")) {
                    String pathVal = command.getString("value");
                    int id = parsePath(pathVal);
                    if (named) {
                        mWriter.setStringName(id, varName);
                    }
                    mPaths.put(varName, id);
                } else if (varType.equals("color")) {
                    boolean commit = command.optBoolean("commit", false);
                    if (mInFirstPass || commit) {
                        int colorVal = parseColor(command.getString("value"));
                        int colorId;
                        if (named) {
                            colorId = mWriter.addNamedColor(varName, colorVal);
                        } else {
                            colorId = mWriter.addColor(colorVal);
                        }
                        mVariables.put(varName, (float) colorId);
                    } else {
                        mDeferredVariables.put(varName, command);
                    }
                } else if (varType.equals("string")) {
                    boolean commit = command.optBoolean("commit", false);
                    if (mInFirstPass || commit) {
                        int textId = resolveTextId(command.get("value"));
                        mVariables.put(varName, (float) textId);
                        if (named) {
                            mWriter.setStringName(textId, varName);
                        }
                    } else {
                        mDeferredVariables.put(varName, command);
                    }
                } else {
                    boolean commit = command.optBoolean("commit", false);
                    boolean flush = command.optBoolean("flush", false);
                    if (mInFirstPass || commit || flush) {
                        float varVal;
                        Object val = command.get("value");
                        int targetId = -1;
                        if (mVariables.containsKey(varName)) {
                            float existingVal = mVariables.get(varName);
                            if (Float.isNaN(existingVal)) {
                                targetId = Utils.idFromNan(existingVal);
                            } else {
                                targetId = (int) existingVal;
                            }
                        }

                        if (targetId != -1) {
                            if (val instanceof org.json.JSONArray
                                    || val instanceof org.json.JSONObject) {
                                varVal = Utils.asNan(targetId);
                            } else if (val instanceof String && isMathExpression((String) val)) {
                                float[] exp = parseFloatExpression(val);
                                mWriter.getBuffer().addAnimatedFloat(targetId, exp);
                                varVal = Utils.asNan(targetId);
                            } else {
                                float floatVal;
                                if (val instanceof Number) {
                                    floatVal = ((Number) val).floatValue();
                                } else {
                                    floatVal = parseFloat(val);
                                }
                                if (Float.isNaN(floatVal)) {
                                    varVal = floatVal;
                                } else {
                                    mWriter.getBuffer().addFloat(targetId, floatVal);
                                    varVal = Utils.asNan(targetId);
                                }
                            }
                        } else if (val instanceof JSONObject) {
                            JSONObject vo = (JSONObject) val;
                            if (vo.optString("type", "").equals("textFromFloat")) {
                                float floatVal = parseFloat(vo.get("value"));
                                int after = vo.optInt("decimal", 3);
                                int before = vo.optInt("whole", 0);
                                int flags = vo.optInt("flags", 0);
                                varVal = (float) mWriter.createTextFromFloat(floatVal, before,
                                        after, flags);
                            } else if (vo.optString("type", "")
                                    .equals("textMerge")) {
                                int id1 = resolveTextId(vo.get("id1"));
                                int id2 = resolveTextId(vo.get("id2"));
                                varVal = (float) mWriter.textMerge(id1, id2);
                            } else {
                                varVal = parseFloat(val);
                            }
                        } else if (flush) {
                            if (val instanceof JSONArray) {
                                JSONArray arr = (JSONArray) val;
                                float[] data = new float[arr.length()];
                                for (int i = 0; i < arr.length(); i++) {
                                    data[i] = Float.intBitsToFloat(arr.getInt(i));
                                }
                                varVal = mWriter.floatExpression(data);
                            } else {
                                float[] exp = parseFloatExpression(val);
                                varVal = mWriter.floatExpression(exp);
                            }
                            if (named) {
                                mWriter.setFloatName(Utils.idFromNan(varVal), varName);
                            }
                        } else if (named) {
                            varVal = parseFloat(val);
                            if (Float.isNaN(varVal)) {
                                mWriter.setFloatName(Utils.idFromNan(varVal), varName);
                            } else {
                                varVal = mWriter.addNamedFloat(varName, varVal);
                            }
                        } else {
                            if (val instanceof Number) {
                                varVal = mWriter.addFloatConstant(((Number) val).floatValue());
                            } else {
                                varVal = parseFloat(val);
                            }
                        }
                        mVariables.put(varName, varVal);
                    } else {
                        mDeferredVariables.put(varName, command);
                    }
                }
                break;
            }
            case "conditionalOperations": {
                String conditionStr = command.getString("condition");
                byte type_val = 0;
                switch (conditionStr.toLowerCase()) {
                    case "gt": type_val = (byte) ConditionalOperations.TYPE_GT; break;
                    case "ge": type_val = (byte) ConditionalOperations.TYPE_GTE; break;
                    case "lt": type_val = (byte) ConditionalOperations.TYPE_LT; break;
                    case "le": type_val = (byte) ConditionalOperations.TYPE_LTE; break;
                    case "eq": type_val = (byte) ConditionalOperations.TYPE_EQ; break;
                }
                float v1 = parseFloat(command.get("v1"));
                float v2 = parseFloat(command.get("v2"));
                mWriter.conditionalOperations(type_val, v1, v2);
                parseCommands(command.optJSONArray("commands"));
                mWriter.endConditionalOperations();
                break;
            }
            case "global": {
                beginGlobal();
                parseCommands(command.optJSONArray("commands"));
                endGlobal();
                break;
            }
            case "save":
                mWriter.save();
                if (command.has("commands")) {
                    JSONArray saveCommands = command.getJSONArray("commands");
                    for (int j = 0; j < saveCommands.length(); j++) {
                        parseCommand(saveCommands.getJSONObject(j));
                    }
                    mWriter.restore();
                }
                break;
            case "restore":
                mWriter.restore();
                break;
            case "clipRect":
                mWriter.clipRect(
                        parseFloat(command.get("left")),
                        parseFloat(command.get("top")),
                        parseFloat(command.get("right")),
                        parseFloat(command.get("bottom"))
                );
                break;
            case "scale":
                mWriter.scale(
                        parseFloat(command.get("sx")),
                        parseFloat(command.get("sy"))
                );
                break;
            case "resources":
                if (command.has("resources")) {
                    mResourceParser.parseResources(command.getJSONObject("resources"));
                } else {
                    mResourceParser.parseResources(command);
                }
                break;
        }
    }

    @NonNull Shape parseShape(@NonNull JSONObject obj) throws JSONException {
        String type = obj.getString("type").toLowerCase();
        switch (type) {
            case "circle":
                return new CircleShape();
            case "rect":
                return new RectShape(
                        (float) obj.optDouble("left", 0),
                        (float) obj.optDouble("top", 0),
                        (float) obj.optDouble("right", 100),
                        (float) obj.optDouble("bottom", 100)
                );
            case "roundrect":
            case "roundedrect":
                if (obj.has("radius")) {
                    float r = (float) obj.optDouble("radius", 0);
                    return new RoundedRectShape(r, r, r, r);
                } else {
                    return new RoundedRectShape(
                            (float) obj.optDouble("topStart", 0),
                            (float) obj.optDouble("topEnd", 0),
                            (float) obj.optDouble("bottomStart", 0),
                            (float) obj.optDouble("bottomEnd", 0)
                    );
                }
            default:
                throw new JSONException("Unknown shape type: " + type);
        }
    }

    @NonNull RecordingModifier parseModifiers(
            @Nullable JSONArray modifiers) throws JSONException {
        RecordingModifier recordingModifier = new RecordingModifier();
        if (modifiers == null) return recordingModifier;
        for (int i = 0; i < modifiers.length(); i++) {
            Object item = modifiers.get(i);
            JSONObject mod;
            String key;
            if (item instanceof String) {
                key = (String) item;
                mod = new JSONObject();
                mod.put(key, "NaN");
            } else {
                mod = (JSONObject) item;
                key = mod.keys().next();
            }
            JsonModifierParser modParser = mModifierParsers.get(key.toLowerCase());
            if (modParser != null) {
                modParser.parse(mod, key, recordingModifier, this);
            }
        }
        return recordingModifier;
    }

    float resolveDeferredVariable(String name) throws JSONException {
        Object commandObj = mDeferredVariables.remove(name);
        if (commandObj instanceof JSONObject) {
            JSONObject command = (JSONObject) commandObj;
            boolean named = command.optBoolean("export", false);
            float varVal;
            Object val = command.get("value");
            if (val instanceof JSONObject) {
                JSONObject vo = (JSONObject) val;
                if (vo.has("type") && vo.getString("type").equals("textFromFloat")) {
                    float floatVal = parseFloat(vo.get("value"));
                    int after = vo.optInt("after", 3);
                    if (vo.has("decimal")) after = vo.optInt("decimal", 3);
                    int before = vo.optInt("before", 0);
                    if (vo.has("whole")) before = vo.optInt("whole", 0);
                    int flags = vo.optInt("flags", 0);
                    int textId = mWriter.createTextFromFloat(floatVal, before, after, flags);
                    mVariables.put(name, (float) textId);
                    if (named) {
                        mWriter.setStringName(textId, name);
                    }
                    return (float) textId;
                }
                if (vo.has("type") && vo.getString("type").equals("textMerge")) {
                    int id1 = resolveTextId(vo.get("id1"));
                    int id2 = resolveTextId(vo.get("id2"));
                    int mergedId = mWriter.textMerge(id1, id2);
                    mVariables.put(name, (float) mergedId);
                    if (named) {
                        mWriter.setStringName(mergedId, name);
                    }
                    return (float) mergedId;
                }
            }
            String varType = command.optString("vtype", "float");
            if (varType.equals("string")) {
                int textId = mWriter.textCreateId(command.getString("value"));
                mVariables.put(name, (float) textId);
                if (named) {
                    mWriter.setStringName(textId, name);
                }
                return (float) textId;
            }
            if (named) {
                varVal = parseFloat(val);
                if (Float.isNaN(varVal)) {
                    mWriter.setFloatName(Utils.idFromNan(varVal), name);
                } else {
                    varVal = mWriter.addNamedFloat(name, varVal);
                }
            } else {
                if (val instanceof Number) {
                    varVal = mWriter.addFloatConstant(((Number) val).floatValue());
                } else {
                    varVal = parseFloat(val);
                }
            }
            mVariables.put(name, varVal);
            return varVal;
        }
        return Float.NaN;
    }

    float parseFloat(@Nullable Object value) throws JSONException {
        if (value == null) return Float.NaN;
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else if (value instanceof String) {
            String s = (String) value;
            if (s.equals("NaN")) return Float.NaN;
            if (s.equals("Infinity")) return Float.POSITIVE_INFINITY;
            if (s.equals("-Infinity")) return Float.NEGATIVE_INFINITY;
            if (s.equals("max")) return Float.MAX_VALUE;
            if (isVariableRef(s) && s.indexOf(' ') == -1) {
                String name = getVariableNameFromRef(s);
                Float id = mVariables.get(name);
                if (id != null) return id;
                if (mDeferredVariables.containsKey(name)) {
                    return resolveDeferredVariable(name);
                }
            }
            if ((s.startsWith("$matrices.") || s.startsWith("@matrices."))
                    && s.indexOf(' ') == -1 && s.length() > 10) {
                String name = s.substring(10);
                Float id = mMatrices.get(name);
                if (id != null) return id;
            }
            if (mExpressionParser.isVariable(s)) {
                return mExpressionParser.getVariableNan(s);
            }
            return mExpressionParser.parseExpression(s);
        } else if (value instanceof JSONObject) {
            return mExpressionParser.parseExpression(value);
        }
        return 0.0f;
    }

    int parseColor(@Nullable Object value) throws JSONException {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            String colorStr = (String) value;
            if (colorStr.startsWith("$colors.") || colorStr.startsWith("@colors.")) {
                String name = colorStr.substring(8);
                Integer id = mColors.get(name);
                if (DEBUG) {
                    System.out.println("### parseColor: name=" + name + " id=" + id);
                }
                if (id == null) throw new JSONException("Color not found: " + name);
                return id;
            }
            if (isVariableRef(colorStr)) {
                String name = getVariableNameFromRef(colorStr);
                Float id = mVariables.get(name);
                if (id != null) {
                    if (Float.isNaN(id)) {
                        return androidx.compose.remote.core.operations.Utils.idFromNan(id);
                    }
                    return id.intValue();
                }
                if (mDeferredVariables.containsKey(name)) {
                    return (int) resolveDeferredVariable(name);
                }
            }
            if (colorStr.startsWith("#")) {
                long rawColor = Long.parseLong(colorStr.substring(1), 16);
                if (colorStr.length() <= 7) {
                    rawColor |= 0xFF000000L;
                }
                return (int) rawColor;
            }
        }
        return 0;
    }

    int parsePath(@NonNull String pathStr) throws JSONException {
        Integer id = mPaths.get(pathStr);
        if (id != null) return id;
        if (pathStr.startsWith("$paths.") || pathStr.startsWith("@paths.")) {
            String name = pathStr.substring(7);
            id = mPaths.get(name);
            if (id == null) throw new JSONException("Path not found: " + name);
            return id;
        }
        return mWriter.addPathString(pathStr);
    }



    float parseMatrixOperator(String op) {
        switch (op) {
            case "IDENTITY": return MatrixOperations.IDENTITY;
            case "TRANSLATE_X": return MatrixOperations.TRANSLATE_X;
            case "TRANSLATE_Y": return MatrixOperations.TRANSLATE_Y;
            case "TRANSLATE_Z": return MatrixOperations.TRANSLATE_Z;
            case "TRANSLATE2": return MatrixOperations.TRANSLATE2;
            case "TRANSLATE3": return MatrixOperations.TRANSLATE3;
            case "SCALE_X": return MatrixOperations.SCALE_X;
            case "SCALE_Y": return MatrixOperations.SCALE_Y;
            case "SCALE_Z": return MatrixOperations.SCALE_Z;
            case "SCALE2": return MatrixOperations.SCALE2;
            case "SCALE3": return MatrixOperations.SCALE3;
            case "ROT_X": return MatrixOperations.ROT_X;
            case "ROT_Y": return MatrixOperations.ROT_Y;
            case "ROT_Z": return MatrixOperations.ROT_Z;
            case "ROT_PZ": return MatrixOperations.ROT_PZ;
            case "ROT_AXIS": return MatrixOperations.ROT_AXIS;
            case "MUL": return MatrixOperations.MUL;
            case "PROJECTION": return MatrixOperations.PROJECTION;
            default: return 0;
        }
    }

    int resolveTextId(Object textObj) throws JSONException {
        if (textObj instanceof Number) {
            Number num = (Number) textObj;
            if (num instanceof Double || num instanceof Float
                    || num instanceof java.math.BigDecimal) {
                float val = num.floatValue();
                return androidx.compose.remote.core.operations.Utils.idFromNan(
                        mWriter.addFloatConstant(val));
            }
            return num.intValue();
        } else if (textObj instanceof String) {
            String str = (String) textObj;
            if (isVariableRef(str)) {
                String name = getVariableNameFromRef(str);
                Float val = mVariables.get(name);
                if (val == null && mDeferredVariables.containsKey(name)) {
                    val = resolveDeferredVariable(name);
                }
                if (val != null) {
                    if (Float.isNaN(val)) {
                        return androidx.compose.remote.core.operations.Utils.idFromNan(val);
                    }
                    return (int) val.floatValue();
                } else {
                    throw new JSONException("Variable not found: " + name);
                }
            } else {
                return mWriter.textCreateId(str);
            }
        } else if (textObj instanceof JSONObject) {
            JSONObject vo = (JSONObject) textObj;
            if (vo.has("type") && vo.getString("type").equals("textMerge")) {
                int id1 = resolveTextId(vo.get("id1"));
                int id2 = resolveTextId(vo.get("id2"));
                return mWriter.textMerge(id1, id2);
            } else if (vo.has("type") && vo.getString("type").equals("textFromFloat")) {
                float val = parseFloat(vo.get("value"));
                int after = vo.optInt("after", 3);
                if (vo.has("decimal")) after = vo.optInt("decimal", 3);
                int before = vo.optInt("before", 0);
                if (vo.has("whole")) before = vo.optInt("whole", 0);
                int flags = vo.optInt("flags", 0);
                return mWriter.createTextFromFloat(val, before, after, flags);
            } else {
                throw new JSONException("Unsupported text object type: " + vo.optString("type"));
            }
        } else {
            throw new JSONException("Invalid text parameter: " + textObj);
        }
    }

    float[] parseFloatExpression(Object expObj) throws JSONException {
        if (expObj instanceof String) {
            String expStr = (String) expObj;
            List<Object> rpn = mExpressionParser.infixToRpn(expStr);
            float[] exp = new float[rpn.size()];
            for (int i = 0; i < rpn.size(); i++) {
                Object o = rpn.get(i);
                if (o instanceof Float) {
                    exp[i] = (Float) o;
                } else if (o instanceof Integer) {
                    exp[i] = Utils.asNan((Integer) o);
                } else if (o instanceof Number) {
                    exp[i] = ((Number) o).floatValue();
                } else {
                    throw new JSONException("Unexpected non-numeric token in RPN: " + o);
                }
            }
            return exp;
        } else {
            throw new JSONException("Invalid float expression: " + expObj);
        }
    }

    boolean isMathExpression(String s) {
        return s.contains("+") || s.contains("-") || s.contains("*") || s.contains("/")
                || s.contains("%") || s.contains("(") || s.contains(")") || s.contains(",");
    }

    static boolean isVariableRef(String s) {
        if (s == null || s.length() < 2) {
            return false;
        }
        if (!s.startsWith("$") && !s.startsWith("@")) {
            return false;
        }
        if (s.startsWith("$colors.") || s.startsWith("@colors.")) {
            return false;
        }
        if (s.startsWith("$paths.") || s.startsWith("@paths.")) {
            return false;
        }
        if (s.startsWith("$matrices.") || s.startsWith("@matrices.")) {
            return false;
        }
        return true;
    }

    static String getVariableNameFromRef(String s) {
        if (!isVariableRef(s)) {
            return null;
        }
        if (s.startsWith("$vars.") || s.startsWith("@vars.")) {
            return s.substring(6);
        }
        return s.substring(1);
    }
}
