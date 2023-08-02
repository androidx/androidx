/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.constraintlayout.core.motion.utils;

/**
 * Provides an interface to values used in KeyFrames and in
 * Starting and Ending Widgets
 */
public interface TypedValues {
    String S_CUSTOM = "CUSTOM";
    int BOOLEAN_MASK = 1;
    int INT_MASK = 2;
    int FLOAT_MASK = 4;
    int STRING_MASK = 8;

    /**
     * Used to set integer values
     *
     * @return true if it accepted the value
     */
    boolean setValue(int id, int value);

    /**
     * Used to set float values
     *
     * @return true if it accepted the value
     */
    boolean setValue(int id, float value);

    /**
     * Used to set String values
     *
     * @return true if it accepted the value
     */
    boolean setValue(int id, String value);

    /**
     * Used to set boolean values
     *
     * @return true if it accepted the value
     */
    boolean setValue(int id, boolean value);

    // @TODO: add description
    int getId(String name);

    int TYPE_FRAME_POSITION = 100;
    int TYPE_TARGET = 101;

    interface AttributesType {
        String NAME = "KeyAttributes";

        int TYPE_CURVE_FIT = 301;
        int TYPE_VISIBILITY = 302;
        int TYPE_ALPHA = 303;
        int TYPE_TRANSLATION_X = 304;
        int TYPE_TRANSLATION_Y = 305;
        int TYPE_TRANSLATION_Z = 306;
        int TYPE_ELEVATION = 307;
        int TYPE_ROTATION_X = 308;
        int TYPE_ROTATION_Y = 309;
        int TYPE_ROTATION_Z = 310;
        int TYPE_SCALE_X = 311;
        int TYPE_SCALE_Y = 312;
        int TYPE_PIVOT_X = 313;
        int TYPE_PIVOT_Y = 314;
        int TYPE_PROGRESS = 315;
        int TYPE_PATH_ROTATE = 316;
        int TYPE_EASING = 317;
        int TYPE_PIVOT_TARGET = 318;

        String S_CURVE_FIT = "curveFit";
        String S_VISIBILITY = "visibility";
        String S_ALPHA = "alpha";

        String S_TRANSLATION_X = "translationX";
        String S_TRANSLATION_Y = "translationY";
        String S_TRANSLATION_Z = "translationZ";
        String S_ELEVATION = "elevation";
        String S_ROTATION_X = "rotationX";
        String S_ROTATION_Y = "rotationY";
        String S_ROTATION_Z = "rotationZ";
        String S_SCALE_X = "scaleX";
        String S_SCALE_Y = "scaleY";
        String S_PIVOT_X = "pivotX";
        String S_PIVOT_Y = "pivotY";
        String S_PROGRESS = "progress";
        String S_PATH_ROTATE = "pathRotate";
        String S_EASING = "easing";
        String S_CUSTOM = "CUSTOM";
        String S_FRAME = "frame";
        String S_TARGET = "target";
        String S_PIVOT_TARGET = "pivotTarget";

        String[] KEY_WORDS = {
                S_CURVE_FIT,
                S_VISIBILITY,
                S_ALPHA,
                S_TRANSLATION_X,
                S_TRANSLATION_Y,
                S_TRANSLATION_Z,
                S_ELEVATION,
                S_ROTATION_X,
                S_ROTATION_Y,
                S_ROTATION_Z,
                S_SCALE_X,
                S_SCALE_Y,
                S_PIVOT_X,
                S_PIVOT_Y,
                S_PROGRESS,
                S_PATH_ROTATE,
                S_EASING,
                S_CUSTOM,
                S_FRAME,
                S_TARGET,
                S_PIVOT_TARGET,
        };

