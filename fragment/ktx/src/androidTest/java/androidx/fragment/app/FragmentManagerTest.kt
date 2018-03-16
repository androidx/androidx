package androidx.fragment.app

import android.support.test.annotation.UiThreadTest
import android.support.test.filters.MediumTest
import android.support.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@MediumTest
class FragmentManagerTest {
    @get:Rule val activityRule = ActivityTestRule<TestActivity>(TestActivity::class.java)
    private val fragmentManager get() = activityRule.activity.supportFragmentManager

    @UiThreadTest
    @Test fun transaction() {
        val fragment = TestFragment()
        fragmentManager.transaction {
            add(fragment, null)
        }
        assertThat(fragmentManager.fragments).doesNotContain(fragment)
        fragmentManager.executePendingTransactions()
        assertThat(fragmentManager.fragments).contains(fragment)
    }

    @UiThreadTest
    @Test fun transactionNow() {
        val fragment = TestFragment()
        fragmentManager.transaction(now = true) {
            add(fragment, null)
        }
        assertThat(fragmentManager.fragments).contains(fragment)
    }

    @UiThreadTest
    @Test fun transactionAllowingStateLoss() {
        // Use a detached FragmentManager to ensure state loss.
        val fragmentManager = FragmentManagerImpl()

        fragmentManager.transaction(allowStateLoss = true) {
            add(TestFragment(), null)
        }
        assertThat(fragmentManager.fragments).isEmpty()
    }

    @UiThreadTest
    @Test fun transactionNowAllowingStateLoss() {
        // Use a detached FragmentManager to ensure state loss.
        val fragmentManager = FragmentManagerImpl()

        fragmentManager.transaction(now = true, allowStateLoss = true) {
            add(TestFragment(), null)
        }
        assertThat(fragmentManager.fragments).isEmpty()
    }
}

class TestActivity : FragmentActivity()
class TestFragment : Fragment()
