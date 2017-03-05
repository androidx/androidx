/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;

import java.util.Map;

/**
 * This transition captures the layout bounds of target views before and after
 * the scene change and animates those changes during the transition.
 *
 * <p>A ChangeBounds transition can be described in a resource file by using the
 * tag <code>changeBounds</code>, along with the other standard attributes of Transition.</p>
 */
public class ChangeBounds extends Transition {

    private static final String PROPNAME_BOUNDS = "android:changeBounds:bounds";
    private static final String PROPNAME_PARENT = "android:changeBounds:parent";
    private static final String PROPNAME_WINDOW_X = "android:changeBounds:windowX";
    private static final String PROPNAME_WINDOW_Y = "android:changeBounds:windowY";
    private static final String[] sTransitionProperties = {
            PROPNAME_BOUNDS,
            PROPNAME_PARENT,
            PROPNAME_WINDOW_X,
            PROPNAME_WINDOW_Y
    };

    private static final Property<Drawable, PointF> DRAWABLE_ORIGIN_PROPERTY =
            new Property<Drawable, PointF>(PointF.class, "boundsOrigin") {
                private Rect mBounds = new Rect();

                @Override
                public void set(Drawable object, PointF value) {
                    object.copyBounds(mBounds);
                    mBounds.offsetTo(Math.round(value.x), Math.round(value.y));
                    object.setBounds(mBounds);
                }

                @Override
                public PointF get(Drawable object) {
                    object.copyBounds(mBounds);
                    return new PointF(mBounds.left, mBounds.top);
                }
            };

    private int[] mTempLocation = new int[2];
    private boolean mResizeClip = false;
    private boolean mReparent = false;

    private static RectEvaluator sRectEvaluator = new RectEvaluator();

    public ChangeBounds() {
    }

