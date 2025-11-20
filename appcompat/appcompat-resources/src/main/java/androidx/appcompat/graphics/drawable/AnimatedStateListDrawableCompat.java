/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.appcompat.graphics.drawable;

import static androidx.core.content.res.TypedArrayUtils.obtainAttributes;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.StateSet;
import android.util.Xml;

import androidx.annotation.DrawableRes;
import androidx.appcompat.resources.R;
import androidx.appcompat.widget.ResourceManagerInternal;
import androidx.collection.LongSparseArray;
import androidx.collection.SparseArrayCompat;
import androidx.core.graphics.drawable.TintAwareDrawable;
import androidx.core.util.ObjectsCompat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * A {@link Drawable} providing animated transitions between states.
 *
 * <p>A port of {@link android.graphics.drawable.AnimatedStateListDrawable} compatible with older
 * versions of the platform.
 *
 * <p>This drawable can be defined in an XML file with the <code>
 * &lt;animated-selector></code> element. Each keyframe Drawable is defined in a
 * nested <code>&lt;item></code> element. Transitions are defined in a nested
 * <code>&lt;transition></code> element.
 *
 * <p>Notable exceptions not supported by this class:
 *
 * <ul>
 * <li><code>drawable</code>s defined as children of <code>&lt;item></code>s or
 * <code>&lt;transition></code>s (<b>except</b> vectors) ignore theme attributes prior to API level
 * 21</li>
 * <li>Animated vector transitions do not support reversing prior to API level 24</li>
 * </ul>
 *
 * {@link android.R.attr#state_focused}
 * {@link android.R.attr#state_window_focused}
 * {@link android.R.attr#state_enabled}
 * {@link android.R.attr#state_checkable}
 * {@link android.R.attr#state_checked}
 * {@link android.R.attr#state_selected}
 * {@link android.R.attr#state_activated}
 * {@link android.R.attr#state_active}
 * {@link android.R.attr#state_single}
 * {@link android.R.attr#state_first}
 * {@link android.R.attr#state_middle}
 * {@link android.R.attr#state_last}
 * {@link android.R.attr#state_pressed}
 */
public class AnimatedStateListDrawableCompat extends StateListDrawableCompat
        implements TintAwareDrawable {
    private static final String LOGTAG = AnimatedStateListDrawableCompat.class.getSimpleName();
    private static final String ELEMENT_TRANSITION = "transition";
    private static final String ELEMENT_ITEM = "item";
    private static final String TRANSITION_MISSING_DRAWABLE_ERROR =
            ": <transition> tag requires a 'drawable' attribute or child tag defining a drawable";
    private static final String TRANSITION_MISSING_FROM_TO_ID =
            ": <transition> tag requires 'fromId' & 'toId' attributes";
    private static final String ITEM_MISSING_DRAWABLE_ERROR =
            ": <item> tag requires a 'drawable' attribute or child tag defining a drawable";
    private AnimatedStateListState mState;
    /** The currently running transition, if any. */
    private Transition mTransition;
    /** Index to be set after the transition ends. */
    private int mTransitionToIndex = -1;
    /** Index away from which we are transitioning. */
    private int mTransitionFromIndex = -1;
    private boolean mMutated;

    public AnimatedStateListDrawableCompat() {
        this(null, null);
    }

    AnimatedStateListDrawableCompat(@Nullable AnimatedStateListState state,
            @Nullable Resources res) {
        super(null);
        // Every animated state list drawable has its own constant state.
        final AnimatedStateListState newState = new AnimatedStateListState(state, this, res);
        setConstantState(newState);
        onStateChange(getState());
        jumpToCurrentState();
    }

    /**
     * Creates an AnimatedStateListDrawableCompat.
     *
     * @param context context to inflate against
     * @param resId the resource ID for AnimatedStateListDrawable object.
     * @param theme the theme to apply, may be null.
     * @return a new AnimatedStateListDrawableCompat or null if parsing error is found.
     */
    public static @Nullable AnimatedStateListDrawableCompat create(
            @NonNull Context context,
            @DrawableRes int resId,
            Resources.@Nullable Theme theme) {
        try {
            final Resources res = context.getResources();
            @SuppressLint("ResourceType")
            final XmlPullParser parser = res.getXml(resId);
            final AttributeSet attrs = Xml.asAttributeSet(parser);
            int type;
            //noinspection StatementWithEmptyBody
            while ((type = parser.next()) != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT) {
                // Empty loop
            }
            if (type != XmlPullParser.START_TAG) {
                throw new XmlPullParserException("No start tag found");
            }
            return createFromXmlInner(context, res, parser, attrs, theme);
        } catch (XmlPullParserException e) {
            Log.e(LOGTAG, "parser error", e);
        } catch (IOException e) {
            Log.e(LOGTAG, "parser error", e);
        }
        return null;
    }

    /**
     * Create a AnimatedStateListDrawableCompat from inside an XML document using an optional
     * {@link Resources.Theme}. Called on a parser positioned at a tag in an XML
     * document, tries to create an AnimatedStateListDrawableCompat from that tag.
     */
    public static @NonNull AnimatedStateListDrawableCompat createFromXmlInner(
            @NonNull Context context,
            @NonNull Resources resources,
            @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs,
            Resources.@Nullable Theme theme)
            throws IOException, XmlPullParserException {
        final String name = parser.getName();
        if (!name.equals("animated-selector")) {
            throw new XmlPullParserException(
                    parser.getPositionDescription() + ": invalid animated-selector tag " + name);
        }
        AnimatedStateListDrawableCompat asl = new AnimatedStateListDrawableCompat();
        asl.inflate(context, resources, parser, attrs, theme);
        return asl;
    }

    /**
     * Inflate this Drawable from an XML resource optionally styled by a theme.
     * This can't be called more than once for each Drawable.
     *
     * @param context context to inflate against
     * @param resources Resources used to resolve attribute values
     * @param parser XML parser from which to inflate this Drawable
     * @param attrs Base set of attribute values
     * @param theme Theme to apply, may be null
     * @throws XmlPullParserException if the XML is malformed
     * @throws IOException if the XML could not be read
     */
    @Override
    public void inflate(
            @NonNull Context context,
            @NonNull Resources resources,
            @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs,
            Resources.@Nullable Theme theme)
            throws XmlPullParserException, IOException {
        final TypedArray a = obtainAttributes(
                resources, theme, attrs, R.styleable.AnimatedStateListDrawableCompat);
        setVisible(a.getBoolean(R.styleable.AnimatedStateListDrawableCompat_android_visible, true),
                true);
        updateStateFromTypedArray(a);
        updateDensity(resources);
        a.recycle();
        inflateChildElements(context, resources, parser, attrs, theme);
        init();
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        final boolean changed = super.setVisible(visible, restart);
        if (mTransition != null && (changed || restart)) {
            if (visible) {
                mTransition.start();
            } else {
                // Ensure we're showing the correct state when visible.
                jumpToCurrentState();
            }
        }
        return changed;
    }

    /**
     * Add a new drawable to the set of keyframes.
     *
     * @param stateSet An array of resource IDs to associate with the keyframe
     * @param drawable The drawable to show when in the specified state, may not be null
     * @param id       The unique identifier for the keyframe
     */
    public void addState(int @NonNull [] stateSet, @NonNull Drawable drawable, int id) {
        ObjectsCompat.requireNonNull(drawable);
        mState.addStateSet(stateSet, drawable, id);
        onStateChange(getState());
    }

    /**
     * Adds a new transition between keyframes.
     *
     * @param fromId     Unique identifier of the starting keyframe
     * @param toId       Unique identifier of the ending keyframe
     * @param transition An {@link Animatable} drawable to use as a transition, may not be null
     * @param reversible Whether the transition can be reversed
     */
    public <T extends Drawable & Animatable> void addTransition(int fromId, int toId,
            @NonNull T transition, boolean reversible) {
        ObjectsCompat.requireNonNull(transition);
        mState.addTransition(fromId, toId, transition, reversible);
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    public void jumpToCurrentState() {
        super.jumpToCurrentState();
        if (mTransition != null) {
            mTransition.stop();
            mTransition = null;
            selectDrawable(mTransitionToIndex);
            mTransitionToIndex = -1;
            mTransitionFromIndex = -1;
        }
    }

    @Override
    protected boolean onStateChange(int @NonNull [] stateSet) {
        // If we're not already at the target index, either attempt to find a
        // valid transition to it or jump directly there.
        final int targetIndex = mState.indexOfKeyframe(stateSet);
        boolean changed = targetIndex != getCurrentIndex()
                && (selectTransition(targetIndex) || selectDrawable(targetIndex));
        // We need to propagate the state change to the current drawable, but
        // we can't call StateListDrawable.onStateChange() without changing the
        // current drawable.
        final Drawable current = getCurrent();
        if (current != null) {
            changed |= current.setState(stateSet);
        }
        return changed;
    }

    private boolean selectTransition(int toIndex) {
        final int fromIndex;
        final Transition currentTransition = mTransition;
        if (currentTransition != null) {
            if (toIndex == mTransitionToIndex) {
                // Already animating to that keyframe.
                return true;
            } else if (toIndex == mTransitionFromIndex && currentTransition.canReverse()) {
                // Reverse the current animation.
                currentTransition.reverse();
                mTransitionToIndex = mTransitionFromIndex;
                mTransitionFromIndex = toIndex;
                return true;
            }
            // Start the next transition from the end of the current one.
            fromIndex = mTransitionToIndex;
            // Changing animation, end the current animation.
            currentTransition.stop();
        } else {
            fromIndex = getCurrentIndex();
        }
        // Reset state.
        mTransition = null;
        mTransitionFromIndex = -1;
        mTransitionToIndex = -1;
        final AnimatedStateListState state = mState;
        final int fromId = state.getKeyframeIdAt(fromIndex);
        final int toId = state.getKeyframeIdAt(toIndex);
        if (toId == 0 || fromId == 0) {
            // Missing a keyframe ID.
            return false;
        }
        final int transitionIndex = state.indexOfTransition(fromId, toId);
        if (transitionIndex < 0) {
            // Couldn't select a transition.
            return false;
        }
        boolean hasReversibleFlag = state.transitionHasReversibleFlag(fromId, toId);
        // This may fail if we're already on the transition, but that's okay!
        selectDrawable(transitionIndex);
        final Transition transition;
        final Drawable d = getCurrent();
        if (d instanceof AnimationDrawable) {
            final boolean reversed = state.isTransitionReversed(fromId, toId);
            transition = new AnimationDrawableTransition((AnimationDrawable) d,
                    reversed, hasReversibleFlag);
        } else if (d instanceof AnimatedVectorDrawableCompat) {
            //final boolean reversed = state.isTransitionReversed(fromId, toId);
            transition = new AnimatedVectorDrawableTransition((AnimatedVectorDrawableCompat) d);
        } else if (d instanceof Animatable) {
            transition = new AnimatableTransition((Animatable) d);
        } else {
            // We don't know how to animate this transition.
            return false;
        }
        transition.start();
        mTransition = transition;
        mTransitionFromIndex = fromIndex;
        mTransitionToIndex = toIndex;
        return true;
    }

    private abstract static class Transition {
        public abstract void start();

        public abstract void stop();

        public void reverse() {
            // Not supported by default.
        }

        public boolean canReverse() {
            return false;
        }
    }

    private static class AnimatableTransition extends Transition {
        private final Animatable mA;

        AnimatableTransition(Animatable a) {
            mA = a;
        }

        @Override
        public void start() {
            mA.start();
        }

        @Override
        public void stop() {
            mA.stop();
        }
    }

    private static class AnimationDrawableTransition extends Transition {
        private final ObjectAnimator mAnim;
        // Even AnimationDrawable is always reversible technically, but
        // we should obey the XML's android:reversible flag.
        private final boolean mHasReversibleFlag;

        AnimationDrawableTransition(AnimationDrawable ad,
                boolean reversed, boolean hasReversibleFlag) {
            final int frameCount = ad.getNumberOfFrames();
            final int fromFrame = reversed ? frameCount - 1 : 0;
            final int toFrame = reversed ? 0 : frameCount - 1;
            final FrameInterpolator interp = new FrameInterpolator(ad, reversed);
            @SuppressLint("ObjectAnimatorBinding")
            final ObjectAnimator anim =
                    ObjectAnimator.ofInt(ad, "currentIndex", fromFrame, toFrame);
            anim.setAutoCancel(true);
            anim.setDuration(interp.getTotalDuration());
            anim.setInterpolator(interp);
            mHasReversibleFlag = hasReversibleFlag;
            mAnim = anim;
        }

        @Override
        public boolean canReverse() {
            return mHasReversibleFlag;
        }

        @Override
        public void start() {
            mAnim.start();
        }

        @Override
        public void reverse() {
            mAnim.reverse();
        }

        @Override
        public void stop() {
            mAnim.cancel();
        }
    }

    private static class AnimatedVectorDrawableTransition extends Transition {
        private final AnimatedVectorDrawableCompat mAvd;

        AnimatedVectorDrawableTransition(AnimatedVectorDrawableCompat avd) {
            mAvd = avd;
        }

        @Override
        public void start() {
            mAvd.start();
        }

        @Override
        public void stop() {
            mAvd.stop();
        }
    }

    private void updateStateFromTypedArray(TypedArray a) {
        final AnimatedStateListState state = mState;
        // Account for any configuration changes.
        state.mChangingConfigurations |= a.getChangingConfigurations();
        // Extract the theme attributes, if any.
        state.setVariablePadding(
                a.getBoolean(R.styleable.AnimatedStateListDrawableCompat_android_variablePadding,
                        state.mVariablePadding));
        state.setConstantSize(
                a.getBoolean(R.styleable.AnimatedStateListDrawableCompat_android_constantSize,
                        state.mConstantSize));
        state.setEnterFadeDuration(
                a.getInt(R.styleable.AnimatedStateListDrawableCompat_android_enterFadeDuration,
                        state.mEnterFadeDuration));
        state.setExitFadeDuration(
                a.getInt(R.styleable.AnimatedStateListDrawableCompat_android_exitFadeDuration,
                        state.mExitFadeDuration));
        setDither(a.getBoolean(R.styleable.AnimatedStateListDrawableCompat_android_dither,
                state.mDither));
    }

    private void init() {
        onStateChange(getState());
    }

    private void inflateChildElements(
            @NonNull Context context,
            @NonNull Resources resources,
            @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs,
            Resources.@Nullable Theme theme)
            throws XmlPullParserException, IOException {
        int type;
        final int innerDepth = parser.getDepth() + 1;
        int depth;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && ((depth = parser.getDepth()) >= innerDepth
                || type != XmlPullParser.END_TAG)) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }
            if (depth > innerDepth) {
                continue;
            }
            if (parser.getName().equals(ELEMENT_ITEM)) {
                parseItem(context, resources, parser, attrs, theme);
            } else if (parser.getName().equals(ELEMENT_TRANSITION)) {
                parseTransition(context, resources, parser, attrs, theme);
            }
        }
    }

    private int parseTransition(
            @NonNull Context context,
            @NonNull Resources resources,
            @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs,
            Resources.@Nullable Theme theme)
            throws XmlPullParserException, IOException {

        final TypedArray a = obtainAttributes(resources, theme, attrs,
                R.styleable.AnimatedStateListDrawableTransition);
        final int fromId = a.getResourceId(
                R.styleable.AnimatedStateListDrawableTransition_android_fromId, -1);
        final int toId = a.getResourceId(
                R.styleable.AnimatedStateListDrawableTransition_android_toId, -1);
        Drawable dr = null;
        final int drawableId = a.getResourceId(
                R.styleable.AnimatedStateListDrawableTransition_android_drawable, -1);
        if (drawableId > 0) {
            dr = ResourceManagerInternal.get().getDrawable(context, drawableId);
        }
        final boolean reversible = a.getBoolean(
                R.styleable.AnimatedStateListDrawableTransition_android_reversible, false);
        a.recycle();
        // Loading child elements modifies the state of the AttributeSet's underlying parser, so
        // it needs to happen after obtaining attributes and extracting states.
        if (dr == null) {
            int type;
            //noinspection StatementWithEmptyBody
            while ((type = parser.next()) == XmlPullParser.TEXT) {
                // no-op
            }
            if (type != XmlPullParser.START_TAG) {
                throw new XmlPullParserException(
                        parser.getPositionDescription() + TRANSITION_MISSING_DRAWABLE_ERROR);
            }
            // Attempt to parse child AVDs
            if (parser.getName().equals("animated-vector")) {
                dr = AnimatedVectorDrawableCompat.createFromXmlInner(context, resources, parser,
                        attrs, theme);
            } else {
                dr = Drawable.createFromXmlInner(resources, parser, attrs, theme);
            }
        }
        if (dr == null) {
            throw new XmlPullParserException(
                    parser.getPositionDescription() + TRANSITION_MISSING_DRAWABLE_ERROR);
        }
        if (fromId == -1 || toId == -1) {
            throw new XmlPullParserException(
                    parser.getPositionDescription() + TRANSITION_MISSING_FROM_TO_ID);
        }
        return mState.addTransition(fromId, toId, dr, reversible);
    }

    private int parseItem(
            @NonNull Context context,
            @NonNull Resources resources,
            @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs,
            Resources.@Nullable Theme theme)
            throws XmlPullParserException, IOException {
        final TypedArray a = obtainAttributes(resources, theme, attrs,
                R.styleable.AnimatedStateListDrawableItem);
        final int keyframeId = a.getResourceId(R.styleable.AnimatedStateListDrawableItem_android_id,
                0);
        Drawable dr = null;
        final int drawableId =
                a.getResourceId(R.styleable.AnimatedStateListDrawableItem_android_drawable, -1);
        if (drawableId > 0) {
            dr = ResourceManagerInternal.get().getDrawable(context, drawableId);
        }
        a.recycle();
        final int[] states = extractStateSet(attrs);
        // Loading child elements modifies the state of the AttributeSet's underlying parser, so
        // it needs to happen after obtaining attributes and extracting states.
        if (dr == null) {
            int type;
            //noinspection StatementWithEmptyBody
            while ((type = parser.next()) == XmlPullParser.TEXT) {
                // no-op
            }
            if (type != XmlPullParser.START_TAG) {
                throw new XmlPullParserException(
                        parser.getPositionDescription() + ITEM_MISSING_DRAWABLE_ERROR);
            }
            // Attempt to parse child VDs
            if (parser.getName().equals("vector")) {
                dr = VectorDrawableCompat.createFromXmlInner(resources, parser, attrs, theme);
            } else {
                dr = Drawable.createFromXmlInner(resources, parser, attrs, theme);
            }
        }
        if (dr == null) {
            throw new XmlPullParserException(
                    parser.getPositionDescription() + ITEM_MISSING_DRAWABLE_ERROR);
        }
        return mState.addStateSet(states, dr, keyframeId);
    }

    @Override
    public @NonNull Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mState.mutate();
            mMutated = true;
        }
        return this;
    }

    @Override
    AnimatedStateListState cloneConstantState() {
        return new AnimatedStateListState(mState, this, null);
    }

    @Override
    void clearMutated() {
        super.clearMutated();
        mMutated = false;
    }

    static class AnimatedStateListState extends StateListState {
        // REVERSED_BIT is indicating the current transition's direction.
        private static final long REVERSED_BIT = 0x100000000L;
        // REVERSIBLE_FLAG_BIT is indicating whether the whole transition has
        // reversible flag set to true.
        private static final long REVERSIBLE_FLAG_BIT = 0x200000000L;
        LongSparseArray<Long> mTransitions;
        SparseArrayCompat<Integer> mStateIds;

        AnimatedStateListState(@Nullable AnimatedStateListState orig,
                @NonNull AnimatedStateListDrawableCompat owner, @Nullable Resources res) {
            super(orig, owner, res);
            if (orig != null) {
                // Perform a shallow copy and rely on mutate() to deep-copy.
                mTransitions = orig.mTransitions;
                mStateIds = orig.mStateIds;
            } else {
                mTransitions = new LongSparseArray<>();
                mStateIds = new SparseArrayCompat<>();
            }
        }

        @Override
        void mutate() {
            mTransitions = mTransitions.clone();
            mStateIds = mStateIds.clone();
        }

        int addTransition(int fromId, int toId, @NonNull Drawable anim, boolean reversible) {
            final int pos = super.addChild(anim);
            final long keyFromTo = generateTransitionKey(fromId, toId);
            long reversibleBit = 0;
            if (reversible) {
                reversibleBit = REVERSIBLE_FLAG_BIT;
            }
            mTransitions.append(keyFromTo, pos | reversibleBit);
            if (reversible) {
                final long keyToFrom = generateTransitionKey(toId, fromId);
                mTransitions.append(keyToFrom, pos | REVERSED_BIT | reversibleBit);
            }
            return pos;
        }

        int addStateSet(int @NonNull [] stateSet, @NonNull Drawable drawable, int id) {
            final int index = super.addStateSet(stateSet, drawable);
            mStateIds.put(index, id);
            return index;
        }

        int indexOfKeyframe(int @NonNull [] stateSet) {
            final int index = super.indexOfStateSet(stateSet);
            if (index >= 0) {
                return index;
            }
            return super.indexOfStateSet(StateSet.WILD_CARD);
        }

        int getKeyframeIdAt(int index) {
            return index < 0 ? 0 : mStateIds.get(index, 0);
        }

        int indexOfTransition(int fromId, int toId) {
            final long keyFromTo = generateTransitionKey(fromId, toId);
            return (int) mTransitions.get(keyFromTo, -1L).longValue();
        }

        boolean isTransitionReversed(int fromId, int toId) {
            final long keyFromTo = generateTransitionKey(fromId, toId);
            return (mTransitions.get(keyFromTo, -1L) & REVERSED_BIT) != 0L;
        }

        boolean transitionHasReversibleFlag(int fromId, int toId) {
            final long keyFromTo = generateTransitionKey(fromId, toId);
            return (mTransitions.get(keyFromTo, -1L) & REVERSIBLE_FLAG_BIT) != 0L;
        }

        @Override
        public @NonNull Drawable newDrawable() {
            return new AnimatedStateListDrawableCompat(this, null);
        }

        @Override
        public @NonNull Drawable newDrawable(Resources res) {
            return new AnimatedStateListDrawableCompat(this, res);
        }

        private static long generateTransitionKey(int fromId, int toId) {
            return (long) fromId << 32 | toId;
        }
    }

    @Override
    void setConstantState(@NonNull DrawableContainerState state) {
        super.setConstantState(state);
        if (state instanceof AnimatedStateListState) {
            mState = (AnimatedStateListState) state;
        }
    }

    /**
     * Interpolates between frames with respect to their individual durations.
     */
    private static class FrameInterpolator implements TimeInterpolator {
        private int[] mFrameTimes;
        private int mFrames;
        private int mTotalDuration;

        FrameInterpolator(AnimationDrawable d, boolean reversed) {
            updateFrames(d, reversed);
        }

        int updateFrames(AnimationDrawable d, boolean reversed) {
            final int frameCount = d.getNumberOfFrames();
            mFrames = frameCount;
            if (mFrameTimes == null || mFrameTimes.length < frameCount) {
                mFrameTimes = new int[frameCount];
            }
            final int[] frameTimes = mFrameTimes;
            int totalDuration = 0;
            for (int i = 0; i < frameCount; i++) {
                final int duration = d.getDuration(reversed ? frameCount - i - 1 : i);
                frameTimes[i] = duration;
                totalDuration += duration;
            }
            mTotalDuration = totalDuration;
            return totalDuration;
        }

        int getTotalDuration() {
            return mTotalDuration;
        }

        @Override
        public float getInterpolation(float input) {
            final int elapsed = (int) (input * mTotalDuration + 0.5f);
            final int frameCount = mFrames;
            final int[] frameTimes = mFrameTimes;
            // Find the current frame and remaining time within that frame.
            int remaining = elapsed;
            int i = 0;
            while (i < frameCount && remaining >= frameTimes[i]) {
                remaining -= frameTimes[i];
                i++;
            }
            // Remaining time is relative of total duration.
            final float frameElapsed;
            if (i < frameCount) {
                frameElapsed = remaining / (float) mTotalDuration;
            } else {
                frameElapsed = 0;
            }
            return i / (float) frameCount + frameElapsed;
        }
    }
}
