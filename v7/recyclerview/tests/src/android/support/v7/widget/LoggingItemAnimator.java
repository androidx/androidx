/*
 * Copyright (C) 2015 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.v7.widget;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import android.support.v7.widget.BaseRecyclerViewAnimationsTest.AnimateDisappearance;
import android.support.v7.widget.BaseRecyclerViewAnimationsTest.AnimateChange;
import android.support.v7.widget.BaseRecyclerViewAnimationsTest.AnimatePersistence;
import android.support.v7.widget.BaseRecyclerViewAnimationsTest.AnimateAppearance;

public class LoggingItemAnimator extends DefaultItemAnimator {

    final ArrayList<RecyclerView.ViewHolder> mAddVHs = new ArrayList<RecyclerView.ViewHolder>();

    final ArrayList<RecyclerView.ViewHolder> mRemoveVHs = new ArrayList<RecyclerView.ViewHolder>();

    final ArrayList<RecyclerView.ViewHolder> mMoveVHs = new ArrayList<RecyclerView.ViewHolder>();

    final ArrayList<RecyclerView.ViewHolder> mChangeOldVHs = new ArrayList<RecyclerView.ViewHolder>();

    final ArrayList<RecyclerView.ViewHolder> mChangeNewVHs = new ArrayList<RecyclerView.ViewHolder>();

    List<AnimateAppearance> mAnimateAppearanceList = new ArrayList<>();
    List<AnimateDisappearance> mAnimateDisappearanceList = new ArrayList<>();
    List<AnimatePersistence> mAnimatePersistenceList = new ArrayList<>();
    List<AnimateChange> mAnimateChangeList = new ArrayList<>();

    CountDownLatch mWaitForPendingAnimations;

    public boolean contains(RecyclerView.ViewHolder viewHolder,
            List<? extends BaseRecyclerViewAnimationsTest.AnimateLogBase> list) {
        for (BaseRecyclerViewAnimationsTest.AnimateLogBase log : list) {
            if (log.viewHolder == viewHolder) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean animateDisappearance(@NonNull RecyclerView.ViewHolder viewHolder,
            @NonNull ItemHolderInfo preLayoutInfo, ItemHolderInfo postLayoutInfo) {
        mAnimateDisappearanceList
                .add(new AnimateDisappearance(viewHolder, null, null));
        return super.animateDisappearance(viewHolder, preLayoutInfo, postLayoutInfo);
    }

    @Override
    public boolean animateAppearance(@NonNull RecyclerView.ViewHolder viewHolder,
            ItemHolderInfo preLayoutInfo,
            @NonNull ItemHolderInfo postLayoutInfo) {
        mAnimateAppearanceList
                .add(new AnimateAppearance(viewHolder, null, null));
        return super.animateAppearance(viewHolder, preLayoutInfo, postLayoutInfo);
    }

    @Override
    public boolean animatePersistence(@NonNull RecyclerView.ViewHolder viewHolder,
            @NonNull ItemHolderInfo preInfo,
            @NonNull ItemHolderInfo postInfo) {
        mAnimatePersistenceList
                .add(new AnimatePersistence(viewHolder, null, null));
        return super.animatePersistence(viewHolder, preInfo, postInfo);
    }

    @Override
    public boolean animateChange(@NonNull RecyclerView.ViewHolder oldHolder,
            @NonNull RecyclerView.ViewHolder newHolder, @NonNull ItemHolderInfo preInfo,
            @NonNull ItemHolderInfo postInfo) {
        mAnimateChangeList
                .add(new AnimateChange(oldHolder, newHolder, null, null));
        return super.animateChange(oldHolder, newHolder, preInfo, postInfo);
    }

    @Override
    public void runPendingAnimations() {
        if (mWaitForPendingAnimations != null) {
            mWaitForPendingAnimations.countDown();
        }
        super.runPendingAnimations();
    }

    public void expectRunPendingAnimationsCall(int count) {
        mWaitForPendingAnimations = new CountDownLatch(count);
    }

    public void waitForPendingAnimationsCall(int seconds) throws InterruptedException {
        mWaitForPendingAnimations.await(seconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        mAddVHs.add(holder);
        return super.animateAdd(holder);
    }

    @Override
    public boolean animateRemove(RecyclerView.ViewHolder holder) {
        mRemoveVHs.add(holder);
        return super.animateRemove(holder);
    }

    @Override
    public boolean animateMove(RecyclerView.ViewHolder holder, int fromX, int fromY,
            int toX, int toY) {
        mMoveVHs.add(holder);
        return super.animateMove(holder, fromX, fromY, toX, toY);
    }

    @Override
    public boolean animateChange(RecyclerView.ViewHolder oldHolder,
            RecyclerView.ViewHolder newHolder, int fromX, int fromY, int toX, int toY) {
        if (oldHolder != null) {
            mChangeOldVHs.add(oldHolder);
        }
        if (newHolder != null) {
            mChangeNewVHs.add(newHolder);
        }
        return super.animateChange(oldHolder, newHolder, fromX, fromY, toX, toY);
    }

    public void reset() {
        mAddVHs.clear();
        mRemoveVHs.clear();
        mMoveVHs.clear();
        mChangeOldVHs.clear();
        mChangeNewVHs.clear();
        mAnimateChangeList.clear();
        mAnimatePersistenceList.clear();
        mAnimateAppearanceList.clear();
        mAnimateDisappearanceList.clear();
    }
}