/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.sharetarget.testapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.core.app.Person;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Start activity for the Direct Share Sample application. This class manages the UI which allows a
 * user to push or remove direct share targets.
 */
public class MainActivity extends Activity {

    private static final String CATEGORY_TEXT_SHARE_TARGET =
            "androidx.sharetarget.testapp.category.TEXT_SHARE_TARGET";
    private static final String CATEGORY_OTHER_TEXT_SHARE_TARGET =
            "androidx.sharetarget.testapp.category.OTHER_TEXT_SHARE_TARGET";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.push_targets).setOnClickListener(mOnClickListener);
        findViewById(R.id.remove_targets).setOnClickListener(mOnClickListener);
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.push_targets:
                    pushDirectShareTargets();
                    break;
                case R.id.remove_targets:
                    removeAllDirectShareTargets();
                    break;
            }
        }
    };

    private void pushDirectShareTargets() {
        Intent intent = new Intent(Intent.ACTION_DEFAULT);

        Set<String> categories1 = new HashSet<>();
        categories1.add(CATEGORY_TEXT_SHARE_TARGET);

        Set<String> categories2 = new HashSet<>();
        categories2.add(CATEGORY_OTHER_TEXT_SHARE_TARGET);

        ArrayList<ShortcutInfoCompat> shortcuts = new ArrayList<>();

        shortcuts.add(new ShortcutInfoCompat.Builder(this, "Person_One_ID")
                .setShortLabel("Person_One")
                .setIcon(IconCompat.createWithResource(this, R.mipmap.logo_avatar))
                .setIntent(intent)
                .setLongLived()
                .setPerson(new Person.Builder().build())
                .setCategories(categories1)
                .build());
        shortcuts.add(new ShortcutInfoCompat.Builder(this, "Person_Two_ID")
                .setShortLabel("Person_Two")
                .setIcon(IconCompat.createWithResource(this, R.mipmap.logo_avatar))
                .setIntent(intent)
                .setLongLived()
                .setPerson(new Person.Builder().build())
                .setCategories(categories2)
                .build());

        ShortcutManagerCompat.addDynamicShortcuts(this, shortcuts);
    }

    private void removeAllDirectShareTargets() {
        ShortcutManagerCompat.removeAllDynamicShortcuts(this);
    }
}
