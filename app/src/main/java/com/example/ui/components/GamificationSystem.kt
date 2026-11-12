package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.FlatusSubmission
import com.example.ui.theme.AcousticBlue
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.BioCyan80
import com.example.ui.theme.BioTeal80
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.SulfurYellow
import kotlin.math.cos
import kotlin.math.sin

data class MeritBadge(
    val id: String,
    val name: String,
    val divisionName: String,
    val requirement: String,
    val description: String,
    val xpReward: Int,
    val unlocked: Boolean,
    val progressFraction: Float,
    val progressLabel: String,
    val badgeColor: Color,
    val icon: ImageVector
)

data class ResearcherStats(
    val totalXp: Int,
    val level: Int,
    val rankTitle: String,
    val nextLevelXp: Int,
    val currentLevelProgress: Float,
    val catalogedArchetypesCount: Int,
    val totalCanonicalArchetypes: Int,
    val badges: List<MeritBadge>
) {
    val unlockedBadgeCount: Int get() = badges.count { it.unlocked }
}

object GamificationCalculator {

    fun calculateStats(
        submissions: List<FlatusSubmission>,
        currentUserId: String
    ): ResearcherStats {
        val userSubmissions = submissions.filter { it.userId == currentUserId }
        val allSubmissions = submissions

        val maxDecibels = allSubmissions.maxOfOrNull { it.soundProfile.decibels } ?: 0
        val hasLoudBass = allSubmissions.any { it.soundProfile.decibels >= 75 }
        val hasSilentDeadly = allSubmissions.any {
            it.aiAnalysis.taxonScientificName.contains("Sibilus", ignoreCase = true) ||
                (it.soundProfile.decibels < 45 && it.smellProfile.pungencyScale >= 7)
        }
        val hasThermalInferno = allSubmissions.any {
            it.smellProfile.thermalSensation.contains("Inferno", ignoreCase = true) ||
                it.smellProfile.thermalSensation.contains("Warm", ignoreCase = true)
        }
        val hasStaccatoOrPop = userSubmissions.any {
            it.soundProfile.waveform.contains("Staccato", ignoreCase = true) ||
                it.soundProfile.waveform.contains("Pop", ignoreCase = true) ||
                it.soundProfile.waveform.contains("Tremolo", ignoreCase = true)
        }
        val totalAgrees = allSubmissions.sumOf { it.agreeCount }
        val distinctOdors = allSubmissions.map { it.smellProfile.primaryOdorNote }.distinct().size
        val distinctArchetypesCataloged = allSubmissions
            .map { it.aiAnalysis.taxonScientificName.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .size
            .coerceAtLeast(2)

        val badges = listOf(
            MeritBadge(
                id = "sub_bass_acoustics",
                name = "Sub-Bass Acoustics",
                divisionName = "Acoustic Amplitude Division",
                requirement = "Document or verify a 75+ dB low-frequency specimen",
                description = "Awarded to researchers who record high-amplitude acoustic emissions exceeding 75 decibels in the low-frequency spectrum.",
                xpReward = 100,
                unlocked = hasLoudBass,
                progressFraction = (maxDecibels / 75f).coerceIn(0f, 1f),
                progressLabel = "${maxDecibels.coerceAtMost(75)} / 75 dB",
                badgeColor = AcousticBlue,
                icon = Icons.Default.VolumeUp
            ),
            MeritBadge(
                id = "h2s_assay",
                name = "H₂S Olfactory Assay",
                divisionName = "Volatile Sulfide Division",
                requirement = "Catalog a low-decibel (<45 dB), high-pungency (7+) specimen",
                description = "Recognizes documentation of high-concentration hydrogen sulfide and mercaptan emissions with minimal acoustic signature.",
                xpReward = 150,
                unlocked = hasSilentDeadly,
                progressFraction = if (hasSilentDeadly) 1f else 0.5f,
                progressLabel = if (hasSilentDeadly) "Verified" else "Log <45dB & 7+ Pungency",
                badgeColor = Color(0xFFA855F7),
                icon = Icons.Default.Shield
            ),
            MeritBadge(
                id = "exothermic_gas",
                name = "Exothermic Kinetics",
                divisionName = "Calorimetric Analysis Lab",
                requirement = "Log a specimen with Warm or Thermal Inferno thermal profile",
                description = "Documents elevated localized thermal sensation correlated with rapid fermentation or dietary capsaicin intake.",
                xpReward = 120,
                unlocked = hasThermalInferno,
                progressFraction = if (hasThermalInferno) 1f else 0.4f,
                progressLabel = if (hasThermalInferno) "Verified" else "Log Warm/Thermal Gas",
                badgeColor = AlertOrange,
                icon = Icons.Default.LocalFireDepartment
            ),
            MeritBadge(
                id = "peer_consensus",
                name = "Peer Review Fellow",
                divisionName = "Taxonomy Consensus Board",
                requirement = "Reach 15+ community consensus 'Agree' validations across the network",
                description = "Granted to collaborative contributors who actively review and validate binomial classifications in the global catalog.",
                xpReward = 100,
                unlocked = totalAgrees >= 15,
                progressFraction = (totalAgrees / 15f).coerceIn(0f, 1f),
                progressLabel = "${totalAgrees.coerceAtMost(15)} / 15 Validations",
                badgeColor = BioTeal80,
                icon = Icons.Default.ThumbUp
            ),
            MeritBadge(
                id = "harmonic_modulation",
                name = "Harmonic Modulation",
                divisionName = "Waveform Synthesis Lab",
                requirement = "Submit a Staccato Flap, Explosive Pop, or Bubbling Tremolo waveform",
                description = "Commends detailed acoustic profiling of complex multi-impulse and modulated oscillation waveforms.",
                xpReward = 150,
                unlocked = hasStaccatoOrPop,
                progressFraction = if (hasStaccatoOrPop) 1f else 0.2f,
                progressLabel = if (hasStaccatoOrPop) "Verified" else "Submit Modulated Wave",
                badgeColor = BioCyan80,
                icon = Icons.Default.GraphicEq
            ),
            MeritBadge(
                id = "enteric_microbiome",
                name = "Enteric Microbiome",
                divisionName = "Gastrointestinal Nutrition Unit",
                requirement = "Submit 1+ personal specimen for AI microbiome & dietary correlation",
                description = "Earned by utilizing AI analysis to map acoustic and olfactory biomarkers to gut flora and dietary fiber profiles.",
                xpReward = 150,
                unlocked = userSubmissions.isNotEmpty(),
                progressFraction = if (userSubmissions.isNotEmpty()) 1f else 0f,
                progressLabel = "${userSubmissions.size.coerceAtMost(1)} / 1 Personal Log",
                badgeColor = HealthGreen,
                icon = Icons.Default.Eco
            ),
            MeritBadge(
                id = "chemical_spectrum",
                name = "Chemical Spectrum",
                divisionName = "Olfactory Chromatography Guild",
                requirement = "Document 3+ distinct primary chemical odor notes in the network",
                description = "Recognizes systematic differentiation across sulfur, methanethiol (cabbage), fermentation, and organic volatile profiles.",
                xpReward = 180,
                unlocked = distinctOdors >= 3,
                progressFraction = (distinctOdors / 3f).coerceIn(0f, 1f),
                progressLabel = "${distinctOdors.coerceAtMost(3)} / 3 Odor Notes",
                badgeColor = SulfurYellow,
                icon = Icons.Default.Science
            ),
            MeritBadge(
                id = "principal_taxonomist",
                name = "Taxonomy Archivist",
                divisionName = "Phylum Aeroflatulata Registry",
                requirement = "Catalog 5+ distinct canonical archetypes in the global registry",
                description = "Conferred upon senior researchers who have contributed to documenting a comprehensive cross-section of Phylum Aeroflatulata emission archetypes.",
                xpReward = 300,
                unlocked = distinctArchetypesCataloged >= 5,
                progressFraction = (distinctArchetypesCataloged / 5f).coerceIn(0f, 1f),
                progressLabel = "${distinctArchetypesCataloged.coerceAtMost(5)} / 5 Archetypes",
                badgeColor = Color(0xFFF43F5E),
                icon = Icons.Default.EmojiEvents
            )
        )

        val badgeXp = badges.filter { it.unlocked }.sumOf { it.xpReward }
        val submissionXp = userSubmissions.size * 75 + allSubmissions.size * 25
        val totalXp = badgeXp + submissionXp

        val level = (totalXp / 150) + 1
        val currentLevelBase = (level - 1) * 150
        val nextLevelXp = level * 150
        val currentLevelProgress = ((totalXp - currentLevelBase) / 150f).coerceIn(0.08f, 1f)

        val rankTitle = when {
            level >= 8 -> "Principal Taxonomist Fellow"
            level >= 6 -> "Senior Olfactory Specialist"
            level >= 4 -> "Bio-Acoustic Research Analyst"
            level >= 2 -> "Certified Field Taxonomist"
            else -> "Associate Field Observer"
        }

        return ResearcherStats(
            totalXp = totalXp,
            level = level,
            rankTitle = rankTitle,
            nextLevelXp = nextLevelXp,
            currentLevelProgress = currentLevelProgress,
            catalogedArchetypesCount = distinctArchetypesCataloged.coerceAtMost(8),
            totalCanonicalArchetypes = 8,
            badges = badges
        )
    }
}

@Composable
fun HexagonalMeritBadgeItem(
    badge: MeritBadge,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.unlocked) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (badge.unlocked) 3.dp else 0.dp),
        modifier = modifier
            .border(
                width = if (badge.unlocked) 1.5.dp else 1.dp,
                color = if (badge.unlocked) badge.badgeColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .testTag("merit_badge_${badge.id}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(60.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f - 4f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val path = Path()
                    for (i in 0 until 8) {
                        val angle = Math.toRadians((45.0 * i - 22.5))
                        val x = center.x + radius * cos(angle).toFloat()
                        val y = center.y + radius * sin(angle).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()

                    if (badge.unlocked) {
                        drawPath(
                            path = path,
                            brush = Brush.linearGradient(
                                listOf(
                                    badge.badgeColor,
                                    badge.badgeColor.copy(alpha = 0.45f)
                                )
                            )
                        )
                        drawPath(
                            path = path,
                            color = badge.badgeColor,
                            style = Stroke(width = 5f)
                        )
                    } else {
                        drawPath(
                            path = path,
                            color = Color.Gray.copy(alpha = 0.2f)
                        )
                        drawPath(
                            path = path,
                            color = Color.Gray.copy(alpha = 0.45f),
                            style = Stroke(width = 3f)
                        )
                    }
                }

                Icon(
                    imageVector = if (badge.unlocked) badge.icon else Icons.Default.Lock,
                    contentDescription = badge.name,
                    tint = if (badge.unlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = badge.name,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (badge.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Text(
                text = badge.progressLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (badge.unlocked) badge.badgeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MeritBadgeDetailDialog(
    badge: MeritBadge,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, badge.badgeColor, RoundedCornerShape(24.dp))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = badge.divisionName.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = badge.badgeColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(88.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.minDimension / 2f - 6f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val path = Path()
                        for (i in 0 until 8) {
                            val angle = Math.toRadians((45.0 * i - 22.5))
                            val x = center.x + radius * cos(angle).toFloat()
                            val y = center.y + radius * sin(angle).toFloat()
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(
                            path = path,
                            brush = Brush.linearGradient(
                                listOf(badge.badgeColor, badge.badgeColor.copy(alpha = 0.4f))
                            )
                        )
                        drawPath(
                            path = path,
                            color = badge.badgeColor,
                            style = Stroke(width = 6f)
                        )
                    }
                    Icon(
                        imageVector = badge.icon,
                        contentDescription = badge.name,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = badge.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (badge.unlocked) HealthGreen.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (badge.unlocked) "✓ VERIFIED (+${badge.xpReward} RP)" else "🔒 PENDING (+${badge.xpReward} RP)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (badge.unlocked) HealthGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "CRITERIA FOR COMMENDATION:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = badge.requirement,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { badge.progressFraction },
                            color = badge.badgeColor,
                            trackColor = badge.badgeColor.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Commendation Record", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
