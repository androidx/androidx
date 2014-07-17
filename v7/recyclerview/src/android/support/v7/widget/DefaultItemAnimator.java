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
package android.support.v7.widget;

import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;

import java.util.ArrayList;

/**
 * This implementation of {@link RecyclerView.ItemAnimator} provides basic
 * animations on remove, add, and move events that happen to the items in
 * a RecyclerView. RecyclerView uses a DefaultItemAnimator by default.
 *
 * @see RecyclerView#setItemAnimator(RecyclerView.ItemAnimator)
 */
public class DefaultItemAnimator extends RecyclerView.ItemAnimator {

    private ArrayList<ViewHolder> mPendingRemovals = new ArrayList<ViewHolder>();
    private ArrayList<ViewHolder> mPendingAdditions = new ArrayList<ViewHolder>();
    private ArrayList<MoveInfo> mPendingMoves = new ArrayList<MoveInfo>();
    private ArrayList<ChangeInfo> mPendingChanges = new ArrayList<ChangeInfo>();

    private ArrayList<ViewHolder> mAdditions = new ArrayList<ViewHolder>();
    private ArrayList<MoveInfo> mMoves = new ArrayList<MoveInfo>();
    private ArrayList<ChangeInfo> mChanges = new ArrayList<ChangeInfo>();

    private ArrayList<ViewHolder> mAddAnimations = new ArrayList<ViewHolder>();
    private ArrayList<ViewHolder> mMoveAnimations = new ArrayList<ViewHolder>();
    private ArrayList<ViewHolder> mRemoveAnimations = new ArrayList<ViewHolder>();
    private ArrayList<ChangeInfo> mChangeAnimations = new ArrayList<ChangeInfo>();

    private static class MoveInfo {
        public ViewHolder holder;
        public int fromX, fromY, toX, toY;

        private MoveInfo(ViewHolder holder, int fromX, int fromY, int toX, int toY) {
            this.holder = holder;
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }
    }

    private static class ChangeInfo {
        public ViewHolder oldHolder, newHolder;

        private ChangeInfo(ViewHolder oldHolder, ViewHolder newHolder) {
            this.oldHolder = oldHolder;
            this.newHolder = newHolder;
        }
    }

    @Override
    public void runPendingAnimations() {
        boolean removalsPending = !mPendingRemovals.isEmpty();
        boolean movesPending = !mPendingMoves.isEmpty();
        boolean changesPending = !mPendingChanges.isEmpty();
        boolean additionsPending = !mPendingAdditions.isEmpty();
        if (!removalsPending && !movesPending && !additionsPending) {
            // nothing to animate
            return;
        }
        // First, remove stuff
        for (ViewHolder holder : mPendingRemovals) {
            animateRemoveImpl(holder);
        }
        mPendingRemovals.clear();
        // Next, move stuff
        if (movesPending) {
            mMoves.addAll(mPendingMoves);
            mPendingMoves.clear();
            Runnable mover = new Runnable() {
                @Override
                public void run() {
                    for (MoveInfo moveInfo : mMoves) {
                        animateMoveImpl(moveInfo.holder, moveInfo.fromX, moveInfo.fromY,
                                moveInfo.toX, moveInfo.toY);
                    }
                    mMoves.clear();
                }
            };
            if (removalsPending) {
                View view = mMoves.get(0).holder.itemView;
                ViewCompat.postOnAnimationDelayed(view, mover, getRemoveDuration());
            } else {
                mover.run();
            }
        }
        // Next, change stuff, to run in parallel with move animations
        if (changesPending) {
            mChanges.addAll(mPendingChanges);
            mPendingChanges.clear();
            Runnable changer = new Runnable() {
                @Override
                public void run() {
                    for (ChangeInfo change : mChanges) {
                        animateChangeImpl(change);
                    }
                    mChanges.clear();
                }
            };
            if (removalsPending) {
                ViewHolder holder = mChanges.get(0).oldHolder;
                ViewCompat.postOnAnimationDelayed(holder.itemView, changer, getRemoveDuration());
            } else {
                changer.run();
            }
        }
        // Next, add stuff
        if (additionsPending) {
            mAdditions.addAll(mPendingAdditions);
            mPendingAdditions.clear();
            Runnable adder = new Runnable() {
                public void run() {
                    for (ViewHolder holder : mAdditions) {
                        animateAddImpl(holder);
                    }
                    mAdditions.clear();
                }
            };
            if (removalsPending || movesPending) {
                long removeDuration = removalsPending ? getRemoveDuration() : 0;
                long moveDuration = movesPending ? getMoveDuration() : 0;
                long changeDuration = changesPending ? getChangeDuration() : 0;
                long totalDelay = removeDuration + Math.max(moveDuration, changeDuration);
                View view = mAdditions.get(0).itemView;
                ViewCompat.postOnAnimationDelayed(view, adder, totalDelay);
            } else {
                adder.run();
            }
        }
    }

