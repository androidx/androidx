/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface.editor.sample

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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.versionedparcelable.ParcelUtils
import androidx.wear.watchface.R
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleData
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.BooleanUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.BooleanUserStyleSetting.BooleanOption
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption
import androidx.wear.watchface.style.UserStyleSetting.CustomValueUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.DoubleRangeUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.DoubleRangeUserStyleSetting.DoubleRangeOption
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.LongRangeUserStyleSetting
import androidx.wear.watchface.style.data.UserStyleSchemaWireFormat
import androidx.wear.watchface.style.data.UserStyleWireFormat
import androidx.wear.widget.SwipeDismissFrameLayout
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView

/**
 * Fragment for selecting a userStyle setting within a particular setting.
 */
internal class StyleConfigFragment : Fragment(), ClickListener {

    private lateinit var settingId: String
    private lateinit var styleSchema: UserStyleSchema
    private lateinit var styleSetting: UserStyleSetting
    private lateinit var userStyle: UserStyle

    companion object {
        const val SETTING_ID = "SETTING_ID"
        const val USER_STYLE = "USER_STYLE"
        const val STYLE_SCHEMA = "STYLE_SCHEMA"

        fun newInstance(
            settingId: String,
            styleSchema: UserStyleSchema,
            userStyle: UserStyle
        ) = StyleConfigFragment().apply {
            arguments = Bundle().apply {
                putCharSequence(SETTING_ID, settingId)
                putParcelable(
                    STYLE_SCHEMA,
                    ParcelUtils.toParcelable(styleSchema.toWireFormat())
                )
                putParcelable(USER_STYLE, ParcelUtils.toParcelable(userStyle.toWireFormat()))
            }
        }
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

        val booleanStyle = view.findViewById<ToggleButton>(R.id.styleToggle)
        val styleOptionsList = view.findViewById<WearableRecyclerView>(R.id.styleOptionsList)
        val rangedStyle = view.findViewById<SeekBar>(R.id.styleRange)

        val userStyleOption = userStyle[styleSetting]!!
        when (styleSetting) {
            is BooleanUserStyleSetting -> {
                booleanStyle.isChecked = (userStyleOption as BooleanOption).value
                booleanStyle.setOnCheckedChangeListener { _, isChecked ->
                    setUserStyleOption(BooleanOption.from(isChecked))
                }
                styleOptionsList.visibility = View.GONE
                styleOptionsList.isEnabled = false
                rangedStyle.visibility = View.GONE
                rangedStyle.isEnabled = false
            }

            is ListUserStyleSetting -> {
                booleanStyle.isEnabled = false
                booleanStyle.visibility = View.GONE
                styleOptionsList.adapter =
                    ListStyleSettingViewAdapter(
                        requireContext(),
                        styleSetting.options.filterIsInstance<ListUserStyleSetting.ListOption>(),
                        this@StyleConfigFragment
                    )
                styleOptionsList.layoutManager = WearableLinearLayoutManager(context)
                rangedStyle.isEnabled = false
                rangedStyle.visibility = View.GONE
            }

            is ComplicationSlotsUserStyleSetting -> {
                booleanStyle.isEnabled = false
                booleanStyle.visibility = View.GONE
                styleOptionsList.adapter =
                    ComplicationsStyleSettingViewAdapter(
                        requireContext(),
                        styleSetting.options.filterIsInstance<ComplicationSlotsOption>(),
                        this@StyleConfigFragment
                    )
                styleOptionsList.layoutManager = WearableLinearLayoutManager(context)
                rangedStyle.isEnabled = false
                rangedStyle.visibility = View.GONE
            }

            is CustomValueUserStyleSetting -> {
                // TODO(alexclarke): Implement.
            }

            is DoubleRangeUserStyleSetting -> {
                val rangedStyleSetting = styleSetting as DoubleRangeUserStyleSetting
                val minValue =
                    (rangedStyleSetting.options.first() as DoubleRangeOption).value
                val maxValue =
                    (rangedStyleSetting.options.last() as DoubleRangeOption).value
                val delta = (maxValue - minValue) / 100.0f
                val value = (userStyleOption as DoubleRangeOption).value.toFloat()
                rangedStyle.progress = ((value - minValue) / delta).toInt()
                rangedStyle.setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(
                            seekBar: SeekBar,
                            progress: Int,
                            fromUser: Boolean
                        ) {
                            setUserStyleOption(
                                rangedStyleSetting.getOptionForId(
                                    DoubleRangeOption(minValue + delta * progress.toFloat())
                                        .id
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

            is LongRangeUserStyleSetting -> {
                // TODO(alexclarke): Implement.
            }
        }

        view.addCallback(object : SwipeDismissFrameLayout.Callback() {
            override fun onDismissed(layout: SwipeDismissFrameLayout) {
                parentFragmentManager.popBackStackImmediate()
            }
        })

        return view
    }

    private fun readOptionsFromArguments() {
        settingId = requireArguments().getCharSequence(SETTING_ID).toString()

        styleSchema = UserStyleSchema(
            ParcelUtils.fromParcelable<UserStyleSchemaWireFormat>(
                requireArguments().getParcelable(STYLE_SCHEMA)!!
            )!!
        )

        userStyle = UserStyle(
            UserStyleData(
                ParcelUtils.fromParcelable<UserStyleWireFormat>(
                    requireArguments().getParcelable(USER_STYLE)!!
                )!!
            ),
            styleSchema
        )

        styleSetting = styleSchema.userStyleSettings.first { it.id.value == settingId }
    }

    internal fun setUserStyleOption(userStyleOption: UserStyleSetting.Option) {
        val watchFaceConfigActivity = (activity as WatchFaceConfigActivity)
        val editorSession = watchFaceConfigActivity.editorSession
        editorSession.userStyle.value = userStyle.toMutableUserStyle().apply {
            this[styleSetting] = userStyleOption
        }.toUserStyle()
    }

    override fun onItemClick(userStyleOption: UserStyleSetting.Option) {
        setUserStyleOption(userStyleOption)
        parentFragmentManager.popBackStackImmediate()
    }
}

internal class StyleSettingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var userStyleOption: UserStyleSetting.Option? = null
}

internal interface ClickListener {
    /** Called when a userStyle option is selected. */
    fun onItemClick(userStyleOption: UserStyleSetting.Option)
}

/**
 * An adapter for [ListUserStyleSetting].
 */
internal class ListStyleSettingViewAdapter(
    private val context: Context,
    private val styleOptions: List<ListUserStyleSetting.ListOption>,
    private val clickListener: ClickListener
) :
    RecyclerView.Adapter<StyleSettingViewHolder>() {

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

/**
 * An adapter for [ComplicationSlotsUserStyleSetting]. This is a very minimal placeholder UI.
 */
internal class ComplicationsStyleSettingViewAdapter(
    private val context: Context,
    private val styleOptions: List<ComplicationSlotsUserStyleSetting.ComplicationSlotsOption>,
    private val clickListener: ClickListener
) :
    RecyclerView.Adapter<StyleSettingViewHolder>() {

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
