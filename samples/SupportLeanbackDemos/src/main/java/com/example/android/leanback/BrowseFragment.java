/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.example.android.leanback;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.app.GuidedStepFragment;
import androidx.leanback.app.RowsFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.DividerRow;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.PageRow;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SectionRow;

public class BrowseFragment extends androidx.leanback.app.BrowseFragment {
    private static final String TAG = "leanback.BrowseFragment";

    private static final boolean TEST_ENTRANCE_TRANSITION = true;
    private static final int NUM_ROWS = 8;
    private static final long HEADER_ID1 = 1001;
    private static final long HEADER_ID2 = 1002;
    private static final long HEADER_ID3 = 1003;

    private ArrayObjectAdapter mRowsAdapter;
    private BackgroundHelper mBackgroundHelper;

    // For good performance, it's important to use a single instance of
    // a card presenter for all rows using that presenter.
    final CardPresenter mCardPresenter = new CardPresenter();
    final CardPresenter mCardPresenter2 = new CardPresenter(R.style.MyImageCardViewTheme);

    public BrowseFragment() {
        getMainFragmentRegistry().registerFragment(PageRow.class, new PageRowFragmentFactory());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        mBackgroundHelper = new BackgroundHelper(getActivity());
        mBackgroundHelper.attachToWindow();

        setBadgeDrawable(ResourcesCompat.getDrawable(getActivity().getResources(),
                R.drawable.ic_title, getActivity().getTheme()));
        setTitle("Leanback Sample App");
        setHeadersState(HEADERS_ENABLED);
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                    RowPresenter.ViewHolder rowViewHolder, Row row) {
                Log.i(TAG, "onItemSelected: " + item + " row " + row);

                updateBackgroundToSelection();
            }
        });
        setBrowseTransitionListener(new BrowseTransitionListener() {
            @Override
            public void onHeadersTransitionStop(boolean withHeaders) {
                updateBackgroundToSelection();
            }
        });
        if (TEST_ENTRANCE_TRANSITION) {
            // don't run entrance transition if fragment is restored.
            if (savedInstanceState == null) {
                prepareEntranceTransition();
            }
        }

        // simulates in a real world use case  data being loaded two seconds later
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setupRows();
                loadData();
                startEntranceTransition();
            }
        }, 2000);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateBackgroundToSelection();
    }

    void updateBackgroundToSelection() {
        if (!isShowingHeaders()) {
            RowPresenter.ViewHolder rowViewHolder = getSelectedRowViewHolder();
            Object item = rowViewHolder == null ? null : rowViewHolder.getSelectedItem();
            if (item != null) {
                mBackgroundHelper.setBackground(((PhotoItem) item).getImageResourceId());
            } else {
                mBackgroundHelper.clearDrawable();
            }
        } else {
            mBackgroundHelper.clearDrawable();
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void setupRows() {
        ListRowPresenter listRowPresenter = new ListRowPresenter();
        listRowPresenter.setNumRows(1);
        mRowsAdapter = new ArrayObjectAdapter(listRowPresenter);
    }

    private void loadData() {
        int i = 0;

        mRowsAdapter.add(new PageRow(new HeaderItem(HEADER_ID1, "Page Row 0")));
        mRowsAdapter.add(new DividerRow());

        mRowsAdapter.add(new SectionRow(new HeaderItem("section 0")));
        for (; i < NUM_ROWS; ++i) {
            HeaderItem headerItem = new HeaderItem(i, "Row " + i);
            headerItem.setDescription("Description for Row "+i);
            mRowsAdapter.add(new ListRow(headerItem, createListRowAdapter(i)));
        }

        mRowsAdapter.add(new DividerRow());
        mRowsAdapter.add(new PageRow(new HeaderItem(HEADER_ID2, "Page Row 1")));

        mRowsAdapter.add(new PageRow(new HeaderItem(HEADER_ID3, "Page Row 2")));
        setAdapter(mRowsAdapter);
    }

    private ArrayObjectAdapter createListRowAdapter(int i) {
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter((i & 1) == 0 ?
                mCardPresenter : mCardPresenter2);
        listRowAdapter.add(new PhotoItem(
                "Hello world",
                R.drawable.gallery_photo_1));
        listRowAdapter.add(new PhotoItem(
                "This is a test",
                "Only a test",
                R.drawable.gallery_photo_2));
        listRowAdapter.add(new PhotoItem(
                "Android TV",
                "by Google",
                R.drawable.gallery_photo_3));
        listRowAdapter.add(new PhotoItem(
                "Leanback",
                R.drawable.gallery_photo_4));
        listRowAdapter.add(new PhotoItem(
                "GuidedStep (Slide left/right)",
                R.drawable.gallery_photo_5));
        listRowAdapter.add(new PhotoItem(
                "GuidedStep (Slide bottom up)",
                "Open GuidedStepFragment",
                R.drawable.gallery_photo_6));
        listRowAdapter.add(new PhotoItem(
                "Android TV",
                "open RowsActivity",
                R.drawable.gallery_photo_7));
        listRowAdapter.add(new PhotoItem(
                "Leanback",
                "open BrowseActivity",
                R.drawable.gallery_photo_8));
        listRowAdapter.add(new PhotoItem(
                "Hello world",
                R.drawable.gallery_photo_1));
        listRowAdapter.add(new PhotoItem(
                "This is a test",
                "Only a test",
                R.drawable.gallery_photo_2));
        listRowAdapter.add(new PhotoItem(
                "Android TV",
                "by Google",
                R.drawable.gallery_photo_3));
        listRowAdapter.add(new PhotoItem(
                "Leanback",
                R.drawable.gallery_photo_4));
        return listRowAdapter;
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {

            Intent intent;
            Bundle bundle;
            if (((PhotoItem) item).getImageResourceId() == R.drawable.gallery_photo_6) {
                GuidedStepFragment.add(getFragmentManager(),
                        new GuidedStepHalfScreenActivity.FirstStepFragment(),
                        R.id.lb_guidedstep_host);
                return;
            } else if (((PhotoItem) item).getImageResourceId() == R.drawable.gallery_photo_5) {
                GuidedStepFragment.add(getFragmentManager(),
                        new GuidedStepActivity.FirstStepFragment(), R.id.lb_guidedstep_host);
                return;
            } else if (((PhotoItem) item).getImageResourceId() == R.drawable.gallery_photo_8) {
                intent = new Intent(getActivity(), BrowseActivity.class);
                bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity())
                        .toBundle();
            } else if (((PhotoItem) item).getImageResourceId() == R.drawable.gallery_photo_7) {
                intent = new Intent(getActivity(), RowsActivity.class);
                bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity())
                        .toBundle();
            } else {
                intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(DetailsActivity.EXTRA_ITEM, (PhotoItem) item);
                bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
            }
            getActivity().startActivity(intent, bundle);
        }
    }

    public static class PageRowFragmentFactory extends FragmentFactory {

        @Override
        public Fragment createFragment(Object rowObj) {
            Row row = (Row) rowObj;
            if (row.getHeaderItem().getId() == HEADER_ID1) {
                return new SampleRowsFragment();
            } else if (row.getHeaderItem().getId() == HEADER_ID2) {
                return new SampleRowsFragment();
            } else if (row.getHeaderItem().getId() == HEADER_ID3) {
                return new SampleFragment();
            }

            return null;
        }
    }

    public static class SampleRowsFragment extends RowsFragment {
        final CardPresenter mCardPresenter = new CardPresenter();
        final CardPresenter mCardPresenter2 = new CardPresenter(R.style.MyImageCardViewTheme);

        void loadFragmentData() {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(new ListRowPresenter());
            for (int i = 0; i < 4; i++) {
                ListRow row = new ListRow(new HeaderItem("Row " + i), createListRowAdapter(i));
                adapter.add(row);
            }
            if (getMainFragmentAdapter() != null) {
                getMainFragmentAdapter().getFragmentHost()
                        .notifyDataReady(getMainFragmentAdapter());
            }
            setAdapter(adapter);
        }

        public SampleRowsFragment() {
            // simulates late data loading:
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadFragmentData();
                }
            }, 500);

            setOnItemViewClickedListener(new OnItemViewClickedListener() {
                @Override
                public void onItemClicked(
                        Presenter.ViewHolder itemViewHolder,
                        Object item,
                        RowPresenter.ViewHolder rowViewHolder,
                        Row row) {
                    Intent intent;
                    Bundle bundle;
                    if (((PhotoItem) item).getImageResourceId() == R.drawable.gallery_photo_6) {
                        GuidedStepFragment.add(getActivity().getFragmentManager(),
                                new GuidedStepHalfScreenActivity.FirstStepFragment(),
                                R.id.lb_guidedstep_host);
                        return;
                    } else if (((PhotoItem) item).getImageResourceId() == R.drawable.gallery_photo_5) {
                        GuidedStepFragment.add(getActivity().getFragmentManager(),
                                new GuidedStepActivity.FirstStepFragment(), R.id.lb_guidedstep_host);
                        return;
                    } else if (((PhotoItem) item).getImageResourceId() == R.drawable.gallery_photo_8) {
                        intent = new Intent(getActivity(), BrowseActivity.class);
                        bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity())
                                .toBundle();
                    } else if (((PhotoItem) item).getImageResourceId() == R.drawable.gallery_photo_7) {
                        intent = new Intent(getActivity(), RowsActivity.class);
                        bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity())
                                .toBundle();
                    } else {
                        intent = new Intent(getActivity(), DetailsActivity.class);
                        intent.putExtra(DetailsActivity.EXTRA_ITEM, (PhotoItem) item);
                        bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                getActivity(),
                                ((ImageCardView) itemViewHolder.view).getMainImageView(),
                                DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                    }
                    getActivity().startActivity(intent, bundle);
                }
            });
        }

        private ArrayObjectAdapter createListRowAdapter(int i) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter((i & 1) == 0 ?
                    mCardPresenter : mCardPresenter2);
            listRowAdapter.add(new PhotoItem(
                    "Hello world",
                    R.drawable.gallery_photo_1));
            listRowAdapter.add(new PhotoItem(
                    "This is a test",
                    "Only a test",
                    R.drawable.gallery_photo_2));
            listRowAdapter.add(new PhotoItem(
                    "Android TV",
                    "by Google",
                    R.drawable.gallery_photo_3));
            listRowAdapter.add(new PhotoItem(
                    "Leanback",
                    R.drawable.gallery_photo_4));
            listRowAdapter.add(new PhotoItem(
                    "GuidedStep (Slide left/right)",
                    R.drawable.gallery_photo_5));
            listRowAdapter.add(new PhotoItem(
                    "GuidedStep (Slide bottom up)",
                    "Open GuidedStepFragment",
                    R.drawable.gallery_photo_6));
            listRowAdapter.add(new PhotoItem(
                    "Android TV",
                    "open RowsActivity",
                    R.drawable.gallery_photo_7));
            listRowAdapter.add(new PhotoItem(
                    "Leanback",
                    "open BrowseActivity",
                    R.drawable.gallery_photo_8));
            return listRowAdapter;
        }
    }

    public static class PageFragmentAdapterImpl extends MainFragmentAdapter<SampleFragment> {

        public PageFragmentAdapterImpl(SampleFragment fragment) {
            super(fragment);
            setScalingEnabled(true);
        }

        @Override
        public void setEntranceTransitionState(boolean state) {
            getFragment().setEntranceTransitionState(state);
        }
    }

    public static class SampleFragment extends Fragment implements MainFragmentAdapterProvider {

        final PageFragmentAdapterImpl mMainFragmentAdapter = new PageFragmentAdapterImpl(this);

        public void setEntranceTransitionState(boolean state) {
            final View view = getView();
            int visibility = state ? View.VISIBLE : View.INVISIBLE;
            view.findViewById(R.id.tv1).setVisibility(visibility);
            view.findViewById(R.id.tv2).setVisibility(visibility);
            view.findViewById(R.id.tv3).setVisibility(visibility);
        }

        @Override
        public View onCreateView(
                final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.page_fragment, container, false);
            view.findViewById(R.id.tv1).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(inflater.getContext(), GuidedStepActivity.class);
                    startActivity(intent);
                }
            });

            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            // static layout has view and data ready immediately
            mMainFragmentAdapter.getFragmentHost().notifyViewCreated(mMainFragmentAdapter);
            mMainFragmentAdapter.getFragmentHost().notifyDataReady(mMainFragmentAdapter);
        }

        @Override
        public MainFragmentAdapter getMainFragmentAdapter() {
            return mMainFragmentAdapter;
        }
    }
}
