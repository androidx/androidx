/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package android.support.graphics.drawable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
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
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * For API 24 and above, this class is delegating to the framework's {@link
 * AnimatedVectorDrawable}.
 * For older API version, this class uses {@link android.animation.ObjectAnimator} and
 * {@link android.animation.AnimatorSet} to animate the properties of a
 * {@link VectorDrawableCompat} to create an animated drawable.
 * <p/>
 * AnimatedVectorDrawableCompat are defined in the same XML format as
 * {@link AnimatedVectorDrawable}.
 * <p/>
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
 *         <td>trimPathOffset</td>
 *     </tr>
 * </table>
 * <p/>
 * You can always create a AnimatedVectorDrawableCompat object and use it as a Drawable by the Java
 * API. In order to refer to AnimatedVectorDrawableCompat inside a XML file, you can use
 * app:srcCompat attribute in AppCompat library's ImageButton or ImageView.
 * <p/>
 * Note that the animation in AnimatedVectorDrawableCompat now can support the following features:
 * <ul>
 * <li>Path Morphing (PathType evaluator). This is used for morphing one path into another.</li>
 * <li>Path Interpolation. This is used to defined a flexible interpolator (represented as a path)
 * instead of the system defined ones like LinearInterpolator.</li>
 * <li>Animating 2 values in one ObjectAnimator according to one path's X value and Y value. One
 * usage is moving one object in both X and Y dimensions along an path.</li>
 * </ul>
 */

