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
package android.support.v17.leanback.app;

import android.content.Context;
import android.database.DataSetObserver;
import android.media.AudioManager;
import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v17.leanback.widget.GuidedActionsStylist;
import android.support.v17.leanback.widget.ImeKeyMonitor;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.util.ArrayList;
import java.util.List;

/**
 * GuidedActionAdapter instantiates views for guided actions, and manages their interactions.
 * Presentation (view creation and state animation) is delegated to a {@link
 * GuidedActionsStylist}, while clients are notified of interactions via
 * {@link GuidedActionAdapter.ClickListener} and {@link GuidedActionAdapter.FocusListener}.
 */
class GuidedActionAdapter extends RecyclerView.Adapter {
    private static final String TAG = "GuidedActionAdapter";
    private static final boolean DEBUG = false;

    private static final String TAG_EDIT = "EditableAction";
    private static final boolean DEBUG_EDIT = false;

    /**
     * Object listening for click events within a {@link GuidedActionAdapter}.
     */
    public interface ClickListener {

        /**
         * Called when the user clicks on an action.
         */
        public void onGuidedActionClicked(GuidedAction action);
    }

    /**
     * Object listening for focus events within a {@link GuidedActionAdapter}.
     */
    public interface FocusListener {

        /**
         * Called when the user focuses on an action.
         */
        public void onGuidedActionFocused(GuidedAction action);
    }

    /**
     * Object listening for edit events within a {@link GuidedActionAdapter}.
     */
    public interface EditListener {

        /**
         * Called when the user enters or exits edit mode on an action.
         */
        public void onGuidedActionEdited(GuidedAction action, boolean entering);
    }

    /**
     * View holder containing a {@link GuidedAction}.
     */
    private static class ActionViewHolder extends ViewHolder {

        private final GuidedActionsStylist.ViewHolder mStylistViewHolder;
        private GuidedAction mAction;

        /**
         * Constructs a view holder that can be associated with a GuidedAction.
         */
        public ActionViewHolder(View v, GuidedActionsStylist.ViewHolder subViewHolder) {
            super(v);
            mStylistViewHolder = subViewHolder;
        }

        /**
         * Retrieves the action associated with this view holder.
         * @return The GuidedAction associated with this view holder.
         */
        public GuidedAction getAction() {
            return mAction;
        }

        /**
         * Sets the action associated with this view holder.
         * @param action The GuidedAction associated with this view holder.
         */
        public void setAction(GuidedAction action) {
            mAction = action;
        }
    }

