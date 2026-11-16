/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.arcore.integration.tests;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import org.jspecify.annotations.Nullable;

/** Pure-Java facilitator activities declared in the test APK manifest (no kotlin-stdlib dep). */
public final class LifecycleFacilitatorActivities {
    static final String ACTION_OPAQUE_FACILITATOR_RESUMED =
            "androidx.xr.arcore.integration.tests.ACTION_OPAQUE_FACILITATOR_RESUMED";
    static final String ACTION_FLOATING_FACILITATOR_RESUMED =
            "androidx.xr.arcore.integration.tests.ACTION_FLOATING_FACILITATOR_RESUMED";
    static final String ACTION_FINISH_FACILITATOR_ACTIVITIES =
            "androidx.xr.arcore.integration.tests.ACTION_FINISH_FACILITATOR_ACTIVITIES";

    private LifecycleFacilitatorActivities() {}

    /** Base facilitator activity that signals resume and finishes on a test broadcast. */
    public abstract static class FacilitatorActivity extends Activity {
        private final String mResumedAction;
        private boolean mReceiverRegistered;
        private final BroadcastReceiver mFinishReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        finish();
                    }
                };

        FacilitatorActivity(String resumedAction) {
            mResumedAction = resumedAction;
        }

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            registerReceiver(
                    mFinishReceiver,
                    new IntentFilter(ACTION_FINISH_FACILITATOR_ACTIVITIES),
                    Context.RECEIVER_EXPORTED);
            mReceiverRegistered = true;
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0);
        }

        @Override
        public void finish() {
            super.finish();
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0);
        }

        @Override
        protected void onResume() {
            super.onResume();
            sendBroadcast(new Intent(mResumedAction));
        }

        @Override
        protected void onDestroy() {
            if (mReceiverRegistered) {
                unregisterReceiver(mFinishReceiver);
                mReceiverRegistered = false;
            }
            super.onDestroy();
        }
    }

    /** Fullscreen opaque facilitator that stops the underlying test activity. */
    public static final class OpaqueFacilitatorActivity extends FacilitatorActivity {
        public OpaqueFacilitatorActivity() {
            super(ACTION_OPAQUE_FACILITATOR_RESUMED);
        }
    }

    /** Floating dialog-themed facilitator that pauses without stopping the underlying activity. */
    public static final class FloatingFacilitatorActivity extends FacilitatorActivity {
        public FloatingFacilitatorActivity() {
            super(ACTION_FLOATING_FACILITATOR_RESUMED);
        }
    }
}
