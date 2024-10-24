/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.car.app.sample.showcase.common;

import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.screens.MapDemosScreen;
import androidx.car.app.sample.showcase.common.screens.NavigationDemosScreen;
import androidx.car.app.sample.showcase.common.screens.SettingsScreen;
import androidx.car.app.sample.showcase.common.screens.TemplateLayoutsDemoScreen;
import androidx.car.app.sample.showcase.common.screens.UserInteractionsDemoScreen;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.NonNull;

/** The starting screen of the app. */
public final class StartScreen extends Screen {
    private final @NonNull ShowcaseSession mShowcaseSession;

    public StartScreen(@NonNull CarContext carContext, @NonNull ShowcaseSession showcaseSession) {
        super(carContext);
        mShowcaseSession = showcaseSession;
    }

    @Override
    public @NonNull Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        listBuilder.addItem(createRowForScreen(R.string.template_layouts_demo_title,
                new TemplateLayoutsDemoScreen(getCarContext())));

        listBuilder.addItem(createRowForScreen(R.string.user_interactions_demo_title,
                new UserInteractionsDemoScreen(1, getCarContext())));

        listBuilder.addItem(createRowForScreen(R.string.map_demos_title,
                createCarIconForImage(R.drawable.ic_place_white_24dp),
                new MapDemosScreen(getCarContext())));

        listBuilder.addItem(createRowForScreen(R.string.nav_demos_title,
                createCarIconForImage(R.drawable.ic_map_white_48dp),
                NavigationDemosScreen.createScreen(getCarContext())));

        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setHeader(new Header.Builder()
                        .setTitle(getCarContext().getString(R.string.showcase_demos_title))
                        .setStartHeaderAction(Action.APP_ICON)
                        .addEndHeaderAction(createSettingsActionButton())
                        .build())
                .build();
    }

    /**
     * Creates a new Settings Action button in the top right of the Home page
     */
    public @NonNull Action createSettingsActionButton() {
        return new Action.Builder()
                .setTitle(getCarContext().getString(R.string.settings_action_title))
                .setOnClickListener(() -> getScreenManager().push(
                        new SettingsScreen(getCarContext(), mShowcaseSession)))
                .build();
    }

    /**
     * Creates new row given a title, and the next screen on clicking the row
     */
    public @NonNull Row createRowForScreen(int titleId, @NonNull Screen screen) {
        return new Row.Builder()
            .setTitle(getCarContext().getString(titleId))
            .setOnClickListener(() -> getScreenManager().push(screen))
            .setBrowsable(true)
            .build();
    }

    /**
     * Creates new row given a title, CarIcon image and the next screen on clicking the row
     */
    public @NonNull Row createRowForScreen(int titleId, @NonNull CarIcon image,
            @NonNull Screen screen) {
        return new Row.Builder()
                .setImage(image, Row.IMAGE_TYPE_ICON)
                .setTitle(getCarContext().getString(titleId))
                .setOnClickListener(() -> getScreenManager().push(screen))
                .setBrowsable(true)
                .build();
    }

    /**
    * Given an imageId (as a drawable resource), this function outputs an CarIcon
    */
    public @NonNull CarIcon createCarIconForImage(int imageId) {
        return new CarIcon.Builder(
                IconCompat.createWithResource(
                        getCarContext(),
                        imageId))
                .build();
    }
}

