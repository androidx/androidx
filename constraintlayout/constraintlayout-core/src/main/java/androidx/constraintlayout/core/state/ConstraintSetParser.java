/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.core.state;

import static androidx.constraintlayout.core.motion.utils.TypedValues.MotionType.TYPE_QUANTIZE_INTERPOLATOR_TYPE;
import static androidx.constraintlayout.core.motion.utils.TypedValues.MotionType.TYPE_QUANTIZE_MOTIONSTEPS;
import static androidx.constraintlayout.core.motion.utils.TypedValues.MotionType.TYPE_QUANTIZE_MOTION_PHASE;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.HORIZONTAL;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.VERTICAL;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.constraintlayout.core.motion.utils.TypedBundle;
import androidx.constraintlayout.core.motion.utils.TypedValues;
import androidx.constraintlayout.core.parser.CLArray;
import androidx.constraintlayout.core.parser.CLElement;
import androidx.constraintlayout.core.parser.CLKey;
import androidx.constraintlayout.core.parser.CLNumber;
import androidx.constraintlayout.core.parser.CLObject;
import androidx.constraintlayout.core.parser.CLParser;
import androidx.constraintlayout.core.parser.CLParsingException;
import androidx.constraintlayout.core.parser.CLString;
import androidx.constraintlayout.core.state.helpers.BarrierReference;
import androidx.constraintlayout.core.state.helpers.ChainReference;
import androidx.constraintlayout.core.state.helpers.FlowReference;
import androidx.constraintlayout.core.state.helpers.GridReference;
import androidx.constraintlayout.core.state.helpers.GuidelineReference;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.Flow;

import java.util.ArrayList;
import java.util.HashMap;

public class ConstraintSetParser {

    private static final boolean PARSER_DEBUG = false;

    public static class DesignElement {
        String mId;
        String mType;
        HashMap<String, String> mParams;

        public String getId() {
            return mId;
        }

        public String getType() {
            return mType;
        }

        public HashMap<String, String> getParams() {
            return mParams;
        }

        DesignElement(String id,
                String type,
                HashMap<String, String> params) {
            mId = id;
            mType = type;
            mParams = params;
        }
    }

    /**
     * Provide the storage for managing Variables in the system.
     * When the json has a variable:{   } section this is used.
     */
    public static class LayoutVariables {
        HashMap<String, Integer> mMargins = new HashMap<>();
        HashMap<String, GeneratedValue> mGenerators = new HashMap<>();
        HashMap<String, ArrayList<String>> mArrayIds = new HashMap<>();

        void put(String elementName, int element) {
            mMargins.put(elementName, element);
        }

        void put(String elementName, float start, float incrementBy) {
            if (mGenerators.containsKey(elementName)) {
                if (mGenerators.get(elementName) instanceof OverrideValue) {
                    return;
                }
            }
            mGenerators.put(elementName, new Generator(start, incrementBy));
        }

        void put(String elementName,
                float from,
                float to,
                float step,
                String prefix,
                String postfix) {
            if (mGenerators.containsKey(elementName)) {
                if (mGenerators.get(elementName) instanceof OverrideValue) {
                    return;
                }
            }
            FiniteGenerator generator =
                    new FiniteGenerator(from, to, step, prefix, postfix);
            mGenerators.put(elementName, generator);
            mArrayIds.put(elementName, generator.array());

        }

        /**
         * insert an override variable
         *
         * @param elementName the name
         * @param value       the value a float
         */
        public void putOverride(String elementName, float value) {
            GeneratedValue generator = new OverrideValue(value);
            mGenerators.put(elementName, generator);
        }

        float get(Object elementName) {
            if (elementName instanceof CLString) {
                String stringValue = ((CLString) elementName).content();
                if (mGenerators.containsKey(stringValue)) {
                    return mGenerators.get(stringValue).value();
                }
                if (mMargins.containsKey(stringValue)) {
                    return mMargins.get(stringValue).floatValue();
                }
            } else if (elementName instanceof CLNumber) {
                return ((CLNumber) elementName).getFloat();
            }
            return 0f;
        }

        ArrayList<String> getList(String elementName) {
            if (mArrayIds.containsKey(elementName)) {
                return mArrayIds.get(elementName);
            }
            return null;
        }

        void put(String elementName, ArrayList<String> elements) {
            mArrayIds.put(elementName, elements);
        }

    }

    interface GeneratedValue {
        float value();
    }

    /**
     * Generate a floating point value
     */
    static class Generator implements GeneratedValue {
        float mStart = 0;
        float mIncrementBy = 0;
        float mCurrent = 0;
        boolean mStop = false;

        Generator(float start, float incrementBy) {
            mStart = start;
            mIncrementBy = incrementBy;
            mCurrent = start;
        }

        @Override
        public float value() {
            if (!mStop) {
                mCurrent += mIncrementBy;
            }
            return mCurrent;
        }
    }

    /**
     * Generate values like button1, button2 etc.
     */
    static class FiniteGenerator implements GeneratedValue {
        float mFrom = 0;
        float mTo = 0;
        float mStep = 0;
        boolean mStop = false;
        String mPrefix;
        String mPostfix;
        float mCurrent = 0;
        float mInitial;
        float mMax;

        FiniteGenerator(float from,
                float to,
                float step,
                String prefix,
                String postfix) {
            mFrom = from;
            mTo = to;
            mStep = step;
            mPrefix = (prefix == null) ? "" : prefix;
            mPostfix = (postfix == null) ? "" : postfix;
            mMax = to;
            mInitial = from;
        }

        @Override
        public float value() {
            if (mCurrent >= mMax) {
                mStop = true;
            }
            if (!mStop) {
                mCurrent += mStep;
            }
            return mCurrent;
        }

        public ArrayList<String> array() {
            ArrayList<String> array = new ArrayList<>();
            int value = (int) mInitial;
            int maxInt = (int) mMax;
            for (int i = value; i <= maxInt; i++) {
                array.add(mPrefix + value + mPostfix);
                value += (int) mStep;
            }
            return array;

        }

    }

    static class OverrideValue implements GeneratedValue {
        float mValue;

        OverrideValue(float value) {
            mValue = value;
        }

        @Override
        public float value() {
            return mValue;
        }
    }
//==================== end store variables =========================
//==================== MotionScene =========================

    public enum MotionLayoutDebugFlags {
        NONE,
        SHOW_ALL,
        UNKNOWN
    }

    //==================== end Motion Scene =========================

    /**
     * Parse and populate a transition
     *
     * @param content    JSON string to parse
     * @param transition The Transition to be populated
     * @param state
     */
    public static void parseJSON(String content, Transition transition, int state) {
        try {
            CLObject json = CLParser.parse(content);
            ArrayList<String> elements = json.names();
            if (elements == null) {
                return;
            }
            for (String elementName : elements) {
                CLElement base_element = json.get(elementName);
                if (base_element instanceof CLObject) {
                    CLObject element = (CLObject) base_element;
                    CLObject customProperties = element.getObjectOrNull("custom");
                    if (customProperties != null) {
                        ArrayList<String> properties = customProperties.names();
                        for (String property : properties) {
                            CLElement value = customProperties.get(property);
                            if (value instanceof CLNumber) {
                                transition.addCustomFloat(
                                        state,
                                        elementName,
                                        property,
                                        value.getFloat()
                                );
                            } else if (value instanceof CLString) {
                                long color = parseColorString(value.content());
                                if (color != -1) {
                                    transition.addCustomColor(state,
                                            elementName, property, (int) color);
                                }
                            }
                        }
                    }
                }

            }
        } catch (CLParsingException e) {
            System.err.println("Error parsing JSON " + e);
        }
    }