        /**
         * Method to go from String names of values to id of the values
         * IDs are use for efficiency
         *
         * @param name the name of the value
         * @return the id of the vlalue or -1 if no value exist
         */
        static int getId(String name) {
            switch (name) {
                case S_CURVE_FIT:
                    return TYPE_CURVE_FIT;
                case S_VISIBILITY:
                    return TYPE_VISIBILITY;
                case S_ALPHA:
                    return TYPE_ALPHA;
                case S_TRANSLATION_X:
                    return TYPE_TRANSLATION_X;
                case S_TRANSLATION_Y:
                    return TYPE_TRANSLATION_Y;
                case S_TRANSLATION_Z:
                    return TYPE_TRANSLATION_Z;
                case S_ELEVATION:
                    return TYPE_ELEVATION;
                case S_ROTATION_X:
                    return TYPE_ROTATION_X;
                case S_ROTATION_Y:
                    return TYPE_ROTATION_Y;
                case S_ROTATION_Z:
                    return TYPE_ROTATION_Z;
                case S_SCALE_X:
                    return TYPE_SCALE_X;
                case S_SCALE_Y:
                    return TYPE_SCALE_Y;
                case S_PIVOT_X:
                    return TYPE_PIVOT_X;
                case S_PIVOT_Y:
                    return TYPE_PIVOT_Y;
                case S_PROGRESS:
                    return TYPE_PROGRESS;
                case S_PATH_ROTATE:
                    return TYPE_PATH_ROTATE;
                case S_EASING:
                    return TYPE_EASING;
                case S_FRAME:
                    return TYPE_FRAME_POSITION;
                case S_TARGET:
                    return TYPE_TARGET;
                case S_PIVOT_TARGET:
                    return TYPE_PIVOT_TARGET;
            }
            return -1;
        }

        static int getType(int name) {
            switch (name) {
                case TYPE_CURVE_FIT:
                case TYPE_VISIBILITY:
                case TYPE_FRAME_POSITION:
                    return INT_MASK;
                case TYPE_ALPHA:
                case TYPE_TRANSLATION_X:
                case TYPE_TRANSLATION_Y:
                case TYPE_TRANSLATION_Z:
                case TYPE_ELEVATION:
                case TYPE_ROTATION_X:
                case TYPE_ROTATION_Y:
                case TYPE_ROTATION_Z:
                case TYPE_SCALE_X:
                case TYPE_SCALE_Y:
                case TYPE_PIVOT_X:
                case TYPE_PIVOT_Y:
                case TYPE_PROGRESS:
                case TYPE_PATH_ROTATE:
                    return FLOAT_MASK;
                case TYPE_EASING:
                case TYPE_TARGET:
                case TYPE_PIVOT_TARGET:
                    return STRING_MASK;
            }
            return -1;
        }
    }

    interface CycleType {
        String NAME = "KeyCycle";
        int TYPE_CURVE_FIT = 401;
        int TYPE_VISIBILITY = 402;
        int TYPE_ALPHA = 403;
        int TYPE_TRANSLATION_X = AttributesType.TYPE_TRANSLATION_X;
        int TYPE_TRANSLATION_Y = AttributesType.TYPE_TRANSLATION_Y;
        int TYPE_TRANSLATION_Z = AttributesType.TYPE_TRANSLATION_Z;
        int TYPE_ELEVATION = AttributesType.TYPE_ELEVATION;

        int TYPE_ROTATION_X = AttributesType.TYPE_ROTATION_X;
        int TYPE_ROTATION_Y = AttributesType.TYPE_ROTATION_Y;
        int TYPE_ROTATION_Z = AttributesType.TYPE_ROTATION_Z;
        int TYPE_SCALE_X = AttributesType.TYPE_SCALE_X;
        int TYPE_SCALE_Y = AttributesType.TYPE_SCALE_Y;
        int TYPE_PIVOT_X = AttributesType.TYPE_PIVOT_X;
        int TYPE_PIVOT_Y = AttributesType.TYPE_PIVOT_Y;
        int TYPE_PROGRESS = AttributesType.TYPE_PROGRESS;
        int TYPE_PATH_ROTATE = 416;
        int TYPE_EASING = 420;
        int TYPE_WAVE_SHAPE = 421;
        int TYPE_CUSTOM_WAVE_SHAPE = 422;
        int TYPE_WAVE_PERIOD = 423;
        int TYPE_WAVE_OFFSET = 424;
        int TYPE_WAVE_PHASE = 425;

