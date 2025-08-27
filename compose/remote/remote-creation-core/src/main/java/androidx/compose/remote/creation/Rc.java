/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.remote.creation;

import static androidx.compose.remote.core.operations.ColorAttribute.COLOR_ALPHA;
import static androidx.compose.remote.core.operations.ColorAttribute.COLOR_BLUE;
import static androidx.compose.remote.core.operations.ColorAttribute.COLOR_BRIGHTNESS;
import static androidx.compose.remote.core.operations.ColorAttribute.COLOR_GREEN;
import static androidx.compose.remote.core.operations.ColorAttribute.COLOR_HUE;
import static androidx.compose.remote.core.operations.ColorAttribute.COLOR_RED;
import static androidx.compose.remote.core.operations.ColorAttribute.COLOR_SATURATION;
import static androidx.compose.remote.core.operations.PathExpression.LINEAR;
import static androidx.compose.remote.core.operations.PathExpression.LOOP;
import static androidx.compose.remote.core.operations.PathExpression.MONOTONIC;
import static androidx.compose.remote.core.operations.PathExpression.POLAR;

import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.ConditionalOperations;
import androidx.compose.remote.core.operations.DebugMessage;
import androidx.compose.remote.core.operations.DrawTextAnchored;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.TimeAttribute;
import androidx.compose.remote.core.operations.layout.managers.TextLayout;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.ImageScaling;
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator;
import androidx.compose.remote.core.operations.utilities.easing.Easing;

/** Constants use in RemoteCompose */
public class Rc {
    /** Used in FloatExpressions */
    public static class FloatExpression {

        /** ADD operator */
        public static final float ADD = AnimatedFloatExpression.ADD;

        /** SUB operator */
        public static final float SUB = AnimatedFloatExpression.SUB;

        /** MUL operator */
        public static final float MUL = AnimatedFloatExpression.MUL;

        /** DIV operator */
        public static final float DIV = AnimatedFloatExpression.DIV;

        /** MOD operator */
        public static final float MOD = AnimatedFloatExpression.MOD;

        /** MIN operator */
        public static final float MIN = AnimatedFloatExpression.MIN;

        /** MAX operator */
        public static final float MAX = AnimatedFloatExpression.MAX;

        /** POW operator */
        public static final float POW = AnimatedFloatExpression.POW;

        /** SQRT operator */
        public static final float SQRT = AnimatedFloatExpression.SQRT;

        /** ABS operator */
        public static final float ABS = AnimatedFloatExpression.ABS;

        /** SIGN operator */
        public static final float SIGN = AnimatedFloatExpression.SIGN;

        /** COPY_SIGN operator */
        public static final float COPY_SIGN = AnimatedFloatExpression.COPY_SIGN;

        /** EXP operator */
        public static final float EXP = AnimatedFloatExpression.EXP;

        /** FLOOR operator */
        public static final float FLOOR = AnimatedFloatExpression.FLOOR;

        /** LOG operator */
        public static final float LOG = AnimatedFloatExpression.LOG;

        /** LN operator */
        public static final float LN = AnimatedFloatExpression.LN;

        /** ROUND operator */
        public static final float ROUND = AnimatedFloatExpression.ROUND;

        /** SIN operator */
        public static final float SIN = AnimatedFloatExpression.SIN;

        /** COS operator */
        public static final float COS = AnimatedFloatExpression.COS;

        /** TAN operator */
        public static final float TAN = AnimatedFloatExpression.TAN;

        /** ASIN operator */
        public static final float ASIN = AnimatedFloatExpression.ASIN;

        /** ACOS operator */
        public static final float ACOS = AnimatedFloatExpression.ACOS;

        /** ATAN operator */
        public static final float ATAN = AnimatedFloatExpression.ATAN;

        /** ATAN2 operator */
        public static final float ATAN2 = AnimatedFloatExpression.ATAN2;

        /** MAD operator */
        public static final float MAD = AnimatedFloatExpression.MAD;

        /** IFELSE operator */
        public static final float IFELSE = AnimatedFloatExpression.IFELSE;

