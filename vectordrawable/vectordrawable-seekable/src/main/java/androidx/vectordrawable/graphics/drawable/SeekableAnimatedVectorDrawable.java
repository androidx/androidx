/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.vectordrawable.graphics.drawable;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SimpleArrayMap;
import androidx.core.animation.Animator;
import androidx.core.animation.AnimatorInflater;
import androidx.core.animation.AnimatorListenerAdapter;
import androidx.core.animation.AnimatorSet;
import androidx.core.animation.ObjectAnimator;
import androidx.core.content.res.TypedArrayUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This class animates properties of a {@link VectorDrawableCompat} with animations defined using
 * {@link ObjectAnimator} or {@link AnimatorSet}.
 *
 * <p>
 * SeekableAnimatedVectorDrawable is defined in the same XML format as
 * {@link android.graphics.drawable.AnimatedVectorDrawable}.
 * <p>
 * Here are all the animatable attributes in {@link VectorDrawableCompat}:
 * <table border="2" align="center" cellpadding="5">
 *     <thead>
 *         <tr>
 *             <th>Element Name</th>
 *             <th>Animatable attribute name</th>
 *         </tr>
 *     </thead>
 *     <tr>
 *         <td>&lt;vector&gt;</td>
 *         <td>alpha</td>
 *     </tr>
 *     <tr>
 *         <td rowspan="7">&lt;group&gt;</td>
 *         <td>rotation</td>
 *     </tr>
 *     <tr>
 *         <td>pivotX</td>
 *     </tr>
 *     <tr>
 *         <td>pivotY</td>
 *     </tr>
 *     <tr>
 *         <td>scaleX</td>
 *     </tr>
 *     <tr>
 *         <td>scaleY</td>
 *     </tr>
 *     <tr>
 *         <td>translateX</td>
 *     </tr>
 *     <tr>
 *         <td>translateY</td>
 *     </tr>
 *     <tr>
 *         <td rowspan="8">&lt;path&gt;</td>
 *         <td>fillColor</td>
 *     </tr>
 *     <tr>
 *         <td>pathData</td>
 *     </tr>
 *     <tr>
 *         <td>strokeColor</td>
 *     </tr>
 *     <tr>
 *         <td>strokeWidth</td>
 *     </tr>
 *     <tr>
 *         <td>strokeAlpha</td>
 *     </tr>
 *     <tr>
 *         <td>fillAlpha</td>
 *     </tr>
 *     <tr>
 *         <td>trimPathStart</td>
 *     </tr>
 *     <tr>
 *         <td>trimPathEnd</td>
 *     </tr>
 *     <tr>
 *         <td>trimPathOffset</td>
 *     </tr>
 * </table>
 * <p>
 * You can always create a SeekableAnimatedVectorDrawable object and use it as a Drawable by the
 * Java API. In order to refer to SeekableAnimatedVectorDrawable inside an XML file, you can
 * use app:srcCompat attribute in AppCompat library's ImageButton or ImageView.
 * <p>
 * SeekableAnimatedVectorDrawable supports the following features too:
 * <ul>
 * <li>Path Morphing (PathType evaluator). This is used for morphing one path into another.</li>
 * <li>Path Interpolation. This is used to defined a flexible interpolator (represented as a path)
 * instead of the system defined ones like LinearInterpolator.</li>
 * <li>Animating 2 values in one ObjectAnimator according to one path's X value and Y value. One
 * usage is moving one object in both X and Y dimensions along an path.</li>
 * </ul>
 * <p>
 * Unlike {@code AnimatedVectorDrawableCompat}, this class does not delegate to the platform
 * {@link android.graphics.drawable.AnimatedVectorDrawable} on any API levels.
 */
public class SeekableAnimatedVectorDrawable extends Drawable implements Animatable {

    private static final String LOGTAG = "SeekableAVD";

    private static final String ANIMATED_VECTOR = "animated-vector";
    private static final String TARGET = "target";

    private static final boolean DBG_ANIMATION_VECTOR_DRAWABLE = false;

    private AnimatedVectorDrawableState mAnimatedVectorState;

    // An internal listener to bridge between Animator and SAVD's callbacks.
    private InternalAnimatorListener mAnimatorListener = null;

