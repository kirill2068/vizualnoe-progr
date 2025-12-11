package com.example.visual

import android.Manifest.permission.READ_MEDIA_AUDIO
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MediaActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var listView: ListView
    private lateinit var btnPlay: Button
    private lateinit var seekBar: SeekBar
    private lateinit var volumeBar: SeekBar
    private lateinit var tvCurrent: TextView
    private lateinit var tvTotal: TextView

    private val tracks = mutableListOf<String>()
    private var currentTrack = -1
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_media)

        listView = findViewById(R.id.listViewTracks)
        btnPlay = findViewById(R.id.btnPlay)
        seekBar = findViewById(R.id.seekBarProgress)
        volumeBar = findViewById(R.id.seekBarVolume)
        tvCurrent = findViewById(R.id.tvCurrentPosition)
        tvTotal = findViewById(R.id.tvTotalDuration)

            val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()
                loadTracks()
            } else {
                Toast.makeText(this, "Please grant permission", Toast.LENGTH_LONG).show()
            }
        }
        requestPermissionLauncher.launch(READ_MEDIA_AUDIO)

        btnPlay.setOnClickListener {
            if (currentTrack != -1 && ::mediaPlayer.isInitialized) {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                    btnPlay.text = "Играть"
                } else {
                    mediaPlayer.start()
                    btnPlay.text = "Пауза"
                    updateProgress()
                }
            }
        }

        volumeBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val volume = progress / 100f
                    mediaPlayer.setVolume(volume, volume)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && ::mediaPlayer.isInitialized) {
                    mediaPlayer.seekTo(progress)
                    tvCurrent.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun loadTracks() {
        var musicPath: String = Environment.getExternalStorageDirectory().path + "/Music"

        var directory: File = File(musicPath)
        directory.listFiles()?.forEach {
            if (it.isFile && it.extension.lowercase() in listOf("mp3", "wav", "m4a")) {
                tracks.add(it.name)
            }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, tracks)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            playTrack(position)
        }
    }

    private fun playTrack(index: Int) {
        if (::mediaPlayer.isInitialized) mediaPlayer.release()
        val musicDir = File(Environment.getExternalStorageDirectory().path + "/Music")
        val file = File(musicDir, tracks[index])

        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
        }

        currentTrack = index
        seekBar.max = mediaPlayer.duration
        seekBar.progress = 0
        tvTotal.text = formatTime(mediaPlayer.duration)
        tvCurrent.text = formatTime(0)

        btnPlay.text = "Пауза"
        updateProgress()
    }

    private fun updateProgress() {
        if (::mediaPlayer.isInitialized && mediaPlayer.isPlaying) {
            val pos = mediaPlayer.currentPosition
            seekBar.progress = pos
            tvCurrent.text = formatTime(pos)

            updateRunnable?.let { handler.removeCallbacks(it) }

            updateRunnable = Runnable { updateProgress() }

            handler.postDelayed(updateRunnable!!, 1000)
        }
    }

    private fun formatTime(ms: Int): String {
        val s = ms / 1000
        val min = s / 60
        val sec = s % 60
        return String.format("%02d:%02d", min, sec)
    }

    override fun onDestroy() {
        super.onDestroy()
        updateRunnable?.let { handler.removeCallbacks(it) }
        if (::mediaPlayer.isInitialized) mediaPlayer.release()
    }
}