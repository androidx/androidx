package android.support.v17.leanback.supportleanbackshowcase.app.page;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.app.VerticalGridFragment;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.supportleanbackshowcase.app.details.ShadowRowPresenterSelector;
import android.support.v17.leanback.supportleanbackshowcase.cards.presenters.CardPresenterSelector;
import android.support.v17.leanback.supportleanbackshowcase.cards.presenters.IconCardPresenter;
import android.support.v17.leanback.supportleanbackshowcase.models.Card;
import android.support.v17.leanback.supportleanbackshowcase.models.CardRow;
import android.support.v17.leanback.supportleanbackshowcase.utils.CardListRow;
import android.support.v17.leanback.supportleanbackshowcase.utils.Utils;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.FocusHighlight;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.PageRow;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.VerticalGridPresenter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.google.gson.Gson;

/**
 * Sample {@link BrowseFragment} implementation showcasing the use of {@link PageRow} and
 * {@link ListRow}.
 */
public class PageAndListRowFragment extends BrowseFragment {
    private static final long HEADER_ID_1 = 1;
    private static final String HEADER_NAME_1 = "Page Fragment";
    private static final long HEADER_ID_2 = 2;
    private static final String HEADER_NAME_2 = "Rows Fragment";
    private static final long HEADER_ID_3 = 3;
    private static final String HEADER_NAME_3 = "Settings Fragment";
    private static final long HEADER_ID_4 = 4;
    private static final String HEADER_NAME_4 = "User agreement Fragment";
    private BackgroundManager mBackgroundManager;

    private ArrayObjectAdapter mRowsAdapter;