public class AnimatedVectorDrawableCompat extends VectorDrawableCommon
        implements Animatable2Compat {
    private static final String LOGTAG = "AnimatedVDCompat";

    private static final String ANIMATED_VECTOR = "animated-vector";
    private static final String TARGET = "target";

    private static final boolean DBG_ANIMATION_VECTOR_DRAWABLE = false;

    private AnimatedVectorDrawableCompatState mAnimatedVectorState;

    private Context mContext;

    private ArgbEvaluator mArgbEvaluator = null;

    AnimatedVectorDrawableDelegateState mCachedConstantStateDelegate;

    // Use internal listener to support AVDC's callback.
    private Animator.AnimatorListener mAnimatorListener = null;

    // Use an array to keep track of multiple call back associated with one drawable.
    private ArrayList<Animatable2Compat.AnimationCallback> mAnimationCallbacks = null;


    AnimatedVectorDrawableCompat() {
        this(null, null, null);
    }

    private AnimatedVectorDrawableCompat(@Nullable Context context) {
        this(context, null, null);
    }

    private AnimatedVectorDrawableCompat(@Nullable Context context,
            @Nullable AnimatedVectorDrawableCompatState state,
            @Nullable Resources res) {
        mContext = context;
        if (state != null) {
            mAnimatedVectorState = state;
        } else {
            mAnimatedVectorState = new AnimatedVectorDrawableCompatState(context, state, mCallback,
                    res);
        }
    }

    /**
     * mutate() will be effective only if the getConstantState() is returning non-null.
     * Otherwise, it just return the current object without modification.
     */
    @Override
    public Drawable mutate() {
        if (mDelegateDrawable != null) {
            mDelegateDrawable.mutate();
        }
        // For older platforms that there is no delegated drawable, we just return this without
        // any modification here, and the getConstantState() will return null in this case.
        return this;
    }


    /**
     * Create a AnimatedVectorDrawableCompat object.
     *
     * @param context the context for creating the animators.
     * @param resId   the resource ID for AnimatedVectorDrawableCompat object.
     * @return a new AnimatedVectorDrawableCompat or null if parsing error is found.
     */
    @Nullable
    public static AnimatedVectorDrawableCompat create(@NonNull Context context,
            @DrawableRes int resId) {
        if (Build.VERSION.SDK_INT >= 24) {
            final AnimatedVectorDrawableCompat drawable = new AnimatedVectorDrawableCompat(context);
            drawable.mDelegateDrawable = ResourcesCompat.getDrawable(context.getResources(), resId,
                    context.getTheme());
            drawable.mDelegateDrawable.setCallback(drawable.mCallback);
            drawable.mCachedConstantStateDelegate = new AnimatedVectorDrawableDelegateState(
                    drawable.mDelegateDrawable.getConstantState());
            return drawable;
        }
        Resources resources = context.getResources();
        try {
            //noinspection AndroidLintResourceType - Parse drawable as XML.
            final XmlPullParser parser = resources.getXml(resId);
            final AttributeSet attrs = Xml.asAttributeSet(parser);
            int type;
            while ((type = parser.next()) != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT) {
                // Empty loop
            }
            if (type != XmlPullParser.START_TAG) {
                throw new XmlPullParserException("No start tag found");
            }
            return createFromXmlInner(context, context.getResources(), parser, attrs,
                    context.getTheme());
        } catch (XmlPullParserException e) {
            Log.e(LOGTAG, "parser error", e);
        } catch (IOException e) {
            Log.e(LOGTAG, "parser error", e);
        }
        return null;
    }

    /**
     * Create a AnimatedVectorDrawableCompat from inside an XML document using an optional
     * {@link Theme}. Called on a parser positioned at a tag in an XML
     * document, tries to create a Drawable from that tag. Returns {@code null}
     * if the tag is not a valid drawable.
     */
    public static AnimatedVectorDrawableCompat createFromXmlInner(Context context, Resources r,
            XmlPullParser parser, AttributeSet attrs, Theme theme)
            throws XmlPullParserException, IOException {
        final AnimatedVectorDrawableCompat drawable = new AnimatedVectorDrawableCompat(context);
        drawable.inflate(r, parser, attrs, theme);
        return drawable;
    }

    /**
     * {@inheritDoc}
     * <strong>Note</strong> that we don't support constant state when SDK < 24.
     * Make sure you check the return value before using it.
     */
    @Override
    public ConstantState getConstantState() {
        if (mDelegateDrawable != null && Build.VERSION.SDK_INT >= 24) {
            return new AnimatedVectorDrawableDelegateState(mDelegateDrawable.getConstantState());
        }
        // We can't support constant state in older platform.
        // We need Context to create the animator, and we can't save the context in the constant
        // state.
        return null;
    }

    @Override
    public int getChangingConfigurations() {
        if (mDelegateDrawable != null) {
            return mDelegateDrawable.getChangingConfigurations();
        }
        return super.getChangingConfigurations() | mAnimatedVectorState.mChangingConfigurations;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable.draw(canvas);
            return;
        }
        mAnimatedVectorState.mVectorDrawable.draw(canvas);
        if (mAnimatedVectorState.mAnimatorSet.isStarted()) {
            invalidateSelf();
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable.setBounds(bounds);
            return;
        }
        mAnimatedVectorState.mVectorDrawable.setBounds(bounds);
    }

    @Override
    protected boolean onStateChange(int[] state) {
        if (mDelegateDrawable != null) {
            return mDelegateDrawable.setState(state);
        }
        return mAnimatedVectorState.mVectorDrawable.setState(state);
    }

    @Override
    protected boolean onLevelChange(int level) {
        if (mDelegateDrawable != null) {
            return mDelegateDrawable.setLevel(level);
        }
        return mAnimatedVectorState.mVectorDrawable.setLevel(level);
    }

    @Override
    public int getAlpha() {
        if (mDelegateDrawable != null) {
            return DrawableCompat.getAlpha(mDelegateDrawable);
        }
        return mAnimatedVectorState.mVectorDrawable.getAlpha();
    }

    @Override
    public void setAlpha(int alpha) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable.setAlpha(alpha);
            return;
        }
        mAnimatedVectorState.mVectorDrawable.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        if (mDelegateDrawable != null) {
            mDelegateDrawable.setColorFilter(colorFilter);
            return;
        }
        mAnimatedVectorState.mVectorDrawable.setColorFilter(colorFilter);
    }

    @Override
    public void setTint(int tint) {
        if (mDelegateDrawable != null) {
            DrawableCompat.setTint(mDelegateDrawable, tint);
            return;
        }

        mAnimatedVectorState.mVectorDrawable.setTint(tint);
    }

    @Override
    public void setTintList(ColorStateList tint) {
        if (mDelegateDrawable != null) {
            DrawableCompat.setTintList(mDelegateDrawable, tint);
            return;
        }

        mAnimatedVectorState.mVectorDrawable.setTintList(tint);
    }

    @Override
    public void setTintMode(PorterDuff.Mode tintMode) {
        if (mDelegateDrawable != null) {
            DrawableCompat.setTintMode(mDelegateDrawable, tintMode);
            return;
        }

        mAnimatedVectorState.mVectorDrawable.setTintMode(tintMode);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        if (mDelegateDrawable != null) {
            return mDelegateDrawable.setVisible(visible, restart);
        }
        mAnimatedVectorState.mVectorDrawable.setVisible(visible, restart);
        return super.setVisible(visible, restart);
    }

    @Override
    public boolean isStateful() {
        if (mDelegateDrawable != null) {
            return mDelegateDrawable.isStateful();
        }
        return mAnimatedVectorState.mVectorDrawable.isStateful();
    }

    @Override
    public int getOpacity() {
        if (mDelegateDrawable != null) {
            return mDelegateDrawable.getOpacity();
        }
        return mAnimatedVectorState.mVectorDrawable.getOpacity();
    }

    @Override
    public int getIntrinsicWidth() {
        if (mDelegateDrawable != null) {
            return mDelegateDrawable.getIntrinsicWidth();
        }
        return mAnimatedVectorState.mVectorDrawable.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        if (mDelegateDrawable != null) {
            return mDelegateDrawable.getIntrinsicHeight();
        }
        return mAnimatedVectorState.mVectorDrawable.getIntrinsicHeight();
    }

    @Override
    public boolean isAutoMirrored() {
        if (mDelegateDrawable != null) {
            return DrawableCompat.isAutoMirrored(mDelegateDrawable);
        }
        return mAnimatedVectorState.mVectorDrawable.isAutoMirrored();
    }

    @Override
    public void setAutoMirrored(boolean mirrored) {
        if (mDelegateDrawable != null) {
            DrawableCompat.setAutoMirrored(mDelegateDrawable, mirrored);
            return;
        }
        mAnimatedVectorState.mVectorDrawable.setAutoMirrored(mirrored);
    }

    @Override
    public void inflate(Resources res, XmlPullParser parser, AttributeSet attrs, Theme theme)
            throws XmlPullParserException, IOException {
        if (mDelegateDrawable != null) {
            DrawableCompat.inflate(mDelegateDrawable, res, parser, attrs, theme);
            return;
        }
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
                        VectorDrawableCompat vectorDrawable = VectorDrawableCompat.create(res,
                                drawableRes, theme);
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
                        if (mContext != null) {
                            // There are some important features (like path morphing), added into
                            // Animator code to support AVD at API 21.
                            Animator objectAnimator = AnimatorInflaterCompat.loadAnimator(
                                    mContext, id);
                            setupAnimatorsForTarget(target, objectAnimator);
                        } else {
                            a.recycle();
                            throw new IllegalStateException("Context can't be null when inflating" +
                                    " animators");
                        }
                    }
                    a.recycle();
                }
            }
            eventType = parser.next();
        }

        mAnimatedVectorState.setupAnimatorSet();
    }

    @Override
    public void inflate(Resources res, XmlPullParser parser, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        inflate(res, parser, attrs, null);
    }

    @Override
    public void applyTheme(Theme t) {
        if (mDelegateDrawable != null) {
            DrawableCompat.applyTheme(mDelegateDrawable, t);
            return;
        }
        // TODO: support theming in older platform.
        return;
    }

    @Override
    public boolean canApplyTheme() {
        if (mDelegateDrawable != null) {
            return DrawableCompat.canApplyTheme(mDelegateDrawable);
        }
        // TODO: support theming in older platform.
        return false;
    }

    /**
     * Constant state for delegating the creating drawable job.
     * Instead of creating a VectorDrawable, create a VectorDrawableCompat instance which contains
     * a delegated VectorDrawable instance.
     */
    @RequiresApi(24)
    private static class AnimatedVectorDrawableDelegateState extends ConstantState {
        private final ConstantState mDelegateState;

        public AnimatedVectorDrawableDelegateState(ConstantState state) {
            mDelegateState = state;
        }

        @Override
        public Drawable newDrawable() {
            AnimatedVectorDrawableCompat drawableCompat =
                    new AnimatedVectorDrawableCompat();
            drawableCompat.mDelegateDrawable = mDelegateState.newDrawable();
            drawableCompat.mDelegateDrawable.setCallback(drawableCompat.mCallback);
            return drawableCompat;
        }

        @Override
        public Drawable newDrawable(Resources res) {
            AnimatedVectorDrawableCompat drawableCompat =
                    new AnimatedVectorDrawableCompat();
            drawableCompat.mDelegateDrawable = mDelegateState.newDrawable(res);
            drawableCompat.mDelegateDrawable.setCallback(drawableCompat.mCallback);
            return drawableCompat;
        }

        @Override
        public Drawable newDrawable(Resources res, Theme theme) {
            AnimatedVectorDrawableCompat drawableCompat =
                    new AnimatedVectorDrawableCompat();
            drawableCompat.mDelegateDrawable = mDelegateState.newDrawable(res, theme);
            drawableCompat.mDelegateDrawable.setCallback(drawableCompat.mCallback);
            return drawableCompat;
        }

        @Override
        public boolean canApplyTheme() {
            return mDelegateState.canApplyTheme();
        }

        @Override
        public int getChangingConfigurations() {
            return mDelegateState.getChangingConfigurations();
        }
    }

    private static class AnimatedVectorDrawableCompatState extends ConstantState {
        int mChangingConfigurations;
        VectorDrawableCompat mVectorDrawable;
        // Combining the array of Animators into a single AnimatorSet to hook up listener easier.
        AnimatorSet mAnimatorSet;
        private ArrayList<Animator> mAnimators;
        ArrayMap<Animator, String> mTargetNameMap;

        public AnimatedVectorDrawableCompatState(Context context,
                AnimatedVectorDrawableCompatState copy, Callback owner, Resources res) {
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
                    mAnimators = new ArrayList<Animator>(numAnimators);
                    mTargetNameMap = new ArrayMap<Animator, String>(numAnimators);
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

        @Override
        public Drawable newDrawable() {
            throw new IllegalStateException("No constant state support for SDK < 24.");
        }

        @Override
        public Drawable newDrawable(Resources res) {
            throw new IllegalStateException("No constant state support for SDK < 24.");
        }

        @Override
        public int getChangingConfigurations() {
            return mChangingConfigurations;
        }

        public void setupAnimatorSet() {
            if (mAnimatorSet == null) {
                mAnimatorSet = new AnimatorSet();
            }
            mAnimatorSet.playTogether(mAnimators);
        }
    }

    /**
     * Utility function to fix color interpolation prior to Lollipop. Without this fix, colors
     * are evaluated as raw integers instead of as colors, which leads to artifacts during
     * fillColor animations.
     */
    private void setupColorAnimator(Animator animator) {
        if (animator instanceof AnimatorSet) {
            List<Animator> childAnimators = ((AnimatorSet) animator).getChildAnimations();
            if (childAnimators != null) {
                for (int i = 0; i < childAnimators.size(); ++i) {
                    setupColorAnimator(childAnimators.get(i));
                }
            }
        }
        if (animator instanceof ObjectAnimator) {
            ObjectAnimator objectAnim = (ObjectAnimator) animator;
            final String propertyName = objectAnim.getPropertyName();
            if ("fillColor".equals(propertyName) || "strokeColor".equals(propertyName)) {
                if (mArgbEvaluator == null) {
                    mArgbEvaluator = new ArgbEvaluator();
                }
                objectAnim.setEvaluator(mArgbEvaluator);
            }
        }
    }

    private void setupAnimatorsForTarget(String name, Animator animator) {
        Object target = mAnimatedVectorState.mVectorDrawable.getTargetByName(name);
        animator.setTarget(target);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setupColorAnimator(animator);
        }
        if (mAnimatedVectorState.mAnimators == null) {
            mAnimatedVectorState.mAnimators = new ArrayList<Animator>();
            mAnimatedVectorState.mTargetNameMap = new ArrayMap<Animator, String>();
        }
        mAnimatedVectorState.mAnimators.add(animator);
        mAnimatedVectorState.mTargetNameMap.put(animator, name);
        if (DBG_ANIMATION_VECTOR_DRAWABLE) {
            Log.v(LOGTAG, "add animator  for target " + name + " " + animator);
        }
    }

    @Override
    public boolean isRunning() {
        if (mDelegateDrawable != null) {
            //noinspection AndroidLintNewApi - Implicit when delegate is non-null.
            return ((AnimatedVectorDrawable) mDelegateDrawable).isRunning();
        }
        return mAnimatedVectorState.mAnimatorSet.isRunning();
    }

    @Override
    public void start() {
        if (mDelegateDrawable != null) {
            //noinspection AndroidLintNewApi - Implicit when delegate is non-null.
            ((AnimatedVectorDrawable) mDelegateDrawable).start();
            return;
        }
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
        if (mDelegateDrawable != null) {
            //noinspection AndroidLintNewApi - Implicit when delegate is non-null.
            ((AnimatedVectorDrawable) mDelegateDrawable).stop();
            return;
        }
        mAnimatedVectorState.mAnimatorSet.end();
    }

    final Callback mCallback = new Callback() {
        @Override
        public void invalidateDrawable(Drawable who) {
            invalidateSelf();
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            scheduleSelf(what, when);
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            unscheduleSelf(what);
        }
    };

    /**
     * A helper function to unregister the Animatable2Compat callback from the platform's
     * Animatable2 callback, while keeping the internal array of callback up to date.
     */
    @RequiresApi(23)
    private static boolean unregisterPlatformCallback(AnimatedVectorDrawable dr,
            Animatable2Compat.AnimationCallback callback) {
        return dr.unregisterAnimationCallback(callback.getPlatformCallback());
    }

    @Override
    public void registerAnimationCallback(@NonNull Animatable2Compat.AnimationCallback
            callback) {
        if (mDelegateDrawable != null) {
            //noinspection AndroidLintNewApi - Implicit when delegate is non-null.
            registerPlatformCallback((AnimatedVectorDrawable) mDelegateDrawable, callback);
            return;
        }

        if (callback == null) {
            return;
        }

        // Add listener accordingly.
        if (mAnimationCallbacks == null) {
            mAnimationCallbacks = new ArrayList<>();
        }

        if (mAnimationCallbacks.contains(callback)) {
            // If this call back is already in, then don't need to append another copy.
            return;
        }

        mAnimationCallbacks.add(callback);

        if (mAnimatorListener == null) {
            // Create a animator listener and trigger the callback events when listener is
            // triggered.
            mAnimatorListener = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    ArrayList<Animatable2Compat.AnimationCallback> tmpCallbacks =
                            new ArrayList<>(mAnimationCallbacks);
                    int size = tmpCallbacks.size();
                    for (int i = 0; i < size; i++) {
                        tmpCallbacks.get(i).onAnimationStart(AnimatedVectorDrawableCompat.this);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    ArrayList<Animatable2Compat.AnimationCallback> tmpCallbacks =
                            new ArrayList<>(mAnimationCallbacks);
                    int size = tmpCallbacks.size();
                    for (int i = 0; i < size; i++) {
                        tmpCallbacks.get(i).onAnimationEnd(AnimatedVectorDrawableCompat.this);
                    }
                }
            };
        }
        mAnimatedVectorState.mAnimatorSet.addListener(mAnimatorListener);
    }

    /**
     * A helper function to register the Animatable2Compat callback on the platform's Animatable2
     * callback.
     */
    @RequiresApi(23)
    private static void registerPlatformCallback(@NonNull AnimatedVectorDrawable avd,
            @NonNull final Animatable2Compat.AnimationCallback callback) {
        avd.registerAnimationCallback(callback.getPlatformCallback());
    }

    /**
     * A helper function to clean up the animator listener in the mAnimatorSet.
     */
    private void removeAnimatorSetListener() {
        if (mAnimatorListener != null) {
            mAnimatedVectorState.mAnimatorSet.removeListener(mAnimatorListener);
            mAnimatorListener = null;
        }
    }

    @Override
    public boolean unregisterAnimationCallback(
            @NonNull Animatable2Compat.AnimationCallback callback) {
        if (mDelegateDrawable != null) {
            //noinspection AndroidLintNewApi - Implicit when delegate is non-null.
            unregisterPlatformCallback((AnimatedVectorDrawable) mDelegateDrawable, callback);
        }

        if (mAnimationCallbacks == null || callback == null) {
            // Nothing to be removed.
            return false;
        }
        boolean removed = mAnimationCallbacks.remove(callback);

        //  When the last call back unregistered, remove the listener accordingly.
        if (mAnimationCallbacks.size() == 0) {
            removeAnimatorSetListener();
        }
        return removed;
    }

    @Override
    public void clearAnimationCallbacks() {
        if (mDelegateDrawable != null) {
            //noinspection AndroidLintNewApi - Implicit when delegate is non-null.
            ((AnimatedVectorDrawable) mDelegateDrawable).clearAnimationCallbacks();
            return;
        }
        removeAnimatorSetListener();
        if (mAnimationCallbacks == null) {
            return;
        }

        mAnimationCallbacks.clear();
    }

    /**
     * Utility function to register callback to Drawable, when the drawable is created from XML and
     * referred in Java code, e.g: ImageView.getDrawable().
     * From API 24 on, the drawable is treated as an AnimatedVectorDrawable.
     * Otherwise, it is treated as AnimatedVectorDrawableCompat.
     */
    public static void registerAnimationCallback(Drawable dr,
            Animatable2Compat.AnimationCallback callback) {
        if (dr == null || callback == null) {
            return;
        }
        if (!(dr instanceof Animatable)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 24) {
            registerPlatformCallback((AnimatedVectorDrawable) dr, callback);
        } else {
            ((AnimatedVectorDrawableCompat) dr).registerAnimationCallback(callback);
        }
    }

    /**
     * Utility function to unregister animation callback from Drawable, when the drawable is
     * created from XML and referred in Java code, e.g: ImageView.getDrawable().
     * From API 24 on, the drawable is treated as an AnimatedVectorDrawable.
     * Otherwise, it is treated as AnimatedVectorDrawableCompat.
     */
    public static boolean unregisterAnimationCallback(Drawable dr,
            Animatable2Compat.AnimationCallback callback) {
        if (dr == null || callback == null) {
            return false;
        }
        if (!(dr instanceof Animatable)) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= 24) {
            return unregisterPlatformCallback((AnimatedVectorDrawable) dr, callback);
        } else {
            return ((AnimatedVectorDrawableCompat) dr).unregisterAnimationCallback(callback);
        }
    }

    /**
     * Utility function to clear animation callbacks from Drawable, when the drawable is
     * created from XML and referred in Java code, e.g: ImageView.getDrawable().
     * From API 24 on, the drawable is treated as an AnimatedVectorDrawable.
     * Otherwise, it is treated as AnimatedVectorDrawableCompat.
     */
    public static void clearAnimationCallbacks(Drawable dr) {
        if (dr == null || !(dr instanceof Animatable)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 24) {
            ((AnimatedVectorDrawable) dr).clearAnimationCallbacks();
        } else {
            ((AnimatedVectorDrawableCompat) dr).clearAnimationCallbacks();
        }

    }
}
