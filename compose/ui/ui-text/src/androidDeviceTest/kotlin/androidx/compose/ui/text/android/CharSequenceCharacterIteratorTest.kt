package androidx.compose.ui.text.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class CharSequenceCharacterIteratorTest {
    @Test
    fun canClone() {
        val subject = CharSequenceCharacterIterator("", 0, 0)
        val cloned = subject.clone()
        assertThat(subject).isNotSameInstanceAs(cloned)
    }
}
