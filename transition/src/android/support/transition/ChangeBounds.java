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
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.TypedArrayUtils;
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
    private static final String PROPNAME_CLIP = "android:changeBounds:clip";
    private static final String PROPNAME_PARENT = "android:changeBounds:parent";
    private static final String PROPNAME_WINDOW_X = "android:changeBounds:windowX";
    private static final String PROPNAME_WINDOW_Y = "android:changeBounds:windowY";
    private static final String[] sTransitionProperties = {
            PROPNAME_BOUNDS,
            PROPNAME_CLIP,
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

    private static final Property<ViewBounds, PointF> TOP_LEFT_PROPERTY =
            new Property<ViewBounds, PointF>(PointF.class, "topLeft") {
                @Override
                public void set(ViewBounds viewBounds, PointF topLeft) {
                    viewBounds.setTopLeft(topLeft);
                }

                @Override
                public PointF get(ViewBounds viewBounds) {
                    return null;
                }
            };

    private static final Property<ViewBounds, PointF> BOTTOM_RIGHT_PROPERTY =
            new Property<ViewBounds, PointF>(PointF.class, "bottomRight") {
                @Override
                public void set(ViewBounds viewBounds, PointF bottomRight) {
                    viewBounds.setBottomRight(bottomRight);
                }

                @Override
                public PointF get(ViewBounds viewBounds) {
                    return null;
                }
            };

    private static final Property<View, PointF> BOTTOM_RIGHT_ONLY_PROPERTY =
            new Property<View, PointF>(PointF.class, "bottomRight") {
                @Override
                public void set(View view, PointF bottomRight) {
                    int left = view.getLeft();
                    int top = view.getTop();
                    int right = Math.round(bottomRight.x);
                    int bottom = Math.round(bottomRight.y);
                    ViewUtils.setLeftTopRightBottom(view, left, top, right, bottom);
                }

                @Override
                public PointF get(View view) {
                    return null;
                }
            };

    private static final Property<View, PointF> TOP_LEFT_ONLY_PROPERTY =
            new Property<View, PointF>(PointF.class, "topLeft") {
                @Override
                public void set(View view, PointF topLeft) {
                    int left = Math.round(topLeft.x);
                    int top = Math.round(topLeft.y);
                    int right = view.getRight();
                    int bottom = view.getBottom();
                    ViewUtils.setLeftTopRightBottom(view, left, top, right, bottom);
                }

                @Override
                public PointF get(View view) {
                    return null;
                }
            };

    private static final Property<View, PointF> POSITION_PROPERTY =
            new Property<View, PointF>(PointF.class, "position") {
                @Override
                public void set(View view, PointF topLeft) {
                    int left = Math.round(topLeft.x);
                    int top = Math.round(topLeft.y);
                    int right = left + view.getWidth();
                    int bottom = top + view.getHeight();
                    ViewUtils.setLeftTopRightBottom(view, left, top, right, bottom);
                }

                @Override
                public PointF get(View view) {
                    return null;
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

        TypedArray a = context.obtainStyledAttributes(attrs, Styleable.CHANGE_BOUNDS);
        boolean resizeClip = TypedArrayUtils.getNamedBoolean(a, (XmlResourceParser) attrs,
                "resizeClip", Styleable.ChangeBounds.RESIZE_CLIP, false);
        a.recycle();
        setResizeClip(resizeClip);
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

    /**
     * Returns true when the ChangeBounds will resize by changing the clip bounds during the
     * view animation or false when bounds are changed. The default value is false.
     *
     * @return true when the ChangeBounds will resize by changing the clip bounds during the
     * view animation or false when bounds are changed. The default value is false.
     */
    public boolean getResizeClip() {
        return mResizeClip;
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
            if (mResizeClip) {
                values.values.put(PROPNAME_CLIP, ViewCompat.getClipBounds(view));
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
            final int startLeft = startBounds.left;
            final int endLeft = endBounds.left;
            final int startTop = startBounds.top;
            final int endTop = endBounds.top;
            final int startRight = startBounds.right;
            final int endRight = endBounds.right;
            final int startBottom = startBounds.bottom;
            final int endBottom = endBounds.bottom;
            final int startWidth = startRight - startLeft;
            final int startHeight = startBottom - startTop;
            final int endWidth = endRight - endLeft;
            final int endHeight = endBottom - endTop;
            Rect startClip = (Rect) startValues.values.get(PROPNAME_CLIP);
            Rect endClip = (Rect) endValues.values.get(PROPNAME_CLIP);
            int numChanges = 0;
            if ((startWidth != 0 && startHeight != 0) || (endWidth != 0 && endHeight != 0)) {
                if (startLeft != endLeft || startTop != endTop) ++numChanges;
                if (startRight != endRight || startBottom != endBottom) ++numChanges;
            }
            if ((startClip != null && !startClip.equals(endClip))
                    || (startClip == null && endClip != null)) {
                ++numChanges;
            }
            if (numChanges > 0) {
                Animator anim;
                if (!mResizeClip) {
                    ViewUtils.setLeftTopRightBottom(view, startLeft, startTop, startRight,
                            startBottom);
                    if (numChanges == 2) {
                        if (startWidth == endWidth && startHeight == endHeight) {
                            Path topLeftPath = getPathMotion().getPath(startLeft, startTop, endLeft,
                                    endTop);
                            anim = ObjectAnimatorUtils.ofPointF(view, POSITION_PROPERTY,
                                    topLeftPath);
                        } else {
                            final ViewBounds viewBounds = new ViewBounds(view);
                            Path topLeftPath = getPathMotion().getPath(startLeft, startTop,
                                    endLeft, endTop);
                            ObjectAnimator topLeftAnimator = ObjectAnimatorUtils
                                    .ofPointF(viewBounds, TOP_LEFT_PROPERTY, topLeftPath);

                            Path bottomRightPath = getPathMotion().getPath(startRight, startBottom,
                                    endRight, endBottom);
                            ObjectAnimator bottomRightAnimator = ObjectAnimatorUtils.ofPointF(
                                    viewBounds, BOTTOM_RIGHT_PROPERTY, bottomRightPath);
                            AnimatorSet set = new AnimatorSet();
                            set.playTogether(topLeftAnimator, bottomRightAnimator);
                            anim = set;
                            set.addListener(new AnimatorListenerAdapter() {
                                // We need a strong reference to viewBounds until the
                                // animator ends (The ObjectAnimator holds only a weak reference).
                                @SuppressWarnings("unused")
                                private ViewBounds mViewBounds = viewBounds;
                            });
                        }
                    } else if (startLeft != endLeft || startTop != endTop) {
                        Path topLeftPath = getPathMotion().getPath(startLeft, startTop,
                                endLeft, endTop);
                        anim = ObjectAnimatorUtils.ofPointF(view, TOP_LEFT_ONLY_PROPERTY,
                                topLeftPath);
                    } else {
                        Path bottomRight = getPathMotion().getPath(startRight, startBottom,
                                endRight, endBottom);
                        anim = ObjectAnimatorUtils.ofPointF(view, BOTTOM_RIGHT_ONLY_PROPERTY,
                                bottomRight);
                    }
                } else {
                    int maxWidth = Math.max(startWidth, endWidth);
                    int maxHeight = Math.max(startHeight, endHeight);

                    ViewUtils.setLeftTopRightBottom(view, startLeft, startTop, startLeft + maxWidth,
                            startTop + maxHeight);

                    ObjectAnimator positionAnimator = null;
                    if (startLeft != endLeft || startTop != endTop) {
                        Path topLeftPath = getPathMotion().getPath(startLeft, startTop, endLeft,
                                endTop);
                        positionAnimator = ObjectAnimatorUtils.ofPointF(view, POSITION_PROPERTY,
                                topLeftPath);
                    }
                    final Rect finalClip = endClip;
                    if (startClip == null) {
                        startClip = new Rect(0, 0, startWidth, startHeight);
                    }
                    if (endClip == null) {
                        endClip = new Rect(0, 0, endWidth, endHeight);
                    }
                    ObjectAnimator clipAnimator = null;
                    if (!startClip.equals(endClip)) {
                        ViewCompat.setClipBounds(view, startClip);
                        clipAnimator = ObjectAnimator.ofObject(view, "clipBounds", sRectEvaluator,
                                startClip, endClip);
                        clipAnimator.addListener(new AnimatorListenerAdapter() {
                            private boolean mIsCanceled;

                            @Override
                            public void onAnimationCancel(Animator animation) {
                                mIsCanceled = true;
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (!mIsCanceled) {
                                    ViewCompat.setClipBounds(view, finalClip);
                                    ViewUtils.setLeftTopRightBottom(view, endLeft, endTop, endRight,
                                            endBottom);
                                }
                            }
                        });
                    }
                    anim = TransitionUtils.mergeAnimators(positionAnimator,
                            clipAnimator);
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
                            transition.removeListener(this);
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
                @SuppressWarnings("deprecation") final BitmapDrawable drawable = new BitmapDrawable(
                        bitmap);
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

    private static class ViewBounds {

        private int mLeft;
        private int mTop;
        private int mRight;
        private int mBottom;
        private View mView;
        private int mTopLeftCalls;
        private int mBottomRightCalls;

        ViewBounds(View view) {
            mView = view;
        }

        void setTopLeft(PointF topLeft) {
            mLeft = Math.round(topLeft.x);
            mTop = Math.round(topLeft.y);
            mTopLeftCalls++;
            if (mTopLeftCalls == mBottomRightCalls) {
                setLeftTopRightBottom();
            }
        }

        void setBottomRight(PointF bottomRight) {
            mRight = Math.round(bottomRight.x);
            mBottom = Math.round(bottomRight.y);
            mBottomRightCalls++;
            if (mTopLeftCalls == mBottomRightCalls) {
                setLeftTopRightBottom();
            }
        }

        private void setLeftTopRightBottom() {
            ViewUtils.setLeftTopRightBottom(mView, mLeft, mTop, mRight, mBottom);
            mTopLeftCalls = 0;
            mBottomRightCalls = 0;
        }

    }

}
