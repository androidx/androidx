/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.screens.mapdemos.mapwithcontent;

import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.model.Toggle;
import androidx.car.app.navigation.model.MapWithContentTemplate;
import androidx.car.app.sample.showcase.common.R;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.NonNull;

/** Simple demo of how to present a map template with a list. */
@RequiresCarApi(6)
public class MapTemplateWithToggleDemoScreen extends Screen {

    private boolean mAvoidTolls;
    private boolean mAvoidHighways;
    private boolean mAvoidFerries;

    public MapTemplateWithToggleDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @Override
    public @NonNull Template onGetTemplate() {

        Toggle mToggleForTolls = new Toggle.Builder((checked) -> {
            mAvoidTolls = !mAvoidTolls;
            invalidate();
        }).setChecked(mAvoidTolls).build();

        Toggle mToggleForHighways = new Toggle.Builder((checked) -> {
            mAvoidHighways = !mAvoidHighways;
            invalidate();
        }).setChecked(mAvoidHighways).build();

        Toggle mToggleForFerries = new Toggle.Builder((checked) -> {
            mAvoidFerries = !mAvoidFerries;
            invalidate();
        }).setChecked(mAvoidFerries).build();

        ItemList.Builder listBuilder = new ItemList.Builder();

        listBuilder.addItem(buildRowForTemplate(R.string.avoid_tolls_row_title,
                mToggleForTolls, buildCarIcon(R.drawable.baseline_toll_24)));

        listBuilder.addItem(buildRowForTemplate(R.string.avoid_highways_row_title,
                mToggleForHighways, buildCarIcon(R.drawable.baseline_add_road_24)));

        listBuilder.addItem(buildRowForTemplate(R.string.avoid_ferries_row_title,
                mToggleForFerries, buildCarIcon(R.drawable.baseline_directions_boat_filled_24)));

        Header header = new Header.Builder()
                .setStartHeaderAction(Action.BACK)
                .addEndHeaderAction(new Action.Builder()
                        .setOnClickListener(() -> finish())
                        .setIcon(
                                new CarIcon.Builder(
                                        IconCompat.createWithResource(
                                                getCarContext(),
                                                R.drawable.ic_close_white_24dp))
                                        .build())
                        .build())
                .setTitle(getCarContext().getString(R.string.route_options_demo_title))
                .build();

        MapWithContentTemplate.Builder builder = new MapWithContentTemplate.Builder()
                .setContentTemplate(new ListTemplate.Builder()
                        .setSingleList(listBuilder.build())
                        .setHeader(header)
                        .build());
        return builder.build();
    }

    private CarIcon buildCarIcon(int icon) {
        return new CarIcon.Builder(IconCompat.createWithResource(getCarContext(), icon))
                .build();
    }

    private Row buildRowForTemplate(int title, Toggle toggle, CarIcon icon) {
        return new Row.Builder()
            .setTitle(getCarContext().getString(title))
            .setImage(icon)
            .setToggle(toggle)
            .build();
    }
}
