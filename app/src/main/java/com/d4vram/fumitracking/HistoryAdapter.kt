package com.d4vram.fumitracking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data class
data class HistoryItem(
    val date: String,
    val cbdCount: Int,
    val thcCount: Int,
    val dateObject: Date
) {
    val totalCount: Int get() = cbdCount + thcCount
}

// Adapter
class ImprovedHistoryAdapter(
    private val historyList: List<HistoryItem>,
    private val onDayClick: (String) -> Unit,
    private val fragmentManager: androidx.fragment.app.FragmentManager? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
    }

    private val groupedData = mutableListOf<Any>()

    init { groupData() }

    private fun groupData() {
        groupedData.clear()
        if (historyList.isEmpty()) return
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        var lastMonth = ""
        historyList.forEach { item ->
            val monthYear = dateFormat.format(item.dateObject)
                .replaceFirstChar { it.uppercase() }
            if (monthYear != lastMonth) {
                groupedData.add(monthYear)
                lastMonth = monthYear
            }
            groupedData.add(item)
        }
    }

    override fun getItemViewType(position: Int) =
        if (groupedData[position] is String) TYPE_HEADER else TYPE_ITEM

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.historyDate)
        val countText: TextView = itemView.findViewById(R.id.historyCount)  // Oculto
        val cbdChip: TextView = itemView.findViewById(R.id.cbdChip)
        val thcChip: TextView = itemView.findViewById(R.id.thcChip)
        val emojiText: TextView = itemView.findViewById(R.id.historyEmoji)
        val progressBar: View = itemView.findViewById(R.id.progressBar)
        val noteBadge: TextView? = itemView.findViewById(R.id.noteBadge)
        val audioBadge: TextView? = itemView.findViewById(R.id.audioBadge)
        val voiceNoteButton: android.widget.ImageButton? = itemView.findViewById(R.id.voiceNoteButton)
    }
    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val headerText: TextView = itemView.findViewById(R.id.headerText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        if (viewType == TYPE_HEADER) {
            HeaderViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.history_header, parent, false))
        } else {
            ItemViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.history_item, parent, false))
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.headerText.text = groupedData[position] as String
            is ItemViewHolder -> {
                val item = groupedData[position] as HistoryItem
                val dayFormat = SimpleDateFormat("EEEE dd", Locale("es", "ES"))
                holder.dateText.text = dayFormat.format(item.dateObject)
                    .replaceFirstChar { it.uppercase() }

                // Mostrar chips según los datos
                if (item.cbdCount > 0) {
                    holder.cbdChip.text = "${item.cbdCount} CBD"
                    holder.cbdChip.visibility = View.VISIBLE
                } else {
                    holder.cbdChip.visibility = View.GONE
                }

                if (item.thcCount > 0) {
                    holder.thcChip.text = "${item.thcCount} THC"
                    holder.thcChip.visibility = View.VISIBLE
                } else {
                    holder.thcChip.visibility = View.GONE
                }

                // Si ambos son 0, mostrar chip CBD con 0
                if (item.cbdCount == 0 && item.thcCount == 0) {
                    holder.cbdChip.text = "0 CBD"
                    holder.cbdChip.visibility = View.VISIBLE
                }

                val total = item.totalCount
                holder.emojiText.text = EmojiUtils.emojiForCount(total, holder.itemView.context)

                // Barra de progreso basada en el total
                val maxWidth = holder.itemView.width
                val progress = minOf(total / 10f, 1f)
                val layoutParams = holder.progressBar.layoutParams
                layoutParams.width = (maxWidth * progress).toInt()
                holder.progressBar.layoutParams = layoutParams

                // Color de la barra: del mayor, o verde si es 0
                val barColor = when {
                    total == 0 -> R.color.green_safe
                    item.thcCount > item.cbdCount -> {
                        // THC es mayor, usar escala verde
                        when {
                            total <= 4 -> R.color.thc_primary_light
                            total <= 6 -> R.color.orange_danger
                            else -> R.color.red_critical
                        }
                    }
                    else -> {
                        // CBD es mayor o igual, usar escala azul
                        when {
                            total <= 4 -> R.color.primary_light
                            total <= 6 -> R.color.orange_danger
                            else -> R.color.red_critical
                        }
                    }
                }
                holder.progressBar.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.context, barColor)
                )

                // Badge de nota visible si existe nota para ese día
                val ctx = holder.itemView.context
                holder.noteBadge?.visibility =
                    if (Prefs.hasNote(ctx, item.date)) View.VISIBLE else View.GONE

                // Badge de audio visible si existe audio para ese día
                holder.audioBadge?.visibility =
                    if (Prefs.hasAudio(ctx, item.date)) View.VISIBLE else View.GONE

                // Botón de nota de voz: alpha según si ya hay grabación
                holder.voiceNoteButton?.alpha =
                    if (Prefs.hasVoiceNote(ctx, item.date)) 1f else 0.4f
                holder.voiceNoteButton?.setOnClickListener {
                    val fm = fragmentManager
                        ?: (ctx as? androidx.appcompat.app.AppCompatActivity)?.supportFragmentManager
                    fm?.let {
                        VoiceNoteBottomSheet.new(item.date).show(it, "voice_note")
                    }
                }

                // Clicks para abrir el modal
                holder.itemView.setOnClickListener { onDayClick(item.date) }
                holder.noteBadge?.setOnClickListener { onDayClick(item.date) }
                holder.audioBadge?.setOnClickListener { onDayClick(item.date) }
            }
        }
    }

    override fun getItemCount() = groupedData.size

    fun refresh() {
        groupData()
        notifyDataSetChanged()
    }
}


// Decoración
class HistoryItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.bottom = spacing
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = spacing
        }
    }
}
