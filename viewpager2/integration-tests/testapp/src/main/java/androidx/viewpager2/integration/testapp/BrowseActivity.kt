/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.viewpager2.integration.testapp

import android.app.ListActivity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.SimpleAdapter

/**
 * This activity lists all the activities in this application.
 */
class BrowseActivity : ListActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listAdapter = SimpleAdapter(this, getData(),
                android.R.layout.simple_list_item_1, arrayOf("title"),
                intArrayOf(android.R.id.text1))
    }

    private fun getData(): List<Map<String, Any>> {
        val myData = mutableListOf<Map<String, Any>>()

        myData.add(mapOf("title" to "ViewPager2 with Views",
                "intent" to activityToIntent(CardViewActivity::class.java.name)))
        myData.add(mapOf("title" to "ViewPager2 with Fragments",
                "intent" to activityToIntent(CardFragmentActivity::class.java.name)))
        myData.add(mapOf("title" to "ViewPager2 with a Mutable Collection (Views)",
                "intent" to activityToIntent(MutableCollectionViewActivity::class.java.name)))
        myData.add(mapOf("title" to "ViewPager2 with a Mutable Collection (Fragments)",
                "intent" to activityToIntent(MutableCollectionFragmentActivity::class.java.name)))
        myData.add(mapOf("title" to "ViewPager2 with a TabLayout (Views)",
                "intent" to activityToIntent(CardViewTabLayoutActivity::class.java.name)))
        myData.add(mapOf("title" to "ViewPager2 with Fake Dragging",
                "intent" to activityToIntent(FakeDragActivity::class.java.name)))

        return myData
    }

    private fun activityToIntent(activity: String): Intent =
            Intent(Intent.ACTION_VIEW).setClassName(this.packageName, activity)

    override fun onListItemClick(listView: ListView, view: View, position: Int, id: Long) {
        val map = listView.getItemAtPosition(position) as Map<*, *>

        val intent = Intent(map["intent"] as Intent)
        intent.addCategory(Intent.CATEGORY_SAMPLE_CODE)
        startActivity(intent)
    }
}