        String S_CURVE_FIT = "curveFit";
        String S_VISIBILITY = "visibility";
        String S_ALPHA = AttributesType.S_ALPHA;
        String S_TRANSLATION_X = AttributesType.S_TRANSLATION_X;
        String S_TRANSLATION_Y = AttributesType.S_TRANSLATION_Y;
        String S_TRANSLATION_Z = AttributesType.S_TRANSLATION_Z;
        String S_ELEVATION = AttributesType.S_ELEVATION;
        String S_ROTATION_X = AttributesType.S_ROTATION_X;
        String S_ROTATION_Y = AttributesType.S_ROTATION_Y;
        String S_ROTATION_Z = AttributesType.S_ROTATION_Z;
        String S_SCALE_X = AttributesType.S_SCALE_X;
        String S_SCALE_Y = AttributesType.S_SCALE_Y;
        String S_PIVOT_X = AttributesType.S_PIVOT_X;
        String S_PIVOT_Y = AttributesType.S_PIVOT_Y;
        String S_PROGRESS = AttributesType.S_PROGRESS;

        String S_PATH_ROTATE = "pathRotate";
        String S_EASING = "easing";
        String S_WAVE_SHAPE = "waveShape";
        String S_CUSTOM_WAVE_SHAPE = "customWave";
        String S_WAVE_PERIOD = "period";
        String S_WAVE_OFFSET = "offset";
        String S_WAVE_PHASE = "phase";
        String[] KEY_WORDS = {
                S_CURVE_FIT,
                S_VISIBILITY,
                S_ALPHA,
                S_TRANSLATION_X,
                S_TRANSLATION_Y,
                S_TRANSLATION_Z,
                S_ELEVATION,
                S_ROTATION_X,
                S_ROTATION_Y,
                S_ROTATION_Z,
                S_SCALE_X,
                S_SCALE_Y,
                S_PIVOT_X,
                S_PIVOT_Y,
                S_PROGRESS,

                S_PATH_ROTATE,
                S_EASING,
                S_WAVE_SHAPE,
                S_CUSTOM_WAVE_SHAPE,
                S_WAVE_PERIOD,
                S_WAVE_OFFSET,
                S_WAVE_PHASE,
        };

        /**
         * Method to go from String names of values to id of the values
         * IDs are use for efficiency
         *
         * @param name the name of the value
         * @return the id of the vlalue or -1 if no value exist
         */
        static int getId(String name) {
            switch (name) {
                case S_CURVE_FIT:
                    return TYPE_CURVE_FIT;
                case S_VISIBILITY:
                    return TYPE_VISIBILITY;
                case S_ALPHA:
                    return TYPE_ALPHA;
                case S_TRANSLATION_X:
                    return TYPE_TRANSLATION_X;
                case S_TRANSLATION_Y:
                    return TYPE_TRANSLATION_Y;
                case S_TRANSLATION_Z:
                    return TYPE_TRANSLATION_Z;
                case S_ROTATION_X:
                    return TYPE_ROTATION_X;
                case S_ROTATION_Y:
                    return TYPE_ROTATION_Y;
                case S_ROTATION_Z:
                    return TYPE_ROTATION_Z;
                case S_SCALE_X:
                    return TYPE_SCALE_X;
                case S_SCALE_Y:
                    return TYPE_SCALE_Y;
                case S_PIVOT_X:
                    return TYPE_PIVOT_X;
                case S_PIVOT_Y:
                    return TYPE_PIVOT_Y;
                case S_PROGRESS:
                    return TYPE_PROGRESS;
                case S_PATH_ROTATE:
                    return TYPE_PATH_ROTATE;
                case S_EASING:
                    return TYPE_EASING;
            }
            return -1;
        }

