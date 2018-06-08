/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.leanback.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import android.content.Context;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.leanback.R;
import androidx.recyclerview.widget.RecyclerView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

@SmallTest
@RunWith(JUnit4.class)
public class ObjectAdapterTest {

    private static final String ID = "id";
    private static final String STRING_MEMBER_ONE = "stringMemberOne";
    private static final String STRING_MEMBER_TWO = "stringMemberTwo";
    private static final String NOT_RELATED_STRING_MEMBER = "notRelatedStringMember";

    protected ItemBridgeAdapter mBridgeAdapter;
    protected ArrayObjectAdapter mAdapter;

    private ArrayList mItems;
    private DiffCallback<AdapterItem> mMockedCallback;
    private DiffCallback<AdapterItem> mCallbackWithoutPayload;
    private RecyclerView.AdapterDataObserver mObserver;

    private Context mContext;
    private ListRowPresenter mListRowPresenter;
    private ListRowPresenter.ViewHolder mListVh;
    private ArrayObjectAdapter mRowsAdapter;
    private AdapterItemPresenter mAdapterItemPresenter;

    private ListRow mRow;

    /**
     * This type is used to test setItems() API.
     */
    private static class AdapterItem {
        private int mId;
        private String mStringMemberOne;

        // mStringMemberTwo is only used to test if correct payload can be generated.
        private String mStringMemberTwo;

        // not related string will not impact the result of our equals function.
        // Used to verify if payload computing process still honor the rule set by
        // areContentsTheSame() method
        private String mNotRelatedStringMember;

        AdapterItem(int id, String stringMemberOne) {
            mId = id;
            mStringMemberOne = stringMemberOne;
            mStringMemberTwo = "";
            mNotRelatedStringMember = "";
        }

        AdapterItem(int id, String stringMemberOne, String stringMemberTwo) {
            mId = id;
            mStringMemberOne = stringMemberOne;
            mStringMemberTwo = stringMemberTwo;
            mNotRelatedStringMember = "";
        }

        AdapterItem(int id, String stringMemberOne, String stringMemberTwo,
                String notRelatedStringMember) {
            mId = id;
            mStringMemberOne = stringMemberOne;
            mStringMemberTwo = stringMemberTwo;
            mNotRelatedStringMember = notRelatedStringMember;
        }

        public int getId() {
            return mId;
        }

        public String getStringMemberOne() {
            return mStringMemberOne;
        }

        public String getStringMemberTwo() {
            return mStringMemberTwo;
        }

        public String getNotRelatedStringMember() {
            return mNotRelatedStringMember;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AdapterItem that = (AdapterItem) o;

            if (mId != that.mId) return false;
            if (mStringMemberOne != null ? !mStringMemberOne.equals(that.mStringMemberOne)
                    : that.mStringMemberOne != null) {
                return false;
            }
            return mStringMemberTwo != null ? mStringMemberTwo.equals(that.mStringMemberTwo)
                    : that.mStringMemberTwo == null;
        }

        @Override
        public int hashCode() {
            int result = mId;
            result = 31 * result + (mStringMemberOne != null ? mStringMemberOne.hashCode() : 0);
            result = 31 * result + (mStringMemberTwo != null ? mStringMemberTwo.hashCode() : 0);
            return result;
        }
    }

    /**
     * Extend from DiffCallback extended class to define the rule to compare if two items are the
     * same/ have the same content and how to calculate the payload.
     *
     * The payload will only be calculated when the two items are the same but with different
     * contents. So we make this class as a public class which can be mocked by mockito to verify
     * if the calculation process satisfies our requirement.
     */
    public static class DiffCallbackPayloadTesting extends DiffCallback<AdapterItem> {
        // Using item's mId as the standard to judge if two items is the same
        @Override
        public boolean areItemsTheSame(AdapterItem oldItem, AdapterItem newItem) {
            return oldItem.getId() == newItem.getId();
        }

        // Using equals method to judge if two items have the same content.
        @Override
        public boolean areContentsTheSame(AdapterItem oldItem, AdapterItem newItem) {
            return oldItem.equals(newItem);
        }

        @Nullable
        @Override
        public Object getChangePayload(AdapterItem oldItem,
                AdapterItem newItem) {
            Bundle diff = new Bundle();
            if (oldItem.getId() != newItem.getId()) {
                diff.putInt(ID, newItem.getId());
            }

            if (!oldItem.getStringMemberOne().equals(newItem.getStringMemberOne())) {
                diff.putString(STRING_MEMBER_ONE, newItem.getStringMemberOne());
            }

            if (!oldItem.getStringMemberTwo().equals(newItem.getStringMemberTwo())) {
                diff.putString(STRING_MEMBER_TWO, newItem.getStringMemberTwo());
            }

            if (!oldItem.getNotRelatedStringMember().equals(newItem.getNotRelatedStringMember())) {
                diff.putString(NOT_RELATED_STRING_MEMBER, newItem.getNotRelatedStringMember());
            }

            if (diff.size() == 0) {
                return null;
            }
            return diff;
        }
    }