        /** CLAMP operator */
        public static final float CLAMP = AnimatedFloatExpression.CLAMP;

        /** CBRT operator */
        public static final float CBRT = AnimatedFloatExpression.CBRT;

        /** DEG operator */
        public static final float DEG = AnimatedFloatExpression.DEG;

        /** RAD operator */
        public static final float RAD = AnimatedFloatExpression.RAD;

        /** CEIL operator */
        public static final float CEIL = AnimatedFloatExpression.CEIL;

        // Array ops
        /** A DEREF operator */
        public static final float A_DEREF = AnimatedFloatExpression.A_DEREF;

        /** Array MAX operator */
        public static final float A_MAX = AnimatedFloatExpression.A_MAX;

        /** Array MIN operator */
        public static final float A_MIN = AnimatedFloatExpression.A_MIN;

        /** A_SUM operator */
        public static final float A_SUM = AnimatedFloatExpression.A_SUM;

        /** A_AVG operator */
        public static final float A_AVG = AnimatedFloatExpression.A_AVG;

        /** A_LEN operator */
        public static final float A_LEN = AnimatedFloatExpression.A_LEN;

        /** A_SPLINE operator */
        public static final float A_SPLINE = AnimatedFloatExpression.A_SPLINE;

        /** RAND Random number 0..1 */
        public static final float RAND = AnimatedFloatExpression.RAND;

        /** RAND_SEED operator */
        public static final float RAND_SEED = AnimatedFloatExpression.RAND_SEED;

        /** NOISE_FROM operator calculate a random 0..1 number based on a seed */
        public static final float NOISE_FROM = AnimatedFloatExpression.NOISE_FROM;

        /** RANDOM_IN_RANGE random number in range */
        public static final float RAND_IN_RANGE = AnimatedFloatExpression.RAND_IN_RANGE;

        /** SQUARE_SUM the sum of the square of two numbers */
        public static final float SQUARE_SUM = AnimatedFloatExpression.SQUARE_SUM;

        /** STEP x > edge ? 1 : 0; */
        public static final float STEP = AnimatedFloatExpression.STEP;

        /** SQUARE x*x; */
        public static final float SQUARE = AnimatedFloatExpression.SQUARE;

        /** DUP x,x; */
        public static final float DUP = AnimatedFloatExpression.DUP;

        /** HYPOT sqrt(x*x+y*y); */
        public static final float HYPOT = AnimatedFloatExpression.HYPOT;

        /** SWAP y,x; */
        public static final float SWAP = AnimatedFloatExpression.SWAP;

        /** LERP (1-t)*x+t*y; */
        public static final float LERP = AnimatedFloatExpression.LERP;

        /** SMOOTH_STEP (1-smoothstep(edge0,edge1,x)); */
        public static final float SMOOTH_STEP = AnimatedFloatExpression.SMOOTH_STEP;

        /** LOG2 (log base 2) operator */
        public static final float LOG2 = AnimatedFloatExpression.LOG2;

        /** INV 1/x operator */
        public static final float INV = AnimatedFloatExpression.INV;

        /** FRACT (fractional part) operator */
        public static final float FRACT = AnimatedFloatExpression.FRACT;

        /** PINGPONG (go pp(x,y) = x%2*y < x ? x%2*y : y-x%2*y) operator */
        public static final float PINGPONG = AnimatedFloatExpression.PINGPONG;

        /** VAR1 operator */
        public static final float VAR1 = AnimatedFloatExpression.VAR1;

        /** VAR2 operator */
        public static final float VAR2 = AnimatedFloatExpression.VAR2;

        /** VAR2 operator */
        public static final float VAR3 = AnimatedFloatExpression.VAR3;
    }

    /** Used in IntegerExpressions */
    public static class IntegerExpression {
        /** addition (op1 + op2) */
        public static final long L_ADD = 0x100000000L + IntegerExpressionEvaluator.I_ADD;

        /** subtraction (op1 - op2) */
        public static final long L_SUB = 0x100000000L + IntegerExpressionEvaluator.I_SUB;

