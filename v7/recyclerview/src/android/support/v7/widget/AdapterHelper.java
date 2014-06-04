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

import android.support.v4.util.Pools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.support.v7.widget.RecyclerView.*;

/**
 * Helper class that can enqueue and process adapter update operations.
 * <p>
 * To support animations, RecyclerView presents an older version the Adapter to best represent
 * previous state of the layout. Sometimes, this is not trivial when items are removed that were
 * not laid out, in which case, RecyclerView has no way of providing that item's view for
 * animations.
 * <p>
 * AdapterHelper creates an UpdateOp for each adapter data change then pre-processes them. During
 * pre processing, AdapterHelper finds out which UpdateOps can be deferred to second layout pass
 * and which cannot. For the UpdateOps that cannot be deferred, AdapterHelper will change them
 * according to previously deferred operation and dispatch them before the first layout pass. It
 * also takes care of updating deferred UpdateOps since order of operations is changed by this
 * process.
 * <p>
 * Although operations may be forwarded to LayoutManager in different orders, resulting data set
 * is guaranteed to be the consistent.
 */
class AdapterHelper {

    final static int POSITION_TYPE_INVISIBLE = 0;

    final static int POSITION_TYPE_NEW_OR_LAID_OUT = 1;

    private Pools.Pool<UpdateOp> mUpdateOpPool = new Pools.SimplePool<UpdateOp>(UpdateOp.POOL_SIZE);

    final ArrayList<UpdateOp> mPendingUpdates = new ArrayList<UpdateOp>();

    final ArrayList<UpdateOp> mPostponedList = new ArrayList<UpdateOp>();

    final Callback mCallback;

    Runnable mOnItemProcessedCallback;

    final boolean mDisableRecycler;

    AdapterHelper(Callback callback) {
        this(callback, false);
    }

    AdapterHelper(Callback callback, boolean disableRecycler) {
        mCallback = callback;
        mDisableRecycler = disableRecycler;
    }

    AdapterHelper addUpdateOp(UpdateOp... ops) {
        Collections.addAll(mPendingUpdates, ops);
        return this;
    }

    void reset() {
        recycleUpdateOpsAndClearList(mPendingUpdates);
        recycleUpdateOpsAndClearList(mPostponedList);
    }

    void preProcess() {
        final int count = mPendingUpdates.size();
        for (int i = 0; i < count; i++) {
            UpdateOp op = mPendingUpdates.get(i);
            switch (op.cmd) {
                case UpdateOp.ADD:
                    applyAdd(op);
                    break;
                case UpdateOp.REMOVE:
                    applyRemove(op);
                    break;
                case UpdateOp.UPDATE:
                    applyUpdate(op);
                    break;
            }
            if (mOnItemProcessedCallback != null) {
                mOnItemProcessedCallback.run();
            }
        }
        mPendingUpdates.clear();
    }

    void consumePostponedUpdates() {
        final int count = mPostponedList.size();
        for (int i = 0; i < count; i++) {
            mCallback.onDispatchSecondPass(mPostponedList.get(i));
        }
        recycleUpdateOpsAndClearList(mPostponedList);
    }

    private void applyRemove(UpdateOp op) {
        int tmpStart = op.positionStart;
        int tmpCount = 0;
        int tmpEnd = op.positionStart + op.itemCount;
        int type = -1;
        for (int position = op.positionStart; position < tmpEnd; position++) {
            boolean typeChanged = false;
            ViewHolder vh = mCallback.findViewHolder(position);
            if (vh != null || isNewlyAdded(position)) {
                // If a ViewHolder exists or this is a newly added item, we can defer this update
                // to post layout stage.
                // * For existing ViewHolders, we'll fake its existence in the pre-layout phase.
                // * For items that are added and removed in the same process cycle, they won't
                // have any effect in pre-layout since their add ops are already deferred to
                // post-layout pass.
                if (type == POSITION_TYPE_INVISIBLE) {
                    // Looks like we have other updates that we cannot merge with this one.
                    // Create an UpdateOp and dispatch it to LayoutManager.
                    UpdateOp newOp = obtainUpdateOp(UpdateOp.REMOVE, tmpStart, tmpCount);
                    mCallback.offsetPositionsForRemovingInvisible(newOp.positionStart,
                            newOp.itemCount);
                    dispatch(newOp);
                    typeChanged = true;
                }
                type = POSITION_TYPE_NEW_OR_LAID_OUT;
            } else {
                // This update cannot be recovered because we don't have a ViewHolder representing
                // this position. Instead, post it to LayoutManager immediately
                if (type == POSITION_TYPE_NEW_OR_LAID_OUT) {
                    // Looks like we have other updates that we cannot merge with this one.
                    // Create UpdateOp op and dispatch it to LayoutManager.
                    UpdateOp newOp = obtainUpdateOp(UpdateOp.REMOVE, tmpStart, tmpCount);
                    mCallback.offsetPositionsForRemovingLaidOutOrNewView(newOp.positionStart,
                            newOp.itemCount);
                    postpone(newOp);
                    typeChanged = true;
                }
                type = POSITION_TYPE_INVISIBLE;
            }
            if (typeChanged) {
                position -= tmpCount; // also equal to tmpStart
                tmpEnd -= tmpCount;
                tmpCount = 1;
            } else {
                tmpCount++;
            }
        }
        if (tmpCount != op.itemCount) { // all 1 effect
            recycleUpdateOp(op);
            op = obtainUpdateOp(UpdateOp.REMOVE, tmpStart, tmpCount);
        }
        if (type == POSITION_TYPE_INVISIBLE) {
            mCallback.offsetPositionsForRemovingInvisible(op.positionStart, op.itemCount);
            dispatch(op);
        } else {
            mCallback.offsetPositionsForRemovingLaidOutOrNewView(op.positionStart, op.itemCount);
            postpone(op);
        }
    }

