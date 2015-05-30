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

import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.supportleanbackshowcase.cards.models.Card;
import android.support.v17.leanback.supportleanbackshowcase.cards.models.CardRow;
import android.support.v17.leanback.supportleanbackshowcase.cards.presenters.CardPresenterSelector;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.SearchOrbView;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;


public class BrowserExample01Fragment extends BrowseFragment {

    private static final String TAG = "BrowserExample01Fragment";

    private ArrayObjectAdapter mRowsAdapter;

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupUi();
        setupRowAdapter();
    }

    private void setupUi() {
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        setSearchAffordanceColors(
                new SearchOrbView.Colors(getResources().getColor(R.color.search_color),
                                         getResources().getColor(R.color.search_bright_color),
                                         getResources().getColor(R.color.search_icon_color)));
        setBrandColor(getResources().getColor(R.color.fastlane_background));
        setTitle("Browser Example 01");
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Toast.makeText(getActivity(), getString(R.string.implement_search),
                               Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupRowAdapter() {
        mRowsAdapter = new ArrayObjectAdapter(new ShadowRowPresenterSelector());
        createRows();
        setAdapter(mRowsAdapter);
    }

    private void createRows() {
        String json = Utils
                .inputStreamToString(getResources().openRawResource(R.raw.browsing_example_01));
        CardRow[] rows = new Gson().fromJson(json, CardRow[].class);
        for (CardRow row : rows) {
            mRowsAdapter.add(createCardRow(row));
        }
    }

    private ListRow createCardRow(final CardRow cardRow) {
        // Build main row using the ImageCardViewPresenter.
        PresenterSelector presenterSelector = new CardPresenterSelector(getActivity());
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(presenterSelector);
        for (Card card : cardRow.getCards()) {
            listRowAdapter.add(card);
        }
        HeaderItem header = new HeaderItem(cardRow.getTitle());
        return new CardListRow(header, listRowAdapter, cardRow);
    }

}
