/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app.model;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a simple list of {@link Action} models.
 *
 * <p>This model is intended for internal and host use only, as a transport artifact for
 * homogeneous lists of {@link Action} items.
 */
public class ActionList {
    private final List<Action> mList;

    /**
     * Returns the list of {@link Action}'s.
     */
    @NonNull
    public List<Action> getList() {
        return mList;
    }

    /**
     * Creates an {@link ActionList} instance based on the list of {@link Action}'s.
     */
    @NonNull
    public static ActionList create(@NonNull List<Action> list) {
        requireNonNull(list);
        for (Action action : list) {
            if (action == null) {
                throw new IllegalArgumentException("Disallowed null action found in action list");
            }
        }
        return new ActionList(list);
    }

    @Override
    @NonNull
    public String toString() {
        return mList.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mList);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ActionList)) {
            return false;
        }
        ActionList otherActionList = (ActionList) other;

        return Objects.equals(mList, otherActionList.mList);
    }

    private ActionList(List<Action> list) {
        this.mList = new ArrayList<>(list);
    }

    /** For serialization. */
    private ActionList() {
        mList = Collections.emptyList();
    }
}
