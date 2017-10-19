package android.arch.paging

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import java.util.Collections


@RunWith(JUnit4::class)
class TiledDataSourceTest {
    @Test
    fun loadInitialEmpty() {
        @Suppress("UNCHECKED_CAST")
        val receiver = mock(PageResult.Receiver::class.java) as PageResult.Receiver<Int, String>
        val dataSource = EmptyDataSource()
        dataSource.loadRangeInitial(0, 0, 1, 0, receiver)

        @Suppress("UNCHECKED_CAST")
        val argument = ArgumentCaptor.forClass(PageResult::class.java)
                as ArgumentCaptor<PageResult<Int, String>>
        verify(receiver).onPageResult(argument.capture())
        verifyNoMoreInteractions(receiver)

        val observed = argument.value

        assertEquals(PageResult.INIT, observed.type)
        assertEquals(Collections.EMPTY_LIST, observed.page.items)
    }

    class EmptyDataSource : TiledDataSource<String>() {
        override fun countItems(): Int {
            return 0
        }

        override fun loadRange(startPosition: Int, count: Int): List<String> {
            @Suppress("UNCHECKED_CAST")
            return Collections.EMPTY_LIST as List<String>
        }
    }
}