        /** multiplication (op1 * op2) */
        public static final long L_MUL = 0x100000000L + IntegerExpressionEvaluator.I_MUL;

        /** division (op1 / op2) */
        public static final long L_DIV = 0x100000000L + IntegerExpressionEvaluator.I_DIV;

        /** modulo (op1 % op2) */
        public static final long L_MOD = 0x100000000L + IntegerExpressionEvaluator.I_MOD;

        /** bitwise left shift (op1 << op2) */
        public static final long L_SHL = 0x100000000L + IntegerExpressionEvaluator.I_SHL;

        /** bitwise signed right shift (op1 >> op2) */
        public static final long L_SHR = 0x100000000L + IntegerExpressionEvaluator.I_SHR;

        /** bitwise unsigned right shift (op1 >>> op2) */
        public static final long L_USHR = 0x100000000L + IntegerExpressionEvaluator.I_USHR;

        /** bitwise AND (op1 & op2) */
        public static final long L_OR = 0x100000000L + IntegerExpressionEvaluator.I_OR;

        /** bitwise XOR (op1 ^ op2) */
        public static final long L_AND = 0x100000000L + IntegerExpressionEvaluator.I_AND;

        /** copy sign (returns op1 with sign of op2) */
        public static final long L_XOR = 0x100000000L + IntegerExpressionEvaluator.I_XOR;

        /** copy sign (returns op1 with sign of op2) */
        public static final long L_COPY_SIGN =
                0x100000000L + IntegerExpressionEvaluator.I_COPY_SIGN;

        /** minimum (min(op1, op2)) */
        public static final long L_MIN = 0x100000000L + IntegerExpressionEvaluator.I_MIN;

        /** maximum (max(op1, op2)) */
        public static final long L_MAX = 0x100000000L + IntegerExpressionEvaluator.I_MAX;

        /** negation (-op1) */
        public static final long L_NEG = 0x100000000L + IntegerExpressionEvaluator.I_NEG;

        /** absolute value (abs(op1)) */
        public static final long L_ABS = 0x100000000L + IntegerExpressionEvaluator.I_ABS;

        /** increment (op1 + 1) */
        public static final long L_INCR = 0x100000000L + IntegerExpressionEvaluator.I_INCR;

        /** decrement (op1 - 1) */
        public static final long L_DECR = 0x100000000L + IntegerExpressionEvaluator.I_DECR;

        /** bitwise NOT (~op1) */
        public static final long L_NOT = 0x100000000L + IntegerExpressionEvaluator.I_NOT;

        /** signum (sign(op1)) */
        public static final long L_SIGN = 0x100000000L + IntegerExpressionEvaluator.I_SIGN;

        /** clamp (clamp(value, min, max)) */
        public static final long L_CLAMP = 0x100000000L + IntegerExpressionEvaluator.I_CLAMP;

        /** if-else (condition ? if_true : if_false) */
        public static final long L_IFELSE = 0x100000000L + IntegerExpressionEvaluator.I_IFELSE;

        /** multiply-add (op1 * op2 + op3) */
        public static final long L_MAD = 0x100000000L + IntegerExpressionEvaluator.I_MAD;

        /** to push the value of variable 1 */
        public static final long L_VAR1 = 0x100000000L + IntegerExpressionEvaluator.I_VAR1;

        /** to push the value variable 2 */
        public static final long L_VAR2 = 0x100000000L + IntegerExpressionEvaluator.I_VAR2;
    }

    /** Used in Easing */
    public static class Animate {
        /** cubic Easing function that accelerates and decelerates */
        public static final int CUBIC_STANDARD = Easing.CUBIC_STANDARD;

        /** cubic Easing function that accelerates */
        public static final int CUBIC_ACCELERATE = Easing.CUBIC_ACCELERATE;

        /** cubic Easing function that decelerates */
        public static final int CUBIC_DECELERATE = Easing.CUBIC_DECELERATE;

        /** cubic Easing function that just linearly interpolates */
        public static final int CUBIC_LINEAR = Easing.CUBIC_LINEAR;

