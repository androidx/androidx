/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package android.support.v17.leanback.supportleanbackshowcase;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.supportleanbackshowcase.cards.models.Card;
import android.support.v17.leanback.supportleanbackshowcase.cards.models.CardRow;
import android.support.v17.leanback.supportleanbackshowcase.cards.presenters.CardPresenterSelector;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.gson.Gson;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;


public class MainFragment extends BrowseFragment {

    public static final String VIDEO_SURFACE_FRAGMENT_TAG = "VIDEO_SURFACE";
    private static final String TAG = "MainFragment";
    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int DEFAULT_BACKGROUND_IMAGE = R.drawable.default_background;
    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private Target mBackgroundTarget;
    private Timer mBackgroundTimer;
    private URI mBackgroundURI;
    private BackgroundManager mBackgroundManager;
    private DisplayMetrics mMetrics;

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        if (Constants.LOCAL_LOGD) Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);

        setupBackgroundManager();
        setupUIElements();
        setupRowAdapter();
        setupEventListeners();
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundTimer) {
            if (Constants.LOCAL_LOGD) Log.d(TAG, "onDestroy: " + mBackgroundTimer.toString());
            mBackgroundTimer.cancel();
        }
    }

    private void setupRowAdapter() {
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        createRows();
        setAdapter(mRowsAdapter);
    }

    private void createRows() {
        String json = Utils
                .inputStreamToString(getResources().openRawResource(R.raw.cards_launcher));
        CardRow[] rows = new Gson().fromJson(json, CardRow[].class);
        for (CardRow row : rows) {
            mRowsAdapter.add(createCardRow(row));
        }
    }

    private ListRow createCardRow(CardRow cardRow) {
        PresenterSelector presenterSelector = new CardPresenterSelector(getActivity());
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(presenterSelector);
        for (Card card : cardRow.getCards()) {
            listRowAdapter.add(card);
        }
        return new ListRow(listRowAdapter);
    }

    private void setupBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.setThemeDrawableResourceId(DEFAULT_BACKGROUND_IMAGE);
        mBackgroundManager.attach(getActivity().getWindow());

        mBackgroundTarget = new PicassoBackgroundManagerTarget(mBackgroundManager);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
        getView().setBackgroundResource(R.drawable.bg_living_room_wide);
        //updateBackgroundImage(R.drawable.bg_living_room_wide);
    }

    private void setupUIElements() {
        setTitle(getString(R.string.browse_title));
        setBadgeDrawable(getResources().getDrawable(R.drawable.title_android_tv, null));
        setHeadersState(HEADERS_DISABLED);
        setHeadersTransitionOnBackEnabled(false);
        setBrandColor(getResources().getColor(R.color.fastlane_background));
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    protected void updateBackgroundImage(URI uri) {
        // Deactivated until we decide whether to load a background image from an URL or resource. @hahnr
        if (true) return;
        Picasso.with(getActivity()).load(uri.toString())
               .resize(mMetrics.widthPixels, mMetrics.heightPixels).centerCrop()
               .error(DEFAULT_BACKGROUND_IMAGE).into(mBackgroundTarget);
    }

    protected void updateBackgroundImage(Drawable drawable) {
        mBackgroundManager.setDrawable(drawable);
    }

    protected void updateBackgroundImage(int resId) {
        mBackgroundManager.setDrawable(getResources().getDrawable(resId, null));
    }

    private void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {

        @Override public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                            RowPresenter.ViewHolder rowViewHolder, Row row) {
            Card card = (Card) item;
            int id = card.getId();
            switch (id) {
                case 0: {
                    updateBackgroundImage(new ColorDrawable(
                            getResources().getColor(R.color.card_examples_background)));
                    Fragment fragment = new CardExampleFragment();
                    getFragmentManager().beginTransaction()
                                        .replace(R.id.fragmentContainer, fragment)
                                        .addToBackStack(null).commit();
                    break;
                }
                case 2: {
                    updateBackgroundImage(
                            getResources().getDrawable(R.drawable.background_canyon, null));
                    Fragment fragment = new GridExample();
                    getFragmentManager().beginTransaction()
                                        .replace(R.id.fragmentContainer, fragment)
                                        .addToBackStack(null).commit();
                    break;
                }
                case 3: {
                    Intent intent = new Intent(getActivity().getBaseContext(),
                            DetailViewExampleActivity.class);
                    startActivity(intent);
                    break;
                }
                case 4: {
                    updateBackgroundImage(
                            getResources().getDrawable(R.drawable.background_canyon, null));
                    Fragment fragment = new VideoConsumptionExampleFragment();
                    getFragmentManager().beginTransaction()
                                        .replace(R.id.fragmentContainer, new VideoSurfaceFragment(),
                                                 VIDEO_SURFACE_FRAGMENT_TAG)
                                        .add(R.id.fragmentContainer, fragment).addToBackStack(null)
                                        .commit();
                    break;
                }
                case 5: {
                    updateBackgroundImage(
                            getResources().getDrawable(R.drawable.background_sax, null));
                    Fragment fragment = new MusicConsumptionExampleFragment();
                    getFragmentManager().beginTransaction()
                                        .replace(R.id.fragmentContainer, fragment)
                                        .addToBackStack(null).commit();
                    break;
                }
                case 6: {
                    // Let's create a new Wizard for a given Movie. The movie can come from any sort
                    // of data source. To simplify this example we decode it from a JSON source
                    // which might be loaded from a server in a real world example.
                    Intent intent = new Intent(getActivity().getBaseContext(),
                            WizardExampleActivity.class);

                    // Prepare extras which contains the Movie and will be passed to the Activity
                    // which is started through the Intent/.
                    Bundle extras = new Bundle();
                    String json = Utils.inputStreamToString(
                            getResources().openRawResource(R.raw.wizard_example));
                    Movie movie = new Gson().fromJson(json, Movie.class);
                    extras.putSerializable("movie", movie);
                    intent.putExtras(extras);

                    // Finally, start the wizard Activity.
                    startActivity(intent);
                    break;
                }
                case 7: {
                    Intent intent = new Intent(getActivity().getBaseContext(),
                            SettingsExampleActivity.class);
                    startActivity(intent);
                    break;
                }
                case 8: {
                    Intent intent = new Intent(getActivity().getBaseContext(),
                            DialogExampleActivity.class);
                    startActivity(intent);
                    break;
                }
                default:
                    break;
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {

        @Override public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                             RowPresenter.ViewHolder rowViewHolder, Row row) {
        }
    }

    private class UpdateBackgroundTask extends TimerTask {

        @Override public void run() {
            mHandler.post(new Runnable() {
                @Override public void run() {
                    if (mBackgroundURI != null) {
                        updateBackgroundImage(mBackgroundURI);
                    }
                }
            });
        }
    }
}
