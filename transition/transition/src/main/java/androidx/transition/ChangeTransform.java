/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.TypedArrayUtils;
import androidx.core.view.ViewCompat;

import org.xmlpull.v1.XmlPullParser;

/**
 * This Transition captures scale and rotation for Views before and after the
 * scene change and animates those changes during the transition.
 *
 * A change in parent is handled as well by capturing the transforms from
 * the parent before and after the scene change and animating those during the
 * transition.
 */
public class ChangeTransform extends Transition {

    private static final String PROPNAME_MATRIX = "android:changeTransform:matrix";
    private static final String PROPNAME_TRANSFORMS = "android:changeTransform:transforms";
    private static final String PROPNAME_PARENT = "android:changeTransform:parent";
    private static final String PROPNAME_PARENT_MATRIX = "android:changeTransform:parentMatrix";
    private static final String PROPNAME_INTERMEDIATE_PARENT_MATRIX =
            "android:changeTransform:intermediateParentMatrix";
    private static final String PROPNAME_INTERMEDIATE_MATRIX =
            "android:changeTransform:intermediateMatrix";

    private static final String[] sTransitionProperties = {
            PROPNAME_MATRIX,
            PROPNAME_TRANSFORMS,
            PROPNAME_PARENT_MATRIX,
    };

    /**
     * This property sets the animation matrix properties that are not translations.
     */
    private static final Property<PathAnimatorMatrix, float[]> NON_TRANSLATIONS_PROPERTY =
            new Property<PathAnimatorMatrix, float[]>(float[].class, "nonTranslations") {
                @Override
                public float[] get(PathAnimatorMatrix object) {
                    return null;
                }

                @Override
                public void set(PathAnimatorMatrix object, float[] value) {
                    object.setValues(value);
                }
            };

    /**
     * This property sets the translation animation matrix properties.
     */
    private static final Property<PathAnimatorMatrix, PointF> TRANSLATIONS_PROPERTY =
            new Property<PathAnimatorMatrix, PointF>(PointF.class, "translations") {
                @Override
                public PointF get(PathAnimatorMatrix object) {
                    return null;
                }

                @Override
                public void set(PathAnimatorMatrix object, PointF value) {
                    object.setTranslation(value);
                }
            };

    /**
     * Newer platforms suppress view removal at the beginning of the animation.
     */
    private static final boolean SUPPORTS_VIEW_REMOVAL_SUPPRESSION = Build.VERSION.SDK_INT >= 21;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mUseOverlay = true;
    private boolean mReparent = true;
    private Matrix mTempMatrix = new Matrix();

    public ChangeTransform() {
    }

