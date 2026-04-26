package com.d4vram.cbdcounter

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class InfusionChoiceBottomSheet : BottomSheetDialogFragment() {

    interface Listener {
        fun onInfusionTypeSelected(type: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        val v = LayoutInflater.from(context)
            .inflate(R.layout.bottom_sheet_infusion_choice, null, false)
        dialog.setContentView(v)

        v.findViewById<MaterialButton>(R.id.btnWeed).setOnClickListener {
            (activity as? Listener)?.onInfusionTypeSelected("weed")
            dismiss()
        }
        v.findViewById<MaterialButton>(R.id.btnPolen).setOnClickListener {
            (activity as? Listener)?.onInfusionTypeSelected("polen")
            dismiss()
        }

        return dialog
    }

    companion object {
        fun new() = InfusionChoiceBottomSheet()
    }
}
