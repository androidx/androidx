/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.support.v17.leanback.app;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.test.R;
import android.view.View;

/**
 * Activity containing {@link DetailsFragmentTest} used for testing.
 */
public class DetailsFragmentTestActivity extends Activity {
    private DetailsTestFragment mFragment;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        setContentView(R.layout.details);
        mFragment = new DetailsTestFragment();

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent.getExtras() != null) {
                Bundle arguments = new Bundle();
                arguments.putAll(intent.getExtras());
                mFragment.setArguments(arguments);
            }
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_root, mFragment);
            ft.commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mFragment.setItem(new PhotoItem("Hello world", "Fake content goes here",
                R.drawable.spiderman));
    }

    public DetailsTestFragment getDetailsFragment() {
        return mFragment;
    }
}
