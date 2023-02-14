/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.constraintlayout.motion.widget;

import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.SharedValues;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Container for ViewTransitions. It dispatches the run of a ViewTransition.
 * It receives animate calls
 */
public class ViewTransitionController {
    private final MotionLayout mMotionLayout;
    private ArrayList<ViewTransition> mViewTransitions = new ArrayList<>();
    private HashSet<View> mRelatedViews;
    private String mTAG = "ViewTransitionController";

    public ViewTransitionController(MotionLayout layout) {
        mMotionLayout = layout;
    }

    /**
     * Add a ViewTransition
     * @param viewTransition
     */
    public void add(ViewTransition viewTransition) {
        mViewTransitions.add(viewTransition);
        mRelatedViews = null;

        if (viewTransition.getStateTransition() == ViewTransition.ONSTATE_SHARED_VALUE_SET) {
            listenForSharedVariable(viewTransition, true);
        } else if (viewTransition.getStateTransition()
                == ViewTransition.ONSTATE_SHARED_VALUE_UNSET) {
            listenForSharedVariable(viewTransition, false);
        }
    }

    void remove(int id) {
        ViewTransition del = null;
        for (ViewTransition viewTransition : mViewTransitions) {
            if (viewTransition.getId() == id) {
                del = viewTransition;
                break;
            }
        }
        if (del != null) {
            mRelatedViews = null;
            mViewTransitions.remove(del);
        }
    }

    private void viewTransition(ViewTransition vt, View... view) {
        int currentId = mMotionLayout.getCurrentState();
        if (vt.mViewTransitionMode != ViewTransition.VIEWTRANSITIONMODE_NOSTATE) {
            if (currentId == -1) {
                Log.w(mTAG, "No support for ViewTransition within transition yet. Currently: "
                        + mMotionLayout.toString());
                return;
            }
            ConstraintSet current = mMotionLayout.getConstraintSet(currentId);
            if (current == null) {
                return;
            }
            vt.applyTransition(this, mMotionLayout, currentId, current, view);
        } else {
            vt.applyTransition(this, mMotionLayout, currentId, null, view);
        }
    }

    void enableViewTransition(int id, boolean enable) {
        for (ViewTransition viewTransition : mViewTransitions) {
            if (viewTransition.getId() == id) {
                viewTransition.setEnabled(enable);
                break;
            }
        }
    }

    boolean isViewTransitionEnabled(int id) {
        for (ViewTransition viewTransition : mViewTransitions) {
            if (viewTransition.getId() == id) {
                return viewTransition.isEnabled();
            }
        }
        return false;
    }

    /**
     * Support call from MotionLayout.viewTransition
     *
     * @param id    the id of a ViewTransition
     * @param views the list of views to transition simultaneously
     */
    void viewTransition(int id, View... views) {
        ViewTransition vt = null;
        ArrayList<View> list = new ArrayList<>();
        for (ViewTransition viewTransition : mViewTransitions) {
            if (viewTransition.getId() == id) {
                vt = viewTransition;
                for (View view : views) {
                    if (viewTransition.checkTags(view)) {
                        list.add(view);
                    }
                }
                if (!list.isEmpty()) {
                    viewTransition(vt, list.toArray(new View[0]));
                    list.clear();
                }
            }
        }
        if (vt == null) {
            Log.e(mTAG, " Could not find ViewTransition");
            return;
        }
    }

