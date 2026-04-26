package com.d4vram.cbdcounter

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.io.File

class DayModalFragment : DialogFragment() {

    private var date: String? = null
    private var mediaRecorder: android.media.MediaRecorder? = null
    private var isRecording = false
    private var audioFile: File? = null
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var audioSeekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var seekBarContainer: android.widget.LinearLayout
    private lateinit var playAudioButton: MaterialButton
    private lateinit var deleteAudioButton: MaterialButton
    private lateinit var audioControlsLayout: android.widget.LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private var updateProgressRunnable: Runnable? = null
    private var recordStartTime: Long = 0


    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

        fun newInstance(date: String): DayModalFragment {
            return DayModalFragment().apply {
                arguments = Bundle().apply {
                    putString("date", date)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        date = arguments?.getString("date")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_day_modal, null)

        val modalTitle = view.findViewById<TextView>(R.id.modalTitle)
        val takesLabel = view.findViewById<TextView>(R.id.takesLabel)
        val noteEditText = view.findViewById<TextInputEditText>(R.id.noteEditText)
        val recordAudioButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.recordAudioButton)
        val saveButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.saveButton)
        val audioStatus = view.findViewById<TextView>(R.id.audioStatus)
        audioSeekBar = view.findViewById(R.id.audioSeekBar)
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime)
        tvTotalTime = view.findViewById(R.id.tvTotalTime)
        seekBarContainer = view.findViewById(R.id.seekBarContainer)
        playAudioButton = view.findViewById(R.id.playAudioButton)
        deleteAudioButton = view.findViewById(R.id.deleteAudioButton)
        audioControlsLayout = view.findViewById(R.id.audioControlsLayout)

        setupSeekBarListener()


        val breakdownLabel = view.findViewById<TextView>(R.id.breakdownLabel)

        date?.let { d ->
            modalTitle.text = "Día: $d"

            // Load count (TOTAL = CBD + THC)
            val count = Prefs.getTotalCount(requireContext(), d)
            takesLabel.text = "Tomas: $count"

            // Desglose por tipo (contando emojis en las notas)
            val note = Prefs.getNote(requireContext(), d) ?: ""
            val lines = note.split("\n").filter { it.isNotBlank() }
            val cbdPure = lines.count { it.trim().startsWith("🔹") || it.trim().startsWith("🟢") }
            val weedCount = lines.count { it.contains("🌿") }
            val polenCount = lines.count { it.contains("🍫") }

            val breakdown = buildList {
                if (cbdPure > 0) add("$cbdPure 🔹")
                if (weedCount > 0) add("$weedCount 🌿")
                if (polenCount > 0) add("$polenCount 🍫")
            }.joinToString("  ·  ")

            if (breakdown.isNotEmpty()) {
                breakdownLabel.text = breakdown
                breakdownLabel.visibility = View.VISIBLE
            } else {
                breakdownLabel.visibility = View.GONE
            }

            // Load note
            noteEditText.setText(Prefs.getNote(requireContext(), d) ?: "")

            // Check if audio exists
            audioFile = getAudioFile(d)
            if (audioFile?.exists() == true) {
                audioStatus.text = "Audio grabado disponible"
                seekBarContainer.visibility = View.VISIBLE
                audioControlsLayout.visibility = View.VISIBLE
                setupAudioPlayer(audioFile!!)
            } else {
                seekBarContainer.visibility = View.GONE
                audioControlsLayout.visibility = View.GONE
            }
        }

 recordAudioButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
                recordAudioButton.text = "Grabar Audio"
                audioStatus.text = "Grabación detenida"
                
                // Si se guardó el archivo, cargarlo
                audioFile?.let { file ->
                    if (file.exists()) {
                        audioStatus.text = "Audio grabado disponible"
                        seekBarContainer.visibility = View.VISIBLE
                        audioControlsLayout.visibility = View.VISIBLE
                        setupAudioPlayer(file)
                    }
                }
            } else {
                startRecording()
                recordAudioButton.text = "Detener Grabación"
                audioStatus.text = "Grabando..."
                seekBarContainer.visibility = View.VISIBLE
                audioControlsLayout.visibility = View.GONE
            }
        }



        playAudioButton.setOnClickListener {
            togglePlayback()
        }

        deleteAudioButton.setOnClickListener {
            audioFile?.delete()
            audioFile = null
            mediaPlayer?.release()
            mediaPlayer = null
            audioStatus.text = ""
            seekBarContainer.visibility = View.GONE
            audioControlsLayout.visibility = View.GONE
            Toast.makeText(context, "Audio borrado", Toast.LENGTH_SHORT).show()
        }



        saveButton.setOnClickListener {
            date?.let { d ->
                val note = noteEditText.text?.toString()?.trim()
                Prefs.setNote(requireContext(), d, if (note.isNullOrEmpty()) null else note)

                // Notify listeners (like MainActivity or StatsActivity)
                (parentFragment as? NoteBottomSheet.Listener ?: activity as? NoteBottomSheet.Listener)?.onNoteChanged(d)

                dismiss()
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .setCancelable(true)
            .create()
    }

    private fun getAudioFile(date: String): File {
        val dir = File(requireContext().filesDir, "audios")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "audio_$date.3gp")
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
            return
        }

        date?.let { d ->
            try {
                val audioFile = getAudioFile(d)
                if (!audioFile.parentFile?.exists()!!) {
                    audioFile.parentFile?.mkdirs()
                }
                mediaRecorder = android.media.MediaRecorder(requireContext()).apply {
                    setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                    setOutputFormat(android.media.MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AMR_NB)
                    setOutputFile(audioFile.absolutePath)
                    prepare()
                    start()
                }
                isRecording = true
                recordStartTime = System.currentTimeMillis()
                startRecordingProgressUpdate()
                Toast.makeText(context, "Grabación iniciada", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al iniciar grabación: ${e.message} (Código: ${e.cause})", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun stopRecording() {
        if (isRecording) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
                stopProgressUpdates()
                Toast.makeText(context, "Grabación guardada", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al detener grabación: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                Toast.makeText(context, "Permiso de grabación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun setupAudioPlayer(file: File) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    updatePlayButtonText()
                    audioSeekBar.progress = 0
                    tvCurrentTime.text = formatTime(0)
                    stopProgressUpdates()
                }
            }
            val duration = mediaPlayer!!.duration
            audioSeekBar.max = duration
            tvTotalTime.text = formatTime(duration)
            tvCurrentTime.text = formatTime(0)
            audioSeekBar.progress = 0
            
        } catch (e: Exception) {
            Toast.makeText(context, "Error al cargar audio: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSeekBarListener() {
        audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    tvCurrentTime.text = formatTime(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (mediaPlayer?.isPlaying == true) {
                    stopProgressUpdates()
                }
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (mediaPlayer?.isPlaying == true) {
                    startPlaybackProgressUpdate()
                }
            }
        })
    }

    private fun startPlaybackProgressUpdate() {
        stopProgressUpdates()
        updateProgressRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        val current = it.currentPosition
                        audioSeekBar.progress = current
                        tvCurrentTime.text = formatTime(current)
                        handler.postDelayed(this, 100)
                    }
                }
            }
        }
        handler.post(updateProgressRunnable!!)
    }

    private fun startRecordingProgressUpdate() {
        stopProgressUpdates()
        audioSeekBar.max = 300000 // 5 minutos max (referencia, se puede ajustar)
        tvTotalTime.text = "--:--"
        updateProgressRunnable = object : Runnable {
            override fun run() {
                if (isRecording) {
                    val elapsed = (System.currentTimeMillis() - recordStartTime).toInt()
                    audioSeekBar.progress = elapsed
                    tvCurrentTime.text = formatTime(elapsed)
                    handler.postDelayed(this, 100)
                }
            }
        }
        handler.post(updateProgressRunnable!!)
    }

    private fun stopProgressUpdates() {
        updateProgressRunnable?.let { handler.removeCallbacks(it) }
        updateProgressRunnable = null
    }

    private fun formatTime(millis: Int): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun togglePlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                stopProgressUpdates()
            } else {
                it.start()
                startPlaybackProgressUpdate()
            }
            updatePlayButtonText()
        }
    }

    private fun updatePlayButtonText() {
        mediaPlayer?.let {
            playAudioButton.text = if (it.isPlaying) "⏸️ Pause" else "▶️ Play"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}