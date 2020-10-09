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
import android.widget.SeekBar
import android.widget.TextView
import android.widget.ToggleButton
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.versionedparcelable.ParcelUtils
import androidx.wear.watchface.R
import androidx.wear.watchface.style.BooleanUserStyleCategory
import androidx.wear.watchface.style.DoubleRangeUserStyleCategory
import androidx.wear.watchface.style.ListUserStyleCategory
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleCategory
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat
import androidx.wear.widget.SwipeDismissFrameLayout
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView

/**
 * Fragment for selecting a userStyle setting within a particular category.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
internal class StyleConfigFragment :
    Fragment(),
    StyleSettingViewAdapter.ClickListener {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal lateinit var watchFaceConfigActivity: WatchFaceConfigActivity
    private lateinit var categoryId: String
    private lateinit var styleSchema: List<UserStyleCategory>
    private lateinit var styleCategory: UserStyleCategory
    private lateinit var userStyle: UserStyle

    companion object {
        const val CATEGORY_ID = "CATEGORY_ID"
        const val USER_STYLE = "USER_STYLE"
        const val STYLE_SCHEMA = "STYLE_SCHEMA"

        fun newInstance(
            categoryId: String,
            styleSchema: List<UserStyleCategory>,
            userStyle: UserStyle
        ) = StyleConfigFragment().apply {
            arguments = Bundle().apply {
                putCharSequence(CATEGORY_ID, categoryId)
                putParcelable(
                    STYLE_SCHEMA,
                    ParcelUtils.toParcelable(
                        UserStyleSchemaWireFormat(styleSchema.map { it.toWireFormat() })
                    )
                )
                putParcelable(USER_STYLE, ParcelUtils.toParcelable(userStyle.toWireFormat()))
            }
        }
    }

    @SuppressWarnings("deprecation")
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

        val styleOptions = styleCategory.options
        val booleanUserStyleCategory =
            styleOptions.filterIsInstance<BooleanUserStyleCategory.BooleanOption>()
        val ListUserStyleCategory =
            styleOptions.filterIsInstance<ListUserStyleCategory.ListOption>()
        val rangeUserStyleCategory =
            styleOptions.filterIsInstance<DoubleRangeUserStyleCategory.DoubleRangeOption>()

        val booleanStyle = view.findViewById<ToggleButton>(R.id.styleToggle)
        val styleOptionsList = view.findViewById<WearableRecyclerView>(R.id.styleOptionsList)
        val rangedStyle = view.findViewById<SeekBar>(R.id.styleRange)

        when {
            booleanUserStyleCategory.isNotEmpty() -> {
                booleanStyle.isChecked = userStyle.options[styleCategory]!!.id.toBoolean()
                booleanStyle.setOnCheckedChangeListener { _, isChecked ->
                    setUserStyleOption(styleCategory.getOptionForId(isChecked.toString()))
                }
                styleOptionsList.visibility = View.GONE
                styleOptionsList.isEnabled = false
                rangedStyle.visibility = View.GONE
                rangedStyle.isEnabled = false
            }

            ListUserStyleCategory.isNotEmpty() -> {
                booleanStyle.isEnabled = false
                booleanStyle.visibility = View.GONE
                styleOptionsList.adapter =
                    StyleSettingViewAdapter(
                        requireContext(),
                        ListUserStyleCategory,
                        this@StyleConfigFragment
                    )
                styleOptionsList.layoutManager = WearableLinearLayoutManager(context)
                rangedStyle.isEnabled = false
                rangedStyle.visibility = View.GONE
            }

            rangeUserStyleCategory.isNotEmpty() -> {
                val rangedStyleCategory = styleCategory as DoubleRangeUserStyleCategory
                val minValue =
                    (
                        rangedStyleCategory.options.first() as
                            DoubleRangeUserStyleCategory.DoubleRangeOption
                        ).value
                val maxValue =
                    (
                        rangedStyleCategory.options.last() as
                            DoubleRangeUserStyleCategory.DoubleRangeOption
                        ).value
                val delta = (maxValue - minValue) / 100.0f
                val value = userStyle.options[styleCategory]!!.id.toFloat()
                rangedStyle.progress = ((value - minValue) / delta).toInt()
                rangedStyle.setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            setUserStyleOption(
                                rangedStyleCategory.getOptionForId(
                                    (minValue + delta * progress.toFloat()).toString()
                                )
                            )
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {}

                        override fun onStopTrackingTouch(seekBar: SeekBar) {}
                    }
                )
                booleanStyle.isEnabled = false
                booleanStyle.visibility = View.GONE
                styleOptionsList.isEnabled = false
                styleOptionsList.visibility = View.GONE
            }
        }

        view.addCallback(object : SwipeDismissFrameLayout.Callback() {
            override fun onDismissed(layout: SwipeDismissFrameLayout) {
                parentFragmentManager.popBackStackImmediate()
            }
        })

        return view
    }

    internal fun readOptionsFromArguments() {
        categoryId = requireArguments().getCharSequence(CATEGORY_ID).toString()

        val wireFormat: UserStyleSchemaWireFormat =
            ParcelUtils.fromParcelable(requireArguments().getParcelable(STYLE_SCHEMA)!!)
        styleSchema = wireFormat.mSchema.map { UserStyleCategory.createFromWireFormat(it) }

        userStyle = UserStyle(
            ParcelUtils.fromParcelable(requireArguments().getParcelable(USER_STYLE)!!),
            styleSchema
        )

        styleCategory = styleSchema.first { it.id == categoryId }
    }

    internal fun setUserStyleOption(userStyleOption: UserStyleCategory.Option) {
        val hashmap = userStyle.options as HashMap<UserStyleCategory, UserStyleCategory.Option>
        hashmap[styleCategory] = userStyleOption
        watchFaceConfigActivity.watchFaceConfigDelegate.setUserStyle(userStyle.toWireFormat())
    }

    override fun onItemClick(userStyleOption: UserStyleCategory.Option) {
        setUserStyleOption(userStyleOption)
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
@RestrictTo(LIBRARY)
internal class StyleSettingViewAdapter(
    private val context: Context,
    private val styleOptions: List<ListUserStyleCategory.ListOption>,
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
                    Helper.wrapIcon(context, drawable),
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
