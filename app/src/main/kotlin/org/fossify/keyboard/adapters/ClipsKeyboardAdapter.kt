package org.fossify.keyboard.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.keyboard.R
import org.fossify.keyboard.databinding.ItemClipOnKeyboardBinding
import org.fossify.keyboard.databinding.ItemSectionLabelBinding
import org.fossify.keyboard.extensions.clipsDB
import org.fossify.keyboard.extensions.config
import org.fossify.keyboard.extensions.getCurrentClip
import org.fossify.keyboard.extensions.getStrokeColor
import org.fossify.keyboard.helpers.ClipsHelper
import org.fossify.keyboard.helpers.ITEM_CLIP
import org.fossify.keyboard.helpers.ITEM_SECTION_LABEL
import org.fossify.keyboard.interfaces.RefreshClipsListener
import org.fossify.keyboard.models.Clip
import org.fossify.keyboard.models.ClipsSectionLabel
import org.fossify.keyboard.models.ListItem

class ClipsKeyboardAdapter(
    val context: Context, var items: ArrayList<ListItem>, val refreshClipsListener: RefreshClipsListener,
    val itemClick: (clip: Clip) -> Unit
) : RecyclerView.Adapter<ClipsKeyboardAdapter.ViewHolder>() {

    private val layoutInflater = LayoutInflater.from(context)

    private var textColor = context.getProperTextColor()
    private var backgroundColor = context.getProperBackgroundColor()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = when (viewType) {
            ITEM_SECTION_LABEL -> ItemSectionLabelBinding.inflate(layoutInflater, parent, false)
            else -> ItemClipOnKeyboardBinding.inflate(layoutInflater, parent, false)
        }

        return ViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bindView(item) { itemView ->
            when (item) {
                is Clip -> setupClip(itemView, item)
                is ClipsSectionLabel -> setupSection(itemView, item)
            }

            (itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams).isFullSpan = item is ClipsSectionLabel
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when {
        items[position] is ClipsSectionLabel -> ITEM_SECTION_LABEL
        else -> ITEM_CLIP
    }

    private fun setupClip(view: View, clip: Clip) {
        ItemClipOnKeyboardBinding.bind(view).apply {
            clipValue.apply {
                text = clip.value
                removeUnderlines()
            }

            clipPinBtn.apply {
                if (clip.isPinned) {
                    setImageResource(R.drawable.ic_pin_cyan)
                    contentDescription = context.getString(R.string.unpin_text)
                } else {
                    setImageResource(R.drawable.ic_pin_outline_gray)
                    contentDescription = context.getString(R.string.pin_text)
                }

                setOnClickListener {
                    ensureBackgroundThread {
                        val newPinned = !clip.isPinned
                        if (clip.id != null && clip.id != -1L) {
                            context.clipsDB.updatePinned(clip.id!!, newPinned)
                        } else {
                            val newClip = Clip(null, clip.value, newPinned)
                            ClipsHelper(context).insertClip(newClip)
                        }
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            context.toast(if (newPinned) R.string.text_pinned else R.string.text_unpinned)
                            refreshClipsListener.refreshClips()
                        }
                    }
                }
            }

            clipDeleteBtn.apply {
                setOnClickListener {
                    if (clip.isPinned) {
                        // CRITICAL: Pinned clips MUST NOT be deleted unless unpinned!
                        context.toast(R.string.cannot_delete_pinned)
                        return@setOnClickListener
                    }
                    ensureBackgroundThread {
                        if (clip.id != null && clip.id != -1L) {
                            context.clipsDB.delete(clip.id!!)
                        }
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            refreshClipsListener.refreshClips()
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setupSection(view: View, sectionLabel: ClipsSectionLabel) {
        ItemSectionLabelBinding.bind(view).apply {
            clipsSectionLabel.apply {
                text = sectionLabel.value
                setTextColor(textColor)
            }

            clipsSectionIcon.apply {
                applyColorFilter(textColor)

                if (sectionLabel.isCurrent) {
                    setOnLongClickListener { context.toast(R.string.pin_text); true; }
                    setImageDrawable(resources.getDrawable(R.drawable.ic_pin_vector))
                    setOnClickListener {
                        ensureBackgroundThread {
                            val currentClip = context.getCurrentClip() ?: return@ensureBackgroundThread
                            val clip = Clip(null, currentClip)
                            ClipsHelper(context).insertClip(clip)
                            refreshClipsListener.refreshClips()
                            context.toast(R.string.text_pinned)
                            if (context.config.vibrateOnKeypress) {
                                performHapticFeedback()
                            }
                        }
                    }
                } else {
                    setImageDrawable(resources.getDrawable(R.drawable.ic_pin_filled_vector))
                    background = null   // avoid doing any animations on clicking clipboard_manager_holder
                }
            }
        }
    }

    open inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(any: Any, callback: (itemView: View) -> Unit): View {
            return itemView.apply {
                callback(this)

                if (any is Clip) {
                    setOnClickListener {
                        itemClick.invoke(any)
                    }
                }
            }
        }
    }
}
