/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget.demos

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.ImageProvider
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.demos.ShareableImageUtils.getShareableImageUri
import androidx.glance.appwidget.demos.ShareableImageUtils.uriImageFile
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import java.io.File

class ImageAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ImageAppWidget()
}

/** Sample AppWidget that showcase the [ContentScale] options for [Image] */
class ImageAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val imageUri: Uri = getShareableImageUri(context)

        provideContent {
            Scaffold(titleBar = { Header() }, content = { BodyContent(imageUri = imageUri) })
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val imageUri: Uri = getShareableImageUri(context)

        provideContent {
            Scaffold(titleBar = { Header() }, content = { BodyContent(imageUri = imageUri) })
        }
    }
}

@Composable
private fun Header() {
    val context = LocalContext.current
    var shouldTintHeaderIcon by remember { mutableStateOf(true) }

    Row(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier.fillMaxWidth().background(Color.White)
    ) {
        // Demonstrates toggling application of color filter on an image
        Image(
            provider = ImageProvider(R.drawable.ic_android),
            contentDescription = null,
            colorFilter =
                if (shouldTintHeaderIcon) {
                    ColorFilter.tint(ColorProvider(day = Color.Green, night = Color.Blue))
                } else {
                    null
                },
            modifier = GlanceModifier.clickable { shouldTintHeaderIcon = !shouldTintHeaderIcon }
        )
        Text(
            text = context.getString(R.string.image_widget_name),
            modifier = GlanceModifier.padding(8.dp),
        )
    }
}

@Composable
private fun BodyContent(imageUri: Uri) {
    var type by remember { mutableStateOf(ContentScale.Fit) }
    Column(modifier = GlanceModifier.fillMaxSize().padding(8.dp)) {
        Spacer(GlanceModifier.size(4.dp))
        Button(
            text = "Content Scale: ${type.asString()}",
            modifier = GlanceModifier.fillMaxWidth(),
            onClick = {
                type =
                    when (type) {
                        ContentScale.Crop -> ContentScale.FillBounds
                        ContentScale.FillBounds -> ContentScale.Fit
                        else -> ContentScale.Crop
                    }
            }
        )
        Spacer(GlanceModifier.size(4.dp))

        val itemModifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)

        LazyColumn(GlanceModifier.fillMaxSize()) {
            val textModifier = GlanceModifier.padding(bottom = 8.dp)
            item {
                Column {
                    // An image who's provider uses a resource.
                    ResourceImage(contentScale = type, modifier = itemModifier)
                    Text("ImageProvider(resourceId)", textModifier)
                }
            }
            item {
                Column {
                    // An image who's provider uses a content uri. This uri will be passed through
                    // the remote views to the AppWidget's host. The host will query our app via
                    // content provider to resolve the Uri into a bitmap.
                    UriImage(uri = imageUri, contentScale = type, modifier = itemModifier)
                    Text("ImageProvider(uri)", textModifier)
                }
            }

            item {
                Column {
                    // An image who's provider holds an in-memory bitmap.
                    BitmapImage(contentScale = type, modifier = itemModifier)
                    Text("ImageProvider(bitmap)", textModifier)
                }
            }

            item {
                Column {
                    // An image who's provider uses the Icon api.
                    IconImage(contentScale = type, modifier = itemModifier)
                    Text("ImageProvider(icon)", textModifier)
                }
            }
        }
    }
}

@Composable
private fun ResourceImage(contentScale: ContentScale, modifier: GlanceModifier = GlanceModifier) {
    Image(
        provider = ImageProvider(R.drawable.compose),
        contentDescription = "Content Scale image sample (value: ${contentScale.asString()})",
        contentScale = contentScale,
        modifier = modifier
    )
}

/**
 * Demonstrates using the Uri image provider in `androidx.glance.appwidget`. This image will be sent
 * to the RemoteViews as a uri. In the AppWidgetHost, the uri will be resolved by querying back to
 * this app's [ContentProvider], see [ImageAppWidgetImageContentProvider]. There are several
 * drawbacks to this approach. Consider them before going this route.
 * - Images that are within the app's private directories can only be exposed via ContentProvider; a
 *   direct reference via file:// uri will not work.
 * - The ContentProvider approach will not work across user/work profiles.
 * - Any time the image is loaded, the AppWidget's process will be started, consuming battery and
 *   memory.
 * - FileProvider cannot be used due to a permissions issue.
 */
