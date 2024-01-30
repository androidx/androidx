package app.cash.paparazzi

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import app.cash.paparazzi.internal.ImageUtils
import com.android.ide.common.rendering.api.SessionParams
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.Description

class RenderingModeTest {

  @Ignore("b/245941625")
  @Test
  fun `shrinks to wrap view`() {
    Paparazzi(
      snapshotHandler = TestSnapshotVerifier(),
      deviceConfig = DeviceConfig.NEXUS_5.copy(
        softButtons = false
      ),
      renderingMode = SessionParams.RenderingMode.SHRINK
    ).runTest("shrinks to wrap view") {
      val view = buildView(
        context,
        ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
      )
      snapshot(view, "rendering-mode-shrink")
    }
  }

  @Ignore("b/245941625")
  @Test
  fun `renders full device with RenderingMode NORMAL`() {
    Paparazzi(
      snapshotHandler = TestSnapshotVerifier(true),
      deviceConfig = DeviceConfig.NEXUS_5.copy(
        softButtons = false
      ),
      renderingMode = SessionParams.RenderingMode.NORMAL
    ).runTest("renders full device with RenderingMode NORMAL") {
      val view = buildView(
        context,
        ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
      )
      snapshot(view, "rendering-mode-normal")
    }
  }

  private fun Paparazzi.runTest(name: String, body: Paparazzi.() -> Unit) {
    try {
      prepare(Description.createTestDescription(this@RenderingModeTest::class.java, name))
      body()
    } finally {
      close()
    }
  }

  private fun buildView(
    context: Context,
    rootLayoutParams: ViewGroup.LayoutParams? = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )
  ) =
    LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      rootLayoutParams?.let { layoutParams = it }
      addView(
        TextView(context).apply {
          id = 1
          text = "Text View Sample"
        }
      )

      addView(
        View(context).apply {
          id = 2
          layoutParams = LinearLayout.LayoutParams(100, 100)
          contentDescription = "Content Description Sample"
        }
      )

      addView(
        View(context).apply {
          id = 3
          layoutParams = LinearLayout.LayoutParams(100, 100).apply {
            setMarginsRelative(20, 20, 20, 20)
          }
          contentDescription = "Margin Sample"
        }
      )

      addView(
        View(context).apply {
          id = 4
          layoutParams = LinearLayout.LayoutParams(100, 100).apply {
            setMarginsRelative(20, 20, 20, 20)
          }
          foreground = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.YELLOW, Color.BLUE)
          ).apply {
            shape = GradientDrawable.OVAL
          }
          contentDescription = "Foreground Drawable"
        }
      )

      addView(
        Button(context).apply {
          id = 5
          layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
          ).apply {
            gravity = Gravity.CENTER
          }
          text = "Button Sample"
        }
      )
    }
}

private class TestSnapshotVerifier(val writeImages: Boolean = false) : SnapshotHandler {
  override fun newFrameHandler(
    snapshot: Snapshot,
    frameCount: Int,
    fps: Int
  ): SnapshotHandler.FrameHandler {
    return object : SnapshotHandler.FrameHandler {
      override fun handle(image: BufferedImage) {
        val expected = File("src/test/resources/${snapshot.name}.png")
        if (writeImages) {
          ImageIO.write(image, "png", expected)
        } else {
          ImageUtils.assertImageSimilar(
            relativePath = expected.path,
            image = image,
            goldenImage = ImageIO.read(expected),
            maxPercentDifferent = 0.1
          )
        }
      }

      override fun close() = Unit
    }
  }

  override fun close() = Unit
}
