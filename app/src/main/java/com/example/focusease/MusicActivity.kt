package com.example.focusease

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MusicActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnNotification: ImageView
    private lateinit var btnFavorite: ImageView
    private lateinit var btnPlayPause: ImageView
    private lateinit var btnPrevious: ImageView
    private lateinit var btnNext: ImageView
    private lateinit var btnRewind: ImageView
    private lateinit var btnForward: ImageView
    private lateinit var seekBarMusic: SeekBar
    private lateinit var cardStudyingMood: CardView
    private lateinit var cardDayMood: CardView
    private lateinit var tvPlaylistTitle: TextView
    private lateinit var ivAlbumArt: ImageView  // Tambahkan ini

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var isFavorite = false
    private val handler = Handler(Looper.getMainLooper())

    // Daftar musik
    private val musicList = listOf(
        R.raw.morning_music,
        R.raw.studying_mood,
        R.raw.day_mood
    )

    private val musicTitles = listOf(
        "Morning Playlist",
        "Studying Mood",
        "Day Mood"
    )

    // Daftar cover gambar untuk setiap lagu
    private val albumCovers = listOf(
        R.drawable.morning_music_cover,
        R.drawable.studying_mood_cover,
        R.drawable.day_mood_cover
    )

    private var currentMusicIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music)

        initViews()
        setupListeners()
        setupBottomNavigation()

        loadMusic(currentMusicIndex)
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnNotification = findViewById(R.id.btnNotification)
        btnFavorite = findViewById(R.id.btnFavorite)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        btnRewind = findViewById(R.id.btnRewind)
        btnForward = findViewById(R.id.btnForward)
        seekBarMusic = findViewById(R.id.seekBarMusic)
        cardStudyingMood = findViewById(R.id.cardStudyingMood)
        cardDayMood = findViewById(R.id.cardDayMood)
        tvPlaylistTitle = findViewById(R.id.tvPlaylistTitle)
        ivAlbumArt = findViewById(R.id.ivAlbumArt)  // Inisialisasi
    }

    private fun loadMusic(index: Int) {
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer.create(this, musicList[index])
        currentMusicIndex = index

        // Update judul
        tvPlaylistTitle.text = musicTitles[index]

        // Update gambar album art dengan animasi fade
        updateAlbumArtWithAnimation(albumCovers[index])

        seekBarMusic.max = mediaPlayer?.duration ?: 0

        mediaPlayer?.setOnCompletionListener {
            playNext()
        }

        isPlaying = false
        btnPlayPause.setImageResource(R.drawable.ic_play)

        // Reset favorite saat ganti lagu
        isFavorite = false
        btnFavorite.setImageResource(R.drawable.ic_heart)
    }

    private fun updateAlbumArtWithAnimation(coverResId: Int) {
        // Animasi fade out
        ivAlbumArt.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                // Ganti gambar
                ivAlbumArt.setImageResource(coverResId)

                // Animasi fade in
                ivAlbumArt.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnNotification.setOnClickListener {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show()
        }

        btnFavorite.setOnClickListener {
            toggleFavorite()
        }

        btnPlayPause.setOnClickListener {
            togglePlayPause()
        }

        btnPrevious.setOnClickListener {
            playPrevious()
        }

        btnNext.setOnClickListener {
            playNext()
        }

        btnRewind.setOnClickListener {
            rewind10Seconds()
        }

        btnForward.setOnClickListener {
            forward10Seconds()
        }

        seekBarMusic.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        cardStudyingMood.setOnClickListener {
            loadMusic(1)  // Index 1 = Studying Mood
            playMusic()   // Langsung play
        }

        cardDayMood.setOnClickListener {
            loadMusic(2)  // Index 2 = Day Mood
            playMusic()   // Langsung play
        }
    }

    private fun toggleFavorite() {
        isFavorite = !isFavorite

        if (isFavorite) {
            btnFavorite.setImageResource(R.drawable.ic_heart_filled)
            Toast.makeText(this, "Added to favorites ❤️", Toast.LENGTH_SHORT).show()
            animateHeart()
        } else {
            btnFavorite.setImageResource(R.drawable.ic_heart)
            Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
            animateHeartOut()
        }
    }

    private fun animateHeart() {
        val scaleX = ObjectAnimator.ofFloat(btnFavorite, "scaleX", 1f, 1.4f, 1f)
        val scaleY = ObjectAnimator.ofFloat(btnFavorite, "scaleY", 1f, 1.4f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 500
        animatorSet.interpolator = BounceInterpolator()
        animatorSet.start()
    }

    private fun animateHeartOut() {
        val scaleX = ObjectAnimator.ofFloat(btnFavorite, "scaleX", 1f, 0.8f, 1f)
        val scaleY = ObjectAnimator.ofFloat(btnFavorite, "scaleY", 1f, 0.8f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 300
        animatorSet.start()
    }

    private fun togglePlayPause() {
        if (isPlaying) {
            pauseMusic()
        } else {
            playMusic()
        }
    }

    private fun playMusic() {
        mediaPlayer?.start()
        isPlaying = true
        btnPlayPause.setImageResource(R.drawable.ic_pause)
        updateSeekBar()
    }

    private fun pauseMusic() {
        mediaPlayer?.pause()
        isPlaying = false
        btnPlayPause.setImageResource(R.drawable.ic_play)
    }

    private fun playPrevious() {
        val newIndex = if (currentMusicIndex > 0) currentMusicIndex - 1 else musicList.size - 1
        loadMusic(newIndex)
        if (isPlaying) {
            playMusic()
        }
    }

    private fun playNext() {
        val newIndex = if (currentMusicIndex < musicList.size - 1) currentMusicIndex + 1 else 0
        loadMusic(newIndex)
        if (isPlaying) {
            playMusic()
        }
    }

    private fun rewind10Seconds() {
        mediaPlayer?.let {
            val newPosition = (it.currentPosition - 10000).coerceAtLeast(0)
            it.seekTo(newPosition)
            Toast.makeText(this, "Rewind 10s", Toast.LENGTH_SHORT).show()
        }
    }

    private fun forward10Seconds() {
        mediaPlayer?.let {
            val newPosition = (it.currentPosition + 10000).coerceAtMost(it.duration)
            it.seekTo(newPosition)
            Toast.makeText(this, "Forward 10s", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isPlaying) {
                    seekBarMusic.progress = mediaPlayer?.currentPosition ?: 0
                    handler.postDelayed(this, 100)
                }
            }
        }, 100)
    }

    private fun setupBottomNavigation() {
        val navHome = findViewById<View>(R.id.navHome)
        val navMusic = findViewById<View>(R.id.navMusic)
        val navClock = findViewById<View>(R.id.navClock)
        val navProfile = findViewById<View>(R.id.navProfile)

        navHome?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            Log.d("NAV", "Home clicked")
        }

        navMusic?.setOnClickListener {
            Log.d("NAV", "Music clicked")
        }

        navClock?.setOnClickListener {
            val intent = Intent(this, ClockActivity::class.java)
            startActivity(intent)
            Log.d("NAV", "Clock clicked")
        }

        navProfile?.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            Log.d("NAV", "Profile clicked")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
    }

    override fun onPause() {
        super.onPause()
        if (isPlaying) {
            pauseMusic()
        }
    }
}