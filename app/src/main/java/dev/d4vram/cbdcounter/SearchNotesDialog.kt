package dev.d4vram.cbdcounter

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class SearchNotesDialog : DialogFragment() {

    private lateinit var notesRecyclerView: RecyclerView
    private lateinit var searchEditText: TextInputEditText
    private lateinit var adapter: NotesAdapter
    private val allNotes = mutableListOf<NoteItem>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_search_notes, null)

        notesRecyclerView = view.findViewById(R.id.notesRecyclerView)
        searchEditText = view.findViewById(R.id.searchNotesEditText)

        loadAllNotes()
        adapter = NotesAdapter(allNotes.toMutableList())
        notesRecyclerView.layoutManager = LinearLayoutManager(context)
        notesRecyclerView.adapter = adapter

        adapter.setOnItemClickListener { noteItem ->
            // Abrir el modal del día para editar
            DayModalFragment.newInstance(noteItem.date).show(parentFragmentManager, "day_modal")
            dismiss() // Cerrar el diálogo de búsqueda
        }

        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterNotes(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        return MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .setPositiveButton("Cerrar", null)
            .create()
    }

    private fun loadAllNotes() {
        allNotes.clear()
        val prefs = requireContext().getSharedPreferences("CBDCounter", android.content.Context.MODE_PRIVATE)
        val allEntries = prefs.all
        allEntries.forEach { (key, value) ->
            if (key.startsWith("NOTE_") && value is String && value.isNotBlank()) {
                val date = key.removePrefix("NOTE_")
                allNotes.add(NoteItem(date, value))
            }
        }
        allNotes.sortByDescending { it.date } // Más recientes primero
    }

    private fun filterNotes(query: String) {
        val filtered = if (query.isBlank()) {
            allNotes
        } else {
            allNotes.filter { it.note.contains(query, ignoreCase = true) || it.date.contains(query, ignoreCase = true) }
        }
        adapter.updateList(filtered)
    }
}

data class NoteItem(val date: String, val note: String)

class NotesAdapter(private var notes: MutableList<NoteItem>) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    private var onItemClickListener: ((NoteItem) -> Unit)? = null

    fun setOnItemClickListener(listener: (NoteItem) -> Unit) {
        onItemClickListener = listener
    }

    class NoteViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val dateText: android.widget.TextView = itemView.findViewById(R.id.noteDate)
        val noteText: android.widget.TextView = itemView.findViewById(R.id.noteContent)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val item = notes[position]
        holder.dateText.text = item.date
        holder.noteText.text = item.note.replace("\n", "\n")
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(item)
        }
    }

    override fun getItemCount() = notes.size

    fun updateList(newList: List<NoteItem>) {
        notes.clear()
        notes.addAll(newList)
        notifyDataSetChanged()
    }
}