/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v17.leanback.app;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.RectEvaluator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionValues;

import java.util.Map;

/**
 * This is a replacement of android.transition.ChangeBounds that treat reparent
 * views slightly differently:  see "PATCH" in the code.
 */
class ChangeBoundsKitKat extends ChangeBounds {

    private static final String PROPNAME_PARENT = "android:changeBounds:parent";
    private static final String PROPNAME_WINDOW_X = "android:changeBounds:windowX";
    private static final String PROPNAME_WINDOW_Y = "android:changeBounds:windowY";

    int[] tempLocation = new int[2];
    boolean mReparent = false;
    private static final String LOG_TAG = "ChangeBoundsKitKat";

    private static RectEvaluator sRectEvaluator = new RectEvaluator();

    @Override
    public void setReparent(boolean reparent) {
        super.setReparent(reparent);
        mReparent = reparent;
    }

    @Override
    public Animator createAnimator(final ViewGroup sceneRoot, TransitionValues startValues,
            TransitionValues endValues) {
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
        boolean parentsEqual = (startParent == endParent) ||
                (startParent.getId() == endParent.getId());
        // TODO: Might want reparenting to be separate/subclass transition, or at least
        // triggered by a property on ChangeBounds. Otherwise, we're forcing the requirement that
        // all parents in layouts have IDs to avoid layout-inflation resulting in a side-effect
        // of reparenting the views.
        if (!mReparent || parentsEqual) {
            return super.createAnimator(sceneRoot, startValues, endValues);
        } else {
            int startX = (Integer) startValues.values.get(PROPNAME_WINDOW_X);
            int startY = (Integer) startValues.values.get(PROPNAME_WINDOW_Y);
            int endX = (Integer) endValues.values.get(PROPNAME_WINDOW_X);
            int endY = (Integer) endValues.values.get(PROPNAME_WINDOW_Y);
            // TODO: also handle size changes: check bounds and animate size changes
            if (startX != endX || startY != endY) {
                sceneRoot.getLocationInWindow(tempLocation);
                Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                view.draw(canvas);
                final BitmapDrawable drawable = new BitmapDrawable(bitmap);
                view.setVisibility(View.INVISIBLE);
                sceneRoot.getOverlay().add(drawable);
                Rect startBounds1 = new Rect(startX - tempLocation[0], startY - tempLocation[1],
                        startX - tempLocation[0] + view.getWidth(),
                        startY - tempLocation[1] + view.getHeight());
                // PATCH : initialize the startBounds immediately so that the bitmap
                // will show up immediately without waiting start delay.
                drawable.setBounds(startBounds1);
                Rect endBounds1 = new Rect(endX - tempLocation[0], endY - tempLocation[1],
                        endX - tempLocation[0] + view.getWidth(),
                        endY - tempLocation[1] + view.getHeight());
                ObjectAnimator anim = ObjectAnimator.ofObject(drawable, "bounds",
                        sRectEvaluator, startBounds1, endBounds1);
                // PATCH : switch back to view when whole transition finishes.
                TransitionListener transitionListener = new TransitionListener() {
                    @Override
                    public void onTransitionCancel(Transition transition) {
                    }

                    @Override
                    public void onTransitionEnd(Transition transition) {
                        sceneRoot.getOverlay().remove(drawable);
                        view.setVisibility(View.VISIBLE);
                        removeListener(this);
                    }

                    @Override
                    public void onTransitionPause(Transition transition) {
                    }

                    @Override
                    public void onTransitionResume(Transition transition) {
                    }

                    @Override
                    public void onTransitionStart(Transition transition) {
                    }
                };
                addListener(transitionListener);
                return anim;
            }
        }
        return null;
    }
}