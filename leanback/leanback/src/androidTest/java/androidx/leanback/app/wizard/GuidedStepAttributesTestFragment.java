/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.leanback.app.wizard;

import android.os.Bundle;

import androidx.leanback.app.GuidedStepFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import java.util.HashMap;
import java.util.List;

public class GuidedStepAttributesTestFragment extends GuidedStepFragment {

    private static String TAG = "GuidedStepAttributesTestFragment";
    static class Callback {
        public void onActionClicked(GuidedStepFragment fragment, long id) {
        }
    }

    static HashMap<Long, Callback> sCallbacks = new HashMap();
    public static GuidanceStylist.Guidance GUIDANCE = null;
    public static List<GuidedAction> ACTION_LIST = null;
    public static long LAST_CLICKED_ACTION_ID = -1;
    public static long LAST_SELECTED_ACTION_ID = -1;

    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        if (GUIDANCE == null ) {
            return new GuidanceStylist.Guidance("", "", "", null);
        }
        return GUIDANCE;
    }

    @Override
    public void onCreateActions(List<GuidedAction> actions, Bundle savedInstanceState) {
        if (ACTION_LIST == null)
            return;
        actions.addAll(ACTION_LIST);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        super.onGuidedActionFocused(action);
        Callback callback = sCallbacks.get(action.getId());
        if (callback != null) {
            callback.onActionClicked(this, action.getId());
        } else {
            LAST_CLICKED_ACTION_ID = action.getId();
        }
    }

    @Override
    public void onGuidedActionFocused(GuidedAction action) {
        super.onGuidedActionFocused(action);
        LAST_SELECTED_ACTION_ID = action.getId();
    }

    @Override
    public boolean onSubGuidedActionClicked(GuidedAction action) {
        super.onSubGuidedActionClicked(action);
        LAST_CLICKED_ACTION_ID = action.getId();
        return true;
    }

    @Override
    public long onGuidedActionEditedAndProceed(GuidedAction action) {

        Callback callback = sCallbacks.get(action.getId());
        if (callback != null) {
            callback.onActionClicked(this, action.getId());
        } else {
            super.onGuidedActionEditedAndProceed(action);
        }
        return GuidedAction.ACTION_ID_CURRENT;
    }

    public static void setActionClickCallback(long id, Callback callback) {
        sCallbacks.put(id, callback);
    }

    public static void clear() {
        LAST_CLICKED_ACTION_ID = -1;
        LAST_SELECTED_ACTION_ID = -1;
        sCallbacks.clear();
    }
}
