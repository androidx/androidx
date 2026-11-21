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
package androidx.compose.remote.core.operations;

// This class duplicates parts of TouchExpression until that class is removed.
// CPD-OFF

import static androidx.compose.remote.core.documentation.DocumentedOperation.FLOAT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.REPEATED_FLOAT;
import static androidx.compose.remote.core.input.PointerInputRouter.EVENT_TYPE_POINTER;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_PASS_MAIN;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_CONSUMED;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_NOT_CONSUMED;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_CANCEL;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_DOWN;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_MOVE;
import static androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_UP;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Limits;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableProvider;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.events.Event;
import androidx.compose.remote.core.events.EventHandler;
import androidx.compose.remote.core.input.MetadataUtil;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.NanMap;
import androidx.compose.remote.core.operations.utilities.touch.VelocityEasing;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Touch expression operation dispatched through the unified pointer input router.
 *
 * <p>Creates a float variable whose value is driven by pointer movement through an RPN
 * expression, combined with deceleration physics, notch snapping and haptic feedback. This is
 * the {@link EventHandler} based replacement for {@link TouchExpression}: it shares the same wire
 * format and opcode, but receives pointer events from the {@code PointerInputRouter} instead of
 * the legacy {@code TouchListener} / {@code CoreDocument.touchDown} path.
 *
 * <p>Differences from {@link TouchExpression}:
 *
 * <ul>
 *   <li>Hit testing is performed by the router; this operation no longer tracks its component's
 *       bounds in window coordinates.
 *   <li>{@code TOUCH_POS_X} / {@code TOUCH_POS_Y} inside the expression are fed from the pointer
 *       event, in coordinates local to the hosting component, instead of being read from the
 *       {@link RemoteContext}.
 *   <li>Pointer events are consumed while a gesture is active, and a down event that has already
 *       been consumed by a descendant is ignored.
 * </ul>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TouchExpressionOperation2 extends Operation
        implements ComponentData, VariableSupport, Serializable, VariableProvider, EventHandler {
    private static final int OP_CODE = Operations.TOUCH_EXPRESSION;
    private static final String CLASS_NAME = "TouchExpression";

    /** Stop with some deceleration */
    public static final int STOP_GENTLY = TouchExpression.STOP_GENTLY;

    /** Stop only at the start or end */
    public static final int STOP_ENDS = TouchExpression.STOP_ENDS;

    /** Stop on touch up */
    public static final int STOP_INSTANTLY = TouchExpression.STOP_INSTANTLY;

    /** Stop at evenly spaced notches */
    public static final int STOP_NOTCHES_EVEN = TouchExpression.STOP_NOTCHES_EVEN;

    /** Stop at a collection points described in percents of the range */
    public static final int STOP_NOTCHES_PERCENTS = TouchExpression.STOP_NOTCHES_PERCENTS;

    /** Stop at a collection of point described in absolute coordinates */
    public static final int STOP_NOTCHES_ABSOLUTE = TouchExpression.STOP_NOTCHES_ABSOLUTE;

    /** Jump to the absolute position of the point */
    public static final int STOP_ABSOLUTE_POS = TouchExpression.STOP_ABSOLUTE_POS;

    /** Stop at evenly spaced single step notches */
    public static final int STOP_NOTCHES_SINGLE_EVEN = TouchExpression.STOP_NOTCHES_SINGLE_EVEN;

    /** Mode in which the output is the change of the expression since the down event. */
    private static final int MODE_DELTA = 0;

    /** Mode in which the output is the absolute value of the expression. */
    private static final int MODE_ABSOLUTE = 1;

    // ---------------------------------------------------------------------------------------
    // Wire format fields
    // ---------------------------------------------------------------------------------------
    private int mId;
    public float @Nullable [] mSrcExp;
    private final float mDefValue;
    private final float mMin;
    private final float mMax;
    private final float mVelocityId;
    private final int mTouchEffects;
    private final int mStopMode;
    private final float @NonNull [] mStopSpec;
    private final float @Nullable [] mEasingSpec;

    // ---------------------------------------------------------------------------------------
    // Derived configuration
    // ---------------------------------------------------------------------------------------
    private final int mMode;
    private final boolean mWrapMode;
    private float mMaxTime = 1;
    private float mMaxAcceleration = 5;
    private float mMaxVelocity = 7;

    // ---------------------------------------------------------------------------------------
    // Resolved (per frame) values
    // ---------------------------------------------------------------------------------------
    private float mOutDefValue;
    private float mOutMin = 1;
    private float mOutMax = 1;
    private float @Nullable [] mOutStopSpec;
    private float @Nullable [] mPreCalcValue;

    // ---------------------------------------------------------------------------------------
    // Gesture state
    // ---------------------------------------------------------------------------------------
    private final AnimatedFloatExpression mExp = new AnimatedFloatExpression();
    private final VelocityEasing mEasyTouch = new VelocityEasing();
    private boolean mUnmodified = true;
    private boolean mTouchDown = false;
    private boolean mEasingToStop = false;
    private float mTouchUpTime = 0;
    private float mCurrentValue = Float.NaN;
    private float mLastValue = 0;
    private float mMaxAtDown = Float.NaN;
    private float mMinAtDown = Float.NaN;
    private float mValueAtDown; // The currently "displayed" value at down
    private float mDownTouchValue; // The calculated value at down

    /** Last pointer position, in coordinates local to the hosting component. */
    private float mTouchX = 0f;

    private float mTouchY = 0f;

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    /**
     * Creates a touch expression.
     *
     * @param id the float id the value is output to
     * @param exp the expression (containing TOUCH_*)
     * @param defValue the default value
     * @param min the minimum value
     * @param max the maximum value
     * @param touchEffects the type of touch mode
     * @param velocityId the velocity (not used)
     * @param stopMode the behaviour on touch up
     * @param stopSpec the parameters that affect the touch up behaviour
     * @param easingSpec the easing parameters for coming to a stop
     */
    public TouchExpressionOperation2(
            int id,
            float @NonNull [] exp,
            float defValue,
            float min,
            float max,
            int touchEffects,
            float velocityId,
            int stopMode,
            float @Nullable [] stopSpec,
            float @Nullable [] easingSpec) {
        mId = id;
        mSrcExp = exp;
        mOutDefValue = mDefValue = defValue;
        mMode = STOP_ABSOLUTE_POS == stopMode ? MODE_ABSOLUTE : MODE_DELTA;
        mOutMax = mMax = max;
        mTouchEffects = touchEffects;
        mVelocityId = velocityId;
        mStopMode = stopMode;
        mStopSpec = stopSpec != null ? stopSpec : new float[0];
        mOutStopSpec = Arrays.copyOf(mStopSpec, mStopSpec.length);
        mEasingSpec = easingSpec;
        if (Float.isNaN(min) && Utils.idFromNan(min) == 0) {
            mWrapMode = true;
            mMin = min;
        } else {
            mWrapMode = false;
            mOutMin = mMin = min;
        }
        if (easingSpec != null && easingSpec.length >= 4) {
            if (Float.floatToRawIntBits(easingSpec[0]) == 0) {
                mMaxTime = easingSpec[1];
                mMaxAcceleration = easingSpec[2];
                mMaxVelocity = easingSpec[3];
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // VariableSupport
    // ---------------------------------------------------------------------------------------

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        if (mPreCalcValue == null || mPreCalcValue.length != mSrcExp.length) {
            mPreCalcValue = new float[mSrcExp.length];
        }
        if (mOutStopSpec == null || mOutStopSpec.length != mStopSpec.length) {
            mOutStopSpec = new float[mStopSpec.length];
        }
        if (Float.isNaN(mMax)) {
            mOutMax = context.getFloat(Utils.idFromNan(mMax));
        }
        if (!mWrapMode && Float.isNaN(mMin)) {
            mOutMin = context.getFloat(Utils.idFromNan(mMin));
        }
        if (Float.isNaN(mDefValue)) {
            mOutDefValue = context.getFloat(Utils.idFromNan(mDefValue));
        }

        for (int i = 0; i < mSrcExp.length; i++) {
            float v = mSrcExp[i];
            if (Float.isNaN(v)
                    && !AnimatedFloatExpression.isMathOperator(v)
                    && !NanMap.isDataVariable(v)) {
                int id = Utils.idFromNan(v);
                if (id == RemoteContext.ID_TOUCH_POS_X) {
                    // Touch positions are fed from the pointer event, in local coordinates.
                    mPreCalcValue[i] = mTouchX;
                } else if (id == RemoteContext.ID_TOUCH_POS_Y) {
                    mPreCalcValue[i] = mTouchY;
                } else {
                    mPreCalcValue[i] = context.getFloat(id);
                }
            } else {
                mPreCalcValue[i] = v;
            }
        }
        for (int i = 0; i < mStopSpec.length; i++) {
            float v = mStopSpec[i];
            if (Float.isNaN(v)) {
                mOutStopSpec[i] = context.getFloat(Utils.idFromNan(v));
            } else {
                mOutStopSpec[i] = v;
            }
        }
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (Float.isNaN(mMax)) {
            context.listensTo(Utils.idFromNan(mMax), this);
        }
        if (!mWrapMode && Float.isNaN(mMin)) {
            context.listensTo(Utils.idFromNan(mMin), this);
        }
        if (Float.isNaN(mDefValue)) {
            context.listensTo(Utils.idFromNan(mDefValue), this);
        }
        for (float v : mSrcExp) {
            if (Float.isNaN(v)
                    && !AnimatedFloatExpression.isMathOperator(v)
                    && !NanMap.isDataVariable(v)) {
                int id = Utils.idFromNan(v);
                // Touch positions come from pointer events, not from context variables.
                if (id != RemoteContext.ID_TOUCH_POS_X && id != RemoteContext.ID_TOUCH_POS_Y) {
                    context.listensTo(id, this);
                }
            }
        }
        for (float v : mStopSpec) {
            if (Float.isNaN(v)) {
                context.listensTo(Utils.idFromNan(v), this);
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // EventHandler
    // ---------------------------------------------------------------------------------------

    /**
     * Returns whether this handler matches the given event type.
     *
     * @param eventType event type identifier
     * @return true if event is a pointer event
     */
    @Override
    public boolean matchesEvent(int eventType) {
        return eventType == EVENT_TYPE_POINTER;
    }

    /**
     * Handles a pointer event and drives the touch expression value.
     *
     * <p>Event data layout: {@code [pointerId, localX, localY, velocityX, velocityY]}.
     *
     * @param context current remote context
     * @param document document containing this operation
     * @param event pointer event being dispatched
     * @return status indicating whether the event was consumed
     */
    @Override
    public int onEvent(
            @NonNull RemoteContext context, @NonNull CoreDocument document, @NonNull Event event) {
        int metadata = event.getMetadata();
        if (MetadataUtil.unpackPass(metadata) != POINTER_PASS_MAIN) {
            return POINTER_STATUS_NOT_CONSUMED;
        }

        float[] data = event.getData();
        if (data == null || data.length < 3) {
            return POINTER_STATUS_NOT_CONSUMED;
        }
        float x = data[1];
        float y = data[2];
        int subtype = MetadataUtil.unpackSubtype(metadata);

        switch (subtype) {
            case POINTER_SUBTYPE_DOWN:
                if (MetadataUtil.unpackConsumed(metadata) == POINTER_STATUS_CONSUMED) {
                    // A descendant already claimed this pointer.
                    return POINTER_STATUS_NOT_CONSUMED;
                }
                onPointerDown(context, x, y);
                return POINTER_STATUS_CONSUMED;
            case POINTER_SUBTYPE_MOVE:
                if (!mTouchDown) {
                    return POINTER_STATUS_NOT_CONSUMED;
                }
                onPointerMove(context, x, y);
                return POINTER_STATUS_CONSUMED;
            case POINTER_SUBTYPE_UP:
                if (!mTouchDown) {
                    return POINTER_STATUS_NOT_CONSUMED;
                }
                float velocityX = data.length > 4 ? data[3] : 0f;
                float velocityY = data.length > 4 ? data[4] : 0f;
                onPointerUp(context, x, y, velocityX, velocityY);
                return POINTER_STATUS_CONSUMED;
            case POINTER_SUBTYPE_CANCEL:
                if (!mTouchDown) {
                    return POINTER_STATUS_NOT_CONSUMED;
                }
                onPointerCancel(context, x, y);
                return POINTER_STATUS_CONSUMED;
            default:
                return POINTER_STATUS_NOT_CONSUMED;
        }
    }

    /**
     * Returns whether a pointer is currently driving this expression.
     *
     * @return true between a pointer down and the matching up / cancel
     */
    public boolean isPointerDown() {
        return mTouchDown;
    }

    /**
     * Starts a gesture at the given position. Stops any in-flight easing and, in delta mode,
     * anchors the output to its current value.
     *
     * <p>Exposed so that hosting operations (e.g. a scroll modifier) can drive the expression
     * directly instead of routing through {@link #onEvent}.
     *
     * @param context current remote context
     * @param x pointer x position, local to the hosting component
     * @param y pointer y position, local to the hosting component
     */
    public void onPointerDown(@NonNull RemoteContext context, float x, float y) {
        setTouchPosition(context, x, y);
        touchDown(context);
    }

    /**
     * Updates the gesture with a new pointer position and recomputes the output value.
     *
     * @param context current remote context
     * @param x pointer x position, local to the hosting component
     * @param y pointer y position, local to the hosting component
     */
    public void onPointerMove(@NonNull RemoteContext context, float x, float y) {
        if (!mTouchDown) {
            return;
        }
        setTouchPosition(context, x, y);
        touchDrag(context);
    }

    /**
     * Ends the gesture and starts easing towards the stop position.
     *
     * @param context current remote context
     * @param x pointer x position, local to the hosting component
     * @param y pointer y position, local to the hosting component
     * @param velocityX pointer x velocity in pixels per second
     * @param velocityY pointer y velocity in pixels per second
     */
    public void onPointerUp(
            @NonNull RemoteContext context, float x, float y, float velocityX, float velocityY) {
        if (!mTouchDown) {
            return;
        }
        setTouchPosition(context, x, y);
        touchUp(context, velocityX, velocityY);
    }

    /**
     * Cancels the gesture; behaves like an up with no velocity.
     *
     * @param context current remote context
     * @param x pointer x position, local to the hosting component
     * @param y pointer y position, local to the hosting component
     */
    public void onPointerCancel(@NonNull RemoteContext context, float x, float y) {
        onPointerUp(context, x, y, 0f, 0f);
    }

    /**
     * Records the pointer position and refreshes the pre-calculated expression inputs.
     *
     * @param context current remote context
     * @param x pointer x position, local to the hosting component
     * @param y pointer y position, local to the hosting component
     */
    private void setTouchPosition(@NonNull RemoteContext context, float x, float y) {
        mTouchX = x;
        mTouchY = y;
        updateVariables(context);
    }

    private void touchDown(@NonNull RemoteContext context) {
        mEasingToStop = false;
        mTouchDown = true;
        mUnmodified = false;
        if (mMode == MODE_DELTA) {
            mValueAtDown = context.getFloat(mId);
            if (STOP_NOTCHES_SINGLE_EVEN == mStopMode) {
                float min = mWrapMode ? 0 : mOutMin;
                int evenSpacing = (int) mOutStopSpec[0];
                float notchMax = (mOutStopSpec.length > 1) ? mOutStopSpec[1] : mOutMax;
                float step = (notchMax - min) / evenSpacing;
                float notch = mValueAtDown;
                mMaxAtDown = notch + step;
                mMinAtDown = notch - step;
            }
            mDownTouchValue = evaluate(context);
        }
        context.needsRepaint();
    }

    private void touchDrag(@NonNull RemoteContext context) {
        apply(context);
        context.needsRepaint();
    }

    /**
     * Ends the gesture and starts easing towards the stop position.
     *
     * @param context current remote context
     * @param velocityX pointer x velocity in pixels per second
     * @param velocityY pointer y velocity in pixels per second
     */
    private void touchUp(@NonNull RemoteContext context, float velocityX, float velocityY) {
        mTouchDown = false;
        if (mStopMode == STOP_INSTANTLY) {
            return;
        }
        // Calculate the slope of the expression with respect to the pointer velocity, using a
        // small displacement along the velocity vector.
        float dt = 0.0001f;
        float v = evaluate(context);
        for (int i = 0; i < mSrcExp.length; i++) {
            if (Float.isNaN(mSrcExp[i])) {
                int id = Utils.idFromNan(mSrcExp[i]);
                if (id == RemoteContext.ID_TOUCH_POS_X) {
                    mPreCalcValue[i] = mTouchX + velocityX * dt;
                } else if (id == RemoteContext.ID_TOUCH_POS_Y) {
                    mPreCalcValue[i] = mTouchY + velocityY * dt;
                }
            }
        }
        float vdt = evaluate(context);
        float slope = (vdt - v) / dt; // the rate of change with respect to the pointer movement
        float value = context.getFloat(mId);

        mTouchUpTime = context.getAnimationTime();

        float dest = getStopPosition(value, slope);
        float time = Math.min(2, mMaxTime * Math.abs(dest - value) / (2 * mMaxVelocity));
        mEasyTouch.config(value, dest, slope, time, mMaxAcceleration, mMaxVelocity, null);
        mEasingToStop = true;
        context.needsRepaint();
    }

    private float evaluate(@NonNull RemoteContext context) {
        return mExp.eval(context.getCollectionsAccess(), mPreCalcValue, mPreCalcValue.length);
    }

    // ---------------------------------------------------------------------------------------
    // Value computation
    // ---------------------------------------------------------------------------------------

    private float wrap(float pos) {
        if (!mWrapMode) {
            return pos;
        }
        pos = pos % mOutMax;
        if (pos < 0) {
            pos += mOutMax;
        }
        return pos;
    }

    private float getStopPosition(float pos, float slope) {
        float target = pos + slope / 2f;
        if (mWrapMode) {
            pos = wrap(pos);
            target = pos + slope / 2f;
        } else {
            target = Math.max(Math.min(target, mOutMax), mOutMin);
        }
        float min = mWrapMode ? 0 : mOutMin;

        switch (mStopMode) {
            case STOP_ENDS:
                return ((pos + slope) > (mOutMax + min) / 2) ? mOutMax : min;
            case STOP_INSTANTLY:
                return pos;
            case STOP_NOTCHES_EVEN:
            case STOP_NOTCHES_SINGLE_EVEN:
                int evenSpacing = (int) mOutStopSpec[0];
                float notchMax = (mOutStopSpec.length > 1) ? mOutStopSpec[1] : mOutMax;
                float step = (notchMax - min) / evenSpacing;
                float notch = min + step * (int) (0.5f + (target - mOutMin) / step);
                if (mStopMode == STOP_NOTCHES_SINGLE_EVEN) {
                    notch = Math.min(mMaxAtDown, notch);
                    notch = Math.max(mMinAtDown, notch);
                }
                if (!mWrapMode) {
                    notch = Math.max(Math.min(notch, mOutMax), min);
                }
                return notch;
            case STOP_NOTCHES_PERCENTS:
                float minPos = min;
                float minPosDist = Math.abs(mOutMin - target);
                for (int i = 0; i < mStopSpec.length; i++) {
                    float p = mOutMin + mStopSpec[i] * (mOutMax - mOutMin);
                    float dist = Math.abs(p - target);
                    if (minPosDist > dist) {
                        minPosDist = dist;
                        minPos = p;
                    }
                }
                return minPos;
            case STOP_NOTCHES_ABSOLUTE:
                minPos = mOutMin;
                minPosDist = Math.abs(mOutMin - target);
                for (int i = 0; i < mStopSpec.length; i++) {
                    float dist = Math.abs(mStopSpec[i] - target);
                    if (minPosDist > dist) {
                        minPosDist = dist;
                        minPos = mStopSpec[i];
                    }
                }
                return minPos;
            case STOP_GENTLY:
            default:
                return target;
        }
    }

    private void haptic(@NonNull RemoteContext context) {
        int touch = (mTouchEffects & 0xFF);
        if ((mTouchEffects & (1 << 15)) != 0) {
            touch = context.getInteger(mTouchEffects & 0x7FFF);
        }
        context.hapticEffect(touch);
    }

    private void crossNotchCheck(@NonNull RemoteContext context) {
        float prev = mLastValue;
        float next = mCurrentValue;
        mLastValue = next;

        float min = mWrapMode ? 0 : mOutMin;
        float max = mOutMax;

        switch (mStopMode) {
            case STOP_ENDS:
                if (((min - prev) * (max - prev) < 0) ^ ((min - next) * (max - next)) < 0) {
                    haptic(context);
                }
                break;
            case STOP_INSTANTLY:
                haptic(context);
                break;
            case STOP_NOTCHES_EVEN:
            case STOP_NOTCHES_SINGLE_EVEN:
                int evenSpacing = (int) mStopSpec[0];
                float step = (max - min) / evenSpacing;
                if ((int) ((prev - min) / step) != (int) ((next - min) / step)) {
                    haptic(context);
                }
                break;
            case STOP_NOTCHES_PERCENTS:
                for (int i = 0; i < mStopSpec.length; i++) {
                    float p = mOutMin + mStopSpec[i] * (mOutMax - mOutMin);
                    if ((prev - p) * (next - p) < 0) {
                        haptic(context);
                    }
                }
                break;
            case STOP_NOTCHES_ABSOLUTE:
                for (int i = 0; i < mStopSpec.length; i++) {
                    float p = mStopSpec[i];
                    if ((prev - p) * (next - p) < 0) {
                        haptic(context);
                    }
                }
                break;
            case STOP_GENTLY:
            default:
                break;
        }
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        if (context.mRemoteComposeState.isFloatOverridden(mId)) {
            // An override (ValueFloatChange, rcDoc variable mapping, host named-variable
            // override) set our output: take it as the new position and resume owning the id.
            if (!mTouchDown) {
                // Refresh min/max first (as a pointer down does): on this path they may still
                // hold the values read before layout, and the adopted value is clamped to them
                // below.
                updateVariables(context);
                mUnmodified = false;
                mEasingToStop = false;
                mCurrentValue = context.getFloat(mId);
            }
            context.mRemoteComposeState.clearFloatOverride(mId);
        }
        if (mUnmodified) {
            mCurrentValue = mOutDefValue;
            context.loadFloat(mId, wrap(mCurrentValue));
            return;
        }
        if (mEasingToStop) {
            float time = context.getAnimationTime() - mTouchUpTime;
            float value = mEasyTouch.getPos(time);
            mCurrentValue = value;
            if (mWrapMode) {
                value = wrap(value);
            } else {
                value = Math.min(Math.max(value, mOutMin), mOutMax);
            }
            context.loadFloat(mId, value);
            if (mEasyTouch.getDuration() < time) {
                mEasingToStop = false;
            }
            crossNotchCheck(context);
            context.needsRepaint();
            return;
        }
        if (mTouchDown) {
            float value = evaluate(context);
            if (mMode == MODE_DELTA) {
                value = mValueAtDown + (value - mDownTouchValue);
            }
            if (mWrapMode) {
                value = wrap(value);
            } else {
                value = Math.min(Math.max(value, mOutMin), mOutMax);
            }
            mCurrentValue = value;
        }
        crossNotchCheck(context);
        if (mStopMode == STOP_NOTCHES_SINGLE_EVEN && !Float.isNaN(mMaxAtDown)) {
            mCurrentValue = Math.min(mMaxAtDown, mCurrentValue);
            mCurrentValue = Math.max(mMinAtDown, mCurrentValue);
        }
        if (!mWrapMode) {
            if (!Float.isNaN(mOutMin)) mCurrentValue = Math.max(mCurrentValue, mOutMin);
            if (!Float.isNaN(mOutMax)) mCurrentValue = Math.min(mCurrentValue, mOutMax);
        }
        context.loadFloat(mId, wrap(mCurrentValue));
    }

    // ---------------------------------------------------------------------------------------
    // Serialization
    // ---------------------------------------------------------------------------------------

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(
                buffer,
                mId,
                mDefValue,
                mMin,
                mMax,
                mVelocityId,
                mTouchEffects,
                mSrcExp,
                mStopMode,
                mStopSpec,
                mEasingSpec);
    }

    @NonNull
    @Override
    public String toString() {
        String[] labels = new String[mSrcExp.length];
        for (int i = 0; i < mSrcExp.length; i++) {
            if (Float.isNaN(mSrcExp[i])) {
                labels[i] = "[" + Utils.idStringFromNan(mSrcExp[i]) + "]";
            }
        }
        float[] values = mPreCalcValue != null ? mPreCalcValue : mSrcExp;
        return CLASS_NAME
                + "["
                + mId
                + "] = ("
                + AnimatedFloatExpression.toString(values, labels)
                + ")";
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("id", mId)
                .add("defValue", mDefValue, mOutDefValue)
                .add("min", mMin, mOutMin)
                .add("max", mMax, mOutMax)
                .add("mode", mMode)
                .addFloatExpressionSrc("srcExp", mSrcExp);
    }

    /** Returns {@code mStopMode}. */
    public int getStopMode() {
        return mStopMode;
    }

    /** Returns {@code mStopSpec}. */
    public float @NonNull [] getStopSpec() {
        return mStopSpec;
    }

    // ===================== static ======================

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return CLASS_NAME;
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return OP_CODE;
    }

    /**
     * Writes out the operation to the buffer. The wire format is identical to {@link
     * TouchExpression}.
     *
     * @param buffer The buffer to write to
     * @param id the id of the resulting float
     * @param value the initial value
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @param velocityId the velocity id
     * @param touchEffects the type touch effect
     * @param exp the expression the maps touch drags to movement
     * @param touchMode the touch mode e.g. notch modes
     * @param touchSpec the spec of the touch modes
     * @param easingSpec the spec of when the object comes to an easing
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int id,
            float value,
            float min,
            float max,
            float velocityId,
            int touchEffects,
            float @NonNull [] exp,
            int touchMode,
            float @Nullable [] touchSpec,
            float @Nullable [] easingSpec) {
        TouchExpression.apply(
                buffer,
                id,
                value,
                min,
                max,
                velocityId,
                touchEffects,
                exp,
                touchMode,
                touchSpec,
                easingSpec);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.readId();
        float startValue = buffer.readNanId();
        float min = buffer.readNanId();
        float max = buffer.readNanId();
        float velocityId = buffer.readNanId(); // TODO future support
        int touchEffects = buffer.readInt();
        int len = buffer.readInt();
        int valueLen = len & 0xFFFF;
        if (valueLen > Limits.MAX_EXPRESSION_SIZE) {
            throw new RuntimeException("Float expression to long");
        }
        float[] exp = new float[valueLen];
        for (int i = 0; i < exp.length; i++) {
            exp[i] = buffer.readNanId();
        }
        int stopLogic = buffer.readInt();
        int stopLen = stopLogic & 0xFFFF;
        int stopMode = stopLogic >> 16;
        if (stopLen > Limits.MAX_TOUCH_STOPS) {
            throw new RuntimeException(
                    "stops too long: " + stopLen + " > " + Limits.MAX_TOUCH_STOPS);
        }
        float[] stopsData = new float[stopLen];
        for (int i = 0; i < stopsData.length; i++) {
            stopsData[i] = buffer.readNanId();
        }
        int easingLen = buffer.readInt();
        if (easingLen > Limits.MAX_EASING_LEN) {
            throw new RuntimeException("Easing spec too long: " + easingLen);
        }
        float[] easingData = new float[easingLen];
        for (int i = 0; i < easingData.length; i++) {
            easingData[i] = buffer.readNanId();
        }

        operations.add(
                new TouchExpressionOperation2(
                        id,
                        exp,
                        startValue,
                        min,
                        max,
                        touchEffects,
                        velocityId,
                        stopMode,
                        stopsData,
                        easingData));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Logic & Expressions Operations", OP_CODE, CLASS_NAME)
                .description("Define a float value derived from touch interactions")
                .field(INT, "id", "The ID of the resulting float variable")
                .field(FLOAT, "value", "The initial value")
                .field(FLOAT, "min", "The minimum allowed value")
                .field(FLOAT, "max", "The maximum allowed value")
                .field(FLOAT, "velocityId", "Reserved for velocity ID")
                .field(INT, "touchEffects", "Haptic feedback and touch behavior flags")
                .field(INT, "expression_length", "The length of the touch mapping expression")
                .field(
                        REPEATED_FLOAT,
                        "expression",
                        "Sequence of floats representing touch mapping (RPN)")
                .field(INT, "stopModeAndLen", "Encoded stop mode and length of stop spec")
                .field(REPEATED_FLOAT, "stopSpec", "Parameters for stop behavior (e.g., notches)")
                .field(INT, "easingLen", "The length of the easing spec")
                .field(REPEATED_FLOAT, "easingSpec", "Parameters for deceleration easing");
    }
}
// CPD-ON
