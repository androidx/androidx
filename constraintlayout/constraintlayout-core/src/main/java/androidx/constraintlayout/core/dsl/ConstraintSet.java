/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.core.dsl;

import java.util.ArrayList;

/**
 * Provides the API for creating a ConstraintSet Object for use in the Core
 * ConstraintLayout & MotionLayout system
 */
public class ConstraintSet {
    private final String mName;
    ArrayList<Constraint> mConstraints = new ArrayList<>();
    ArrayList<Helper> mHelpers = new ArrayList<>();

    public ConstraintSet(String name) {
        mName = name;
    }

    public void add(Constraint c) {
        mConstraints.add(c);
    }

    public void add(Helper h) {
        mHelpers.add(h);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder(mName + ":{\n");
        if (!mConstraints.isEmpty()) {
            for (Constraint cs: mConstraints) {
                ret.append(cs.toString());
            }
        }

        if (!mHelpers.isEmpty()) {
            for (Helper h: mHelpers) {
                ret.append(h.toString());
            }
        }

        ret.append("},\n");
        return ret.toString();
    }
}
