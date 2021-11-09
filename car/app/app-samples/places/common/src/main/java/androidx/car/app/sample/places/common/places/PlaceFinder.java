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

package androidx.car.app.sample.places.common.places;

import android.location.Location;
import android.util.Log;
import android.util.Xml.Encoding;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/** Implements methods to access the Places API. */
public class PlaceFinder {
    private static final String TAG = "PlacesDemo";
    private static final String PLACES_BASE_URL = "https://maps.googleapis.com/maps/api/place";
    private static final String SEARCH_URL = PLACES_BASE_URL + "/nearbysearch/json?";
    private static final String DETAILS_URL = PLACES_BASE_URL + "/details/json?";
    private static final String PHOTO_URL = PLACES_BASE_URL + "/photo?";

    @NonNull
    private final String mApiKey;

    public PlaceFinder(@NonNull String apiKey) {
        this.mApiKey = apiKey;
    }

    /** Queries the details for a place give its id. */
    @Nullable
    public PlaceDetails getPlaceDetails(@NonNull String placeId) {
        try {
            String jsonResult = getResult(makeDetailsURL(placeId));
            JSONObject root = throwIfError(new JSONObject(jsonResult));
            JSONObject result = root.getJSONObject("result");

            return new PlaceDetails(
                    placeFromJson(result),
                    safeGetString(result, "formatted_phone_number"),
                    safeGetDouble(result, "rating"),
                    safePhotosFromJson(result),
                    safeGetString(result, "icon"));
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error getting place details.", e);
            return null;
        }
    }

    /**
     * Queries the map server and obtains a list of places within the radius of the given location,
     * for the given category.
     *
     * @param location the location to search around of
     * @param radius   the radius around location to search for (in m)
     * @param maxCount the maximum number of places to return in the list
     */
    @NonNull
    public List<PlaceInfo> getPlacesByCategory(
            @NonNull Location location, double radius, int maxCount, @NonNull String category) {
        return getPlacesInternal(location, radius, maxCount, category, true);
    }

    /**
     * Queries the map server and obtains a list of places within the radius of the given location,
     * that match the given name.
     *
     * @param location the location to search around of
     * @param radius   the radius around location to search for (in m)
     * @param maxCount the maximum number of places to return in the list
     */
    @NonNull
    public List<PlaceInfo> getPlacesByName(
            @NonNull Location location, double radius, int maxCount, @NonNull String name) {
        return getPlacesInternal(location, radius, maxCount, name, false);
    }

    private List<PlaceInfo> getPlacesInternal(
            Location location, double radius, int maxCount, String searchTerm, boolean isCategory) {
        List<PlaceInfo> places = new ArrayList<>();
        try {
            URL url = makeSearchURL(location, radius, searchTerm, isCategory);
            Log.i(TAG, "Searching with URL: " + url);
            String jsonResult = getResult(url);

            JSONObject root = throwIfError(new JSONObject(jsonResult));
            JSONArray jArray = root.getJSONArray("results");

            Log.i(TAG, "Search returned " + jArray.length() + " results");
            for (int i = 0; i < jArray.length() && i < maxCount; i++) {
                places.add(placeFromJson(jArray.getJSONObject(i)));
            }
            return places;
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error getting locations.", e);
        }

        return places;
    }

    private JSONObject throwIfError(JSONObject root) throws IOException {
        String error = root.optString("error_message");
        if (!error.isEmpty()) {
            throw new IOException(error);
        }
        return root;
    }

    /**
     * Prepares the URL to connect to the Places server from the specified location coordinates.
     *
     * @param location the location to search around of
     * @param radius   Radius in meters around the location to search through
     * @return URL The Places URL created based on the given lat/lon/radius
     */
    private URL makeSearchURL(
            Location location, double radius, String searchTerm, boolean isCategory)
            throws MalformedURLException {
        String url =
                SEARCH_URL
                        + "location="
                        + location.getLatitude()
                        + ","
                        + location.getLongitude()
                        + "&radius="
                        + radius
                        + "&sensor=true"
                        + "&key="
                        + mApiKey;

        if (isCategory) {
            url += "&type=" + searchTerm;
        } else {
            url += "&name=" + searchTerm;
        }

        return new URL(url);
    }

    private String makePhotoURL(String photoReference) throws MalformedURLException {
        return new URL(
                PHOTO_URL
                        + "maxwidth=400"
                        + "&photoreference="
                        + photoReference
                        + "&key="
                        + mApiKey)
                .toString();
    }

    private URL makeDetailsURL(String placeId) throws MalformedURLException {
        return new URL(
                DETAILS_URL
                        + "place_id="
                        + placeId
                        + "&fields=name,rating,formatted_phone_number,geometry,place_id,geometry,"
                        + "photo,icon"
                        + "&key="
                        + mApiKey);
    }

    private static String getResult(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        return streamToString(connection.getInputStream());
    }

    private static String streamToString(InputStream inputStream) throws IOException {
        StringBuilder outputBuilder = new StringBuilder();
        String string;
        if (inputStream != null) {
            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(inputStream, Encoding.UTF_8.toString()));
            while (null != (string = reader.readLine())) {
                outputBuilder.append(string).append('\n');
            }
        }
        return outputBuilder.toString();
    }

    private List<String> safePhotosFromJson(JSONObject json) throws JSONException {
        List<String> photos = new ArrayList<>();
        JSONArray jsonPhotos;
        try {
            jsonPhotos = json.getJSONArray("photos");
        } catch (JSONException e) {
            return new ArrayList<>();
        }

        for (int i = 0; i < jsonPhotos.length(); ++i) {
            JSONObject jsonPhoto = (JSONObject) jsonPhotos.get(i);
            String photoReference = jsonPhoto.getString("photo_reference");
            try {
                photos.add(makePhotoURL(photoReference));
            } catch (MalformedURLException e) {
                Log.e(TAG, "Failed to make URL for photo reference: " + photoReference);
            }
        }
        return photos;
    }

    private static PlaceInfo placeFromJson(JSONObject json) throws JSONException {
        JSONObject jsonLocation = json.getJSONObject("geometry").getJSONObject("location");
        Location placeLocation = new Location(json.getClass().toString());
        placeLocation.setLatitude(jsonLocation.getDouble("lat"));
        placeLocation.setLongitude(jsonLocation.getDouble("lng"));

        return new PlaceInfo(json.getString("place_id"), json.getString("name"), placeLocation);
    }

    @Nullable
    private static String safeGetString(JSONObject json, String key) {
        try {
            return json.getString(key);
        } catch (JSONException e) {
            return null;
        }
    }

    private static double safeGetDouble(JSONObject json, String key) {
        try {
            return json.getDouble(key);
        } catch (JSONException e) {
            return -1;
        }
    }
}