    /**
     * Parse and build a motionScene
     *
     * this should be in a MotionScene / MotionSceneParser
     */
    public static void parseMotionSceneJSON(CoreMotionScene scene, String content) {
        try {
            CLObject json = CLParser.parse(content);
            ArrayList<String> elements = json.names();
            if (elements == null) {
                return;
            }
            for (String elementName : elements) {
                CLElement element = json.get(elementName);
                if (element instanceof CLObject) {
                    CLObject clObject = (CLObject) element;
                    switch (elementName) {
                        case "ConstraintSets":
                            parseConstraintSets(scene, clObject);
                            break;
                        case "Transitions":
                            parseTransitions(scene, clObject);
                            break;
                        case "Header":
                            parseHeader(scene, clObject);
                            break;
                    }
                }
            }
        } catch (CLParsingException e) {
            System.err.println("Error parsing JSON " + e);
        }
    }

    /**
     * Parse ConstraintSets and populate MotionScene
     */
    static void parseConstraintSets(CoreMotionScene scene,
            CLObject json) throws CLParsingException {
        ArrayList<String> constraintSetNames = json.names();
        if (constraintSetNames == null) {
            return;
        }

        for (String csName : constraintSetNames) {
            CLObject constraintSet = json.getObject(csName);
            boolean added = false;
            String ext = constraintSet.getStringOrNull("Extends");
            if (ext != null && !ext.isEmpty()) {
                String base = scene.getConstraintSet(ext);
                if (base == null) {
                    continue;
                }

                CLObject baseJson = CLParser.parse(base);
                ArrayList<String> widgetsOverride = constraintSet.names();
                if (widgetsOverride == null) {
                    continue;
                }

                for (String widgetOverrideName : widgetsOverride) {
                    CLElement value = constraintSet.get(widgetOverrideName);
                    if (value instanceof CLObject) {
                        override(baseJson, widgetOverrideName, (CLObject) value);
                    }
                }

                scene.setConstraintSetContent(csName, baseJson.toJSON());
                added = true;
            }
            if (!added) {
                scene.setConstraintSetContent(csName, constraintSet.toJSON());
            }
        }

    }

    static void override(CLObject baseJson,
            String name, CLObject overrideValue) throws CLParsingException {
        if (!baseJson.has(name)) {
            baseJson.put(name, overrideValue);
        } else {
            CLObject base = baseJson.getObject(name);
            ArrayList<String> keys = overrideValue.names();
            for (String key : keys) {
                if (!key.equals("clear")) {
                    base.put(key, overrideValue.get(key));
                    continue;
                }
                CLArray toClear = overrideValue.getArray("clear");
                for (int i = 0; i < toClear.size(); i++) {
                    String clearedKey = toClear.getStringOrNull(i);
                    if (clearedKey == null) {
                        continue;
                    }
                    switch (clearedKey) {
                        case "dimensions":
                            base.remove("width");
                            base.remove("height");
                            break;
                        case "constraints":
                            base.remove("start");
                            base.remove("end");
                            base.remove("top");
                            base.remove("bottom");
                            base.remove("baseline");
                            base.remove("center");
                            base.remove("centerHorizontally");
                            base.remove("centerVertically");
                            break;
                        case "transforms":
                            base.remove("visibility");
                            base.remove("alpha");
                            base.remove("pivotX");
                            base.remove("pivotY");
                            base.remove("rotationX");
                            base.remove("rotationY");
                            base.remove("rotationZ");
                            base.remove("scaleX");
                            base.remove("scaleY");
                            base.remove("translationX");
                            base.remove("translationY");
                            break;
                        default:
                            base.remove(clearedKey);

                    }
                }
            }
        }
    }

    /**
     * Parse the Transition
     */
    static void parseTransitions(CoreMotionScene scene, CLObject json) throws CLParsingException {
        ArrayList<String> elements = json.names();
        if (elements == null) {
            return;
        }
        for (String elementName : elements) {
            scene.setTransitionContent(elementName, json.getObject(elementName).toJSON());
        }
    }

    /**
     * Used to parse for "export"
     */
    static void parseHeader(CoreMotionScene scene, CLObject json) {
        String name = json.getStringOrNull("export");
        if (name != null) {
            scene.setDebugName(name);
        }
    }

    /**
     * Top leve parsing of the json ConstraintSet supporting
     * "Variables", "Helpers", "Generate", guidelines, and barriers
     *
     * @param content         the JSON string
     * @param state           the state to populate
     * @param layoutVariables the variables to override
     */
    public static void parseJSON(String content, State state,
                                 LayoutVariables layoutVariables) throws CLParsingException {
        try {
            CLObject json = CLParser.parse(content);
            populateState(json, state, layoutVariables);
        } catch (CLParsingException e) {
            System.err.println("Error parsing JSON " + e);
        }
    }

    /**
     * Populates the given {@link State} with the parameters from {@link CLObject}. Where the
     * object represents a parsed JSONObject of a ConstraintSet.
     *
     * @param parsedJson CLObject of the parsed ConstraintSet
     * @param state the state to populate
     * @param layoutVariables the variables to override
     * @throws CLParsingException when parsing fails
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void populateState(
            @NonNull CLObject parsedJson,
            @NonNull State state,
            @NonNull LayoutVariables layoutVariables
    ) throws CLParsingException {
        ArrayList<String> elements = parsedJson.names();
        if (elements == null) {
            return;
        }
        for (String elementName : elements) {
            CLElement element = parsedJson.get(elementName);
            if (PARSER_DEBUG) {
                System.out.println("[" + elementName + "] = " + element
                        + " > " + element.getContainer());
            }
            switch (elementName) {
                case "Variables":
                    if (element instanceof CLObject) {
                        parseVariables(state, layoutVariables, (CLObject) element);
                    }
                    break;
                case "Helpers":
                    if (element instanceof CLArray) {
                        parseHelpers(state, layoutVariables, (CLArray) element);
                    }
                    break;
                case "Generate":
                    if (element instanceof CLObject) {
                        parseGenerate(state, layoutVariables, (CLObject) element);
                    }
                    break;
                default:
                    if (element instanceof CLObject) {
                        String type = lookForType((CLObject) element);
                        if (type != null) {
                            switch (type) {
                                case "hGuideline":
                                    parseGuidelineParams(
                                            ConstraintWidget.HORIZONTAL,
                                            state,
                                            elementName,
                                            (CLObject) element
                                    );
                                    break;
                                case "vGuideline":
                                    parseGuidelineParams(
                                            ConstraintWidget.VERTICAL,
                                            state,
                                            elementName,
                                            (CLObject) element
                                    );
                                    break;
                                case "barrier":
                                    parseBarrier(state, elementName, (CLObject) element);
                                    break;
                                case "vChain":
                                case "hChain":
                                    parseChainType(
                                            type,
                                            state,
                                            elementName,
                                            layoutVariables,
                                            (CLObject) element
                                    );
                                    break;
                                case "vFlow":
                                case "hFlow":
                                    parseFlowType(
                                            type,
                                            state,
                                            elementName,
                                            layoutVariables,
                                            (CLObject) element
                                    );
                                    break;
                                case "grid":
                                case "row":
                                case "column":
                                    parseGridType(
                                            type,
                                            state,
                                            elementName,
                                            layoutVariables,
                                            (CLObject) element
                                    );
                                    break;
                            }
                        } else {
                            parseWidget(state,
                                    layoutVariables,
                                    elementName,
                                    (CLObject) element);
                        }
                    } else if (element instanceof CLNumber) {
                        layoutVariables.put(elementName, element.getInt());
                    }
            }
        }
    }

    private static void parseVariables(State state,
            LayoutVariables layoutVariables,
            CLObject json) throws CLParsingException {
        ArrayList<String> elements = json.names();
        if (elements == null) {
            return;
        }
        for (String elementName : elements) {
            CLElement element = json.get(elementName);
            if (element instanceof CLNumber) {
                layoutVariables.put(elementName, element.getInt());
            } else if (element instanceof CLObject) {
                CLObject obj = (CLObject) element;
                ArrayList<String> arrayIds;
                if (obj.has("from") && obj.has("to")) {
                    float from = layoutVariables.get(obj.get("from"));
                    float to = layoutVariables.get(obj.get("to"));
                    String prefix = obj.getStringOrNull("prefix");
                    String postfix = obj.getStringOrNull("postfix");
                    layoutVariables.put(elementName, from, to, 1f, prefix, postfix);
                } else if (obj.has("from") && obj.has("step")) {
                    float start = layoutVariables.get(obj.get("from"));
                    float increment = layoutVariables.get(obj.get("step"));
                    layoutVariables.put(elementName, start, increment);

                } else if (obj.has("ids")) {
                    CLArray ids = obj.getArray("ids");
                    arrayIds = new ArrayList<>();
                    for (int i = 0; i < ids.size(); i++) {
                        arrayIds.add(ids.getString(i));
                    }
                    layoutVariables.put(elementName, arrayIds);
                } else if (obj.has("tag")) {
                    arrayIds = state.getIdsForTag(obj.getString("tag"));
                    layoutVariables.put(elementName, arrayIds);
                }
            }
        }
    }

    /**
     * parse the Design time elements.
     *
     * @param content the json
     * @param list    output the list of design elements
     */
    public static void parseDesignElementsJSON(
            String content, ArrayList<DesignElement> list) throws CLParsingException {
        CLObject json = CLParser.parse(content);
        ArrayList<String> elements = json.names();
        if (elements == null) {
            return;
        }
        for (int i = 0; i < elements.size(); i++) {
            String elementName = elements.get(i);
            CLElement element = json.get(elementName);
            if (PARSER_DEBUG) {
                System.out.println("[" + element + "] " + element.getClass());
            }
            switch (elementName) {
                case "Design":
                    if (!(element instanceof CLObject)) {
                        return;
                    }
                    CLObject obj = (CLObject) element;
                    elements = obj.names();
                    for (int j = 0; j < elements.size(); j++) {
                        String designElementName = elements.get(j);
                        CLObject designElement =
                                (CLObject) ((CLObject) element).get(designElementName);
                        System.out.printf("element found " + designElementName + "");
                        String type = designElement.getStringOrNull("type");
                        if (type != null) {
                            HashMap<String, String> parameters = new HashMap<String, String>();
                            int size = designElement.size();
                            for (int k = 0; k < size; k++) {

                                CLKey key = (CLKey) designElement.get(j);
                                String paramName = key.content();
                                String paramValue = key.getValue().content();
                                if (paramValue != null) {
                                    parameters.put(paramName, paramValue);
                                }
                            }
                            list.add(new DesignElement(elementName, type, parameters));
                        }
                    }
            }
            break;
        }
    }