        /** cubic Easing function that goes bacwards and then accelerates */
        public static final int CUBIC_ANTICIPATE = Easing.CUBIC_ANTICIPATE;

        /** cubic Easing function that overshoots and then goes back */
        public static final int CUBIC_OVERSHOOT = Easing.CUBIC_OVERSHOOT;

        /** cubic Easing function that you customize */
        public static final int CUBIC_CUSTOM = Easing.CUBIC_CUSTOM;

        /** a monotonic spline Easing function that you customize */
        public static final int SPLINE_CUSTOM = Easing.SPLINE_CUSTOM;

        /** a bouncing Easing function */
        public static final int EASE_OUT_BOUNCE = Easing.EASE_OUT_BOUNCE;

        /** a elastic Easing function */
        public static final int EASE_OUT_ELASTIC = Easing.EASE_OUT_ELASTIC;
    }

    /** Used in ColorExpression */
    public static class ColorExpression {
        /** COLOR_COLOR_INTERPOLATE */
        public static final byte COLOR_COLOR_INTERPOLATE =
                androidx.compose.remote.core.operations.ColorExpression.COLOR_COLOR_INTERPOLATE;

        /** COLOR_ID_INTERPOLATE */
        public static final byte ID_COLOR_INTERPOLATE =
                androidx.compose.remote.core.operations.ColorExpression.ID_COLOR_INTERPOLATE;

        /** ID_COLOR_INTERPOLATE */
        public static final byte COLOR_ID_INTERPOLATE =
                androidx.compose.remote.core.operations.ColorExpression.COLOR_ID_INTERPOLATE;

        /** ID_ID_INTERPOLATE */
        public static final byte ID_ID_INTERPOLATE =
                androidx.compose.remote.core.operations.ColorExpression.ID_ID_INTERPOLATE;

        /** H S V mode */
        public static final byte HSV_MODE =
                androidx.compose.remote.core.operations.ColorExpression.HSV_MODE;

        /** ARGB mode */
        public static final byte ARGB_MODE =
                androidx.compose.remote.core.operations.ColorExpression.ARGB_MODE;

        /** ARGB mode with a being an id */
        public static final byte IDARGB_MODE =
                androidx.compose.remote.core.operations.ColorExpression.IDARGB_MODE;
    }

    /** Used in ImageScale */
    public static class ImageScale {
        /** Center the image in the view, but perform no scaling. */
        public static final int NONE = ImageScaling.SCALE_NONE;

        /**
         * Scale the image uniformly so that the image will be equal to or less than the
         * corresponding dimension of the view
         */
        public static final int INSIDE = ImageScaling.SCALE_INSIDE;

        /** Scale the image uniformly so that the image width = display width */
        public static final int FILL_WIDTH = ImageScaling.SCALE_FILL_WIDTH;

        /** Scale the image uniformly so that the image height = display height */
        public static final int FILL_HEIGHT = ImageScaling.SCALE_FILL_HEIGHT;

        /** Scale the image uniformly so that the image will fit within the view */
        public static final int FIT = ImageScaling.SCALE_FIT;

        /** Scale the image uniformly so that the image will fill the bounds */
        public static final int CROP = ImageScaling.SCALE_CROP;

        /** non uniform scaling to fit the image within the bounds */
        public static final int FILL_BOUNDS = ImageScaling.SCALE_FILL_BOUNDS;

        /** scale by a fixed factor */
        public static final int FIXED_SCALE = ImageScaling.SCALE_FIXED_SCALE;
    }

    /** Used in TextAnchorMask */
    public static class TextAnchorMask {
        /** text is right to left */
        public static final int TEXT_RTL = DrawTextAnchored.ANCHOR_TEXT_RTL;

        /** text is mono space in measure */
        public static final int MONOSPACE_MEASURE = DrawTextAnchored.ANCHOR_MONOSPACE_MEASURE;

        /** force measure every pass (not recommended) */
        public static final int MEASURE_EVERY_TIME = DrawTextAnchored.MEASURE_EVERY_TIME;
    }

