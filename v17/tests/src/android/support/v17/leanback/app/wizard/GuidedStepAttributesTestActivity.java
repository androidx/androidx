package android.support.v17.leanback.app.wizard;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.app.GuidedStepFragment;

public class GuidedStepAttributesTestActivity extends Activity {

    private GuidedStepAttributesTestFragment mGuidedStepAttributesTestFragment;

    public static String EXTRA_GUIDANCE = "guidance";
    public static String EXTRA_ACTION_LIST = "actionList";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        mGuidedStepAttributesTestFragment = new GuidedStepAttributesTestFragment();
        GuidedStepFragment.addAsRoot(this, mGuidedStepAttributesTestFragment, android.R.id.content);
    }
    public Fragment getGuidedStepTestFragment() {
        return getFragmentManager().findFragmentById(android.R.id.content);
    }
}
