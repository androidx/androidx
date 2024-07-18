package app.cash.paparazzi.internal

import app.cash.paparazzi.internal.PaparazziLogger.MultipleFailuresException
import java.io.FileNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test

class PaparazziLoggerTest {
  @Test
  fun testNoErrors() {
    val logger = PaparazziLogger()

    try {
      logger.assertNoErrors()
    } catch (ignored: Exception) {
      fail("Did not expect exception to be thrown: $ignored")
    }
  }

  @Test
  fun testSingleError() {
    val logger = PaparazziLogger()
    logger.error(FileNotFoundException("error1"), null)

    try {
      logger.assertNoErrors()
      fail("Expected exception to be thrown")
    } catch (ignored: Exception) {
      assertThat(ignored).isInstanceOf(FileNotFoundException::class.java)
    }
  }

  @Test
  fun testMultipleErrors() {
    val logger = PaparazziLogger()
    logger.error(FileNotFoundException("error1"), null)
    logger.error("tag", null, IllegalStateException("error2"), null, null)

    try {
      logger.assertNoErrors()
      fail("Expected exceptions to be thrown")
    } catch (ignored: Exception) {
      assertThat(ignored).isInstanceOf(MultipleFailuresException::class.java)
      assertThat(ignored.message).contains("There were 2 errors:")
      assertThat(ignored.message).contains("java.io.FileNotFoundException: error1")
      assertThat(ignored.message).contains("java.lang.IllegalStateException: error2")
    }
  }
}
