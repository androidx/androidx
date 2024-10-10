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
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.GridItem;
import androidx.car.app.model.GridTemplate;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.MapWithContentTemplate;
import androidx.car.app.sample.showcase.common.R;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.NonNull;

/** Simple demo of how to present a map template with a list. */
public class MapWithGridTemplateDemoScreen extends Screen {

    public MapWithGridTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @ExperimentalCarApi
    @RequiresCarApi(7)
    @Override
    public @NonNull Template onGetTemplate() {
        ItemList.Builder gridItemListBuilder = new ItemList.Builder();
        for (int i = 0; i <= 7; i++) {
            gridItemListBuilder.addItem(createGridItem());
        }


        GridTemplate gridTemplate = new GridTemplate.Builder()
                .setSingleList(gridItemListBuilder.build())
                .setHeader(new Header.Builder()
                        .setStartHeaderAction(Action.BACK)
                        .setTitle("Report?")
                        .build())
                .build();


        ActionStrip actionStrip = new ActionStrip.Builder()
                .addAction(
                        new Action.Builder()
                                .setOnClickListener(
                                        () -> CarToast.makeText(
                                                        getCarContext(),
                                                        getCarContext().getString(
                                                                R.string.bug_reported_toast_msg),
                                                        CarToast.LENGTH_SHORT)
                                                .show())
                                .setIcon(
                                        new CarIcon.Builder(
                                                IconCompat.createWithResource(
                                                        getCarContext(),
                                                        R.drawable.ic_bug_report_24px))
                                                .build())
                                .setFlags(Action.FLAG_IS_PERSISTENT)
                                .build())
                .build();

        MapWithContentTemplate.Builder builder = new MapWithContentTemplate.Builder()
                .setContentTemplate(gridTemplate)
                .setActionStrip(actionStrip);

        return builder.build();
    }

    private GridItem createGridItem() {
        return new GridItem.Builder()
                .setImage(new CarIcon.Builder(IconCompat.createWithResource(getCarContext(),
                        R.drawable.ic_fastfood_white_48dp)).build())
                .setTitle("Primary")
                .setText("Secondary")
                .setOnClickListener(() -> CarToast.makeText(
                                getCarContext(),
                                "Clicked!",
                                CarToast.LENGTH_SHORT)
                        .show())
                .build();
    }
}
