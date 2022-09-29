/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.extensions.embedding;

import android.app.Activity;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Description of a group of activities stacked on top of each other and shown as a single
 * container, all within the same task.
 */
public class ActivityStack {
    @NonNull
    private final List<Activity> mActivities;

    private final boolean mIsEmpty;

    public ActivityStack(@NonNull List<Activity> activities, boolean isEmpty) {
        mActivities = new ArrayList<>(activities);
        mIsEmpty = isEmpty;
    }

    /**
     * Returns {@link Activity Activities} in this application's process that belongs to this
     * ActivityStack.
     * <p>
     * Note that Activities that are running in other processes are not reported in the returned
     * Activity list. They can be in any position in terms of ordering relative to the activities
     * in the list.
     * </p>
     */
    @NonNull
    public List<Activity> getActivities() {
        return new ArrayList<>(mActivities);
    }

    /**
     * Returns {@code true} if there's no {@link Activity} running in this ActivityStack.
     * <p>
     * Note that {@link #getActivities()} only report Activity in the process used to create this
     * ActivityStack. That said, if this ActivityStack only contains activities from another
     * process, {@link #getActivities()} will return empty list, while this method will return
     * {@code false}.
     * </p>
     */
    public boolean isEmpty() {
        return mIsEmpty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivityStack)) return false;
        ActivityStack that = (ActivityStack) o;
        return mActivities.equals(that.mActivities)
                && mIsEmpty == that.mIsEmpty;
    }

    @Override
    public int hashCode() {
        int result = (mIsEmpty ? 1 : 0);
        return result * 31 + mActivities.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "ActivityStack{" + "mActivities=" + mActivities
                + ", mIsEmpty=" + mIsEmpty + '}';
    }
}