    static void parseHelpers(State state,
            LayoutVariables layoutVariables,
            CLArray element) throws CLParsingException {
        for (int i = 0; i < element.size(); i++) {
            CLElement helper = element.get(i);
            if (helper instanceof CLArray) {
                CLArray array = (CLArray) helper;
                if (array.size() > 1) {
                    switch (array.getString(0)) {
                        case "hChain":
                            parseChain(ConstraintWidget.HORIZONTAL, state, layoutVariables, array);
                            break;
                        case "vChain":
                            parseChain(ConstraintWidget.VERTICAL, state, layoutVariables, array);
                            break;
                        case "hGuideline":
                            parseGuideline(ConstraintWidget.HORIZONTAL, state, array);
                            break;
                        case "vGuideline":
                            parseGuideline(ConstraintWidget.VERTICAL, state, array);
                            break;
                    }
                }
            }
        }
    }

    static void parseGenerate(State state,
            LayoutVariables layoutVariables,
            CLObject json) throws CLParsingException {
        ArrayList<String> elements = json.names();
        if (elements == null) {
            return;
        }
        for (String elementName : elements) {
            CLElement element = json.get(elementName);
            ArrayList<String> arrayIds = layoutVariables.getList(elementName);
            if (arrayIds != null && element instanceof CLObject) {
                for (String id : arrayIds) {
                    parseWidget(state, layoutVariables, id, (CLObject) element);
                }
            }
        }
    }

    static void parseChain(int orientation, State state,
            LayoutVariables margins, CLArray helper) throws CLParsingException {
        ChainReference chain = (orientation == ConstraintWidget.HORIZONTAL)
                ? state.horizontalChain() : state.verticalChain();
        CLElement refs = helper.get(1);
        if (!(refs instanceof CLArray) || ((CLArray) refs).size() < 1) {
            return;
        }
        for (int i = 0; i < ((CLArray) refs).size(); i++) {
            chain.add(((CLArray) refs).getString(i));
        }

        if (helper.size() > 2) { // we have additional parameters
            CLElement params = helper.get(2);
            if (!(params instanceof CLObject)) {
                return;
            }
            CLObject obj = (CLObject) params;
            ArrayList<String> constraints = obj.names();
            for (String constraintName : constraints) {
                switch (constraintName) {
                    case "style":
                        CLElement styleObject = ((CLObject) params).get(constraintName);
                        String styleValue;
                        if (styleObject instanceof CLArray && ((CLArray) styleObject).size() > 1) {
                            styleValue = ((CLArray) styleObject).getString(0);
                            float biasValue = ((CLArray) styleObject).getFloat(1);
                            chain.bias(biasValue);
                        } else {
                            styleValue = styleObject.content();
                        }
                        switch (styleValue) {
                            case "packed":
                                chain.style(State.Chain.PACKED);
                                break;
                            case "spread_inside":
                                chain.style(State.Chain.SPREAD_INSIDE);
                                break;
                            default:
                                chain.style(State.Chain.SPREAD);
                                break;
                        }

                        break;
                    default:
                        parseConstraint(
                                state,
                                margins,
                                (CLObject) params,
                                (ConstraintReference) chain,
                                constraintName
                        );
                        break;
                }
            }
        }
    }

    private static float toPix(State state, float dp) {
        return state.getDpToPixel().toPixels(dp);
    }

