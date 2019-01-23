package foo.flavor

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

class MainFragmentDirections private constructor() {
    companion object {
        fun startLogin(): NavDirections = ActionOnlyNavDirections(foo.R.id.start_login)
    }
}
