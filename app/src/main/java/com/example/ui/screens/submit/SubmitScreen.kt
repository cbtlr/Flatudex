package com.example.ui.screens.submit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiAnalysisResult
import com.example.data.model.FlatusSubmission
import com.example.data.model.InferredSpecimenProperties
import com.example.data.model.SmellProfile
import com.example.data.model.SoundProfile
import com.example.data.model.SubmissionContext
import com.example.data.repository.AiTaxonomyRepository
import com.example.data.repository.TaxonomyRepository
import com.example.util.AudioRecordingManager
import com.example.util.RecordedSpecimen
import com.example.util.RecordingStatus
import com.example.ui.components.AnimatedWaveformVisualizer
import com.example.ui.components.SoundPlayer
import com.example.ui.theme.AcousticBlue
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.BioCyan80
import com.example.ui.theme.BioTeal80
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.SulfurYellow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubmitScreen(
    currentUserId: String,
    currentUserDisplayName: String,
    taxonomyRepository: TaxonomyRepository,
    aiRepository: AiTaxonomyRepository,
    onSubmissionPublished: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Sound Profile State
    var durationSec by remember { mutableDoubleStateOf(1.8) }
    var decibels by remember { mutableIntStateOf(72) }
    var pitchHz by remember { mutableIntStateOf(130) }
    var waveform by remember { mutableStateOf("Continuous Drone") }
    var resonance by remember { mutableStateOf("Echoey") }
    var soundNotes by remember { mutableStateOf("") }

    // Smell Profile State
    var pungency by remember { mutableIntStateOf(6) }
    var lingeringSec by remember { mutableIntStateOf(45) }
    var dispersionMeters by remember { mutableDoubleStateOf(4.0) }
    var thermalSensation by remember { mutableStateOf("Warm") }
    var primaryOdor by remember { mutableStateOf("Sulfur") }
    var smellNotes by remember { mutableStateOf("") }

    // Context
    var location by remember { mutableStateOf("Home") }
    var sensation by remember { mutableStateOf("Sudden Relief") }
    val selectedFoods = remember { mutableStateListOf("Eggs", "Coffee") }

    // AI & Submit States
    var isAnalyzing by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }
    var aiResult by remember { mutableStateOf<AiAnalysisResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Audio Recording Manager & AI Specimen Auto-Fill States
    val context = LocalContext.current
    val recorderManager = remember { AudioRecordingManager(context) }
    DisposableEffect(Unit) {
        onDispose { recorderManager.release() }
    }

    val recordingStatus by recorderManager.status.collectAsStateWithLifecycle()
    val currentDecibels by recorderManager.currentDecibels.collectAsStateWithLifecycle()
    val currentAmpNorm by recorderManager.currentAmplitudeNormalized.collectAsStateWithLifecycle()
    val elapsedSeconds by recorderManager.elapsedSeconds.collectAsStateWithLifecycle()
    val recorderError by recorderManager.errorMessage.collectAsStateWithLifecycle()
    val lastRecordedSpecimen by recorderManager.lastRecording.collectAsStateWithLifecycle()
    val isAudioPlaying by recorderManager.isPlaying.collectAsStateWithLifecycle()

    var isAudioAnalyzing by remember { mutableStateOf(false) }
    var audioInferredProperties by remember { mutableStateOf<InferredSpecimenProperties?>(null) }
    var hasMicPermission by remember { mutableStateOf(recorderManager.hasRecordPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            recorderManager.startRecording(scope)
        }
    }

    val foodOptions = listOf("Eggs", "Broccoli/Cabbage", "Beans/Legumes", "Dairy/Cheese", "Whey Protein", "Beer/Alcohol", "Garlic/Onions", "Spicy Chili", "Oats/Fiber", "Coffee", "Apples")
    val locationOptions = listOf("Home", "Office", "Elevator", "Gym", "Transit", "Bed")
    val sensationOptions = listOf("Sudden Relief", "Abdominal Cramp Relief", "Surprise", "Exertion")
    val waveformOptions = listOf("Continuous Drone", "Staccato Flap", "Rising Whistle", "Explosive Pop", "Bubbling Tremolo", "Silent")
    val resonanceOptions = listOf("Hollow", "Tight", "Echoey", "Muffled", "Sharp")
    val odorOptions = listOf("Sulfur", "Cabbage", "Sweet Fermentation", "Earthy", "Burnt Rubber", "Metallic", "Yeasty", "Odorless")
    val thermalOptions = listOf("Cool", "Neutral", "Warm", "Thermal Inferno")

    fun triggerGeminiAudioAnalysis(specimen: RecordedSpecimen) {
        isAudioAnalyzing = true
        errorMessage = null
        scope.launch {
            try {
                val inferred = aiRepository.analyzeAudioAndInferProperties(
                    audioBytes = specimen.wavBytes,
                    measuredDuration = specimen.durationSeconds,
                    measuredDecibels = specimen.peakDecibels,
                    measuredPitchHz = specimen.fundamentalPitchHz,
                    detectedWaveform = specimen.detectedWaveform,
                    detectedResonance = specimen.detectedResonance
                )

                durationSec = inferred.durationSeconds.coerceIn(0.1, 10.0)
                decibels = inferred.decibels.coerceIn(25, 105)
                pitchHz = inferred.pitchHz.coerceIn(40, 800)
                if (waveformOptions.contains(inferred.waveform)) {
                    waveform = inferred.waveform
                }
                if (resonanceOptions.contains(inferred.resonance)) {
                    resonance = inferred.resonance
                }
                pungency = inferred.suggestedPungency.coerceIn(1, 10)
                if (thermalOptions.contains(inferred.suggestedThermal)) {
                    thermalSensation = inferred.suggestedThermal
                }
                if (odorOptions.contains(inferred.suggestedPrimaryOdor)) {
                    primaryOdor = inferred.suggestedPrimaryOdor
                }
                if (inferred.soundNotes.isNotBlank()) {
                    soundNotes = inferred.soundNotes
                }
                audioInferredProperties = inferred
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Gemini audio analysis failed"
            } finally {
                isAudioAnalyzing = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SPECIMEN RECORDING LAB",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Quantitative & Qualitative Log",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 0: INTEGRATED ACOUSTIC AUDIO RECORDER & GEMINI AUTO-FILL
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("audio_recorder_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (recordingStatus == RecordingStatus.RECORDING) AlertOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Microphone",
                            tint = if (recordingStatus == RecordingStatus.RECORDING) AlertOrange else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Acoustic Audio Recording Lab",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI AUTO-FILL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = BioTeal80,
                                modifier = Modifier
                                    .background(BioTeal80.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Record live emission -> Gemini AI analyzes spectrogram & fills all properties",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!hasMicPermission) {
                    // Microphone permission required card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Microphone permission is needed to record live acoustic emissions and auto-fill properties with Gemini AI.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("request_mic_permission_button")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enable Microphone Access")
                            }
                        }
                    }
                } else {
                    when {
                        recordingStatus == RecordingStatus.RECORDING -> {
                            // Active Live Recording View
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.5.dp, AlertOrange, RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(AlertOrange, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "RECORDING SPECIMEN...",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = AlertOrange
                                            )
                                        }
                                        Text(
                                            text = "${String.format("%.1f", elapsedSeconds)}s / 10.0s",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Real-time Decibel meter and level gauge
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "$currentDecibels dB",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (currentDecibels > 80) AlertOrange else BioTeal80
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Live Input Level", style = MaterialTheme.typography.labelSmall)
                                                Text(
                                                    if (currentDecibels < 45) "Subtle" else if (currentDecibels < 75) "Moderate" else "High Energy",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (currentDecibels > 80) AlertOrange else BioTeal80
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                val progress = ((currentDecibels - 25) / 80f).coerceIn(0.05f, 1f)
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(progress)
                                                        .height(8.dp)
                                                        .background(
                                                            if (currentDecibels > 80) AlertOrange else BioTeal80
                                                        )
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Stop Recording Button
                                    Button(
                                        onClick = {
                                            recorderManager.stopRecording(scope) { specimen ->
                                                triggerGeminiAudioAnalysis(specimen)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AlertOrange,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("stop_recording_button")
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = "Stop Recording")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Stop & Submit to Gemini AI",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        isAudioAnalyzing || recordingStatus == RecordingStatus.ANALYZING -> {
                            // Audio Gemini Analyzing state
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BioTeal80.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        color = BioTeal80,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Gemini AI Analyzing Audio Specimen...",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Deconstructing acoustic frequencies, amplitude envelope, and inferring slider properties.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        else -> {
                            // IDLE or COMPLETED
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (lastRecordedSpecimen != null) {
                                    // Audio Playback & Metrics Card
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                // Play / Pause specimen
                                                IconButton(
                                                    onClick = {
                                                        if (isAudioPlaying) {
                                                            recorderManager.stopPlayback()
                                                        } else {
                                                            recorderManager.playRecordedAudio()
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .background(
                                                            color = if (isAudioPlaying) AlertOrange.copy(alpha = 0.15f) else AcousticBlue.copy(alpha = 0.15f),
                                                            shape = CircleShape
                                                        )
                                                        .testTag("play_recorded_specimen_button")
                                                ) {
                                                    Icon(
                                                        imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                        contentDescription = if (isAudioPlaying) "Pause Recording" else "Play Recording",
                                                        tint = if (isAudioPlaying) AlertOrange else AcousticBlue
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (isAudioPlaying) "Playing Recorded Specimen..." else "Recorded Audio Clip",
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "${lastRecordedSpecimen!!.durationSeconds}s",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text(
                                                            text = "${lastRecordedSpecimen!!.peakDecibels} dB",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text(
                                                            text = "${lastRecordedSpecimen!!.fundamentalPitchHz} Hz",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = BioTeal80,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                // Re-record button
                                                Button(
                                                    onClick = {
                                                        recorderManager.startRecording(scope)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                        contentColor = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.testTag("rerecord_audio_button")
                                                ) {
                                                    Icon(Icons.Default.Replay, contentDescription = "Re-record", modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Re-record", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }

                                            // If AI has completed auto-fill
                                            if (audioInferredProperties != null) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                                Spacer(modifier = Modifier.height(10.dp))

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Success",
                                                        tint = HealthGreen,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Sliders & Properties Auto-Populated by Gemini!",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = HealthGreen
                                                    )
                                                }
                                                Text(
                                                    text = "Archetype detected: ${audioInferredProperties!!.archetypeScientific} (${audioInferredProperties!!.archetypeCommon}). Waveform: ${audioInferredProperties!!.waveform}, Resonance: ${audioInferredProperties!!.resonance}.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Primary Record CTA
                                    Button(
                                        onClick = {
                                            recorderManager.startRecording(scope)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("record_audio_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Record Microphone",
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Record Acoustic Specimen",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Text(
                                        text = "💡 Tap record, capture the sound, and Gemini will analyze the audio to automatically configure all sliders, waveforms, and property selectors below.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (recorderError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = recorderError ?: "Error recording audio",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 1: ACOUSTIC PROFILE
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Sound Wave",
                            tint = AcousticBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Acoustic Waveform & Decibels",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Play Synthesizer Preview Tone
                    Button(
                        onClick = {
                            SoundPlayer.playTone(pitchHz, durationSec, waveform)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcousticBlue.copy(alpha = 0.2f),
                            contentColor = AcousticBlue
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("preview_audio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Preview Tone",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Animated Waveform Visualizer
                AnimatedWaveformVisualizer(
                    pitchHz = pitchHz,
                    decibels = decibels,
                    waveform = waveform
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Duration Slider
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Duration:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${String.format("%.1f", durationSec)} seconds",
                        fontWeight = FontWeight.Bold,
                        color = AcousticBlue
                    )
                }
                Slider(
                    value = durationSec.toFloat().coerceIn(0.1f, 10.0f),
                    onValueChange = { durationSec = (it * 10).roundToInt() / 10.0 },
                    valueRange = 0.1f..10.0f,
                    colors = SliderDefaults.colors(thumbColor = AcousticBlue, activeTrackColor = AcousticBlue),
                    modifier = Modifier.testTag("duration_slider")
                )

                // Decibels Slider
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Loudness (Amplitude):", style = MaterialTheme.typography.bodyMedium)
                    Text("$decibels dB", fontWeight = FontWeight.Bold, color = AcousticBlue)
                }
                Slider(
                    value = decibels.toFloat().coerceIn(25f, 105f),
                    onValueChange = { decibels = it.roundToInt() },
                    valueRange = 25f..105f,
                    colors = SliderDefaults.colors(thumbColor = AcousticBlue, activeTrackColor = AcousticBlue),
                    modifier = Modifier.testTag("decibels_slider")
                )

                // Pitch Frequency Slider
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fundamental Pitch:", style = MaterialTheme.typography.bodyMedium)
                    val pitchNote = when {
                        pitchHz < 100 -> "Sub-Bass Rumble"
                        pitchHz < 180 -> "Baritone"
                        pitchHz < 350 -> "Tenor"
                        else -> "High Squeak / Whistle"
                    }
                    Text("$pitchHz Hz ($pitchNote)", fontWeight = FontWeight.Bold, color = BioTeal80)
                }
                Slider(
                    value = pitchHz.toFloat().coerceIn(50f, 800f),
                    onValueChange = { pitchHz = it.roundToInt() },
                    valueRange = 50f..800f,
                    colors = SliderDefaults.colors(thumbColor = BioTeal80, activeTrackColor = BioTeal80),
                    modifier = Modifier.testTag("pitch_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Acoustic Waveform Pattern:", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    waveformOptions.forEach { wf ->
                        FilterChip(
                            selected = waveform == wf,
                            onClick = { waveform = wf },
                            label = { Text(wf) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AcousticBlue,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Acoustic Resonance:", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    resonanceOptions.forEach { res ->
                        FilterChip(
                            selected = resonance == res,
                            onClick = { resonance = res },
                            label = { Text(res) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = soundNotes,
                    onValueChange = { soundNotes = it },
                    label = { Text("Sound Description / Acoustic Notes") },
                    placeholder = { Text("e.g. Deep brass trumpet tone, reverberated off car leather") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sound_notes_input")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 2: OLFACTORY PROFILE
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = "Smell Sensor",
                        tint = SulfurYellow,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Olfactory & Chemical Profile",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pungency Index Slider (1-10)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pungency Index (1-10):", style = MaterialTheme.typography.bodyMedium)
                    val pungencyLabel = when (pungency) {
                        in 1..2 -> "Near Odorless"
                        in 3..4 -> "Mild / Earthy"
                        in 5..6 -> "Definite Room Presence"
                        in 7..8 -> "Room Clearing"
                        else -> "Biohazard / Nuclear ☣"
                    }
                    Text(
                        "$pungency / 10 ($pungencyLabel)",
                        fontWeight = FontWeight.Bold,
                        color = if (pungency >= 7) AlertOrange else SulfurYellow
                    )
                }
                Slider(
                    value = pungency.toFloat(),
                    onValueChange = { pungency = it.roundToInt() },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = if (pungency >= 7) AlertOrange else SulfurYellow,
                        activeTrackColor = if (pungency >= 7) AlertOrange else SulfurYellow
                    ),
                    modifier = Modifier.testTag("pungency_slider")
                )

                // Dispersion Radius
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dispersion Radius:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${String.format("%.1f", dispersionMeters)} meters",
                        fontWeight = FontWeight.Bold,
                        color = SulfurYellow
                    )
                }
                Slider(
                    value = dispersionMeters.toFloat(),
                    onValueChange = { dispersionMeters = (it * 10).roundToInt() / 10.0 },
                    valueRange = 0.5f..12.0f,
                    colors = SliderDefaults.colors(thumbColor = SulfurYellow, activeTrackColor = SulfurYellow)
                )

                // Lingering Half-Life
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Lingering Half-Life:", style = MaterialTheme.typography.bodyMedium)
                    Text("$lingeringSec seconds", fontWeight = FontWeight.Bold, color = SulfurYellow)
                }
                Slider(
                    value = lingeringSec.toFloat(),
                    onValueChange = { lingeringSec = it.roundToInt() },
                    valueRange = 5f..180f,
                    colors = SliderDefaults.colors(thumbColor = SulfurYellow, activeTrackColor = SulfurYellow)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Primary Chemical Odor Note:", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    odorOptions.forEach { odor ->
                        FilterChip(
                            selected = primaryOdor == odor,
                            onClick = { primaryOdor = odor },
                            label = { Text(odor) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SulfurYellow,
                                selectedLabelColor = Color(0xFF1E293B),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Thermal Sensation:", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    thermalOptions.forEach { therm ->
                        FilterChip(
                            selected = thermalSensation == therm,
                            onClick = { thermalSensation = therm },
                            label = { Text(therm) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (therm.contains("Inferno")) AlertOrange else MaterialTheme.colorScheme.primary,
                                selectedLabelColor = if (therm.contains("Inferno")) Color.White else MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = smellNotes,
                    onValueChange = { smellNotes = it },
                    label = { Text("Olfactory Description / Notes") },
                    placeholder = { Text("e.g. Sharp boiled egg sulfur with cabbage notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smell_notes_input")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 3: CONTEXT & RECENT FOODS
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = "Dietary Context",
                        tint = HealthGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Dietary Intake & Environmental Context",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Recent Meals / Trigger Foods (Select all that apply):", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    foodOptions.forEach { food ->
                        val isSelected = selectedFoods.contains(food)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedFoods.remove(food) else selectedFoods.add(food)
                            },
                            label = { Text(food) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HealthGreen,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Location / Biome:", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    locationOptions.forEach { loc ->
                        FilterChip(
                            selected = location == loc,
                            onClick = { location = loc },
                            label = { Text(loc) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Physical Sensation:", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    sensationOptions.forEach { sens ->
                        FilterChip(
                            selected = sensation == sens,
                            onClick = { sensation = sens },
                            label = { Text(sens) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AI CLASSIFICATION TRIGGER BUTTON
        Button(
            onClick = {
                isAnalyzing = true
                errorMessage = null
                scope.launch {
                    try {
                        val sound = SoundProfile(
                            durationSeconds = durationSec,
                            decibels = decibels,
                            pitchHz = pitchHz,
                            waveform = waveform,
                            resonance = resonance,
                            qualitativeNotes = soundNotes
                        )
                        val smell = SmellProfile(
                            pungencyScale = pungency,
                            lingeringSeconds = lingeringSec,
                            dispersionRadiusMeters = dispersionMeters,
                            thermalSensation = thermalSensation,
                            primaryOdorNote = primaryOdor,
                            qualitativeNotes = smellNotes
                        )
                        val ctx = SubmissionContext(
                            locationType = location,
                            recentFoods = selectedFoods.toList(),
                            physicalSensation = sensation
                        )

                        val analysis = aiRepository.analyzeFlatus(sound, smell, ctx)
                        aiResult = analysis
                    } catch (e: Exception) {
                        errorMessage = e.localizedMessage ?: "AI analysis failed"
                    } finally {
                        isAnalyzing = false
                    }
                }
            },
            enabled = !isAnalyzing,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("run_ai_analysis_button")
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing Bio-Acoustics via Gemini...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyze & Classify in Taxonomy", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // AI ANALYSIS RESULTS CARD
        AnimatedVisibility(visible = aiResult != null) {
            aiResult?.let { result ->
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, BioTeal80, RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = "AI Classifier",
                                        tint = BioTeal80
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "AI TAXONOMY PLACEMENT",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = BioTeal80
                                    )
                                }

                                Text(
                                    text = "${(result.confidenceScore * 100).roundToInt()}% Match",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = HealthGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = result.taxonScientificName,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Common: ${result.taxonCommonName} (${result.taxonomicRank})",
                                style = MaterialTheme.typography.titleSmall,
                                color = BioCyan80
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = result.classificationReasoning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 14.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            )

                            // Health Indicators
                            Text(
                                text = "GUT MICROBIOME & HEALTH INDICATORS:",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = HealthGreen
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            result.healthIndicators.forEach { ind ->
                                Text(
                                    text = "• $ind",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = result.gutHealthSummary,
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 14.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            )

                            // Personalized Diet Recommendations
                            Text(
                                text = "PERSONALIZED DIETARY RECOMMENDATIONS:",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = SulfurYellow
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            result.dietaryRecommendations.forEach { diet ->
                                Text(
                                    text = "✓ $diet",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Publish Button
                            Button(
                                onClick = {
                                    isPublishing = true
                                    scope.launch {
                                        try {
                                            val submission = FlatusSubmission(
                                                userId = currentUserId,
                                                authorName = currentUserDisplayName.ifBlank { "Anonymous Taxonomist" },
                                                soundProfile = SoundProfile(
                                                    durationSeconds = durationSec,
                                                    decibels = decibels,
                                                    pitchHz = pitchHz,
                                                    waveform = waveform,
                                                    resonance = resonance,
                                                    qualitativeNotes = soundNotes
                                                ),
                                                smellProfile = SmellProfile(
                                                    pungencyScale = pungency,
                                                    lingeringSeconds = lingeringSec,
                                                    dispersionRadiusMeters = dispersionMeters,
                                                    thermalSensation = thermalSensation,
                                                    primaryOdorNote = primaryOdor,
                                                    qualitativeNotes = smellNotes
                                                ),
                                                context = SubmissionContext(
                                                    locationType = location,
                                                    recentFoods = selectedFoods.toList(),
                                                    physicalSensation = sensation
                                                ),
                                                aiAnalysis = result,
                                                agreeCount = 0
                                            )
                                            taxonomyRepository.addSubmission(submission)
                                            onSubmissionPublished()
                                        } catch (e: Exception) {
                                            errorMessage = e.localizedMessage ?: "Failed to publish"
                                        } finally {
                                            isPublishing = false
                                        }
                                    }
                                },
                                enabled = !isPublishing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("publish_submission_button")
                            ) {
                                if (isPublishing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSecondary)
                                } else {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Publish to Global Collaborative Feed", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
