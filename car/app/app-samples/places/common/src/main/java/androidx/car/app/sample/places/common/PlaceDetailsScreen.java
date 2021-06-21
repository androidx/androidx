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

package androidx.car.app.sample.places.common;

import static androidx.car.app.CarToast.LENGTH_LONG;
import static androidx.car.app.sample.places.common.Executors.BACKGROUND_EXECUTOR;
import static androidx.car.app.sample.places.common.Executors.UI_EXECUTOR;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.HostException;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ForegroundCarColorSpan;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.places.common.places.PlaceDetails;
import androidx.car.app.sample.places.common.places.PlaceFinder;
import androidx.car.app.sample.places.common.places.PlaceInfo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A screen that displays a the details for a given place. */
public class PlaceDetailsScreen extends Screen implements DefaultLifecycleObserver {
    private static final String FULL_STAR = "\u2605";
    private static final String HALF_STAR = "\u00BD";

    private final PlaceInfo mPlace;

    // Loaded asynchronously from the network.
    @Nullable
    private Bitmap mPhoto;

    private Geocoder mGeocoder;
    @Nullable
    private PlaceDetails mDetails;
    @NonNull
    private PlaceFinder mPlaceFinder;

    /** Returns a screen showing the details of the given {@link PlaceInfo}. */
    static PlaceDetailsScreen create(@NonNull CarContext carContext, @NonNull PlaceInfo place) {
        return new PlaceDetailsScreen(carContext, place);
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        mGeocoder = new Geocoder(getCarContext());
        mPlaceFinder =
                new PlaceFinder(getCarContext().getResources().getString(R.string.PLACES_API_KEY));
    }

    @Override
    @SuppressWarnings({"FutureReturnValueIgnored"})
    public void onStart(@NonNull LifecycleOwner owner) {
        update(getCarContext());
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        Pane.Builder paneBuilder = new Pane.Builder();

        // If we don't have any places yet, show a loading progress indicator.
        if (mDetails == null) {
            paneBuilder.setLoading(true);
        } else {
            Row.Builder row1Builder = new Row.Builder().setTitle("Address");

            // Add the address, split in multiple lines.
            List<CharSequence> addressLines = getAddressLines(mPlace.getAddress(mGeocoder));
            for (CharSequence line : addressLines) {
                row1Builder.addText(line);
            }

            if (mPhoto != null) {
                row1Builder.setImage(
                        new CarIcon.Builder(IconCompat.createWithBitmap(mPhoto)).build(),
                        Row.IMAGE_TYPE_LARGE);
            }

            paneBuilder.addRow(row1Builder.build());

            boolean hasSecondRow = false;
            Row.Builder row2Builder = new Row.Builder().setTitle("Phone Number and Rating");

            // Add the phone number.
            String phoneNumber = mDetails.getPhoneNumber();
            if (phoneNumber != null) {
                hasSecondRow = true;
                row2Builder.addText(phoneNumber);
            }

            // Add the place's ratings.
            double ratings = mDetails.getRatings();
            if (ratings >= 0) {
                hasSecondRow = true;
                row2Builder.addText(getRatingsString(ratings));
            }

            if (hasSecondRow) {
                paneBuilder.addRow(row2Builder.build());
            }

            // Add a button with a navigate action.
            paneBuilder.addAction(
                    new Action.Builder()
                            .setTitle("Navigate")
                            .setOnClickListener(this::onClickNavigate)
                            .build());
        }

        return new PaneTemplate.Builder(paneBuilder.build())
                .setTitle(mPlace.getName())
                .setHeaderAction(Action.BACK)
                .build();
    }

    private void onClickNavigate() {
        Uri uri = Uri.parse("geo:0,0?q=" + mPlace.getAddress(mGeocoder).getAddressLine(0));
        Intent intent = new Intent(CarContext.ACTION_NAVIGATE, uri);

        try {
            getCarContext().startCarApp(intent);
        } catch (HostException e) {
            CarToast.makeText(
                    getCarContext(),
                    "Failure starting navigation",
                    LENGTH_LONG)
                    .show();
        }
    }

    private static List<CharSequence> getAddressLines(Address address) {
        List<CharSequence> list = new ArrayList<>();

        // First line: [street number address].
        String separator = " ";
        String s = append(address.getSubThoroughfare(), address.getThoroughfare(), separator);
        if (s != null) {
            list.add(s);
        }

        // Second line: [city, state, postal code].
        separator = ", ";
        s =
                append(
                        append(address.getLocality(), address.getAdminArea(), separator),
                        address.getPostalCode(),
                        separator);
        if (s != null) {
            list.add(s);
        }

        return list;
    }

    @Nullable
    private static String append(@Nullable String a, @Nullable String b, String separator) {
        return a == null ? b : b == null ? a : a + (a.isEmpty() ? b : separator + b);
    }

    private static CharSequence getRatingsString(Double ratings) {
        String s;
        double r;
        for (s = "", r = ratings; r > 0; --r) {
            s += r < 1 ? HALF_STAR : FULL_STAR;
        }
        SpannableString ss =
                new SpannableString(s + " ratings: " + String.format(Locale.US, "%.2f", ratings));
        if (!s.isEmpty()) {
            colorize(ss, CarColor.YELLOW, 0, s.length());
        }
        return ss;
    }

    static void colorize(SpannableString s, CarColor color, int index, int length) {
        s.setSpan(
                ForegroundCarColorSpan.create(color),
                index,
                index + length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private ListenableFuture<Void> update(Context context) {
        // Load details first, then the icon, then the photo.
        return Futures.transformAsync(
                loadDetails(),
                details1 ->
                        Futures.transformAsync(
                                loadPhoto(context, details1),
                                details2 -> {
                                    invalidate();
                                    return Futures.immediateFuture(null);
                                },
                                UI_EXECUTOR),
                UI_EXECUTOR);
    }

    private ListenableFuture<Void> loadPhoto(Context context, @Nullable PlaceDetails details) {
        if (details == null) {
            return Futures.immediateFuture(null);
        }

        List<String> photos = details.getPhotoUrls();
        if (photos.isEmpty()) {
            return Futures.immediateFuture(null);
        }
        return Futures.transformAsync(
                ImageUtil.loadBitmapFromUrl(context, photos.get(0)),
                bitmap -> {
                    mPhoto = bitmap;
                    invalidate();
                    return Futures.immediateFuture(null);
                },
                UI_EXECUTOR);
    }

    @SuppressWarnings({"FutureReturnValueIgnored"})
    private ListenableFuture<PlaceDetails> loadDetails() {
        return Futures.transformAsync(
                // Run the query in the background thread, and update with the results in the UI
                // thread.
                Futures.submitAsync(
                        () -> Futures.immediateFuture(mPlaceFinder.getPlaceDetails(mPlace.getId())),
                        BACKGROUND_EXECUTOR),
                place -> {
                    mDetails = place;
                    return Futures.immediateFuture(place);
                },
                UI_EXECUTOR);
    }

    private PlaceDetailsScreen(@NonNull CarContext carContext, @NonNull PlaceInfo place) {
        super(carContext);
        mPlace = place;

        getLifecycle().addObserver(this);
    }
}
