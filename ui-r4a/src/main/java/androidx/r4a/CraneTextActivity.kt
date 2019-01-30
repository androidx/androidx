package androidx.r4a

import android.app.Activity
import android.os.Bundle
import com.google.r4a.setContent
import com.google.r4a.composer

class CraneTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { <TextDemo /> }
    }
}