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

package androidx.car.app.sample.showcase.common.misc;

import static androidx.car.app.model.Action.BACK;
import static androidx.car.app.model.CarColor.BLUE;
import static androidx.car.app.model.CarColor.GREEN;
import static androidx.car.app.model.CarColor.PRIMARY;
import static androidx.car.app.model.CarColor.RED;
import static androidx.car.app.model.CarColor.SECONDARY;
import static androidx.car.app.model.CarColor.YELLOW;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.common.Utils;
import androidx.core.graphics.drawable.IconCompat;

/** Creates a screen that demonstrate the usage of colored texts and icons in the library. */
public final class ColorDemoScreen extends Screen {
    public ColorDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle(getCarContext().getString(R.string.example_title, 1))
                        .addText(Utils.colorize(getCarContext().getString(R.string.example_1_text),
                                RED, 16, 3))
                        .setImage(new CarIcon.Builder(
                                IconCompat.createWithResource(
                                        getCarContext(),
                                        R.drawable.ic_fastfood_white_48dp))
                                .setTint(RED)
                                .build())
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle(getCarContext().getString(R.string.example_title, 2))
                        .addText(Utils.colorize(getCarContext().getString(R.string.example_2_text),
                                GREEN, 16, 5))
                        .setImage(new CarIcon.Builder(
                                IconCompat.createWithResource(
                                        getCarContext(),
                                        R.drawable.ic_fastfood_white_48dp))
                                .setTint(GREEN)
                                .build())
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle(getCarContext().getString(R.string.example_title, 3))
                        .addText(Utils.colorize(getCarContext().getString(R.string.example_3_text),
                                BLUE, 16, 4))
                        .setImage(new CarIcon.Builder(
                                IconCompat.createWithResource(
                                        getCarContext(),
                                        R.drawable.ic_fastfood_white_48dp))
                                .setTint(BLUE)
                                .build())
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle(getCarContext().getString(R.string.example_title, 4))
                        .addText(Utils.colorize(getCarContext().getString(R.string.example_4_text),
                                YELLOW, 16, 6))
                        .setImage(new CarIcon.Builder(
                                IconCompat.createWithResource(
                                        getCarContext(),
                                        R.drawable.ic_fastfood_white_48dp))
                                .setTint(YELLOW)
                                .build())
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle(getCarContext().getString(R.string.example_title, 5))
                        .addText(Utils.colorize(getCarContext().getString(R.string.example_5_text),
                                PRIMARY, 19, 7))
                        .setImage(new CarIcon.Builder(
                                IconCompat.createWithResource(
                                        getCarContext(),
                                        R.drawable.ic_fastfood_white_48dp))
                                .setTint(PRIMARY)
                                .build())
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setTitle(getCarContext().getString(R.string.example_title, 6))
                        .addText(
                                Utils.colorize(
                                        getCarContext().getString(R.string.example_6_text),
                                        SECONDARY, 19, 9))
                        .setImage(new CarIcon.Builder(
                                IconCompat.createWithResource(
                                        getCarContext(),
                                        R.drawable.ic_fastfood_white_48dp))
                                .setTint(SECONDARY)
                                .build())
                        .build());

        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle(getCarContext().getString(R.string.color_demo))
                .setHeaderAction(BACK)
                .build();
    }
}