        static int getType(int name) {
            switch (name) {
                case TYPE_CURVE_FIT:
                case TYPE_VISIBILITY:
                case TYPE_FRAME_POSITION:
                    return INT_MASK;
                case TYPE_ALPHA:
                case TYPE_TRANSLATION_X:
                case TYPE_TRANSLATION_Y:
                case TYPE_TRANSLATION_Z:
                case TYPE_ELEVATION:
                case TYPE_ROTATION_X:
                case TYPE_ROTATION_Y:
                case TYPE_ROTATION_Z:
                case TYPE_SCALE_X:
                case TYPE_SCALE_Y:
                case TYPE_PIVOT_X:
                case TYPE_PIVOT_Y:
                case TYPE_PROGRESS:
                case TYPE_PATH_ROTATE:
                case TYPE_WAVE_PERIOD:
                case TYPE_WAVE_OFFSET:
                case TYPE_WAVE_PHASE:
                    return FLOAT_MASK;
                case TYPE_EASING:
                case TYPE_TARGET:
                case TYPE_WAVE_SHAPE:
                    return STRING_MASK;
            }
            return -1;
        }
    }

    interface TriggerType {
        String NAME = "KeyTrigger";
        String VIEW_TRANSITION_ON_CROSS = "viewTransitionOnCross";
        String VIEW_TRANSITION_ON_POSITIVE_CROSS = "viewTransitionOnPositiveCross";
        String VIEW_TRANSITION_ON_NEGATIVE_CROSS = "viewTransitionOnNegativeCross";
        String POST_LAYOUT = "postLayout";
        String TRIGGER_SLACK = "triggerSlack";
        String TRIGGER_COLLISION_VIEW = "triggerCollisionView";
        String TRIGGER_COLLISION_ID = "triggerCollisionId";
        String TRIGGER_ID = "triggerID";
        String POSITIVE_CROSS = "positiveCross";
        String NEGATIVE_CROSS = "negativeCross";
        String TRIGGER_RECEIVER = "triggerReceiver";
        String CROSS = "CROSS";
        String[] KEY_WORDS = {
                VIEW_TRANSITION_ON_CROSS,
                VIEW_TRANSITION_ON_POSITIVE_CROSS,
                VIEW_TRANSITION_ON_NEGATIVE_CROSS,
                POST_LAYOUT,
                TRIGGER_SLACK,
                TRIGGER_COLLISION_VIEW,
                TRIGGER_COLLISION_ID,
                TRIGGER_ID,
                POSITIVE_CROSS,
                NEGATIVE_CROSS,
                TRIGGER_RECEIVER,
                CROSS,
        };
        int TYPE_VIEW_TRANSITION_ON_CROSS = 301;
        int TYPE_VIEW_TRANSITION_ON_POSITIVE_CROSS = 302;
        int TYPE_VIEW_TRANSITION_ON_NEGATIVE_CROSS = 303;
        int TYPE_POST_LAYOUT = 304;
        int TYPE_TRIGGER_SLACK = 305;
        int TYPE_TRIGGER_COLLISION_VIEW = 306;
        int TYPE_TRIGGER_COLLISION_ID = 307;
        int TYPE_TRIGGER_ID = 308;
        int TYPE_POSITIVE_CROSS = 309;
        int TYPE_NEGATIVE_CROSS = 310;
        int TYPE_TRIGGER_RECEIVER = 311;
        int TYPE_CROSS = 312;

