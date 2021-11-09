/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.mediarouter.media;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.test.core.app.ActivityScenario;

public class MediaRouter2TestActivity extends Activity {
    private static ActivityScenario<MediaRouter2TestActivity> sActivityScenario;

    public static ActivityScenario<MediaRouter2TestActivity> startActivity(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(context, MediaRouter2TestActivity.class);
        sActivityScenario = ActivityScenario.launch(intent);
        return sActivityScenario;
    }

    public static void finishActivity() {
        if (sActivityScenario != null) {
            sActivityScenario.close();
            sActivityScenario = null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTurnScreenOn(true);
        setShowWhenLocked(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
