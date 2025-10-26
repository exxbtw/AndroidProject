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
import java.io.File
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

class MediaPlayerActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTrack: TextView


    private val handler = Handler(Looper.getMainLooper())
    private val log_tag = "MusicPlayer"
    private val musicList = mutableListOf<Pair<String, Uri>>()
    private var currentTrackIndex = 0


    private val updateRunnable = object : Runnable {
        override fun run() {
            seekBar.progress = mediaPlayer.currentPosition
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

        val playButton: Button = findViewById(R.id.btnPlay)
        val pauseButton: Button = findViewById(R.id.btnPause)
        val nextButton: Button = findViewById(R.id.btnNext)
        val prevButton: Button = findViewById(R.id.btnPrev)

        val listTracks: ListView = findViewById(R.id.listTracks)
        val btnTrackList: Button = findViewById(R.id.btnTrackList)

        val adapter = ArrayAdapter<String>(
            this, android.R.layout.simple_list_item_1, mutableListOf())
        listTracks.adapter = adapter

        btnTrackList.setOnClickListener {
            if (musicList.isEmpty()) {
                Toast.makeText(this, "Музыка не найдена", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (listTracks.visibility == ListView.GONE) {
                adapter.clear()
                adapter.addAll(musicList.map { it.first.substringBeforeLast('.') })
                adapter.notifyDataSetChanged()
                listTracks.visibility = ListView.VISIBLE
                btnTrackList.text = "Скрыть список"
            } else {
                listTracks.visibility = ListView.GONE
                btnTrackList.text = "Список треков"
            }
        }
        listTracks.setOnItemClickListener { _, _, position, _ ->
            currentTrackIndex = position
            playTrack(position)
            listTracks.visibility = ListView.GONE
            btnTrackList.text = "Список треков"
        }


        playButton.setOnClickListener {
            if (!mediaPlayer.isPlaying) {
                if (mediaPlayer.currentPosition == 0 && musicList.isNotEmpty()) {
                    playTrack(currentTrackIndex)
                } else {
                    mediaPlayer.start()
                }
                playButton.setBackgroundColor(Color.parseColor("#73AFF0"))
                pauseButton.setBackgroundColor(Color.parseColor("#B0C4DE"))
            }
        }

        pauseButton.setOnClickListener {
            mediaPlayer.pause()
            pauseButton.setBackgroundColor(Color.parseColor("#73AFF0"))
            playButton.setBackgroundColor(Color.parseColor("#B0C4DE"))
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
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO
            )
        )

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
            mediaPlayer.setDataSource(this, track.second)
            mediaPlayer.prepare()
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

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DATA),
            "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataIndex)
                if (path?.contains("/Music/") == true) {
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        cursor.getLong(idIndex)
                    )
                    musicList.add(cursor.getString(nameIndex) to uri)
                }
            }
        }

        if (musicList.isEmpty()) {
            Toast.makeText(this, "Музыка не найдена", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }
}