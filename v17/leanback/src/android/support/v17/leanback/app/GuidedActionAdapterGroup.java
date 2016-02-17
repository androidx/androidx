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
import android.support.v17.leanback.app.GuidedActionAdapter.ActionViewHolder;
import android.support.v17.leanback.app.GuidedActionAdapter.ClickListener;
import android.support.v17.leanback.app.GuidedActionAdapter.EditListener;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v17.leanback.widget.ImeKeyMonitor;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.util.ArrayList;

/**
 * Internal implementation manages a group of GuidedActionAdapters, control the next action after
 * editing finished, maintain the Ime open/close status.
 */
class GuidedActionAdapterGroup {

    private static final String TAG_EDIT = "EditableAction";
    private static final boolean DEBUG_EDIT = false;

    ArrayList<GuidedActionAdapter> mAdapters = new ArrayList<GuidedActionAdapter>();
    private boolean mImeOpened;
    private EditListener mEditListener;

    GuidedActionAdapterGroup() {
    }

    public void addAdpter(GuidedActionAdapter adapter) {
        mAdapters.add(adapter);
        adapter.mGroup = this;
    }

    public void setEditListener(EditListener listener) {
        mEditListener = listener;
    }

    boolean focusToNextAction(GuidedActionAdapter adapter, GuidedAction action, long nextActionId) {
        // for ACTION_ID_NEXT, we first find out the matching index in Actions list.
        int index = 0;
        if (nextActionId == GuidedAction.ACTION_ID_NEXT) {
            index = adapter.indexOf(action);
            if (index < 0) {
                return false;
            }
            // start from next, if reach end, will go next Adapter below
            index++;
        }

        int adapterIndex = mAdapters.indexOf(adapter);
        do {
            int size = adapter.getCount();
            if (nextActionId == GuidedAction.ACTION_ID_NEXT) {
                while (index < size && !adapter.getItem(index).isFocusable()) {
                    index++;
                }
            } else {
                while (index < size && adapter.getItem(index).getId() != nextActionId) {
                    index++;
                }
            }
            if (index < size) {
                ActionViewHolder vh = (ActionViewHolder) adapter.getGuidedActionsStylist()
                        .getActionsGridView().findViewHolderForPosition(index);
                if (vh != null) {
                    if (vh.getAction().isEditable() || vh.getAction().isDescriptionEditable()) {
                        if (DEBUG_EDIT) Log.v(TAG_EDIT, "openIme of next Action");
                        // open Ime on next action.
                        openIme(adapter, vh);
                    } else {
                        if (DEBUG_EDIT) Log.v(TAG_EDIT, "closeIme and focus to next Action");
                        // close IME and focus to next (not editable) action
                        closeIme(vh.mStylistViewHolder.view);
                        vh.mStylistViewHolder.view.requestFocus();
                    }
                    return true;
                }
                return false;
            }
            // search from index 0 of next Adapter
            adapterIndex++;
            if (adapterIndex >= mAdapters.size()) {
                break;
            }
            adapter = mAdapters.get(adapterIndex);
            index = 0;
        } while (true);
        return false;
    }

    public void openIme(GuidedActionAdapter adapter, ActionViewHolder avh) {
        adapter.getGuidedActionsStylist().setEditingMode(avh.mStylistViewHolder, avh.getAction(),
                true);
        View v = avh.mStylistViewHolder.getEditingView();
        if (v == null) {
            return;
        }
        InputMethodManager mgr = (InputMethodManager)
                v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        v.requestFocus();
        mgr.showSoftInput(v, 0);
        if (!mImeOpened) {
            mImeOpened = true;
            mEditListener.onImeOpen();
        }
    }

    public void closeIme(View v) {
        if (mImeOpened) {
            mImeOpened = false;
            InputMethodManager mgr = (InputMethodManager)
                    v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            mgr.hideSoftInputFromWindow(v.getWindowToken(), 0);
            mEditListener.onImeClose();
        }
    }

    private long finishEditing(GuidedActionAdapter adapter, ActionViewHolder avh) {
        long nextActionId = mEditListener.onGuidedActionEdited(avh.getAction());
        adapter.getGuidedActionsStylist().setEditingMode(avh.mStylistViewHolder, avh.getAction(),
                false);
        return nextActionId;
    }

    public void fillAndStay(GuidedActionAdapter adapter, TextView v) {
        ActionViewHolder avh = adapter.findSubChildViewHolder(v);
        updateTextIntoAction(avh, v);
        finishEditing(adapter, avh);
        closeIme(v);
        avh.mStylistViewHolder.view.requestFocus();
    }

    public void fillAndGoNext(GuidedActionAdapter adapter, TextView v) {
        boolean handled = false;
        ActionViewHolder avh = adapter.findSubChildViewHolder(v);
        updateTextIntoAction(avh, v);
        adapter.performOnActionClick(avh);
        long nextActionId = finishEditing(adapter, avh);
        if (nextActionId != GuidedAction.ACTION_ID_CURRENT
                && nextActionId != avh.getAction().getId()) {
            handled = focusToNextAction(adapter, avh.getAction(), nextActionId);
        }
        if (!handled) {
            if (DEBUG_EDIT) Log.v(TAG_EDIT, "closeIme no next action");
            handled = true;
            closeIme(v);
            avh.mStylistViewHolder.view.requestFocus();
        }
    }

    private void updateTextIntoAction(ActionViewHolder avh, TextView v) {
        GuidedAction action = avh.getAction();
        if (v == avh.mStylistViewHolder.getDescriptionView()) {
            if (action.getEditDescription() != null) {
                action.setEditDescription(v.getText());
            } else {
                action.setDescription(v.getText());
            }
        } else if (v == avh.mStylistViewHolder.getTitleView()) {
            if (action.getEditTitle() != null) {
                action.setEditTitle(v.getText());
            } else {
                action.setTitle(v.getText());
            }
        }
    }

}
