package androidx.fragment.app

import androidx.test.annotation.UiThreadTest
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@SmallTest
@Suppress("DEPRECATION")
class FragmentManagerTest {
    @get:Rule val activityRule = androidx.test.rule.ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
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

    @UiThreadTest
    @Test fun commit() {
        val fragment = TestFragment()
        fragmentManager.commit {
            add(fragment, null)
        }
        assertThat(fragmentManager.fragments).doesNotContain(fragment)
        fragmentManager.executePendingTransactions()
        assertThat(fragmentManager.fragments).contains(fragment)
    }

    @UiThreadTest
    @Test fun commitWithBackStackId() {
        val fragment = TestFragment()
        val backStackId = fragmentManager.commit {
            add(fragment, null)
            addToBackStack("test")
        }
        assertThat(fragmentManager.fragments).doesNotContain(fragment)
        fragmentManager.executePendingTransactions()
        assertThat(fragmentManager.fragments).contains(fragment)
        assertThat(backStackId).isGreaterThan(-1)
    }

    @UiThreadTest
    @Test fun commitAllowingStateLoss() {
        // Use a detached FragmentManager to ensure state loss.
        val fragmentManager = FragmentManagerImpl()

        fragmentManager.commit(allowStateLoss = true) {
            add(TestFragment(), null)
        }
        assertThat(fragmentManager.fragments).isEmpty()
    }

    @UiThreadTest
    @Test fun commitAllowingStateLossWithBackStackId() {
        // Use a detached FragmentManager to ensure state loss.
        val fragmentManager = FragmentManagerImpl()

        val backStackId = fragmentManager.commit(allowStateLoss = true) {
            add(TestFragment(), null)
            addToBackStack("test")
        }
        assertThat(fragmentManager.fragments).isEmpty()
        assertThat(backStackId).isGreaterThan(-1)
    }

    @UiThreadTest
    @Test fun commitNow() {
        val fragment = TestFragment()
        fragmentManager.commitNow {
            add(fragment, null)
        }
        assertThat(fragmentManager.fragments).contains(fragment)
    }

    @UiThreadTest
    @Test fun commitNowAllowingStateLoss() {
        // Use a detached FragmentManager to ensure state loss.
        val fragmentManager = FragmentManagerImpl()

        fragmentManager.commitNow(allowStateLoss = true) {
            add(TestFragment(), null)
        }
        assertThat(fragmentManager.fragments).isEmpty()
    }
}

class TestActivity : FragmentActivity()
class TestFragment : Fragment()