    /**
     * The presenter designed for adapter item.
     *
     * The reason to set this class as a public class is for Mockito to mock it. So we can observe
     * method's dispatching easily
     */
    public static class AdapterItemPresenter extends Presenter {
        int mWidth;
        int mHeight;

        AdapterItemPresenter() {
            this(100, 100);
        }

        AdapterItemPresenter(int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            View view = new View(parent.getContext());
            view.setFocusable(true);
            view.setId(R.id.lb_action_button);
            view.setLayoutParams(new ViewGroup.LayoutParams(mWidth, mHeight));
            return new Presenter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            // no - op
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
            // no - op
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item,
                List<Object> payloads) {
            // no - op
        }
    }


    /**
     * Initialize test-related members.
     */
    @Before
    public void setup() {
        mAdapter = new ArrayObjectAdapter();
        mBridgeAdapter = new ItemBridgeAdapter(mAdapter);
        mItems = new ArrayList();
        mMockedCallback = Mockito.spy(DiffCallbackPayloadTesting.class);

        // the diff callback without calculating the payload
        mCallbackWithoutPayload = new DiffCallback<AdapterItem>() {

            // Using item's mId as the standard to judge if two items is the same
            @Override
            public boolean areItemsTheSame(AdapterItem oldItem, AdapterItem newItem) {
                return oldItem.getId() == newItem.getId();
            }

            // Using equals method to judge if two items have the same content.
            @Override
            public boolean areContentsTheSame(AdapterItem oldItem, AdapterItem newItem) {
                return oldItem.equals(newItem);
            }
        };

        // Spy the RecyclerView.AdapterObserver
        mObserver = Mockito.spy(RecyclerView.AdapterDataObserver.class);

        // register observer so we can observe the events
        mBridgeAdapter.registerAdapterDataObserver(mObserver);

        // obtain context through instrumentation registry
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        //
        ListRowPresenter listRowPresenter = new ListRowPresenter();
        mListRowPresenter = Mockito.spy(listRowPresenter);

        // mock item presenter
        AdapterItemPresenter adapterItemPresenter = new AdapterItemPresenter();
        mAdapterItemPresenter = Mockito.spy(adapterItemPresenter);
        mRow = new ListRow(new ArrayObjectAdapter(mAdapterItemPresenter));
    }

    /**
     * The following test case is mainly focused on the basic functionality provided by
     * Object Adapter.
     *
     * The key purpose for this test is to make sure when adapter send out a signal through
     * notify function, it will finally be intercepted by recycler view's observer
     */
    @Test
    public void testBasicFunctionality() {
        mItems.add("a");
        mItems.add("b");
        mItems.add("c");
        mAdapter.addAll(0, mItems);

        // size
        assertEquals(mAdapter.size(), 3);

        // get
        assertEquals(mAdapter.get(0), "a");
        assertEquals(mAdapter.get(1), "b");
        assertEquals(mAdapter.get(2), "c");

        // indexOf
        assertEquals(mAdapter.indexOf("a"), 0);
        assertEquals(mAdapter.indexOf("b"), 1);
        assertEquals(mAdapter.indexOf("c"), 2);

        // insert
        mAdapter.add(1, "a1");
        Mockito.verify(mObserver).onItemRangeInserted(1, 1);
        assertAdapterContent(mAdapter, new Object[]{"a", "a1", "b", "c"});
        Mockito.reset(mObserver);

        // insert multiple
        ArrayList newItems1 = new ArrayList();
        newItems1.add("a2");
        newItems1.add("a3");
        mAdapter.addAll(1, newItems1);
        Mockito.verify(mObserver).onItemRangeInserted(1, 2);
        assertAdapterContent(mAdapter, new Object[]{"a", "a2", "a3", "a1", "b", "c"});
        Mockito.reset(mObserver);

        // update
        mAdapter.notifyArrayItemRangeChanged(2, 3);
        Mockito.verify(mObserver).onItemRangeChanged(2, 3);
        assertAdapterContent(mAdapter, new Object[]{"a", "a2", "a3", "a1", "b", "c"});
        Mockito.reset(mObserver);

        // remove
        mAdapter.removeItems(1, 4);
        Mockito.verify(mObserver).onItemRangeRemoved(1, 4);
        assertAdapterContent(mAdapter, new Object[]{"a", "c"});
        Mockito.reset(mObserver);

        // move
        mAdapter.move(0, 1);
        Mockito.verify(mObserver).onItemRangeMoved(0, 1, 1);
        assertAdapterContent(mAdapter, new Object[]{"c", "a"});
        Mockito.reset(mObserver);

        // replace
        mAdapter.replace(0, "a");
        Mockito.verify(mObserver).onItemRangeChanged(0, 1);
        assertAdapterContent(mAdapter, new Object[]{"a", "a"});
        Mockito.reset(mObserver);
        mAdapter.replace(1, "b");
        Mockito.verify(mObserver).onItemRangeChanged(1, 1);
        assertAdapterContent(mAdapter, new Object[]{"a", "b"});
        Mockito.reset(mObserver);

        // remove multiple
        mItems.clear();
        mItems.add("a");
        mItems.add("b");
        mAdapter.addAll(0, mItems);
        mAdapter.removeItems(0, 2);
        Mockito.verify(mObserver).onItemRangeRemoved(0, 2);
        assertAdapterContent(mAdapter, new Object[]{"a", "b"});
        Mockito.reset(mObserver);

        // clear
        mAdapter.clear();
        Mockito.verify(mObserver).onItemRangeRemoved(0, 2);
        assertAdapterContent(mAdapter, new Object[]{});
        Mockito.reset(mObserver);

        // isImmediateNotifySupported
        assertTrue(mAdapter.isImmediateNotifySupported());
    }


