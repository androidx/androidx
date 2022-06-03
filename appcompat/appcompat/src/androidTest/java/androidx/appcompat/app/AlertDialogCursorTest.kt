/*
 * Copyright (C) 2015 The Android Open Source Project
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
@file:Suppress("DEPRECATION")

package androidx.appcompat.app

import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.database.Cursor
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteDatabase
import android.view.View
import android.widget.Button
import android.widget.CheckedTextView
import android.widget.ListView
import androidx.appcompat.test.R
import androidx.appcompat.testutils.TestUtilsMatchers
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import java.io.File
import org.hamcrest.Matchers
import org.hamcrest.core.AllOf
import org.hamcrest.core.Is
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

@LargeTest
@RunWith(AndroidJUnit4::class)
class AlertDialogCursorTest {
    @Rule
    val mActivityTestRule: ActivityTestRule<AlertDialogTestActivity> = ActivityTestRule(
        AlertDialogTestActivity::class.java
    )

    private var mButton: Button? = null
    private var mTextContent: Array<String> = emptyArray()
    private var mCheckedContent: BooleanArray = booleanArrayOf()
    private var mProjectionWithChecked: Array<String> = emptyArray()
    private var mProjectionWithoutChecked: Array<String> = emptyArray()
    private var mDatabase: SQLiteDatabase? = null
    private var mDatabaseFile: File? = null
    private var mCursor: Cursor? = null
    private var mAlertDialog: AlertDialog? = null

    @Before
    fun setUp() {
        // Ideally these constant arrays would be defined as final static fields on the
        // class level, but for some reason those get reset to null on v9- devices after
        // the first test method has been executed.
        mTextContent = arrayOf("Adele", "Beyonce", "Ciara", "Dido")
        mCheckedContent = booleanArrayOf(false, false, true, false)
        mProjectionWithChecked = arrayOf(
            "_id", // 0
            TEXT_COLUMN_NAME, // 1
            CHECKED_COLUMN_NAME // 2
        )
        mProjectionWithoutChecked = arrayOf(
            "_id", // 0
            TEXT_COLUMN_NAME // 1
        )

        val activity = mActivityTestRule.activity
        mButton = activity.findViewById<View>(R.id.test_button) as Button

        val dbDir = activity.getDir("tests", Context.MODE_PRIVATE)
        mDatabaseFile = File(dbDir, "database_alert_dialog_test.db")
        if (mDatabaseFile!!.exists()) {
            mDatabaseFile!!.delete()
        }

        mDatabase = SQLiteDatabase.openOrCreateDatabase(mDatabaseFile!!.path, null)
        Assert.assertNotNull(mDatabase)
        // Create and populate a test table
        mDatabase!!.execSQL(
            "CREATE TABLE test (_id INTEGER PRIMARY KEY, " + TEXT_COLUMN_NAME +
                " TEXT, " + CHECKED_COLUMN_NAME + " INTEGER);"
        )
        for (i in mTextContent.indices) {
            mDatabase!!.execSQL(
                "INSERT INTO test (" + TEXT_COLUMN_NAME + ", " +
                    CHECKED_COLUMN_NAME + ") VALUES ('" + mTextContent[i] + "', " +
                    (if (mCheckedContent[i]) "1" else "0") + ");"
            )
        }
    }

    @After
    @Throws(Throwable::class)
    fun tearDown() {
        mCursor?.let { cursor ->
            // Close the cursor on the UI thread as the list view in the alert dialog
            // will get notified of any change to the underlying cursor.
            mActivityTestRule.runOnUiThread {
                cursor.close()
            }
        }
        mDatabase?.close()
        mDatabaseFile?.delete()
        mAlertDialog?.dismiss()
    }

    private fun wireBuilder(builder: AlertDialog.Builder) {
        mButton!!.setOnClickListener { mAlertDialog = builder.show() }
    }

    private fun verifySimpleItemsContent(
        expectedContent: Array<String>,
        onClickListener: DialogInterface.OnClickListener
    ) {
        val expectedCount = expectedContent.size
        Espresso.onView(ViewMatchers.withId(R.id.test_button)).perform(ViewActions.click())
        val listView = mAlertDialog!!.listView
        Assert.assertNotNull("List view is shown", listView)
        val listAdapter = listView.adapter
        Assert.assertEquals(
            "List has $expectedCount entries",
            expectedCount.toLong(), listAdapter.count.toLong()
        )

        // Test that all items are showing
        Espresso.onView(ViewMatchers.withText("Dialog title")).inRoot(RootMatchers.isDialog())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        for (s in expectedContent) {
            val rowInteraction = Espresso.onData(
                AllOf.allOf(
                    Is.`is`(
                        Matchers.instanceOf(
                            SQLiteCursor::class.java
                        )
                    ),
                    TestUtilsMatchers.withCursorItemContent(TEXT_COLUMN_NAME, s)
                )
            )
            rowInteraction.inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        }

        // Verify that our click listener hasn't been called yet
        Mockito.verify(onClickListener, Mockito.never()).onClick(
            ArgumentMatchers.any(
                DialogInterface::class.java
            ), ArgumentMatchers.any(Int::class.javaPrimitiveType)
        )
        // Test that a click on an item invokes the registered listener
        val indexToClick = expectedCount - 2
        val interactionForClick = Espresso.onData(
            AllOf.allOf(
                Is.`is`(
                    Matchers.instanceOf(
                        SQLiteCursor::class.java
                    )
                ),
                TestUtilsMatchers.withCursorItemContent(
                    TEXT_COLUMN_NAME, expectedContent[indexToClick]
                )
            )
        )
        interactionForClick.inRoot(RootMatchers.isDialog()).perform(ViewActions.click())
        Mockito.verify(onClickListener, Mockito.times(1)).onClick(mAlertDialog, indexToClick)
    }

    @Test
    fun testSimpleItemsFromCursor() {
        mCursor = mDatabase!!.query(
            "test", mProjectionWithoutChecked,
            null, null, null, null, null
        )
        Assert.assertNotNull(mCursor)
        val mockClickListener = Mockito.mock(
            DialogInterface.OnClickListener::class.java
        )
        val builder = AlertDialog.Builder(mActivityTestRule.activity)
            .setTitle(R.string.alert_dialog_title)
            .setCursor(mCursor, mockClickListener, "text")
        wireBuilder(builder)
        verifySimpleItemsContent(mTextContent, mockClickListener)
    }

    /**
     * Helper method to verify the state of the multi-choice items list. It gets the String
     * array of content and verifies that:
     *
     * 1. The items in the array are rendered as CheckedTextViews inside a ListView
     * 2. Each item in the array is displayed
     * 3. Checked state of each row in the ListView corresponds to the matching entry in the
     * passed boolean array
     */
    private fun verifyMultiChoiceItemsState(
        expectedContent: Array<String>,
        checkedTracker: BooleanArray
    ) {
        val expectedCount = expectedContent.size
        val listView = mAlertDialog!!.listView
        Assert.assertNotNull("List view is shown", listView)
        val listAdapter = listView.adapter
        Assert.assertEquals(
            "List has $expectedCount entries",
            expectedCount.toLong(), listAdapter.count.toLong()
        )
        for (i in 0 until expectedCount) {
            val checkedStateMatcher = if (checkedTracker[i])
                TestUtilsMatchers.isCheckedTextView() else TestUtilsMatchers.isNonCheckedTextView()
            // Check that the corresponding row is rendered as CheckedTextView with expected
            // checked state.
            val rowInteraction = Espresso.onData(
                AllOf.allOf(
                    Is.`is`(
                        Matchers.instanceOf(
                            SQLiteCursor::class.java
                        )
                    ),
                    TestUtilsMatchers.withCursorItemContent(TEXT_COLUMN_NAME, expectedContent[i])
                )
            )
            rowInteraction.inRoot(RootMatchers.isDialog()).check(
                ViewAssertions.matches(
                    AllOf.allOf(
                        ViewMatchers.isDisplayed(),
                        ViewMatchers.isAssignableFrom(CheckedTextView::class.java),
                        ViewMatchers.isDescendantOfA(
                            ViewMatchers.isAssignableFrom(ListView::class.java)
                        ),
                        checkedStateMatcher
                    )
                )
            )
        }
    }

    private fun verifyMultiChoiceItemsContent(
        expectedContent: Array<String>,
        checkedTracker: BooleanArray
    ) {
        val expectedCount = expectedContent.size
        Espresso.onView(ViewMatchers.withId(R.id.test_button)).perform(ViewActions.click())
        val listView = mAlertDialog!!.listView
        Assert.assertNotNull("List view is shown", listView)
        val listAdapter = listView.adapter
        Assert.assertEquals(
            "List has $expectedCount entries",
            expectedCount.toLong(), listAdapter.count.toLong()
        )

        // Test that all items are showing
        Espresso.onView(ViewMatchers.withText("Dialog title")).inRoot(RootMatchers.isDialog())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        verifyMultiChoiceItemsState(expectedContent, checkedTracker)

        // We're going to click item #1 and test that the click listener has been invoked to
        // update the original state array
        val expectedAfterClick1 = checkedTracker.clone()
        expectedAfterClick1[1] = !expectedAfterClick1[1]
        var interactionForClick = Espresso.onData(
            AllOf.allOf(
                Is.`is`(
                    Matchers.instanceOf(
                        SQLiteCursor::class.java
                    )
                ),
                TestUtilsMatchers.withCursorItemContent(TEXT_COLUMN_NAME, expectedContent[1])
            )
        )
        interactionForClick.inRoot(RootMatchers.isDialog()).perform(ViewActions.click())
        verifyMultiChoiceItemsState(expectedContent, expectedAfterClick1)

        // Now click item #1 again and test that the click listener has been invoked to update the
        // original state array again
        expectedAfterClick1[1] = !expectedAfterClick1[1]
        interactionForClick.inRoot(RootMatchers.isDialog()).perform(ViewActions.click())
        verifyMultiChoiceItemsState(expectedContent, expectedAfterClick1)

        // Now we're going to click the last item and test that the click listener has been invoked
        // to update the original state array
        val expectedAfterClickLast = checkedTracker.clone()
        expectedAfterClickLast[expectedCount - 1] = !expectedAfterClickLast[expectedCount - 1]
        interactionForClick = Espresso.onData(
            AllOf.allOf(
                Is.`is`(
                    Matchers.instanceOf(
                        SQLiteCursor::class.java
                    )
                ),
                TestUtilsMatchers.withCursorItemContent(
                    TEXT_COLUMN_NAME,
                    expectedContent[expectedCount - 1]
                )
            )
        )
        interactionForClick.inRoot(RootMatchers.isDialog()).perform(ViewActions.click())
        verifyMultiChoiceItemsState(expectedContent, expectedAfterClickLast)
    }

    @LargeTest
    @Test
    fun testMultiChoiceItemsFromCursor() {
        mCursor = mDatabase!!.query(
            "test", mProjectionWithChecked,
            null, null, null, null, null
        )
        Assert.assertNotNull(mCursor)
        val checkedTracker = mCheckedContent.clone()
        val builder = AlertDialog.Builder(mActivityTestRule.activity)
            .setTitle(R.string.alert_dialog_title)
            .setMultiChoiceItems(
                mCursor, CHECKED_COLUMN_NAME, TEXT_COLUMN_NAME
            ) { _: DialogInterface?, which: Int, isChecked: Boolean ->
                // Update the underlying database with the new checked
                // state for the specific row
                mCursor!!.moveToPosition(which)
                val valuesToUpdate = ContentValues()
                valuesToUpdate.put(CHECKED_COLUMN_NAME, if (isChecked) 1 else 0)
                mDatabase!!.update(
                    "test", valuesToUpdate,
                    "$TEXT_COLUMN_NAME = ?", arrayOf(mCursor!!.getString(1))
                )
                mCursor!!.requery()
                checkedTracker[which] = isChecked
            }
        wireBuilder(builder)

        // Pass the same boolean[] array as used for initialization since our click listener
        // will be updating its content.
        verifyMultiChoiceItemsContent(mTextContent, checkedTracker)
    }

    /**
     * Helper method to verify the state of the single-choice items list. It gets the String
     * array of content and verifies that:
     *
     * 1. The items in the array are rendered as CheckedTextViews inside a ListView
     * 2. Each item in the array is displayed
     * 3. Only one row in the ListView is checked, and that corresponds to the passed
     * integer index.
     */
    private fun verifySingleChoiceItemsState(
        expectedContent: Array<String>,
        currentlyExpectedSelectionIndex: Int
    ) {
        val expectedCount = expectedContent.size
        val listView = mAlertDialog!!.listView
        Assert.assertNotNull("List view is shown", listView)
        val listAdapter = listView.adapter
        Assert.assertEquals(
            "List has $expectedCount entries",
            expectedCount.toLong(), listAdapter.count.toLong()
        )
        for (i in 0 until expectedCount) {
            val checkedStateMatcher = if (i == currentlyExpectedSelectionIndex)
                TestUtilsMatchers.isCheckedTextView() else TestUtilsMatchers.isNonCheckedTextView()
            // Check that the corresponding row is rendered as CheckedTextView with expected
            // checked state.
            val rowInteraction = Espresso.onData(
                AllOf.allOf(
                    Is.`is`(
                        Matchers.instanceOf(
                            SQLiteCursor::class.java
                        )
                    ),
                    TestUtilsMatchers.withCursorItemContent(TEXT_COLUMN_NAME, expectedContent[i])
                )
            )
            rowInteraction.inRoot(RootMatchers.isDialog()).check(
                ViewAssertions.matches(
                    AllOf.allOf(
                        ViewMatchers.isDisplayed(),
                        ViewMatchers.isAssignableFrom(CheckedTextView::class.java),
                        ViewMatchers.isDescendantOfA(
                            ViewMatchers.isAssignableFrom(ListView::class.java)
                        ),
                        checkedStateMatcher
                    )
                )
            )
        }
    }

    private fun verifySingleChoiceItemsContent(
        expectedContent: Array<String>,
        @Suppress("SameParameterValue") initialSelectionIndex: Int,
        onClickListener: DialogInterface.OnClickListener
    ) {
        val expectedCount = expectedContent.size
        var currentlyExpectedSelectionIndex = initialSelectionIndex
        Espresso.onView(ViewMatchers.withId(R.id.test_button)).perform(ViewActions.click())

        // Test that all items are showing
        Espresso.onView(ViewMatchers.withText("Dialog title")).inRoot(RootMatchers.isDialog())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        verifySingleChoiceItemsState(expectedContent, currentlyExpectedSelectionIndex)

        // We're going to click the first unselected item and test that the click listener has
        // been invoked.
        currentlyExpectedSelectionIndex = if (currentlyExpectedSelectionIndex == 0) 1 else 0
        var interactionForClick = Espresso.onData(
            AllOf.allOf(
                Is.`is`(
                    Matchers.instanceOf(
                        SQLiteCursor::class.java
                    )
                ),
                TestUtilsMatchers.withCursorItemContent(
                    TEXT_COLUMN_NAME,
                    expectedContent[currentlyExpectedSelectionIndex]
                )
            )
        )
        interactionForClick.inRoot(RootMatchers.isDialog()).perform(ViewActions.click())
        Mockito.verify(onClickListener, Mockito.times(1))
            .onClick(mAlertDialog, currentlyExpectedSelectionIndex)
        verifySingleChoiceItemsState(expectedContent, currentlyExpectedSelectionIndex)

        // Now click the same item again and test that the selection has not changed
        interactionForClick.inRoot(RootMatchers.isDialog()).perform(ViewActions.click())
        Mockito.verify(onClickListener, Mockito.times(2))
            .onClick(mAlertDialog, currentlyExpectedSelectionIndex)
        verifySingleChoiceItemsState(expectedContent, currentlyExpectedSelectionIndex)

        // Now we're going to click the last item and test that the click listener has been invoked
        // to update the original state array
        currentlyExpectedSelectionIndex = expectedCount - 1
        interactionForClick = Espresso.onData(
            AllOf.allOf(
                Is.`is`(
                    Matchers.instanceOf(
                        SQLiteCursor::class.java
                    )
                ),
                TestUtilsMatchers.withCursorItemContent(
                    TEXT_COLUMN_NAME,
                    expectedContent[currentlyExpectedSelectionIndex]
                )
            )
        )
        interactionForClick.inRoot(RootMatchers.isDialog()).perform(ViewActions.click())
        Mockito.verify(onClickListener, Mockito.times(1))
            .onClick(mAlertDialog, currentlyExpectedSelectionIndex)
        verifySingleChoiceItemsState(expectedContent, currentlyExpectedSelectionIndex)
    }

    @LargeTest
    @Test
    fun testSingleChoiceItemsFromCursor() {
        mCursor = mDatabase!!.query(
            "test", mProjectionWithoutChecked,
            null, null, null, null, null
        )
        Assert.assertNotNull(mCursor)
        val mockClickListener = Mockito.mock(
            DialogInterface.OnClickListener::class.java
        )
        val builder = AlertDialog.Builder(mActivityTestRule.activity)
            .setTitle(R.string.alert_dialog_title)
            .setSingleChoiceItems(mCursor, 2, TEXT_COLUMN_NAME, mockClickListener)
        wireBuilder(builder)
        verifySingleChoiceItemsContent(mTextContent, 2, mockClickListener)
    }

    companion object {
        private const val TEXT_COLUMN_NAME = "text"
        private const val CHECKED_COLUMN_NAME = "checked"
    }
}