    /**
     * Support parsing Chain in the following manner
     * chainId : {
     *      type:'hChain'  // or vChain
     *      contains: ['id1', 'id2', 'id3' ]
     *      contains: [['id', weight, marginL ,marginR], 'id2', 'id3' ]
     *      start: ['parent', 'start',0],
     *      end: ['parent', 'end',0],
     *      top: ['parent', 'top',0],
     *      bottom: ['parent', 'bottom',0],
     *      style: 'spread'
     * }

     * @throws CLParsingException
     */
    private static void parseChainType(String orientation,
            State state,
            String chainName,
            LayoutVariables margins,
            CLObject object) throws CLParsingException {

        ChainReference chain = (orientation.charAt(0) == 'h')
                ? state.horizontalChain() : state.verticalChain();
        chain.setKey(chainName);

        for (String params : object.names()) {
            switch (params) {
                case "contains":
                    CLElement refs = object.get(params);
                    if (!(refs instanceof CLArray) || ((CLArray) refs).size() < 1) {
                        System.err.println(
                                chainName + " contains should be an array \"" + refs.content()
                                        + "\"");
                        return;
                    }
                    for (int i = 0; i < ((CLArray) refs).size(); i++) {
                        CLElement chainElement = ((CLArray) refs).get(i);
                        if (chainElement instanceof CLArray) {
                            CLArray array = (CLArray) chainElement;
                            if (array.size() > 0) {
                                String id = array.get(0).content();
                                float weight = Float.NaN;
                                float preMargin = Float.NaN;
                                float postMargin = Float.NaN;
                                float preGoneMargin = Float.NaN;
                                float postGoneMargin = Float.NaN;
                                switch (array.size()) {
                                    case 2: // sets only the weight
                                        weight = array.getFloat(1);
                                        break;
                                    case 3: // sets the pre and post margin to the 2 arg
                                        weight = array.getFloat(1);
                                        postMargin = preMargin = toPix(state, array.getFloat(2));
                                        break;
                                    case 4: // sets the pre and post margin
                                        weight = array.getFloat(1);
                                        preMargin = toPix(state, array.getFloat(2));
                                        postMargin = toPix(state, array.getFloat(3));
                                        break;
                                    case 6: // weight, preMargin, postMargin, preGoneMargin,
                                        // postGoneMargin
                                        weight = array.getFloat(1);
                                        preMargin = toPix(state, array.getFloat(2));
                                        postMargin = toPix(state, array.getFloat(3));
                                        preGoneMargin = toPix(state, array.getFloat(4));
                                        postGoneMargin = toPix(state, array.getFloat(5));
                                        break;
                                }
                                chain.addChainElement(id,
                                        weight,
                                        preMargin,
                                        postMargin,
                                        preGoneMargin,
                                        postGoneMargin);
                            }
                        } else {
                            chain.add(chainElement.content());
                        }
                    }
                    break;
                case "start":
                case "end":
                case "top":
                case "bottom":
                case "left":
                case "right":
                    parseConstraint(state, margins, object, chain, params);
                    break;
                case "style":

                    CLElement styleObject = object.get(params);
                    String styleValue;
                    if (styleObject instanceof CLArray && ((CLArray) styleObject).size() > 1) {
                        styleValue = ((CLArray) styleObject).getString(0);
                        float biasValue = ((CLArray) styleObject).getFloat(1);
                        chain.bias(biasValue);
                    } else {
                        styleValue = styleObject.content();
                    }
                    switch (styleValue) {
                        case "packed":
                            chain.style(State.Chain.PACKED);
                            break;
                        case "spread_inside":
                            chain.style(State.Chain.SPREAD_INSIDE);
                            break;
                        default:
                            chain.style(State.Chain.SPREAD);
                            break;
                    }

                    break;
            }
        }
    }

    /**
     * Support parsing Grid in the following manner
     * chainId : {
     *      height: "parent",
     *      width: "parent",
     *      type: "Grid",
     *      vGap: 10,
     *      hGap: 10,
     *      orientation: 0,
     *      rows: 0,
     *      columns: 1,
     *      columnWeights: "",
     *      rowWeights: "",
     *      contains: ["btn1", "btn2", "btn3", "btn4"],
     *      top: ["parent", "top", 10],
     *      bottom: ["parent", "bottom", 20],
     *      right: ["parent", "right", 30],
     *      left: ["parent", "left", 40],
     * }
     *
     * @param gridType type of the Grid helper could be "Grid"|"Row"|"Column"
     * @param state ConstraintLayout State
     * @param name the name of the Grid Helper
     * @param layoutVariables layout margins
     * @param element the element to be parsed
     * @throws CLParsingException
     */
    private static void parseGridType(String gridType,
                                      State state,
                                      String name,
                                      LayoutVariables layoutVariables,
                                      CLObject element) throws CLParsingException {

        GridReference grid = state.getGrid(name, gridType);

        for (String param : element.names()) {
            switch (param) {
                case "contains":
                    CLArray list = element.getArrayOrNull(param);
                    if (list != null) {
                        for (int j = 0; j < list.size(); j++) {

                            String elementNameReference = list.get(j).content();
                            ConstraintReference elementReference =
                                    state.constraints(elementNameReference);
                            grid.add(elementReference);
                        }
                    }
                    break;
                case "orientation":
                    int orientation = element.get(param).getInt();
                    grid.setOrientation(orientation);
                    break;
                case "rows":
                    int rows = element.get(param).getInt();
                    grid.setRowsSet(rows);
                    break;
                case "columns":
                    int columns = element.get(param).getInt();
                    grid.setColumnsSet(columns);
                    break;
                case "hGap":
                    float hGap = element.get(param).getFloat();
                    grid.setHorizontalGaps(toPix(state, hGap));
                    break;
                case "vGap":
                    float vGap = element.get(param).getFloat();
                    grid.setVerticalGaps(toPix(state, vGap));
                    break;
                case "spans":
                    String spans = element.get(param).content();
                    if (spans != null && spans.contains("x") && spans.contains(":")) {
                        grid.setSpans(spans);
                    }
                    break;
                case "skips":
                    String skips = element.get(param).content();
                    if (skips != null && skips.contains("x") && skips.contains(":")) {
                        grid.setSkips(skips);
                    }
                    break;
                case "rowWeights":
                    String rowWeights = element.get(param).content();
                    if (rowWeights != null && rowWeights.contains(",")) {
                        grid.setRowWeights(rowWeights);
                    }
                    break;
                case "columnWeights":
                    String columnWeights = element.get(param).content();
                    if (columnWeights != null && columnWeights.contains(",")) {
                        grid.setColumnWeights(columnWeights);
                    }
                    break;
                default:
                    ConstraintReference reference = state.constraints(name);
                    applyAttribute(state, layoutVariables, reference, element, param);
            }
        }
    }

