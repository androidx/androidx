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

import static androidx.constraintlayout.core.state.ConstraintSetParser.parseColorString;

import androidx.constraintlayout.core.motion.CustomVariable;
import androidx.constraintlayout.core.motion.utils.TypedBundle;
import androidx.constraintlayout.core.motion.utils.TypedValues;
import androidx.constraintlayout.core.motion.utils.Utils;
import androidx.constraintlayout.core.parser.CLArray;
import androidx.constraintlayout.core.parser.CLContainer;
import androidx.constraintlayout.core.parser.CLElement;
import androidx.constraintlayout.core.parser.CLKey;
import androidx.constraintlayout.core.parser.CLNumber;
import androidx.constraintlayout.core.parser.CLObject;
import androidx.constraintlayout.core.parser.CLParsingException;

import java.util.ArrayList;

/**
 * Contains code for Parsing Transitions
 */
public class TransitionParser {
    /**
     * Parse a JSON string of a Transition and insert it into the Transition object
     *
     * @param json       Transition Object to parse.
     * @param transition Transition Object to write transition to
     */
    public static void parse(CLObject json, Transition transition, CorePixelDp dpToPixel)
            throws CLParsingException {
        String pathMotionArc = json.getStringOrNull("pathMotionArc");
        TypedBundle bundle = new TypedBundle();
        transition.mToPixel = dpToPixel;
        boolean setBundle = false;
        if (pathMotionArc != null) {
            setBundle = true;
            switch (pathMotionArc) {
                // TODO use map
                case "none":
                    bundle.add(TypedValues.PositionType.TYPE_PATH_MOTION_ARC, 0);
                    break;
                case "startVertical":
                    bundle.add(TypedValues.PositionType.TYPE_PATH_MOTION_ARC, 1);
                    break;
                case "startHorizontal":
                    bundle.add(TypedValues.PositionType.TYPE_PATH_MOTION_ARC, 2);
                    break;
                case "flip":
                    bundle.add(TypedValues.PositionType.TYPE_PATH_MOTION_ARC, 3);
            }

        }
        // TODO: Add duration
        String interpolator = json.getStringOrNull("interpolator");
        if (interpolator != null) {
            setBundle = true;
            bundle.add(TypedValues.TransitionType.TYPE_INTERPOLATOR, interpolator);
        }

        float staggered = json.getFloatOrNaN("staggered");
        if (!Float.isNaN(staggered)) {
            setBundle = true;
            bundle.add(TypedValues.TransitionType.TYPE_STAGGERED, staggered);
        }
        if (setBundle) {
            transition.setTransitionProperties(bundle);
        }

        CLContainer onSwipe = json.getObjectOrNull("onSwipe");

        if (onSwipe != null) {
            parseOnSwipe(onSwipe, transition);
        }
        parseKeyFrames(json, transition);
    }

    private static void parseOnSwipe(CLContainer onSwipe, Transition transition) {
        String anchor = onSwipe.getStringOrNull("anchor");
        int side = map(onSwipe.getStringOrNull("side"), Transition.OnSwipe.SIDES);
        int direction = map(onSwipe.getStringOrNull("direction"),
                Transition.OnSwipe.DIRECTIONS);
        float scale = onSwipe.getFloatOrNaN("scale");
        float threshold = onSwipe.getFloatOrNaN("threshold");
        float maxVelocity = onSwipe.getFloatOrNaN("maxVelocity");
        float maxAccel = onSwipe.getFloatOrNaN("maxAccel");
        String limitBounds = onSwipe.getStringOrNull("limitBounds");
        int autoCompleteMode = map(onSwipe.getStringOrNull("mode"), Transition.OnSwipe.MODE);
        int touchUp = map(onSwipe.getStringOrNull("touchUp"), Transition.OnSwipe.TOUCH_UP);
        float springMass = onSwipe.getFloatOrNaN("springMass");
        float springStiffness = onSwipe.getFloatOrNaN("springStiffness");
        float springDamping = onSwipe.getFloatOrNaN("springDamping");
        float stopThreshold = onSwipe.getFloatOrNaN("stopThreshold");
        int springBoundary = map(onSwipe.getStringOrNull("springBoundary"),
                Transition.OnSwipe.BOUNDARY);
        String around = onSwipe.getStringOrNull("around");

        Transition.OnSwipe swipe = transition.createOnSwipe();
        swipe.setAnchorId(anchor);
        swipe.setAnchorSide(side);
        swipe.setDragDirection(direction);
        swipe.setDragScale(scale);
        swipe.setDragThreshold(threshold);
        swipe.setMaxVelocity(maxVelocity);
        swipe.setMaxAcceleration(maxAccel);
        swipe.setLimitBoundsTo(limitBounds);
        swipe.setAutoCompleteMode(autoCompleteMode);
        swipe.setOnTouchUp(touchUp);
        swipe.setSpringMass(springMass);
        swipe.setSpringStiffness(springStiffness);
        swipe.setSpringDamping(springDamping);
        swipe.setSpringStopThreshold(stopThreshold);
        swipe.setSpringBoundary(springBoundary);
        swipe.setRotationCenterId(around);
    }


