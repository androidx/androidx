@file:Suppress("DEPRECATION")

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager

open class FragmentPagerActivity : FragmentActivity() {
    lateinit var pager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pager = ViewPager(this).apply {
            id = 2
            adapter = MyAdapter(supportFragmentManager)
        }

        setContentView(
            LinearLayout(this).apply {
                addView(pager)
            }
        )
    }

    fun next() {
        pager.currentItem = pager.currentItem + 1
    }

    class MyAdapter(fm: FragmentManager) :
        FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getCount(): Int {
            return NUM_ITEMS
        }

        override fun getItem(position: Int): Fragment {
            return ViewFragment()
        }
    }

    class ViewFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return TextView(activity)
        }
    }

    companion object {
        internal const val NUM_ITEMS = 5
    }
}

class FragmentStatePagerActivity : FragmentPagerActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pager.adapter = MyAdapter(supportFragmentManager)
    }

    class MyAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getCount(): Int {
            return NUM_ITEMS
        }

        override fun getItem(position: Int): Fragment {
            return ViewFragment()
        }
    }
}
