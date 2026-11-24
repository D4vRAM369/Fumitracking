package com.d4vram.fumitracking

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

            // Anillo de ese día. Metas = las actuales (no se guardan metas por día).
            val ctx = requireContext()
            val cbd = Prefs.getCbdCount(ctx, d)
            val thc = Prefs.getThcCount(ctx, d)
            val goals = ctx.getSharedPreferences(GoalsActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            val cbdGoal = goals.getInt(GoalsActivity.KEY_CBD_MAX, 0)
            val thcGoal = goals.getInt(GoalsActivity.KEY_THC_MAX, 0)
            val note = Prefs.getNote(ctx, d)
            view.findViewById<RingsView>(R.id.ringsDayView).setProgress(cbd, thc, cbdGoal, thcGoal, note)

            // Desglose por tipo con el color de su trozo. Antes 🟢 contaba como CBD;
            // ahora sale de dayMix, que lo cuenta como weed (THC), igual que el anillo.
            val mix = RingProgress.dayMix(cbd, thc, RingProgress.kindsFromNote(note))
            val breakdown = breakdownText(ctx, mix, cbdGoal)
            breakdownLabel.text = breakdown
            breakdownLabel.visibility = if (breakdown.isEmpty()) View.GONE else View.VISIBLE

            // Load note
            noteEditText.setText(Prefs.getNote(requireContext(), d) ?: "")

            // Check if audio exists
            audioFile = VoiceNoteRepository.existingFile(requireContext(), d)
            if (audioFile != null) {
                audioStatus.setText(R.string.voice_note_available)
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
                recordAudioButton.setText(R.string.voice_note_record_audio)
                audioStatus.setText(R.string.voice_note_stopped)
                
                // Si se guardó el archivo, cargarlo
                audioFile?.let { file ->
                    if (file.exists()) {
                        audioStatus.setText(R.string.voice_note_available)
                        seekBarContainer.visibility = View.VISIBLE
                        audioControlsLayout.visibility = View.VISIBLE
                        setupAudioPlayer(file)
                    }
                }
            } else {
                startRecording()
                recordAudioButton.setText(R.string.voice_note_stop_recording)
                audioStatus.setText(R.string.voice_note_recording_plain)
                seekBarContainer.visibility = View.VISIBLE
                audioControlsLayout.visibility = View.GONE
            }
        }



        playAudioButton.setOnClickListener {
            togglePlayback()
        }

        deleteAudioButton.setOnClickListener {
            date?.let { d -> VoiceNoteRepository.deleteAllForDate(requireContext(), d) }
            audioFile = null
            mediaPlayer?.release()
            mediaPlayer = null
            audioStatus.text = ""
            seekBarContainer.visibility = View.GONE
            audioControlsLayout.visibility = View.GONE
            Toast.makeText(context, getString(R.string.voice_note_deleted), Toast.LENGTH_SHORT).show()
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

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
            return
        }

        date?.let { d ->
            try {
                val audioFile = VoiceNoteRepository.outputFile(requireContext(), d)
                this.audioFile = audioFile
                // MediaRecorder(Context) existe desde Android 12 (API 31); minSdk es 26, así que antes se usa el constructor viejo.
                mediaRecorder = (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
                    android.media.MediaRecorder(requireContext())
                else @Suppress("DEPRECATION") android.media.MediaRecorder()).apply {
                    setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                    setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(audioFile.absolutePath)
                    prepare()
                    start()
                }
                isRecording = true
                recordStartTime = System.currentTimeMillis()
                startRecordingProgressUpdate()
                Toast.makeText(context, getString(R.string.voice_note_started), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    getString(R.string.voice_note_error_start_with_cause, e.message ?: "", e.cause ?: ""),
                    Toast.LENGTH_LONG
                ).show()
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
                Toast.makeText(context, getString(R.string.voice_note_recording_saved), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    getString(R.string.voice_note_error_stop, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                Toast.makeText(context, getString(R.string.voice_note_recording_denied), Toast.LENGTH_SHORT).show()
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
            Toast.makeText(
                context,
                getString(R.string.voice_note_error_load, e.message ?: ""),
                Toast.LENGTH_SHORT
            ).show()
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

    /** «🔹 3/5 · 🌿 2 · 🍫 1», cada parte del color de su trozo; las que valen 0 no salen. */
    private fun breakdownText(ctx: android.content.Context, mix: DayMix, cbdGoal: Int): CharSequence {
        val parts = buildList {
            if (mix.cbd > 0) add((if (cbdGoal > 0) "🔹 ${mix.cbd}/$cbdGoal" else "🔹 ${mix.cbd}") to R.color.ring_seg_cbd)
            if (mix.weed > 0) add("🌿 ${mix.weed}" to R.color.ring_seg_weed)
            if (mix.polen > 0) add("🍫 ${mix.polen}" to R.color.ring_seg_polen)
        }
        val out = android.text.SpannableStringBuilder()
        parts.forEachIndexed { i, (text, color) ->
            if (i > 0) out.append("  ·  ")
            val start = out.length
            out.append(text)
            out.setSpan(
                android.text.style.ForegroundColorSpan(ContextCompat.getColor(ctx, color)),
                start, out.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return out
    }
}
