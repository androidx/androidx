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

package androidx.car.app.sample.showcase.common.screens.templatelayouts.listtemplates;

import static androidx.car.app.CarToast.LENGTH_LONG;

import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.NonNull;

/** A screen demonstrating lists with secondary actions and numeric decorations. */
@RequiresCarApi(6)
public class SecondaryActionsAndDecorationDemoScreen extends Screen {
    public SecondaryActionsAndDecorationDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @Override
    public @NonNull Template onGetTemplate() {
        Action action = new Action.Builder()
                .setIcon(buildCarIconWithResources(R.drawable.baseline_question_mark_24))
                .setOnClickListener(() -> CarToast.makeText(getCarContext(),
                        R.string.secondary_action_toast, LENGTH_LONG).show())
                .build();

        ItemList.Builder listBuilder = new ItemList.Builder();
        listBuilder.addItem(buildRowForTemplate(R.string.decoration_test_title, 3));

        listBuilder.addItem(buildRowForTemplate(R.string.secondary_actions_test_title, action));

        listBuilder.addItem(buildRowForTemplate(R.string.secondary_actions_decoration_test_title,
                12,
                action));

        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setHeader(new Header.Builder()
                        .setTitle(getCarContext()
                                .getString(R.string.secondary_actions_decoration_button_demo_title))
                        .setStartHeaderAction(Action.BACK)
                        .addEndHeaderAction(new Action.Builder()
                                .setTitle(getCarContext().getString(
                                        R.string.home_caps_action_title))
                                .setOnClickListener(
                                        () -> getScreenManager().popToRoot())
                                .build())
                        .build())
                .build();
    }

    private CarIcon buildCarIconWithResources(int imageId) {
        return new CarIcon.Builder(
                IconCompat.createWithResource(
                                getCarContext(),
                                imageId))
                .build();
    }

    private Row buildRowForTemplate(int title, int numericDecoration) {
        return new Row.Builder()
                .setTitle(getCarContext().getString(title))
                .setNumericDecoration(numericDecoration)
                .build();
    }

    private Row buildRowForTemplate(int title, Action action) {
        return new Row.Builder()
                .setTitle(getCarContext().getString(title))
                .addAction(action)
                .addText(getCarContext().getString(R.string.secondary_actions_test_subtitle))
                .build();
    }

    private Row buildRowForTemplate(int title, int numericDecoration, Action action) {
        return new Row.Builder()
                .setTitle(getCarContext().getString(title))
                .setNumericDecoration(numericDecoration)
                .setOnClickListener(() -> CarToast.makeText(getCarContext(),
                        R.string.row_primary_action_toast, LENGTH_LONG).show())
                .addText(getCarContext()
                        .getString(R.string.secondary_actions_decoration_test_subtitle))
                .addAction(action)
                .build();
    }
}
