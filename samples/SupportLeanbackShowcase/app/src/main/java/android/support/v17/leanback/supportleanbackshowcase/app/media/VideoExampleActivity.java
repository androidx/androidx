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

package android.support.v17.leanback.supportleanbackshowcase.app.media;

import android.app.Activity;
import android.os.Bundle;
import android.support.v17.leanback.supportleanbackshowcase.R;

/**
 * TODO: Javadoc
 */
public class VideoExampleActivity extends Activity {

    public static final String VIDEO_SURFACE_FRAGMENT_TAG = "VIDEO_SURFACE";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_example);


        getFragmentManager().beginTransaction()
                .replace(R.id.videoFragment, new VideoSurfaceFragment(), VIDEO_SURFACE_FRAGMENT_TAG)
                .add(R.id.videoFragment, new VideoConsumptionExampleFragment())
                .commit();
    }
}
