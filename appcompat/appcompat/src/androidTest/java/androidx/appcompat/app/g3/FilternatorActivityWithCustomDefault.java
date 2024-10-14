/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appcompat.app.g3;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.concurrent.CountDownLatch;

public class FilternatorActivityWithCustomDefault extends AppCompatActivity {
    public static CountDownLatch configurationLatch = new CountDownLatch(1);
    public static Exception configurationException = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Configuration initialConfig =
                new Configuration(getApplication().getResources().getConfiguration());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        super.onCreate(savedInstanceState);

        if (!initialConfig.equals(getApplication().getResources().getConfiguration())) {
            throw new IllegalStateException("Base configuration got messed up");
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Configuration actualConfig = getResources().getConfiguration();
        if (actualConfig.equals(newConfig)) {
            int diff = actualConfig.diff(newConfig);
            configurationException = new RuntimeException("Configuration changes not correctly "
                    + "reflected in getResources().getConfiguration(), diff is " + diff + ", "
                    + "actual config is " + actualConfig + ", new config is " + newConfig);
        }

        if (configurationLatch.getCount() > 0) {
            configurationLatch.countDown();
        }
    }
}
