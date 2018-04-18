/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.recyclerview.widget;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This is a dummy ItemAnimator class that does not depends on Duration, Tests would use this class
 * to control whenever they want the Animator to finish.
 * 1. Test MUST call endAnimation(ViewHolder) on UI thread to finish animation of a given ViewHolder
 *    Or Test calls endAnimations() on UI thread to end animations for all.
 * 2. Test can call getAddAnimations() etc. to get ViewHolders that currently running animation.
 * 3. Test can call {@link #expect(int, int)} and {@link #waitFor(int)} to wait given
 *    Events are fired.
 */
public class DummyItemAnimator extends SimpleItemAnimator {

    static final long TIMEOUT_SECOND = 10;

    ArrayList<RecyclerView.ViewHolder> mAdds = new ArrayList();
    ArrayList<RecyclerView.ViewHolder> mRemoves = new ArrayList();
    ArrayList<RecyclerView.ViewHolder> mMoves = new ArrayList();
    ArrayList<RecyclerView.ViewHolder> mChangesOld = new ArrayList();
    ArrayList<RecyclerView.ViewHolder> mChangesNew = new ArrayList();

    @Retention(CLASS)
    @Target({PARAMETER, METHOD, LOCAL_VARIABLE, FIELD})
    public @interface CountDownLatchIndex {
    }

    @CountDownLatchIndex
    public static final int ADD_START = 0;

    @CountDownLatchIndex
    public static final int ADD_FINISHED = 1;

    @CountDownLatchIndex
    public static final int REMOVE_START = 2;

    @CountDownLatchIndex
    public static final int REMOVE_FINISHED = 3;

    @CountDownLatchIndex
    public static final int MOVE_START = 4;

    @CountDownLatchIndex
    public static final int MOVE_FINISHED = 5;

    @CountDownLatchIndex
    public static final int CHANGE_OLD_START = 6;

    @CountDownLatchIndex
    public static final int CHANGE_OLD_FINISHED = 7;

    @CountDownLatchIndex
    public static final int CHANGE_NEW_START = 8;

    @CountDownLatchIndex
    public static final int CHANGE_NEW_FINISHED = 9;

    static final int NUM_COUNT_DOWN_LATCH = 10;

    CountDownLatch[] mCountDownLatches = new CountDownLatch[NUM_COUNT_DOWN_LATCH];


    public List<RecyclerView.ViewHolder> getAddAnimations() {
        return mAdds;
    }

    public List<RecyclerView.ViewHolder> getRemoveAnimations() {
        return mRemoves;
    }

    public List<RecyclerView.ViewHolder> getMovesAnimations() {
        return mMoves;
    }

    public List<RecyclerView.ViewHolder> getChangesOldAnimations() {
        return mChangesOld;
    }

    public List<RecyclerView.ViewHolder> getChangesNewAnimations() {
        return mChangesNew;
    }

    @Override
    public boolean animateRemove(RecyclerView.ViewHolder holder) {
        mRemoves.add(holder);
        dispatchRemoveStarting(holder);
        return false;
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        mAdds.add(holder);
        dispatchAddStarting(holder);
        return false;
    }

    @Override
    public boolean animateMove(RecyclerView.ViewHolder holder, int fromX, int fromY, int toX,
            int toY) {
        mMoves.add(holder);
        dispatchMoveStarting(holder);
        return false;
    }

    @Override
    public boolean animateChange(RecyclerView.ViewHolder oldHolder,
            RecyclerView.ViewHolder newHolder, int fromLeft, int fromTop, int toLeft, int toTop) {
        mChangesOld.add(oldHolder);
        mChangesNew.add(newHolder);
        dispatchChangeStarting(oldHolder, true);
        dispatchChangeStarting(newHolder, false);
        return false;
    }

    public void expect(@CountDownLatchIndex int index, int count) {
        mCountDownLatches[index] = new CountDownLatch(count);
    }

    public void waitFor(@CountDownLatchIndex int index)
            throws InterruptedException {
        mCountDownLatches[index].await(TIMEOUT_SECOND, TimeUnit.SECONDS);
    }

    @Override
    public void onChangeStarting(RecyclerView.ViewHolder item, boolean oldItem) {
        CountDownLatch latch = mCountDownLatches[oldItem ? CHANGE_OLD_START : CHANGE_NEW_START];
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public void onMoveStarting(RecyclerView.ViewHolder item) {
        CountDownLatch latch = mCountDownLatches[MOVE_START];
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public void onAddStarting(RecyclerView.ViewHolder item) {
        CountDownLatch latch = mCountDownLatches[ADD_START];
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public void onRemoveStarting(RecyclerView.ViewHolder item) {
        CountDownLatch latch = mCountDownLatches[REMOVE_START];
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public void onChangeFinished(RecyclerView.ViewHolder item, boolean oldItem) {
        CountDownLatch latch = mCountDownLatches[oldItem
                ? CHANGE_OLD_FINISHED : CHANGE_NEW_FINISHED];
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public void onMoveFinished(RecyclerView.ViewHolder item) {
        CountDownLatch latch = mCountDownLatches[MOVE_FINISHED];
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public void onAddFinished(RecyclerView.ViewHolder item) {
        CountDownLatch latch = mCountDownLatches[ADD_FINISHED];
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public void onRemoveFinished(RecyclerView.ViewHolder item) {
        CountDownLatch latch = mCountDownLatches[REMOVE_FINISHED];
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public void runPendingAnimations() {
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        if (mAdds.remove(item)) {
            dispatchAddFinished(item);
        } else if (mRemoves.remove(item)) {
            dispatchRemoveFinished(item);
        } else if (mMoves.remove(item)) {
            dispatchMoveFinished(item);
        } else if (mChangesOld.remove(item)) {
            dispatchChangeFinished(item, true);
        } else if (mChangesNew.remove(item)) {
            dispatchChangeFinished(item, false);
        }
    }

    @Override
    public void endAnimations() {
        for (int i = mAdds.size() - 1; i >= 0; i--) {
            endAnimation(mAdds.get(i));
        }
        for (int i = mRemoves.size() - 1; i >= 0; i--) {
            endAnimation(mRemoves.get(i));
        }
        for (int i = mMoves.size() - 1; i >= 0; i--) {
            endAnimation(mMoves.get(i));
        }
        for (int i = mChangesOld.size() - 1; i >= 0; i--) {
            endAnimation(mChangesOld.get(i));
        }
        for (int i = mChangesNew.size() - 1; i >= 0; i--) {
            endAnimation(mChangesNew.get(i));
        }
    }

    @Override
    public boolean isRunning() {
        return mAdds.size() != 0
                || mRemoves.size() != 0
                || mMoves.size() != 0
                || mChangesOld.size() != 0
                || mChangesNew.size() != 0;
    }
}