        /**
         * Method to go from String names of values to id of the values
         * IDs are use for efficiency
         *
         * @param name the name of the value
         * @return the id of the vlalue or -1 if no value exist
         */
        static int getId(String name) {
            switch (name) {
                case VIEW_TRANSITION_ON_CROSS:
                    return TYPE_VIEW_TRANSITION_ON_CROSS;
                case VIEW_TRANSITION_ON_POSITIVE_CROSS:
                    return TYPE_VIEW_TRANSITION_ON_POSITIVE_CROSS;
                case VIEW_TRANSITION_ON_NEGATIVE_CROSS:
                    return TYPE_VIEW_TRANSITION_ON_NEGATIVE_CROSS;
                case POST_LAYOUT:
                    return TYPE_POST_LAYOUT;
                case TRIGGER_SLACK:
                    return TYPE_TRIGGER_SLACK;
                case TRIGGER_COLLISION_VIEW:
                    return TYPE_TRIGGER_COLLISION_VIEW;
                case TRIGGER_COLLISION_ID:
                    return TYPE_TRIGGER_COLLISION_ID;
                case TRIGGER_ID:
                    return TYPE_TRIGGER_ID;
                case POSITIVE_CROSS:
                    return TYPE_POSITIVE_CROSS;
                case NEGATIVE_CROSS:
                    return TYPE_NEGATIVE_CROSS;
                case TRIGGER_RECEIVER:
                    return TYPE_TRIGGER_RECEIVER;
                case CROSS:
                    return TYPE_CROSS;
            }
            return -1;
        }
    }

    interface PositionType {
        String NAME = "KeyPosition";
        String S_TRANSITION_EASING = "transitionEasing";
        String S_DRAWPATH = "drawPath";
        String S_PERCENT_WIDTH = "percentWidth";
        String S_PERCENT_HEIGHT = "percentHeight";
        String S_SIZE_PERCENT = "sizePercent";
        String S_PERCENT_X = "percentX";
        String S_PERCENT_Y = "percentY";

        int TYPE_TRANSITION_EASING = 501;
        int TYPE_DRAWPATH = 502;
        int TYPE_PERCENT_WIDTH = 503;
        int TYPE_PERCENT_HEIGHT = 504;
        int TYPE_SIZE_PERCENT = 505;
        int TYPE_PERCENT_X = 506;
        int TYPE_PERCENT_Y = 507;
        int TYPE_CURVE_FIT = 508;
        int TYPE_PATH_MOTION_ARC = 509;
        int TYPE_POSITION_TYPE = 510;
        String[] KEY_WORDS = {
                S_TRANSITION_EASING,
                S_DRAWPATH,
                S_PERCENT_WIDTH,
                S_PERCENT_HEIGHT,
                S_SIZE_PERCENT,
                S_PERCENT_X,
                S_PERCENT_Y,
        };

        /**
         * Method to go from String names of values to id of the values
         * IDs are use for efficiency
         *
         * @param name the name of the value
         * @return the id of the vlalue or -1 if no value exist
         */
        static int getId(String name) {
            switch (name) {
                case S_TRANSITION_EASING:
                    return PositionType.TYPE_TRANSITION_EASING;
                case S_DRAWPATH:
                    return PositionType.TYPE_DRAWPATH;
                case S_PERCENT_WIDTH:
                    return PositionType.TYPE_PERCENT_WIDTH;
                case S_PERCENT_HEIGHT:
                    return PositionType.TYPE_PERCENT_HEIGHT;
                case S_SIZE_PERCENT:
                    return PositionType.TYPE_SIZE_PERCENT;
                case S_PERCENT_X:
                    return PositionType.TYPE_PERCENT_X;
                case S_PERCENT_Y:
                    return PositionType.TYPE_PERCENT_Y;
            }
            return -1;
        }

        static int getType(int name) {
            switch (name) {
                case TYPE_CURVE_FIT:
                case TYPE_FRAME_POSITION:
                    return INT_MASK;
                case TYPE_PERCENT_WIDTH:
                case TYPE_PERCENT_HEIGHT:
                case TYPE_SIZE_PERCENT:
                case TYPE_PERCENT_X:
                case TYPE_PERCENT_Y:
                    return FLOAT_MASK;
                case TYPE_TRANSITION_EASING:
                case TYPE_TARGET:
                case TYPE_DRAWPATH:
                    return STRING_MASK;
            }
            return -1;
        }


    }

