package com.d4vram.fumitracking

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class NoteBottomSheet : BottomSheetDialogFragment() {

    interface Listener {
        fun onNoteChanged(date: String)
    }

    private val dateArg by lazy { requireArguments().getString(ARG_DATE)!! }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        val v = LayoutInflater.from(context)
            .inflate(R.layout.dialog_note_bottomsheet, null, false)
        dialog.setContentView(v)

        val tvTitle = v.findViewById<TextView>(R.id.tvTitle)
        val etNote = v.findViewById<TextInputEditText>(R.id.etNote)
        val btnSave = v.findViewById<MaterialButton>(R.id.btnSave)
        val btnDelete = v.findViewById<MaterialButton>(R.id.btnDelete)

        tvTitle.text = getString(R.string.note_title_with_date, dateArg)
        val initialNote = Prefs.getNote(requireContext(), dateArg) ?: ""
        etNote.setText(initialNote)

        btnSave.setOnClickListener {
            val text = etNote.text?.toString()?.trim()
            // si está vacío, elimina; si no, guarda
            Prefs.setNote(requireContext(), dateArg, text.takeUnless { it.isNullOrEmpty() })
            (parentFragment as? Listener ?: activity as? Listener)?.onNoteChanged(dateArg)
            dismiss()
        }

        btnDelete.setOnClickListener {
            Prefs.setNote(requireContext(), dateArg, null) // elimina la nota
            (parentFragment as? Listener ?: activity as? Listener)?.onNoteChanged(dateArg)
            dismiss()
        }

        return dialog
    }

    companion object {
        private const val ARG_DATE = "arg_date"
        fun new(date: String) = NoteBottomSheet().apply {
            arguments = Bundle().apply { putString(ARG_DATE, date) }
        }
    }
}
