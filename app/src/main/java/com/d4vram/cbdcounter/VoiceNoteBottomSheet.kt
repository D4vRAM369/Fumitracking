package com.d4vram.cbdcounter

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import java.io.File

class VoiceNoteBottomSheet : BottomSheetDialogFragment() {

    interface Listener {
        fun onVoiceNoteChanged(date: String)
    }

    private val dateArg by lazy { requireArguments().getString(ARG_DATE)!! }
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var isRecording = false

    // Referencias para poder actualizar la UI desde onRequestPermissionsResult
    private var btnRecordRef: MaterialButton? = null
    private var waveRef: TextView? = null
    private var btnPlayRef: MaterialButton? = null
    private var btnDeleteRef: MaterialButton? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
        val v = LayoutInflater.from(context)
            .inflate(R.layout.bottom_sheet_voice_note, null, false)
        dialog.setContentView(v)

        val btnRecord = v.findViewById<MaterialButton>(R.id.btnRecord)
        val btnPlay = v.findViewById<MaterialButton>(R.id.btnPlay)
        val btnDelete = v.findViewById<MaterialButton>(R.id.btnDeleteVoice)
        val waveform = v.findViewById<TextView>(R.id.waveformIndicator)

        btnRecordRef = btnRecord
        waveRef = waveform
        btnPlayRef = btnPlay
        btnDeleteRef = btnDelete

        val audioPath = Prefs.getVoiceNotePath(requireContext(), dateArg)

        if (File(audioPath).exists()) {
            btnPlay.visibility = View.VISIBLE
            btnDelete.visibility = View.VISIBLE
        }

        btnRecord.setOnClickListener {
            if (!isRecording) startRecording(audioPath, btnRecord, waveform)
            else stopRecording(btnRecord, waveform, btnPlay, btnDelete)
        }

        btnPlay.setOnClickListener { playAudio(audioPath) }

        btnDelete.setOnClickListener {
            File(audioPath).delete()
            (activity as? Listener)?.onVoiceNoteChanged(dateArg)
            dismiss()
        }

        return dialog
    }

    private fun startRecording(path: String, btn: MaterialButton, wave: TextView) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            @Suppress("DEPRECATION")
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO_PERMISSION)
            return
        }

        try {
            recorder = MediaRecorder(requireContext()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(path)
                prepare()
                start()
            }
            isRecording = true
            btn.text = "💾 Guardar"
            wave.visibility = View.VISIBLE
            Toast.makeText(context, "Grabando…", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error al iniciar grabación: ${e.message}", Toast.LENGTH_SHORT).show()
            recorder?.release()
            recorder = null
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val audioPath = Prefs.getVoiceNotePath(requireContext(), dateArg)
                val btn = btnRecordRef ?: return
                val wave = waveRef ?: return
                startRecording(audioPath, btn, wave)
            } else {
                Toast.makeText(context, "Permiso de micrófono denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopRecording(
        btn: MaterialButton, wave: TextView,
        btnPlay: MaterialButton, btnDelete: MaterialButton
    ) {
        try {
            recorder?.stop()
            recorder?.release()
        } catch (e: Exception) {
            // La grabación podría no haberse iniciado correctamente
        }
        recorder = null
        isRecording = false
        btn.text = "🎙️ Grabar"
        wave.visibility = View.GONE
        btnPlay.visibility = View.VISIBLE
        btnDelete.visibility = View.VISIBLE
        Toast.makeText(context, "Nota de voz guardada", Toast.LENGTH_SHORT).show()
        (activity as? Listener)?.onVoiceNoteChanged(dateArg)
    }

    private fun playAudio(path: String) {
        try {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error al reproducir audio", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        btnRecordRef = null
        waveRef = null
        btnPlayRef = null
        btnDeleteRef = null
        recorder?.release()
        player?.release()
    }

    companion object {
        private const val ARG_DATE = "arg_date"
        private const val REQUEST_AUDIO_PERMISSION = 101
        fun new(date: String) = VoiceNoteBottomSheet().apply {
            arguments = Bundle().apply { putString(ARG_DATE, date) }
        }
    }
}
