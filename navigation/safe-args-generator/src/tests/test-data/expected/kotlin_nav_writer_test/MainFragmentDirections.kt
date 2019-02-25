package a.b

import android.os.Bundle
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import kotlin.Int
import kotlin.String

class MainFragmentDirections private constructor() {
    private data class Next(val main: String, val optional: String = "bla") : NavDirections {
        override fun getActionId(): Int = R.id.next

        override fun getArguments(): Bundle {
            val result = Bundle()
            result.putString("main", this.main)
            result.putString("optional", this.optional)
            return result
        }
    }

    companion object {
        fun previous(): NavDirections = ActionOnlyNavDirections(R.id.previous)

        fun next(main: String, optional: String = "bla"): NavDirections = Next(main, optional)
    }
}