    @Test
    public void testSetItemsNoDiffCallback() {
        mItems.add(new AdapterItem(1, "a"));
        mItems.add(new AdapterItem(2, "b"));
        mItems.add(new AdapterItem(3, "c"));
        mAdapter.setItems(mItems, null);
        Mockito.verify(mObserver, times(1)).onChanged();
        Mockito.verify(mObserver, never()).onItemRangeInserted(anyInt(), anyInt());
        Mockito.verify(mObserver, never()).onItemRangeRemoved(anyInt(), anyInt());
        Mockito.verify(mObserver, never()).onItemRangeMoved(anyInt(), anyInt(), anyInt());

        mItems.add(new AdapterItem(4, "a"));
        mItems.add(new AdapterItem(5, "b"));
        mItems.add(new AdapterItem(6, "c"));
        mAdapter.setItems(mItems, null);
        Mockito.verify(mObserver, times(2)).onChanged();
        Mockito.verify(mObserver, never()).onItemRangeInserted(anyInt(), anyInt());
        Mockito.verify(mObserver, never()).onItemRangeRemoved(anyInt(), anyInt());
        Mockito.verify(mObserver, never()).onItemRangeMoved(anyInt(), anyInt(), anyInt());
    }

    /**
     * The following test cases are mainly focused on the basic functionality provided by setItems
     * function
     *
     * It can be deemed as an extension to the previous test, and won't consider payload in this
     * test case.
     *
     * Test0 will treat all items as the same item with same content.
     */
    @Test
    public void testSetItemsMethod0() {
        mItems.add("a");
        mItems.add("b");
        mItems.add("c");

        DiffCallback<String> callback = new DiffCallback<String>() {

            // Always treat two items are the same.
            @Override
            public boolean areItemsTheSame(String oldItem, String newItem) {
                return true;
            }

            // Always treat two items have the same content.
            @Override
            public boolean areContentsTheSame(String oldItem, String newItem) {
                return true;
            }
        };

        mAdapter.setItems(mItems, callback);
        // verify method dispatching
        Mockito.verify(mObserver).onItemRangeInserted(0, 3);

        // Clear previous items and set a new list of items.
        mItems.clear();
        mItems.add("a");
        mItems.add("b");
        mItems.add("c");

        // reset mocked object before calling setItems method
        Mockito.reset(mObserver);
        mAdapter.setItems(mItems, callback);

        // verify method dispatching
        Mockito.verify(mObserver, never()).onItemRangeChanged(anyInt(), anyInt(), any());
        Mockito.verify(mObserver, never()).onItemRangeMoved(anyInt(), anyInt(), anyInt());
        Mockito.verify(mObserver, never()).onItemRangeRemoved(anyInt(), anyInt());
        Mockito.verify(mObserver, never()).onItemRangeInserted(anyInt(), anyInt());
        assertAdapterContent(mAdapter, new Object[]{"a", "b", "c"});
    }

    /**
     * Test1 will treat all items as the same item with same content.
     */
    @Test
    public void testSetItemsMethod1() {
        mItems.add("a");
        mItems.add("b");
        mItems.add("c");

        DiffCallback<String> callback = new DiffCallback<String>() {

            // Always treat two items are the different.
            @Override
            public boolean areItemsTheSame(String oldItem, String newItem) {
                return false;
            }

            // Always treat two items have the different content.
            @Override
            public boolean areContentsTheSame(String oldItem, String newItem) {
                return false;
            }
        };

        mAdapter.setItems(mItems, callback);
        // verify method dispatching
        Mockito.verify(mObserver).onItemRangeInserted(0, 3);

        // Clear previous items and set a new list of items.
        mItems.clear();
        mItems.add("a");
        mItems.add("b");
        mItems.add("c");

        // reset mocked object before calling setItems method
        Mockito.reset(mObserver);
        mAdapter.setItems(mItems, callback);

        // No change or move event should be fired under current callback.
        Mockito.verify(mObserver, never()).onItemRangeChanged(anyInt(), anyInt(), any());
        Mockito.verify(mObserver, never()).onItemRangeMoved(anyInt(), anyInt(), anyInt());
        Mockito.verify(mObserver).onItemRangeRemoved(0, 3);
        Mockito.verify(mObserver).onItemRangeInserted(0, 3);
        assertAdapterContent(mAdapter, new Object[]{"a", "b", "c"});
    }

