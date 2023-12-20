package androidx.core.graphics.sample

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.graphics.BitmapCompat

class GraphicsSampleActivity : Activity() {
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        // decode a large natural image to use as input to different scaling algorithms
        // it's too big to draw in it's current form.
        val source: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.crowd_stripes)

        val width = 240
        val height = 160

        // resize the bitmap with the builtin method.
        // Bilinear with a 2x2 sample.
        val scaled1: Bitmap = Bitmap.createScaledBitmap(source, width, height, true)

        // resize the bitmap with the method from androidx bitmapcompat
        // repeated 2x2 bilinear as if creating mipmaps but stopping at the desired size
        val scaled2: Bitmap = BitmapCompat.createScaledBitmap(source, width, height, null, true)

        // draw both images scaled to fill their container without filtering so as to
        // make the difference obvious.
        val topview: ImageView = findViewById<View>(R.id.imageView2) as ImageView
        val drawable1: BitmapDrawable = BitmapDrawable(resources, scaled1)
        drawable1.setFilterBitmap(false)
        topview.setImageDrawable(drawable1)

        val bottomview: ImageView = findViewById<View>(R.id.imageView) as ImageView
        val drawable2: BitmapDrawable = BitmapDrawable(resources, scaled2)
        drawable2.setFilterBitmap(false)
        bottomview.setImageDrawable(drawable2)
    }
}