    private void applyUpdate(UpdateOp op) {
        int tmpStart = op.positionStart;
        int tmpCount = 0;
        int tmpEnd = op.positionStart + op.itemCount;
        int type = -1;
        for (int position = op.positionStart; position < tmpEnd; position++) {
            ViewHolder vh = mCallback.findViewHolder(position);
            if (vh != null || isNewlyAdded(position)) { // deferred
                if (type == POSITION_TYPE_INVISIBLE) {
                    UpdateOp newOp = obtainUpdateOp(UpdateOp.UPDATE, tmpStart, tmpCount);
                    mCallback.markViewHoldersUpdated(newOp.positionStart, newOp.itemCount);
                    dispatch(newOp);
                    // tmpStart is still same since dispatch already shifts elements
                    position -= newOp.itemCount; // also equal to tmpStart
                    tmpEnd -= newOp.itemCount;
                    tmpCount = 0;
                }
                type = POSITION_TYPE_NEW_OR_LAID_OUT;
            } else { // applied
                if (type == POSITION_TYPE_NEW_OR_LAID_OUT) {
                    UpdateOp newOp = obtainUpdateOp(UpdateOp.REMOVE, tmpStart, tmpCount);
                    mCallback.markViewHoldersUpdated(newOp.positionStart, newOp.itemCount);
                    postpone(newOp);
                    // both type-new and type-laid-out are deferred. This is why we are
                    // resetting out position to here.
                    position -= newOp.itemCount; // also equal to tmpStart
                    tmpEnd -= newOp.itemCount;
                    tmpCount = 0;
                }
                type = POSITION_TYPE_INVISIBLE;
            }
            tmpCount++;
        }
        if (tmpCount != op.itemCount) { // all 1 effect
            recycleUpdateOp(op);
            op = obtainUpdateOp(UpdateOp.REMOVE, tmpStart, tmpCount);
        }
        mCallback.markViewHoldersUpdated(op.positionStart, op.itemCount);
        if (type == POSITION_TYPE_INVISIBLE) {
            dispatch(op);
        } else {
            postpone(op);
        }
    }

    private void dispatch(UpdateOp op) {
        // tricky part.
        // traverse all postpones and revert their changes on this op if necessary, apply updated
        // dispatch to them since now they are after this op.
        final int count = mPostponedList.size();
        for (int i = count - 1; i >= 0; i--) {
            UpdateOp postponed = mPostponedList.get(i);
            if (postponed.positionStart <= op.positionStart) {
                op.positionStart += postponed.itemCount;
            } else {
                postponed.positionStart -= op.itemCount;
            }
        }
        mCallback.onDispatchFirstPass(op);
        recycleUpdateOp(op);
    }

    private boolean isNewlyAdded(int position) {
        final int count = mPostponedList.size();
        for (int i = 0; i < count; i++) {
            UpdateOp op = mPostponedList.get(i);
            if (op.cmd != UpdateOp.ADD) {
                continue;
            }
            int updatedStart = findPositionOffset(op.positionStart, i + 1);
            if (updatedStart <= position && updatedStart + op.itemCount > position) {
                return true;
            }
        }
        return false;
    }

    private void applyAdd(UpdateOp op) {
        mCallback.offsetPositionsForAdd(op.positionStart, op.itemCount);
        postpone(op);
    }

    private void postpone(UpdateOp op) {
        mPostponedList.add(op);
    }

    boolean hasPendingUpdates() {
        return mPendingUpdates.size() > 0;
    }

    int findPositionOffset(int position) {
        return findPositionOffset(position, 0);
    }

