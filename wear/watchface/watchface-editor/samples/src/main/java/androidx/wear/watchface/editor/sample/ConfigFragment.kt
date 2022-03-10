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
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.wearable.watchface.Constants
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.watchface.complications.ComplicationDataSourceInfoRetriever
import androidx.wear.watchface.editor.samples.R
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.widget.SwipeDismissFrameLayout
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import kotlinx.coroutines.launch

/**
 * Lets the user select whether they want to select a complication to configure, configure a
 * background complication or select an option from a user style setting. Should only be used if
 * theres's at least two items from that list.
 *
 * Note hierarchical UserStyleSchemas are supported.
 */
internal class ConfigFragment : Fragment() {

    private val watchFaceConfigActivity: WatchFaceConfigActivity
        get() = activity as WatchFaceConfigActivity

    /** Ids for the [androidx.wear.watchface.style.UserStyleSetting]s to select from. */
    private lateinit var settingIds: java.util.ArrayList<String>
    private lateinit var view: SwipeDismissFrameLayout
    private lateinit var configViewAdapter: ConfigViewAdapter

    companion object {
        const val SETTINGS_ID = "SETTINGS_ID"

        fun newInstance(
            settingIds: ArrayList<String>
        ) = ConfigFragment().apply {
            arguments = Bundle().apply {
                this.putStringArrayList(SETTINGS_ID, settingIds)
            }
        }
    }

    private var lifecycleObserver = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_START) {
            initConfigOptions()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        lifecycle.addObserver(lifecycleObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedState: Bundle?
    ): View {
        settingIds = requireArguments().getStringArrayList(SETTINGS_ID)!!

        view = inflater.inflate(R.layout.config_layout, container, false) as
            SwipeDismissFrameLayout

        view.addCallback(object : SwipeDismissFrameLayout.Callback() {
            override fun onDismissed(layout: SwipeDismissFrameLayout) {
                parentFragmentManager.popBackStackImmediate()
            }
        })

        return view
    }

    private fun initConfigOptions() {
        val editingSession = watchFaceConfigActivity.editorSession
        val hasBackgroundComplication = editingSession.backgroundComplicationSlotId != null
        val numComplications = editingSession.complicationSlotsState.value.size
        val hasNonBackgroundComplication =
            numComplications > if (hasBackgroundComplication) 1 else 0
        val configOptions = ArrayList<ConfigOption>()

        if (hasNonBackgroundComplication) {
            configOptions.add(
                ConfigOption(
                    id = Constants.KEY_COMPLICATIONS_SETTINGS,
                    icon = Icon.createWithResource(
                        context,
                        R.drawable.ic_elements_settings_complications
                    ),
                    title = resources.getString(R.string.settings_complications),
                    summary = "",
                    highlight = false
                )
            )
        }

        if (hasBackgroundComplication) {
            configOptions.add(createBackgroundConfigOption())
        }

        for (settingId in settingIds) {
            val styleCategory = editingSession.userStyleSchema[UserStyleSetting.Id(settingId)]!!
            configOptions.add(
                ConfigOption(
                    id = styleCategory.id.value,
                    icon = styleCategory.watchFaceEditorData?.icon ?: styleCategory.icon,
                    title = styleCategory.displayName.toString(),
                    summary = styleCategory.description.toString(),
                    highlight = false
                )
            )
        }

        configViewAdapter = ConfigViewAdapter(
            requireContext(),
            configOptions,
            this::onItemClick
        )
        view.findViewById<WearableRecyclerView>(R.id.configOptionsList).apply {
            adapter = configViewAdapter
            isEdgeItemsCenteringEnabled = true
            layoutManager = WearableLinearLayoutManager(context)
        }

        lifecycle.removeObserver(lifecycleObserver)
    }

    private fun createBackgroundConfigOption(): ConfigOption {
        // Initially assume there is no background image data source.
        val backgroundConfigOption = ConfigOption(
            id = Constants.KEY_BACKGROUND_IMAGE_SETTINGS,
            icon = Icon.createWithResource(
                context,
                R.drawable.ic_elements_comps_bg
            ),
            title = getResources().getString(R.string.settings_background_image),
            summary = resources.getString(R.string.none_background_image_provider),
            highlight = false
        )

        // Update the summary with the actual background complication data source name, if there is
        // one.
        watchFaceConfigActivity.coroutineScope.launch {
            val dataSourceInfoRetriever =
                ComplicationDataSourceInfoRetriever(activity as WatchFaceConfigActivity)
            val infoArray = dataSourceInfoRetriever.retrieveComplicationDataSourceInfo(
                watchFaceConfigActivity.editorSession.watchFaceComponentName,
                intArrayOf(watchFaceConfigActivity.editorSession.backgroundComplicationSlotId!!)
            )
            infoArray?.let {
                it[0].info?.apply {
                    backgroundConfigOption.summary = name
                }
                configViewAdapter.notifyDataSetChanged()
            }
            dataSourceInfoRetriever.close()
        }
        return backgroundConfigOption
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun onItemClick(configOption: ConfigOption) {
        val editingSession = watchFaceConfigActivity.editorSession
        when (configOption.id) {
            Constants.KEY_COMPLICATIONS_SETTINGS ->
                watchFaceConfigActivity.fragmentController.showComplicationConfigSelectionFragment()

            Constants.KEY_BACKGROUND_IMAGE_SETTINGS -> {
                watchFaceConfigActivity.coroutineScope.launch {
                    watchFaceConfigActivity.fragmentController.showComplicationConfig(
                        editingSession.backgroundComplicationSlotId!!
                    )
                }
            }

            else -> {
                watchFaceConfigActivity.fragmentController.showStyleConfigFragment(
                    configOption.id,
                    editingSession.userStyleSchema,
                    editingSession.userStyle.value
                )
            }
        }
    }
}

internal data class ConfigOption(
    val id: String,
    val icon: Icon?,
    val title: String,
    var summary: String,
    var highlight: Boolean
)

internal class ConfigViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var configOption: ConfigOption? = null
}

