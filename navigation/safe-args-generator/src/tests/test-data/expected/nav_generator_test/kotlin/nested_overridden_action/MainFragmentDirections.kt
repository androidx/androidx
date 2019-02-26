package foo.flavor

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import foo.R

class MainFragmentDirections private constructor() {
    companion object {
        fun startLogin(): NavDirections = ActionOnlyNavDirections(R.id.start_login)
    }
}