    /**
     * It's used to parse the Flow type of Helper with the following format:
     * flowID: {
     *    type: 'hFlow'|'vFlow’
     *    wrap: 'chain'|'none'|'aligned',
     *    contains: ['id1', 'id2', 'id3' ] |
     *              [['id1', weight, preMargin , postMargin], 'id2', 'id3'],
     *    vStyle: 'spread'|'spread_inside'|'packed' | ['first', 'middle', 'last'],
     *    hStyle: 'spread'|'spread_inside'|'packed' | ['first', 'middle', 'last'],
     *    vAlign: 'top'|'bottom'|'baseline'|'center',
     *    hAlign: 'start'|'end'|'center',
     *    vGap: 32,
     *    hGap: 23,
     *    padding: 32,
     *    maxElement: 5,
     *    vBias: 0.3 | [0.0, 0.5, 0.5],
     *    hBias: 0.4 | [0.0, 0.5, 0.5],
     *    start: ['parent', 'start', 0],
     *    end: ['parent', 'end', 0],
     *    top: ['parent', 'top', 0],
     *    bottom: ['parent', 'bottom', 0],
     * }
     *
     * @param flowType orientation of the Flow Helper
     * @param state ConstraintLayout State
     * @param flowName the name of the Flow Helper
     * @param layoutVariables layout margins
     * @param element the element to be parsed
     * @throws CLParsingException
     */
    private static void parseFlowType(String flowType,
                                       State state,
                                       String flowName,
                                       LayoutVariables layoutVariables,
                                       CLObject element) throws CLParsingException {
        boolean isVertical = flowType.charAt(0) == 'v';
        FlowReference flow = state.getFlow(flowName, isVertical);

        for (String param : element.names()) {
            switch (param) {
                case "contains":
                    CLElement refs = element.get(param);
                    if (!(refs instanceof CLArray) || ((CLArray) refs).size() < 1) {
                        System.err.println(
                                flowName + " contains should be an array \"" + refs.content()
                                        + "\"");
                        return;
                    }
                    for (int i = 0; i < ((CLArray) refs).size(); i++) {
                        CLElement chainElement = ((CLArray) refs).get(i);
                        if (chainElement instanceof CLArray) {
                            CLArray array = (CLArray) chainElement;
                            if (array.size() > 0) {
                                String id = array.get(0).content();
                                float weight = Float.NaN;
                                float preMargin = Float.NaN;
                                float postMargin = Float.NaN;
                                switch (array.size()) {
                                    case 2: // sets only the weight
                                        weight = array.getFloat(1);
                                        break;
                                    case 3: // sets the pre and post margin to the 2 arg
                                        weight = array.getFloat(1);
                                        postMargin = preMargin = toPix(state, array.getFloat(2));
                                        break;
                                    case 4: // sets the pre and post margin
                                        weight = array.getFloat(1);
                                        preMargin = toPix(state, array.getFloat(2));
                                        postMargin = toPix(state, array.getFloat(3));
                                        break;
                                }
                                flow.addFlowElement(id, weight, preMargin, postMargin);
                            }
                        } else {
                            flow.add(chainElement.content());
                        }
                    }
                    break;
                case "type":
                    if (element.get(param).content().equals("hFlow")) {
                        flow.setOrientation(HORIZONTAL);
                    } else {
                        flow.setOrientation(VERTICAL);
                    }
                    break;
                case "wrap":
                    String wrapValue = element.get(param).content();
                    flow.setWrapMode(State.Wrap.getValueByString(wrapValue));
                    break;
                case "vGap":
                    String vGapValue = element.get(param).content();
                    try {
                        int value = Integer.parseInt(vGapValue);
                        flow.setVerticalGap(value);
                    } catch(NumberFormatException e) {

                    }
                    break;
                case "hGap":
                    String hGapValue = element.get(param).content();
                    try {
                        int value = Integer.parseInt(hGapValue);
                        flow.setHorizontalGap(value);
                    } catch(NumberFormatException e) {

                    }
                    break;
                case "maxElement":
                    String maxElementValue = element.get(param).content();
                    try {
                        int value = Integer.parseInt(maxElementValue);
                        flow.setMaxElementsWrap(value);
                    } catch(NumberFormatException e) {

                    }
                    break;
                case "padding":
                    CLElement paddingObject = element.get(param);
                    int paddingLeft = 0;
                    int paddingTop = 0;
                    int paddingRight = 0;
                    int paddingBottom = 0;
                    if (paddingObject instanceof CLArray && ((CLArray) paddingObject).size() > 1) {
                        paddingLeft = ((CLArray) paddingObject).getInt(0);
                        paddingRight = paddingLeft;
                        paddingTop = ((CLArray) paddingObject).getInt(1);
                        paddingBottom = paddingTop;
                        if (((CLArray) paddingObject).size() > 2) {
                            paddingRight = ((CLArray) paddingObject).getInt(2);
                            try {
                                paddingBottom = ((CLArray) paddingObject).getInt(3);
                            } catch (ArrayIndexOutOfBoundsException e) {
                                paddingBottom = 0;
                            }

                        }
                    } else {
                        paddingLeft = paddingObject.getInt();
                        paddingTop = paddingLeft;
                        paddingRight = paddingLeft;
                        paddingBottom = paddingLeft;
                    }
                    flow.setPaddingLeft(paddingLeft);
                    flow.setPaddingTop(paddingTop);
                    flow.setPaddingRight(paddingRight);
                    flow.setPaddingBottom(paddingBottom);
                    break;
                case "vAlign":
                    String vAlignValue = element.get(param).content();
                    switch (vAlignValue) {
                        case "top":
                            flow.setVerticalAlign(Flow.VERTICAL_ALIGN_TOP);
                            break;
                        case "bottom":
                            flow.setVerticalAlign(Flow.VERTICAL_ALIGN_BOTTOM);
                            break;
                        case "baseline":
                            flow.setVerticalAlign(Flow.VERTICAL_ALIGN_BASELINE);
                            break;
                        default:
                            flow.setVerticalAlign(Flow.VERTICAL_ALIGN_CENTER);
                            break;
                    }
                    break;
                case "hAlign":
                    String hAlignValue = element.get(param).content();
                    switch (hAlignValue) {
                        case "start":
                            flow.setHorizontalAlign(Flow.HORIZONTAL_ALIGN_START);
                            break;
                        case "end":
                            flow.setHorizontalAlign(Flow.HORIZONTAL_ALIGN_END);
                            break;
                        default:
                            flow.setHorizontalAlign(Flow.HORIZONTAL_ALIGN_CENTER);
                            break;
                    }
                    break;
                case "vFlowBias":
                    CLElement vBiasObject = element.get(param);
                    Float vBiasValue = 0.5f;
                    Float vFirstBiasValue = 0.5f;
                    Float vLastBiasValue = 0.5f;
                    if (vBiasObject instanceof CLArray && ((CLArray) vBiasObject).size() > 1) {
                        vFirstBiasValue = ((CLArray) vBiasObject).getFloat(0);
                        vBiasValue = ((CLArray) vBiasObject).getFloat(1);
                        if (((CLArray) vBiasObject).size() > 2) {
                            vLastBiasValue = ((CLArray) vBiasObject).getFloat(2);
                        }
                    } else {
                        vBiasValue = vBiasObject.getFloat();
                    }
                    try {
                        flow.verticalBias(vBiasValue);
                        if (vFirstBiasValue != 0.5f) {
                            flow.setFirstVerticalBias(vFirstBiasValue);
                        }
                        if (vLastBiasValue != 0.5f) {
                            flow.setLastVerticalBias(vLastBiasValue);
                        }
                    } catch(NumberFormatException e) {

                    }
                    break;
                case "hFlowBias":
                    CLElement hBiasObject = element.get(param);
                    Float hBiasValue = 0.5f;
                    Float hFirstBiasValue = 0.5f;
                    Float hLastBiasValue = 0.5f;
                    if (hBiasObject instanceof CLArray && ((CLArray) hBiasObject).size() > 1) {
                        hFirstBiasValue = ((CLArray) hBiasObject).getFloat(0);
                        hBiasValue = ((CLArray) hBiasObject).getFloat(1);
                        if (((CLArray) hBiasObject).size() > 2) {
                            hLastBiasValue = ((CLArray) hBiasObject).getFloat(2);
                        }
                    } else {
                        hBiasValue = hBiasObject.getFloat();
                    }
                    try {
                        flow.horizontalBias(hBiasValue);
                        if (hFirstBiasValue != 0.5f) {
                            flow.setFirstHorizontalBias(hFirstBiasValue);
                        }
                        if (hLastBiasValue != 0.5f) {
                            flow.setLastHorizontalBias(hLastBiasValue);
                        }
                    } catch(NumberFormatException e) {

                    }
                    break;
                case "vStyle":
                    CLElement vStyleObject = element.get(param);
                    String vStyleValueStr = "";
                    String vFirstStyleValueStr = "";
                    String vLastStyleValueStr = "";
                    if (vStyleObject instanceof CLArray && ((CLArray) vStyleObject).size() > 1) {
                        vFirstStyleValueStr = ((CLArray) vStyleObject).getString(0);
                        vStyleValueStr = ((CLArray) vStyleObject).getString(1);
                        if (((CLArray) vStyleObject).size() > 2) {
                            vLastStyleValueStr = ((CLArray) vStyleObject).getString(2);
                        }
                    } else {
                        vStyleValueStr = vStyleObject.content();
                    }

                    if (!vStyleValueStr.equals("")) {
                        flow.setVerticalStyle(State.Chain.getValueByString(vStyleValueStr));
                    }
                    if (!vFirstStyleValueStr.equals("")) {
                        flow.setFirstVerticalStyle(
                                State.Chain.getValueByString(vFirstStyleValueStr));
                    }
                    if (!vLastStyleValueStr.equals("")) {
                        flow.setLastVerticalStyle(State.Chain.getValueByString(vLastStyleValueStr));
                    }
                    break;
                case "hStyle":
                    CLElement hStyleObject = element.get(param);
                    String hStyleValueStr = "";
                    String hFirstStyleValueStr = "";
                    String hLastStyleValueStr = "";
                    if (hStyleObject instanceof CLArray && ((CLArray) hStyleObject).size() > 1) {
                        hFirstStyleValueStr = ((CLArray) hStyleObject).getString(0);
                        hStyleValueStr = ((CLArray) hStyleObject).getString(1);
                        if (((CLArray) hStyleObject).size() > 2) {
                            hLastStyleValueStr = ((CLArray) hStyleObject).getString(2);
                        }
                    } else {
                        hStyleValueStr = hStyleObject.content();
                    }

                    if (!hStyleValueStr.equals("")) {
                        flow.setHorizontalStyle(State.Chain.getValueByString(hStyleValueStr));
                    }
                    if (!hFirstStyleValueStr.equals("")) {
                        flow.setFirstHorizontalStyle(
                                State.Chain.getValueByString(hFirstStyleValueStr));
                    }
                    if (!hLastStyleValueStr.equals("")) {
                        flow.setLastHorizontalStyle(
                                State.Chain.getValueByString(hLastStyleValueStr));
                    }
                    break;
                default:
                    // Get the underlying reference for the flow, apply the constraints
                    // attributes to it
                    ConstraintReference reference = state.constraints(flowName);
                    applyAttribute(state, layoutVariables, reference, element, param);
            }
        }
    }