    /**
     * Test2 will trigger notifyItemRangeChanged event
     */
    @Test
    public void testSetItemsMethod2() {
        // initial item list
        mItems.add(new AdapterItem(1, "a"));
        mItems.add(new AdapterItem(2, "b"));
        mItems.add(new AdapterItem(3, "c"));
        mAdapter.setItems(mItems, mCallbackWithoutPayload);

        // Clear previous items and set a new list of items.
        mItems.clear();
        mItems.add(new AdapterItem(1, "a"));
        mItems.add(new AdapterItem(2, "c"));
        mItems.add(new AdapterItem(3, "b"));

        // reset mocked object before calling setItems method
        Mockito.reset(mObserver);
        mAdapter.setItems(mItems, mCallbackWithoutPayload);

        // verify method dispatching
        Mockito.verify(mObserver).onItemRangeChanged(1, 2, null);
    }


    /**
     * Test3 will trigger notifyItemMoved event
     */
    @Test
    public void testSetItemsMethod3() {
        // initial item list
        mItems.add(new AdapterItem(1, "a"));
        mItems.add(new AdapterItem(2, "b"));
        mItems.add(new AdapterItem(3, "c"));
        mAdapter.setItems(mItems, mCallbackWithoutPayload);

        // Clear previous items and set a new list of items.
        mItems.clear();
        mItems.add(new AdapterItem(2, "b"));
        mItems.add(new AdapterItem(1, "a"));
        mItems.add(new AdapterItem(3, "c"));

        // reset mocked object before calling setItems method
        Mockito.reset(mObserver);
        mAdapter.setItems(mItems, mCallbackWithoutPayload);

        // verify method dispatching
        Mockito.verify(mObserver).onItemRangeMoved(1, 0, 1);
    }

    /**
     * Test4 will trigger notifyItemRangeRemoved event
     */
    @Test
    public void testSetItemsMethod4() {
        // initial item list
        mItems.add(new AdapterItem(1, "a"));
        mItems.add(new AdapterItem(2, "b"));
        mItems.add(new AdapterItem(3, "c"));
        mAdapter.setItems(mItems, mCallbackWithoutPayload);

        // Clear previous items and set a new list of items.
        mItems.clear();
        mItems.add(new AdapterItem(2, "b"));
        mItems.add(new AdapterItem(3, "c"));

        // reset mocked object before calling setItems method
        Mockito.reset(mObserver);
        mAdapter.setItems(mItems, mCallbackWithoutPayload);

        // verify method dispatching
        Mockito.verify(mObserver).onItemRangeRemoved(0, 1);
    }

    /**
     * Test5 will trigger notifyItemRangeInserted event
     */
    @Test
    public void testSetItemsMethod5() {
        // initial item list
        mItems.add(new AdapterItem(1, "a"));
        mItems.add(new AdapterItem(2, "b"));
        mItems.add(new AdapterItem(3, "c"));
        mAdapter.setItems(mItems, mCallbackWithoutPayload);

        // Clear previous items and set a new list of items.
        mItems.clear();
        mItems.add(new AdapterItem(1, "a"));
        mItems.add(new AdapterItem(2, "b"));
        mItems.add(new AdapterItem(3, "c"));
        mItems.add(new AdapterItem(4, "d"));

        // reset mocked object before calling setItems method
        Mockito.reset(mObserver);
        mAdapter.setItems(mItems, mCallbackWithoutPayload);

        // verify method dispatching
        Mockito.verify(mObserver).onItemRangeInserted(3, 1);
    }


    /**
     * Test6 will trigger notifyItemRangeInserted event and notifyItemRangeRemoved event
     * simultaneously
     */
    @Test
    public void testSetItemsMethod6() {
        // initial item list
        mItems.add(new AdapterItem(1, "a"));
        mItems.add(new AdapterItem(2, "b"));
        mItems.add(new AdapterItem(3, "c"));
        mAdapter.setItems(mItems, mCallbackWithoutPayload);

        // Clear previous items and set a new list of items.
        mItems.clear();
        mItems.add(new AdapterItem(2, "a"));
        mItems.add(new AdapterItem(2, "b"));
        mItems.add(new AdapterItem(3, "c"));

        // reset mocked object before calling setItems method
        Mockito.reset(mObserver);
        mAdapter.setItems(mItems, mCallbackWithoutPayload);

        // verify method dispatching
        Mockito.verify(mObserver).onItemRangeRemoved(0, 1);
        Mockito.verify(mObserver).onItemRangeInserted(0, 1);
    }