    /** Used in Haptic */
    public static class Haptic {
        /** No haptic feedback should be performed. */
        public static final int NO_HAPTICS = 0;

        /** a long press on an object */
        public static final int LONG_PRESS = 1;

        /** pressed on a virtual on-screen key. */
        public static final int VIRTUAL_KEY = 2;

        /** pressed a soft keyboard key. */
        public static final int KEYBOARD_TAP = 3;

        /** an hour or minute tick of a Clock. */
        public static final int CLOCK_TICK = 4;

        /** a context click on an object. */
        public static final int CONTEXT_CLICK = 5;

        /** a virtual or software keyboard key. */
        public static final int KEYBOARD_PRESS = 6;

        /** released a virtual keyboard key. */
        public static final int KEYBOARD_RELEASE = 7;

        /** The user has released a virtual key. */
        public static final int VIRTUAL_KEY_RELEASE = 8;

        /** performed a selection/insertion handle move on text field. */
        public static final int TEXT_HANDLE_MOVE = 9;

        /** The user has started a gesture (e.g. on the soft keyboard). */
        public static final int GESTURE_START = 10;

        /** finished a gesture (e.g. on the soft keyboard). */
        public static final int GESTURE_END = 11;

        /** confirmation or successful completion of a user interaction. */
        public static final int CONFIRM = 12;

        /** rejection or failure of a user interaction. */
        public static final int REJECT = 13;

        /** The user has toggled a switch or button into the on position. */
        public static final int TOGGLE_ON = 14;

        /** toggled a switch or button into the off position. */
        public static final int TOGGLE_OFF = 15;

        /** a swipe/drag-style gesture passed the threshold */
        public static final int GESTURE_THRESHOLD_ACTIVATE = 16;

        /** a swipe/drag-style gesture cancelled by reversing */
        public static final int GESTURE_THRESHOLD_DEACTIVATE = 17;

        /** a drag-and-drop gesture. The drag target has just been "picked up". */
        public static final int DRAG_START = 18;

        /** switching between a series of potential choices */
        public static final int SEGMENT_TICK = 19;

        /** switching quickly between a series of potential choices */
        public static final int SEGMENT_FREQUENT_TICK = 20;
    }

    /** Used for Time variables */
    public static class Time {
        /** CONTINUOUS_SEC is seconds from midnight looping every hour 0-3600 */
        public static final float CONTINUOUS_SEC = RemoteContext.FLOAT_CONTINUOUS_SEC;

        /** seconds run from Midnight=0 quantized to seconds hour 0..3599 */
        public static final float TIME_IN_SEC = RemoteContext.FLOAT_TIME_IN_SEC;

        /** minutes run from Midnight=0 quantized to minutes 0..1439 */
        public static final float TIME_IN_MIN = RemoteContext.FLOAT_TIME_IN_MIN;

        /** hours run from Midnight=0 quantized to Hours 0-23 */
        public static final float TIME_IN_HR = RemoteContext.FLOAT_TIME_IN_HR;

        /** Moth of Year quantized to MONTHS 1-12. 1 = January */
        public static final float CALENDAR_MONTH = RemoteContext.FLOAT_CALENDAR_MONTH;

        /** DAY OF THE WEEK 1-7. 1 = Monday */
        public static final float WEEK_DAY = RemoteContext.FLOAT_WEEK_DAY;

        /** DAY OF THE MONTH 1-31 */
        public static final float DAY_OF_MONTH = RemoteContext.FLOAT_DAY_OF_MONTH;

        /** ID_OFFSET_TO_UTC is the offset from UTC in sec (typically / 3600f) */
        public static final float OFFSET_TO_UTC = RemoteContext.FLOAT_OFFSET_TO_UTC;

        /** Animation time in seconds */
        public static final float ANIMATION_TIME = RemoteContext.FLOAT_ANIMATION_TIME;

        /** Frame to Frame time changes */
        public static final float ANIMATION_DELTA_TIME = RemoteContext.FLOAT_ANIMATION_DELTA_TIME;

