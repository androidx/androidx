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

package androidx.car.app.sample.showcase.common.templates;

import static androidx.car.app.CarToast.LENGTH_LONG;
import static androidx.car.app.model.Action.BACK;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.GridItem;
import androidx.car.app.model.GridTemplate;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

/** Creates a screen that demonstrates usage of the full screen {@link GridTemplate}. */
public final class GridTemplateDemoScreen extends Screen implements DefaultLifecycleObserver {
    private static final int LOADING_TIME_MILLIS = 2000;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Nullable
    private IconCompat mImage;
    @Nullable
    private IconCompat mIcon;
    private boolean mIsFourthItemLoading;
    private boolean mThirdItemToggleState;
    private boolean mFourthItemToggleState;
    private boolean mFifthItemToggleState;

    public GridTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        getLifecycle().addObserver(this);
        mIsFourthItemLoading = false;
        mThirdItemToggleState = false;
        mFourthItemToggleState = true;
        mFifthItemToggleState = false;
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        Resources resources = getCarContext().getResources();
        Bitmap bitmap = BitmapFactory.decodeResource(resources, R.drawable.test_image_square);
        mImage = IconCompat.createWithBitmap(bitmap);
        mIcon = IconCompat.createWithResource(getCarContext(), R.drawable.ic_fastfood_white_48dp);
    }

    @Override
    @SuppressWarnings({"FutureReturnValueIgnored"})
    public void onStart(@NonNull LifecycleOwner owner) {
        mIsFourthItemLoading = false;

        // Post a message that starts loading the fourth item for some time.
        triggerFourthItemLoading();
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder gridItemListBuilder = new ItemList.Builder();

        // Grid item with an icon and a title.
        gridItemListBuilder.addItem(
                new GridItem.Builder()
                        .setImage(new CarIcon.Builder(mIcon).build(), GridItem.IMAGE_TYPE_ICON)
                        .setTitle("Non-actionable")
                        .build());

        // Grid item with an icon, a title, onClickListener and no text.
        gridItemListBuilder.addItem(
                new GridItem.Builder()
                        .setImage(new CarIcon.Builder(mIcon).build(), GridItem.IMAGE_TYPE_ICON)
                        .setTitle("Second Item")
                        .setOnClickListener(
                                () ->
                                        CarToast.makeText(
                                                getCarContext(),
                                                "Clicked second item",
                                                LENGTH_LONG)
                                                .show())
                        .build());

        // Grid item with an icon marked as icon, a title, a text and a toggle in unchecked state.
        gridItemListBuilder.addItem(
                new GridItem.Builder()
                        .setImage(new CarIcon.Builder(mIcon).build(), GridItem.IMAGE_TYPE_ICON)
                        .setTitle("Third Item")
                        .setText(mThirdItemToggleState ? "Checked" : "Unchecked")
                        .setOnClickListener(
                                () -> {
                                    mThirdItemToggleState = !mThirdItemToggleState;
                                    CarToast.makeText(
                                            getCarContext(),
                                            "Third item checked: " + mThirdItemToggleState,
                                            LENGTH_LONG)
                                            .show();
                                    invalidate();
                                })
                        .build());

        // Grid item with an image, a title, a long text and a toggle that takes some time to
        // update.
        if (mIsFourthItemLoading) {
            gridItemListBuilder.addItem(
                    new GridItem.Builder()
                            .setTitle("Fourth")
                            .setText(mFourthItemToggleState ? "On" : "Off")
                            .setLoading(true)
                            .build());
        } else {
            gridItemListBuilder.addItem(
                    new GridItem.Builder()
                            .setImage(new CarIcon.Builder(mImage).build())
                            .setTitle("Fourth")
                            .setText(mFourthItemToggleState ? "On" : "Off")
                            .setOnClickListener(
                                    this::triggerFourthItemLoading)
                            .build());
        }

        // Grid item with a large image, a long title, no text and a toggle in unchecked state.
        gridItemListBuilder.addItem(
                new GridItem.Builder()
                        .setImage(new CarIcon.Builder(mImage).build(), GridItem.IMAGE_TYPE_LARGE)
                        .setTitle("Fifth Item has a long title set")
                        .setOnClickListener(
                                () -> {
                                    mFifthItemToggleState = !mFifthItemToggleState;
                                    CarToast.makeText(
                                            getCarContext(),
                                            "Fifth item checked: " + mFifthItemToggleState,
                                            LENGTH_LONG)
                                            .show();
                                    invalidate();
                                })
                        .build());

        // Grid item with an image marked as an icon, a long title, a long text and onClickListener.
        gridItemListBuilder.addItem(
                new GridItem.Builder()
                        .setImage(new CarIcon.Builder(mIcon).build(), GridItem.IMAGE_TYPE_ICON)
                        .setTitle("Sixth Item has a long title set")
                        .setText("Sixth Item has a long text set")
                        .setOnClickListener(
                                () ->
                                        CarToast.makeText(
                                                getCarContext(),
                                                "Clicked sixth item",
                                                LENGTH_LONG)
                                                .show())
                        .build());

        // Some hosts may allow more items in the grid than others, so create more.
        if (getCarContext().getCarAppApiLevel() > CarAppApiLevels.LEVEL_1) {
            int itemLimit =
                    7 + getCarContext().getCarService(ConstraintManager.class).getContentLimit(
                            ConstraintManager.CONTENT_LIMIT_TYPE_GRID);

            for (int i = 7; i <= itemLimit; i++) {
                String titleText = "Item: " + i;
                String toastText = "Clicked item " + i;

                gridItemListBuilder.addItem(
                        new GridItem.Builder()
                                .setImage(new CarIcon.Builder(mIcon).build(),
                                        GridItem.IMAGE_TYPE_ICON)
                                .setTitle(titleText)
                                .setOnClickListener(
                                        () ->
                                                CarToast.makeText(
                                                        getCarContext(),
                                                        toastText,
                                                        LENGTH_LONG)
                                                        .show())
                                .build());
            }
        }

        return new GridTemplate.Builder()
                .setHeaderAction(Action.APP_ICON)
                .setSingleList(gridItemListBuilder.build())
                .setTitle("Grid Template Demo")
                .setActionStrip(
                        new ActionStrip.Builder()
                                .addAction(
                                        new Action.Builder()
                                                .setTitle("Settings")
                                                .setOnClickListener(
                                                        () ->
                                                                CarToast.makeText(
                                                                        getCarContext(),
                                                                        "Clicked Settings",
                                                                        LENGTH_LONG)
                                                                        .show())
                                                .build())
                                .build())
                .setHeaderAction(BACK)
                .build();
    }

    /**
     * Changes the fourth item to a loading state for some time and changes it back to the loaded
     * state.
     */
    private void triggerFourthItemLoading() {
        mHandler.post(
                () -> {
                    mIsFourthItemLoading = true;
                    invalidate();

                    mHandler.postDelayed(
                            () -> {
                                mIsFourthItemLoading = false;
                                mFourthItemToggleState = !mFourthItemToggleState;
                                invalidate();
                            },
                            LOADING_TIME_MILLIS);
                });
    }
}
