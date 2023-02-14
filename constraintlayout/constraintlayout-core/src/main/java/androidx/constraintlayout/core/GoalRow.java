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

package androidx.constraintlayout.core;

public class GoalRow extends ArrayRow {

    public GoalRow(Cache cache) {
        super(cache);
    }

    @Override
    public void addError(SolverVariable error) {
        super.addError(error);
        // error variables in the goal shouldn't be tracked (we only care if they are
        // in the system rows)
        error.usageInRowCount--;
    }
}
