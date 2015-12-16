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

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v17.leanback.R;
import android.support.v4.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.InputType;

import java.util.List;

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
 * <p>
 * GuidedAction recommends to use {@link Builder}. When application subclass GuidedAction, it
 * can subclass {@link BuilderBase}, implement {@link BuilderBase#build()} where it should
 * call {@link BuilderBase#applyValues(GuidedAction)}.
 */
public class GuidedAction extends Action {

    private static final String TAG = "GuidedAction";

    /**
     * Special check set Id that is neither checkbox nor radio.
     */
    public static final int NO_CHECK_SET = 0;
    /**
     * Default checkset Id for radio.
     */
    public static final int DEFAULT_CHECK_SET_ID = 1;
    /**
     * Checkset Id for checkbox.
     */
    public static final int CHECKBOX_CHECK_SET_ID = -1;

    /**
     * When finishing editing, goes to next action.
     */
    public static final long ACTION_ID_NEXT = -2;
    /**
     * When finishing editing, stay on current action.
     */
    public static final long ACTION_ID_CURRENT = -3;

    /**
     * Id of standard OK action.
     */
    public static final long ACTION_ID_OK = -4;

    /**
     * Id of standard Cancel action.
     */
    public static final long ACTION_ID_CANCEL = -5;

    /**
     * Id of standard Finish action.
     */
    public static final long ACTION_ID_FINISH = -6;

    /**
     * Id of standard Finish action.
     */
    public static final long ACTION_ID_CONTINUE = -7;

    /**
     * Id of standard Yes action.
     */
    public static final long ACTION_ID_YES = -8;

    /**
     * Id of standard No action.
     */
    public static final long ACTION_ID_NO = -9;

    /**
     * Base builder class to build a {@link GuidedAction} object.  When subclass GuidedAction, you
     * can override this BuilderBase class, implements {@link #build()} and call
     * {@link #applyValues(GuidedAction)}.  When using GuidedAction directly, use {@link Builder}.
     */
    public abstract static class BuilderBase<T extends GuidedAction> {
        private Context mContext;
        private long mId;
        private CharSequence mTitle;
        private CharSequence mEditTitle;
        private CharSequence mDescription;
        private CharSequence mEditDescription;
        private Drawable mIcon;
        private boolean mChecked;
        private boolean mMultilineDescription;
        private boolean mHasNext;
        private boolean mInfoOnly;
        private boolean mEditable = false;
        private boolean mDescriptionEditable = false;
        private int mInputType = InputType.TYPE_CLASS_TEXT;
        private int mDescriptionInputType = InputType.TYPE_CLASS_TEXT;
        private int mEditInputType = InputType.TYPE_CLASS_TEXT;
        private int mDescriptionEditInputType = InputType.TYPE_CLASS_TEXT;
        private int mCheckSetId = NO_CHECK_SET;
        private boolean mEnabled = true;
        private boolean mFocusable = true;
        private List<GuidedAction> mSubActions;
        private Intent mIntent;

        /**
         * Creates a BuilderBase for GuidedAction or its subclass.
         * @param context Context object used to build the GuidedAction.
         */
        public BuilderBase(Context context) {
            mContext = context;
        }

        /**
         * Returns Context of this Builder.
         * @return Context of this Builder.
         */
        public Context getContext() {
            return mContext;
        }

        /**
         * Builds the GuidedAction corresponding to this Builder.
         * @return the GuidedAction as configured through this BuilderBase.
         */
        public abstract T build();

        /**
         * Subclass of BuilderBase should call this function to apply values.
         * @param action GuidedAction to apply BuilderBase values.
         */
        protected final void applyValues(GuidedAction action) {
            // Base Action values
            action.setId(mId);
            action.setLabel1(mTitle);
            action.setEditTitle(mEditTitle);
            action.setLabel2(mDescription);
            action.setEditDescription(mEditDescription);
            action.setIcon(mIcon);

            // Subclass values
            action.mIntent = mIntent;
            action.mEditable = mEditable;
            action.mDescriptionEditable = mDescriptionEditable;
            action.mInputType = mInputType;
            action.mDescriptionInputType = mDescriptionInputType;
            action.mEditInputType = mEditInputType;
            action.mDescriptionEditInputType = mDescriptionEditInputType;
            action.mChecked = mChecked;
            action.mCheckSetId = mCheckSetId;
            action.mMultilineDescription = mMultilineDescription;
            action.mHasNext = mHasNext;
            action.mInfoOnly = mInfoOnly;
            action.mEnabled = mEnabled;
            action.mFocusable = mFocusable;
            action.mSubActions = mSubActions;
        }

        /**
         * Construct a clickable action with associated id and auto assign pre-defined title for the
         * action. If the id is not supported, the method simply does nothing.
         * @param id One of {@link GuidedAction#ACTION_ID_OK} {@link GuidedAction#ACTION_ID_CANCEL}
         * {@link GuidedAction#ACTION_ID_FINISH} {@link GuidedAction#ACTION_ID_CONTINUE}
         * {@link GuidedAction#ACTION_ID_YES} {@link GuidedAction#ACTION_ID_NO}.
         * @return The same BuilderBase object.
         */
        public BuilderBase<T> clickAction(long id) {
            if (id == ACTION_ID_OK) {
                mId = ACTION_ID_OK;
                mTitle = mContext.getString(android.R.string.ok);
            } else if (id == ACTION_ID_CANCEL) {
                mId = ACTION_ID_CANCEL;
                mTitle = mContext.getString(android.R.string.cancel);
            } else if (id == ACTION_ID_FINISH) {
                mId = ACTION_ID_FINISH;
                mTitle = mContext.getString(R.string.lb_guidedaction_finish_title);
            } else if (id == ACTION_ID_CONTINUE) {
                mId = ACTION_ID_CONTINUE;
                mTitle = mContext.getString(R.string.lb_guidedaction_continue_title);
            } else if (id == ACTION_ID_YES) {
                mId = ACTION_ID_YES;
                mTitle = mContext.getString(android.R.string.yes);
            } else if (id == ACTION_ID_NO) {
                mId = ACTION_ID_NO;
                mTitle = mContext.getString(android.R.string.no);
            }
            return this;
        }

        /**
         * Sets the ID associated with this action.  The ID can be any value the client wishes;
         * it is typically used to determine what to do when an action is clicked.
         * @param id The ID to associate with this action.
         */
        public BuilderBase<T> id(long id) {
            mId = id;
            return this;
        }

        /**
         * Sets the title for this action.  The title is typically a short string indicating the
         * action to be taken on click, e.g. "Continue" or "Cancel".
         * @param title The title for this action.
         */
        public BuilderBase<T> title(CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets the title for this action.  The title is typically a short string indicating the
         * action to be taken on click, e.g. "Continue" or "Cancel".
         * @param titleResourceId The resource id of title for this action.
         */
        public BuilderBase<T> title(@StringRes int titleResourceId) {
            mTitle = getContext().getString(titleResourceId);
            return this;
        }

        /**
         * Sets the optional title text to edit.  When TextView is activated, the edit title
         * replaces the string of title.
         * @param editTitle The optional title text to edit when TextView is activated.
         */
        public BuilderBase<T> editTitle(CharSequence editTitle) {
            mEditTitle = editTitle;
            return this;
        }

        /**
         * Sets the optional title text to edit.  When TextView is activated, the edit title
         * replaces the string of title.
         * @param editTitleResourceId String resource id of the optional title text to edit when
         * TextView is activated.
         */
        public BuilderBase<T> editTitle(@StringRes int editTitleResourceId) {
            mEditTitle = getContext().getString(editTitleResourceId);
            return this;
        }

        /**
         * Sets the description for this action.  The description is typically a longer string
         * providing extra information on what the action will do.
         * @param description The description for this action.
         */
        public BuilderBase<T> description(CharSequence description) {
            mDescription = description;
            return this;
        }

        /**
         * Sets the description for this action.  The description is typically a longer string
         * providing extra information on what the action will do.
         * @param descriptionResourceId String resource id of the description for this action.
         */
        public BuilderBase<T> description(@StringRes int descriptionResourceId) {
            mDescription = getContext().getString(descriptionResourceId);
            return this;
        }

        /**
         * Sets the optional description text to edit.  When TextView is activated, the edit
         * description replaces the string of description.
         * @param description The description to edit for this action.
         */
        public BuilderBase<T> editDescription(CharSequence description) {
            mEditDescription = description;
            return this;
        }

        /**
         * Sets the optional description text to edit.  When TextView is activated, the edit
         * description replaces the string of description.
         * @param descriptionResourceId String resource id of the description to edit for this
         * action.
         */
        public BuilderBase<T> editDescription(@StringRes int descriptionResourceId) {
            mEditDescription = getContext().getString(descriptionResourceId);
            return this;
        }

        /**
         * Sets the intent associated with this action.  Clients would typically fire this intent
         * directly when the action is clicked.
         * @param intent The intent associated with this action.
         */
        public BuilderBase<T> intent(Intent intent) {
            mIntent = intent;
            return this;
        }

        /**
         * Sets the action's icon drawable.
         * @param icon The drawable for the icon associated with this action.
         */
        public BuilderBase<T> icon(Drawable icon) {
            mIcon = icon;
            return this;
        }

        /**
         * Sets the action's icon drawable by retrieving it by resource ID from the specified
         * context. This is a convenience function that simply looks up the drawable and calls
         * {@link #icon(Drawable)}.
         * @param iconResourceId The resource ID for the icon associated with this action.
         * @param context The context whose resource ID should be retrieved.
         * @deprecated Use {@link #icon(int)}.
         */
        @Deprecated
        public BuilderBase<T> iconResourceId(@DrawableRes int iconResourceId, Context context) {
            return icon(ContextCompat.getDrawable(context, iconResourceId));
        }

        /**
         * Sets the action's icon drawable by retrieving it by resource ID from Builder's
         * context. This is a convenience function that simply looks up the drawable and calls
         * {@link #icon(Drawable)}.
         * @param iconResourceId The resource ID for the icon associated with this action.
         */
        public BuilderBase<T> icon(@DrawableRes int iconResourceId) {
            return icon(ContextCompat.getDrawable(getContext(), iconResourceId));
        }

        /**
         * Indicates whether this action title is editable. Note: Editable actions cannot also be
         * checked, or belong to a check set.
         * @param editable Whether this action is editable.
         */
        public BuilderBase<T> editable(boolean editable) {
            mEditable = editable;
            if (mChecked || mCheckSetId != NO_CHECK_SET) {
                throw new IllegalArgumentException("Editable actions cannot also be checked");
            }
            return this;
        }

        /**
         * Indicates whether this action's description is editable
         * @param editable Whether this action description is editable.
         */
        public BuilderBase<T> descriptionEditable(boolean editable) {
            mDescriptionEditable = editable;
            if (mChecked || mCheckSetId != NO_CHECK_SET) {
                throw new IllegalArgumentException("Editable actions cannot also be checked");
            }
            return this;
        }

        /**
         * Sets {@link InputType} of this action title not in editing.
         *
         * @param inputType InputType for the action title not in editing.
         */
        public BuilderBase<T> inputType(int inputType) {
            mInputType = inputType;
            return this;
        }

        /**
         * Sets {@link InputType} of this action description not in editing.
         *
         * @param inputType InputType for the action description not in editing.
         */
        public BuilderBase<T> descriptionInputType(int inputType) {
            mDescriptionInputType = inputType;
            return this;
        }


        /**
         * Sets {@link InputType} of this action title in editing.
         *
         * @param inputType InputType for the action title in editing.
         */
        public BuilderBase<T> editInputType(int inputType) {
            mEditInputType = inputType;
            return this;
        }

        /**
         * Sets {@link InputType} of this action description in editing.
         *
         * @param inputType InputType for the action description in editing.
         */
        public BuilderBase<T> descriptionEditInputType(int inputType) {
            mDescriptionEditInputType = inputType;
            return this;
        }


        /**
         * Indicates whether this action is initially checked.
         * @param checked Whether this action is checked.
         */
        public BuilderBase<T> checked(boolean checked) {
            mChecked = checked;
            if (mEditable || mDescriptionEditable) {
                throw new IllegalArgumentException("Editable actions cannot also be checked");
            }
            return this;
        }

        /**
         * Indicates whether this action is part of a single-select group similar to radio buttons
         * or this action is a checkbox. When one item in a check set is checked, all others with
         * the same check set ID will be checked automatically.
         * @param checkSetId The check set ID, or {@link GuidedAction#NO_CHECK_SET} to indicate not
         * radio or checkbox, or {@link GuidedAction#CHECKBOX_CHECK_SET_ID} to indicate a checkbox.
         */
        public BuilderBase<T> checkSetId(int checkSetId) {
            mCheckSetId = checkSetId;
            if (mEditable || mDescriptionEditable) {
                throw new IllegalArgumentException("Editable actions cannot also be in check sets");
            }
            return this;
        }

        /**
         * Indicates whether the title and description are long, and should be displayed
         * appropriately.
         * @param multilineDescription Whether this action has a multiline description.
         */
        public BuilderBase<T> multilineDescription(boolean multilineDescription) {
            mMultilineDescription = multilineDescription;
            return this;
        }

        /**
         * Indicates whether this action has a next state and should display a chevron.
         * @param hasNext Whether this action has a next state.
         */
        public BuilderBase<T> hasNext(boolean hasNext) {
            mHasNext = hasNext;
            return this;
        }

        /**
         * Indicates whether this action is for information purposes only and cannot be clicked.
         * @param infoOnly Whether this action has a next state.
         */
        public BuilderBase<T> infoOnly(boolean infoOnly) {
            mInfoOnly = infoOnly;
            return this;
        }

        /**
         * Indicates whether this action is enabled.  If not enabled, an action cannot be clicked.
         * @param enabled Whether the action is enabled.
         */
        public BuilderBase<T> enabled(boolean enabled) {
            mEnabled = enabled;
            return this;
        }

        /**
         * Indicates whether this action can take focus.
         * @param focusable
         * @return The same BuilderBase object.
         */
        public BuilderBase<T> focusable(boolean focusable) {
            mFocusable = focusable;
            return this;
        }

        /**
         * Sets sub actions list.
         * @param subActions
         * @return The same BuilderBase object.
         */
        public BuilderBase<T> subActions(List<GuidedAction> subActions) {
            mSubActions = subActions;
            return this;
        }
    }

    /**
     * Builds a {@link GuidedAction} object.
     */
    public static class Builder extends BuilderBase<GuidedAction> {

        /**
         * @deprecated Use {@link GuidedAction.Builder#GuidedAction.Builder(Context)}.
         */
        @Deprecated
        public Builder() {
            super(null);
        }

        /**
         * Creates a Builder for GuidedAction.
         * @param context Context to build GuidedAction.
         */
        public Builder(Context context) {
            super(context);
        }

        @Override
        public GuidedAction build() {
            GuidedAction action = new GuidedAction();
            applyValues(action);
            return action;
        }

    }

    private CharSequence mEditTitle;
    private CharSequence mEditDescription;
    private boolean mEditable;
    private boolean mDescriptionEditable;
    private int mInputType;
    private int mDescriptionInputType;
    private int mEditInputType;
    private int mDescriptionEditInputType;
    private boolean mMultilineDescription;
    private boolean mHasNext;
    private boolean mChecked;
    private boolean mInfoOnly;
    private int mCheckSetId;
    private boolean mEnabled;
    private boolean mFocusable;
    private List<GuidedAction> mSubActions;

    private Intent mIntent;

    protected GuidedAction() {
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
     * Sets the title of this action.
     * @param title The title set when this action was built.
     */
    public void setTitle(CharSequence title) {
        setLabel1(title);
    }

    /**
     * Returns the optional title text to edit.  When not null, it is being edited instead of
     * {@link #getTitle()}.
     * @return Optional title text to edit instead of {@link #getTitle()}.
     */
    public CharSequence getEditTitle() {
        return mEditTitle;
    }

    /**
     * Sets the optional title text to edit instead of {@link #setTitle(CharSequence)}.
     * @param editTitle Optional title text to edit instead of {@link #setTitle(CharSequence)}.
     */
    public void setEditTitle(CharSequence editTitle) {
        mEditTitle = editTitle;
    }

    /**
     * Returns the optional description text to edit.  When not null, it is being edited instead of
     * {@link #getDescription()}.
     * @return Optional description text to edit instead of {@link #getDescription()}.
     */
    public CharSequence getEditDescription() {
        return mEditDescription;
    }

    /**
     * Sets the optional description text to edit instead of {@link #setDescription(CharSequence)}.
     * @param editDescription Optional description text to edit instead of
     * {@link #setDescription(CharSequence)}.
     */
    public void setEditDescription(CharSequence editDescription) {
        mEditDescription = editDescription;
    }

    /**
     * Returns true if {@link #getEditTitle()} is not null.  When true, the {@link #getEditTitle()}
     * is being edited instead of {@link #getTitle()}.
     * @return true if {@link #getEditTitle()} is not null.
     */
    public boolean isEditTitleUsed() {
        return mEditTitle != null;
    }

    /**
     * Returns the description of this action.
     * @return The description of this action.
     */
    public CharSequence getDescription() {
        return getLabel2();
    }

    /**
     * Sets the description of this action.
     * @param description The description of the action.
     */
    public void setDescription(CharSequence description) {
        setLabel2(description);
    }

    /**
     * Returns the intent associated with this action.
     * @return The intent set when this action was built.
     */
    public Intent getIntent() {
        return mIntent;
    }

    /**
     * Sets the intent of this action.
     * @param intent New intent to set on this action.
     */
    public void setIntent(Intent intent) {
        mIntent = intent;
    }

    /**
     * Returns whether this action title is editable.
     * @return true if the action title is editable, false otherwise.
     */
    public boolean isEditable() {
        return mEditable;
    }

    /**
     * Returns whether this action description is editable.
     * @return true if the action description is editable, false otherwise.
     */
    public boolean isDescriptionEditable() {
        return mDescriptionEditable;
    }

    /**
     * Returns InputType of action title in editing; only valid when {@link #isEditable()} is true.
     * @return InputType of action title in editing.
     */
    public int getEditInputType() {
        return mEditInputType;
    }

    /**
     * Returns InputType of action description in editing; only valid when
     * {@link #isDescriptionEditable()} is true.
     * @return InputType of action description in editing.
     */
    public int getDescriptionEditInputType() {
        return mDescriptionEditInputType;
    }

    /**
     * Returns InputType of action title not in editing.
     * @return InputType of action title not in editing.
     */
    public int getInputType() {
        return mInputType;
    }

    /**
     * Returns InputType of action description not in editing.
     * @return InputType of action description not in editing.
     */
    public int getDescriptionInputType() {
        return mDescriptionInputType;
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
     * Returns the check set id this action is a part of. All actions in the same list with the same
     * check set id are considered linked. When one of the actions within that set is selected, that
     * action becomes checked, while all the other actions become unchecked.
     *
     * @return an integer representing the check set this action is a part of, or
     *         {@link #CHECKBOX_CHECK_SET_ID} if this is a checkbox, or {@link #NO_CHECK_SET} if
     *         this action is not a checkbox or radiobutton.
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
     * Returns whether this action is focusable.
     * @return true if the action is currently focusable, false otherwise.
     */
    public boolean isFocusable() {
        return mFocusable;
    }

    /**
     * Sets whether this action is focusable.
     * @param focusable Whether this action should be focusable.
     */
    public void setFocusable(boolean focusable) {
        mFocusable = focusable;
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

    /**
     * Change sub actions list.
     * @param actions Sub actions list to set on this action.  Sets null to disable sub actions.
     */
    public void setSubActions(List<GuidedAction> actions) {
        mSubActions = actions;
    }

    /**
     * @return List of sub actions or null if sub actions list is not enabled.
     */
    public List<GuidedAction> getSubActions() {
        return mSubActions;
    }

    /**
     * @return True if has sub actions list, even it's currently empty.
     */
    public boolean hasSubActions() {
        return mSubActions != null;
    }

}
