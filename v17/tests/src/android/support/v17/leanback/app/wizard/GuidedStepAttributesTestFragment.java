package android.support.v17.leanback.app.wizard;

import android.os.Bundle;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.util.Log;

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

    public static void setActionClickCallback(long id, Callback callback) {
        sCallbacks.put(id, callback);
    }

    public static void clear() {
        LAST_CLICKED_ACTION_ID = -1;
        LAST_SELECTED_ACTION_ID = -1;
        sCallbacks.clear();
    }
}
