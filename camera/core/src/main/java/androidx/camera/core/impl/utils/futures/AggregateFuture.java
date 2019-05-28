/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core.impl.utils.futures;

import androidx.annotation.Nullable;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A future made up of a collection of sub-futures.
 *
 * <p>Copied and adapted from Guava.
 *
 * @param <InputT>  the type of the individual inputs
 * @param <OutputT> the type of the output (i.e. this) future
 */
abstract class AggregateFuture<InputT, OutputT> extends AbstractFuture.TrustedFuture<OutputT> {
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final Logger sLogger = Logger.getLogger(AggregateFuture.class.getName());

    /*
     * In certain circumstances, this field might theoretically not be visible to an afterDone()
     * call triggered by cancel(). For details, see the comments on the fields of TimeoutFuture.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @Nullable
    RunningState mRunningState;

    @Override
    protected final void afterDone() {
        super.afterDone();
        RunningState localRunningState = mRunningState;
        if (localRunningState != null) {
            // Let go of the memory held by the running state
            this.mRunningState = null;
            Collection<? extends ListenableFuture<? extends InputT>> futures =
                    localRunningState.mFutures;
            boolean wasInterrupted = wasInterrupted();

            if (wasInterrupted) {
                localRunningState.interruptTask();
            }

            if (isCancelled() & futures != null) {
                for (ListenableFuture<?> future : futures) {
                    future.cancel(wasInterrupted);
                }
            }
        }
    }

    @Override
    protected String pendingToString() {
        RunningState localRunningState = mRunningState;
        if (localRunningState == null) {
            return null;
        }
        Collection<? extends ListenableFuture<? extends InputT>> localFutures =
                localRunningState.mFutures;
        if (localFutures != null) {
            return "mFutures=[" + localFutures + "]";
        }
        return null;
    }

    /** Must be called at the end of each sub-class's constructor. */
    final void init(RunningState runningState) {
        this.mRunningState = runningState;
        runningState.init();
    }

    abstract class RunningState extends AggregateFutureState implements Runnable {
        @SuppressWarnings("WeakerAccess") /* synthetic access */
                Collection<? extends ListenableFuture<? extends InputT>> mFutures;
        private final boolean mAllMustSucceed;
        private final boolean mCollectsValues;

        RunningState(
                Collection<? extends ListenableFuture<? extends InputT>> futures,
                boolean allMustSucceed,
                boolean collectsValues) {
            super(futures.size());
            this.mFutures = Preconditions.checkNotNull(futures);
            this.mAllMustSucceed = allMustSucceed;
            this.mCollectsValues = collectsValues;
        }

        /* Used in the !mAllMustSucceed case so we don't have to instantiate a listener. */
        @Override
        public final void run() {
            decrementCountAndMaybeComplete();
        }

