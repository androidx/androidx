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

package androidx.car.app.sample.showcase.common.screens;

import static androidx.car.app.CarToast.LENGTH_LONG;
import static androidx.car.app.model.Action.BACK;

import android.content.Context;

import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.model.Toggle;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.ShowcaseService;
import androidx.car.app.sample.showcase.common.ShowcaseSession;
import androidx.car.app.sample.showcase.common.screens.settings.CarHardwareDemoScreen;
import androidx.car.app.sample.showcase.common.screens.settings.ContentLimitsDemoScreen;
import androidx.car.app.sample.showcase.common.screens.settings.LatestFeatures;
import androidx.car.app.sample.showcase.common.screens.settings.ParkedVsDrivingDemoScreen;

import org.jspecify.annotations.NonNull;

/** A screen demonstrating selectable lists. */
public final class SettingsScreen extends Screen {

    private boolean mLoadingToggleState;

    private final @NonNull ShowcaseSession mShowcaseSession;

    public SettingsScreen(@NonNull CarContext carContext,
            @NonNull ShowcaseSession showcaseSession) {
        super(carContext);
        mShowcaseSession = showcaseSession;
    }

    @Override
    public @NonNull Template onGetTemplate() {
        Toggle mLoadingToggle = new Toggle.Builder((checked) -> {
            if (checked) {
                makeCarToast(R.string.loading_toggle_enabled);
                setLoadingKeyValue(true);
            } else {
                makeCarToast(R.string.loading_toggle_disabled);
                setLoadingKeyValue(false);
            }
            mLoadingToggleState = !mLoadingToggleState;
        }).setChecked(mLoadingToggleState).build();

        ItemList.Builder listBuilder = new ItemList.Builder();

        listBuilder.addItem(buildRowForTemplate(new LatestFeatures(getCarContext()),
                R.string.latest_feature_title));

        listBuilder.addItem(buildRowForTemplate(R.string.loading_demo_title, mLoadingToggle));

        listBuilder.addItem(buildRowForTemplate(new ContentLimitsDemoScreen(getCarContext()),
                R.string.content_limits_demo_title));

        listBuilder.addItem(buildRowForTemplate(new ParkedVsDrivingDemoScreen(getCarContext()),
                R.string.parking_vs_driving_demo_title));

        listBuilder.addItem(buildRowForTemplate(new CarHardwareDemoScreen(getCarContext(),
                mShowcaseSession), R.string.car_hardware_demo_title));

        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setHeader(new Header.Builder()
                        .setTitle(getCarContext().getString(R.string.settings_action_title) + " ("
                                + getCarContext().getString(R.string.cal_api_level_prefix,
                                getCarContext().getCarAppApiLevel()) + ")")
                        .setStartHeaderAction(BACK)
                        .build())
                .build();
    }

    private Row buildRowForTemplate(Screen screen, int title) {
        return new Row.Builder()
                .setTitle(getCarContext().getString(title))
                .setOnClickListener(() -> getScreenManager().push(screen))
                .setBrowsable(true)
                .build();
    }

    private Row buildRowForTemplate(int title, Toggle toggle) {
        return new Row.Builder()
                .setTitle(getCarContext().getString(title))
                .setToggle(toggle)
                .build();
    }

    private void makeCarToast(int toastText) {
        CarToast.makeText(getCarContext(), toastText,
                LENGTH_LONG).show();
    }

    private void setLoadingKeyValue(boolean val) {
        getCarContext()
                .getSharedPreferences(
                        ShowcaseService.SHARED_PREF_KEY,
                        Context.MODE_PRIVATE)
                .edit()
                .putBoolean(
                        ShowcaseService.LOADING_KEY, val)
                .apply();
    }
}