        /** The time in seconds since the epoch. */
        public static final long INT_EPOCH_SECOND = RemoteContext.INT_EPOCH_SECOND;

        /** DAY OF THE YEAR 1-366 */
        public static final float DAY_OF_YEAR = RemoteContext.FLOAT_DAY_OF_YEAR;

        /** The Year e.g. 2025 */
        public static final float YEAR = RemoteContext.FLOAT_YEAR;
    }

    /** Used for System wide variables like font size, window size, api level */
    @SuppressWarnings("JavaLangClash")
    public static class System {
        /** The default font size */
        public static final float FONT_SIZE = RemoteContext.FLOAT_FONT_SIZE;

        /** the width the document is output to */
        public static final float WINDOW_WIDTH = RemoteContext.FLOAT_WINDOW_WIDTH;

        /** the height the document is output to */
        public static final float WINDOW_HEIGHT = RemoteContext.FLOAT_WINDOW_HEIGHT;

        /** When was this player built */
        public static final float API_LEVEL = RemoteContext.FLOAT_API_LEVEL;

        /** The density of the device */
        public static final float DENSITY = RemoteContext.FLOAT_DENSITY;

        /** Path or Bitmap need to be dereferenced */
        public static final int ID_DEREF = PaintOperation.PTR_DEREFERENCE;
    }

    /** Used for Touch variables */
    public static class Touch {
        /** TOUCH_POS_X is the x position of the touch */
        public static final float POSITION_X = RemoteContext.FLOAT_TOUCH_POS_X;

        /** TOUCH_POS_Y is the y position of the touch */
        public static final float POSITION_Y = RemoteContext.FLOAT_TOUCH_POS_Y;

        /** TOUCH_VEL_X is the x velocity of the touch */
        public static final float VELOCITY_X = RemoteContext.FLOAT_TOUCH_VEL_X;

        /** TOUCH_VEL_Y is the x velocity of the touch */
        public static final float VELOCITY_Y = RemoteContext.FLOAT_TOUCH_VEL_Y;

        /** TOUCH_EVENT_TIME the time of the touch */
        public static final float TOUCH_EVENT_TIME = RemoteContext.FLOAT_TOUCH_EVENT_TIME;
    }

    /** Used for Sensor variables */
    public static class Sensor {
        /** X acceleration sensor value in M/s^2 */
        public static final float ACCELERATION_X = RemoteContext.FLOAT_ACCELERATION_X;

        /** Y acceleration sensor value in M/s^2 */
        public static final float ACCELERATION_Y = RemoteContext.FLOAT_ACCELERATION_Y;

        /** Z acceleration sensor value in M/s^2 */
        public static final float ACCELERATION_Z = RemoteContext.FLOAT_ACCELERATION_Z;

        /** X Gyroscope rotation rate sensor value in radians/second */
        public static final float GYRO_ROT_X = RemoteContext.FLOAT_GYRO_ROT_X;

        /** Y Gyroscope rotation rate sensor value in radians/second */
        public static final float GYRO_ROT_Y = RemoteContext.FLOAT_GYRO_ROT_Y;

        /** Z Gyroscope rotation rate sensor value in radians/second */
        public static final float GYRO_ROT_Z = RemoteContext.FLOAT_GYRO_ROT_Z;

        /** Ambient magnetic field in X. sensor value in micro-Tesla (uT) */
        public static final float MAGNETIC_X = RemoteContext.FLOAT_MAGNETIC_X;

        /** Ambient magnetic field in Y. sensor value in micro-Tesla (uT) */
        public static final float MAGNETIC_Y = RemoteContext.FLOAT_MAGNETIC_Y;

        /** Ambient magnetic field in Z. sensor value in micro-Tesla (uT) */
        public static final float MAGNETIC_Z = RemoteContext.FLOAT_MAGNETIC_Z;

        /** Ambient light level in SI lux */
        public static final float LIGHT = RemoteContext.FLOAT_LIGHT;
    }

    /** Use in configuration of RC doc headers */
    public static class DocHeader {

        /** the width of the document */
        public static final short DOC_WIDTH = Header.DOC_WIDTH;

