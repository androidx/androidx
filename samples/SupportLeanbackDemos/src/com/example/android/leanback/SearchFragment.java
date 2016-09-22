package com.example.android.leanback;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.text.TextUtils;
import android.util.Log;

public class SearchFragment extends android.support.v17.leanback.app.SearchFragment
    implements android.support.v17.leanback.app.SearchFragment.SearchResultProvider {
    private static final String TAG = "leanback.SearchFragment";
    private static final int NUM_ROWS = 3;
    private static final int SEARCH_DELAY_MS = 1000;

    private ArrayObjectAdapter mRowsAdapter;
    private Handler mHandler = new Handler();
    private String mQuery;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        final Context context = getActivity();
        setBadgeDrawable(ResourcesCompat.getDrawable(context.getResources(),
                R.drawable.ic_title, context.getTheme()));
        setTitle("Leanback Sample App");
        setSearchResultProvider(this);
        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        Log.i(TAG, String.format("Search Query Text Change %s", newQuery));
        mRowsAdapter.clear();
        loadQuery(newQuery);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.i(TAG, String.format("Search Query Text Submit %s", query));
        mRowsAdapter.clear();
        loadQuery(query);
        return true;
    }

    private void loadQuery(String query) {
        mQuery = query;
        mHandler.removeCallbacks(mDelayedLoad);
        if (!TextUtils.isEmpty(query) && !query.equals("nil")) {
            mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        }
    }

    private void loadRows() {
        for (int i = 0; i < NUM_ROWS; ++i) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
            listRowAdapter.add(new PhotoItem("Hello world", R.drawable.gallery_photo_1));
            listRowAdapter.add(new PhotoItem("This is a test", R.drawable.gallery_photo_2));
            HeaderItem header = new HeaderItem(i, mQuery + " results row " + i);
            mRowsAdapter.add(new ListRow(header, listRowAdapter));
        }
    }

    private Runnable mDelayedLoad = new Runnable() {
        @Override
        public void run() {
            loadRows();
        }
    };

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {
            Intent intent = new Intent(getActivity(), DetailsActivity.class);
            intent.putExtra(DetailsActivity.EXTRA_ITEM, (PhotoItem) item);

            Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    getActivity(),
                    ((ImageCardView)itemViewHolder.view).getMainImageView(),
                    DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
            getActivity().startActivity(intent, bundle);
        }
    }
}
