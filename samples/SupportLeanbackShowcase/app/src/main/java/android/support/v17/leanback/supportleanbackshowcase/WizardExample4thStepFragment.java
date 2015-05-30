package android.support.v17.leanback.supportleanbackshowcase;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.widget.Toast;

import java.util.List;

/**
 * TODO: JavaDoc
 */
public class WizardExample4thStepFragment extends WizardExampleBaseStepFragment {

    private static final int ACTION_ID_WATCH = 1;
    private static final int ACTION_ID_LATER = ACTION_ID_WATCH + 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWizardActivity().setStep(4);
    }

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(mMovie.getTitle(),
                getString(R.string.wizard_example_rental_period),
                mMovie.getBreadcrump(), null);
        return guidance;
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        GuidedAction action = new GuidedAction.Builder()
                .id(ACTION_ID_WATCH)
                .title(getString(R.string.wizard_example_watch_now))
                .build();
        actions.add(action);
        action = new GuidedAction.Builder()
                .id(ACTION_ID_LATER)
                .title(getString(R.string.wizard_example_later))
                .build();
        actions.add(action);
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        if (ACTION_ID_WATCH == action.getId()) {
            Toast.makeText(getActivity(), getString(R.string.wizard_example_watch_now_clicked),
                    Toast.LENGTH_SHORT).show();
        }
        getActivity().finish();
    }
}