    @Override
    public boolean animateRemove(final ViewHolder holder) {
        endAnimation(holder);
        mPendingRemovals.add(holder);
        return true;
    }

    private void animateRemoveImpl(final ViewHolder holder) {
        final View view = holder.itemView;
        ViewCompat.animate(view).setDuration(getRemoveDuration()).
                alpha(0).setListener(new VpaListenerAdapter() {
            @Override
            public void onAnimationEnd(View view) {
                ViewCompat.setAlpha(view, 1);
                dispatchRemoveFinished(holder);
                mRemoveAnimations.remove(holder);
                dispatchFinishedWhenDone();
            }
        }).start();
        mRemoveAnimations.add(holder);
    }

    @Override
    public boolean animateAdd(final ViewHolder holder) {
        endAnimation(holder);
        ViewCompat.setAlpha(holder.itemView, 0);
        mPendingAdditions.add(holder);
        return true;
    }

    private void animateAddImpl(final ViewHolder holder) {
        final View view = holder.itemView;
        ViewCompat.animate(view).alpha(1).setDuration(getAddDuration()).
                setListener(new VpaListenerAdapter() {
                    @Override
                    public void onAnimationCancel(View view) {
                        ViewCompat.setAlpha(view, 1);
                    }

                    @Override
                    public void onAnimationEnd(View view) {
                        dispatchAddFinished(holder);
                        mAddAnimations.remove(holder);
                        dispatchFinishedWhenDone();
                    }
                }).start();
        mAddAnimations.add(holder);
    }

    @Override
    public boolean animateMove(final ViewHolder holder, int fromX, int fromY,
            int toX, int toY) {
        final View view = holder.itemView;
        fromX += ViewCompat.getTranslationX(holder.itemView);
        fromY += ViewCompat.getTranslationY(holder.itemView);
        endAnimation(holder);
        int deltaX = toX - fromX;
        int deltaY = toY - fromY;
        if (deltaX == 0 && deltaY == 0) {
            dispatchMoveFinished(holder);
            return false;
        }
        if (deltaX != 0) {
            ViewCompat.setTranslationX(view, -deltaX);
        }
        if (deltaY != 0) {
            ViewCompat.setTranslationY(view, -deltaY);
        }
        mPendingMoves.add(new MoveInfo(holder, fromX, fromY, toX, toY));
        return true;
    }

    private void animateMoveImpl(final ViewHolder holder, int fromX, int fromY, int toX, int toY) {
        final View view = holder.itemView;
        final int deltaX = toX - fromX;
        final int deltaY = toY - fromY;
        if (deltaX != 0) {
            ViewCompat.animate(view).translationX(0);
        }
        if (deltaY != 0) {
            ViewCompat.animate(view).translationY(0);
        }
        // TODO: make EndActions end listeners instead, since end actions aren't called when
        // vpas are canceled (and can't end them. why?)
        // need listener functionality in VPACompat for this. Ick.
        ViewCompat.animate(view).setDuration(getMoveDuration()).setListener(new VpaListenerAdapter() {
            @Override
            public void onAnimationCancel(View view) {
                if (deltaX != 0) {
                    ViewCompat.setTranslationX(view, 0);
                }
                if (deltaY != 0) {
                    ViewCompat.setTranslationY(view, 0);
                }
            }
            @Override
            public void onAnimationEnd(View view) {
                dispatchMoveFinished(holder);
                mMoveAnimations.remove(holder);
                dispatchFinishedWhenDone();
            }
        }).start();
        mMoveAnimations.add(holder);
    }

    @Override
    public boolean animateChange(ViewHolder oldHolder, ViewHolder newHolder) {
        endAnimation(oldHolder);
        if (newHolder != null && newHolder.itemView != null) {
            endAnimation(newHolder);
            ViewCompat.setAlpha(newHolder.itemView, 0);
        }
        mPendingChanges.add(new ChangeInfo(oldHolder, newHolder));
        return true;
    }