        /**
         * The "real" initialization; we can't put this in the constructor because, in the case
         * where mFutures are already complete, we would not initialize the subclass before calling
         * {@link #handleOneInputDone}. As this is called after the subclass is constructed, we're
         * guaranteed to have properly initialized the subclass.
         */
        void init() {
            // Corner case: List is empty.
            if (mFutures.isEmpty()) {
                handleAllCompleted();
                return;
            }

            // NOTE: If we ever want to use a custom executor here, have a look at CombinedFuture
            // as we'll need to handle RejectedExecutionException

            if (mAllMustSucceed) {
                // We need fail fast, so we have to keep track of which future failed so we can
                // propagate the exception immediately

                // Register a listener on each Future in the list to update the state of this
                // future. Note that if all the mFutures on the list are done prior to completing
                // this loop, the last call to addListener() will callback to setOneValue(),
                // transitively call our cleanup listener, and set this.mFutures to null. This is
                // not actually a problem, since the foreach only needs this.mFutures to be
                // non-null at the beginning of the loop.
                int i = 0;
                for (final ListenableFuture<? extends InputT> listenable : mFutures) {
                    final int index = i++;
                    listenable.addListener(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        handleOneInputDone(index, listenable);
                                    } finally {
                                        decrementCountAndMaybeComplete();
                                    }
                                }
                            },
                            CameraXExecutors.directExecutor());
                }
            } else {
                // We'll only call the callback when all mFutures complete, regardless of whether
                // some failed. Hold off on calling setOneValue until all complete, so we can share
                // the same listener.
                for (ListenableFuture<? extends InputT> listenable : mFutures) {
                    listenable.addListener(this, CameraXExecutors.directExecutor());
                }
            }
        }

        /**
         * Fails this future with the given Throwable if {@link #mAllMustSucceed} is true. Also,
         * logs the throwable if it is an {@link Error} or if {@link #mAllMustSucceed} is {@code
         * true}, the throwable did not cause this future to fail, and it is the first time we've
         * seen that particular Throwable.
         */
        private void handleException(Throwable throwable) {
            Preconditions.checkNotNull(throwable);

            boolean completedWithFailure = false;
            boolean firstTimeSeeingThisException = true;
            if (mAllMustSucceed) {
                // As soon as the first one fails, throw the exception up.
                // The result of all other inputs is then ignored.
                completedWithFailure = setException(throwable);
                if (completedWithFailure) {
                    releaseResourcesAfterFailure();
                } else {
                    // Go up the causal chain to see if we've already seen this cause; if we
                    // have, even if it's wrapped by a different exception, don't log it.
                    firstTimeSeeingThisException = addCausalChain(getOrInitSeenExceptions(),
                            throwable);
                }
            }

            // | and & used because it's faster than the branch required for || and &&
            if (throwable instanceof Error
                    | (mAllMustSucceed & !completedWithFailure & firstTimeSeeingThisException)) {
                String message =
                        (throwable instanceof Error)
                                ? "Input Future failed with Error"
                                : "Got more than one input Future failure. Logging failures after"
                                        + " the first";
                sLogger.log(Level.SEVERE, message, throwable);
            }
        }

        @Override
        final void addInitialException(Set<Throwable> seen) {
            if (!isCancelled()) {
                boolean unused = addCausalChain(seen, tryInternalFastPathGetFailure());
            }
        }

        /** Handles the input at the given index completing. */
        void handleOneInputDone(int index, Future<? extends InputT> future) {
            // The only cases in which this Future should already be done are (a) if it was
            // cancelled or (b) if an input failed and we propagated that immediately because of
            // mAllMustSucceed.
            Preconditions.checkState(
                    mAllMustSucceed || !isDone() || isCancelled(),
                    "Future was done before all dependencies completed");

            try {
                Preconditions.checkState(future.isDone(),
                        "Tried to set value from future which is not done");
                if (mAllMustSucceed) {
                    if (future.isCancelled()) {
                        // clear running state prior to cancelling children, this sets our own
                        // state but lets the input mFutures keep running as some of them may be
                        // used elsewhere.
                        mRunningState = null;
                        cancel(false);
                    } else {
                        // We always get the result so that we can have fail-fast, even if we
                        // don't collect
                        InputT result = Futures.getDone(future);
                        if (mCollectsValues) {
                            collectOneValue(mAllMustSucceed, index, result);
                        }
                    }
                } else if (mCollectsValues && !future.isCancelled()) {
                    collectOneValue(mAllMustSucceed, index, Futures.getDone(future));
                }
            } catch (ExecutionException e) {
                handleException(e.getCause());
            } catch (Throwable t) {
                handleException(t);
            }
        }

        void decrementCountAndMaybeComplete() {
            int newRemaining = decrementRemainingAndGet();
            Preconditions.checkState(newRemaining >= 0, "Less than 0 remaining mFutures");
            if (newRemaining == 0) {
                processCompleted();
            }
        }

        private void processCompleted() {
            // Collect the values if (a) our output requires collecting them and (b) we haven't been
            // collecting them as we go. (We've collected them as we go only if we needed to fail
            // fast)
            if (mCollectsValues & !mAllMustSucceed) {
                int i = 0;
                for (ListenableFuture<? extends InputT> listenable : mFutures) {
                    handleOneInputDone(i++, listenable);
                }
            }
            handleAllCompleted();
        }

        /**
         * Listeners implicitly keep a reference to {@link RunningState} as they're inner
         * classes, so we free resources here as well for the mAllMustSucceed=true case (i.e. when a
         * future fails, we immediately release resources we no longer need); additionally, the
         * future will release its reference to {@link RunningState}, which should free all
         * associated memory when all the mFutures complete and the listeners are released.
         */
        void releaseResourcesAfterFailure() {
            this.mFutures = null;
        }

        /**
         * Called only if {@code mCollectsValues} is true.
         *
         * <p>If {@code mAllMustSucceed} is true, called as each future completes; otherwise,
         * called for each future when all mFutures complete.
         */
        abstract void collectOneValue(boolean allMustSucceed, int index,
                @Nullable InputT returnValue);

        abstract void handleAllCompleted();

        void interruptTask() {
        }
    }

    /** Adds the chain to the seen set, and returns whether all the chain was new to us. */
    static boolean addCausalChain(Set<Throwable> seen, Throwable t) {
        for (; t != null; t = t.getCause()) {
            boolean firstTimeSeen = seen.add(t);
            if (!firstTimeSeen) {
                /*
                 * We've seen this, so we've seen its causes, too. No need to re-add them. (There's
                 * one case where this isn't true, but we ignore it: If we record an exception, then
                 * someone calls initCause() on it, and then we examine it again, we'll conclude
                 * that we've seen the whole chain before when it fact we haven't. But this should
                 * be rare.)
                 */
                return false;
            }
        }
        return true;
    }
}
