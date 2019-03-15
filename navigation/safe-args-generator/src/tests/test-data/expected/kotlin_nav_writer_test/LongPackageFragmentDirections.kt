package a.b.reallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallylongpackage

import a.b.secondreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallylongpackage.R
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

class LongPackageFragmentDirections private constructor() {
    companion object {
        fun next(): NavDirections = ActionOnlyNavDirections(R.id.next)
    }
}