    interface MotionType {
        String NAME = "Motion";

        String S_STAGGER = "Stagger";
        String S_PATH_ROTATE = "PathRotate";
        String S_QUANTIZE_MOTION_PHASE = "QuantizeMotionPhase";
        String S_EASING = "TransitionEasing";
        String S_QUANTIZE_INTERPOLATOR = "QuantizeInterpolator";
        String S_ANIMATE_RELATIVE_TO = "AnimateRelativeTo";
        String S_ANIMATE_CIRCLEANGLE_TO = "AnimateCircleAngleTo";
        String S_PATHMOTION_ARC = "PathMotionArc";
        String S_DRAW_PATH = "DrawPath";
        String S_POLAR_RELATIVETO = "PolarRelativeTo";
        String S_QUANTIZE_MOTIONSTEPS = "QuantizeMotionSteps";
        String S_QUANTIZE_INTERPOLATOR_TYPE = "QuantizeInterpolatorType";
        String S_QUANTIZE_INTERPOLATOR_ID = "QuantizeInterpolatorID";
        String[] KEY_WORDS = {
                S_STAGGER,
                S_PATH_ROTATE,
                S_QUANTIZE_MOTION_PHASE,
                S_EASING,
                S_QUANTIZE_INTERPOLATOR,
                S_ANIMATE_RELATIVE_TO,
                S_ANIMATE_CIRCLEANGLE_TO,
                S_PATHMOTION_ARC,
                S_DRAW_PATH,
                S_POLAR_RELATIVETO,
                S_QUANTIZE_MOTIONSTEPS,
                S_QUANTIZE_INTERPOLATOR_TYPE,
                S_QUANTIZE_INTERPOLATOR_ID,
        };
        int TYPE_STAGGER = 600;
        int TYPE_PATH_ROTATE = 601;
        int TYPE_QUANTIZE_MOTION_PHASE = 602;
        int TYPE_EASING = 603;
        int TYPE_QUANTIZE_INTERPOLATOR = 604;
        int TYPE_ANIMATE_RELATIVE_TO = 605;
        int TYPE_ANIMATE_CIRCLEANGLE_TO = 606;
        int TYPE_PATHMOTION_ARC = 607;
        int TYPE_DRAW_PATH = 608;
        int TYPE_POLAR_RELATIVETO = 609;
        int TYPE_QUANTIZE_MOTIONSTEPS = 610;
        int TYPE_QUANTIZE_INTERPOLATOR_TYPE = 611;
        int TYPE_QUANTIZE_INTERPOLATOR_ID = 612;

        /**
         * Method to go from String names of values to id of the values
         * IDs are use for efficiency
         *
         * @param name the name of the value
         * @return the id of the vlalue or -1 if no value exist
         */
        static int getId(String name) {
            switch (name) {
                case S_STAGGER:
                    return TYPE_STAGGER;
                case S_PATH_ROTATE:
                    return TYPE_PATH_ROTATE;
                case S_QUANTIZE_MOTION_PHASE:
                    return TYPE_QUANTIZE_MOTION_PHASE;
                case S_EASING:
                    return TYPE_EASING;
                case S_QUANTIZE_INTERPOLATOR:
                    return TYPE_QUANTIZE_INTERPOLATOR;
                case S_ANIMATE_RELATIVE_TO:
                    return TYPE_ANIMATE_RELATIVE_TO;
                case S_ANIMATE_CIRCLEANGLE_TO:
                    return TYPE_ANIMATE_CIRCLEANGLE_TO;
                case S_PATHMOTION_ARC:
                    return TYPE_PATHMOTION_ARC;
                case S_DRAW_PATH:
                    return TYPE_DRAW_PATH;
                case S_POLAR_RELATIVETO:
                    return TYPE_POLAR_RELATIVETO;
                case S_QUANTIZE_MOTIONSTEPS:
                    return TYPE_QUANTIZE_MOTIONSTEPS;
                case S_QUANTIZE_INTERPOLATOR_TYPE:
                    return TYPE_QUANTIZE_INTERPOLATOR_TYPE;
                case S_QUANTIZE_INTERPOLATOR_ID:
                    return TYPE_QUANTIZE_INTERPOLATOR_ID;
            }
            return -1;
        }

    }