    static void parseGuideline(int orientation,
            State state, CLArray helper) throws CLParsingException {
        CLElement params = helper.get(1);
        if (!(params instanceof CLObject)) {
            return;
        }
        String guidelineId = ((CLObject) params).getStringOrNull("id");
        if (guidelineId == null) return;
        parseGuidelineParams(orientation, state, guidelineId, (CLObject) params);
    }

    static void parseGuidelineParams(
            int orientation,
            State state,
            String guidelineId,
            CLObject params
    ) throws CLParsingException {
        ArrayList<String> constraints = params.names();
        if (constraints == null) return;
        ConstraintReference reference = state.constraints(guidelineId);

        if (orientation == ConstraintWidget.HORIZONTAL) {
            state.horizontalGuideline(guidelineId);
        } else {
            state.verticalGuideline(guidelineId);
        }

        // Ignore LTR for Horizontal guidelines, since `start` & `end` represent the distance
        // from `top` and `bottom` respectively
        boolean isLtr = state.isLtr() || orientation == ConstraintWidget.HORIZONTAL;

        GuidelineReference guidelineReference = (GuidelineReference) reference.getFacade();

        // Whether the guideline is based on percentage or distance
        boolean isPercent = false;

        // Percent or distance value of the guideline
        float value = 0f;

        // Indicates if the value is considered from the "start" position,
        // meaning "left" anchor for vertical guidelines and "top" anchor for
        // horizontal guidelines
        boolean fromStart = true;
        for (String constraintName : constraints) {
            switch (constraintName) {
                // left and right are here just to support LTR independent vertical guidelines
                case "left":
                    value = toPix(state, params.getFloat(constraintName));
                    fromStart = true;
                    break;
                case "right":
                    value = toPix(state, params.getFloat(constraintName));
                    fromStart = false;
                    break;
                case "start":
                    value = toPix(state, params.getFloat(constraintName));
                    fromStart = isLtr;
                    break;
                case "end":
                    value = toPix(state, params.getFloat(constraintName));
                    fromStart = !isLtr;
                    break;
                case "percent":
                    isPercent = true;
                    CLArray percentParams = params.getArrayOrNull(constraintName);
                    if (percentParams == null) {
                        fromStart = true;
                        value = params.getFloat(constraintName);
                    } else if (percentParams.size() > 1) {
                        String origin = percentParams.getString(0);
                        value = percentParams.getFloat(1);
                        switch (origin) {
                            case "left":
                                fromStart = true;
                                break;
                            case "right":
                                fromStart = false;
                                break;
                            case "start":
                                fromStart = isLtr;
                                break;
                            case "end":
                                fromStart = !isLtr;
                                break;
                        }
                    }
                    break;
            }
        }

        // Populate the guideline based on the resolved properties
        if (isPercent) {
            if (fromStart) {
                guidelineReference.percent(value);
            } else {
                guidelineReference.percent(1f - value);
            }
        } else {
            if (fromStart) {
                guidelineReference.start(value);
            } else {
                guidelineReference.end(value);
            }
        }
    }

    static void parseBarrier(
            State state,
            String elementName, CLObject element
    ) throws CLParsingException {
        boolean isLtr = state.isLtr();
        BarrierReference reference = state.barrier(elementName, State.Direction.END);
        ArrayList<String> constraints = element.names();
        if (constraints == null) {
            return;
        }
        for (String constraintName : constraints) {
            switch (constraintName) {
                case "direction": {
                    switch (element.getString(constraintName)) {
                        case "start":
                            if (isLtr) {
                                reference.setBarrierDirection(State.Direction.LEFT);
                            } else {
                                reference.setBarrierDirection(State.Direction.RIGHT);
                            }
                            break;
                        case "end":
                            if (isLtr) {
                                reference.setBarrierDirection(State.Direction.RIGHT);
                            } else {
                                reference.setBarrierDirection(State.Direction.LEFT);
                            }
                            break;
                        case "left":
                            reference.setBarrierDirection(State.Direction.LEFT);
                            break;
                        case "right":
                            reference.setBarrierDirection(State.Direction.RIGHT);
                            break;
                        case "top":
                            reference.setBarrierDirection(State.Direction.TOP);
                            break;
                        case "bottom":
                            reference.setBarrierDirection(State.Direction.BOTTOM);
                            break;
                    }
                }
                break;
                case "margin":
                    float margin = element.getFloatOrNaN(constraintName);
                    if (!Float.isNaN(margin)) {
                        reference.margin(toPix(state, margin));
                    }
                    break;
                case "contains":
                    CLArray list = element.getArrayOrNull(constraintName);
                    if (list != null) {
                        for (int j = 0; j < list.size(); j++) {

                            String elementNameReference = list.get(j).content();
                            ConstraintReference elementReference =
                                    state.constraints(elementNameReference);
                            if (PARSER_DEBUG) {
                                System.out.println(
                                        "Add REFERENCE "
                                                + "($elementNameReference = $elementReference) "
                                                + "TO BARRIER "
                                );
                            }
                            reference.add(elementReference);
                        }
                    }
                    break;
            }
        }
    }

    static void parseWidget(
            State state,
            LayoutVariables layoutVariables,
            String elementName,
            CLObject element
    ) throws CLParsingException {
        ConstraintReference reference = state.constraints(elementName);
        parseWidget(state, layoutVariables, reference, element);
    }

