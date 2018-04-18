package com.example.android.leanback;

import static com.example.android.leanback.CardPresenter.CONTENT;
import static com.example.android.leanback.CardPresenter.IMAGE;
import static com.example.android.leanback.CardPresenter.TITLE;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.DiffCallback;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;

import java.util.ArrayList;

public class SearchFragment extends androidx.leanback.app.SearchFragment
        implements androidx.leanback.app.SearchFragment.SearchResultProvider {
    private static final String TAG = "leanback.SearchFragment";
    private static final int NUM_ROWS = 3;
    private static final int SEARCH_DELAY_MS = 1000;

    private ArrayObjectAdapter mRowsAdapter;
    private Handler mHandler = new Handler();
    private String mQuery;

    // Flag to represent if data set one is presented in the fragment
    private boolean mIsDataSetOnePresented;

    // Adapter for first row
    private ArrayObjectAdapter mFirstRowAdapter;

    // The diff callback which defines the standard to judge if two items are the same or if
    // two items have the same content.
    private DiffCallback<PhotoItem> mDiffCallback = new DiffCallback<PhotoItem>() {

        // when two photo items have the same id, they are the same from adapter's
        // perspective
        @Override
        public boolean areItemsTheSame(PhotoItem oldItem, PhotoItem newItem) {
            return oldItem.getId() == newItem.getId();
        }

        // when two photo items is equal to each other (based on the equal method defined in
        // PhotoItem), they have the same content.
        @Override
        public boolean areContentsTheSame(PhotoItem oldItem, PhotoItem newItem) {
            return oldItem.equals(newItem);
        }

        @Nullable
        @Override
        public Object getChangePayload(PhotoItem oldItem, PhotoItem newItem) {
            Bundle diff = new Bundle();
            if (oldItem.getImageResourceId()
                    != newItem.getImageResourceId()) {
                diff.putLong(IMAGE, newItem.getImageResourceId());
            }

            if (oldItem.getTitle() != null && newItem.getTitle() != null
                    && !oldItem.getTitle().equals(newItem.getTitle())) {
                diff.putString(TITLE, newItem.getTitle());
            }

            if (oldItem.getContent() != null && newItem.getContent() != null
                    && !oldItem.getContent().equals(newItem.getContent())) {
                diff.putString(CONTENT, newItem.getContent());
            }
            return diff;
        }
    };

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
        HeaderItem header = new HeaderItem(0, mQuery + " results row " + 0);

        // Every time when the query event is fired, we will update the fake search result in the
        // first row based on the flag mIsDataSetOnePresented flag.
        // Also the first row adapter will only be created once so the animation will be triggered
        // when the items in the adapter changed.
        if (!mIsDataSetOnePresented) {
            if (mFirstRowAdapter == null) {
                mFirstRowAdapter = createFirstListRowAdapter();
            } else {
                mFirstRowAdapter.setItems(createDataSetOneDebug(), mDiffCallback);
            }
            mIsDataSetOnePresented = true;
        } else {
            mFirstRowAdapter.setItems(createDataSetTwoDebug(), mDiffCallback);
            mIsDataSetOnePresented = false;
        }
        mRowsAdapter.add(new ListRow(header, mFirstRowAdapter));
        for (int i = 1; i < NUM_ROWS + 1; ++i) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
            listRowAdapter.add(new PhotoItem("Hello world", R.drawable.gallery_photo_1));
            listRowAdapter.add(new PhotoItem("This is a test", R.drawable.gallery_photo_2));
            header = new HeaderItem(i, mQuery + " results row " + i);
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
                    ((ImageCardView) itemViewHolder.view).getMainImageView(),
                    DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
            getActivity().startActivity(intent, bundle);
        }
    }


    private ArrayObjectAdapter createFirstListRowAdapter() {
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
        listRowAdapter.setItems(createDataSetOneDebug(), mDiffCallback);
        mIsDataSetOnePresented = true;
        return listRowAdapter;
    }

    /**
     * Create a data set (data set one) for the last row of this browse fragment. It will be
     * changed by another set of data when user click one of the photo items in the list.
     * Different with other rows in the browsing fragment, the photo item in last row all have been
     * allocated with a unique id. And the id will be used to jduge if two photo items are the same
     * or not.
     *
     * @return List of photoItem
     */
    private ArrayList<PhotoItem> createDataSetOne() {
        ArrayList<PhotoItem> photoItems = new ArrayList<>();
        photoItems.add(new PhotoItem(
                "Hello world",
                R.drawable.gallery_photo_1,
                1));
        photoItems.add(new PhotoItem(
                "This is a test",
                "Only a test",
                R.drawable.gallery_photo_2,
                2));
        photoItems.add(new PhotoItem(
                "Android TV",
                "by Google",
                R.drawable.gallery_photo_3,
                3));
        photoItems.add(new PhotoItem(
                "Leanback",
                R.drawable.gallery_photo_4,
                4));
        photoItems.add(new PhotoItem(
                "GuidedStep (Slide left/right)",
                R.drawable.gallery_photo_5,
                5));
        photoItems.add(new PhotoItem(
                "GuidedStep (Slide bottom up)",
                "Open GuidedStepFragment",
                R.drawable.gallery_photo_6,
                6));
        photoItems.add(new PhotoItem(
                "Android TV",
                "open RowsActivity",
                R.drawable.gallery_photo_7,
                7));
        photoItems.add(new PhotoItem(
                "Leanback",
                "open BrowseActivity",
                R.drawable.gallery_photo_8,
                8));
        photoItems.add(new PhotoItem(
                "Hello world",
                R.drawable.gallery_photo_1,
                1));
        photoItems.add(new PhotoItem(
                "This is a test",
                "Only a test",
                R.drawable.gallery_photo_2,
                2));
        photoItems.add(new PhotoItem(
                "Android TV",
                "by Google",
                R.drawable.gallery_photo_3,
                3));
        photoItems.add(new PhotoItem(
                "Leanback",
                R.drawable.gallery_photo_4,
                4));
        return photoItems;
    }

    /**
     * Create a new data set (data set one) for the last row of this browse fragment. It will be
     * changed by another set of data when user click one of the photo items in the list.
     * Different with other rows in the browsing fragment, the photo item in last row all have been
     * allocated with a unique id. And the id will be used to jduge if two photo items are the same
     * or not.
     *
     * @return List of photoItem
     */
    private ArrayList<PhotoItem> createDataSetTwo() {
        ArrayList<PhotoItem> photoItems = new ArrayList<>();
        photoItems.add(new PhotoItem(
                "This is a test",
                "Only a test",
                R.drawable.gallery_photo_2,
                2));
        photoItems.add(new PhotoItem(
                "Hello world",
                R.drawable.gallery_photo_1,
                1));
        photoItems.add(new PhotoItem(
                "Leanback",
                R.drawable.gallery_photo_4,
                4));
        photoItems.add(new PhotoItem(
                "Android TV",
                "by Google",
                R.drawable.gallery_photo_3,
                3));
        photoItems.add(new PhotoItem(
                "change title",
                R.drawable.gallery_photo_5,
                5));
        photoItems.add(new PhotoItem(
                "GuidedStep (Slide bottom up)",
                "change comment",
                R.drawable.gallery_photo_6,
                6));
        photoItems.add(new PhotoItem(
                "Android TV",
                R.drawable.gallery_photo_7,
                7));
        photoItems.add(new PhotoItem(
                "Leanback",
                "open BrowseActivity",
                R.drawable.gallery_photo_7,
                8));
        photoItems.add(new PhotoItem(
                "Hello world",
                R.drawable.gallery_photo_1,
                10));
        photoItems.add(new PhotoItem(
                "This is a test",
                "Only a test",
                R.drawable.gallery_photo_2,
                20));
        photoItems.add(new PhotoItem(
                "Android TV",
                "by Google",
                R.drawable.gallery_photo_3,
                30));
        photoItems.add(new PhotoItem(
                "Leanback",
                R.drawable.gallery_photo_4,
                40));
        return photoItems;
    }


    private ArrayList<PhotoItem> createDataSetOneDebug() {
        ArrayList<PhotoItem> photoItems = new ArrayList<>();
        photoItems.add(new PhotoItem(
                "Hello world",
                R.drawable.gallery_photo_1,
                1));
        return photoItems;
    }

    /**
     * Create a new data set (data set one) for the last row of this browse fragment. It will be
     * changed by another set of data when user click one of the photo items in the list.
     * Different with other rows in the browsing fragment, the photo item in last row all have been
     * allocated with a unique id. And the id will be used to jduge if two photo items are the same
     * or not.
     *
     * @return List of photoItem
     */
    private ArrayList<PhotoItem> createDataSetTwoDebug() {
        ArrayList<PhotoItem> photoItems = new ArrayList<>();
        photoItems.add(new PhotoItem(
                "Hello world Hello world",
                R.drawable.gallery_photo_1,
                1));
        return photoItems;
    }
}