    // An array to keep track of multiple callbacks associated with one drawable.
    @SuppressWarnings("WeakerAccess")
    ArrayList<AnimationCallback> mAnimationCallbacks = null;

    /**
     * Abstract class for animation callback. Used to notify animation events.
     */
    public abstract static class AnimationCallback {

        /**
         * Called when the animation starts.
         *
         * @param drawable The drawable started the animation.
         */
        public void onAnimationStart(@NonNull SeekableAnimatedVectorDrawable drawable) {
        }

        /**
         * Called when the animation ends.
         *
         * @param drawable The drawable finished the animation.
         */
        public void onAnimationEnd(@NonNull SeekableAnimatedVectorDrawable drawable) {
        }

        /**
         * Called when the animation is paused.
         *
         * @param drawable The drawable.
         */
        public void onAnimationPause(@NonNull SeekableAnimatedVectorDrawable drawable) {
        }

        /**
         * Called when the animation is resumed.
         *
         * @param drawable The drawable.
         */
        public void onAnimationResume(@NonNull SeekableAnimatedVectorDrawable drawable) {
        }

        /**
         * Called on every frame while the animation is running. The implementation must not
         * register or unregister any {@link AnimationCallback} here.
         *
         * @param drawable The drawable.
         */
        public void onAnimationUpdate(@NonNull SeekableAnimatedVectorDrawable drawable) {
        }
    }

    private final Callback mCallback = new Callback() {

        @Override
        public void invalidateDrawable(@NonNull Drawable who) {
            invalidateSelf();
        }

        @Override
        public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
            scheduleSelf(what, when);
        }

