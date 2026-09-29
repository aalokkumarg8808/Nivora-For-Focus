package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

object SoundscapeManager {
    private const val SAMPLE_RATE = 44100
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val availableSoundscapes = listOf("Forest", "Rain", "Ocean Waves", "Cafe", "Deep Space", "None")

    private val _currentSoundscape = MutableStateFlow("Forest")
    val currentSoundscape: StateFlow<String> = _currentSoundscape.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    @Volatile
    private var currentVolFloat: Float = 0.7f
    private val _volume = MutableStateFlow(0.7f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val random = Random()

    fun play(soundscapeName: String) {
        val trimmed = soundscapeName.trim()
        if (trimmed.equals("None", ignoreCase = true) || trimmed.isBlank()) {
            stop()
            _currentSoundscape.value = "None"
            return
        }

        _currentSoundscape.value = trimmed
        _isPlaying.value = true

        if (playbackJob?.isActive == true && audioTrack != null && audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
            // Target scene updated in flow, loop picks it up immediately
            return
        }

        startPlayback(trimmed)
    }

    fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun resume() {
        val current = _currentSoundscape.value
        if (current != "None" && !_isPlaying.value) {
            _isPlaying.value = true
            startPlayback(current)
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            val scene = if (_currentSoundscape.value == "None") "Forest" else _currentSoundscape.value
            play(scene)
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        currentVolFloat = clamped
        _volume.value = clamped
        try {
            audioTrack?.setVolume(clamped)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun stop() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioTrack = null
    }

    private fun startPlayback(sceneName: String) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)

                // Clean up previous track safely
                audioTrack?.let {
                    try {
                        it.stop()
                        it.release()
                    } catch (e: Exception) {
                        // Ignore
                    }
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.setVolume(currentVolFloat)
                track.play()

                val audioBuffer = ShortArray(bufferSize)

                // State variables for procedural sound synthesis
                var sampleIndex = 0L
                var phase1 = 0.0
                var phase2 = 0.0
                var phase3 = 0.0
                var phase4 = 0.0
                var filterState1 = 0.0
                var filterState2 = 0.0

                // Rain drop impulse generator state
                var dropPhase = 0.0
                var dropDecay = 0.0
                var dropFreq = 1200.0

                // Bird song state
                var birdTime = 0.0
                var birdFreq = 2400.0
                var isChirping = false
                var chirpPhase = 0.0
                var chirpLength = 0.0

                // Cafe chatter state
                var clinkDecay = 0.0
                var clinkFreq = 3200.0

                // Pink noise generator filters (Paul Kellet's algorithm)
                var b0 = 0.0
                var b1 = 0.0
                var b2 = 0.0
                var b3 = 0.0
                var b4 = 0.0
                var b5 = 0.0
                var b6 = 0.0

                while (isActive && _isPlaying.value) {
                    val targetScene = _currentSoundscape.value
                    if (targetScene == "None") {
                        break
                    }

                    val vol = currentVolFloat

                    for (i in audioBuffer.indices) {
                        sampleIndex++
                        val timeSeconds = sampleIndex.toDouble() / SAMPLE_RATE

                        // Base white noise
                        val white = random.nextDouble() * 2.0 - 1.0

                        // Generate pink noise
                        b0 = 0.99886 * b0 + white * 0.0555179
                        b1 = 0.99332 * b1 + white * 0.0750759
                        b2 = 0.96900 * b2 + white * 0.1538520
                        b3 = 0.86650 * b3 + white * 0.3104856
                        b4 = 0.55000 * b4 + white * 0.5329522
                        b5 = -0.7616 * b5 - white * 0.0168980
                        val pink = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362) * 0.11
                        b6 = white * 0.115926

                        val rawSample = when (targetScene) {
                            "Rain" -> {
                                // Continuous soothing rainfall bed
                                filterState1 = filterState1 * 0.88 + pink * 0.12
                                val rainBed = filterState1 * 0.45

                                // Pattering raindrop droplets
                                if (dropDecay <= 0.0) {
                                    if (random.nextDouble() < 0.0025) {
                                        dropDecay = 1.0
                                        dropFreq = 800.0 + random.nextDouble() * 1600.0
                                        dropPhase = 0.0
                                    }
                                }

                                var dropSound = 0.0
                                if (dropDecay > 0.0) {
                                    dropPhase += (2.0 * Math.PI * dropFreq) / SAMPLE_RATE
                                    dropSound = sin(dropPhase) * dropDecay * 0.18
                                    dropDecay -= 0.004
                                }

                                (rainBed + dropSound) * 0.85
                            }

                            "Forest" -> {
                                // Wind rustling through leaves with natural slow swells
                                val windSwell = (sin(2.0 * Math.PI * timeSeconds / 5.2) + 1.0) * 0.5
                                filterState1 = filterState1 * 0.95 + pink * 0.05
                                val wind = filterState1 * (0.12 + 0.18 * windSwell)

                                // Occasional authentic chirping birds
                                birdTime += 1.0 / SAMPLE_RATE
                                if (!isChirping && birdTime > 3.2 && random.nextDouble() < 0.0006) {
                                    isChirping = true
                                    birdTime = 0.0
                                    chirpLength = 0.18 + random.nextDouble() * 0.12
                                    birdFreq = 2200.0 + random.nextDouble() * 800.0
                                    chirpPhase = 0.0
                                }

                                var birdSound = 0.0
                                if (isChirping) {
                                    val progress = (chirpPhase / (2.0 * Math.PI * birdFreq / SAMPLE_RATE)) / (chirpLength * SAMPLE_RATE)
                                    val currentFreq = birdFreq + sin(progress * Math.PI * 4.0) * 400.0
                                    chirpPhase += (2.0 * Math.PI * currentFreq) / SAMPLE_RATE
                                    val env = sin(progress.coerceIn(0.0, 1.0) * Math.PI)
                                    birdSound = sin(chirpPhase) * env * 0.14

                                    if (progress >= 1.0) {
                                        isChirping = false
                                    }
                                }

                                (wind + birdSound) * 0.85
                            }

                            "Ocean Waves" -> {
                                // Rhythmic oceanic rolling swells
                                val waveCycle = (timeSeconds % 6.8) / 6.8
                                val swellEnv = if (waveCycle < 0.55) {
                                    sin(waveCycle / 0.55 * Math.PI * 0.5)
                                } else {
                                    cos((waveCycle - 0.55) / 0.45 * Math.PI * 0.5)
                                }

                                filterState1 = filterState1 * 0.91 + pink * 0.09
                                filterState2 = filterState2 * 0.98 + white * 0.02
                                val foam = filterState1 * (0.04 + 0.32 * swellEnv)
                                val deepRumble = filterState2 * (0.06 + 0.20 * swellEnv)

                                (foam + deepRumble) * 0.9
                            }

                            "Cafe" -> {
                                // Warm acoustic background murmur
                                filterState1 = filterState1 * 0.86 + pink * 0.14
                                val murmur = filterState1 * 0.24

                                // Occasional porcelain cup & saucer clink
                                if (clinkDecay <= 0.0) {
                                    if (random.nextDouble() < 0.00015) {
                                        clinkDecay = 1.0
                                        clinkFreq = 2800.0 + random.nextDouble() * 600.0
                                        phase4 = 0.0
                                    }
                                }

                                var clinkSound = 0.0
                                if (clinkDecay > 0.0) {
                                    phase4 += (2.0 * Math.PI * clinkFreq) / SAMPLE_RATE
                                    clinkSound = (sin(phase4) * 0.6 + sin(phase4 * 1.5) * 0.4) * clinkDecay * 0.12
                                    clinkDecay -= 0.0008
                                }

                                (murmur + clinkSound) * 0.8
                            }

                            "Deep Space" -> {
                                // Deep soothing alpha harmonic binaural drone
                                phase1 += (2.0 * Math.PI * 54.0) / SAMPLE_RATE
                                phase2 += (2.0 * Math.PI * 108.0) / SAMPLE_RATE
                                phase3 += (2.0 * Math.PI * 216.0) / SAMPLE_RATE
                                val cosmicMod = (sin(2.0 * Math.PI * timeSeconds * 0.12) + 1.0) * 0.5

                                val drone = sin(phase1) * 0.15 +
                                        sin(phase2) * 0.12 +
                                        sin(phase3) * (0.04 + 0.04 * cosmicMod)

                                drone * 0.85
                            }

                            else -> 0.0
                        }

                        // Apply volume directly to PCM amplitude for instant attenuation down to 0%
                        val finalValue = (rawSample * vol).coerceIn(-1.0, 1.0)
                        audioBuffer[i] = (finalValue * 32767.0).toInt().toShort()
                    }

                    if (_isPlaying.value && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.write(audioBuffer, 0, audioBuffer.size)
                    }
                }
            } catch (e: Exception) {
                // Playback interrupted or completed
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (e: Exception) {
                    // Ignore
                }
                audioTrack = null
            }
        }
    }
}
