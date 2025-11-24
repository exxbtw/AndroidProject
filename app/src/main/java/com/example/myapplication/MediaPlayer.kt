package com.example.myapplication

import android.Manifest
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.graphics.Color
import android.net.Uri
import java.io.File

class MediaPlayerActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTrack: TextView
    private lateinit var tvCurrentTime: TextView
    private lateinit var Duration: TextView
    private lateinit var playPauseButton: Button

    private val handler = Handler(Looper.getMainLooper())
    private val log_tag = "MusicPlayer"
    private val musicList = mutableListOf<Pair<String, Uri>>()
    private var currentTrackIndex = 0
    private var firstLaunch = true



    private val updateRunnable = object : Runnable {
        override fun run() {
            seekBar.progress = mediaPlayer.currentPosition
            val currentSeconds = mediaPlayer.currentPosition / 1000
            tvCurrentTime.text = String.format("%d:%02d", currentSeconds / 60, currentSeconds % 60)
            handler.postDelayed(this, 500)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val granted = results.entries.all { it.value }
            if (granted) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()
                loadMusicFromStorage()
            } else {
                Toast.makeText(this, "Please grant permission", Toast.LENGTH_LONG).show()
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        handler.removeCallbacks(updateRunnable)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_media_player)

        //mediaPlayer = MediaPlayer.create(this, R.raw.song)
        mediaPlayer = MediaPlayer()

        mediaPlayer.setOnCompletionListener {
            if (musicList.isNotEmpty()) {
                currentTrackIndex = (currentTrackIndex + 1) % musicList.size
                playTrack(currentTrackIndex)
            }
        }

        playPauseButton = findViewById(R.id.btnPlayPause)
        val nextButton: Button = findViewById(R.id.btnNext)
        val prevButton: Button = findViewById(R.id.btnPrev)
        tvCurrentTime = findViewById(R.id.tvCurrentTime)
        Duration = findViewById(R.id.Duration)
        val shuffleButton: Button = findViewById(R.id.btnShuffle)
        val sortButton: Button = findViewById(R.id.btnSort)
        val volumeSeekBar: SeekBar = findViewById(R.id.volumeSeekBar)
        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)


        volumeSeekBar.max = maxVolume
        volumeSeekBar.progress = currentVolume
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(
                        android.media.AudioManager.STREAM_MUSIC,
                        progress,
                        0
                    )
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        sortButton.setOnClickListener {
            if (musicList.isEmpty()) {
                Toast.makeText(this, "Нет треков для сортировки", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            musicList.sortBy { it.first.lowercase() }
            currentTrackIndex = 0
            playTrack(currentTrackIndex)
            Toast.makeText(this, "Отсортировано", Toast.LENGTH_SHORT).show()
        }

        shuffleButton.setOnClickListener {
            if (musicList.isNotEmpty()) {
                musicList.shuffle()
                currentTrackIndex = 0
                playTrack(currentTrackIndex)
                Toast.makeText(this, "Треки перемешаны", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Нет доступных треков", Toast.LENGTH_SHORT).show()
            }
        }

        val listTracks: ListView = findViewById(R.id.listTracks)

        val adapter = ArrayAdapter<String>(
            this, android.R.layout.simple_list_item_1, mutableListOf()
        )
        listTracks.adapter = adapter
        loadMusicFromStorage()
        if (musicList.isNotEmpty()) {
            adapter.clear()
            adapter.addAll(musicList.map { it.first })
            adapter.notifyDataSetChanged()
        }
        listTracks.setOnItemClickListener { _, _, position, _ ->
            currentTrackIndex = position
            playTrack(position)
        }


        playPauseButton.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                playPauseButton.text = "Paused"
                playPauseButton.setBackgroundColor(Color.parseColor("#B0C4DE"))
            } else {
                if (musicList.isNotEmpty()) {
                    if (mediaPlayer.currentPosition == 0) {
                        playTrack(currentTrackIndex)
                    } else {
                        mediaPlayer.start()
                    }
                    playPauseButton.text = "Playing"
                    playPauseButton.setBackgroundColor(Color.parseColor("#73AFF0"))
                } else {
                    Toast.makeText(this, "Нет доступных треков", Toast.LENGTH_SHORT).show()
                }
            }
        }
        nextButton.setOnClickListener {
            if (musicList.isNotEmpty()) {
                currentTrackIndex = (currentTrackIndex + 1) % musicList.size
                playTrack(currentTrackIndex)
            }
        }

        prevButton.setOnClickListener {
            if (musicList.isNotEmpty()) {
                currentTrackIndex = if (currentTrackIndex - 1 < 0) musicList.size - 1 else currentTrackIndex - 1
                playTrack(currentTrackIndex)
            }
        }

        seekBar = findViewById(R.id.seekBar)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer.seekTo(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })


        requestPermissionLauncher.launch(
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        )

        loadMusicFromStorage()

        tvCurrentTrack = findViewById(R.id.CurrentTrack)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun playTrack(index: Int) {
        if (index !in musicList.indices) return

        val track = musicList[index]

        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(this, track.second) //2 - URI
            mediaPlayer.prepare()

            val totalSeconds = mediaPlayer.duration / 1000
            Duration.text = String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60, totalSeconds)

            mediaPlayer.start()
            tvCurrentTrack.text = track.first.substringBeforeLast('.')
            seekBar.max = mediaPlayer.duration
            handler.post(updateRunnable)
        } catch (e: Exception) {
            Log.e(log_tag, "Ошибка воспроизведения", e)
        }
    }

    private fun loadMusicFromStorage() {
        musicList.clear()

        val musicDir = File(Environment.getExternalStorageDirectory(), "Music")

        if (musicDir.exists() && musicDir.isDirectory) {
            musicDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".mp3", true)) {
                    val uri = Uri.fromFile(file)
                    musicList.add(file.nameWithoutExtension to uri)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!firstLaunch && !mediaPlayer.isPlaying && currentTrackIndex >= 0) {
            mediaPlayer.start()
        }
        firstLaunch = false
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }
}