    private void animateChangeImpl(final ChangeInfo changeInfo) {
        final ViewHolder holder = changeInfo.oldHolder;
        final View view = holder.itemView;
        final ViewHolder newHolder = changeInfo.newHolder;
        final View newView = newHolder != null ? newHolder.itemView : null;
        if (newView != null) {
            ViewCompat.animate(newView).cancel();
        }
        ViewCompat.animate(view).setDuration(getChangeDuration()).
                alpha(0).setListener(new VpaListenerAdapter() {
            @Override
            public void onAnimationCancel(View view) {
                if (newView != null) {
                    ViewCompat.animate(newView).cancel();
                }
            }
            @Override
            public void onAnimationEnd(View view) {
                ViewCompat.setAlpha(view, 1);
                dispatchChangeFinished(holder);
                mChangeAnimations.remove(changeInfo);
                dispatchFinishedWhenDone();
            }
        }).start();
        if (newView != null) {
            ViewCompat.animate(newView).setDuration(getChangeDuration()).
                    alpha(1).setListener(new VpaListenerAdapter() {
                @Override
                public void onAnimationEnd(View view) {
                    ViewCompat.setAlpha(newView, 1);
                }
            }).start();
        }
        mChangeAnimations.add(changeInfo);
    }

    @Override
    public void endAnimation(ViewHolder item) {
        final View view = item.itemView;
        ViewCompat.animate(view).cancel();
        for (int i = mPendingMoves.size() - 1; i >= 0; i--) {
            MoveInfo moveInfo = mPendingMoves.get(i);
            if (moveInfo.holder == item) {
                ViewCompat.setTranslationY(view, 0);
                ViewCompat.setTranslationX(view, 0);
                dispatchMoveFinished(item);
                mPendingMoves.remove(item);
                break;
            }
        }
        for (int i = mPendingChanges.size() - 1; i >= 0; i--) {
            ChangeInfo changeInfo = mPendingChanges.get(i);
            if (changeInfo.oldHolder == item || changeInfo.newHolder == item) {
                View newView = changeInfo.newHolder != null ? changeInfo.newHolder.itemView : null;
                ViewCompat.setAlpha(view, 1);
                if (newView != null) {
                    ViewCompat.setAlpha(newView, 1);
                }
                dispatchChangeFinished(changeInfo.oldHolder);
                mPendingChanges.remove(changeInfo);
                break;
            }
        }
        if (mPendingRemovals.contains(item)) {
            dispatchRemoveFinished(item);
            mPendingRemovals.remove(item);
        }
        if (mPendingAdditions.contains(item)) {
            ViewCompat.setAlpha(view, 1);
            dispatchAddFinished(item);
            mPendingAdditions.remove(item);
        }
        if (mMoveAnimations.contains(item)) {
            ViewCompat.setTranslationY(view, 0);
            ViewCompat.setTranslationX(view, 0);
            dispatchMoveFinished(item);
            mMoveAnimations.remove(item);
        }
        for (int i = mChanges.size() - 1; i >= 0; i--) {
            ChangeInfo changeInfo = mChanges.get(i);
            if (changeInfo.oldHolder == item || changeInfo.newHolder == item) {
                View newView = changeInfo.newHolder != null ? changeInfo.newHolder.itemView : null;
                ViewCompat.setAlpha(view, 1);
                if (newView != null) {
                    ViewCompat.setAlpha(newView, 1);
                }
                dispatchChangeFinished(changeInfo.oldHolder);
                mChanges.remove(changeInfo);
                break;
            }
        }
        for (int i = mMoves.size() - 1; i >= 0; i--) {
            MoveInfo moveInfo = mMoves.get(i);
            if (moveInfo.holder == item) {
                ViewCompat.setTranslationY(view, 0);
                ViewCompat.setTranslationX(view, 0);
                dispatchMoveFinished(item);
                mMoves.remove(i);
                break;
            }
        }
        if (mAdditions.contains(item)) {
            ViewCompat.setAlpha(view, 1);
            dispatchAddFinished(item);
            mAdditions.remove(item);
        }
        if (mRemoveAnimations.contains(item)) {
            ViewCompat.setAlpha(view, 1);
            dispatchRemoveFinished(item);
            mRemoveAnimations.remove(item);
        }
        if (mAddAnimations.contains(item)) {
            ViewCompat.setAlpha(view, 1);
            dispatchAddFinished(item);
            mAddAnimations.remove(item);
        }
        for (int i = mChangeAnimations.size() - 1; i >= 0; i--) {
            ChangeInfo changeInfo = mChangeAnimations.get(i);
            if (changeInfo.oldHolder == item || changeInfo.newHolder == item) {
                View newView = changeInfo.newHolder != null ? changeInfo.newHolder.itemView : null;
                ViewCompat.setAlpha(view, 1);
                if (newView != null) {
                    ViewCompat.setAlpha(newView, 1);
                }
                dispatchChangeFinished(changeInfo.oldHolder);
                mChangeAnimations.remove(changeInfo);
                break;
            }
        }
        dispatchFinishedWhenDone();
    }

