package a.b

import android.os.Bundle
import androidx.navigation.NavDirections
import kotlin.Int
import kotlin.String

class MainFragmentDirections private constructor() {
    private object Previous : NavDirections {
        override fun getActionId(): Int = a.b.R.id.previous

        override fun getArguments(): Bundle {
            val result = Bundle()
            return result
        }
    }

    private data class Next(val main: String, val optional: String = "bla") : NavDirections {
        override fun getActionId(): Int = a.b.R.id.next

        override fun getArguments(): Bundle {
            val result = Bundle()
            result.putString("main", this.main)
            result.putString("optional", this.optional)
            return result
        }
    }

    companion object {
        fun previous(): NavDirections = Previous

        fun next(main: String, optional: String = "bla"): NavDirections = Next(main, optional)
    }
}
