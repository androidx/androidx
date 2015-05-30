package android.support.v17.leanback.supportleanbackshowcase;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.widget.GuidanceStylist;
import android.support.v17.leanback.widget.GuidedAction;
import android.support.v17.leanback.widget.GuidedActionsStylist;
import android.util.Log;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * TODO: JavaDoc
 */
public class WizardExample3rdStepFragment extends WizardExampleBaseStepFragment {

    private static final int ACTION_ID_PROCESSING = 1;
    private final Handler mFakeHttpHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWizardActivity().setStep(3);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Fake Http call by creating some sort of delay.
        mFakeHttpHandler.postDelayed(fakeHttpRequestRunnable, 4000L);
    }

    @Override
    public GuidedActionsStylist onCreateActionsStylist() {
        GuidedActionsStylist stylist = new GuidedActionsStylist() {
            @Override
            public int onProvideItemLayoutId() {
                return R.layout.wizard_progress_action_item;
            }

            @Override
            public int onProvideLayoutId() {
                return R.layout.wizard_progress_action_container;
            }
        };
        return stylist;
    }

    @Override
    public void onStop() {
        super.onStop();

        // Make sure to cancel the execution of the Runnable in case the fragment is stopped.
        mFakeHttpHandler.removeCallbacks(fakeHttpRequestRunnable);
    }

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(mMovie.getTitle(),
                "Just a second...",
                mMovie.getBreadcrump(), null);
        return guidance;
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        GuidedAction action = new GuidedAction.Builder()
                .id(ACTION_ID_PROCESSING)
                .title(getString(R.string.wizard_example_processing))
                .infoOnly(true)
                .build();
        actions.add(action);
    }

    private final Runnable fakeHttpRequestRunnable = new Runnable() {
        @Override
        public void run() {
            GuidedStepFragment fragment = new WizardExample4thStepFragment();
            fragment.setArguments(getArguments());
            add(getFragmentManager(), fragment);
        }
    };

}