    public ChangeBounds(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Nullable
    @Override
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    /**
     * When <code>resizeClip</code> is true, ChangeBounds resizes the view using the clipBounds
     * instead of changing the dimensions of the view during the animation. When
     * <code>resizeClip</code> is false, ChangeBounds resizes the View by changing its dimensions.
     *
     * <p>When resizeClip is set to true, the clip bounds is modified by ChangeBounds. Therefore,
     * {@link android.transition.ChangeClipBounds} is not compatible with ChangeBounds
     * in this mode.</p>
     *
     * @param resizeClip Used to indicate whether the view bounds should be modified or the
     *                   clip bounds should be modified by ChangeBounds.
     * @see android.view.View#setClipBounds(android.graphics.Rect)
     */
    public void setResizeClip(boolean resizeClip) {
        mResizeClip = resizeClip;
    }

    private void captureValues(TransitionValues values) {
        View view = values.view;

        if (ViewCompat.isLaidOut(view) || view.getWidth() != 0 || view.getHeight() != 0) {
            values.values.put(PROPNAME_BOUNDS, new Rect(view.getLeft(), view.getTop(),
                    view.getRight(), view.getBottom()));
            values.values.put(PROPNAME_PARENT, values.view.getParent());
            if (mReparent) {
                values.view.getLocationInWindow(mTempLocation);
                values.values.put(PROPNAME_WINDOW_X, mTempLocation[0]);
                values.values.put(PROPNAME_WINDOW_Y, mTempLocation[1]);
            }
        }
    }

    @Override
    public void captureStartValues(@NonNull TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(@NonNull TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    private boolean parentMatches(View startParent, View endParent) {
        boolean parentMatches = true;
        if (mReparent) {
            TransitionValues endValues = getMatchedTransitionValues(startParent, true);
            if (endValues == null) {
                parentMatches = startParent == endParent;
            } else {
                parentMatches = endParent == endValues.view;
            }
        }
        return parentMatches;
    }

    @Override
    @Nullable
    public Animator createAnimator(@NonNull final ViewGroup sceneRoot,
            @Nullable TransitionValues startValues, @Nullable TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }
        Map<String, Object> startParentVals = startValues.values;
        Map<String, Object> endParentVals = endValues.values;
        ViewGroup startParent = (ViewGroup) startParentVals.get(PROPNAME_PARENT);
        ViewGroup endParent = (ViewGroup) endParentVals.get(PROPNAME_PARENT);
        if (startParent == null || endParent == null) {
            return null;
        }
        final View view = endValues.view;
        if (parentMatches(startParent, endParent)) {
            Rect startBounds = (Rect) startValues.values.get(PROPNAME_BOUNDS);
            Rect endBounds = (Rect) endValues.values.get(PROPNAME_BOUNDS);
            int startLeft = startBounds.left;
            int endLeft = endBounds.left;
            int startTop = startBounds.top;
            int endTop = endBounds.top;
            int startRight = startBounds.right;
            int endRight = endBounds.right;
            int startBottom = startBounds.bottom;
            int endBottom = endBounds.bottom;
            int startWidth = startRight - startLeft;
            int startHeight = startBottom - startTop;
            int endWidth = endRight - endLeft;
            int endHeight = endBottom - endTop;
            int numChanges = 0;
            if (startWidth != 0 && startHeight != 0 && endWidth != 0 && endHeight != 0) {
                if (startLeft != endLeft) {
                    ++numChanges;
                }
                if (startTop != endTop) {
                    ++numChanges;
                }
                if (startRight != endRight) {
                    ++numChanges;
                }
                if (startBottom != endBottom) {
                    ++numChanges;
                }
            }
            if (numChanges > 0) {
                if (!mResizeClip) {
                    Animator anim;
                    if (startWidth == endWidth && startHeight == endHeight) {
                        view.offsetLeftAndRight(startLeft - view.getLeft());
                        view.offsetTopAndBottom(startTop - view.getTop());
                        Path positionPath = getPathMotion().getPath(0, 0, endLeft - startLeft,
                                endTop - startTop);
                        anim = ObjectAnimatorUtils.ofInt(view, new HorizontalOffsetProperty(),
                                new VerticalOffsetProperty(), positionPath);
                    } else {
                        if (startLeft != endLeft) view.setLeft(startLeft);
                        if (startTop != endTop) view.setTop(startTop);
                        if (startRight != endRight) view.setRight(startRight);
                        if (startBottom != endBottom) view.setBottom(startBottom);
                        ObjectAnimator topLeftAnimator = null;
                        if (startLeft != endLeft || startTop != endTop) {
                            Path topLeftPath = getPathMotion().getPath(startLeft, startTop,
                                    endLeft, endTop);
                            topLeftAnimator = ObjectAnimatorUtils
                                    .ofInt(view, "left", "top", topLeftPath);
                        }
                        ObjectAnimator bottomRightAnimator = null;
                        if (startRight != endRight || startBottom != endBottom) {
                            Path bottomRightPath = getPathMotion().getPath(startRight, startBottom,
                                    endRight, endBottom);
                            bottomRightAnimator = ObjectAnimatorUtils.ofInt(view, "right", "bottom",
                                    bottomRightPath);
                        }
                        anim = TransitionUtils.mergeAnimators(topLeftAnimator,
                                bottomRightAnimator);
                    }
                    if (view.getParent() instanceof ViewGroup) {
                        final ViewGroup parent = (ViewGroup) view.getParent();
                        ViewGroupUtils.suppressLayout(parent, true);
                        TransitionListener transitionListener = new TransitionListenerAdapter() {
                            boolean mCanceled = false;

                            @Override
                            public void onTransitionCancel(@NonNull Transition transition) {
                                ViewGroupUtils.suppressLayout(parent, false);
                                mCanceled = true;
                            }

                            @Override
                            public void onTransitionEnd(@NonNull Transition transition) {
                                if (!mCanceled) {
                                    ViewGroupUtils.suppressLayout(parent, false);
                                }
                            }

                            @Override
                            public void onTransitionPause(@NonNull Transition transition) {
                                ViewGroupUtils.suppressLayout(parent, false);
                            }

                            @Override
                            public void onTransitionResume(@NonNull Transition transition) {
                                ViewGroupUtils.suppressLayout(parent, true);
                            }
                        };
                        addListener(transitionListener);
                    }
                    return anim;
                } else {
                    if (startWidth != endWidth) {
                        view.setRight(endLeft + Math.max(startWidth, endWidth));
                    }
                    if (startHeight != endHeight) {
                        view.setBottom(endTop + Math.max(startHeight, endHeight));
                    }
                    // TODO: don't clobber TX/TY
                    if (startLeft != endLeft) {
                        view.setTranslationX(startLeft - endLeft);
                    }
                    if (startTop != endTop) {
                        view.setTranslationY(startTop - endTop);
                    }
                    // Animate location with translationX/Y and size with clip bounds
                    float transXDelta = endLeft - startLeft;
                    float transYDelta = endTop - startTop;
                    int widthDelta = endWidth - startWidth;
                    int heightDelta = endHeight - startHeight;
                    ObjectAnimator translationAnimator = null;
                    if (transXDelta != 0 || transYDelta != 0) {
                        Path topLeftPath = getPathMotion().getPath(0, 0, transXDelta, transYDelta);
                        translationAnimator = ObjectAnimatorUtils.ofFloat(view, View.TRANSLATION_X,
                                View.TRANSLATION_Y, topLeftPath);
                    }
                    ObjectAnimator clipAnimator = null;
                    if (widthDelta != 0 || heightDelta != 0) {
                        Rect tempStartBounds = new Rect(0, 0, startWidth, startHeight);
                        Rect tempEndBounds = new Rect(0, 0, endWidth, endHeight);
                        clipAnimator = ObjectAnimator.ofObject(view, ViewUtils.CLIP_BOUNDS,
                                sRectEvaluator, tempStartBounds, tempEndBounds);
                    }
                    Animator anim = TransitionUtils.mergeAnimators(translationAnimator,
                            clipAnimator);
                    if (view.getParent() instanceof ViewGroup) {
                        final ViewGroup parent = (ViewGroup) view.getParent();
                        ViewGroupUtils.suppressLayout(parent, true);
                        TransitionListener transitionListener = new TransitionListenerAdapter() {
                            boolean mCanceled = false;

                            @Override
                            public void onTransitionCancel(@NonNull Transition transition) {
                                ViewGroupUtils.suppressLayout(parent, false);
                                mCanceled = true;
                            }

                            @Override
                            public void onTransitionEnd(@NonNull Transition transition) {
                                if (!mCanceled) {
                                    ViewGroupUtils.suppressLayout(parent, false);
                                }
                            }

                            @Override
                            public void onTransitionPause(@NonNull Transition transition) {
                                ViewGroupUtils.suppressLayout(parent, false);
                            }

                            @Override
                            public void onTransitionResume(@NonNull Transition transition) {
                                ViewGroupUtils.suppressLayout(parent, true);
                            }
                        };
                        addListener(transitionListener);
                    }
                    anim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            ViewCompat.setClipBounds(view, null);
                        }
                    });
                    return anim;
                }
            }
        } else {
            int startX = (Integer) startValues.values.get(PROPNAME_WINDOW_X);
            int startY = (Integer) startValues.values.get(PROPNAME_WINDOW_Y);
            int endX = (Integer) endValues.values.get(PROPNAME_WINDOW_X);
            int endY = (Integer) endValues.values.get(PROPNAME_WINDOW_Y);
            // TODO: also handle size changes: check bounds and animate size changes
            if (startX != endX || startY != endY) {
                sceneRoot.getLocationInWindow(mTempLocation);
                Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                view.draw(canvas);
                @SuppressWarnings("deprecation")
                final BitmapDrawable drawable = new BitmapDrawable(bitmap);
                final float transitionAlpha = ViewUtils.getTransitionAlpha(view);
                ViewUtils.setTransitionAlpha(view, 0);
                ViewUtils.getOverlay(sceneRoot).add(drawable);
                Path topLeftPath = getPathMotion().getPath(startX - mTempLocation[0],
                        startY - mTempLocation[1], endX - mTempLocation[0],
                        endY - mTempLocation[1]);
                PropertyValuesHolder origin = PropertyValuesHolderUtils.ofPointF(
                        DRAWABLE_ORIGIN_PROPERTY, topLeftPath);
                ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(drawable, origin);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ViewUtils.getOverlay(sceneRoot).remove(drawable);
                        ViewUtils.setTransitionAlpha(view, transitionAlpha);
                    }
                });
                return anim;
            }
        }
        return null;
    }

    private abstract static class OffsetProperty extends Property<View, Integer> {
        int mPreviousValue;

        OffsetProperty(String name) {
            super(Integer.class, name);
        }

        @Override
        public void set(View view, Integer value) {
            int offset = value - mPreviousValue;
            offsetBy(view, offset);
            mPreviousValue = value;
        }

        @Override
        public Integer get(View object) {
            return null;
        }

        protected abstract void offsetBy(View view, int by);
    }

    private static class HorizontalOffsetProperty extends OffsetProperty {
        HorizontalOffsetProperty() {
            super("offsetLeftAndRight");
        }

        @Override
        protected void offsetBy(View view, int by) {
            ViewCompat.offsetLeftAndRight(view, by);
        }
    }

    private static class VerticalOffsetProperty extends OffsetProperty {
        VerticalOffsetProperty() {
            super("offsetTopAndBottom");
        }

        @Override
        protected void offsetBy(View view, int by) {
            ViewCompat.offsetTopAndBottom(view, by);
        }
    }

}
