package foo.flavor

import android.os.Bundle
import androidx.navigation.NavDirections
import kotlin.Int

class MainFragmentDirections private constructor() {
    private object StartLogin : NavDirections {
        override fun getActionId(): Int = foo.R.id.start_login

        override fun getArguments(): Bundle {
            val result = Bundle()
            return result
        }
    }

    companion object {
        fun startLogin(): NavDirections = StartLogin
    }
}