    /**
     * this gets Touch events on the MotionLayout and can fire transitions on down or up
     *
     * @param event
     */
    void touchEvent(MotionEvent event) {
        int currentId = mMotionLayout.getCurrentState();
        if (currentId == -1) {
            return;
        }
        if (mRelatedViews == null) {
            mRelatedViews = new HashSet<>();
            for (ViewTransition viewTransition : mViewTransitions) {
                int count = mMotionLayout.getChildCount();
                for (int i = 0; i < count; i++) {
                    View view = mMotionLayout.getChildAt(i);
                    if (viewTransition.matchesView(view)) {
                        @SuppressWarnings("unused") int id = view.getId();
                        mRelatedViews.add(view);
                    }
                }
            }
        }

        float x = event.getX();
        float y = event.getY();
        Rect rec = new Rect();
        int action = event.getAction();
        if (mAnimations != null && !mAnimations.isEmpty()) {
            for (ViewTransition.Animate animation : mAnimations) {
                animation.reactTo(action, x, y);
            }
        }
        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_DOWN:

                ConstraintSet current = mMotionLayout.getConstraintSet(currentId);
                for (ViewTransition viewTransition : mViewTransitions) {
                    if (viewTransition.supports(action)) {
                        for (View view : mRelatedViews) {
                            if (!viewTransition.matchesView(view)) {
                                continue;
                            }
                            view.getHitRect(rec);
                            if (rec.contains((int) x, (int) y)) {
                                viewTransition.applyTransition(this, mMotionLayout,
                                        currentId, current, view);
                            }

                        }
                    }
                }
                break;
        }
    }

    ArrayList<ViewTransition.Animate> mAnimations;
    ArrayList<ViewTransition.Animate> mRemoveList = new ArrayList<>();

    void addAnimation(ViewTransition.Animate animation) {
        if (mAnimations == null) {
            mAnimations = new ArrayList<>();
        }
        mAnimations.add(animation);
    }

    void removeAnimation(ViewTransition.Animate animation) {
        mRemoveList.add(animation);
    }

    /**
     * Called by motionLayout during draw to allow ViewTransitions to asynchronously animate
     */
    void animate() {
        if (mAnimations == null) {
            return;
        }
        for (ViewTransition.Animate animation : mAnimations) {
            animation.mutate();
        }
        mAnimations.removeAll(mRemoveList);
        mRemoveList.clear();
        if (mAnimations.isEmpty()) {
            mAnimations = null;
        }
    }

    void invalidate() {
        mMotionLayout.invalidate();
    }

    boolean applyViewTransition(int viewTransitionId, MotionController motionController) {
        for (ViewTransition viewTransition : mViewTransitions) {
            if (viewTransition.getId() == viewTransitionId) {
                viewTransition.mKeyFrames.addAllFrames(motionController);
                return true;
            }
        }
        return false;
    }

    private void listenForSharedVariable(ViewTransition viewTransition, boolean isSet) {
        int listen_for_id = viewTransition.getSharedValueID();
        int listen_for_value = viewTransition.getSharedValue();

        ConstraintLayout.getSharedValues().addListener(viewTransition.getSharedValueID(),
                new SharedValues.SharedValuesListener() {
                @Override
                public void onNewValue(int id, int value, int oldValue) {
                    int current_value = viewTransition.getSharedValueCurrent();
                    viewTransition.setSharedValueCurrent(value);
                    if (listen_for_id == id && current_value != value) {
                        if (isSet) {
                            if (listen_for_value == value) {
                                int count = mMotionLayout.getChildCount();

                                for (int i = 0; i < count; i++) {
                                    View view = mMotionLayout.getChildAt(i);
                                    if (viewTransition.matchesView(view)) {
                                        int currentId = mMotionLayout.getCurrentState();
                                        ConstraintSet current =
                                                mMotionLayout.getConstraintSet(currentId);
                                        viewTransition.applyTransition(
                                                ViewTransitionController.this, mMotionLayout,
                                                currentId, current, view);
                                    }
                                }
                            }
                        } else { // not set
                            if (listen_for_value != value) {
                                int count = mMotionLayout.getChildCount();
                                for (int i = 0; i < count; i++) {
                                    View view = mMotionLayout.getChildAt(i);
                                    if (viewTransition.matchesView(view)) {
                                        int currentId = mMotionLayout.getCurrentState();
                                        ConstraintSet current =
                                                mMotionLayout.getConstraintSet(currentId);
                                        viewTransition.applyTransition(
                                                ViewTransitionController.this, mMotionLayout,
                                                currentId, current, view);
                                    }
                                }
                            }
                        }
                    }
                }
            });
    }

}