    int findPositionOffset(int position, int firstPosponedItem) {
        int offsetPosition = position;
        int count = mPostponedList.size();
        for (int i = firstPosponedItem; i < count; ++i) {
            UpdateOp op = mPostponedList.get(i);
            if (op.positionStart <= offsetPosition) {
                if (op.cmd == UpdateOp.REMOVE) {
                    offsetPosition -= op.itemCount;
                } else if (op.cmd == UpdateOp.ADD) {
                    offsetPosition += op.itemCount;
                }
            }
        }
        return offsetPosition;
    }

    /**
     * @return True if updates should be processed.
     */
    boolean onItemRangeChanged(int positionStart, int itemCount) {
        mPendingUpdates.add(obtainUpdateOp(UpdateOp.UPDATE, positionStart, itemCount));
        return mPendingUpdates.size() == 1;
    }

    /**
     * @return True if updates should be processed.
     */
    boolean onItemRangeInserted(int positionStart, int itemCount) {
        mPendingUpdates.add(obtainUpdateOp(UpdateOp.ADD, positionStart, itemCount));
        return mPendingUpdates.size() == 1;
    }

    /**
     * @return True if updates should be processed.
     */
    boolean onItemRangeRemoved(int positionStart, int itemCount) {
        mPendingUpdates.add(obtainUpdateOp(UpdateOp.REMOVE, positionStart, itemCount));
        return mPendingUpdates.size() == 1;
    }

    /**
     * Skips pre-processing and applies all updates in one pass.
     */
    void consumeUpdatesInOnePass() {
        // we still consume postponed updates (if there is) in case there was a pre-process call
        // w/o a matching consumePostponedUpdates.
        consumePostponedUpdates();
        final int count = mPendingUpdates.size();
        for (int i = 0; i < count; i++) {
            UpdateOp op = mPendingUpdates.get(i);
            switch (op.cmd) {
                case UpdateOp.ADD:
                    mCallback.offsetPositionsForAdd(op.positionStart, op.itemCount);
                    mCallback.onDispatchSecondPass(op);
                    break;
                case UpdateOp.REMOVE:
                    mCallback.offsetPositionsForRemovingInvisible(op.positionStart, op.itemCount);
                    mCallback.onDispatchSecondPass(op);
                    break;
                case UpdateOp.UPDATE:
                    mCallback.markViewHoldersUpdated(op.positionStart, op.itemCount);
                    break;
            }
            if (mOnItemProcessedCallback != null) {
                mOnItemProcessedCallback.run();
            }
        }
        recycleUpdateOpsAndClearList(mPendingUpdates);
    }

    /**
     * Queued operation to happen when child views are updated.
     */
    static class UpdateOp {

        static final int ADD = 0;

        static final int REMOVE = 1;

        static final int UPDATE = 2;

        static final int POOL_SIZE = 30;

        int cmd;

        int positionStart;

        int itemCount;

        UpdateOp(int cmd, int positionStart, int itemCount) {
            this.cmd = cmd;
            this.positionStart = positionStart;
            this.itemCount = itemCount;
        }

        String cmdToString() {
            switch (cmd) {
                case ADD:
                    return "add";
                case REMOVE:
                    return "rm";
                case UPDATE:
                    return "up";
            }
            return "??";
        }

        @Override
        public String toString() {
            return "[" + cmdToString() + ",s:" + positionStart + "c:" + itemCount + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            UpdateOp op = (UpdateOp) o;

            if (cmd != op.cmd) {
                return false;
            }
            if (itemCount != op.itemCount) {
                return false;
            }
            if (positionStart != op.positionStart) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = cmd;
            result = 31 * result + positionStart;
            result = 31 * result + itemCount;
            return result;
        }
    }

    UpdateOp obtainUpdateOp(int cmd, int positionStart, int itemCount) {
        UpdateOp op = mUpdateOpPool.acquire();
        if (op == null) {
            op = new UpdateOp(cmd, positionStart, itemCount);
        } else {
            op.cmd = cmd;
            op.positionStart = positionStart;
            op.itemCount = itemCount;
        }
        return op;
    }

    void recycleUpdateOp(UpdateOp op) {
        if (!mDisableRecycler) {
            mUpdateOpPool.release(op);
        }
    }

    void recycleUpdateOpsAndClearList(List<UpdateOp> ops) {
        final int count = ops.size();
        for (int i = 0; i < count; i++) {
            recycleUpdateOp(ops.get(i));
        }
        ops.clear();
    }

    /**
     * Contract between AdapterHelper and RecyclerView.
     */
    static interface Callback {

        ViewHolder findViewHolder(int position);

        void offsetPositionsForRemovingInvisible(int positionStart, int itemCount);

        void offsetPositionsForRemovingLaidOutOrNewView(int positionStart, int itemCount);

        void markViewHoldersUpdated(int positionStart, int itemCount);

        void onDispatchFirstPass(UpdateOp updateOp);

        void onDispatchSecondPass(UpdateOp updateOp);

        void offsetPositionsForAdd(int positionStart, int itemCount);
    }
}