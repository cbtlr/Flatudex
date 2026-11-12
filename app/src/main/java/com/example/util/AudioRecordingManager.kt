package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAG = "AudioRecorderMgr"

data class RecordedSpecimen(
    val file: File,
    val wavBytes: ByteArray,
    val durationSeconds: Double,
    val peakDecibels: Int,
    val meanDecibels: Int,
    val fundamentalPitchHz: Int,
    val detectedWaveform: String,
    val detectedResonance: String
)

enum class RecordingStatus {
    IDLE,
    RECORDING,
    ANALYZING,
    COMPLETED,
    ERROR
}

class AudioRecordingManager(private val context: Context) {

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val _status = MutableStateFlow(RecordingStatus.IDLE)
    val status: StateFlow<RecordingStatus> = _status.asStateFlow()

    private val _currentDecibels = MutableStateFlow(30)
    val currentDecibels: StateFlow<Int> = _currentDecibels.asStateFlow()

    private val _currentAmplitudeNormalized = MutableStateFlow(0f)
    val currentAmplitudeNormalized: StateFlow<Float> = _currentAmplitudeNormalized.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0.0)
    val elapsedSeconds: StateFlow<Double> = _elapsedSeconds.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _lastRecording = MutableStateFlow<RecordedSpecimen?>(null)
    val lastRecording: StateFlow<RecordedSpecimen?> = _lastRecording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var timerJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private val pcmDataStream = ByteArrayOutputStream()

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startRecording(coroutineScope: CoroutineScope) {
        if (!hasRecordPermission()) {
            _errorMessage.value = "Microphone permission is required to capture acoustic recordings."
            _status.value = RecordingStatus.ERROR
            return
        }

        stopPlayback()
        _errorMessage.value = null
        _lastRecording.value = null
        pcmDataStream.reset()
        _elapsedSeconds.value = 0.0
        _currentDecibels.value = 30
        _currentAmplitudeNormalized.value = 0f

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                _errorMessage.value = "Failed to initialize microphone hardware."
                _status.value = RecordingStatus.ERROR
                return
            }

            audioRecord?.startRecording()
            _status.value = RecordingStatus.RECORDING

            // Timer coroutine (max 10 seconds auto-cutoff)
            val startTime = System.currentTimeMillis()
            timerJob = coroutineScope.launch(Dispatchers.Default) {
                while (isActive && _status.value == RecordingStatus.RECORDING) {
                    val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                    _elapsedSeconds.value = (elapsed * 10).roundToInt() / 10.0
                    if (elapsed >= 10.0) {
                        withContext(Dispatchers.Main) {
                            stopRecording(coroutineScope)
                        }
                        break
                    }
                    delay(50)
                }
            }

            // Audio sampling loop
            recordingJob = coroutineScope.launch(Dispatchers.IO) {
                val buffer = ShortArray(bufferSize / 2)
                val byteBuffer = ByteBuffer.allocate(bufferSize).order(ByteOrder.LITTLE_ENDIAN)

                while (isActive && _status.value == RecordingStatus.RECORDING) {
                    val readSamples = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readSamples > 0) {
                        byteBuffer.clear()
                        var sumSquare = 0.0
                        var maxAmp = 0

                        for (i in 0 until readSamples) {
                            val sample = buffer[i]
                            byteBuffer.putShort(sample)
                            sumSquare += (sample.toDouble() * sample.toDouble())
                            val abs = kotlin.math.abs(sample.toInt())
                            if (abs > maxAmp) maxAmp = abs
                        }

                        synchronized(pcmDataStream) {
                            pcmDataStream.write(byteBuffer.array(), 0, readSamples * 2)
                        }

                        val rms = sqrt(sumSquare / readSamples)
                        // Convert RMS to an estimated dB scale calibrated for mobile mic
                        val db = if (rms > 1.0) {
                            (20.0 * log10(rms / 32767.0) + 95.0).roundToInt().coerceIn(25, 105)
                        } else {
                            25
                        }
                        val normAmp = (maxAmp / 32767.0f).coerceIn(0f, 1f)

                        _currentDecibels.value = db
                        _currentAmplitudeNormalized.value = normAmp
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio recording startup exception", e)
            _errorMessage.value = e.localizedMessage ?: "Failed to start recording."
            _status.value = RecordingStatus.ERROR
        }
    }

    fun stopRecording(
        coroutineScope: CoroutineScope,
        onComplete: ((RecordedSpecimen) -> Unit)? = null
    ) {
        if (_status.value != RecordingStatus.RECORDING) return

        _status.value = RecordingStatus.ANALYZING
        timerJob?.cancel()
        timerJob = null

        coroutineScope.launch(Dispatchers.IO) {
            try {
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
            } catch (e: Exception) {
                Log.w(TAG, "Exception releasing AudioRecord", e)
            }

            recordingJob?.cancel()
            recordingJob = null

            val pcmBytes = synchronized(pcmDataStream) { pcmDataStream.toByteArray() }
            if (pcmBytes.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "No audio data recorded. Please try again."
                    _status.value = RecordingStatus.ERROR
                }
                return@launch
            }

            // Convert raw PCM to standard WAV
            val wavBytes = createWavFile(pcmBytes, sampleRate, 1, 16)
            val outputFile = File(context.cacheDir, "specimen_${System.currentTimeMillis()}.wav")
            FileOutputStream(outputFile).use { it.write(wavBytes) }

            // Biophysical analysis of PCM signal
            val totalDurationSec = (pcmBytes.size / 2.0 / sampleRate).coerceIn(0.2, 10.0)
            val durationRounded = (totalDurationSec * 10).roundToInt() / 10.0

            val (peakDb, meanDb) = computeDecibelsFromPcm(pcmBytes)
            val pitchHz = computeFundamentalPitch(pcmBytes, sampleRate)
            val waveform = classifyWaveformFromPcm(pcmBytes, sampleRate, durationRounded, peakDb, pitchHz)
            val resonance = classifyResonanceFromPcm(pitchHz, peakDb, waveform)

            val specimen = RecordedSpecimen(
                file = outputFile,
                wavBytes = wavBytes,
                durationSeconds = durationRounded,
                peakDecibels = peakDb,
                meanDecibels = meanDb,
                fundamentalPitchHz = pitchHz,
                detectedWaveform = waveform,
                detectedResonance = resonance
            )

            withContext(Dispatchers.Main) {
                _lastRecording.value = specimen
                _status.value = RecordingStatus.COMPLETED
                _currentAmplitudeNormalized.value = 0f
                onComplete?.invoke(specimen)
            }
        }
    }

    fun playRecordedAudio() {
        val specimen = _lastRecording.value ?: return
        if (!specimen.file.exists()) return

        stopPlayback()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(specimen.file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    stopPlayback()
                }
                start()
                _isPlaying.value = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio playback error", e)
            _isPlaying.value = false
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "MediaPlayer release error", e)
        }
        mediaPlayer = null
        _isPlaying.value = false
    }

    fun release() {
        stopPlayback()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioRecord = null
        recordingJob?.cancel()
        timerJob?.cancel()
    }

    // Helper: Wrap raw 16-bit PCM in standard 44-byte RIFF WAV header
    private fun createWavFile(pcmData: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * (bitsPerSample / 8)

        val header = ByteArray(44)
        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        // WAVE
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // 'fmt ' chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte() // block align
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        // 'data' chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        val wavStream = ByteArrayOutputStream(header.size + pcmData.size)
        wavStream.write(header)
        wavStream.write(pcmData)
        return wavStream.toByteArray()
    }

    private fun computeDecibelsFromPcm(pcmBytes: ByteArray): Pair<Int, Int> {
        val totalSamples = pcmBytes.size / 2
        if (totalSamples == 0) return Pair(40, 35)

        val windowSize = sampleRate / 10 // 100ms window
        var maxRms = 0.0
        var totalSquareSum = 0.0

        var i = 0
        while (i < totalSamples) {
            val windowEnd = kotlin.math.min(i + windowSize, totalSamples)
            var windowSum = 0.0
            val count = windowEnd - i

            for (j in i until windowEnd) {
                val byteIdx = j * 2
                val low = pcmBytes[byteIdx].toInt() and 0xFF
                val high = pcmBytes[byteIdx + 1].toInt()
                val sample = ((high shl 8) or low).toShort()
                val sampleSq = sample.toDouble() * sample.toDouble()
                windowSum += sampleSq
                totalSquareSum += sampleSq
            }

            val windowRms = sqrt(windowSum / count)
            if (windowRms > maxRms) maxRms = windowRms

            i += windowSize
        }

        val overallRms = sqrt(totalSquareSum / totalSamples)

        val peakDb = if (maxRms > 1.0) {
            (20.0 * log10(maxRms / 32767.0) + 95.0).roundToInt().coerceIn(25, 105)
        } else {
            30
        }

        val meanDb = if (overallRms > 1.0) {
            (20.0 * log10(overallRms / 32767.0) + 95.0).roundToInt().coerceIn(20, peakDb)
        } else {
            25
        }

        return Pair(peakDb, meanDb)
    }

    // Autocorrelation pitch detection on the highest energy segment
    private fun computeFundamentalPitch(pcmBytes: ByteArray, sampleRate: Int): Int {
        val totalSamples = pcmBytes.size / 2
        if (totalSamples < 2048) return 130

        // Find the window with highest energy
        val windowSize = (sampleRate * 0.25).toInt().coerceAtMost(totalSamples) // 250ms
        var maxEnergyIdx = 0
        var maxEnergy = 0.0

        val step = sampleRate / 20 // 50ms hop
        var cursor = 0
        while (cursor + windowSize <= totalSamples) {
            var energy = 0.0
            for (k in 0 until windowSize step 4) {
                val byteIdx = (cursor + k) * 2
                val low = pcmBytes[byteIdx].toInt() and 0xFF
                val high = pcmBytes[byteIdx + 1].toInt()
                val sample = ((high shl 8) or low).toDouble()
                energy += sample * sample
            }
            if (energy > maxEnergy) {
                maxEnergy = energy
                maxEnergyIdx = cursor
            }
            cursor += step
        }

        // Extract samples from highest energy window
        val segment = DoubleArray(windowSize)
        for (k in 0 until windowSize) {
            val byteIdx = (maxEnergyIdx + k) * 2
            val low = pcmBytes[byteIdx].toInt() and 0xFF
            val high = pcmBytes[byteIdx + 1].toInt()
            segment[k] = ((high shl 8) or low).toDouble()
        }

        // Search lags corresponding to 50 Hz - 700 Hz
        val minLag = (sampleRate / 700.0).toInt().coerceAtLeast(2)
        val maxLag = (sampleRate / 50.0).toInt().coerceAtMost(windowSize / 2)

        var bestLag = 0
        var maxCorrelation = -1.0

        for (lag in minLag..maxLag) {
            var correlation = 0.0
            val limit = windowSize - lag
            for (n in 0 until limit step 2) {
                correlation += segment[n] * segment[n + lag]
            }
            if (correlation > maxCorrelation) {
                maxCorrelation = correlation
                bestLag = lag
            }
        }

        if (bestLag > 0) {
            val detectedHz = (sampleRate / bestLag.toDouble()).roundToInt()
            return detectedHz.coerceIn(50, 650)
        }

        return 125
    }

    private fun classifyWaveformFromPcm(
        pcmBytes: ByteArray,
        sampleRate: Int,
        durationSec: Double,
        peakDb: Int,
        pitchHz: Int
    ): String {
        if (peakDb <= 35) return "Silent"
        if (durationSec <= 0.4 && peakDb >= 75) return "Explosive Pop"
        if (pitchHz >= 380) return "Rising Whistle"

        // Count envelope oscillations to check for staccato flaps
        val totalSamples = pcmBytes.size / 2
        val subWindow = sampleRate / 20 // 50ms chunks
        val chunkEnergies = mutableListOf<Double>()

        var idx = 0
        while (idx + subWindow <= totalSamples) {
            var sum = 0.0
            for (k in 0 until subWindow step 4) {
                val byteIdx = (idx + k) * 2
                val low = pcmBytes[byteIdx].toInt() and 0xFF
                val high = pcmBytes[byteIdx + 1].toInt()
                val sample = ((high shl 8) or low).toDouble()
                sum += sample * sample
            }
            chunkEnergies.add(sqrt(sum / subWindow))
            idx += subWindow
        }

        var peakTransitions = 0
        for (i in 1 until chunkEnergies.size - 1) {
            if (chunkEnergies[i] > chunkEnergies[i - 1] && chunkEnergies[i] > chunkEnergies[i + 1]) {
                if (chunkEnergies[i] > 1000.0) {
                    peakTransitions++
                }
            }
        }

        if (peakTransitions >= 3 && durationSec >= 0.6) {
            return "Staccato Flap"
        }

        if (pitchHz < 90 && peakDb in 50..75) {
            return "Bubbling Tremolo"
        }

        return "Continuous Drone"
    }

    private fun classifyResonanceFromPcm(pitchHz: Int, peakDb: Int, waveform: String): String {
        return when {
            waveform == "Explosive Pop" -> "Sharp"
            pitchHz < 100 && peakDb >= 75 -> "Hollow"
            pitchHz in 100..180 -> "Echoey"
            pitchHz > 300 -> "Tight"
            peakDb < 50 -> "Muffled"
            else -> "Echoey"
        }
    }
}