    /**
     * Test7 will trigger notifyItemRangeMoved and notifyItemRangeChanged event simultaneously
     */
    @Test
    public void testItemsMethod7() {
        // initial item list
        mItems.add(new AdapterItem(1, "a"));
        mItems.add(new AdapterItem(2, "b"));
        mItems.add(new AdapterItem(3, "c"));
        mAdapter.setItems(mItems, mCallbackWithoutPayload);

        // Clear previous items and set a new list of items.
        mItems.clear();
        mItems.add(new AdapterItem(1, "aa"));
        mItems.add(new AdapterItem(3, "c"));
        mItems.add(new AdapterItem(2, "b"));

        // reset mocked object before calling setItems method
        Mockito.reset(mObserver);
        mAdapter.setItems(mItems, mCallbackWithoutPayload);

        // verify method dispatching
        Mockito.verify(mObserver).onItemRangeChanged(0, 1, null);
        Mockito.verify(mObserver).onItemRangeMoved(2, 1, 1);
    }

    /**
     * Test8 will trigger multiple items insertion event
     */
    @Test
    public void testSetItemsMethod8() {

        // initial item list
        mAdapter.clear();
        mItems.add(new AdapterItem(0, "a"));
        mItems.add(new AdapterItem(1, "b"));
        mAdapter.clear();
        mAdapter.setItems(mItems, mCallbackWithoutPayload);

        // Clear previous items and set a new list of items.
        mItems.clear();
        mItems.add(new AdapterItem(0, "a"));
        mItems.add(new AdapterItem(1, "b"));
        mItems.add(new AdapterItem(2, "c"));
        mItems.add(new AdapterItem(3, "d"));

        // reset mocked object before calling setItems method
        Mockito.reset(mObserver);
        mAdapter.setItems(mItems, mCallbackWithoutPayload);

        // verify method dispatching
        Mockito.verify(mObserver).onItemRangeInserted(2, 2);
        Mockito.reset(mObserver);
    }


    /**
     * The following test cases are mainly focused on testing setItems method when we need to
     * calculate payload
     *
     * The payload should only be calculated when two items are same but with different contents.
     * I.e. the calculate payload method should only be executed when the previous condition is
     * satisfied. In this test case we use a mocked callback object to verify it and compare the
     * calculated payload with our expected payload.
     *
     * Test 0 will calculate the difference on string member one.
     */
    @Test
    public void testPayloadCalculation0() {
        AdapterItem oldItem = new AdapterItem(1, "a", "a");
        mItems.add(oldItem);
        mAdapter.setItems(mItems, mMockedCallback);

        // Create a new item list which contain a new AdapterItem object
        // test if payload is computed correctly by changing string member 1
        mItems.clear();
        AdapterItem newItem = new AdapterItem(1, "aa", "a");
        mItems.add(newItem);


        // reset mocked object before calling setItems method
        Mockito.reset(mObserver);
        Mockito.reset(mMockedCallback);
        mAdapter.setItems(mItems, mMockedCallback);

        // Create expected payload manually for verification
        Bundle expectedPayload0 = new Bundle();
        expectedPayload0.putString(STRING_MEMBER_ONE, newItem.getStringMemberOne());

        // make sure no other event will be triggered in current scenario
        Mockito.verify(mObserver, never()).onItemRangeInserted(anyInt(), anyInt());
        Mockito.verify(mObserver, never()).onItemRangeRemoved(anyInt(), anyInt());
        Mockito.verify(mObserver, never()).onItemRangeMoved(anyInt(), anyInt(), anyInt());

        // Check if getChangePayload is executed as we expected
        Mockito.verify(mObserver, never()).onItemRangeChanged(anyInt(), anyInt(), eq(null));
        Mockito.verify(mMockedCallback).getChangePayload(oldItem,
                newItem);

        // compare the two bundles by iterating each member
        Bundle calculatedBundle0 = (Bundle) mMockedCallback.getChangePayload(
                oldItem, newItem);
        compareTwoBundles(calculatedBundle0, expectedPayload0);

    }