internal class Helper {
    companion object {
        /**
         * Wraps a given [Drawable] with a standard background to match the normal preference icon
         * styling. The wrapping is idempotent, calling it multiple times will only wrap the icon
         * once.
         *
         * @param context the current [Context], used for resolving resources.
         * @param icon the icon to wrap.
         * @return the wrapped icon.
         */
        fun wrapIcon(
            context: Context,
            icon: Drawable
        ): Drawable {
            if (icon is LayerDrawable && icon.findDrawableByLayerId(R.id.nested_icon) != null) {
                return icon // icon was already wrapped, return the icon without modifying it
            }
            val wrappedDrawable =
                (context.getDrawable(R.drawable.preference_wrapped_icon) as LayerDrawable)
            wrappedDrawable.setDrawableByLayerId(R.id.nested_icon, icon)
            return wrappedDrawable
        }
    }
}

/** Adapter for top level config options. */
internal class ConfigViewAdapter(
    private val context: Context,
    private val configOptions: List<ConfigOption>,
    val clickListener: (ConfigOption) -> Unit
) : RecyclerView.Adapter<ConfigViewHolder>() {

    private val handler = Handler(Looper.getMainLooper())

    fun clearAllHighlights() {
        for (configOption in configOptions) {
            configOption.highlight = false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ConfigViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.configlist_item_layout,
            parent,
            false
        )
    ).apply {
        itemView.setOnClickListener { clickListener(configOption!!) }
    }

    override fun onBindViewHolder(holder: ConfigViewHolder, position: Int) {
        val configOption = configOptions[position]
        holder.configOption = configOption
        val textView = holder.itemView as TextView

        textView.setBackgroundColor(
            if (configOption.highlight) Color.rgb(20, 40, 60) else Color.BLACK
        )

        textView.textSize = 16f

        val builder = SpannableStringBuilder()
        builder.append(configOption.title)
        builder.setSpan(StyleSpan(Typeface.BOLD), 0, configOption.title.length, 0)
        builder.append("\n")
        builder.append(configOption.summary)
        textView.text = builder

        configOption.icon?.loadDrawableAsync(
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

    override fun getItemCount() = configOptions.size
}