    @Override
    public boolean isRunning() {
        return (!mPendingAdditions.isEmpty() ||
                !mMoveAnimations.isEmpty() ||
                !mRemoveAnimations.isEmpty() ||
                !mAddAnimations.isEmpty() ||
                !mChangeAnimations.isEmpty() ||
                !mMoves.isEmpty() ||
                !mAdditions.isEmpty() ||
                !mChanges.isEmpty());
    }

    /**
     * Check the state of currently pending and running animations. If there are none
     * pending/running, call {@link #dispatchAnimationsFinished()} to notify any
     * listeners.
     */
    private void dispatchFinishedWhenDone() {
        if (!isRunning()) {
            dispatchAnimationsFinished();
        }
    }

    @Override
    public void endAnimations() {
        int count = mPendingMoves.size();
        for (int i = count - 1; i >= 0; i--) {
            MoveInfo item = mPendingMoves.get(i);
            View view = item.holder.itemView;
            ViewCompat.setTranslationY(view, 0);
            ViewCompat.setTranslationX(view, 0);
            dispatchMoveFinished(item.holder);
            mPendingMoves.remove(i);
        }
        count = mPendingRemovals.size();
        for (int i = count - 1; i >= 0; i--) {
            ViewHolder item = mPendingRemovals.get(i);
            dispatchRemoveFinished(item);
            mPendingRemovals.remove(i);
        }
        count = mPendingAdditions.size();
        for (int i = count - 1; i >= 0; i--) {
            ViewHolder item = mPendingAdditions.get(i);
            View view = item.itemView;
            ViewCompat.setAlpha(view, 1);
            dispatchAddFinished(item);
            mPendingAdditions.remove(i);
        }
        count = mPendingChanges.size();
        for (int i = count - 1; i >= 0; i--) {
            ChangeInfo item = mPendingChanges.get(i);
            View newView = item.newHolder != null ? item.newHolder.itemView : null;
            if (newView != null) {
                ViewCompat.setAlpha(newView, 1);
            }
            dispatchChangeFinished(item.oldHolder);
            mPendingChanges.remove(i);
        }
        if (!isRunning()) {
            return;
        }

        count = mMoves.size();
        for (int i = count - 1; i >= 0; i--) {
            MoveInfo moveInfo = mMoves.get(i);
            ViewHolder item = moveInfo.holder;
            View view = item.itemView;
            ViewCompat.setTranslationY(view, 0);
            ViewCompat.setTranslationX(view, 0);
            dispatchMoveFinished(moveInfo.holder);
            mMoves.remove(i);
        }
        count = mAdditions.size();
        for (int i = count - 1; i >= 0; i--) {
            ViewHolder item = mAdditions.get(i);
            View view = item.itemView;
            ViewCompat.setAlpha(view, 1);
            dispatchAddFinished(item);
            mAdditions.remove(i);
        }
        count = mChanges.size();
        for (int i = count - 1; i >= 0; i--) {
            ChangeInfo item = mChanges.get(i);
            View newView = item.newHolder != null ? item.newHolder.itemView : null;
            if (newView != null) {
                ViewCompat.setAlpha(newView, 1);
            }
            dispatchChangeFinished(item.oldHolder);
            mChanges.remove(i);
        }

        count = mMoveAnimations.size();
        for (int i = count - 1; i >= 0; i--) {
            ViewHolder item = mMoveAnimations.get(i);
            View view = item.itemView;
            ViewCompat.animate(view).cancel();
        }
        count = mRemoveAnimations.size();
        for (int i = count - 1; i >= 0; i--) {
            ViewHolder item = mRemoveAnimations.get(i);
            View view = item.itemView;
            ViewCompat.animate(view).cancel();
        }
        count = mAddAnimations.size();
        for (int i = count - 1; i >= 0; i--) {
            ViewHolder item = mAddAnimations.get(i);
            View view = item.itemView;
            ViewCompat.animate(view).cancel();
        }
        count = mChangeAnimations.size();
        for (int i = count - 1; i >= 0; i--) {
            ChangeInfo item = mChangeAnimations.get(i);
            View oldView = item.oldHolder.itemView;
            ViewCompat.animate(oldView).cancel();
        }

        dispatchAnimationsFinished();
    }

    private static class VpaListenerAdapter implements ViewPropertyAnimatorListener {
        @Override
        public void onAnimationStart(View view) {}

        @Override
        public void onAnimationEnd(View view) {}

        @Override
        public void onAnimationCancel(View view) {}
    };
}
