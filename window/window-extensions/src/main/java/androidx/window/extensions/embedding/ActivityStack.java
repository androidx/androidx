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
import androidx.window.extensions.ExperimentalWindowExtensionsApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Description of a group of activities stacked on top of each other and shown as a single
 * container, all within the same task.
 */
@ExperimentalWindowExtensionsApi
public class ActivityStack {
    @NonNull
    private final List<Activity> mActivities;

    public ActivityStack(@NonNull List<Activity> activities) {
        mActivities = new ArrayList<>(activities);
    }

    @NonNull
    public List<Activity> getActivities() {
        return new ArrayList<>(mActivities);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivityStack)) return false;
        ActivityStack that = (ActivityStack) o;
        return mActivities.equals(that.mActivities);
    }

    @Override
    public int hashCode() {
        return mActivities.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "ActivityStack{" + "mActivities=" + mActivities + '}';
    }
}