    interface Custom {
        String NAME = "Custom";
        String S_INT = "integer";
        String S_FLOAT = "float";
        String S_COLOR = "color";
        String S_STRING = "string";
        String S_BOOLEAN = "boolean";
        String S_DIMENSION = "dimension";
        String S_REFERENCE = "reference";
        String[] KEY_WORDS = {
                S_FLOAT,
                S_COLOR,
                S_STRING,
                S_BOOLEAN,
                S_DIMENSION,
                S_REFERENCE,
        };
        int TYPE_INT = 900;
        int TYPE_FLOAT = 901;
        int TYPE_COLOR = 902;
        int TYPE_STRING = 903;
        int TYPE_BOOLEAN = 904;
        int TYPE_DIMENSION = 905;
        int TYPE_REFERENCE = 906;

        /**
         * Method to go from String names of values to id of the values
         * IDs are use for efficiency
         *
         * @param name the name of the value
         * @return the id of the vlalue or -1 if no value exist
         */
        static int getId(String name) {
            switch (name) {
                case S_INT:
                    return TYPE_INT;
                case S_FLOAT:
                    return TYPE_FLOAT;
                case S_COLOR:
                    return TYPE_COLOR;
                case S_STRING:
                    return TYPE_STRING;
                case S_BOOLEAN:
                    return TYPE_BOOLEAN;
                case S_DIMENSION:
                    return TYPE_DIMENSION;
                case S_REFERENCE:
                    return TYPE_REFERENCE;
            }
            return -1;
        }
    }

    interface MotionScene {
        String NAME = "MotionScene";
        String S_DEFAULT_DURATION = "defaultDuration";
        String S_LAYOUT_DURING_TRANSITION = "layoutDuringTransition";
        int TYPE_DEFAULT_DURATION = 600;
        int TYPE_LAYOUT_DURING_TRANSITION = 601;

        String[] KEY_WORDS = {
                S_DEFAULT_DURATION,
                S_LAYOUT_DURING_TRANSITION,
        };

        static int getType(int name) {
            switch (name) {
                case TYPE_DEFAULT_DURATION:
                    return INT_MASK;
                case TYPE_LAYOUT_DURING_TRANSITION:
                    return BOOLEAN_MASK;
            }
            return -1;
        }

        /**
         * Method to go from String names of values to id of the values
         * IDs are use for efficiency
         *
         * @param name the name of the value
         * @return the id of the vlalue or -1 if no value exist
         */
        static int getId(String name) {
            switch (name) {
                case S_DEFAULT_DURATION:
                    return TYPE_DEFAULT_DURATION;
                case S_LAYOUT_DURING_TRANSITION:
                    return TYPE_LAYOUT_DURING_TRANSITION;
            }
            return -1;
        }
    }

    interface TransitionType {
        String NAME = "Transitions";
        String S_DURATION = "duration";
        String S_FROM = "from";
        String S_TO = "to";
        String S_PATH_MOTION_ARC = "pathMotionArc";
        String S_AUTO_TRANSITION = "autoTransition";
        String S_INTERPOLATOR = "motionInterpolator";
        String S_STAGGERED = "staggered";
        String S_TRANSITION_FLAGS = "transitionFlags";