    /**
     * Set/apply attribute to a widget/helper reference
     *
     * @param state Constraint State
     * @param layoutVariables layout variables
     * @param reference widget/helper reference
     * @param element the parsed CLObject
     * @param attributeName Name of the attribute to be set/applied
     * @throws CLParsingException
     */
    static void applyAttribute(
            State state,
            LayoutVariables layoutVariables,
            ConstraintReference reference,
            CLObject element,
            String attributeName) throws CLParsingException {

        float value;
        switch (attributeName) {
            case "width":
                reference.setWidth(parseDimension(element,
                        attributeName, state, state.getDpToPixel()));
                break;
            case "height":
                reference.setHeight(parseDimension(element,
                        attributeName, state, state.getDpToPixel()));
                break;
            case "center":
                String target = element.getString(attributeName);

                ConstraintReference targetReference;
                if (target.equals("parent")) {
                    targetReference = state.constraints(State.PARENT);
                } else {
                    targetReference = state.constraints(target);
                }
                reference.startToStart(targetReference);
                reference.endToEnd(targetReference);
                reference.topToTop(targetReference);
                reference.bottomToBottom(targetReference);
                break;
            case "centerHorizontally":
                target = element.getString(attributeName);
                targetReference = target.equals("parent")
                        ? state.constraints(State.PARENT) : state.constraints(target);

                reference.startToStart(targetReference);
                reference.endToEnd(targetReference);
                break;
            case "centerVertically":
                target = element.getString(attributeName);
                targetReference = target.equals("parent")
                        ? state.constraints(State.PARENT) : state.constraints(target);

                reference.topToTop(targetReference);
                reference.bottomToBottom(targetReference);
                break;
            case "alpha":
                value = layoutVariables.get(element.get(attributeName));
                reference.alpha(value);
                break;
            case "scaleX":
                value = layoutVariables.get(element.get(attributeName));
                reference.scaleX(value);
                break;
            case "scaleY":
                value = layoutVariables.get(element.get(attributeName));
                reference.scaleY(value);
                break;
            case "translationX":
                value = layoutVariables.get(element.get(attributeName));
                reference.translationX(value);
                break;
            case "translationY":
                value = layoutVariables.get(element.get(attributeName));
                reference.translationY(value);
                break;
            case "translationZ":
                value = layoutVariables.get(element.get(attributeName));
                reference.translationZ(value);
                break;
            case "pivotX":
                value = layoutVariables.get(element.get(attributeName));
                reference.pivotX(value);
                break;
            case "pivotY":
                value = layoutVariables.get(element.get(attributeName));
                reference.pivotY(value);
                break;
            case "rotationX":
                value = layoutVariables.get(element.get(attributeName));
                reference.rotationX(value);
                break;
            case "rotationY":
                value = layoutVariables.get(element.get(attributeName));
                reference.rotationY(value);
                break;
            case "rotationZ":
                value = layoutVariables.get(element.get(attributeName));
                reference.rotationZ(value);
                break;
            case "visibility":
                switch (element.getString(attributeName)) {
                    case "visible":
                        reference.visibility(ConstraintWidget.VISIBLE);
                        break;
                    case "invisible":
                        reference.visibility(ConstraintWidget.INVISIBLE);
                        break;
                    case "gone":
                        reference.visibility(ConstraintWidget.GONE);
                        break;
                }
                break;
            case "vBias":
                value = layoutVariables.get(element.get(attributeName));
                reference.verticalBias(value);
                break;
            case "hRtlBias":
                // TODO: This is a temporary solution to support bias with start/end constraints,
                //  where the bias needs to be reversed in RTL, we probably want a better or more
                //  intuitive way to do this
                value = layoutVariables.get(element.get(attributeName));
                if (!state.isLtr()) {
                    value = 1f - value;
                }
                reference.horizontalBias(value);
                break;
            case "hBias":
                value = layoutVariables.get(element.get(attributeName));
                reference.horizontalBias(value);
                break;
            case "vWeight":
                value = layoutVariables.get(element.get(attributeName));
                reference.setVerticalChainWeight(value);
                break;
            case "hWeight":
                value = layoutVariables.get(element.get(attributeName));
                reference.setHorizontalChainWeight(value);
                break;
            case "custom":
                parseCustomProperties(element, reference, attributeName);
                break;
            case "motion":
                parseMotionProperties(element.get(attributeName), reference);
                break;
            default:
                parseConstraint(state, layoutVariables, element, reference, attributeName);
        }
    }

    static void parseWidget(
            State state,
            LayoutVariables layoutVariables,
            ConstraintReference reference,
            CLObject element
    ) throws CLParsingException {
        if (reference.getWidth() == null) {
            // Default to Wrap when the Dimension has not been assigned
            reference.setWidth(Dimension.createWrap());
        }
        if (reference.getHeight() == null) {
            // Default to Wrap when the Dimension has not been assigned
            reference.setHeight(Dimension.createWrap());
        }
        ArrayList<String> constraints = element.names();
        if (constraints == null) {
            return;
        }
        for (String constraintName : constraints) {
            applyAttribute(state, layoutVariables, reference, element, constraintName);
        }
    }

    static void parseCustomProperties(
            CLObject element,
            ConstraintReference reference,
            String constraintName
    ) throws CLParsingException {
        CLObject json = element.getObjectOrNull(constraintName);
        if (json == null) {
            return;
        }
        ArrayList<String> properties = json.names();
        if (properties == null) {
            return;
        }
        for (String property : properties) {
            CLElement value = json.get(property);
            if (value instanceof CLNumber) {
                reference.addCustomFloat(property, value.getFloat());
            } else if (value instanceof CLString) {
                long it = parseColorString(value.content());
                if (it != -1) {
                    reference.addCustomColor(property, (int) it);
                }
            }
        }
    }