        /** The height of the document */
        public static final short DOC_HEIGHT = Header.DOC_HEIGHT;

        /** The density at generation */
        public static final short DOC_DENSITY_AT_GENERATION = Header.DOC_DENSITY_AT_GENERATION;

        /** The desired FPS for the document */
        public static final short DOC_DESIRED_FPS = Header.DOC_DESIRED_FPS;

        /** The description of the contents of the document */
        public static final short DOC_CONTENT_DESCRIPTION = Header.DOC_CONTENT_DESCRIPTION;

        /** The source of the document */
        public static final short DOC_SOURCE = Header.DOC_SOURCE;

        /** The document is an update to the existing document */
        public static final short DOC_DATA_UPDATE = Header.DOC_DATA_UPDATE;

        /** integer host action id to call if exception occurs */
        public static final short HOST_EXCEPTION_HANDLER = Header.HOST_EXCEPTION_HANDLER;

        /** profiles */
        public static final short DOC_PROFILES = Header.DOC_PROFILES;
    }

    /** Used in accessing attributes of time */
    public static class TimeAttributes {
        /** (value - currentTimeMillis) * 1E-3 */
        public static final short TIME_FROM_NOW_SEC = TimeAttribute.TIME_FROM_NOW_SEC;

        /** (value - currentTimeMillis) * 1E-3 / 60 */
        public static final short TIME_FROM_NOW_MIN = TimeAttribute.TIME_FROM_NOW_MIN;

        /** (value - currentTimeMillis) * 1E-3 / 3600 */
        public static final short TIME_FROM_NOW_HR = TimeAttribute.TIME_FROM_NOW_HR;

        /** (value - arg[0]) * 1E-3 */
        public static final short TIME_FROM_ARG_SEC = TimeAttribute.TIME_FROM_ARG_SEC;

        /** (value - arg[0]) * 1E-3 / 60 */
        public static final short TIME_FROM_ARG_MIN = TimeAttribute.TIME_FROM_ARG_MIN;

        /** (value - arg[0]) * 1E-3 / 3600 */
        public static final short TIME_FROM_ARG_HR = TimeAttribute.TIME_FROM_ARG_HR;

        /** second-of-minute */
        public static final short TIME_IN_SEC = TimeAttribute.TIME_IN_SEC;

        /** minute-of-hour */
        public static final short TIME_IN_MIN = TimeAttribute.TIME_IN_MIN;

        /** hour-of-day */
        public static final short TIME_IN_HR = TimeAttribute.TIME_IN_HR;

        /** day-of-month */
        public static final short TIME_DAY_OF_MONTH = TimeAttribute.TIME_DAY_OF_MONTH;

        /** month-of-year from 0 to 11 */
        public static final short TIME_MONTH_VALUE = TimeAttribute.TIME_MONTH_VALUE;

        /** day-of-week from 0 to 6 */
        public static final short TIME_DAY_OF_WEEK = TimeAttribute.TIME_DAY_OF_WEEK;

        /** the year */
        public static final short TIME_YEAR = TimeAttribute.TIME_YEAR;

        /** (value - doc_load_time) * 1E-3 */
        public static final short TIME_FROM_LOAD_SEC = TimeAttribute.TIME_FROM_LOAD_SEC;
    }

    /** Constants for use in ConditionalOperations */
    public static class Condition {

        /** Equality comparison */
        public static final byte EQ = ConditionalOperations.TYPE_EQ;

        /** Not equal comparison */
        public static final byte NEQ = ConditionalOperations.TYPE_NEQ;

        /** Less than comparison */
        public static final byte LT = ConditionalOperations.TYPE_LT;

        /** Less than or equal comparison */
        public static final byte LTE = ConditionalOperations.TYPE_LTE;

        /** Greater than comparison */
        public static final byte GT = ConditionalOperations.TYPE_GT;

        /** Greater than or equal comparison */
        public static final byte GTE = ConditionalOperations.TYPE_GTE;
    }

    public static class ColorAttribute {
        /** The hue value of the color */
        public static final short HUE = COLOR_HUE;