    /**
     * Test 1 will calculate the difference on string member two.
     */
    @Test
    public void testPayloadComputation1() {
        AdapterItem oldItem = new AdapterItem(1, "a", "a");
        mItems.add(oldItem);
        mAdapter.setItems(mItems, mMockedCallback);

        // Create a new item list which contain a new AdapterItem object
        // test if payload is computed correctly by changing string member 2
        mItems.clear();
        AdapterItem newItem = new AdapterItem(1, "a", "aa");
        mItems.add(newItem);

        // reset mocked object before calling setItems method
        Mockito.reset(mObserver);
        Mockito.reset(mMockedCallback);
        mAdapter.setItems(mItems, mMockedCallback);

        // Create expected payload manually for verification
        Bundle expectedPayload0 = new Bundle();
        expectedPayload0.putString(STRING_MEMBER_TWO, newItem.getStringMemberTwo());

        // make sure no other event will be triggered in current scenario
        Mockito.verify(mObserver, never()).onItemRangeInserted(anyInt(), anyInt());
        Mockito.verify(mObserver, never()).onItemRangeRemoved(anyInt(), anyInt());
        Mockito.verify(mObserver, never()).onItemRangeMoved(anyInt(), anyInt(), anyInt());

        // Check if getChangePayload is executed as we expected
        Mockito.verify(mObserver, never()).onItemRangeChanged(anyInt(), anyInt(), eq(null));
        Mockito.verify(mMockedCallback).getChangePayload(oldItem,
                newItem);

        // compare the two bundles by iterating each member
        Bundle calculatedBundle0 = (Bundle) mMockedCallback.getChangePayload(
                oldItem, newItem);
        compareTwoBundles(calculatedBundle0, expectedPayload0);

    }

    /**
     * Test 1 will calculate the difference on string member one and string member two.
     */
    @Test
    public void testPayloadComputation2() {
        AdapterItem oldItem = new AdapterItem(1, "a", "a");
        mItems.add(oldItem);
        mAdapter.setItems(mItems, mMockedCallback);

        // Create a new item list which contain a new AdapterItem object
        // test if payload is computed correctly by changing string member 1 and string member 2
        mItems.clear();
        AdapterItem newItem = new AdapterItem(1, "aa", "aa");
        mItems.add(newItem);

        // reset mocked object before calling setItems method
        Mockito.reset(mObserver);
        Mockito.reset(mMockedCallback);
        mAdapter.setItems(mItems, mMockedCallback);

        // Create expected payload manually for verification
        Bundle expectedPayload0 = new Bundle();
        expectedPayload0.putString(STRING_MEMBER_ONE, newItem.getStringMemberOne());
        expectedPayload0.putString(STRING_MEMBER_TWO, newItem.getStringMemberTwo());

        // make sure no other event will be triggered in current scenario
        Mockito.verify(mObserver, never()).onItemRangeInserted(anyInt(), anyInt());
        Mockito.verify(mObserver, never()).onItemRangeRemoved(anyInt(), anyInt());
        Mockito.verify(mObserver, never()).onItemRangeMoved(anyInt(), anyInt(), anyInt());

        // Check if getChangePayload is executed as we expected
        Mockito.verify(mObserver, never()).onItemRangeChanged(anyInt(), anyInt(), eq(null));
        Mockito.verify(mMockedCallback).getChangePayload(oldItem,
                newItem);

        // compare the two bundles by iterating each member
        Bundle calculatedBundle0 = (Bundle) mMockedCallback.getChangePayload(
                oldItem, newItem);
        compareTwoBundles(calculatedBundle0, expectedPayload0);

    }

    /**
     * Test payload computation process under the condition when two items are not the same
     * based on areItemsTheSame function in DiffUtilCallback
     */
    @Test
    public void testPayloadComputationNewItem0() {
        AdapterItem oldItem = new AdapterItem(1, "a", "a");
        mItems.add(oldItem);
        mAdapter.setItems(mItems, mMockedCallback);

        // Create a new item list which contain a new AdapterItem object
        // The id of the new item is changed, and will be treated as a new item according to the
        // rule we set in the callback. This test case is to verify the getChangePayload
        // method still honor the standard we set up to judge new item
        mItems.clear();
        AdapterItem newItem = new AdapterItem(2, "a", "a");
        mItems.add(newItem);

        // reset mocked object before calling setItems method
        Mockito.reset(mObserver);
        Mockito.reset(mMockedCallback);
        mAdapter.setItems(mItems, mMockedCallback);

        // Make sure only remove/ insert event will be fired under this circumstance
        Mockito.verify(mObserver).onItemRangeRemoved(0, 1);
        Mockito.verify(mObserver).onItemRangeInserted(0, 1);
        Mockito.verify(mObserver, never()).onItemRangeChanged(anyInt(), anyInt(), any());
        Mockito.verify(mMockedCallback, never()).getChangePayload((AdapterItem) any(),
                (AdapterItem) any());

    }

    /**
     * Test payload computation process under the condition when two items are not the same
     * based on areItemsTheSame function in DiffUtilCallback
     *
     * But in test 1 we have changed string member one for sanity check.
     */
    @Test
    public void testPayloadComputationNewItem1() {
        AdapterItem oldItem = new AdapterItem(1, "a", "a");
        mItems.add(oldItem);
        mAdapter.setItems(mItems, mMockedCallback);

        // Create a new item list which contain a new AdapterItem object
        // The id of the new item is changed, and will be treated as a new item according to the
        // rule we set in the callback. This test case is to verify the getChangePayload
        // method still honor the standard we set up to judge new item
        mItems.clear();
        AdapterItem newItem = new AdapterItem(2, "aa", "a");
        mItems.add(newItem);

        // reset mocked object before calling setItems method
        Mockito.reset(mObserver);
        Mockito.reset(mMockedCallback);
        mAdapter.setItems(mItems, mMockedCallback);

        // Make sure only remove/ insert event will be fired under this circumstance
        Mockito.verify(mObserver).onItemRangeRemoved(0, 1);
        Mockito.verify(mObserver).onItemRangeInserted(0, 1);
        Mockito.verify(mObserver, never()).onItemRangeChanged(anyInt(), anyInt(), any());
        Mockito.verify(mMockedCallback, never()).getChangePayload((AdapterItem) any(),
                (AdapterItem) any());

    }

