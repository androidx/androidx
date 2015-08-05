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

package android.support.v17.leanback.supportleanbackshowcase.app.wizard;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v17.leanback.app.GuidedStepFragment;
import android.support.v17.leanback.supportleanbackshowcase.R;

/**
 * An Activity displaying a wizard for renting a movie.
 */
public class WizardExampleActivity extends Activity {

    // When the user 'bought' the product and presses back, we don't want to show the 'Processing..'
    // screen again, instead we want to go back to the very first step or close the wizard. Thus, we
    // have to save the current step of the wizard and make it accessible to it's children.
    private int mStep = 0;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(R.drawable.wizard_background_blackned);

        // Recover old step state.
        if (savedInstanceState != null) {
            mStep = savedInstanceState.getInt("step");
        }

        GuidedStepFragment fragment = new WizardExample1stStepFragment();
        fragment.setArguments(getIntent().getExtras()); // Delegate Movie to first step.
        GuidedStepFragment.add(getFragmentManager(), fragment);
    }

    public int getStep() {
        return mStep;
    }

    public void setStep(int step) {
        mStep = step;
    }

    @Override
    public void onBackPressed() {
        if (4 == getStep()) {
            // The user 'bought' the product. When he presses 'Back' the Wizard will be closed and
            // he will not be send back to 'Processing Payment...'-Screen.
            finish();
        } else super.onBackPressed();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        // Save current step persitently.
        outPersistentState.putInt("step", mStep);
        super.onSaveInstanceState(outState, outPersistentState);
    }
}
