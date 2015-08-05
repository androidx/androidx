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

package android.support.v17.leanback.supportleanbackshowcase.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.supportleanbackshowcase.app.cards.CardExampleActivity;
import android.support.v17.leanback.supportleanbackshowcase.app.details.DetailViewExampleActivity;
import android.support.v17.leanback.supportleanbackshowcase.app.dialog.DialogExampleActivity;
import android.support.v17.leanback.supportleanbackshowcase.app.grid.GridExampleActivity;
import android.support.v17.leanback.supportleanbackshowcase.app.media.MusicExampleActivity;
import android.support.v17.leanback.supportleanbackshowcase.app.media.VideoExampleActivity;
import android.support.v17.leanback.supportleanbackshowcase.app.settings.SettingsExampleActivity;
import android.support.v17.leanback.supportleanbackshowcase.app.wizard.WizardExampleActivity;
import android.support.v17.leanback.supportleanbackshowcase.cards.presenters.CardPresenterSelector;
import android.support.v17.leanback.supportleanbackshowcase.models.Card;
import android.support.v17.leanback.supportleanbackshowcase.models.CardRow;
import android.support.v17.leanback.supportleanbackshowcase.models.Movie;
import android.support.v17.leanback.supportleanbackshowcase.utils.Utils;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;

import com.google.gson.Gson;


public class MainFragment extends BrowseFragment {

    private ArrayObjectAdapter mRowsAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setupUIElements();
        setupRowAdapter();
        setupEventListeners();
    }

    private void setupRowAdapter() {
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        createRows();
        setAdapter(mRowsAdapter);
    }

    private void createRows() {
        String json = Utils
                .inputStreamToString(getResources().openRawResource(R.raw.launcher_cards));
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

    private final class ItemViewClickedListener implements OnItemViewClickedListener {

        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            Card card = (Card) item;
            int id = card.getId();
            switch (id) {
                case 0: {
                    Intent intent = new Intent(getActivity().getBaseContext(),
                            CardExampleActivity.class);
                    startActivity(intent);
                    break;
                }
                case 2: {
                    Intent intent = new Intent(getActivity().getBaseContext(),
                            GridExampleActivity.class);
                    startActivity(intent);
                    break;
                }
                case 3: {
                    Intent intent = new Intent(getActivity().getBaseContext(),
                            DetailViewExampleActivity.class);
                    startActivity(intent);
                    break;
                }
                case 4: {
                    Intent intent = new Intent(getActivity().getBaseContext(),
                            VideoExampleActivity.class);
                    startActivity(intent);
                    break;
                }
                case 5: {
                    Intent intent = new Intent(getActivity().getBaseContext(),
                            MusicExampleActivity.class);
                    startActivity(intent);
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

        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
        }
    }
}
