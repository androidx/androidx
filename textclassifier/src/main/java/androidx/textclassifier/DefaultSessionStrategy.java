/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.textclassifier;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

/**
 * Default implementation of {@link SessionStrategy}, sorting out {@link SelectionEvent} by
 * using {@link SelectionEventHelper}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
final class DefaultSessionStrategy implements SessionStrategy {
    private final TextClassifier mSession;
    private final SelectionEventHelper mEventHelper;
    private final TextClassificationSessionId mSessionId;
    private final TextClassificationContext mClassificationContext;

    private boolean mDestroyed;

    DefaultSessionStrategy(TextClassificationContext textClassificationContext,
            TextClassifier textClassifier) {
        mClassificationContext = Preconditions.checkNotNull(textClassificationContext);
        mSession = Preconditions.checkNotNull(textClassifier);
        mSessionId = new TextClassificationSessionId();
        mEventHelper = new SelectionEventHelper(mSessionId, mClassificationContext);
    }

    @Override
    public void reportSelectionEvent(@NonNull SelectionEvent event) {
        Preconditions.checkNotNull(event);
        if (mEventHelper.sanitizeEvent(event)) {
            mSession.onSelectionEvent(event);
        }
    }

    @Override
    public void destroy() {
        mEventHelper.endSession();
        mDestroyed = true;
    }

    @Override
    public boolean isDestroyed() {
        return mDestroyed;
    }

    /**
     * Helper class for updating SelectionEvent fields.
     */
    private static final class SelectionEventHelper {

        private static final boolean DEBUG_LOG_ENABLED = true;
        private static final String TAG = "SelectionEventHelper";
        private final TextClassificationSessionId mSessionId;
        private final TextClassificationContext mContext;

        @SelectionEvent.InvocationMethod
        private int mInvocationMethod = SelectionEvent.INVOCATION_UNKNOWN;
        private SelectionEvent mPrevEvent;
        private SelectionEvent mStartEvent;

        SelectionEventHelper(
                TextClassificationSessionId sessionId, TextClassificationContext context) {
            mSessionId = Preconditions.checkNotNull(sessionId);
            mContext = Preconditions.checkNotNull(context);
        }

        /**
         * Updates the necessary fields in the event for the current session.
         *
         * @return true if the event should be reported. false if the event should be ignored
         */
        boolean sanitizeEvent(SelectionEvent event) {
            updateInvocationMethod(event);

            if (event.getEventType() != SelectionEvent.EVENT_SELECTION_STARTED
                    && mStartEvent == null) {
                if (DEBUG_LOG_ENABLED) {
                    Log.d(TAG, "Selection session not yet started. Ignoring event");
                }
                return false;
            }

            final long now = System.currentTimeMillis();
            switch (event.getEventType()) {
                case SelectionEvent.EVENT_SELECTION_STARTED:
                    Preconditions.checkArgument(
                            event.getAbsoluteEnd() == event.getAbsoluteStart() + 1);
                    event.setSessionId(mSessionId);
                    mStartEvent = event;
                    break;
                case SelectionEvent.EVENT_SELECTION_MODIFIED:  // fall through
                case SelectionEvent.EVENT_AUTO_SELECTION:
                    if (mPrevEvent != null
                            && mPrevEvent.getAbsoluteStart() == event.getAbsoluteStart()
                            && mPrevEvent.getAbsoluteEnd() == event.getAbsoluteEnd()) {
                        // Selection did not change. Ignore event.
                        return false;
                    }
                    break;
                default:
                    // do nothing.
            }

            event.setEventTime(now);
            if (mStartEvent != null) {
                event.setSessionId(mStartEvent.getSessionId())
                        .setDurationSinceSessionStart(now - mStartEvent.getEventTime())
                        .setStart(event.getAbsoluteStart() - mStartEvent.getAbsoluteStart())
                        .setEnd(event.getAbsoluteEnd() - mStartEvent.getAbsoluteStart());
            }
            if (mPrevEvent != null) {
                event.setDurationSincePreviousEvent(now - mPrevEvent.getEventTime())
                        .setEventIndex(mPrevEvent.getEventIndex() + 1);
            }
            mPrevEvent = event;
            return true;
        }

        void endSession() {
            mPrevEvent = null;
            mStartEvent = null;
        }

        private void updateInvocationMethod(SelectionEvent event) {
            event.setTextClassificationSessionContext(mContext);
            if (event.getInvocationMethod() == SelectionEvent.INVOCATION_UNKNOWN) {
                event.setInvocationMethod(mInvocationMethod);
            } else {
                mInvocationMethod = event.getInvocationMethod();
            }
        }
    }
}
