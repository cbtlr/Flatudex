package com.example.ui.components

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AcousticBlue
import com.example.ui.theme.BioTeal80
import kotlin.math.PI
import kotlin.math.sin

object SoundPlayer {
    fun playTone(freqHz: Int, durationSeconds: Double, waveform: String) {
        Thread {
            try {
                val sampleRate = 44100
                val totalSamples = (sampleRate * durationSeconds.coerceIn(0.2, 4.0)).toInt()
                val buffer = ShortArray(totalSamples)

                for (i in 0 until totalSamples) {
                    val time = i.toDouble() / sampleRate
                    // Apply envelope to avoid clicking
                    val envelope = when {
                        time < 0.05 -> time / 0.05
                        time > durationSeconds - 0.05 -> (durationSeconds - time).coerceAtLeast(0.0) / 0.05
                        else -> 1.0
                    }

                    val sampleValue = when (waveform) {
                        "Staccato Flap" -> {
                            // Amplitude modulated flap
                            val mod = (sin(2 * PI * 8 * time) + 1.0) / 2.0
                            sin(2 * PI * freqHz * time) * mod
                        }
                        "Rising Whistle" -> {
                            val currentFreq = freqHz + (time * 150)
                            sin(2 * PI * currentFreq * time)
                        }
                        "Explosive Pop" -> {
                            val decay = kotlin.math.exp(-time * 12.0)
                            sin(2 * PI * freqHz * time) * decay
                        }
                        "Bubbling Tremolo" -> {
                            val tremor = sin(2 * PI * 14 * time) * 0.4
                            sin(2 * PI * (freqHz + tremor * 40) * time)
                        }
                        "Silent" -> 0.0
                        else -> { // Continuous Drone
                            sin(2 * PI * freqHz * time) + 0.3 * sin(2 * PI * (freqHz * 2) * time)
                        }
                    }

                    buffer[i] = (sampleValue * envelope * 0.75 * Short.MAX_VALUE).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
            } catch (e: Exception) {
                // Audio synthesis optional; handle gracefully
            }
        }.start()
    }
}

@Composable
fun AnimatedWaveformVisualizer(
    pitchHz: Int,
    decibels: Int,
    waveform: String,
    modifier: Modifier = Modifier
) {
    val phaseAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        phaseAnim.animateTo(
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val amplitude = (decibels / 100f).coerceIn(0.15f, 0.9f) * (height / 2.5f)
            val cycles = (pitchHz / 120f).coerceIn(1.5f, 6f)

            val path = Path()
            path.moveTo(0f, centerY)

            val points = 120
            for (i in 0..points) {
                val x = (i.toFloat() / points) * width
                val progress = (i.toFloat() / points) * cycles * 2f * PI.toFloat() + phaseAnim.value

                val yOffset = when (waveform) {
                    "Staccato Flap" -> {
                        val mod = (sin(progress * 0.25f) + 1f) / 2f
                        sin(progress) * amplitude * mod
                    }
                    "Explosive Pop" -> {
                        val normX = i.toFloat() / points
                        val popMod = kotlin.math.exp(-normX * 3.5f).toFloat()
                        sin(progress * 1.5f) * amplitude * popMod
                    }
                    "Rising Whistle" -> {
                        val chirp = progress * (1f + (i.toFloat() / points))
                        sin(chirp) * (amplitude * 0.7f)
                    }
                    "Silent" -> {
                        sin(progress) * 4f
                    }
                    else -> {
                        sin(progress) * amplitude
                    }
                }

                path.lineTo(x, centerY + yOffset)
            }

            // Draw glowing gradient line
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(BioTeal80, AcousticBlue, Color(0xFFA855F7), BioTeal80)
                ),
                style = Stroke(width = 3.5.dp.toPx())
            )

            // Draw center baseline
            drawLine(
                color = Color.White.copy(alpha = 0.15f),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}