    private static int map(String val, String... types) {
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(val)) {
                return i;
            }
        }
        return 0;
    }

    private static void map(TypedBundle bundle, int type, String val, String... types) {
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(val)) {
                bundle.add(type, i);
            }
        }
    }

    /**
     * Parses {@code KeyFrames} attributes from the {@link CLObject} into {@link  Transition}.
     *
     * @param transitionCLObject the CLObject for the root transition json
     * @param transition         core object that holds the state of the Transition
     */
    public static void parseKeyFrames(CLObject transitionCLObject, Transition transition)
            throws CLParsingException {
        CLContainer keyframes = transitionCLObject.getObjectOrNull("KeyFrames");
        if (keyframes == null) return;
        CLArray keyPositions = keyframes.getArrayOrNull("KeyPositions");
        if (keyPositions != null) {
            for (int i = 0; i < keyPositions.size(); i++) {
                CLElement keyPosition = keyPositions.get(i);
                if (keyPosition instanceof CLObject) {
                    parseKeyPosition((CLObject) keyPosition, transition);
                }
            }
        }
        CLArray keyAttributes = keyframes.getArrayOrNull("KeyAttributes");
        if (keyAttributes != null) {
            for (int i = 0; i < keyAttributes.size(); i++) {
                CLElement keyAttribute = keyAttributes.get(i);
                if (keyAttribute instanceof CLObject) {
                    parseKeyAttribute((CLObject) keyAttribute, transition);
                }
            }
        }
        CLArray keyCycles = keyframes.getArrayOrNull("KeyCycles");
        if (keyCycles != null) {
            for (int i = 0; i < keyCycles.size(); i++) {
                CLElement keyCycle = keyCycles.get(i);
                if (keyCycle instanceof CLObject) {
                    parseKeyCycle((CLObject) keyCycle, transition);
                }
            }
        }
    }


    private static void parseKeyPosition(CLObject keyPosition,
            Transition transition) throws CLParsingException {
        TypedBundle bundle = new TypedBundle();
        CLArray targets = keyPosition.getArray("target");
        CLArray frames = keyPosition.getArray("frames");
        CLArray percentX = keyPosition.getArrayOrNull("percentX");
        CLArray percentY = keyPosition.getArrayOrNull("percentY");
        CLArray percentWidth = keyPosition.getArrayOrNull("percentWidth");
        CLArray percentHeight = keyPosition.getArrayOrNull("percentHeight");
        String pathMotionArc = keyPosition.getStringOrNull("pathMotionArc");
        String transitionEasing = keyPosition.getStringOrNull("transitionEasing");
        String curveFit = keyPosition.getStringOrNull("curveFit");
        String type = keyPosition.getStringOrNull("type");
        if (type == null) {
            type = "parentRelative";
        }
        if (percentX != null && frames.size() != percentX.size()) {
            return;
        }
        if (percentY != null && frames.size() != percentY.size()) {
            return;
        }
        for (int i = 0; i < targets.size(); i++) {
            String target = targets.getString(i);
            int pos_type = map(type, "deltaRelative", "pathRelative", "parentRelative");
            bundle.clear();
            bundle.add(TypedValues.PositionType.TYPE_POSITION_TYPE, pos_type);
            if (curveFit != null) {
                map(bundle, TypedValues.PositionType.TYPE_CURVE_FIT, curveFit,
                        "spline", "linear");
            }
            bundle.addIfNotNull(TypedValues.PositionType.TYPE_TRANSITION_EASING, transitionEasing);

            if (pathMotionArc != null) {
                map(bundle, TypedValues.PositionType.TYPE_PATH_MOTION_ARC, pathMotionArc,
                        "none", "startVertical", "startHorizontal", "flip");
            }

            for (int j = 0; j < frames.size(); j++) {
                int frame = frames.getInt(j);
                bundle.add(TypedValues.TYPE_FRAME_POSITION, frame);
                set(bundle, TypedValues.PositionType.TYPE_PERCENT_X, percentX, j);
                set(bundle, TypedValues.PositionType.TYPE_PERCENT_Y, percentY, j);
                set(bundle, TypedValues.PositionType.TYPE_PERCENT_WIDTH, percentWidth, j);
                set(bundle, TypedValues.PositionType.TYPE_PERCENT_HEIGHT, percentHeight, j);

                transition.addKeyPosition(target, bundle);
            }
        }
    }

    private static void set(TypedBundle bundle, int type,
            CLArray array, int index) throws CLParsingException {
        if (array != null) {
            bundle.add(type, array.getFloat(index));
        }
    }

    private static void parseKeyAttribute(CLObject keyAttribute,
            Transition transition) throws CLParsingException {
        CLArray targets = keyAttribute.getArrayOrNull("target");
        if (targets == null) {
            return;
        }
        CLArray frames = keyAttribute.getArrayOrNull("frames");
        if (frames == null) {
            return;
        }
        String transitionEasing = keyAttribute.getStringOrNull("transitionEasing");
        // These present an ordered list of attributes that might be used in a keyCycle
        String[] attrNames = {
                TypedValues.AttributesType.S_SCALE_X,
                TypedValues.AttributesType.S_SCALE_Y,
                TypedValues.AttributesType.S_TRANSLATION_X,
                TypedValues.AttributesType.S_TRANSLATION_Y,
                TypedValues.AttributesType.S_TRANSLATION_Z,
                TypedValues.AttributesType.S_ROTATION_X,
                TypedValues.AttributesType.S_ROTATION_Y,
                TypedValues.AttributesType.S_ROTATION_Z,
                TypedValues.AttributesType.S_ALPHA
        };
        int[] attrIds = {
                TypedValues.AttributesType.TYPE_SCALE_X,
                TypedValues.AttributesType.TYPE_SCALE_Y,
                TypedValues.AttributesType.TYPE_TRANSLATION_X,
                TypedValues.AttributesType.TYPE_TRANSLATION_Y,
                TypedValues.AttributesType.TYPE_TRANSLATION_Z,
                TypedValues.AttributesType.TYPE_ROTATION_X,
                TypedValues.AttributesType.TYPE_ROTATION_Y,
                TypedValues.AttributesType.TYPE_ROTATION_Z,
                TypedValues.AttributesType.TYPE_ALPHA
        };
        // if true scale the values from pixels to dp
        boolean[] scaleTypes = {
                false,
                false,
                true,
                true,
                true,
                false,
                false,
                false,
                false,
        };
        TypedBundle[] bundles = new TypedBundle[frames.size()];
        CustomVariable[][] customVars  = null;

        for (int i = 0; i < frames.size(); i++) {
            bundles[i] = new TypedBundle();
        }

        for (int k = 0; k < attrNames.length; k++) {

            String attrName = attrNames[k];
            int attrId = attrIds[k];
            boolean scale = scaleTypes[k];
            CLArray arrayValues = keyAttribute.getArrayOrNull(attrName);
            // array must contain one per frame
            if (arrayValues != null && arrayValues.size() != bundles.length) {
                throw new CLParsingException(
                        "incorrect size for " + attrName + " array, "
                                + "not matching targets array!", keyAttribute);
            }
            if (arrayValues != null) {
                for (int i = 0; i < bundles.length; i++) {
                    float value = arrayValues.getFloat(i);
                    if (scale) {
                        value = transition.mToPixel.toPixels(value);
                    }
                    bundles[i].add(attrId, value);
                }
            } else {
                float value = keyAttribute.getFloatOrNaN(attrName);
                if (!Float.isNaN(value)) {
                    if (scale) {
                        value = transition.mToPixel.toPixels(value);
                    }
                    for (int i = 0; i < bundles.length; i++) {
                        bundles[i].add(attrId, value);
                    }
                }
            }
        }
        // Support for custom attributes in KeyAttributes
        CLElement customElement = keyAttribute.getOrNull("custom");
        if (customElement != null && customElement instanceof CLObject) {
            CLObject customObj = ((CLObject) customElement);
            int n = customObj.size();
            customVars = new CustomVariable[frames.size()][n];
            for (int i = 0; i < n; i++) {
                CLKey key = (CLKey) customObj.get(i);
                String customName = key.content();
                if (key.getValue() instanceof CLArray) {
                    CLArray arrayValues = (CLArray) key.getValue();
                    int vSize = arrayValues.size();
                    if (vSize == bundles.length && vSize > 0) {
                        if (arrayValues.get(0) instanceof CLNumber) {
                            for (int j = 0; j < bundles.length; j++) {
                                customVars[j][i] = new CustomVariable(customName,
                                        TypedValues.Custom.TYPE_FLOAT,
                                        arrayValues.get(j).getFloat());
                            }
                        } else {  // since it is not a number switching to custom color parsing
                            for (int j = 0; j < bundles.length; j++) {
                                long color = parseColorString(arrayValues.get(j).content());
                                if (color != -1) {
                                    customVars[j][i] = new CustomVariable(customName,
                                            TypedValues.Custom.TYPE_COLOR,
                                            (int) color);
                                }
                            }
                        }
                    }
                } else {
                    CLElement value = key.getValue();
                    if (value instanceof CLNumber) {
                        float fValue = value.getFloat();
                        for (int j = 0; j < bundles.length; j++) {
                            customVars[j][i] = new CustomVariable(customName,
                                    TypedValues.Custom.TYPE_FLOAT,
                                    fValue);
                        }
                    } else {
                        long cValue = parseColorString(value.content());
                        if (cValue != -1) {
                            for (int j = 0; j < bundles.length; j++) {
                                customVars[j][i] = new CustomVariable(customName,
                                        TypedValues.Custom.TYPE_COLOR,
                                        (int) cValue);

                            }
                        }
                    }
                }

            }
        }
        String curveFit = keyAttribute.getStringOrNull("curveFit");
        for (int i = 0; i < targets.size(); i++) {
            for (int j = 0; j < bundles.length; j++) {
                String target = targets.getString(i);
                TypedBundle bundle = bundles[j];
                if (curveFit != null) {
                    bundle.add(TypedValues.PositionType.TYPE_CURVE_FIT,
                            map(curveFit, "spline", "linear"));
                }
                bundle.addIfNotNull(TypedValues.PositionType.TYPE_TRANSITION_EASING,
                        transitionEasing);
                int frame = frames.getInt(j);
                bundle.add(TypedValues.TYPE_FRAME_POSITION, frame);
                transition.addKeyAttribute(target, bundle, (customVars != null) ? customVars[j] :
                        null);
            }
        }
    }

    private static void parseKeyCycle(CLObject keyCycleData,
            Transition transition) throws CLParsingException {
        CLArray targets = keyCycleData.getArray("target");
        CLArray frames = keyCycleData.getArray("frames");
        String transitionEasing = keyCycleData.getStringOrNull("transitionEasing");
        // These present an ordered list of attributes that might be used in a keyCycle
        String[] attrNames = {
                TypedValues.CycleType.S_SCALE_X,
                TypedValues.CycleType.S_SCALE_Y,
                TypedValues.CycleType.S_TRANSLATION_X,
                TypedValues.CycleType.S_TRANSLATION_Y,
                TypedValues.CycleType.S_TRANSLATION_Z,
                TypedValues.CycleType.S_ROTATION_X,
                TypedValues.CycleType.S_ROTATION_Y,
                TypedValues.CycleType.S_ROTATION_Z,
                TypedValues.CycleType.S_ALPHA,
                TypedValues.CycleType.S_WAVE_PERIOD,
                TypedValues.CycleType.S_WAVE_OFFSET,
                TypedValues.CycleType.S_WAVE_PHASE,
        };
        int[] attrIds = {
                TypedValues.CycleType.TYPE_SCALE_X,
                TypedValues.CycleType.TYPE_SCALE_Y,
                TypedValues.CycleType.TYPE_TRANSLATION_X,
                TypedValues.CycleType.TYPE_TRANSLATION_Y,
                TypedValues.CycleType.TYPE_TRANSLATION_Z,
                TypedValues.CycleType.TYPE_ROTATION_X,
                TypedValues.CycleType.TYPE_ROTATION_Y,
                TypedValues.CycleType.TYPE_ROTATION_Z,
                TypedValues.CycleType.TYPE_ALPHA,
                TypedValues.CycleType.TYPE_WAVE_PERIOD,
                TypedValues.CycleType.TYPE_WAVE_OFFSET,
                TypedValues.CycleType.TYPE_WAVE_PHASE,
        };
        // type 0 the values are used as.
        // type 1 the value is scaled from dp to pixels.
        // type 2 are scaled if the system has another type 1.
        int[] scaleTypes = {
                0,
                0,
                1,
                1,
                1,
                0,
                0,
                0,
                0,
                0,
                2,
                0,
        };

//  TODO S_WAVE_SHAPE S_CUSTOM_WAVE_SHAPE
        TypedBundle[] bundles = new TypedBundle[frames.size()];
        for (int i = 0; i < bundles.length; i++) {
            bundles[i] = new TypedBundle();
        }
        boolean scaleOffset = false;
        for (int k = 0; k < attrNames.length; k++) {
            if (keyCycleData.has(attrNames[k]) && scaleTypes[k] == 1) {
                scaleOffset = true;
            }
        }
        for (int k = 0; k < attrNames.length; k++) {
            String attrName = attrNames[k];
            int attrId = attrIds[k];
            int scale = scaleTypes[k];
            CLArray arrayValues = keyCycleData.getArrayOrNull(attrName);
            // array must contain one per frame
            if (arrayValues != null && arrayValues.size() != bundles.length) {
                throw new CLParsingException(
                        "incorrect size for $attrName array, "
                                + "not matching targets array!", keyCycleData
                );
            }
            if (arrayValues != null) {
                for (int i = 0; i < bundles.length; i++) {
                    float value = arrayValues.getFloat(i);
                    if (scale == 1) {
                        value = transition.mToPixel.toPixels(value);
                    } else if (scale == 2 && scaleOffset) {
                        value = transition.mToPixel.toPixels(value);
                    }
                    bundles[i].add(attrId, value);
                }
            } else {
                float value = keyCycleData.getFloatOrNaN(attrName);
                if (!Float.isNaN(value)) {
                    if (scale == 1) {
                        value = transition.mToPixel.toPixels(value);
                    } else if (scale == 2 && scaleOffset) {
                        value = transition.mToPixel.toPixels(value);
                    }
                    for (int i = 0; i < bundles.length; i++) {
                        bundles[i].add(attrId, value);
                    }
                }
            }
        }
        String curveFit = keyCycleData.getStringOrNull(TypedValues.CycleType.S_CURVE_FIT);
        String easing = keyCycleData.getStringOrNull(TypedValues.CycleType.S_EASING);
        String waveShape = keyCycleData.getStringOrNull(TypedValues.CycleType.S_WAVE_SHAPE);
        String customWave = keyCycleData.getStringOrNull(TypedValues.CycleType.S_CUSTOM_WAVE_SHAPE);
        for (int i = 0; i < targets.size(); i++) {
            for (int j = 0; j < bundles.length; j++) {
                String target = targets.getString(i);
                TypedBundle bundle = bundles[j];


                if (curveFit != null) {
                    switch (curveFit) {
                        case "spline":
                            bundle.add(TypedValues.CycleType.TYPE_CURVE_FIT, 0);
                            break;
                        case "linear":
                            bundle.add(TypedValues.CycleType.TYPE_CURVE_FIT, 1);
                            break;
                    }
                }
                bundle.addIfNotNull(TypedValues.PositionType.TYPE_TRANSITION_EASING,
                        transitionEasing);
                if (easing != null) {
                    bundle.add(TypedValues.CycleType.TYPE_EASING, easing);
                }
                if (waveShape != null) {
                    bundle.add(TypedValues.CycleType.TYPE_WAVE_SHAPE, waveShape);
                }
                if (customWave != null) {
                    bundle.add(TypedValues.CycleType.TYPE_CUSTOM_WAVE_SHAPE, customWave);
                }

                int frame = frames.getInt(j);
                bundle.add(TypedValues.TYPE_FRAME_POSITION, frame);
                transition.addKeyCycle(target, bundle);

            }
        }
    }
}
