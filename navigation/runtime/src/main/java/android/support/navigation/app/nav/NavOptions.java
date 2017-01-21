/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.navigation.app.nav;

import android.os.Bundle;

/**
 * NavOptions stores special options for navigate actions
 */
public class NavOptions {
    static final int LAUNCH_SINGLE_TOP = 0x1;
    static final int LAUNCH_DOCUMENT = 0x2;
    static final int LAUNCH_CLEAR_TASK = 0x4;

    private static final String KEY_LAUNCH_MODE = "launchMode";
    private static final String KEY_FLOW_NAME = "flowName";

    private int mLaunchMode;
    private String mFlowName;

    NavOptions(int launchMode, String flowName) {
        mLaunchMode = launchMode;
        mFlowName = flowName;
    }

    boolean shouldLaunchSingleTop() {
        return (mLaunchMode & LAUNCH_SINGLE_TOP) != 0;
    }

    boolean shouldLaunchDocument() {
        return (mLaunchMode & LAUNCH_DOCUMENT) != 0;
    }

    boolean shouldClearTask() {
        return (mLaunchMode & LAUNCH_CLEAR_TASK) != 0;
    }

    public String getFlowName() {
        return mFlowName;
    }

    Bundle toBundle() {
        return new Bundle();
    }

    static NavOptions fromBundle(Bundle b) {
        return new NavOptions(b.getInt(KEY_LAUNCH_MODE, 0),
                b.getString(KEY_FLOW_NAME));
    }

    /**
     * Builder for constructing new instances of NavOptions.
     */
    public static class Builder {
        int mLaunchMode;
        String mFlowName;
        String mEndFlow;

        public Builder() {
        }

        /**
         * Launch a navigation target as single-top if you are making a lateral navigation
         * between instances of the same target (e.g. detail pages about similar data items)
         * that should not preserve history.
         *
         * @param singleTop true to launch as single-top
         */
        public Builder setLaunchSingleTop(boolean singleTop) {
            if (singleTop) {
                mLaunchMode |= LAUNCH_SINGLE_TOP;
            } else {
                mLaunchMode &= ~LAUNCH_SINGLE_TOP;
            }
            return this;
        }

        /**
         * Launch a navigation target as a document if you want it to appear as its own
         * entry in the system Overview screen. If the same document is launched multiple times
         * it will not create a new task, it will bring the existing document task to the front.
         *
         * <p>If the user presses the system Back key from a new document task they will land
         * on their previous task. If the user reached the document task from the system Overview
         * screen they will be taken to their home screen.</p>
         *
         * @param launchDocument true to launch a new document task
         */
        public Builder setLaunchDocument(boolean launchDocument) {
            if (launchDocument) {
                mLaunchMode |= LAUNCH_DOCUMENT;
            } else {
                mLaunchMode &= ~LAUNCH_DOCUMENT;
            }
            return this;
        }

        /**
         * Clear the entire task before launching this target. If you are launching as a
         * {@link #setLaunchDocument(boolean) document}, this will clear the document task.
         * Otherwise it will clear the current task.
         *
         * @param clearTask
         * @return
         */
        public Builder setClearTask(boolean clearTask) {
            if (clearTask) {
                mLaunchMode |= LAUNCH_CLEAR_TASK;
            } else {
                mLaunchMode &= ~LAUNCH_CLEAR_TASK;
            }
            return this;
        }

        /**
         * Sets the flow name for this target.
         * @param flowName
         * @return
         */
        public Builder setFlowName(String flowName) {
            mFlowName = flowName;
            return this;
        }

        /**
         * @return a constructed NavOptions
         */
        public NavOptions build() {
            return new NavOptions(mLaunchMode, mFlowName);
        }
    }
}