        int TYPE_DURATION = 700;
        int TYPE_FROM = 701;
        int TYPE_TO = 702;
        int TYPE_PATH_MOTION_ARC = PositionType.TYPE_PATH_MOTION_ARC;
        int TYPE_AUTO_TRANSITION = 704;
        int TYPE_INTERPOLATOR = 705;
        int TYPE_STAGGERED = 706;
        int TYPE_TRANSITION_FLAGS = 707;


        String[] KEY_WORDS = {
                S_DURATION,
                S_FROM,
                S_TO,
                S_PATH_MOTION_ARC,
                S_AUTO_TRANSITION,
                S_INTERPOLATOR,
                S_STAGGERED,
                S_FROM,
                S_TRANSITION_FLAGS,
        };

        static int getType(int name) {
            switch (name) {
                case TYPE_DURATION:
                case TYPE_PATH_MOTION_ARC:
                    return INT_MASK;
                case TYPE_FROM:
                case TYPE_TO:
                case TYPE_INTERPOLATOR:
                case TYPE_TRANSITION_FLAGS:
                    return STRING_MASK;

                case TYPE_STAGGERED:
                    return FLOAT_MASK;
            }
            return -1;
        }

        /**
         * Method to go from String names of values to id of the values
         * IDs are use for efficiency
         *
         * @param name the name of the value
         * @return the id of the vlalue or -1 if no value exist
         */
        static int getId(String name) {
            switch (name) {
                case S_DURATION:
                    return TYPE_DURATION;
                case S_FROM:
                    return TYPE_FROM;
                case S_TO:
                    return TYPE_TO;
                case S_PATH_MOTION_ARC:
                    return TYPE_PATH_MOTION_ARC;
                case S_AUTO_TRANSITION:
                    return TYPE_AUTO_TRANSITION;
                case S_INTERPOLATOR:
                    return TYPE_INTERPOLATOR;
                case S_STAGGERED:
                    return TYPE_STAGGERED;
                case S_TRANSITION_FLAGS:
                    return TYPE_TRANSITION_FLAGS;
            }
            return -1;
        }
    }

    interface OnSwipe {
        String DRAG_SCALE = "dragscale";
        String DRAG_THRESHOLD = "dragthreshold";

        String MAX_VELOCITY = "maxvelocity";
        String MAX_ACCELERATION = "maxacceleration";
        String SPRING_MASS = "springmass";
        String SPRING_STIFFNESS = "springstiffness";
        String SPRING_DAMPING = "springdamping";
        String SPRINGS_TOP_THRESHOLD = "springstopthreshold";

        String DRAG_DIRECTION = "dragdirection";
        String TOUCH_ANCHOR_ID = "touchanchorid";
        String TOUCH_ANCHOR_SIDE = "touchanchorside";
        String ROTATION_CENTER_ID = "rotationcenterid";
        String TOUCH_REGION_ID = "touchregionid";
        String LIMIT_BOUNDS_TO = "limitboundsto";

        String MOVE_WHEN_SCROLLAT_TOP = "movewhenscrollattop";
        String ON_TOUCH_UP = "ontouchup";
        String[] ON_TOUCH_UP_ENUM = {"autoComplete",
                "autoCompleteToStart",
                "autoCompleteToEnd",
                "stop",
                "decelerate",
                "decelerateAndComplete",
                "neverCompleteToStart",
                "neverCompleteToEnd"};


        String SPRING_BOUNDARY = "springboundary";
        String[] SPRING_BOUNDARY_ENUM = {"overshoot",
                "bounceStart",
                "bounceEnd",
                "bounceBoth"};

        String AUTOCOMPLETE_MODE = "autocompletemode";
        String[] AUTOCOMPLETE_MODE_ENUM = {
                "continuousVelocity",
                "spring"};

        String NESTED_SCROLL_FLAGS = "nestedscrollflags";
        String[] NESTED_SCROLL_FLAGS_ENUM = {"none",
                "disablePostScroll",
                "disableScroll",
                "supportScrollUp"};

    }

}
