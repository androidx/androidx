/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * A data class which represents an action within a {@link
 * android.support.v17.leanback.app.GuidedStepFragment}. GuidedActions contain at minimum a title
 * and a description, and typically also an icon.
 * <p>
 * A GuidedAction typically represents a single action a user may take, but may also represent a
 * possible choice out of a group of mutually exclusive choices (similar to radio buttons), or an
 * information-only label (in which case the item cannot be clicked).
 * <p>
 * GuidedActions may optionally be checked. They may also indicate that they will request further
 * user input on selection, in which case they will be displayed with a chevron indicator.
 */
public class GuidedAction extends Action {

    private static final String TAG = "GuidedAction";

    public static final int NO_DRAWABLE = 0;
    public static final int NO_CHECK_SET = 0;
    public static final int DEFAULT_CHECK_SET_ID = 1;

    /**
     * Builds a {@link GuidedAction} object.
     */
    public static class Builder {
        private long mId;
        private String mTitle;
        private String mDescription;
        private Drawable mIcon;
        private boolean mChecked;
        private boolean mMultilineDescription;
        private boolean mHasNext;
        private boolean mInfoOnly;
        private int mCheckSetId = NO_CHECK_SET;
        private boolean mEnabled = true;
        private Intent mIntent;

        /**
         * Builds the GuidedAction corresponding to this Builder.
         * @return the GuidedAction as configured through this Builder.
         */
        public GuidedAction build() {
            GuidedAction action = new GuidedAction();
            // Base Action values
            action.setId(mId);
            action.setLabel1(mTitle);
            action.setLabel2(mDescription);
            action.setIcon(mIcon);

            // Subclass values
            action.mIntent = mIntent;
            action.mChecked = mChecked;
            action.mCheckSetId = mCheckSetId;
            action.mMultilineDescription = mMultilineDescription;
            action.mHasNext = mHasNext;
            action.mInfoOnly = mInfoOnly;
            action.mEnabled = mEnabled;
            return action;
        }

        /**
         * Sets the ID associated with this action.  The ID can be any value the client wishes;
         * it is typically used to determine what to do when an action is clicked.
         * @param id The ID to associate with this action.
         */
        public Builder id(long id) {
            mId = id;
            return this;
        }

        /**
         * Sets the title for this action.  The title is typically a short string indicating the
         * action to be taken on click, e.g. "Continue" or "Cancel".
         * @param title The title for this action.
         */
        public Builder title(String title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets the description for this action.  The description is typically a longer string
         * providing extra information on what the action will do.
         * @param description The description for this action.
         */
        public Builder description(String description) {
            mDescription = description;
            return this;
        }

        /**
         * Sets the intent associated with this action.  Clients would typically fire this intent
         * directly when the action is clicked.
         * @param intent The intent associated with this action.
         */
        public Builder intent(Intent intent) {
            mIntent = intent;
            return this;
        }

        /**
         * Sets the action's icon drawable.
         * @param icon The drawable for the icon associated with this action.
         */
        public Builder icon(Drawable icon) {
            mIcon = icon;
            return this;
        }

        /**
         * Sets the action's icon drawable by retrieving it by resource ID from the specified
         * context. This is a convenience function that simply looks up the drawable and calls
         * {@link #icon}.
         * @param iconResourceId The resource ID for the icon associated with this action.
         * @param context The context whose resource ID should be retrieved.
         */
        public Builder iconResourceId(int iconResourceId, Context context) {
            return icon(context.getResources().getDrawable(iconResourceId));
        }

        /**
         * Indicates whether this action is initially checked.
         * @param checked Whether this action is checked.
         */
        public Builder checked(boolean checked) {
            mChecked = checked;
            return this;
        }

        /**
         * Indicates whether this action is part of a single-select group similar to radio buttons.
         * When one item in a check set is checked, all others with the same check set ID will be
         * unchecked automatically.
         * @param checkSetId The check set ID, or {@link #NO_CHECK_SET) to indicate no check set.
         */
        public Builder checkSetId(int checkSetId) {
            mCheckSetId = checkSetId;
            return this;
        }

        /**
         * Indicates whether the title and description are long, and should be displayed
         * appropriately.
         * @param multilineDescription Whether this action has a multiline description.
         */
        public Builder multilineDescription(boolean multilineDescription) {
            mMultilineDescription = multilineDescription;
            return this;
        }

        /**
         * Indicates whether this action has a next state and should display a chevron.
         * @param hasNext Whether this action has a next state.
         */
        public Builder hasNext(boolean hasNext) {
            mHasNext = hasNext;
            return this;
        }

        /**
         * Indicates whether this action is for information purposes only and cannot be clicked.
         * @param infoOnly Whether this action has a next state.
         */
        public Builder infoOnly(boolean infoOnly) {
            mInfoOnly = infoOnly;
            return this;
        }

        /**
         * Indicates whether this action is enabled.  If not enabled, an action cannot be clicked.
         * @param enabled Whether the action is enabled.
         */
        public Builder enabled(boolean enabled) {
            mEnabled = enabled;
            return this;
        }
    }

    private boolean mChecked;
    private boolean mMultilineDescription;
    private boolean mHasNext;
    private boolean mInfoOnly;
    private int mCheckSetId;
    private boolean mEnabled;

    private Intent mIntent;

    private GuidedAction() {
        super(0);
    }

    /**
     * Returns the title of this action.
     * @return The title set when this action was built.
     */
    public CharSequence getTitle() {
        return getLabel1();
    }

    /**
     * Returns the description of this action.
     * @return The description set when this action was built.
     */
    public CharSequence getDescription() {
        return getLabel2();
    }

    /**
     * Returns the intent associated with this action.
     * @return The intent set when this action was built.
     */
    public Intent getIntent() {
        return mIntent;
    }

    /**
     * Returns whether this action is checked.
     * @return true if the action is currently checked, false otherwise.
     */
    public boolean isChecked() {
        return mChecked;
    }

    /**
     * Sets whether this action is checked.
     * @param checked Whether this action should be checked.
     */
    public void setChecked(boolean checked) {
        mChecked = checked;
    }

    /**
     * Returns the check set id this action is a part of. All actions in the
     * same list with the same check set id are considered linked. When one
     * of the actions within that set is selected, that action becomes
     * checked, while all the other actions become unchecked.
     *
     * @return an integer representing the check set this action is a part of, or
     *         {@link #NO_CHECK_SET} if this action isn't a part of a check set.
     */
    public int getCheckSetId() {
        return mCheckSetId;
    }

    /**
     * Returns whether this action is has a multiline description.
     * @return true if the action was constructed as having a multiline description, false
     * otherwise.
     */
    public boolean hasMultilineDescription() {
        return mMultilineDescription;
    }

    /**
     * Returns whether this action is enabled.
     * @return true if the action is currently enabled, false otherwise.
     */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Sets whether this action is enabled.
     * @param enabled Whether this action should be enabled.
     */
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    /**
     * Returns whether this action will request further user input when selected, such as showing
     * another GuidedStepFragment or launching a new activity. Configured during construction.
     * @return true if the action will request further user input when selected, false otherwise.
     */
    public boolean hasNext() {
        return mHasNext;
    }

    /**
     * Returns whether the action will only display information and is thus not clickable. If both
     * this and {@link #hasNext()} are true, infoOnly takes precedence. The default is false. For
     * example, this might represent e.g. the amount of storage a document uses, or the cost of an
     * app.
     * @return true if will only display information, false otherwise.
     */
    public boolean infoOnly() {
        return mInfoOnly;
    }

}