    public PageAndListRowFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUi();
        loadData();
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        getMainFragmentRegistry().registerFragment(PageRow.class,
                new PageRowFragmentFactory(mBackgroundManager));
    }

    private void setupUi() {
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        setBrandColor(getResources().getColor(R.color.fastlane_background));
        setTitle(getString(R.string.page_list_row_title));
        setBadgeDrawable(getActivity().getResources().getDrawable(
                R.drawable.abc_ab_share_pack_mtrl_alpha));
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Toast.makeText(
                        getActivity(), getString(R.string.implement_search), Toast.LENGTH_SHORT)
                        .show();
            }
        });

        prepareEntranceTransition();
    }

    private void loadData() {
        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mRowsAdapter);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                createRows();
                startEntranceTransition();
            }
        }, 2000);
    }

    private void createRows() {
        HeaderItem headerItem1 = new HeaderItem(HEADER_ID_1, HEADER_NAME_1);
        PageRow pageRow1 = new PageRow(headerItem1);
        mRowsAdapter.add(pageRow1);

        HeaderItem headerItem2 = new HeaderItem(HEADER_ID_2, HEADER_NAME_2);
        PageRow pageRow2 = new PageRow(headerItem2);
        mRowsAdapter.add(pageRow2);

        HeaderItem headerItem3 = new HeaderItem(HEADER_ID_3, HEADER_NAME_3);
        PageRow pageRow3 = new PageRow(headerItem3);
        mRowsAdapter.add(pageRow3);

        HeaderItem headerItem4 = new HeaderItem(HEADER_ID_4, HEADER_NAME_4);
        PageRow pageRow4 = new PageRow(headerItem4);
        mRowsAdapter.add(pageRow4);
    }

    private static class PageRowFragmentFactory extends BrowseFragment.FragmentFactory {
        private final BackgroundManager mBackgroundManager;

        PageRowFragmentFactory(BackgroundManager backgroundManager) {
            this.mBackgroundManager = backgroundManager;
        }

        @Override
        public Fragment createFragment(Object rowObj) {
            Row row = (Row)rowObj;
            mBackgroundManager.setDrawable(null);
            if (row.getHeaderItem().getId() == HEADER_ID_1) {
                return new SampleFragmentA(mBackgroundManager);
            } else if (row.getHeaderItem().getId() == HEADER_ID_2) {
                return new SampleFragmentB();
            } else if (row.getHeaderItem().getId() == HEADER_ID_3) {
                return new SettingsFragment();
            } else if (row.getHeaderItem().getId() == HEADER_ID_4) {
                return new WebViewFragment();
            }

            throw new IllegalArgumentException(String.format("Invalid row %s", rowObj));
        }
    }

    public static class PageFragmentAdapterImpl extends MainFragmentAdapter<SampleFragmentA> {

        public PageFragmentAdapterImpl(SampleFragmentA fragment) {
            super(fragment);
        }
    }

    /**
     * Simple page fragment implementation.
     */
    public static class SampleFragmentA extends VerticalGridFragment
            implements MainFragmentAdapterProvider {

        private static final int COLUMNS = 4;
        private final MainFragmentAdapter mMainFragmentAdapter =
                new PageAndListRowFragment.PageFragmentAdapterImpl(this);
        private final int ZOOM_FACTOR = FocusHighlight.ZOOM_FACTOR_SMALL;
        private final BackgroundManager mBackgroundManager;
        private ArrayObjectAdapter mAdapter;

        public SampleFragmentA(BackgroundManager backgroundManager) {
            this.mBackgroundManager  = backgroundManager;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setupAdapter();
        }

        private void setupAdapter() {
            VerticalGridPresenter presenter = new VerticalGridPresenter(ZOOM_FACTOR);
            presenter.setNumberOfColumns(COLUMNS);
            setGridPresenter(presenter);

            CardPresenterSelector cardPresenter = new CardPresenterSelector(getActivity());
            mAdapter = new ArrayObjectAdapter(cardPresenter);
            setAdapter(mAdapter);

            setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
                @Override
                public void onItemSelected(
                        Presenter.ViewHolder itemViewHolder,
                        Object item,
                        RowPresenter.ViewHolder rowViewHolder,
                        Row row) {

                    Card card = (Card)item;
                    if(card.getLocalImageResourceName() != null) {
                        int resourceId = getActivity().getResources().getIdentifier(
                                card.getLocalImageResourceName(),
                                "drawable",
                                getActivity().getPackageName());

                        mBackgroundManager.setDrawable(getResources().getDrawable(resourceId));
                    }
                }
            });

            prepareEntranceTransition();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadData();
                    startEntranceTransition();
                }
            }, 200);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getMainFragmentAdapter().getFragmentHost().notifyViewCreated(mMainFragmentAdapter);
        }

        private void loadData() {
            String json = Utils.inputStreamToString(getResources().openRawResource(
                    R.raw.grid_example));
            CardRow cardRow = new Gson().fromJson(json, CardRow.class);
            mAdapter.addAll(0, cardRow.getCards());
        }

        @Override
        public View onCreateView(
                LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public MainFragmentAdapter getMainFragmentAdapter() {
            return mMainFragmentAdapter;
        }
    }

    /**
     * Page fragment embeds a rows fragment.
     */
    public static class SampleFragmentB extends RowsFragment {
        private final ArrayObjectAdapter mRowsAdapter;

        public SampleFragmentB() {
            mRowsAdapter = new ArrayObjectAdapter(new ShadowRowPresenterSelector());

            setAdapter(mRowsAdapter);
            setOnItemViewClickedListener(new OnItemViewClickedListener() {
                @Override
                public void onItemClicked(
                        Presenter.ViewHolder itemViewHolder,
                        Object item,
                        RowPresenter.ViewHolder rowViewHolder,
                        Row row) {
                    Toast.makeText(getActivity(), "Implement click handler", Toast.LENGTH_SHORT)
                            .show();
                }
            });
        }


        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    createRows();
                }
            }, 200);
        }

        private void createRows() {
            if (isAdded()) {
                String json = Utils.inputStreamToString(getResources().openRawResource(
                        R.raw.cards_example));
                CardRow[] rows = new Gson().fromJson(json, CardRow[].class);
                for (CardRow row : rows) {
                    mRowsAdapter.add(createCardRow(row));
                }
            }
        }

        private ListRow createCardRow(CardRow cardRow) {
            PresenterSelector presenterSelector = new CardPresenterSelector(getActivity());
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenterSelector);
            for(Card card : cardRow.getCards()) {
                adapter.add(card);
            }

            HeaderItem headerItem = new HeaderItem(cardRow.getTitle());
            return new CardListRow(headerItem, adapter, cardRow);
        }
    }

    public static class SettingsFragment extends RowsFragment {
        private final ArrayObjectAdapter mRowsAdapter;

        public SettingsFragment() {
            ListRowPresenter selector = new ListRowPresenter();
            selector.setNumRows(2);
            mRowsAdapter = new ArrayObjectAdapter(selector);
            setAdapter(mRowsAdapter);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadData();
                }
            }, 200);
        }

        private void loadData() {
            if (isAdded()) {
                String json = Utils.inputStreamToString(getResources().openRawResource(
                        R.raw.icon_example));
                CardRow cardRow = new Gson().fromJson(json, CardRow.class);
                mRowsAdapter.add(createCardRow(cardRow));
            }
        }

        private ListRow createCardRow(CardRow cardRow) {
            SettingsIconPresenter iconCardPresenter = new SettingsIconPresenter(getActivity());
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(iconCardPresenter);
            for(Card card : cardRow.getCards()) {
                adapter.add(card);
            }

            HeaderItem headerItem = new HeaderItem(cardRow.getTitle());
            return new CardListRow(headerItem, adapter, cardRow);
        }
    }

    public static class WebViewFragment extends Fragment implements MainFragmentAdapterProvider {
        private MainFragmentAdapter mMainFragmentAdapter = new MainFragmentAdapter(this);
        private WebView mWebview;

        @Override
        public MainFragmentAdapter getMainFragmentAdapter() {
            return mMainFragmentAdapter;
        }

        @Override
        public View onCreateView(
                LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            FrameLayout root = new FrameLayout(getActivity());
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            lp.setMarginStart(32);
            mWebview = new WebView(getActivity());
            mWebview.setWebViewClient(new WebViewClient());
            mWebview.getSettings().setJavaScriptEnabled(true);
            root.addView(mWebview, lp);
            return root;
        }

        @Override
        public void onResume() {
            super.onResume();
            mWebview.loadUrl("https://www.google.com/policies/terms");
        }
    }
}