@Composable
private fun UriImage(
    contentScale: ContentScale,
    modifier: GlanceModifier = GlanceModifier,
    uri: Uri
) {
    Image(
        provider = ImageProvider(uri),
        contentDescription = "Content Scale image sample (value: ${contentScale.asString()})",
        contentScale = contentScale,
        modifier = modifier
    )
}

/**
 * Bitmaps are passed in memory from the appwidget provider's process to the appwidget host's
 * process. Be careful to not use too many or too large bitmaps. See [android.widget.RemoteViews]
 * for more info.
 */
@Composable
private fun BitmapImage(contentScale: ContentScale, modifier: GlanceModifier = GlanceModifier) {
    fun makeBitmap(): Bitmap {
        val w = 100f
        val h = 100f
        val bmp = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()

        paint.setColor(Color.Black.toArgb()) // transparent
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.setColor(Color.White.toArgb()) // Opaque
        canvas.drawCircle(w / 2f, h / 2f, w / 3f, paint)

        return bmp
    }

    Image(
        provider = ImageProvider(makeBitmap()),
        contentDescription = "An image with an in-memory bitmap provider",
        contentScale = contentScale,
        modifier = modifier
    )
}

/**
 * For displaying [Image]s backed by [android.graphics.drawable.Icon]s. Despite the name, an [Icon]
 * does not need to represent a literal icon.
 */
@Composable
private fun IconImage(contentScale: ContentScale, modifier: GlanceModifier) {
    if (Build.VERSION.SDK_INT < 23) {
        Text("The Icon api requires api >= 23")
        return
    }

    val bitmap = canvasBitmap(200, circleColor = Color.Red)

    Box {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "An image with an in-memory bitmap provider",
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

private fun canvasBitmap(outputCanvasSize: Int, circleColor: Color): Bitmap {
    val bitmap = Bitmap.createBitmap(outputCanvasSize, outputCanvasSize, Bitmap.Config.ARGB_8888)
    val padding = outputCanvasSize * .05f
    val canvas = Canvas(bitmap)

    fun drawBlueSquare(canvas: Canvas) {
        val squareSize = outputCanvasSize * (2f / 3f)

        val x0 = padding
        val x1 = x0 + squareSize
        val y0 = (outputCanvasSize - squareSize - padding)
        val y1 = y0 + squareSize
        val paint = Paint().apply { setColor(Color.Blue.toArgb()) }
        canvas.drawRect(x0, y0, x1, y1, paint)
    }

    fun drawCircle(canvas: Canvas) {
        val r = outputCanvasSize * (1f / 3f)
        val cx = outputCanvasSize - r - padding
        val cy = r + padding

        val paint = Paint().apply { setColor(circleColor.toArgb()) }
        canvas.drawCircle(cx, cy, r, paint)
    }

    drawBlueSquare(canvas)
    drawCircle(canvas)

    return bitmap
}

private fun ContentScale.asString(): String =
    when (this) {
        ContentScale.Fit -> "Fit"
        ContentScale.FillBounds -> "Fill Bounds"
        ContentScale.Crop -> "Crop"
        else -> "Unknown content scale"
    }

private object ShareableImageUtils {
    private val fileProviderDirectory = "imageAppWidget"
    private val fileName = "imageToBeLoadedFromUri.png"

    private fun uri(context: Context, filename: String): Uri {
        val packageName = context.packageName
        return Uri.parse("content://$packageName/$filename")
    }

    val Context.uriImageFile: File
        get() = File(this.filesDir, "$fileProviderDirectory/$fileName")

    /** Create a Uri to share and ensure the file we want to return exists. */
    fun getShareableImageUri(context: Context): Uri {

        val file: File = context.uriImageFile
        file.parentFile?.mkdir()
        val success =
            if (file.exists()) {
                true
            } else {
                val bitmap = canvasBitmap(300, circleColor = Color.Green)
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }

        if (success) {
            return uri(context, file.name)
        } else {
            throw IllegalStateException("Failed to write bitmap")
        }
    }
}

/** Expose an image file via content:// uri. */
class ImageAppWidgetImageContentProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        return true
    }

    /**
     * A simplified version of [openFile] for example only. This version does not validate the uri
     * and always returns the same file.
     */
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val context = context ?: return null
        val file = context.uriImageFile
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return null // unused
    }

    override fun getType(uri: Uri): String? {
        return "image/png"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null // unused
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0 // unused
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return 0 // unused
    }
}