    public ChangeTransform(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, Styleable.CHANGE_TRANSFORM);
        mUseOverlay = TypedArrayUtils.getNamedBoolean(a, (XmlPullParser) attrs,
                "reparentWithOverlay", Styleable.ChangeTransform.REPARENT_WITH_OVERLAY, true);
        mReparent = TypedArrayUtils.getNamedBoolean(a, (XmlPullParser) attrs,
                "reparent", Styleable.ChangeTransform.REPARENT, true);
        a.recycle();
    }

    /**
     * Returns whether changes to parent should use an overlay or not. When the parent
     * change doesn't use an overlay, it affects the transforms of the child. The
     * default value is <code>true</code>.
     *
     * <p>Note: when Overlays are not used when a parent changes, a view can be clipped when
     * it moves outside the bounds of its parent. Setting
     * {@link android.view.ViewGroup#setClipChildren(boolean)} and
     * {@link android.view.ViewGroup#setClipToPadding(boolean)} can help. Also, when
     * Overlays are not used and the parent is animating its location, the position of the
     * child view will be relative to its parent's final position, so it may appear to "jump"
     * at the beginning.</p>
     *
     * @return <code>true</code> when a changed parent should execute the transition
     * inside the scene root's overlay or <code>false</code> if a parent change only
     * affects the transform of the transitioning view.
     */
    public boolean getReparentWithOverlay() {
        return mUseOverlay;
    }

    /**
     * Sets whether changes to parent should use an overlay or not. When the parent
     * change doesn't use an overlay, it affects the transforms of the child. The
     * default value is <code>true</code>.
     *
     * <p>Note: when Overlays are not used when a parent changes, a view can be clipped when
     * it moves outside the bounds of its parent. Setting
     * {@link android.view.ViewGroup#setClipChildren(boolean)} and
     * {@link android.view.ViewGroup#setClipToPadding(boolean)} can help. Also, when
     * Overlays are not used and the parent is animating its location, the position of the
     * child view will be relative to its parent's final position, so it may appear to "jump"
     * at the beginning.</p>
     *
     * @param reparentWithOverlay <code>true</code> when a changed parent should execute the
     *                            transition inside the scene root's overlay or <code>false</code>
     *                            if a parent change only affects the transform of the
     *                            transitioning view.
     */
    public void setReparentWithOverlay(boolean reparentWithOverlay) {
        mUseOverlay = reparentWithOverlay;
    }

    /**
     * Returns whether parent changes will be tracked by the ChangeTransform. If parent
     * changes are tracked, then the transform will adjust to the transforms of the
     * different parents. If they aren't tracked, only the transforms of the transitioning
     * view will be tracked. Default is true.
     *
     * @return whether parent changes will be tracked by the ChangeTransform.
     */
    public boolean getReparent() {
        return mReparent;
    }

    /**
     * Sets whether parent changes will be tracked by the ChangeTransform. If parent
     * changes are tracked, then the transform will adjust to the transforms of the
     * different parents. If they aren't tracked, only the transforms of the transitioning
     * view will be tracked. Default is true.
     *
     * @param reparent Set to true to track parent changes or false to only track changes
     *                 of the transitioning view without considering the parent change.
     */
    public void setReparent(boolean reparent) {
        mReparent = reparent;
    }

    @Override
    @NonNull
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    private void captureValues(TransitionValues transitionValues) {
        View view = transitionValues.view;
        if (view.getVisibility() == View.GONE) {
            return;
        }
        transitionValues.values.put(PROPNAME_PARENT, view.getParent());
        Transforms transforms = new Transforms(view);
        transitionValues.values.put(PROPNAME_TRANSFORMS, transforms);
        Matrix matrix = view.getMatrix();
        if (matrix == null || matrix.isIdentity()) {
            matrix = null;
        } else {
            matrix = new Matrix(matrix);
        }
        transitionValues.values.put(PROPNAME_MATRIX, matrix);
        if (mReparent) {
            Matrix parentMatrix = new Matrix();
            ViewGroup parent = (ViewGroup) view.getParent();
            ViewUtils.transformMatrixToGlobal(parent, parentMatrix);
            parentMatrix.preTranslate(-parent.getScrollX(), -parent.getScrollY());
            transitionValues.values.put(PROPNAME_PARENT_MATRIX, parentMatrix);
            transitionValues.values.put(PROPNAME_INTERMEDIATE_MATRIX,
                    view.getTag(R.id.transition_transform));
            transitionValues.values.put(PROPNAME_INTERMEDIATE_PARENT_MATRIX,
                    view.getTag(R.id.parent_matrix));
        }
    }

    @Override
    public void captureStartValues(@NonNull TransitionValues transitionValues) {
        captureValues(transitionValues);
        if (!SUPPORTS_VIEW_REMOVAL_SUPPRESSION) {
            // We still don't know if the view is removed or not, but we need to do this here, or
            // the view will be actually removed, resulting in flickering at the beginning of the
            // animation. We are canceling this afterwards.
            ((ViewGroup) transitionValues.view.getParent()).startViewTransition(
                    transitionValues.view);
        }
    }

    @Override
    public void captureEndValues(@NonNull TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Nullable
    @Override
    public Animator createAnimator(@NonNull ViewGroup sceneRoot,
            @Nullable TransitionValues startValues,
            @Nullable TransitionValues endValues) {
        if (startValues == null || endValues == null
                || !startValues.values.containsKey(PROPNAME_PARENT)
                || !endValues.values.containsKey(PROPNAME_PARENT)) {
            return null;
        }

        ViewGroup startParent = (ViewGroup) startValues.values.get(PROPNAME_PARENT);
        ViewGroup endParent = (ViewGroup) endValues.values.get(PROPNAME_PARENT);
        boolean handleParentChange = mReparent && !parentsMatch(startParent, endParent);

        Matrix startMatrix = (Matrix) startValues.values.get(PROPNAME_INTERMEDIATE_MATRIX);
        if (startMatrix != null) {
            startValues.values.put(PROPNAME_MATRIX, startMatrix);
        }

        Matrix startParentMatrix = (Matrix)
                startValues.values.get(PROPNAME_INTERMEDIATE_PARENT_MATRIX);
        if (startParentMatrix != null) {
            startValues.values.put(PROPNAME_PARENT_MATRIX, startParentMatrix);
        }

        // First handle the parent change:
        if (handleParentChange) {
            setMatricesForParent(startValues, endValues);
        }

        // Next handle the normal matrix transform:
        ObjectAnimator transformAnimator = createTransformAnimator(startValues, endValues,
                handleParentChange);

        if (handleParentChange && transformAnimator != null && mUseOverlay) {
            createGhostView(sceneRoot, startValues, endValues);
        } else if (!SUPPORTS_VIEW_REMOVAL_SUPPRESSION) {
            // We didn't need to suppress the view removal in this case. Cancel the suppression.
            startParent.endViewTransition(startValues.view);
        }

        return transformAnimator;
    }

    private ObjectAnimator createTransformAnimator(TransitionValues startValues,
            TransitionValues endValues, final boolean handleParentChange) {
        Matrix startMatrix = (Matrix) startValues.values.get(PROPNAME_MATRIX);
        Matrix endMatrix = (Matrix) endValues.values.get(PROPNAME_MATRIX);

        if (startMatrix == null) {
            startMatrix = MatrixUtils.IDENTITY_MATRIX;
        }

        if (endMatrix == null) {
            endMatrix = MatrixUtils.IDENTITY_MATRIX;
        }

        if (startMatrix.equals(endMatrix)) {
            return null;
        }

        final Transforms transforms = (Transforms) endValues.values.get(PROPNAME_TRANSFORMS);

        // clear the transform properties so that we can use the animation matrix instead
        final View view = endValues.view;
        setIdentityTransforms(view);

        final float[] startMatrixValues = new float[9];
        startMatrix.getValues(startMatrixValues);
        final float[] endMatrixValues = new float[9];
        endMatrix.getValues(endMatrixValues);
        final PathAnimatorMatrix pathAnimatorMatrix =
                new PathAnimatorMatrix(view, startMatrixValues);

        PropertyValuesHolder valuesProperty = PropertyValuesHolder.ofObject(
                NON_TRANSLATIONS_PROPERTY, new FloatArrayEvaluator(new float[9]),
                startMatrixValues, endMatrixValues);
        Path path = getPathMotion().getPath(startMatrixValues[Matrix.MTRANS_X],
                startMatrixValues[Matrix.MTRANS_Y], endMatrixValues[Matrix.MTRANS_X],
                endMatrixValues[Matrix.MTRANS_Y]);
        PropertyValuesHolder translationProperty = PropertyValuesHolderUtils.ofPointF(
                TRANSLATIONS_PROPERTY, path);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(pathAnimatorMatrix,
                valuesProperty, translationProperty);

        Listener listener = new Listener(view, transforms, pathAnimatorMatrix, endMatrix,
                handleParentChange, mUseOverlay);

        animator.addListener(listener);
        animator.addPauseListener(listener);
        return animator;
    }

    private boolean parentsMatch(ViewGroup startParent, ViewGroup endParent) {
        boolean parentsMatch = false;
        if (!isValidTarget(startParent) || !isValidTarget(endParent)) {
            parentsMatch = startParent == endParent;
        } else {
            TransitionValues endValues = getMatchedTransitionValues(startParent, true);
            if (endValues != null) {
                parentsMatch = endParent == endValues.view;
            }
        }
        return parentsMatch;
    }

    private void createGhostView(final ViewGroup sceneRoot, TransitionValues startValues,
            TransitionValues endValues) {
        View view = endValues.view;

        Matrix endMatrix = (Matrix) endValues.values.get(PROPNAME_PARENT_MATRIX);
        Matrix localEndMatrix = new Matrix(endMatrix);
        ViewUtils.transformMatrixToLocal(sceneRoot, localEndMatrix);

        GhostView ghostView = GhostViewUtils.addGhost(view, sceneRoot, localEndMatrix);
        if (ghostView == null) {
            return;
        }
        // Ask GhostView to actually remove the start view when it starts drawing the animation.
        ghostView.reserveEndViewTransition((ViewGroup) startValues.values.get(PROPNAME_PARENT),
                startValues.view);

        Transition outerTransition = this;
        while (outerTransition.mParent != null) {
            outerTransition = outerTransition.mParent;
        }

        GhostListener listener = new GhostListener(view, ghostView);
        outerTransition.addListener(listener);

        // We cannot do this for older platforms or it invalidates the view and results in
        // flickering, but the view will still be invisible by actually removing it from the parent.
        if (SUPPORTS_VIEW_REMOVAL_SUPPRESSION) {
            if (startValues.view != endValues.view) {
                ViewUtils.setTransitionAlpha(startValues.view, 0);
            }
            ViewUtils.setTransitionAlpha(view, 1);
        }
    }

    private void setMatricesForParent(TransitionValues startValues, TransitionValues endValues) {
        Matrix endParentMatrix = (Matrix) endValues.values.get(PROPNAME_PARENT_MATRIX);
        endValues.view.setTag(R.id.parent_matrix, endParentMatrix);

        Matrix toLocal = mTempMatrix;
        toLocal.reset();
        endParentMatrix.invert(toLocal);

        Matrix startLocal = (Matrix) startValues.values.get(PROPNAME_MATRIX);
        if (startLocal == null) {
            startLocal = new Matrix();
            startValues.values.put(PROPNAME_MATRIX, startLocal);
        }

        Matrix startParentMatrix = (Matrix) startValues.values.get(PROPNAME_PARENT_MATRIX);
        startLocal.postConcat(startParentMatrix);
        startLocal.postConcat(toLocal);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void setIdentityTransforms(View view) {
        setTransforms(view, 0, 0, 0, 1, 1, 0, 0, 0);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void setTransforms(View view, float translationX, float translationY,
            float translationZ, float scaleX, float scaleY, float rotationX,
            float rotationY, float rotationZ) {
        view.setTranslationX(translationX);
        view.setTranslationY(translationY);
        ViewCompat.setTranslationZ(view, translationZ);
        view.setScaleX(scaleX);
        view.setScaleY(scaleY);
        view.setRotationX(rotationX);
        view.setRotationY(rotationY);
        view.setRotation(rotationZ);
    }

    private static class Transforms {

        final float mTranslationX;
        final float mTranslationY;
        final float mTranslationZ;
        final float mScaleX;
        final float mScaleY;
        final float mRotationX;
        final float mRotationY;
        final float mRotationZ;

        Transforms(View view) {
            mTranslationX = view.getTranslationX();
            mTranslationY = view.getTranslationY();
            mTranslationZ = ViewCompat.getTranslationZ(view);
            mScaleX = view.getScaleX();
            mScaleY = view.getScaleY();
            mRotationX = view.getRotationX();
            mRotationY = view.getRotationY();
            mRotationZ = view.getRotation();
        }

        public void restore(View view) {
            setTransforms(view, mTranslationX, mTranslationY, mTranslationZ, mScaleX, mScaleY,
                    mRotationX, mRotationY, mRotationZ);
        }

        @Override
        public boolean equals(Object that) {
            if (!(that instanceof Transforms)) {
                return false;
            }
            Transforms thatTransform = (Transforms) that;
            return thatTransform.mTranslationX == mTranslationX
                    && thatTransform.mTranslationY == mTranslationY
                    && thatTransform.mTranslationZ == mTranslationZ
                    && thatTransform.mScaleX == mScaleX
                    && thatTransform.mScaleY == mScaleY
                    && thatTransform.mRotationX == mRotationX
                    && thatTransform.mRotationY == mRotationY
                    && thatTransform.mRotationZ == mRotationZ;
        }

        @Override
        public int hashCode() {
            int code = mTranslationX != +0.0f ? Float.floatToIntBits(mTranslationX) : 0;
            code = 31 * code + (mTranslationY != +0.0f ? Float.floatToIntBits(mTranslationY) : 0);
            code = 31 * code + (mTranslationZ != +0.0f ? Float.floatToIntBits(mTranslationZ) : 0);
            code = 31 * code + (mScaleX != +0.0f ? Float.floatToIntBits(mScaleX) : 0);
            code = 31 * code + (mScaleY != +0.0f ? Float.floatToIntBits(mScaleY) : 0);
            code = 31 * code + (mRotationX != +0.0f ? Float.floatToIntBits(mRotationX) : 0);
            code = 31 * code + (mRotationY != +0.0f ? Float.floatToIntBits(mRotationY) : 0);
            code = 31 * code + (mRotationZ != +0.0f ? Float.floatToIntBits(mRotationZ) : 0);
            return code;
        }

    }

    private static class GhostListener extends TransitionListenerAdapter {

        private View mView;
        private GhostView mGhostView;

        GhostListener(View view, GhostView ghostView) {
            mView = view;
            mGhostView = ghostView;
        }

        @Override
        public void onTransitionEnd(@NonNull Transition transition) {
            transition.removeListener(this);
            GhostViewUtils.removeGhost(mView);
            mView.setTag(R.id.transition_transform, null);
            mView.setTag(R.id.parent_matrix, null);
        }

        @Override
        public void onTransitionPause(@NonNull Transition transition) {
            mGhostView.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onTransitionResume(@NonNull Transition transition) {
            mGhostView.setVisibility(View.VISIBLE);
        }

    }

    /**
     * PathAnimatorMatrix allows the translations and the rest of the matrix to be set
     * separately. This allows the PathMotion to affect the translations while scale
     * and rotation are evaluated separately.
     */
    private static class PathAnimatorMatrix {

        private final Matrix mMatrix = new Matrix();
        private final View mView;
        private final float[] mValues;
        private float mTranslationX;
        private float mTranslationY;

        PathAnimatorMatrix(View view, float[] values) {
            mView = view;
            mValues = values.clone();
            mTranslationX = mValues[Matrix.MTRANS_X];
            mTranslationY = mValues[Matrix.MTRANS_Y];
            setAnimationMatrix();
        }

        void setValues(float[] values) {
            System.arraycopy(values, 0, mValues, 0, values.length);
            setAnimationMatrix();
        }

        void setTranslation(PointF translation) {
            mTranslationX = translation.x;
            mTranslationY = translation.y;
            setAnimationMatrix();
        }

        private void setAnimationMatrix() {
            mValues[Matrix.MTRANS_X] = mTranslationX;
            mValues[Matrix.MTRANS_Y] = mTranslationY;
            mMatrix.setValues(mValues);
            ViewUtils.setAnimationMatrix(mView, mMatrix);
        }

        Matrix getMatrix() {
            return mMatrix;
        }
    }

    private static class Listener extends AnimatorListenerAdapter {
        private boolean mIsCanceled;
        private final Matrix mTempMatrix = new Matrix();
        private final boolean mHandleParentChange;
        private final boolean mUseOverlay;
        private final View mView;
        private final Transforms mTransforms;
        private final PathAnimatorMatrix mPathAnimatorMatrix;
        private final Matrix mEndMatrix;

        Listener(View view, Transforms transforms, PathAnimatorMatrix pathAnimatorMatrix,
                Matrix endMatrix, boolean handleParentChange, boolean useOverlay) {
            mHandleParentChange = handleParentChange;
            mUseOverlay = useOverlay;
            mView = view;
            mTransforms = transforms;
            mPathAnimatorMatrix = pathAnimatorMatrix;
            mEndMatrix = endMatrix;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            mIsCanceled = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (!mIsCanceled) {
                if (mHandleParentChange && mUseOverlay) {
                    setCurrentMatrix(mEndMatrix);
                } else {
                    mView.setTag(R.id.transition_transform, null);
                    mView.setTag(R.id.parent_matrix, null);
                }
            }
            ViewUtils.setAnimationMatrix(mView, null);
            mTransforms.restore(mView);
        }

        @Override
        public void onAnimationPause(Animator animation) {
            Matrix currentMatrix = mPathAnimatorMatrix.getMatrix();
            setCurrentMatrix(currentMatrix);
        }

        @Override
        public void onAnimationResume(Animator animation) {
            setIdentityTransforms(mView);
        }

        private void setCurrentMatrix(Matrix currentMatrix) {
            mTempMatrix.set(currentMatrix);
            mView.setTag(R.id.transition_transform, mTempMatrix);
            mTransforms.restore(mView);
        }
    }
}
