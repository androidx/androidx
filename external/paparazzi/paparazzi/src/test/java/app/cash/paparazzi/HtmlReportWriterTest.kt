/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.paparazzi

import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.util.Date
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HtmlReportWriterTest {
  @get:Rule
  val reportRoot: TemporaryFolder = TemporaryFolder()

  @get:Rule
  val snapshotRoot: TemporaryFolder = TemporaryFolder()

  private val anyImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
  private val anyImageHash = "9069ca78e7450a285173431b3e52c5c25299e473"

  @Test
  fun happyPath() {
    val htmlReportWriter = HtmlReportWriter("run_one", reportRoot.root)
    htmlReportWriter.use {
      val frameHandler = htmlReportWriter.newFrameHandler(
        Snapshot(
          name = "loading",
          testName = TestName("app.cash.paparazzi", "CelebrityTest", "testSettings"),
          timestamp = Instant.parse("2019-03-20T10:27:43Z").toDate(),
          tags = listOf("redesign")
        ),
        1,
        -1
      )
      frameHandler.use {
        frameHandler.handle(anyImage)
      }
    }

    assertThat(File("${reportRoot.root}/index.js")).hasContent(
      """
        |window.all_runs = [
        |  "run_one"
        |];
        |
      """.trimMargin()
    )

    assertThat(File("${reportRoot.root}/runs/run_one.js")).hasContent(
      """
        |window.runs["run_one"] = [
        |  {
        |    "name": "loading",
        |    "testName": "app.cash.paparazzi.CelebrityTest#testSettings",
        |    "timestamp": "2019-03-20T10:27:43.000Z",
        |    "tags": [
        |      "redesign"
        |    ],
        |    "file": "images/$anyImageHash.png"
        |  }
        |];
        |
      """.trimMargin()
    )
  }

  @Test
  fun sanitizeForFilename() {
    assertThat("0 Dollars".sanitizeForFilename()).isEqualTo("0_dollars")
    assertThat("`!#$%&*+=|\\'\"<>?/".sanitizeForFilename()).isEqualTo("_________________")
    assertThat("~@^()[]{}:;,.".sanitizeForFilename()).isEqualTo("~@^()[]{}:;,.")
  }

  @Test
  fun noSnapshotOnFailure() {
    val htmlReportWriter = HtmlReportWriter("run_one", reportRoot.root)
    htmlReportWriter.use {
      val frameHandler = htmlReportWriter.newFrameHandler(
        snapshot = Snapshot(
          name = "loading",
          testName = TestName("app.cash.paparazzi", "CelebrityTest", "testSettings"),
          timestamp = Instant.parse("2019-03-20T10:27:43Z").toDate()
        ),
        frameCount = 4,
        fps = -1
      )
      frameHandler.use {
        // intentionally empty, to simulate no content written on exception
      }
    }

    assertThat(File(reportRoot.root, "images")).isEmptyDirectory
    assertThat(File(reportRoot.root, "videos")).isEmptyDirectory
  }

  @Test
  fun alwaysOverwriteOnRecord() {
    // set record mode
    System.setProperty("paparazzi.test.record", "true")

    val htmlReportWriter = HtmlReportWriter("record_run", reportRoot.root, snapshotRoot.root)
    htmlReportWriter.use {
      val now = Instant.parse("2021-02-23T10:27:43Z")
      val snapshot = Snapshot(
        name = "test",
        testName = TestName("app.cash.paparazzi", "HomeView", "testSettings"),
        timestamp = now.toDate()
      )
      val file =
        File("${snapshotRoot.root}/images/app.cash.paparazzi_HomeView_testSettings_test.png")
      val golden = file.toPath()

      // precondition
      assertThat(golden).doesNotExist()

      // take 1
      val frameHandler1 = htmlReportWriter.newFrameHandler(
        snapshot = snapshot,
        frameCount = 1,
        fps = -1
      )
      frameHandler1.use { frameHandler1.handle(anyImage) }
      assertThat(golden).exists()
      val timeFirstWrite = golden.lastModifiedTime()

      // I know....but guarantees writes won't happen in same tick
      Thread.sleep(100)

      // take 2
      val frameHandler2 = htmlReportWriter.newFrameHandler(
        snapshot = snapshot.copy(timestamp = now.plusSeconds(1).toDate()),
        frameCount = 1,
        fps = -1
      )
      frameHandler2.use { frameHandler2.handle(anyImage) }
      assertThat(golden).exists()
      val timeOverwrite = golden.lastModifiedTime()

      // should always overwrite
      assertThat(timeOverwrite).isGreaterThan(timeFirstWrite)
    }
  }

  private fun Instant.toDate() = Date(toEpochMilli())

  private fun Path.lastModifiedTime(): FileTime {
    return Files.readAttributes(this, BasicFileAttributes::class.java).lastModifiedTime()
  }
}
