package com.example.ui.screens.analysis

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FlatusSubmission
import com.example.data.repository.TaxonomyRepository
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

@Composable
fun AnalysisDetailScreen(
    submission: FlatusSubmission,
    taxonomyRepository: TaxonomyRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top Back Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "SPECIMEN ANALYSIS DOSSIER",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "By ${submission.authorName} • in ${submission.context.locationType}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TAXONOMIC CARD
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = BioTeal80
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI TAXONOMY IDENTIFICATION",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = BioTeal80
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = HealthGreen.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${(submission.aiAnalysis.confidenceScore * 100).roundToInt()}% Match",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = HealthGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = submission.aiAnalysis.taxonScientificName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Common: ${submission.aiAnalysis.taxonCommonName} (${submission.aiAnalysis.taxonomicRank})",
                    style = MaterialTheme.typography.titleSmall,
                    color = BioCyan80
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = submission.aiAnalysis.classificationReasoning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ACOUSTIC & WAVEFORM BREAKDOWN
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = AcousticBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Acoustic Decomposition",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = {
                            SoundPlayer.playTone(
                                submission.soundProfile.pitchHz,
                                submission.soundProfile.durationSeconds,
                                submission.soundProfile.waveform
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcousticBlue.copy(alpha = 0.2f),
                            contentColor = AcousticBlue
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Replay Tone", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedWaveformVisualizer(
                    pitchHz = submission.soundProfile.pitchHz,
                    decibels = submission.soundProfile.decibels,
                    waveform = submission.soundProfile.waveform
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("Amplitude", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("${submission.soundProfile.decibels} dB", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = AcousticBlue)
                    }
                    Column {
                        Text("Frequency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("${submission.soundProfile.pitchHz} Hz", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = BioTeal80)
                    }
                    Column {
                        Text("Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("${submission.soundProfile.durationSeconds}s", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = BioCyan80)
                    }
                    Column {
                        Text("Waveform", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(submission.soundProfile.waveform, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // OLFACTORY & CHEMICAL BREAKDOWN
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Hearing, contentDescription = null, tint = SulfurYellow)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Olfactory & Scent Dynamics",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text("Pungency Index", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("${submission.smellProfile.pungencyScale} / 10", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = if (submission.smellProfile.pungencyScale >= 7) AlertOrange else SulfurYellow)
                    }
                    Column {
                        Text("Primary Note", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(submission.smellProfile.primaryOdorNote, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = SulfurYellow)
                    }
                    Column {
                        Text("Radius", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("${submission.smellProfile.dispersionRadiusMeters}m", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = BioCyan80)
                    }
                    Column {
                        Text("Half-Life", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("${submission.smellProfile.lingeringSeconds}s", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // HEALTH INDICATORS & GUT MICROBIOME
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Eco, contentDescription = null, tint = HealthGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gastrointestinal Microbiome Indicators",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                submission.aiAnalysis.healthIndicators.forEach { ind ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = HealthGreen.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "• $ind",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = HealthGreen,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = submission.aiAnalysis.gutHealthSummary,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PERSONALIZED DIETARY RECOMMENDATIONS
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = SulfurYellow)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Personalized Dietary Guidance",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (submission.context.recentFoods.isNotEmpty()) {
                    Text(
                        text = "Correlated Foods Consumed: ${submission.context.recentFoods.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                submission.aiAnalysis.dietaryRecommendations.forEach { diet ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SulfurYellow.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "✓ $diet",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = submission.aiAnalysis.disclaimerNotice,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Consensus Vote Button
        Button(
            onClick = {
                scope.launch {
                    if (submission.id.isNotBlank()) {
                        taxonomyRepository.upvoteSubmission(submission.id)
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("consensus_agree_button")
        ) {
            Icon(Icons.Default.ThumbUp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Validate Taxonomic Placement (${submission.agreeCount} Agreed)",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
