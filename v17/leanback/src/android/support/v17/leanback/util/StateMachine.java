/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Linear or DAG of {@link State}s. StateMachine is by default a linear model, until
 * {@link #addState(State, State)} is called.  Each State has three status:
 * STATUS_ZERO, STATUS_INVOKED, STATUS_EXECUTED.   We allow client to run a State, which will
 * put State in STATUS_INVOKED.  A State will be executed when prior States are executed and
 * Precondition for this State is true, then the State will be marked as STATUS_EXECUTED.
 *
 * @hide
 */
public final class StateMachine {

    /**
     * No request on the State
     */
    public static final int STATUS_ZERO = 0;
    /**
     * Somebody wants to run the state but not yet executed because either the condition is
     * false or lower States are not executed.
     */
    public static final int STATUS_INVOKED = 1;
    /**
     * Somebody wants to run the State and the State was executed.
     */
    public static final int STATUS_EXECUTED = 2;

    public static class State {

        private int mStatus;
        private ArrayList<State> mPriorStates;

        /**
         * Run State, Subclass may override.
         */
        public void run() {
        }

        /**
         * Returns true if State can run, false otherwise.  Subclass may override.
         * @return True if State can run, false otherwise.  Subclass may override.
         */
        public boolean canRun() {
            return true;
        }

        /**
         * @return True if the State has been executed.
         */
        final boolean runIfNeeded() {
            if (mStatus!= STATUS_EXECUTED) {
                if (mStatus == STATUS_INVOKED && canRun()) {
                    run();
                    mStatus = STATUS_EXECUTED;
                } else {
                    return false;
                }
            }
            return true;
        }

        void addPriorState(State state) {
            if (mPriorStates == null) {
                mPriorStates = new ArrayList<State>();
            }
            if (!mPriorStates.contains(state)) {
                mPriorStates.add(state);
            }
        }

        final void markInvoked() {
            if (mStatus == STATUS_ZERO) {
                mStatus = STATUS_INVOKED;
            }
        }

        final void updateStatus(int status) {
            mStatus = status;
        }

        /**
         * Get status, return one of {@link #STATUS_ZERO}, {@link #STATUS_INVOKED},
         * {@link #STATUS_EXECUTED}.
         * @return Status of the State.
         */
        public final int getStatus() {
            return mStatus;
        }

        @Override
        public final boolean equals(Object other) {
            return this == other;
        }
    }

    private boolean mSorted = true;
    private final ArrayList<State> mSortedList = new ArrayList<State>();

    /**
     * Add a State to StateMachine, ignore if it is already added.
     * @param state The state to add.
     */
    public void addState(State state) {
        if (!mSortedList.contains(state)) {
            state.updateStatus(STATUS_ZERO);
            mSortedList.add(state);
        }
    }

    /**
     * Add two States to StateMachine and create an edge between this two.
     * StateMachine is by default a linear model, until {@link #addState(State, State)} is called.
     * sort() is required to sort the Direct acyclic graph.
     * @param fromState The from state to add.
     * @param toState The to state to add.
     */
    public void addState(State fromState, State toState) {
        addState(fromState);
        addState(toState);
        toState.addPriorState(fromState);
        mSorted = false;
    }

    void verifySorted() {
        if (!mSorted) {
            throw new RuntimeException("Graph not sorted");
        }
    }

    public void runState(State state) {
        verifySorted();
        state.markInvoked();
        runPendingStates();
    }

    public void runPendingStates() {
        verifySorted();
        for (int i = 0, size = mSortedList.size(); i < size; i++) {
            if (!mSortedList.get(i).runIfNeeded()) {
                break;
            }
        }
    }

    public void resetStatus() {
        for (int i = 0, size = mSortedList.size(); i < size; i++) {
            mSortedList.get(i).updateStatus(STATUS_ZERO);
        }
    }

    /**
     * StateMachine is by default a linear model, until {@link #addState(State, State)} is called.
     * sort() is required to sort the Direct acyclic graph.
     */
    public void sort() {
        if (mSorted) {
            return;
        }
        // L: Empty list that will contain the sorted States
        ArrayList<State> L = new ArrayList<State>();
        // S: Set of all nodes with no incoming edges
        ArrayList<State> S = new ArrayList<State>();
        HashMap<State, ArrayList<State>> edges = new HashMap<State, ArrayList<State>>();
        for (int i = mSortedList.size() - 1; i >= 0 ; i--) {
            State state = mSortedList.get(i);
            if (state.mPriorStates != null && state.mPriorStates.size() > 0) {
                edges.put(state, new ArrayList<State>(state.mPriorStates));
            } else {
                S.add(state);
            }
        }

        while (!S.isEmpty()) {
            // remove a State without incoming Node from S, add to L
            State state = S.remove(S.size() - 1);
            L.add(state);
            // for each toState that having an incoming edge from "state":
            for (Iterator<Map.Entry<State, ArrayList<State>>> iterator =
                    edges.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<State, ArrayList<State>> entry = iterator.next();
                ArrayList<State> fromStates = entry.getValue();
                // remove edge from graph
                if (fromStates.remove(state)) {
                    if (fromStates.size() == 0) {
                        State toState = entry.getKey();
                        // insert the toState to S if it has no more incoming edges
                        S.add(toState);
                        iterator.remove();
                    }
                }
            }
        }
        if (edges.size() > 0) {
            throw new RuntimeException("Cycle in Graph");
        }

        mSortedList.clear();
        mSortedList.addAll(L);
        mSorted = true;
    }
}
