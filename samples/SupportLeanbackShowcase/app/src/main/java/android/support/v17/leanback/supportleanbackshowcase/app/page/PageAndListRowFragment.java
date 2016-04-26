package android.support.v17.leanback.supportleanbackshowcase.app.page;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.app.RowsFragment;
import android.support.v17.leanback.supportleanbackshowcase.R;
import android.support.v17.leanback.supportleanbackshowcase.app.details.ShadowRowPresenterSelector;
import android.support.v17.leanback.supportleanbackshowcase.cards.presenters.CardPresenterSelector;
import android.support.v17.leanback.supportleanbackshowcase.models.Card;
import android.support.v17.leanback.supportleanbackshowcase.models.CardRow;
import android.support.v17.leanback.supportleanbackshowcase.utils.Utils;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.PageRow;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private static final String HEADER_NAME_3 = "Another page fragment";
    private ArrayObjectAdapter mRowsAdapter;

    public PageAndListRowFragment() {
        getMainFragmentRegistry().registerFragment(PageRow.class,
                new PageRowFragmentFactory());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUi();
        loadData();
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
    }

    private static class PageRowFragmentFactory extends BrowseFragment.FragmentFactory {

        @Override
        public Fragment createFragment(Object rowObj) {
            Row row = (Row)rowObj;
            if (row.getHeaderItem().getId() == HEADER_ID_1
                    || row.getHeaderItem().getId() == HEADER_ID_3) {
                return new SampleFragmentA();
            } else {
                return new SampleFragmentB();
            }
        }
    }

    public static class PageFragmentAdapterImpl extends MainFragmentAdapter<SampleFragmentA> {

        public PageFragmentAdapterImpl(SampleFragmentA fragment) {
            super(fragment);
            setScalingEnabled(true);
        }

        @Override
        public void setEntranceTransitionState(boolean state) {
            getFragment().setEntranceTransitionState(state);
        }
    }

    /**
     * Simple page fragment implementation.
     */
    public static class SampleFragmentA extends Fragment implements MainFragmentAdapterProvider {
        private final MainFragmentAdapter mMainFragmentAdapter =
                new PageAndListRowFragment.PageFragmentAdapterImpl(this);
        private boolean mEntranceTransitionState = true;

        @Override
        public View onCreateView(
                LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.page_fragment, container, false);
        }

        public void setEntranceTransitionState(boolean state) {
            mEntranceTransitionState = state;
            View view = getView();
            if (view == null) {
                return;
            }

            int visibility = state ? View.VISIBLE : View.INVISIBLE;
            view.findViewById(R.id.tv1).setVisibility(visibility);
            view.findViewById(R.id.tv2).setVisibility(visibility);
            view.findViewById(R.id.tv3).setVisibility(visibility);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setEntranceTransitionState(mEntranceTransitionState);
            mMainFragmentAdapter.getFragmentHost().notifyViewCreated(mMainFragmentAdapter);
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

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    createRows();
                }
            }, 500);

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
            return new ListRow(headerItem, adapter);
        }
    }
}