        @Override
        public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
            unscheduleSelf(what);
        }
    };

    private SeekableAnimatedVectorDrawable() {
        this(null, null);
    }

    private SeekableAnimatedVectorDrawable(
            @Nullable AnimatedVectorDrawableState state,
            @Nullable Resources res
    ) {
        if (state != null) {
            mAnimatedVectorState = state;
        } else {
            mAnimatedVectorState =
                    new AnimatedVectorDrawableState(null, mCallback, res);
        }
    }

    /**
     * mutate() is not supported. This method simply returns {@code this}.
     */
    @NonNull
    @Override
    public Drawable mutate() {
        return this;
    }

    /**
     * Create a SeekableAnimatedVectorDrawable object.
     *
     * @param context the context for creating the animators.
     * @param resId   the resource ID for SeekableAnimatedVectorDrawable object.
     * @return a new SeekableAnimatedVectorDrawable or null if parsing error is found.
     */
    @Nullable
    public static SeekableAnimatedVectorDrawable create(
            @NonNull Context context,
            @DrawableRes int resId
    ) {
        Resources resources = context.getResources();
        try {
            //noinspection AndroidLintResourceType - Parse drawable as XML.
            final XmlPullParser parser = resources.getXml(resId);
            final AttributeSet attrs = Xml.asAttributeSet(parser);
            int type;
            do {
                type = parser.next();
            } while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT);
            if (type != XmlPullParser.START_TAG) {
                throw new XmlPullParserException("No start tag found");
            }
            return createFromXmlInner(context.getResources(), parser, attrs, context.getTheme());
        } catch (XmlPullParserException e) {
            Log.e(LOGTAG, "parser error", e);
        } catch (IOException e) {
            Log.e(LOGTAG, "parser error", e);
        }
        return null;
    }

    /**
     * Create a SeekableAnimatedVectorDrawable from inside an XML document using an optional
     * {@link Theme}. Called on a parser positioned at a tag in an XML
     * document, tries to create a Drawable from that tag. Returns {@code null}
     * if the tag is not a valid drawable.
     */
    @NonNull
    public static SeekableAnimatedVectorDrawable createFromXmlInner(
            @NonNull Resources r,
            @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs,
            @Nullable Theme theme
    ) throws XmlPullParserException, IOException {
        final SeekableAnimatedVectorDrawable drawable = new SeekableAnimatedVectorDrawable();
        drawable.inflate(r, parser, attrs, theme);
        return drawable;
    }

    @Nullable
    @Override
    public ConstantState getConstantState() {
        // We can't support constant state in older platform.
        // We need Context to create the animator, and we can't save the context in the constant
        // state.
        return null;
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | mAnimatedVectorState.mChangingConfigurations;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        mAnimatedVectorState.mVectorDrawable.draw(canvas);
        if (mAnimatedVectorState.mAnimatorSet.isStarted()) {
            invalidateSelf();
        }
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        mAnimatedVectorState.mVectorDrawable.setBounds(bounds);
    }

    @Override
    protected boolean onStateChange(@NonNull int[] state) {
        return mAnimatedVectorState.mVectorDrawable.setState(state);
    }

    @Override
    protected boolean onLevelChange(int level) {
        return mAnimatedVectorState.mVectorDrawable.setLevel(level);
    }

    @IntRange(from = 0, to = 255)
    @Override
    public int getAlpha() {
        return mAnimatedVectorState.mVectorDrawable.getAlpha();
    }

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
        mAnimatedVectorState.mVectorDrawable.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mAnimatedVectorState.mVectorDrawable.setColorFilter(colorFilter);
    }

    @Nullable
    @Override
    public ColorFilter getColorFilter() {
        return mAnimatedVectorState.mVectorDrawable.getColorFilter();
    }

    @Override
    public void setTint(@ColorInt int tint) {
        mAnimatedVectorState.mVectorDrawable.setTint(tint);
    }

    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        mAnimatedVectorState.mVectorDrawable.setTintList(tint);
    }

    @Override
    public void setTintMode(@Nullable PorterDuff.Mode tintMode) {
        mAnimatedVectorState.mVectorDrawable.setTintMode(tintMode);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        mAnimatedVectorState.mVectorDrawable.setVisible(visible, restart);
        return super.setVisible(visible, restart);
    }

    @Override
    public boolean isStateful() {
        return mAnimatedVectorState.mVectorDrawable.isStateful();
    }

    /**
     * @return The opacity class of the Drawable.
     * @deprecated This method is no longer used in graphics optimizations
     */
    @Deprecated
    @Override
    public int getOpacity() {
        return mAnimatedVectorState.mVectorDrawable.getOpacity();
    }

    @Override
    public int getIntrinsicWidth() {
        return mAnimatedVectorState.mVectorDrawable.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return mAnimatedVectorState.mVectorDrawable.getIntrinsicHeight();
    }

    @Override
    public boolean isAutoMirrored() {
        return mAnimatedVectorState.mVectorDrawable.isAutoMirrored();
    }

    @Override
    public void setAutoMirrored(boolean mirrored) {
        mAnimatedVectorState.mVectorDrawable.setAutoMirrored(mirrored);
    }

    @Override
    public void inflate(
            @NonNull Resources res,
            @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs,
            @Nullable Theme theme
    ) throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        final int innerDepth = parser.getDepth() + 1;

        // Parse everything until the end of the animated-vector element.
        while (eventType != XmlPullParser.END_DOCUMENT
                && (parser.getDepth() >= innerDepth || eventType != XmlPullParser.END_TAG)) {
            if (eventType == XmlPullParser.START_TAG) {
                final String tagName = parser.getName();
                if (DBG_ANIMATION_VECTOR_DRAWABLE) {
                    Log.v(LOGTAG, "tagName is " + tagName);
                }
                if (ANIMATED_VECTOR.equals(tagName)) {
                    final TypedArray a =
                            TypedArrayUtils.obtainAttributes(res, theme, attrs,
                                    AndroidResources.STYLEABLE_ANIMATED_VECTOR_DRAWABLE);

                    int drawableRes = a.getResourceId(
                            AndroidResources.STYLEABLE_ANIMATED_VECTOR_DRAWABLE_DRAWABLE, 0);
                    if (DBG_ANIMATION_VECTOR_DRAWABLE) {
                        Log.v(LOGTAG, "drawableRes is " + drawableRes);
                    }
                    if (drawableRes != 0) {
                        VectorDrawableCompat vectorDrawable =
                                VectorDrawableCompat.createWithoutDelegate(res, drawableRes, theme);
                        vectorDrawable.setAllowCaching(false);
                        vectorDrawable.setCallback(mCallback);
                        if (mAnimatedVectorState.mVectorDrawable != null) {
                            mAnimatedVectorState.mVectorDrawable.setCallback(null);
                        }
                        mAnimatedVectorState.mVectorDrawable = vectorDrawable;
                    }
                    a.recycle();
                } else if (TARGET.equals(tagName)) {
                    final TypedArray a =
                            res.obtainAttributes(attrs,
                                    AndroidResources.STYLEABLE_ANIMATED_VECTOR_DRAWABLE_TARGET);
                    final String target = a.getString(
                            AndroidResources.STYLEABLE_ANIMATED_VECTOR_DRAWABLE_TARGET_NAME);

                    int id = a.getResourceId(
                            AndroidResources.STYLEABLE_ANIMATED_VECTOR_DRAWABLE_TARGET_ANIMATION,
                            0);
                    if (id != 0) {
                        // There are some important features (like path morphing), added into
                        // Animator code to support AVD at API 21.
                        Animator objectAnimator = AnimatorInflater.loadAnimator(res, theme, id);
                        setupAnimatorsForTarget(target, objectAnimator);
                    }
                    a.recycle();
                }
            }
            eventType = parser.next();
        }

        mAnimatedVectorState.setupAnimatorSet();
    }

    @Override
    public void inflate(
            @NonNull Resources res,
            @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs
    ) throws XmlPullParserException, IOException {
        inflate(res, parser, attrs, null);
    }

    @Override
    public void applyTheme(@NonNull Theme t) {
        // TODO(b/149342571): support theming in older platform.
    }

    @Override
    public boolean canApplyTheme() {
        // TODO(b/149342571): support theming in older platform.
        return false;
    }

    private static class AnimatedVectorDrawableState extends ConstantState {

        int mChangingConfigurations;
        VectorDrawableCompat mVectorDrawable;
        // Combining the array of Animators into a single AnimatorSet to hook up listener easier.
        AnimatorSet mAnimatorSet;
        ArrayList<Animator> mAnimators;
        SimpleArrayMap<Animator, String> mTargetNameMap;

        AnimatedVectorDrawableState(
                AnimatedVectorDrawableState copy,
                Callback owner,
                Resources res
        ) {
            if (copy != null) {
                mChangingConfigurations = copy.mChangingConfigurations;
                if (copy.mVectorDrawable != null) {
                    final ConstantState cs = copy.mVectorDrawable.getConstantState();
                    if (res != null) {
                        mVectorDrawable = (VectorDrawableCompat) cs.newDrawable(res);
                    } else {
                        mVectorDrawable = (VectorDrawableCompat) cs.newDrawable();
                    }
                    mVectorDrawable = (VectorDrawableCompat) mVectorDrawable.mutate();
                    mVectorDrawable.setCallback(owner);
                    mVectorDrawable.setBounds(copy.mVectorDrawable.getBounds());
                    mVectorDrawable.setAllowCaching(false);
                }
                if (copy.mAnimators != null) {
                    final int numAnimators = copy.mAnimators.size();
                    mAnimators = new ArrayList<>(numAnimators);
                    mTargetNameMap = new SimpleArrayMap<>(numAnimators);
                    for (int i = 0; i < numAnimators; ++i) {
                        Animator anim = copy.mAnimators.get(i);
                        Animator animClone = anim.clone();
                        String targetName = copy.mTargetNameMap.get(anim);
                        Object targetObject = mVectorDrawable.getTargetByName(targetName);
                        animClone.setTarget(targetObject);
                        mAnimators.add(animClone);
                        mTargetNameMap.put(animClone, targetName);
                    }
                    setupAnimatorSet();
                }
            }
        }

        @NonNull
        @Override
        public Drawable newDrawable() {
            throw new IllegalStateException("No constant state support for SDK < 24.");
        }

        @NonNull
        @Override
        public Drawable newDrawable(Resources res) {
            throw new IllegalStateException("No constant state support for SDK < 24.");
        }

        @Override
        public int getChangingConfigurations() {
            return mChangingConfigurations;
        }

        void setupAnimatorSet() {
            if (mAnimatorSet == null) {
                mAnimatorSet = new AnimatorSet();
            }
            mAnimatorSet.playTogether(mAnimators);
        }
    }

    private void setupAnimatorsForTarget(String name, Animator animator) {
        Object target = mAnimatedVectorState.mVectorDrawable.getTargetByName(name);
        animator.setTarget(target);
        if (mAnimatedVectorState.mAnimators == null) {
            mAnimatedVectorState.mAnimators = new ArrayList<>();
            mAnimatedVectorState.mTargetNameMap = new SimpleArrayMap<>();
        }
        mAnimatedVectorState.mAnimators.add(animator);
        mAnimatedVectorState.mTargetNameMap.put(animator, name);
        if (DBG_ANIMATION_VECTOR_DRAWABLE) {
            Log.v(LOGTAG, "add animator  for target " + name + " " + animator);
        }
    }

    /**
     * Returns whether the animation is running (has started and not yet ended).
     *
     * @return {@code true} if the animation is running.
     */
    @Override
    public boolean isRunning() {
        return mAnimatedVectorState.mAnimatorSet.isRunning();
    }

    /**
     * Returns whether the animation is currently in a paused state.
     *
     * @return {@code true} if the animation is paused.
     */
    public boolean isPaused() {
        return mAnimatedVectorState.mAnimatorSet.isPaused();
    }

    @Override
    public void start() {
        // If any one of the animator has not ended, do nothing.
        if (mAnimatedVectorState.mAnimatorSet.isStarted()) {
            return;
        }
        // Otherwise, kick off animatorSet.
        mAnimatedVectorState.mAnimatorSet.start();
        invalidateSelf();
    }

    @Override
    public void stop() {
        mAnimatedVectorState.mAnimatorSet.end();
    }

    /**
     * Pauses a running animation. This method should only be called on the same thread on which
     * the animation was started. If the animation has not yet been started or has since ended,
     * then the call is ignored. Paused animations can be resumed by calling {@link #resume()}.
     */
    public void pause() {
        mAnimatedVectorState.mAnimatorSet.pause();
    }

    /**
     * Resumes a paused animation. The animation resumes from where it left off when it was
     * paused. This method should only be called on the same thread on which the animation was
     * started. Calls will be ignored if this {@link SeekableAnimatedVectorDrawable} is not
     * currently paused.
     */
    public void resume() {
        mAnimatedVectorState.mAnimatorSet.resume();
    }

    /**
     * Sets the position of the animation to the specified point in time. This time should be
     * between 0 and the total duration of the animation, including any repetition. If the
     * animation has not yet been started, then it will not advance forward after it is set to this
     * time; it will simply set the time to this value and perform any appropriate actions based on
     * that time. If the animation is already running, then setCurrentPlayTime() will set the
     * current playing time to this value and continue playing from that point.
     *
     * @param playTime The time, in milliseconds, to which the animation is advanced or rewound.
     *                 Unless the animation is reversing, the playtime is considered the time since
     *                 the end of the start delay of the AnimatorSet in a forward playing direction.
     */
    public void setCurrentPlayTime(@IntRange(from = 0) long playTime) {
        mAnimatedVectorState.mAnimatorSet.setCurrentPlayTime(playTime);
        invalidateSelf();
    }

    /**
     * Returns the milliseconds elapsed since the start of the animation.
     *
     * <p>For ongoing animations, this method returns the current progress of the animation in
     * terms of play time. For an animation that has not yet been started: if the animation has been
     * seeked to a certain time via {@link #setCurrentPlayTime(long)}, the seeked play time will
     * be returned; otherwise, this method will return 0.
     *
     * @return the current position in time of the animation in milliseconds
     */
    @IntRange(from = 0)
    public long getCurrentPlayTime() {
        return mAnimatedVectorState.mAnimatorSet.getCurrentPlayTime();
    }

    /**
     * Gets the total duration of the animation, accounting for animation sequences, start delay,
     * and repeating. Return {@link Animator#DURATION_INFINITE} if the duration is infinite.
     *
     * @return Total time the animation takes to finish, starting from the time {@link #start()}
     * is called. {@link Animator#DURATION_INFINITE} if the animation or any of the child
     * animations repeats infinite times.
     */
    public long getTotalDuration() {
        return mAnimatedVectorState.mAnimatorSet.getTotalDuration();
    }

    class InternalAnimatorListener extends AnimatorListenerAdapter
            implements Animator.AnimatorUpdateListener {

        @Override
        public void onAnimationStart(@NonNull Animator animation) {
            final ArrayList<AnimationCallback> callbacks = mAnimationCallbacks;
            if (callbacks != null) {
                for (int i = 0, size = callbacks.size(); i < size; i++) {
                    callbacks.get(i).onAnimationStart(SeekableAnimatedVectorDrawable.this);
                }
            }
        }

        @Override
        public void onAnimationEnd(@NonNull Animator animation) {
            final ArrayList<AnimationCallback> callbacks = mAnimationCallbacks;
            if (callbacks != null) {
                for (int i = 0, size = callbacks.size(); i < size; i++) {
                    callbacks.get(i).onAnimationEnd(SeekableAnimatedVectorDrawable.this);
                }
            }
        }

        @Override
        public void onAnimationPause(@NonNull Animator animation) {
            final ArrayList<AnimationCallback> callbacks = mAnimationCallbacks;
            if (callbacks != null) {
                for (int i = 0, size = callbacks.size(); i < size; i++) {
                    callbacks.get(i).onAnimationPause(SeekableAnimatedVectorDrawable.this);
                }
            }
        }

        @Override
        public void onAnimationResume(@NonNull Animator animation) {
            final ArrayList<AnimationCallback> callbacks = mAnimationCallbacks;
            if (callbacks != null) {
                for (int i = 0, size = callbacks.size(); i < size; i++) {
                    callbacks.get(i).onAnimationResume(SeekableAnimatedVectorDrawable.this);
                }
            }
        }

        @Override
        public void onAnimationUpdate(@NonNull Animator animation) {
            final ArrayList<AnimationCallback> callbacks = mAnimationCallbacks;
            if (callbacks != null) {
                for (int i = 0, size = callbacks.size(); i < size; i++) {
                    callbacks.get(i).onAnimationUpdate(SeekableAnimatedVectorDrawable.this);
                }
            }
        }
    }

    /**
     * Adds a callback to listen to the animation events.
     *
     * @param callback Callback to add.
     */
    public void registerAnimationCallback(@NonNull AnimationCallback callback) {
        // Add listener accordingly.
        if (mAnimationCallbacks == null) {
            mAnimationCallbacks = new ArrayList<>();
        } else if (mAnimationCallbacks.contains(callback)) {
            // If this call back is already in, then don't need to append another copy.
            return;
        } else {
            mAnimationCallbacks = new ArrayList<>(mAnimationCallbacks);
        }

        mAnimationCallbacks.add(callback);

        if (mAnimatorListener == null) {
            // Create an internal listener in order to bridge events to our callbacks.
            mAnimatorListener = new InternalAnimatorListener();
            mAnimatedVectorState.mAnimatorSet.addListener(mAnimatorListener);
            mAnimatedVectorState.mAnimatorSet.addPauseListener(mAnimatorListener);
            mAnimatedVectorState.mAnimatorSet.addUpdateListener(mAnimatorListener);
        }
    }

    /**
     * A helper function to clean up the animator listener in the mAnimatorSet.
     */
    private void removeAnimatorSetListener() {
        if (mAnimatorListener != null) {
            mAnimatedVectorState.mAnimatorSet.removeListener(mAnimatorListener);
            mAnimatedVectorState.mAnimatorSet.removePauseListener(mAnimatorListener);
            mAnimatedVectorState.mAnimatorSet.removeUpdateListener(mAnimatorListener);
            mAnimatorListener = null;
        }
    }

    /**
     * Removes the specified animation callback.
     *
     * @param callback Callback to remove.
     * @return {@code false} if callback didn't exist in the call back list, or {@code true} if
     * callback has been removed successfully.
     */
    public boolean unregisterAnimationCallback(@NonNull AnimationCallback callback) {
        if (mAnimationCallbacks == null) {
            // Nothing to be removed.
            return false;
        }

        boolean removed = false;
        if (mAnimationCallbacks.contains(callback)) {
            mAnimationCallbacks = new ArrayList<>(mAnimationCallbacks);
            mAnimationCallbacks.remove(callback);
            removed = true;
        }

        //  When the last call back unregistered, remove the listener accordingly.
        if (mAnimationCallbacks.isEmpty()) {
            removeAnimatorSetListener();
        }
        return removed;
    }

    /**
     * Removes all existing animation callbacks.
     */
    public void clearAnimationCallbacks() {
        removeAnimatorSetListener();
        if (mAnimationCallbacks == null) {
            return;
        }
        mAnimationCallbacks.clear();
    }
}
