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

package androidx.fragment.app;

import android.util.Log;

import androidx.lifecycle.Lifecycle;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Entry of an operation on the fragment back stack.
 */
final class BackStackRecord extends FragmentTransaction implements
        FragmentManager.BackStackEntry, FragmentManager.OpGenerator {
    private static final String TAG = FragmentManager.TAG;

    final FragmentManager mManager;

    boolean mCommitted;
    int mIndex = -1;
    boolean mBeingSaved = false;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("BackStackEntry{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        if (mIndex >= 0) {
            sb.append(" #");
            sb.append(mIndex);
        }
        if (mName != null) {
            sb.append(" ");
            sb.append(mName);
        }
        sb.append("}");
        return sb.toString();
    }

    public void dump(String prefix, PrintWriter writer) {
        dump(prefix, writer, true);
    }

    public void dump(String prefix, PrintWriter writer, boolean full) {
        if (full) {
            writer.print(prefix); writer.print("mName="); writer.print(mName);
            writer.print(" mIndex="); writer.print(mIndex);
            writer.print(" mCommitted="); writer.println(mCommitted);
            if (mTransition != FragmentTransaction.TRANSIT_NONE) {
                writer.print(prefix); writer.print("mTransition=#");
                writer.print(Integer.toHexString(mTransition));
            }
            if (mEnterAnim != 0 || mExitAnim != 0) {
                writer.print(prefix); writer.print("mEnterAnim=#");
                writer.print(Integer.toHexString(mEnterAnim));
                writer.print(" mExitAnim=#");
                writer.println(Integer.toHexString(mExitAnim));
            }
            if (mPopEnterAnim != 0 || mPopExitAnim != 0) {
                writer.print(prefix); writer.print("mPopEnterAnim=#");
                writer.print(Integer.toHexString(mPopEnterAnim));
                writer.print(" mPopExitAnim=#");
                writer.println(Integer.toHexString(mPopExitAnim));
            }
            if (mBreadCrumbTitleRes != 0 || mBreadCrumbTitleText != null) {
                writer.print(prefix); writer.print("mBreadCrumbTitleRes=#");
                writer.print(Integer.toHexString(mBreadCrumbTitleRes));
                writer.print(" mBreadCrumbTitleText=");
                writer.println(mBreadCrumbTitleText);
            }
            if (mBreadCrumbShortTitleRes != 0 || mBreadCrumbShortTitleText != null) {
                writer.print(prefix); writer.print("mBreadCrumbShortTitleRes=#");
                writer.print(Integer.toHexString(mBreadCrumbShortTitleRes));
                writer.print(" mBreadCrumbShortTitleText=");
                writer.println(mBreadCrumbShortTitleText);
            }
        }

        if (!mOps.isEmpty()) {
            writer.print(prefix); writer.println("Operations:");
            final int numOps = mOps.size();
            for (int opNum = 0; opNum < numOps; opNum++) {
                final Op op = mOps.get(opNum);
                String cmdStr;
                switch (op.mCmd) {
                    case OP_NULL: cmdStr="NULL"; break;
                    case OP_ADD: cmdStr="ADD"; break;
                    case OP_REPLACE: cmdStr="REPLACE"; break;
                    case OP_REMOVE: cmdStr="REMOVE"; break;
                    case OP_HIDE: cmdStr="HIDE"; break;
                    case OP_SHOW: cmdStr="SHOW"; break;
                    case OP_DETACH: cmdStr="DETACH"; break;
                    case OP_ATTACH: cmdStr="ATTACH"; break;
                    case OP_SET_PRIMARY_NAV: cmdStr="SET_PRIMARY_NAV"; break;
                    case OP_UNSET_PRIMARY_NAV: cmdStr="UNSET_PRIMARY_NAV";break;
                    case OP_SET_MAX_LIFECYCLE: cmdStr = "OP_SET_MAX_LIFECYCLE"; break;
                    default: cmdStr = "cmd=" + op.mCmd; break;
                }
                writer.print(prefix); writer.print("  Op #"); writer.print(opNum);
                writer.print(": "); writer.print(cmdStr);
                writer.print(" "); writer.println(op.mFragment);
                if (full) {
                    if (op.mEnterAnim != 0 || op.mExitAnim != 0) {
                        writer.print(prefix); writer.print("enterAnim=#");
                        writer.print(Integer.toHexString(op.mEnterAnim));
                        writer.print(" exitAnim=#");
                        writer.println(Integer.toHexString(op.mExitAnim));
                    }
                    if (op.mPopEnterAnim != 0 || op.mPopExitAnim != 0) {
                        writer.print(prefix); writer.print("popEnterAnim=#");
                        writer.print(Integer.toHexString(op.mPopEnterAnim));
                        writer.print(" popExitAnim=#");
                        writer.println(Integer.toHexString(op.mPopExitAnim));
                    }
                }
            }
        }
    }

    BackStackRecord(@NonNull FragmentManager manager) {
        super(manager.getFragmentFactory(), manager.getHost() != null
                ? manager.getHost().getContext().getClassLoader()
                : null);
        mManager = manager;
    }

    BackStackRecord(@NonNull BackStackRecord bse) {
        super(bse.mManager.getFragmentFactory(), bse.mManager.getHost() != null
                ? bse.mManager.getHost().getContext().getClassLoader()
                : null, bse);
        mManager = bse.mManager;
        mCommitted = bse.mCommitted;
        mIndex = bse.mIndex;
        mBeingSaved = bse.mBeingSaved;
    }


    @Override
    public int getId() {
        return mIndex;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getBreadCrumbTitleRes() {
        return mBreadCrumbTitleRes;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getBreadCrumbShortTitleRes() {
        return mBreadCrumbShortTitleRes;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nullable CharSequence getBreadCrumbTitle() {
        if (mBreadCrumbTitleRes != 0) {
            return mManager.getHost().getContext().getText(mBreadCrumbTitleRes);
        }
        return mBreadCrumbTitleText;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nullable CharSequence getBreadCrumbShortTitle() {
        if (mBreadCrumbShortTitleRes != 0) {
            return mManager.getHost().getContext().getText(mBreadCrumbShortTitleRes);
        }
        return mBreadCrumbShortTitleText;
    }

    @Override
    void doAddOp(int containerViewId, Fragment fragment, @Nullable String tag, int opcmd) {
        super.doAddOp(containerViewId, fragment, tag, opcmd);
        fragment.mFragmentManager = mManager;
    }

    @Override
    public @NonNull FragmentTransaction remove(@NonNull Fragment fragment) {
        if (fragment.mFragmentManager != null && fragment.mFragmentManager != mManager) {
            throw new IllegalStateException("Cannot remove Fragment attached to "
                    + "a different FragmentManager. Fragment " + fragment.toString() + " is already"
                    + " attached to a FragmentManager.");
        }
        return super.remove(fragment);
    }

    @Override
    public @NonNull FragmentTransaction hide(@NonNull Fragment fragment) {
        if (fragment.mFragmentManager != null && fragment.mFragmentManager != mManager) {
            throw new IllegalStateException("Cannot hide Fragment attached to "
                    + "a different FragmentManager. Fragment " + fragment.toString() + " is already"
                    + " attached to a FragmentManager.");
        }
        return super.hide(fragment);
    }

    @Override
    public @NonNull FragmentTransaction show(@NonNull Fragment fragment) {
        if (fragment.mFragmentManager != null && fragment.mFragmentManager != mManager) {
            throw new IllegalStateException("Cannot show Fragment attached to "
                    + "a different FragmentManager. Fragment " + fragment.toString() + " is already"
                    + " attached to a FragmentManager.");
        }
        return super.show(fragment);
    }

    @Override
    public @NonNull FragmentTransaction detach(@NonNull Fragment fragment) {
        if (fragment.mFragmentManager != null && fragment.mFragmentManager != mManager) {
            throw new IllegalStateException("Cannot detach Fragment attached to "
                    + "a different FragmentManager. Fragment " + fragment.toString() + " is already"
                    + " attached to a FragmentManager.");
        }
        return super.detach(fragment);
    }

    @Override
    public @NonNull FragmentTransaction setPrimaryNavigationFragment(@Nullable Fragment fragment) {
        if (fragment != null
                && fragment.mFragmentManager != null && fragment.mFragmentManager != mManager) {
            throw new IllegalStateException("Cannot setPrimaryNavigation for Fragment attached to "
                    + "a different FragmentManager. Fragment " + fragment.toString() + " is already"
                    + " attached to a FragmentManager.");
        }
        return super.setPrimaryNavigationFragment(fragment);
    }

    @Override
    public @NonNull FragmentTransaction setMaxLifecycle(@NonNull Fragment fragment,
            Lifecycle.@NonNull State state) {
        if (fragment.mFragmentManager != mManager) {
            throw new IllegalArgumentException("Cannot setMaxLifecycle for Fragment not attached to"
                    + " FragmentManager " + mManager);
        }
        if (state == Lifecycle.State.INITIALIZED && fragment.mState > Fragment.INITIALIZING) {
            throw new IllegalArgumentException("Cannot set maximum Lifecycle to " + state
                    + " after the Fragment has been created");
        }
        if (state == Lifecycle.State.DESTROYED) {
            throw new IllegalArgumentException("Cannot set maximum Lifecycle to " + state + ". Use "
                    + "remove() to remove the fragment from the FragmentManager and trigger its "
                    + "destruction.");
        }
        return super.setMaxLifecycle(fragment, state);
    }

    void bumpBackStackNesting(int amt) {
        if (!mAddToBackStack) {
            return;
        }
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(TAG, "Bump nesting in " + this + " by " + amt);
        }
        final int numOps = mOps.size();
        for (int opNum = 0; opNum < numOps; opNum++) {
            final Op op = mOps.get(opNum);
            if (op.mFragment != null) {
                op.mFragment.mBackStackNesting += amt;
                if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
                    Log.v(TAG, "Bump nesting of "
                            + op.mFragment + " to " + op.mFragment.mBackStackNesting);
                }
            }
        }
    }

    public void runOnCommitRunnables() {
        if (mCommitRunnables != null) {
            for (int i = 0; i < mCommitRunnables.size(); i++) {
                mCommitRunnables.get(i).run();
            }
            mCommitRunnables = null;
        }
    }

    @Override
    public int commit() {
        return commitInternal(false, true);
    }

    @Override
    public int commitAllowingStateLoss() {
        return commitInternal(true, true);
    }

    @Override
    public void commitNow() {
        disallowAddToBackStack();
        mManager.execSingleAction(this, false);
    }

    @Override
    public void commitNowAllowingStateLoss() {
        disallowAddToBackStack();
        mManager.execSingleAction(this, true);
    }

    int commitInternal(boolean allowStateLoss, boolean commitAction) {
        if (mCommitted) throw new IllegalStateException("commit already called");
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(TAG, "Commit: " + this);
            LogWriter logw = new LogWriter(TAG);
            PrintWriter pw = new PrintWriter(logw);
            dump("  ", pw);
            pw.close();
        }
        mCommitted = true;
        if (mAddToBackStack) {
            mIndex = mManager.allocBackStackIndex();
        } else {
            mIndex = -1;
        }
        if (commitAction) {
            mManager.enqueueAction(this, allowStateLoss);
        }
        return mIndex;
    }

    /**
     * Implementation of {@link FragmentManager.OpGenerator}.
     * This operation is added to the list of pending actions during {@link #commit()}, and
     * will be executed on the UI thread to run this FragmentTransaction.
     *
     * @param records Modified to add this BackStackRecord
     * @param isRecordPop Modified to add a false (this isn't a pop)
     * @return true always because the records and isRecordPop will always be changed
     */
    @Override
    public boolean generateOps(@NonNull ArrayList<BackStackRecord> records,
            @NonNull ArrayList<Boolean> isRecordPop) {
        if (FragmentManager.isLoggingEnabled(Log.VERBOSE)) {
            Log.v(TAG, "Run: " + this);
        }

        records.add(this);
        isRecordPop.add(false);
        if (mAddToBackStack) {
            mManager.addBackStackState(this);
        }
        return true;
    }

    /**
     * Executes the operations contained within this transaction.
     */
    void executeOps() {
        final int numOps = mOps.size();
        for (int opNum = 0; opNum < numOps; opNum++) {
            final Op op = mOps.get(opNum);
            final Fragment f = op.mFragment;
            if (f != null) {
                f.mBeingSaved = mBeingSaved;
                f.setPopDirection(false);
                f.setNextTransition(mTransition);
                f.setSharedElementNames(mSharedElementSourceNames, mSharedElementTargetNames);
            }
            switch (op.mCmd) {
                case OP_ADD:
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.setExitAnimationOrder(f, false);
                    mManager.addFragment(f);
                    break;
                case OP_REMOVE:
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.removeFragment(f);
                    break;
                case OP_HIDE:
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.hideFragment(f);
                    break;
                case OP_SHOW:
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.setExitAnimationOrder(f, false);
                    mManager.showFragment(f);
                    break;
                case OP_DETACH:
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.detachFragment(f);
                    break;
                case OP_ATTACH:
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.setExitAnimationOrder(f, false);
                    mManager.attachFragment(f);
                    break;
                case OP_SET_PRIMARY_NAV:
                    mManager.setPrimaryNavigationFragment(f);
                    break;
                case OP_UNSET_PRIMARY_NAV:
                    mManager.setPrimaryNavigationFragment(null);
                    break;
                case OP_SET_MAX_LIFECYCLE:
                    op.mOldMaxState = f.mMaxState;
                    mManager.setMaxLifecycle(f, op.mCurrentMaxState);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown cmd: " + op.mCmd);
            }
        }
    }

    /**
     * Reverses the execution of the operations within this transaction.
     */
    void executePopOps() {
        for (int opNum = mOps.size() - 1; opNum >= 0; opNum--) {
            final Op op = mOps.get(opNum);
            Fragment f = op.mFragment;
            if (f != null) {
                f.mBeingSaved = mBeingSaved;
                f.setPopDirection(true);
                f.setNextTransition(FragmentManager.reverseTransit(mTransition));
                // Reverse the target and source names for pop operations
                f.setSharedElementNames(mSharedElementTargetNames, mSharedElementSourceNames);
            }
            switch (op.mCmd) {
                case OP_ADD:
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.setExitAnimationOrder(f, true);
                    mManager.removeFragment(f);
                    break;
                case OP_REMOVE:
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.addFragment(f);
                    break;
                case OP_HIDE:
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.showFragment(f);
                    break;
                case OP_SHOW:
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.setExitAnimationOrder(f, true);
                    mManager.hideFragment(f);
                    break;
                case OP_DETACH:
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.attachFragment(f);
                    break;
                case OP_ATTACH:
                    f.setAnimations(op.mEnterAnim, op.mExitAnim, op.mPopEnterAnim, op.mPopExitAnim);
                    mManager.setExitAnimationOrder(f, true);
                    mManager.detachFragment(f);
                    break;
                case OP_SET_PRIMARY_NAV:
                    mManager.setPrimaryNavigationFragment(null);
                    break;
                case OP_UNSET_PRIMARY_NAV:
                    mManager.setPrimaryNavigationFragment(f);
                    break;
                case OP_SET_MAX_LIFECYCLE:
                    op.mCurrentMaxState = f.mMaxState;
                    mManager.setMaxLifecycle(f, op.mOldMaxState);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown cmd: " + op.mCmd);
            }
        }
    }

    /**
     * Expands all meta-ops into their more primitive equivalents. This must be called prior to
     * {@link #executeOps()} or any other call that operations on mOps for forward navigation.
     * It should not be called for pop/reverse navigation operations.
     *
     * <p>Removes all OP_REPLACE ops and replaces them with the proper add and remove
     * operations that are equivalent to the replace.</p>
     *
     * <p>Adds OP_UNSET_PRIMARY_NAV ops to match OP_SET_PRIMARY_NAV, OP_REMOVE and OP_DETACH
     * ops so that we can restore the old primary nav fragment later. Since callers call this
     * method in a loop before running ops from several transactions at once, the caller should
     * pass the return value from this method as the oldPrimaryNav parameter for the next call.
     * The first call in such a loop should pass the value of
     * {@link FragmentManager#getPrimaryNavigationFragment()}.</p>
     *
     * @param added Initialized to the fragments that are in the mManager.mAdded, this
     *              will be modified to contain the fragments that will be in mAdded
     *              after the execution ({@link #executeOps()}.
     * @param oldPrimaryNav The tracked primary navigation fragment as of the beginning of
     *                      this set of ops
     * @return the new oldPrimaryNav fragment after this record's ops would be run
     */
    @SuppressWarnings("ReferenceEquality")
    Fragment expandOps(ArrayList<Fragment> added, Fragment oldPrimaryNav) {
        for (int opNum = 0; opNum < mOps.size(); opNum++) {
            final Op op = mOps.get(opNum);
            switch (op.mCmd) {
                case OP_ADD:
                case OP_ATTACH:
                    added.add(op.mFragment);
                    break;
                case OP_REMOVE:
                case OP_DETACH: {
                    added.remove(op.mFragment);
                    if (op.mFragment == oldPrimaryNav) {
                        mOps.add(opNum, new Op(OP_UNSET_PRIMARY_NAV, op.mFragment));
                        opNum++;
                        oldPrimaryNav = null;
                    }
                }
                break;
                case OP_REPLACE: {
                    final Fragment f = op.mFragment;
                    final int containerId = f.mContainerId;
                    boolean alreadyAdded = false;
                    for (int i = added.size() - 1; i >= 0; i--) {
                        final Fragment old = added.get(i);
                        if (old.mContainerId == containerId) {
                            if (old == f) {
                                alreadyAdded = true;
                            } else {
                                // This is duplicated from above since we only make
                                // a single pass for expanding ops. Unset any outgoing primary nav.
                                if (old == oldPrimaryNav) {
                                    mOps.add(opNum, new Op(OP_UNSET_PRIMARY_NAV, old, true));
                                    opNum++;
                                    oldPrimaryNav = null;
                                }
                                final Op removeOp = new Op(OP_REMOVE, old, true);
                                removeOp.mEnterAnim = op.mEnterAnim;
                                removeOp.mPopEnterAnim = op.mPopEnterAnim;
                                removeOp.mExitAnim = op.mExitAnim;
                                removeOp.mPopExitAnim = op.mPopExitAnim;
                                mOps.add(opNum, removeOp);
                                added.remove(old);
                                opNum++;
                            }
                        }
                    }
                    if (alreadyAdded) {
                        mOps.remove(opNum);
                        opNum--;
                    } else {
                        op.mCmd = OP_ADD;
                        op.mFromExpandedOp = true;
                        added.add(f);
                    }
                }
                break;
                case OP_SET_PRIMARY_NAV: {
                    // It's ok if this is null, that means we will restore to no active
                    // primary navigation fragment on a pop.
                    mOps.add(opNum, new Op(OP_UNSET_PRIMARY_NAV, oldPrimaryNav, true));
                    op.mFromExpandedOp = true;
                    opNum++;
                    // Will be set by the OP_SET_PRIMARY_NAV we inserted before when run
                    oldPrimaryNav = op.mFragment;
                }
                break;
            }
        }
        return oldPrimaryNav;
    }

    /**
     * Removes fragments that are added or removed during a pop operation.
     *
     * @param added Initialized to the fragments that are in the mManager.mAdded, this
     *              will be modified to contain the fragments that will be in mAdded
     *              after the execution ({@link #executeOps()}.
     * @param oldPrimaryNav The tracked primary navigation fragment as of the beginning of
     *                      this set of ops
     * @return the new oldPrimaryNav fragment after this record's ops would be popped
     */
    Fragment trackAddedFragmentsInPop(ArrayList<Fragment> added, Fragment oldPrimaryNav) {
        for (int opNum = mOps.size() - 1; opNum >= 0; opNum--) {
            final Op op = mOps.get(opNum);
            switch (op.mCmd) {
                case OP_ADD:
                case OP_ATTACH:
                    added.remove(op.mFragment);
                    break;
                case OP_REMOVE:
                case OP_DETACH:
                    added.add(op.mFragment);
                    break;
                case OP_UNSET_PRIMARY_NAV:
                    oldPrimaryNav = op.mFragment;
                    break;
                case OP_SET_PRIMARY_NAV:
                    oldPrimaryNav = null;
                    break;
                case OP_SET_MAX_LIFECYCLE:
                    op.mCurrentMaxState = op.mOldMaxState;
                    break;
            }
        }
        return oldPrimaryNav;
    }

    /**
     * Removes any Ops expanded by {@link #expandOps(ArrayList, Fragment)},
     * reverting them back to their collapsed form.
     */
    void collapseOps() {
        for (int opNum = mOps.size() - 1; opNum >= 0; opNum--) {
            final Op op = mOps.get(opNum);
            if (!op.mFromExpandedOp) {
                continue;
            }
            if (op.mCmd == OP_SET_PRIMARY_NAV) {
                // OP_SET_PRIMARY_NAV is always expanded to two ops:
                // 1. The OP_SET_PRIMARY_NAV we want to keep
                op.mFromExpandedOp = false;
                // And the OP_UNSET_PRIMARY_NAV we want to remove
                mOps.remove(opNum - 1);
                opNum--;
            } else {
                // Handle the collapse of an OP_REPLACE, which could start
                // with either an OP_ADD (the usual case) or an OP_REMOVE
                // (if the dev explicitly called add() earlier in the transaction)
                int containerId = op.mFragment.mContainerId;
                // Swap this expanded op with a replace
                op.mCmd = OP_REPLACE;
                op.mFromExpandedOp = false;
                // And remove all other expanded ops with the same containerId
                for (int replaceOpNum = opNum - 1; replaceOpNum >= 0; replaceOpNum--) {
                    final Op potentialReplaceOp = mOps.get(replaceOpNum);
                    if (potentialReplaceOp.mFromExpandedOp
                            && potentialReplaceOp.mFragment.mContainerId == containerId) {
                        mOps.remove(replaceOpNum);
                        opNum--;
                    }
                }
            }
        }
    }

    @Override
    public @Nullable String getName() {
        return mName;
    }

    @Override
    public boolean isEmpty() {
        return mOps.isEmpty();
    }
}
