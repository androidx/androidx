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
package android.support.v17.leanback.widget;

import static android.support.v17.leanback.widget.ObjectAdapter.NO_ID;

/**
 * An action that can be shown on a details page.
 */
public class Action {

    private long mId = NO_ID;
    private CharSequence mLabel;

    public Action(long id) {
        this(id, "");
    }

    public Action(long id, CharSequence label) {
        setId(id);
        setLabel(label);
    }

    /**
     * Set id for this action.
     */
    public final void setId(long id) {
        mId = id;
    }

    /**
     * Returns the id for this action.
     */
    public final long getId() {
        return mId;
    }

    /**
     * Set the label for this action.
     */
    public final void setLabel(CharSequence label) {
        mLabel = label;
    }

    /**
     * Returns the label for this action.
     */
    public final CharSequence getLabel() {
        return mLabel;
    }
}