    /**
     * Test payload computation process under the condition when two items are not the same
     * based on areItemsTheSame function in DiffUtilCallback
     *
     * But in test 2 we have changed string member two for sanity check.
     */
    @Test
    public void testPayloadComputationNewItem2() {
        AdapterItem oldItem = new AdapterItem(1, "a", "a");
        mItems.add(oldItem);
        mAdapter.setItems(mItems, mMockedCallback);

        // Create a new item list which contain a new AdapterItem object
        // The id of the new item is changed, and will be treated as a new item according to the
        // rule we set in the callback. This test case is to verify the getChangePayload
        // method still honor the standard we set up to judge new item
        mItems.clear();
        AdapterItem newItem = new AdapterItem(2, "a", "aa");
        mItems.add(newItem);

        // reset mocked object before calling setItems method
        Mockito.reset(mObserver);
        Mockito.reset(mMockedCallback);
        mAdapter.setItems(mItems, mMockedCallback);

        // Make sure only remove/ insert event will be fired under this circumstance
        Mockito.verify(mObserver).onItemRangeRemoved(0, 1);
        Mockito.verify(mObserver).onItemRangeInserted(0, 1);
        Mockito.verify(mObserver, never()).onItemRangeChanged(anyInt(), anyInt(), any());
        Mockito.verify(mMockedCallback, never()).getChangePayload((AdapterItem) any(),
                (AdapterItem) any());

    }

    /**
     * Test payload computation process under the condition when two items are not the same
     * based on areItemsTheSame function in DiffUtilCallback
     *
     * But in test 3 we have changed string member one and string member two for sanity check.
     */
    @Test
    public void testPayloadComputationNewItem3() {
        AdapterItem oldItem = new AdapterItem(1, "a", "a");
        mItems.add(oldItem);
        mAdapter.setItems(mItems, mMockedCallback);

        // Create a new item list which contain a new AdapterItem object
        // The id of the new item is changed, and will be treated as a new item according to the
        // rule we set in the callback. This test case is to verify the getChangePayload
        // method still honor the standard we set up to judge new item
        mItems.clear();
        AdapterItem newItem = new AdapterItem(2, "aa", "aa");
        mItems.add(newItem);

        // reset mocked object before calling setItems method
        Mockito.reset(mObserver);
        Mockito.reset(mMockedCallback);
        mAdapter.setItems(mItems, mMockedCallback);

        // Make sure only remove/ insert event will be fired under this circumstance
        Mockito.verify(mObserver).onItemRangeRemoved(0, 1);
        Mockito.verify(mObserver).onItemRangeInserted(0, 1);
        Mockito.verify(mObserver, never()).onItemRangeChanged(anyInt(), anyInt(), any());
        Mockito.verify(mMockedCallback, never()).getChangePayload((AdapterItem) any(),
                (AdapterItem) any());
    }

    /**
     * Test payload computation process under the condition when two items have the same content
     * based on areContentsTheSame function in DiffUtilCallback
     */
    @Test
    public void testPayloadComputationSameContent() {
        AdapterItem oldItem = new AdapterItem(1, "a", "a", "a");
        mItems.add(oldItem);
        mAdapter.setItems(mItems, mMockedCallback);

        // Create a new item list which contain a new AdapterItem object
        // The non-related string member of the new item is changed, but the two items are still
        // the same as well as the item's content according to the rule we set in the callback.
        // This test case is to verify the getChangePayload method still honor the standard
        // we set up to determine if a new object is 1. a new item 2. has the same content as the
        // previous one
        mItems.clear();
        AdapterItem newItem = new AdapterItem(1, "a", "a", "aa");
        mItems.add(newItem);

        // reset mocked object before calling setItems method
        Mockito.reset(mObserver);
        Mockito.reset(mMockedCallback);
        mAdapter.setItems(mItems, mMockedCallback);

        // Make sure no even will be fired up in this circumstance
        Mockito.verify(mObserver, never()).onItemRangeRemoved(anyInt(), anyInt());
        Mockito.verify(mObserver, never()).onItemRangeInserted(anyInt(), anyInt());
        Mockito.verify(mObserver, never()).onItemRangeChanged(anyInt(), anyInt(), any());
        Mockito.verify(mMockedCallback, never()).getChangePayload((AdapterItem) any(),
                (AdapterItem) any());
    }