        /** The saturation value of the color */
        public static final short SATURATION = COLOR_SATURATION;

        /** The brightness value of the color */
        public static final short BRIGHTNESS = COLOR_BRIGHTNESS;

        /** The red value of the color */
        public static final short RED = COLOR_RED;

        /** The green value of the color */
        public static final short GREEN = COLOR_GREEN;

        /** The blue value of the color */
        public static final short BLUE = COLOR_BLUE;

        /** The alpha value of the color */
        public static final short ALPHA = COLOR_ALPHA;
    }

    /** used in TextLayout operations */
    public static class Text {
        /** align the text to the left */
        public static final int ALIGN_LEFT = TextLayout.TEXT_ALIGN_LEFT;

        /** align the text to the right */
        public static final int ALIGN_RIGHT = TextLayout.TEXT_ALIGN_RIGHT;

        /** align the text to the center */
        public static final int ALIGN_CENTER = TextLayout.TEXT_ALIGN_CENTER;

        /** align the text to the justify */
        public static final int ALIGN_JUSTIFY = TextLayout.TEXT_ALIGN_JUSTIFY;

        /** align the text to the start */
        public static final int ALIGN_START = TextLayout.TEXT_ALIGN_START;

        /** align the text to the end */
        public static final int ALIGN_END = TextLayout.TEXT_ALIGN_END;

        /** clip the text on overflow */
        public static final int OVERFLOW_CLIP = TextLayout.OVERFLOW_CLIP;

        /** show the text on overflow */
        public static final int OVERFLOW_VISIBLE = TextLayout.OVERFLOW_VISIBLE;

        /** ellipsis the text on overflow */
        public static final int OVERFLOW_ELLIPSIS = TextLayout.OVERFLOW_ELLIPSIS;

        /** start ellipsis the text on overflow */
        public static final int OVERFLOW_START_ELLIPSIS = TextLayout.OVERFLOW_START_ELLIPSIS;

        /** middle ellipsis the text on overflow */
        public static final int OVERFLOW_MIDDLE_ELLIPSIS = TextLayout.OVERFLOW_MIDDLE_ELLIPSIS;
    }

    /** Used in TextFromFloat */
    public static class TextFromFloat {
        /** pad past point with space */
        public static final int PAD_AFTER_SPACE =
                androidx.compose.remote.core.operations.TextFromFloat.PAD_AFTER_SPACE;

        /** do not pad past last digit */
        public static final int PAD_AFTER_NONE =
                androidx.compose.remote.core.operations.TextFromFloat.PAD_AFTER_NONE;

        /** pad with 0 past last digit */
        public static final int PAD_AFTER_ZERO =
                androidx.compose.remote.core.operations.TextFromFloat.PAD_AFTER_ZERO;

        /** pad before number with spaces */
        public static final int PAD_PRE_SPACE =
                androidx.compose.remote.core.operations.TextFromFloat.PAD_PRE_SPACE;

        /** pad before number with 0s */
        public static final int PAD_PRE_NONE =
                androidx.compose.remote.core.operations.TextFromFloat.PAD_PRE_NONE;

        /** do not pad before number */
        public static final int PAD_PRE_ZERO =
                androidx.compose.remote.core.operations.TextFromFloat.PAD_PRE_ZERO;
    }

    /** Used in Texture */
    public static class Texture {
        public static final short TILE_CLAMP = 0;
        public static final short TILE_MIRROR = 1;
        public static final short TILE_REPEAT = 2;
        public static final short TILE_DECAL = 3;

        public static final short FILTER_DEFAULT = 0;
        public static final short FILTER_NEAREST = 1;
        public static final short FILTER_LINEAR = 2;
    }

    public static class Debug {
        public static final int SHOW_USAGE = DebugMessage.SHOW_USAGE;
    }

    public static class PathExpression {
        public static final int LOOP_PATH = LOOP;
        public static final int MONOTONIC_PATH = MONOTONIC;
        public static final int LINEAR_PATH = LINEAR;
        public static final int POLAR_PATH = POLAR;
    }
}
