/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.wear.tiles.demos

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.wear.tiles.manager.TileUiClient
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.adapter.FragmentStateAdapter

private const val NUM_PAGES = 4
private val TILE_PROVIDERS_NAME = arrayOf(
    HelloTileService::class.java,
    CalendarTileService::class.java,
    CountTileService::class.java,
    CurvedLayoutTileService::class.java
)

class TilePageFragment(
    private val activityContext: Context,
    private val position: Int
) : Fragment() {
    lateinit var tileUiClient: TileUiClient

    override fun onCreateView(
        inflator: LayoutInflater,
        container: ViewGroup?,
        savedInstanceBundle: Bundle?
    ): View = inflator.inflate(R.layout.fragment_page, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rootLayout = requireView().findViewById<FrameLayout>(R.id.tile_container)

        tileUiClient = TileUiClient(
            context = activityContext,
            component = ComponentName(activityContext, TILE_PROVIDERS_NAME[position]),
            parentView = rootLayout
        )
        tileUiClient.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        tileUiClient.close()
    }
}

class TilePreviewActivity : FragmentActivity() {

    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceBundle: Bundle?) {
        super.onCreate(savedInstanceBundle)
        setContentView(R.layout.activity_main)
        viewPager = findViewById(R.id.carousel)

        val pagerAdapter = TilePagerAdaptor(this)
        viewPager.adapter = pagerAdapter
    }

    private inner class TilePagerAdaptor(
        private val fa: FragmentActivity
    ) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = NUM_PAGES
        override fun createFragment(position: Int): Fragment = TilePageFragment(fa, position)
    }
}