    private static int indexOf(String val, String... types) {
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(val)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * parse the motion section of a constraint
     * <pre>
     * csetName: {
     *   idToConstrain : {
     *       motion: {
     *          pathArc : 'startVertical'
     *          relativeTo: 'id'
     *          easing: 'curve'
     *          stagger: '2'
     *          quantize: steps or [steps, 'interpolator' phase ]
     *       }
     *   }
     * }
     * </pre>
     */
    private static void parseMotionProperties(
            CLElement element,
            ConstraintReference reference
    ) throws CLParsingException {
        if (!(element instanceof CLObject)) {
            return;
        }
        CLObject obj = (CLObject) element;
        TypedBundle bundle = new TypedBundle();
        ArrayList<String> constraints = obj.names();
        if (constraints == null) {
            return;
        }
        for (String constraintName : constraints) {

            switch (constraintName) {
                case "pathArc":
                    String val = obj.getString(constraintName);
                    int ord = indexOf(val, "none", "startVertical", "startHorizontal", "flip");
                    if (ord == -1) {
                        System.err.println(obj.getLine() + " pathArc = '" + val + "'");
                        break;
                    }
                    bundle.add(TypedValues.MotionType.TYPE_PATHMOTION_ARC, ord);
                    break;
                case "relativeTo":
                    bundle.add(TypedValues.MotionType.TYPE_ANIMATE_RELATIVE_TO,
                            obj.getString(constraintName));
                    break;
                case "easing":
                    bundle.add(TypedValues.MotionType.TYPE_EASING, obj.getString(constraintName));
                    break;
                case "stagger":
                    bundle.add(TypedValues.MotionType.TYPE_STAGGER, obj.getFloat(constraintName));
                    break;
                case "quantize":
                    CLElement quant = obj.get(constraintName);
                    if (quant instanceof CLArray) {
                        CLArray array = (CLArray) quant;
                        int len = array.size();
                        if (len > 0) {
                            bundle.add(TYPE_QUANTIZE_MOTIONSTEPS, array.getInt(0));
                            if (len > 1) {
                                bundle.add(TYPE_QUANTIZE_INTERPOLATOR_TYPE, array.getString(1));
                                if (len > 2) {
                                    bundle.add(TYPE_QUANTIZE_MOTION_PHASE, array.getFloat(2));
                                }
                            }
                        }
                    } else {
                        bundle.add(TYPE_QUANTIZE_MOTIONSTEPS, obj.getInt(constraintName));
                    }
                    break;
            }
        }
        reference.mMotionProperties = bundle;
    }

    static void parseConstraint(
            State state,
            LayoutVariables layoutVariables,
            CLObject element,
            ConstraintReference reference,
            String constraintName
    ) throws CLParsingException {
        boolean isLtr = state.isLtr();
        CLArray constraint = element.getArrayOrNull(constraintName);
        if (constraint != null && constraint.size() > 1) {
            // params: target, anchor
            String target = constraint.getString(0);
            String anchor = constraint.getStringOrNull(1);
            float margin = 0f;
            float marginGone = 0f;
            if (constraint.size() > 2) {
                // params: target, anchor, margin
                CLElement arg2 = constraint.getOrNull(2);
                margin = layoutVariables.get(arg2);
                margin = state.convertDimension(toPix(state, margin));
            }
            if (constraint.size() > 3) {
                // params: target, anchor, margin, marginGone
                CLElement arg2 = constraint.getOrNull(3);
                marginGone = layoutVariables.get(arg2);
                marginGone = state.convertDimension(toPix(state, marginGone));
            }

            ConstraintReference targetReference = target.equals("parent")
                    ? state.constraints(State.PARENT) :
                    state.constraints(target);

            // For simplicity, we'll apply horizontal constraints separately
            boolean isHorizontalConstraint = false;
            boolean isHorOriginLeft = true;
            boolean isHorTargetLeft = true;

            switch (constraintName) {
                case "circular":
                    float angle = layoutVariables.get(constraint.get(1));
                    float distance = 0f;
                    if (constraint.size() > 2) {
                        CLElement distanceArg = constraint.getOrNull(2);
                        distance = layoutVariables.get(distanceArg);
                        distance = state.convertDimension(toPix(state, distance));
                    }
                    reference.circularConstraint(targetReference, angle, distance);
                    break;
                case "top":
                    switch (anchor) {
                        case "top":
                            reference.topToTop(targetReference);
                            break;
                        case "bottom":
                            reference.topToBottom(targetReference);
                    }
                    break;
                case "bottom":
                    switch (anchor) {
                        case "top":
                            reference.bottomToTop(targetReference);
                            break;
                        case "bottom":
                            reference.bottomToBottom(targetReference);
                    }
                    break;
                case "baseline":
                    switch (anchor) {
                        case "baseline":
                            state.baselineNeededFor(reference.getKey());
                            state.baselineNeededFor(targetReference.getKey());
                            reference.baselineToBaseline(targetReference);
                            break;
                        case "top":
                            state.baselineNeededFor(reference.getKey());
                            state.baselineNeededFor(targetReference.getKey());
                            reference.baselineToTop(targetReference);
                            break;
                        case "bottom":
                            state.baselineNeededFor(reference.getKey());
                            state.baselineNeededFor(targetReference.getKey());
                            reference.baselineToBottom(targetReference);
                            break;
                    }
                    break;
                case "left":
                    isHorizontalConstraint = true;
                    isHorOriginLeft = true;
                    break;
                case "right":
                    isHorizontalConstraint = true;
                    isHorOriginLeft = false;
                    break;
                case "start":
                    isHorizontalConstraint = true;
                    isHorOriginLeft = isLtr;
                    break;
                case "end":
                    isHorizontalConstraint = true;
                    isHorOriginLeft = !isLtr;
                    break;
            }

            if (isHorizontalConstraint) {
                // Resolve horizontal target anchor
                switch (anchor) {
                    case "left":
                        isHorTargetLeft = true;
                        break;
                    case "right":
                        isHorTargetLeft = false;
                        break;
                    case "start":
                        isHorTargetLeft = isLtr;
                        break;
                    case "end":
                        isHorTargetLeft = !isLtr;
                        break;
                }

                // Resolved anchors, apply corresponding constraint
                if (isHorOriginLeft) {
                    if (isHorTargetLeft) {
                        reference.leftToLeft(targetReference);
                    } else {
                        reference.leftToRight(targetReference);
                    }
                } else {
                    if (isHorTargetLeft) {
                        reference.rightToLeft(targetReference);
                    } else {
                        reference.rightToRight(targetReference);
                    }
                }
            }

            reference.margin(margin).marginGone(marginGone);
        } else {
            String target = element.getStringOrNull(constraintName);
            if (target != null) {
                ConstraintReference targetReference = target.equals("parent")
                        ? state.constraints(State.PARENT) :
                        state.constraints(target);

                switch (constraintName) {
                    case "start":
                        if (isLtr) {
                            reference.leftToLeft(targetReference);
                        } else {
                            reference.rightToRight(targetReference);
                        }
                        break;
                    case "end":
                        if (isLtr) {
                            reference.rightToRight(targetReference);
                        } else {
                            reference.leftToLeft(targetReference);
                        }
                        break;
                    case "top":
                        reference.topToTop(targetReference);
                        break;
                    case "bottom":
                        reference.bottomToBottom(targetReference);
                        break;
                    case "baseline":
                        state.baselineNeededFor(reference.getKey());
                        state.baselineNeededFor(targetReference.getKey());
                        reference.baselineToBaseline(targetReference);
                        break;
                }
            }
        }
    }

    static Dimension parseDimensionMode(String dimensionString) {
        Dimension dimension = Dimension.createFixed(0);
        switch (dimensionString) {
            case "wrap":
                dimension = Dimension.createWrap();
                break;
            case "preferWrap":
                dimension = Dimension.createSuggested(Dimension.WRAP_DIMENSION);
                break;
            case "spread":
                dimension = Dimension.createSuggested(Dimension.SPREAD_DIMENSION);
                break;
            case "parent":
                dimension = Dimension.createParent();
                break;
            default: {
                if (dimensionString.endsWith("%")) {
                    // parent percent
                    String percentString =
                            dimensionString.substring(0, dimensionString.indexOf('%'));
                    float percentValue = Float.parseFloat(percentString) / 100f;
                    dimension = Dimension.createPercent(0, percentValue).suggested(0);
                } else if (dimensionString.contains(":")) {
                    dimension = Dimension.createRatio(dimensionString)
                            .suggested(Dimension.SPREAD_DIMENSION);
                }
            }
        }
        return dimension;
    }

    static Dimension parseDimension(CLObject element,
            String constraintName,
            State state,
            CorePixelDp dpToPixels) throws CLParsingException {
        CLElement dimensionElement = element.get(constraintName);
        Dimension dimension = Dimension.createFixed(0);
        if (dimensionElement instanceof CLString) {
            dimension = parseDimensionMode(dimensionElement.content());
        } else if (dimensionElement instanceof CLNumber) {
            dimension = Dimension.createFixed(
                    state.convertDimension(dpToPixels.toPixels(element.getFloat(constraintName))));

        } else if (dimensionElement instanceof CLObject) {
            CLObject obj = (CLObject) dimensionElement;
            String mode = obj.getStringOrNull("value");
            if (mode != null) {
                dimension = parseDimensionMode(mode);
            }

            CLElement minEl = obj.getOrNull("min");
            if (minEl != null) {
                if (minEl instanceof CLNumber) {
                    float min = ((CLNumber) minEl).getFloat();
                    dimension.min(state.convertDimension(dpToPixels.toPixels(min)));
                } else if (minEl instanceof CLString) {
                    dimension.min(Dimension.WRAP_DIMENSION);
                }
            }
            CLElement maxEl = obj.getOrNull("max");
            if (maxEl != null) {
                if (maxEl instanceof CLNumber) {
                    float max = ((CLNumber) maxEl).getFloat();
                    dimension.max(state.convertDimension(dpToPixels.toPixels(max)));
                } else if (maxEl instanceof CLString) {
                    dimension.max(Dimension.WRAP_DIMENSION);
                }
            }
        }
        return dimension;
    }

    /**
     * parse a color string
     *
     * @return -1 if it cannot parse unsigned long
     */
    static long parseColorString(String value) {
        String str = value;
        if (str.startsWith("#")) {
            str = str.substring(1);
            if (str.length() == 6) {
                str = "FF" + str;
            }
            return Long.parseLong(str, 16);
        } else {
            return -1L;
        }
    }

    static String lookForType(CLObject element) throws CLParsingException {
        ArrayList<String> constraints = element.names();
        for (String constraintName : constraints) {
            if (constraintName.equals("type")) {
                return element.getString("type");
            }
        }
        return null;
    }
}