    private RecyclerView mRecyclerView;
    private final ActionOnKeyListener mActionOnKeyListener;
    private final ActionOnFocusListener mActionOnFocusListener;
    private final ActionEditListener mActionEditListener;
    private final List<GuidedAction> mActions;
    private ClickListener mClickListener;
    private GuidedActionsStylist mStylist;
    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v != null && v.getWindowToken() != null && mClickListener != null) {
                ActionViewHolder avh = (ActionViewHolder)mRecyclerView.getChildViewHolder(v);
                GuidedAction action = avh.getAction();
                if (action.isEnabled() && !action.infoOnly()) {
                    mClickListener.onGuidedActionClicked(action);
                }
            }
        }
    };

    /**
     * Constructs a GuidedActionAdapter with the given list of guided actions, the given click and
     * focus listeners, and the given presenter.
     * @param actions The list of guided actions this adapter will manage.
     * @param clickListener The click listener for items in this adapter.
     * @param focusListener The focus listener for items in this adapter.
     * @param presenter The presenter that will manage the display of items in this adapter.
     */
    public GuidedActionAdapter(List<GuidedAction> actions, ClickListener clickListener,
            FocusListener focusListener, EditListener editListener,
            GuidedActionsStylist presenter) {
        super();
        mActions = new ArrayList<GuidedAction>(actions);
        mClickListener = clickListener;
        mStylist = presenter;
        mActionOnKeyListener = new ActionOnKeyListener();
        mActionOnFocusListener = new ActionOnFocusListener(focusListener);
        mActionEditListener = new ActionEditListener(editListener);
    }

    /**
     * Sets the list of actions managed by this adapter.
     * @param actions The list of actions to be managed.
     */
    public void setActions(List<GuidedAction> actions) {
        mActionOnFocusListener.unFocus();
        mActions.clear();
        mActions.addAll(actions);
        notifyDataSetChanged();
    }

    /**
     * Returns the count of actions managed by this adapter.
     * @return The count of actions managed by this adapter.
     */
    public int getCount() {
        return mActions.size();
    }

    /**
     * Returns the GuidedAction at the given position in the managed list.
     * @param position The position of the desired GuidedAction.
     * @return The GuidedAction at the given position.
     */
    public GuidedAction getItem(int position) {
        return mActions.get(position);
    }

    /**
     * Sets the click listener for items managed by this adapter.
     * @param clickListener The click listener for this adapter.
     */
    public void setClickListener(ClickListener clickListener) {
        mClickListener = clickListener;
    }

    /**
     * Sets the focus listener for items managed by this adapter.
     * @param focusListener The focus listener for this adapter.
     */
    public void setFocusListener(FocusListener focusListener) {
        mActionOnFocusListener.setFocusListener(focusListener);
    }

    /**
     * Used for serialization only.
     * @hide
     */
    public List<GuidedAction> getActions() {
        return new ArrayList<GuidedAction>(mActions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        GuidedActionsStylist.ViewHolder vh = mStylist.onCreateViewHolder(parent);
        View v = vh.view;
        v.setOnKeyListener(mActionOnKeyListener);
        v.setOnClickListener(mOnClickListener);
        v.setOnFocusChangeListener(mActionOnFocusListener);

        final EditText edit = vh.getEditableTitleView();
        if (edit != null) {
            edit.setPrivateImeOptions("EscapeNorth=1;");
            edit.setOnEditorActionListener(mActionEditListener);
            if (edit instanceof ImeKeyMonitor) {
                ImeKeyMonitor monitor = (ImeKeyMonitor)edit;
                monitor.setImeKeyListener(mActionEditListener);
            }
        }

        return new ActionViewHolder(v, vh);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (position >= mActions.size()) {
            return;
        }
        ActionViewHolder avh = (ActionViewHolder)holder;
        GuidedAction action = mActions.get(position);
        avh.setAction(action);
        mStylist.onBindViewHolder(avh.mStylistViewHolder, action);

        final EditText edit = avh.mStylistViewHolder.getEditableTitleView();
        if (edit != null) {
            int next = getNextEditableActionIndex(action);
            int flag = (next == -1) ? EditorInfo.IME_ACTION_DONE : EditorInfo.IME_ACTION_NEXT;
            edit.setImeOptions(flag);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getItemCount() {
        return mActions.size();
    }

    private int getNextEditableActionIndex(GuidedAction action) {
        int i, size = mActions.size();
        for (i = 0; i < size; i++) {
            GuidedAction a = mActions.get(i);
            if (mActions.get(i) == action) {
                i++;
                break;
            }
        }
        for (; i < size; i++) {
            GuidedAction a = mActions.get(i);
            if (a.isEditable()) {
                break;
            }
        }
        int result = (i == size) ? -1 : i;
        return result;
    }

    private class ActionOnFocusListener implements View.OnFocusChangeListener {

        private FocusListener mFocusListener;
        private View mSelectedView;

        ActionOnFocusListener(FocusListener focusListener) {
            mFocusListener = focusListener;
        }

        public void setFocusListener(FocusListener focusListener) {
            mFocusListener = focusListener;
        }

        public void unFocus() {
            if (mSelectedView != null) {
                ViewHolder vh = mRecyclerView.getChildViewHolder(mSelectedView);
                if (vh != null) {
                    ActionViewHolder avh = (ActionViewHolder)vh;
                    mStylist.onAnimateItemFocused(avh.mStylistViewHolder, false);
                } else {
                    Log.w(TAG, "RecyclerView returned null view holder",
                            new Throwable());
                }
            }
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            ActionViewHolder avh = (ActionViewHolder)mRecyclerView.getChildViewHolder(v);
            mStylist.onAnimateItemFocused(avh.mStylistViewHolder, hasFocus);
            if (hasFocus) {
                mSelectedView = v;
                if (mFocusListener != null) {
                    // We still call onGuidedActionFocused so that listeners can clear
                    // state if they want.
                    mFocusListener.onGuidedActionFocused(avh.getAction());
                }
            } else {
                if (mSelectedView == v) {
                    mSelectedView = null;
                }
            }
        }
    }

    private class ActionOnKeyListener implements View.OnKeyListener {

        private boolean mKeyPressed = false;

        private void playSound(View v, int soundEffect) {
            if (v.isSoundEffectsEnabled()) {
                Context ctx = v.getContext();
                AudioManager manager = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
                manager.playSoundEffect(soundEffect);
            }
        }

        /**
         * Now only handles KEYCODE_ENTER and KEYCODE_NUMPAD_ENTER key event.
         */
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (v == null || event == null) {
                return false;
            }
            boolean handled = false;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                case KeyEvent.KEYCODE_BUTTON_X:
                case KeyEvent.KEYCODE_BUTTON_Y:
                case KeyEvent.KEYCODE_ENTER:

                    ActionViewHolder avh = (ActionViewHolder)mRecyclerView.getChildViewHolder(v);
                    GuidedAction action = avh.getAction();

                    if (!action.isEnabled() || action.infoOnly()) {
                        if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            // TODO: requires API 19
                            //playSound(v, AudioManager.FX_KEYPRESS_INVALID);
                        }
                        return true;
                    }

                    switch (event.getAction()) {
                        case KeyEvent.ACTION_DOWN:
                            if (!mKeyPressed) {
                                mKeyPressed = true;

                                playSound(v, AudioManager.FX_KEY_CLICK);

                                if (DEBUG) {
                                    Log.d(TAG, "Enter Key down");
                                }

                                mStylist.onAnimateItemPressed(avh.mStylistViewHolder,
                                        mKeyPressed);
                                handled = true;
                            }
                            break;
                        case KeyEvent.ACTION_UP:
                            if (mKeyPressed) {
                                mKeyPressed = false;

                                if (DEBUG) {
                                    Log.d(TAG, "Enter Key up");
                                }

                                mStylist.onAnimateItemPressed(avh.mStylistViewHolder, mKeyPressed);
                                if (action.isEditable()) {
                                    if (DEBUG_EDIT) Log.v(TAG_EDIT, "openIme click");
                                    mActionEditListener.openIme(avh, true);
                                } else {
                                    handleCheckedActions(avh, action);
                                    mClickListener.onGuidedActionClicked(action);
                                }
                                handled = true;
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
            return handled;
        }

        private void handleCheckedActions(ActionViewHolder avh, GuidedAction action) {
            int actionCheckSetId = action.getCheckSetId();
            if (actionCheckSetId != GuidedAction.NO_CHECK_SET) {
                // Find any actions that are checked and are in the same group
                // as the selected action. Fade their checkmarks out.
                for (int i = 0, size = mActions.size(); i < size; i++) {
                    GuidedAction a = mActions.get(i);
                    if (a != action && a.getCheckSetId() == actionCheckSetId && a.isChecked()) {
                        a.setChecked(false);
                        ViewHolder vh = mRecyclerView.findViewHolderForPosition(i);
                        if (vh != null) {
                            GuidedActionsStylist.ViewHolder subViewHolder =
                                    ((ActionViewHolder)vh).mStylistViewHolder;
                            mStylist.onAnimateItemChecked(subViewHolder, false);
                        }
                    }
                }

                // If we we'ren't already checked, fade our checkmark in.
                if (!action.isChecked()) {
                    action.setChecked(true);
                    mStylist.onAnimateItemChecked(avh.mStylistViewHolder, true);
                }
            }
        }
    }

    private class ActionEditListener implements OnEditorActionListener,
            ImeKeyMonitor.ImeKeyListener {

        private EditListener mEditListener;

        public ActionEditListener(EditListener listener) {
            mEditListener = listener;
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (DEBUG_EDIT) Log.v(TAG_EDIT, "IME action: " + actionId);
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                actionId == EditorInfo.IME_ACTION_DONE) {

                ActionViewHolder avh = findSubChildViewHolder(v);
                GuidedAction action = avh.getAction();
                if (action.getEditTitle() != null) {
                    action.setEditTitle(v.getText());
                } else {
                    action.setTitle(v.getText());
                }
                mClickListener.onGuidedActionClicked(action);
                int next = getNextEditableActionIndex(action);
                if (next != -1) {
                    ViewHolder vh = mRecyclerView.findViewHolderForPosition(next);
                    if (vh != null) {
                        if (DEBUG_EDIT) Log.v(TAG_EDIT, "openIme next/done");
                        handled = true;
                        openIme((ActionViewHolder)vh, false);
                    }
                }
                if (!handled) {
                    if (DEBUG_EDIT) Log.v(TAG_EDIT, "closeIme no next");
                    handled = true;
                    closeIme(avh);
                }
            } else if (actionId == EditorInfo.IME_ACTION_NONE) {
                if (DEBUG_EDIT) Log.v(TAG_EDIT, "closeIme escape north");
                // Escape north handling: stay on current item, but close editor
                handled = true;
                ActionViewHolder avh = findSubChildViewHolder(v);
                closeIme(avh);
            }
            return handled;
        }

        @Override
        public boolean onKeyPreIme(EditText editText, int keyCode, KeyEvent event) {
            if (DEBUG_EDIT) Log.v(TAG_EDIT, "IME key: " + keyCode);
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                ActionViewHolder avh = findSubChildViewHolder(editText);
                GuidedAction action = avh.getAction();
                if (action.isEditTitleUsed()) {
                    action.setEditTitle(editText.getText());
                } else {
                    action.setTitle(editText.getText());
                }
                editText.clearFocus();
                mEditListener.onGuidedActionEdited(action, false);
                updateTitleAndDescription(avh);
            }
            return false;
        }

        public void openIme(ActionViewHolder avh, boolean notify) {
            View v = avh.mStylistViewHolder.getTitleView();
            CharSequence editTitle = avh.getAction().getEditTitle();
            if (editTitle != null) {
                ((TextView) v).setText(editTitle);
            }
            InputMethodManager mgr = (InputMethodManager)
                    v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            v.requestFocus();
            mgr.showSoftInput(v, 0);
            if (notify) {
                mEditListener.onGuidedActionEdited(avh.getAction(), true);
            }
        }

        public void closeIme(ActionViewHolder avh) {
            View v = avh.mStylistViewHolder.getTitleView();
            InputMethodManager mgr = (InputMethodManager)
                    v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            v.clearFocus();
            mgr.hideSoftInputFromWindow(v.getWindowToken(), 0);
            mEditListener.onGuidedActionEdited(avh.getAction(), false);
            updateTitleAndDescription(avh);
        }

        private void updateTitleAndDescription(ActionViewHolder avh) {
            avh.mStylistViewHolder.getTitleView().setText(avh.getAction().getTitle());
            avh.mStylistViewHolder.getDescriptionView().setText(avh.getAction().getDescription());
        }

        private ActionViewHolder findSubChildViewHolder(View v) {
            // Needed because RecyclerView.getChildViewHolder does not traverse the hierarchy
            ActionViewHolder result = null;
            ViewParent parent = v.getParent();
            while (parent != mRecyclerView && parent != null && v != null) {
                v = (View)parent;
                parent = parent.getParent();
            }
            if (parent != null && v != null) {
                result = (ActionViewHolder)mRecyclerView.getChildViewHolder(v);
            }
            return result;
        }
    }

}
