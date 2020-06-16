/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.ui

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.watchface.R
import androidx.wear.watchfacestyle.ListViewUserStyleCategory
import androidx.wear.watchfacestyle.UserStyleCategory
import androidx.wear.widget.SwipeDismissFrameLayout
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView

/**
 * Fragment for selecting a userStyle setting within a particular category.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class StyleConfigFragment : Fragment(),
    StyleSettingViewAdapter.ClickListener {

    @RestrictTo(LIBRARY_GROUP)
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal lateinit var watchFaceConfigActivity: WatchFaceConfigActivity
    internal lateinit var categoryKey: String
    private lateinit var styleOptions: List<ListViewUserStyleCategory.ListViewOption>

    companion object {
        const val CATEGORY_KEY = "CATEGORY_KEY"

        fun newInstance(
            categoryKey: String,
            userStyleOptions: List<UserStyleCategory.Option>
        ) = StyleConfigFragment().apply {
            arguments = Bundle().apply {
                putCharSequence(CATEGORY_KEY, categoryKey)
                UserStyleCategory.writeOptionListToBundle(userStyleOptions, this)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        watchFaceConfigActivity = activity as WatchFaceConfigActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedState: Bundle?
    ): View {
        readOptionsFromArguments()

        val view =
            inflater.inflate(R.layout.style_options_layout, container, false)
                    as SwipeDismissFrameLayout

        view.findViewById<WearableRecyclerView>(R.id.styleOptionsList).apply {
            adapter =
                StyleSettingViewAdapter(
                    context,
                    styleOptions,
                    this@StyleConfigFragment
                )
            layoutManager = WearableLinearLayoutManager(context)
        }

        view.addCallback(object : SwipeDismissFrameLayout.Callback() {
            override fun onDismissed(layout: SwipeDismissFrameLayout) {
                parentFragmentManager.popBackStack()
            }
        })

        return view
    }

    fun readOptionsFromArguments() {
        categoryKey = requireArguments().getCharSequence(CATEGORY_KEY).toString()
        styleOptions = UserStyleCategory.readOptionsListFromBundle(requireArguments())
            .filterIsInstance<ListViewUserStyleCategory.ListViewOption>()
    }

    override fun onItemClick(userStyleOption: UserStyleCategory.Option) {
        // These will become IPCs eventually, hence the use of Bundles.
        val styleMap = UserStyleCategory.bundleToStyleMap(
            watchFaceConfigActivity.watchFaceConfigDelegate.getUserStyle(),
            watchFaceConfigActivity.styleSchema
        )

        val category = styleMap.keys.first { it.id == categoryKey }
        styleMap[category] = userStyleOption

        watchFaceConfigActivity.watchFaceConfigDelegate.setUserStyle(
            UserStyleCategory.styleMapToBundle(styleMap)
        )

        activity?.finish()
    }
}

internal class StyleSettingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var userStyleOption: UserStyleCategory.Option? = null
}

/**
 * An adapter for lists of selectable userStyle options.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class StyleSettingViewAdapter(
    private val context: Context,
    private val styleOptions: List<ListViewUserStyleCategory.ListViewOption>,
    private val clickListener: ClickListener
) :
    RecyclerView.Adapter<StyleSettingViewHolder>() {

    interface ClickListener {
        /** Called when a userStyle option is selected. */
        fun onItemClick(userStyleOption: UserStyleCategory.Option)
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = StyleSettingViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.stylelist_item_layout, parent, false
        )
    ).apply {
        itemView.setOnClickListener { clickListener.onItemClick(userStyleOption!!) }
    }

    override fun onBindViewHolder(holder: StyleSettingViewHolder, position: Int) {
        val styleOption = styleOptions[position]
        holder.userStyleOption = styleOption
        val textView = holder.itemView as TextView
        textView.text = styleOption.displayName
        styleOption.icon?.loadDrawableAsync(
            context,
            { drawable ->
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    wrapIcon(context, drawable),
                    /* top = */ null,
                    /* end = */ null,
                    /* bottom = */ null
                )
            },
            handler
        )
    }

    override fun getItemCount() = styleOptions.size
}