    /**
     * This test case is targeted at real ui testing. I.e. making sure when the change of adapter's
     * items will trigger the rebinding of view holder with payload. That's item presenter's
     * onBindViewHolder method with payload supporting.
     *
     */
    @Test
    public void testPresenterAndItemBridgeAdapter() {
        // data set one
        final List<AdapterItem> dataSetOne = new ArrayList<>();
        AdapterItem dataSetOne0 = new AdapterItem(1, "a");
        AdapterItem dataSetOne1 = new AdapterItem(2, "b");
        AdapterItem dataSetOne2 = new AdapterItem(3, "c");
        AdapterItem dataSetOne3 = new AdapterItem(4, "d");
        AdapterItem dataSetOne4 = new AdapterItem(5, "3");
        dataSetOne.add(dataSetOne0);
        dataSetOne.add(dataSetOne1);
        dataSetOne.add(dataSetOne2);
        dataSetOne.add(dataSetOne3);
        dataSetOne.add(dataSetOne4);

        // data set two
        final List<AdapterItem> dataSetTwo = new ArrayList<>();
        AdapterItem dataSetTwo0 = new AdapterItem(1, "aa");
        AdapterItem dataSetTwo1 = new AdapterItem(2, "bb");
        AdapterItem dataSetTwo2 = new AdapterItem(3, "cc");
        AdapterItem dataSetTwo3 = new AdapterItem(4, "dd");
        AdapterItem dataSetTwo4 = new AdapterItem(5, "ee");
        dataSetTwo.add(dataSetTwo0);
        dataSetTwo.add(dataSetTwo1);
        dataSetTwo.add(dataSetTwo2);
        dataSetTwo.add(dataSetTwo3);
        dataSetTwo.add(dataSetTwo4);

        ((ArrayObjectAdapter) mRow.getAdapter()).addAll(0, dataSetOne);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {

                // obtain frame layout through context.
                final ViewGroup parent = new FrameLayout(mContext);

                // create view holder and obtain the view object from view holder
                // add view object to our layout
                Presenter.ViewHolder containerVh = mListRowPresenter.onCreateViewHolder(parent);
                parent.addView(containerVh.view, 1000, 1000);

                // set rows adapter and add row to that adapter
                mRowsAdapter = new ArrayObjectAdapter();
                mRowsAdapter.add(mRow);

                // use the presenter to bind row view holder explicitly. So the itemBridgeAdapter
                // will be connected to the adapter inside of the listRow successfully.
                mListVh = (ListRowPresenter.ViewHolder) mListRowPresenter.getRowViewHolder(
                        containerVh);
                mListRowPresenter.onBindRowViewHolder(mListVh, mRow);

                // layout the list row in recycler view
                runRecyclerViewLayout();

                // reset mocked presenter
                Mockito.reset(mListRowPresenter);
                Mockito.reset(mAdapterItemPresenter);

                // calling setItem's method to trigger the diff computation
                ((ArrayObjectAdapter) mRow.getAdapter()).setItems(dataSetTwo,
                        new DiffCallbackPayloadTesting());

                // re-layout the recycler view to trigger getViewForPosition event
                runRecyclerViewLayout();

                // verify method execution
                Mockito.verify(mAdapterItemPresenter, never()).onBindViewHolder(
                        (RowPresenter.ViewHolder) any(), (Object) any());
                Mockito.verify(mAdapterItemPresenter, atLeast(5)).onBindViewHolder(
                        (RowPresenter.ViewHolder) any(), (Object) any(), (List<Object>) any());
            }
        });
    }

    /**
     * Helper function to layout recycler view
     * So the recycler view will execute the getView() method then the onBindViewHolder() method
     * from presenter will be executed
     */
    private void runRecyclerViewLayout() {
        mListVh.view.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        mListVh.view.layout(0, 0, 1000, 1000);
    }

    /**
     * Helper function to compare two bundles through iterating the fields.
     *
     * @param bundle1 bundle 1
     * @param bundle2 bundle 2
     */
    private void compareTwoBundles(Bundle bundle1, Bundle bundle2) {
        assertEquals(bundle1.getInt(ID), bundle2.getInt(ID));
        assertEquals(bundle1.getString(STRING_MEMBER_ONE), bundle2.getString(
                STRING_MEMBER_ONE));
        assertEquals(bundle1.getString(STRING_MEMBER_TWO), bundle2.getString(
                STRING_MEMBER_TWO));
        assertEquals(bundle1.getString(NOT_RELATED_STRING_MEMBER),
                bundle2.getString(NOT_RELATED_STRING_MEMBER));
    }

    /**
     * Helper function to test the content in adapter
     */
    private static void assertAdapterContent(ObjectAdapter adapter, Object[] data) {
        assertEquals(adapter.size(), data.length);
        for (int i = 0; i < adapter.size(); i++) {
            assertEquals(adapter.get(i), data[i]);
        }
    }
}
