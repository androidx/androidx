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
import static androidx.compose.remote.core.operations.Skip.SKIP_IF_API_EQUAL_TO;
import static androidx.compose.remote.core.operations.Skip.SKIP_IF_API_GREATER_THAN;
import static androidx.compose.remote.core.operations.Skip.SKIP_IF_API_LESS_THAN;
import static androidx.compose.remote.core.operations.Skip.SKIP_IF_API_NOT_EQUAL_TO;
import static androidx.compose.remote.core.operations.Skip.SKIP_IF_PROFILE_EXCLUDES;
import static androidx.compose.remote.core.operations.Skip.SKIP_IF_PROFILE_INCLUDES;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.ConditionalOperations;
import androidx.compose.remote.core.operations.DebugMessage;
import androidx.compose.remote.core.operations.DrawTextAnchored;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.TimeAttribute;
import androidx.compose.remote.core.operations.TouchExpression;
import androidx.compose.remote.core.operations.layout.managers.TextLayout;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.ImageScaling;
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator;
import androidx.compose.remote.core.operations.utilities.easing.Easing;

/** Constants use in RemoteCompose */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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

        /** cubic Easing function */
        public static final float CUBIC = AnimatedFloatExpression.CUBIC;

        /** monotonic spline that loops function */
        public static final float A_SPLINE_LOOP = AnimatedFloatExpression.A_SPLINE_LOOP;

        /** Change the sign of value x -> -x */
        public static final float CHANGE_SIGN = AnimatedFloatExpression.CHANGE_SIGN;

        /** sum all values to index */
        public static final float A_SUM_UNTIL = AnimatedFloatExpression.A_SUM_TILL;

        /** A_SUM operator */
        public static final float A_SUM_XY = AnimatedFloatExpression.A_SUM_XY;

        /** A_SUM operator */
        public static final float A_SUM_SQR = AnimatedFloatExpression.A_SUM_SQR;

        /** A_SUM operator */
        public static final float A_LERP = AnimatedFloatExpression.A_LERP;
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

        /** cubic Easing function that goes backwards and then accelerates */
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
        /** force measure every pass (not recommended) */
        public static final int BASELINE_RELATIVE = DrawTextAnchored.BASELINE_RELATIVE;
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

        public static float sLightMode = 0;
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
        /** Stop as soon as touch up */
        public static final int STOP_INSTANTLY = TouchExpression.STOP_INSTANTLY;
        /** Jump to the absolute position of the point */
        public static final int STOP_ABSOLUTE_POS = TouchExpression.STOP_ABSOLUTE_POS;
        /** Stop only at the start or end */
        public static final int STOP_ENDS = TouchExpression.STOP_ENDS;
        /** Stop at a series of notch positions expressed as a percent of the range */
        public static final int STOP_NOTCHES_PERCENTS = TouchExpression.STOP_NOTCHES_PERCENTS;
        /** Stop by decelerating */
        public static final int STOP_GENTLY = TouchExpression.STOP_GENTLY;
        /** Stop at a collection of point described in absolute cordnates */
        public static final int STOP_NOTCHES_ABSOLUTE = TouchExpression.STOP_NOTCHES_ABSOLUTE;
        /** Stop at a series of evenly spaced notches */
        public static final int STOP_NOTCHES_EVEN = TouchExpression.STOP_NOTCHES_EVEN;
        /** Stop at evenly spaced single step notches */
        public static final int STOP_NOTCHES_SINGLE_EVEN =
                TouchExpression.STOP_NOTCHES_SINGLE_EVEN;

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

        /** The day of the year */
        public static final short TIME_DAY_OF_YEAR = TimeAttribute.TIME_DAY_OF_YEAR;
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

        /** default e.g.  e.g. 1234567890.12 */
        public static final int GROUPING_NONE =
                androidx.compose.remote.core.operations.TextFromFloat.GROUPING_NONE;

        /** by 3 digits e.g. 123,456,789.01 */
        public static final int GROUPING_BY3 =
                androidx.compose.remote.core.operations.TextFromFloat.GROUPING_BY3;

        /** by 4 digits e.g. 12,3456,7890.12 */
        public static final int GROUPING_BY4 =
                androidx.compose.remote.core.operations.TextFromFloat.GROUPING_BY4;

        /** by 3 then 2 digits e.g. 1,23,45,67,890.12 */
        public static final int GROUPING_BY32 =
                androidx.compose.remote.core.operations.TextFromFloat.GROUPING_BY32;

        /** use comma as separator e.g. 123,456,789.01 */
        public static final int SEPARATOR_PERIOD_COMMA =
                androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_PERIOD_COMMA;

        /** use period as separator e.g. 123.456.789,01 */
        public static final int SEPARATOR_COMMA_PERIOD =
                androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_COMMA_PERIOD;

        /** use space as separator e.g. 123 456 789,01 */
        public static final int SEPARATOR_SPACE_COMMA =
                androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_SPACE_COMMA;

        /** use space as separator e.g. 123_456_789,01 */
        public static final int SEPARATOR_UNDER_PERIOD =
                androidx.compose.remote.core.operations.TextFromFloat.SEPARATOR_UNDER_PERIOD;

        /** no options */
        public static final int OPTIONS_NONE =
                androidx.compose.remote.core.operations.TextFromFloat.OPTIONS_NONE;

        /** use parentheses as negative sign e.g. (1234567890.12) */
        public static final int OPTIONS_NEGATIVE_PARENTHESES =
                androidx.compose.remote.core.operations.TextFromFloat.OPTIONS_NEGATIVE_PARENTHESES;

        /** round do not truncate (if time typically you do not do this) */
        public static final int OPTIONS_ROUNDING =
                androidx.compose.remote.core.operations.TextFromFloat.OPTIONS_ROUNDING;

        /** Legacy compatibility mode grouping & separator ignored */
        public static final int LEGACY_MODE =
                androidx.compose.remote.core.operations.TextFromFloat.LEGACY_MODE;

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
        public static final int SPLINE_PATH = 0;
        public static final int LOOP_PATH = LOOP;
        public static final int MONOTONIC_PATH = MONOTONIC;
        public static final int LINEAR_PATH = LINEAR;
        public static final int POLAR_PATH = POLAR;
    }

    public static class Layout {
        public static final float FIRST_BASELINE = RemoteContext.FIRST_BASELINE;
        public static final float LAST_BASELINE = RemoteContext.LAST_BASELINE;
    }

    public static class PathEffect {
        public static final int PATH_DASH_TRANSLATE = 0;
        public static final int PATH_DASH_ROTATE = 1;
        public static final int PATH_DASH_MORPH = 2;
    }

    public static class Theme {
        /** region of code is run only in dark mode */
        public static final int DARK = androidx.compose.remote.core.operations.Theme.DARK;
        /** region of code is run only in light mode */
        public static final int LIGHT = androidx.compose.remote.core.operations.Theme.LIGHT;
        /** region of code is run in any mode */
        public static final int UNSPECIFIED =
                androidx.compose.remote.core.operations.Theme.UNSPECIFIED;
    }

    public static class TextAttribute {
        public static final short MEASURE_WIDTH =
                androidx.compose.remote.core.operations.TextAttribute.MEASURE_WIDTH;
        public static final short MEASURE_HEIGHT =
                androidx.compose.remote.core.operations.TextAttribute.MEASURE_HEIGHT;
        public static final short MEASURE_LEFT =
                androidx.compose.remote.core.operations.TextAttribute.MEASURE_LEFT;
        public static final short MEASURE_RIGHT =
                androidx.compose.remote.core.operations.TextAttribute.MEASURE_RIGHT;
        public static final short MEASURE_TOP =
                androidx.compose.remote.core.operations.TextAttribute.MEASURE_TOP;
        public static final short MEASURE_BOTTOM =
                androidx.compose.remote.core.operations.TextAttribute.MEASURE_BOTTOM;
        public static final short TEXT_LENGTH =
                androidx.compose.remote.core.operations.TextAttribute.TEXT_LENGTH;
    }

    /**
     * defining standard system color identifiers.
     */
    public static final class AndroidColors {
        /** The android color group name. */
        public static final String GROUP = "android";

        /** The dark background color identifier. */
        public static final short BACKGROUND_DARK = 0;

        /** The light background color identifier. */
        public static final short BACKGROUND_LIGHT = 1;

        /** The black color identifier. */
        public static final short BLACK = 2;

        /** The darker gray color identifier. */
        public static final short DARKER_GRAY = 3;

        /** The bright holo blue color identifier. */
        public static final short HOLO_BLUE_BRIGHT = 4;

        /** The dark holo blue color identifier. */
        public static final short HOLO_BLUE_DARK = 5;

        /** The light holo blue color identifier. */
        public static final short HOLO_BLUE_LIGHT = 6;

        /** The dark holo green color identifier. */
        public static final short HOLO_GREEN_DARK = 7;

        /** The light holo green color identifier. */
        public static final short HOLO_GREEN_LIGHT = 8;

        /** The dark holo orange color identifier. */
        public static final short HOLO_ORANGE_DARK = 9;

        /** The light holo orange color identifier. */
        public static final short HOLO_ORANGE_LIGHT = 10;

        /** The holo purple color identifier. */
        public static final short HOLO_PURPLE = 11;

        /** The dark holo red color identifier. */
        public static final short HOLO_RED_DARK = 12;

        /** The light holo red color identifier. */
        public static final short HOLO_RED_LIGHT = 13;

        /** The system accent14 0 color identifier. */
        public static final short SYSTEM_ACCENT14_0 = 14;

        /** The system accent15 150 color identifier. */
        public static final short SYSTEM_ACCENT15_150 = 15;

        /** The system accent16 1600 color identifier. */
        public static final short SYSTEM_ACCENT16_1600 = 16;

        /** The system accent17 17000 color identifier. */
        public static final short SYSTEM_ACCENT17_17000 = 17;

        /** The system accent18 200 color identifier. */
        public static final short SYSTEM_ACCENT18_200 = 18;

        /** The system accent19 300 color identifier. */
        public static final short SYSTEM_ACCENT19_300 = 19;

        /** The system accent20 400 color identifier. */
        public static final short SYSTEM_ACCENT20_400 = 20;

        /** The system accent21 50 color identifier. */
        public static final short SYSTEM_ACCENT21_50 = 21;

        /** The system accent22 500 color identifier. */
        public static final short SYSTEM_ACCENT22_500 = 22;

        /** The system accent23 600 color identifier. */
        public static final short SYSTEM_ACCENT23_600 = 23;

        /** The system accent24 700 color identifier. */
        public static final short SYSTEM_ACCENT24_700 = 24;

        /** The system accent25 800 color identifier. */
        public static final short SYSTEM_ACCENT25_800 = 25;

        /** The system accent26 900 color identifier. */
        public static final short SYSTEM_ACCENT26_900 = 26;

        /** The system accent2 0 color identifier. */
        public static final short SYSTEM_ACCENT2_0 = 27;

        /** The system accent2 280 color identifier. */
        public static final short SYSTEM_ACCENT2_280 = 28;

        /** The system accent2 2900 color identifier. */
        public static final short SYSTEM_ACCENT2_2900 = 29;

        /** The system accent2 30000 color identifier. */
        public static final short SYSTEM_ACCENT2_30000 = 30;

        /** The system accent2 200 color identifier. */
        public static final short SYSTEM_ACCENT2_200 = 31;

        /** The system accent2 300 color identifier. */
        public static final short SYSTEM_ACCENT2_300 = 32;

        /** The system accent2 400 color identifier. */
        public static final short SYSTEM_ACCENT2_400 = 33;

        /** The system accent2 50 color identifier. */
        public static final short SYSTEM_ACCENT2_50 = 34;

        /** The system accent2 500 color identifier. */
        public static final short SYSTEM_ACCENT2_500 = 35;

        /** The system accent2 600 color identifier. */
        public static final short SYSTEM_ACCENT2_600 = 36;

        /** The system accent2 700 color identifier. */
        public static final short SYSTEM_ACCENT2_700 = 37;

        /** The system accent2 800 color identifier. */
        public static final short SYSTEM_ACCENT2_800 = 38;

        /** The system accent2 900 color identifier. */
        public static final short SYSTEM_ACCENT2_900 = 39;

        /** The system accent3 0 color identifier. */
        public static final short SYSTEM_ACCENT3_0 = 40;

        /** The system accent3 410 color identifier. */
        public static final short SYSTEM_ACCENT3_410 = 41;

        /** The system accent3 4200 color identifier. */
        public static final short SYSTEM_ACCENT3_4200 = 42;

        /** The system accent3 43000 color identifier. */
        public static final short SYSTEM_ACCENT3_43000 = 43;

        /** The system accent3 200 color identifier. */
        public static final short SYSTEM_ACCENT3_200 = 44;

        /** The system accent3 300 color identifier. */
        public static final short SYSTEM_ACCENT3_300 = 45;

        /** The system accent3 400 color identifier. */
        public static final short SYSTEM_ACCENT3_400 = 46;

        /** The system accent3 50 color identifier. */
        public static final short SYSTEM_ACCENT3_50 = 47;

        /** The system accent3 500 color identifier. */
        public static final short SYSTEM_ACCENT3_500 = 48;

        /** The system accent3 600 color identifier. */
        public static final short SYSTEM_ACCENT3_600 = 49;

        /** The system accent3 700 color identifier. */
        public static final short SYSTEM_ACCENT3_700 = 50;

        /** The system accent3 800 color identifier. */
        public static final short SYSTEM_ACCENT3_800 = 51;

        /** The system accent3 900 color identifier. */
        public static final short SYSTEM_ACCENT3_900 = 52;

        /** The dark system background color identifier. */
        public static final short SYSTEM_BACKGROUND_DARK = 53;

        /** The light system background color identifier. */
        public static final short SYSTEM_BACKGROUND_LIGHT = 54;

        /** The dark system control activated color identifier. */
        public static final short SYSTEM_CONTROL_ACTIVATED_DARK = 55;

        /** The light system control activated color identifier. */
        public static final short SYSTEM_CONTROL_ACTIVATED_LIGHT = 56;

        /** The dark system control highlight color identifier. */
        public static final short SYSTEM_CONTROL_HIGHLIGHT_DARK = 57;

        /** The light system control highlight color identifier. */
        public static final short SYSTEM_CONTROL_HIGHLIGHT_LIGHT = 58;

        /** The dark system control normal color identifier. */
        public static final short SYSTEM_CONTROL_NORMAL_DARK = 59;

        /** The light system control normal color identifier. */
        public static final short SYSTEM_CONTROL_NORMAL_LIGHT = 60;

        /** The system error 0 color identifier. */
        public static final short SYSTEM_ERROR_0 = 61;

        /** The system error 620 color identifier. */
        public static final short SYSTEM_ERROR_620 = 62;

        /** The system error 6300 color identifier. */
        public static final short SYSTEM_ERROR_6300 = 63;

        /** The system error 64000 color identifier. */
        public static final short SYSTEM_ERROR_64000 = 64;

        /** The system error 200 color identifier. */
        public static final short SYSTEM_ERROR_200 = 65;

        /** The system error 300 color identifier. */
        public static final short SYSTEM_ERROR_300 = 66;

        /** The system error 400 color identifier. */
        public static final short SYSTEM_ERROR_400 = 67;

        /** The system error 50 color identifier. */
        public static final short SYSTEM_ERROR_50 = 68;

        /** The system error 500 color identifier. */
        public static final short SYSTEM_ERROR_500 = 69;

        /** The system error 600 color identifier. */
        public static final short SYSTEM_ERROR_600 = 70;

        /** The system error 700 color identifier. */
        public static final short SYSTEM_ERROR_700 = 71;

        /** The system error 800 color identifier. */
        public static final short SYSTEM_ERROR_800 = 72;

        /** The system error 900 color identifier. */
        public static final short SYSTEM_ERROR_900 = 73;

        /** The dark system error container color identifier. */
        public static final short SYSTEM_ERROR_CONTAINER_DARK = 74;

        /** The light system error container color identifier. */
        public static final short SYSTEM_ERROR_CONTAINER_LIGHT = 75;

        /** The dark system error color identifier. */
        public static final short SYSTEM_ERROR_DARK = 76;

        /** The light system error color identifier. */
        public static final short SYSTEM_ERROR_LIGHT = 77;

        /** The system neutral78 0 color identifier. */
        public static final short SYSTEM_NEUTRAL78_0 = 78;

        /** The system neutral79 790 color identifier. */
        public static final short SYSTEM_NEUTRAL79_790 = 79;

        /** The system neutral80 8000 color identifier. */
        public static final short SYSTEM_NEUTRAL80_8000 = 80;

        /** The system neutral81 81000 color identifier. */
        public static final short SYSTEM_NEUTRAL81_81000 = 81;

        /** The system neutral82 200 color identifier. */
        public static final short SYSTEM_NEUTRAL82_200 = 82;

        /** The system neutral83 300 color identifier. */
        public static final short SYSTEM_NEUTRAL83_300 = 83;

        /** The system neutral84 400 color identifier. */
        public static final short SYSTEM_NEUTRAL84_400 = 84;

        /** The system neutral85 50 color identifier. */
        public static final short SYSTEM_NEUTRAL85_50 = 85;

        /** The system neutral86 500 color identifier. */
        public static final short SYSTEM_NEUTRAL86_500 = 86;

        /** The system neutral87 600 color identifier. */
        public static final short SYSTEM_NEUTRAL87_600 = 87;

        /** The system neutral88 700 color identifier. */
        public static final short SYSTEM_NEUTRAL88_700 = 88;

        /** The system neutral89 800 color identifier. */
        public static final short SYSTEM_NEUTRAL89_800 = 89;

        /** The system neutral90 900 color identifier. */
        public static final short SYSTEM_NEUTRAL90_900 = 90;

        /** The system neutral2 0 color identifier. */
        public static final short SYSTEM_NEUTRAL2_0 = 91;

        /** The system neutral2 920 color identifier. */
        public static final short SYSTEM_NEUTRAL2_920 = 92;

        /** The system neutral2 9300 color identifier. */
        public static final short SYSTEM_NEUTRAL2_9300 = 93;

        /** The system neutral2 94000 color identifier. */
        public static final short SYSTEM_NEUTRAL2_94000 = 94;

        /** The system neutral2 200 color identifier. */
        public static final short SYSTEM_NEUTRAL2_200 = 95;

        /** The system neutral2 300 color identifier. */
        public static final short SYSTEM_NEUTRAL2_300 = 96;

        /** The system neutral2 400 color identifier. */
        public static final short SYSTEM_NEUTRAL2_400 = 97;

        /** The system neutral2 50 color identifier. */
        public static final short SYSTEM_NEUTRAL2_50 = 98;

        /** The system neutral2 500 color identifier. */
        public static final short SYSTEM_NEUTRAL2_500 = 99;

        /** The system neutral2 600 color identifier. */
        public static final short SYSTEM_NEUTRAL2_600 = 100;

        /** The system neutral2 700 color identifier. */
        public static final short SYSTEM_NEUTRAL2_700 = 101;

        /** The system neutral2 800 color identifier. */
        public static final short SYSTEM_NEUTRAL2_800 = 102;

        /** The system neutral2 900 color identifier. */
        public static final short SYSTEM_NEUTRAL2_900 = 103;

        /** The dark system on background color identifier. */
        public static final short SYSTEM_ON_BACKGROUND_DARK = 104;

        /** The light system on background color identifier. */
        public static final short SYSTEM_ON_BACKGROUND_LIGHT = 105;

        /** The dark system on error container color identifier. */
        public static final short SYSTEM_ON_ERROR_CONTAINER_DARK = 106;

        /** The light system on error container color identifier. */
        public static final short SYSTEM_ON_ERROR_CONTAINER_LIGHT = 107;

        /** The dark system on error color identifier. */
        public static final short SYSTEM_ON_ERROR_DARK = 108;

        /** The light system on error color identifier. */
        public static final short SYSTEM_ON_ERROR_LIGHT = 109;

        /** The dark system on primary container color identifier. */
        public static final short SYSTEM_ON_PRIMARY_CONTAINER_DARK = 110;

        /** The light system on primary container color identifier. */
        public static final short SYSTEM_ON_PRIMARY_CONTAINER_LIGHT = 111;

        /** The dark system on primary color identifier. */
        public static final short SYSTEM_ON_PRIMARY_DARK = 112;

        /** The system on primary fixed color identifier. */
        public static final short SYSTEM_ON_PRIMARY_FIXED = 113;

        /** The system on primary fixed variant color identifier. */
        public static final short SYSTEM_ON_PRIMARY_FIXED_VARIANT = 114;

        /** The light system on primary color identifier. */
        public static final short SYSTEM_ON_PRIMARY_LIGHT = 115;

        /** The dark system on secondary container color identifier. */
        public static final short SYSTEM_ON_SECONDARY_CONTAINER_DARK = 116;

        /** The light system on secondary container color identifier. */
        public static final short SYSTEM_ON_SECONDARY_CONTAINER_LIGHT = 117;

        /** The dark system on secondary color identifier. */
        public static final short SYSTEM_ON_SECONDARY_DARK = 118;

        /** The system on secondary fixed color identifier. */
        public static final short SYSTEM_ON_SECONDARY_FIXED = 119;

        /** The system on secondary fixed variant color identifier. */
        public static final short SYSTEM_ON_SECONDARY_FIXED_VARIANT = 120;

        /** The light system on secondary color identifier. */
        public static final short SYSTEM_ON_SECONDARY_LIGHT = 121;

        /** The dark system on surface color identifier. */
        public static final short SYSTEM_ON_SURFACE_DARK = 122;

        /** The system on surface disabled color identifier. */
        public static final short SYSTEM_ON_SURFACE_DISABLED = 123;

        /** The light system on surface color identifier. */
        public static final short SYSTEM_ON_SURFACE_LIGHT = 124;

        /** The dark system on surface variant color identifier. */
        public static final short SYSTEM_ON_SURFACE_VARIANT_DARK = 125;

        /** The light system on surface variant color identifier. */
        public static final short SYSTEM_ON_SURFACE_VARIANT_LIGHT = 126;

        /** The dark system on tertiary container color identifier. */
        public static final short SYSTEM_ON_TERTIARY_CONTAINER_DARK = 127;

        /** The light system on tertiary container color identifier. */
        public static final short SYSTEM_ON_TERTIARY_CONTAINER_LIGHT = 128;

        /** The dark system on tertiary color identifier. */
        public static final short SYSTEM_ON_TERTIARY_DARK = 129;

        /** The system on tertiary fixed color identifier. */
        public static final short SYSTEM_ON_TERTIARY_FIXED = 130;

        /** The system on tertiary fixed variant color identifier. */
        public static final short SYSTEM_ON_TERTIARY_FIXED_VARIANT = 131;

        /** The light system on tertiary color identifier. */
        public static final short SYSTEM_ON_TERTIARY_LIGHT = 132;

        /** The dark system outline color identifier. */
        public static final short SYSTEM_OUTLINE_DARK = 133;

        /** The system outline disabled color identifier. */
        public static final short SYSTEM_OUTLINE_DISABLED = 134;

        /** The light system outline color identifier. */
        public static final short SYSTEM_OUTLINE_LIGHT = 135;

        /** The dark system outline variant color identifier. */
        public static final short SYSTEM_OUTLINE_VARIANT_DARK = 136;

        /** The light system outline variant color identifier. */
        public static final short SYSTEM_OUTLINE_VARIANT_LIGHT = 137;

        /** The dark system palette key color neutral color identifier. */
        public static final short SYSTEM_PALETTE_KEY_COLOR_NEUTRAL_DARK = 138;

        /** The light system palette key color neutral color identifier. */
        public static final short SYSTEM_PALETTE_KEY_COLOR_NEUTRAL_LIGHT = 139;

        /** The dark system palette key color neutral variant color identifier. */
        public static final short SYSTEM_PALETTE_KEY_COLOR_NEUTRAL_VARIANT_DARK = 140;

        /** The light system palette key color neutral variant color identifier. */
        public static final short SYSTEM_PALETTE_KEY_COLOR_NEUTRAL_VARIANT_LIGHT = 141;

        /** The dark system palette key color primary color identifier. */
        public static final short SYSTEM_PALETTE_KEY_COLOR_PRIMARY_DARK = 142;

        /** The light system palette key color primary color identifier. */
        public static final short SYSTEM_PALETTE_KEY_COLOR_PRIMARY_LIGHT = 143;

        /** The dark system palette key color secondary color identifier. */
        public static final short SYSTEM_PALETTE_KEY_COLOR_SECONDARY_DARK = 144;

        /** The light system palette key color secondary color identifier. */
        public static final short SYSTEM_PALETTE_KEY_COLOR_SECONDARY_LIGHT = 145;

        /** The dark system palette key color tertiary color identifier. */
        public static final short SYSTEM_PALETTE_KEY_COLOR_TERTIARY_DARK = 146;

        /** The light system palette key color tertiary color identifier. */
        public static final short SYSTEM_PALETTE_KEY_COLOR_TERTIARY_LIGHT = 147;

        /** The dark system primary container color identifier. */
        public static final short SYSTEM_PRIMARY_CONTAINER_DARK = 148;

        /** The light system primary container color identifier. */
        public static final short SYSTEM_PRIMARY_CONTAINER_LIGHT = 149;

        /** The dark system primary color identifier. */
        public static final short SYSTEM_PRIMARY_DARK = 150;

        /** The system primary fixed color identifier. */
        public static final short SYSTEM_PRIMARY_FIXED = 151;

        /** The system primary fixed dim color identifier. */
        public static final short SYSTEM_PRIMARY_FIXED_DIM = 152;

        /** The light system primary color identifier. */
        public static final short SYSTEM_PRIMARY_LIGHT = 153;

        /** The dark system secondary container color identifier. */
        public static final short SYSTEM_SECONDARY_CONTAINER_DARK = 154;

        /** The light system secondary container color identifier. */
        public static final short SYSTEM_SECONDARY_CONTAINER_LIGHT = 155;

        /** The dark system secondary color identifier. */
        public static final short SYSTEM_SECONDARY_DARK = 156;

        /** The system secondary fixed color identifier. */
        public static final short SYSTEM_SECONDARY_FIXED = 157;

        /** The system secondary fixed dim color identifier. */
        public static final short SYSTEM_SECONDARY_FIXED_DIM = 158;

        /** The light system secondary color identifier. */
        public static final short SYSTEM_SECONDARY_LIGHT = 159;

        /** The dark system surface bright color identifier. */
        public static final short SYSTEM_SURFACE_BRIGHT_DARK = 160;

        /** The light system surface bright color identifier. */
        public static final short SYSTEM_SURFACE_BRIGHT_LIGHT = 161;

        /** The dark system surface container color identifier. */
        public static final short SYSTEM_SURFACE_CONTAINER_DARK = 162;

        /** The dark system surface container high color identifier. */
        public static final short SYSTEM_SURFACE_CONTAINER_HIGH_DARK = 163;

        /** The light system surface container high color identifier. */
        public static final short SYSTEM_SURFACE_CONTAINER_HIGH_LIGHT = 164;

        /** The dark system surface container highest color identifier. */
        public static final short SYSTEM_SURFACE_CONTAINER_HIGHEST_DARK = 165;

        /** The light system surface container highest color identifier. */
        public static final short SYSTEM_SURFACE_CONTAINER_HIGHEST_LIGHT = 166;

        /** The light system surface container color identifier. */
        public static final short SYSTEM_SURFACE_CONTAINER_LIGHT = 167;

        /** The dark system surface container low color identifier. */
        public static final short SYSTEM_SURFACE_CONTAINER_LOW_DARK = 168;

        /** The light system surface container low color identifier. */
        public static final short SYSTEM_SURFACE_CONTAINER_LOW_LIGHT = 169;

        /** The dark system surface container lowest color identifier. */
        public static final short SYSTEM_SURFACE_CONTAINER_LOWEST_DARK = 170;

        /** The light system surface container lowest color identifier. */
        public static final short SYSTEM_SURFACE_CONTAINER_LOWEST_LIGHT = 171;

        /** The dark system surface color identifier. */
        public static final short SYSTEM_SURFACE_DARK = 172;

        /** The dark system surface dim color identifier. */
        public static final short SYSTEM_SURFACE_DIM_DARK = 173;

        /** The light system surface dim color identifier. */
        public static final short SYSTEM_SURFACE_DIM_LIGHT = 174;

        /** The system surface disabled color identifier. */
        public static final short SYSTEM_SURFACE_DISABLED = 175;

        /** The light system surface color identifier. */
        public static final short SYSTEM_SURFACE_LIGHT = 176;

        /** The dark system surface variant color identifier. */
        public static final short SYSTEM_SURFACE_VARIANT_DARK = 177;

        /** The light system surface variant color identifier. */
        public static final short SYSTEM_SURFACE_VARIANT_LIGHT = 178;

        /** The dark system tertiary container color identifier. */
        public static final short SYSTEM_TERTIARY_CONTAINER_DARK = 179;

        /** The light system tertiary container color identifier. */
        public static final short SYSTEM_TERTIARY_CONTAINER_LIGHT = 180;

        /** The dark system tertiary color identifier. */
        public static final short SYSTEM_TERTIARY_DARK = 181;

        /** The system tertiary fixed color identifier. */
        public static final short SYSTEM_TERTIARY_FIXED = 182;

        /** The system tertiary fixed dim color identifier. */
        public static final short SYSTEM_TERTIARY_FIXED_DIM = 183;

        /** The light system tertiary color identifier. */
        public static final short SYSTEM_TERTIARY_LIGHT = 184;

        /** The dark system text hint inverse color identifier. */
        public static final short SYSTEM_TEXT_HINT_INVERSE_DARK = 185;

        /** The light system text hint inverse color identifier. */
        public static final short SYSTEM_TEXT_HINT_INVERSE_LIGHT = 186;

        /** The dark system text primary inverse color identifier. */
        public static final short SYSTEM_TEXT_PRIMARY_INVERSE_DARK = 187;

        /** The dark system text primary inverse disable only color identifier. */
        public static final short SYSTEM_TEXT_PRIMARY_INVERSE_DISABLE_ONLY_DARK = 188;

        /** The light system text primary inverse disable only color identifier. */
        public static final short SYSTEM_TEXT_PRIMARY_INVERSE_DISABLE_ONLY_LIGHT = 189;

        /** The light system text primary inverse color identifier. */
        public static final short SYSTEM_TEXT_PRIMARY_INVERSE_LIGHT = 190;

        /** The dark system text secondary and tertiary inverse color identifier. */
        public static final short SYSTEM_TEXT_SECONDARY_AND_TERTIARY_INVERSE_DARK = 191;

        /** The dark system text secondary and tertiary inverse disabled color identifier. */
        public static final short SYSTEM_TEXT_SECONDARY_AND_TERTIARY_INVERSE_DISABLED_DARK = 192;

        /** The light system text secondary and tertiary inverse disabled color identifier. */
        public static final short SYSTEM_TEXT_SECONDARY_AND_TERTIARY_INVERSE_DISABLED_LIGHT = 193;

        /** The light system text secondary and tertiary inverse color identifier. */
        public static final short SYSTEM_TEXT_SECONDARY_AND_TERTIARY_INVERSE_LIGHT = 194;

        /** The tab indicator text color identifier. */
        public static final short TAB_INDICATOR_TEXT = 195;
    }

    public static final class Skip {
        /** skip if the API level is less than the specified value */
        public static final short IF_API_LESS_THAN = SKIP_IF_API_LESS_THAN;

        /** skip if the API level is greater than or equal to the specified value */
        public static final short IF_API_GREATER_THAN = SKIP_IF_API_GREATER_THAN;

        /** skip if the API level is equal to the specified value */
        public static final short IF_API_EQUAL_TO = SKIP_IF_API_EQUAL_TO;

        /** skip if the API level is not equal to the specified value */
        public static final short IF_API_NOT_EQUAL_TO = SKIP_IF_API_NOT_EQUAL_TO;

        /** skip if the profile includes the specified value */
        public static final short IF_PROFILE_INCLUDES = SKIP_IF_PROFILE_INCLUDES;

        /** skip if the profile excludes the specified value */
        public static final short IF_PROFILE_EXCLUDES = SKIP_IF_PROFILE_EXCLUDES;
    }

}
