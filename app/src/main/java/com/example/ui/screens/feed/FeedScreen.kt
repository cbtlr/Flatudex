package com.example.ui.screens.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FlatusSubmission
import com.example.data.repository.TaxonomyRepository
import com.example.ui.theme.AcousticBlue
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.BioCyan80
import com.example.ui.theme.BioTeal80
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.HealthGreen
import com.example.ui.theme.SulfurYellow
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedScreen(
    currentUserId: String,
    currentUserDisplayName: String,
    taxonomyRepository: TaxonomyRepository,
    onNavigateToSubmit: () -> Unit,
    onSelectSubmission: (FlatusSubmission) -> Unit,
    onOpenTutorial: () -> Unit = {},
    onNavigateToBadges: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val submissions by taxonomyRepository.observeSubmissions().collectAsStateWithLifecycle(initialValue = emptyList())

    val researcherStats = remember(submissions, currentUserId) {
        com.example.ui.components.GamificationCalculator.calculateStats(submissions, currentUserId)
    }

    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "High Pungency (7+)", "Loud Bass (75+ dB)", "Silent But Deadly", "Recent")

    LaunchedEffect(Unit) {
        taxonomyRepository.seedTaxaIfEmpty()
        taxonomyRepository.seedSampleSubmissionsIfEmpty(currentUserDisplayName, currentUserId)
    }

    val filteredList = remember(submissions, selectedFilter) {
        when (selectedFilter) {
            "High Pungency (7+)" -> submissions.filter { it.smellProfile.pungencyScale >= 7 }
            "Loud Bass (75+ dB)" -> submissions.filter { it.soundProfile.decibels >= 75 }
            "Silent But Deadly" -> submissions.filter {
                it.aiAnalysis.taxonScientificName.contains("Sibilus", true) ||
                (it.soundProfile.decibels < 45 && it.smellProfile.pungencyScale >= 7)
            }
            else -> submissions
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Global Feed Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Global Network",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GLOBAL COLLABORATIVE FEED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Real-Time Taxonomy Logs",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Protocol Guide / Tutorial Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clickable { onOpenTutorial() }
                        .testTag("open_tutorial_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "📘 Protocol Guide",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Researcher Credential & Merit Badges Bar
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToBadges() }
                    .testTag("researcher_progress_banner")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TIER ${researcherStats.level} • ${researcherStats.rankTitle.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${researcherStats.totalXp} RP • ${researcherStats.catalogedArchetypesCount}/${researcherStats.totalCanonicalArchetypes} Archetypes Cataloged",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SulfurYellow.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "🏅 ${researcherStats.unlockedBadgeCount}/8 Merit Badges",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SulfurYellow,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connecting to global taxonomy network...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredList, key = { it.id.ifBlank { it.hashCode().toString() } }) { submission ->
                        SubmissionFeedCard(
                            submission = submission,
                            onAgreeClicked = {
                                scope.launch {
                                    if (submission.id.isNotBlank()) {
                                        taxonomyRepository.upvoteSubmission(submission.id)
                                    }
                                }
                            },
                            onClick = { onSelectSubmission(submission) }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onNavigateToSubmit,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
                .testTag("record_specimen_fab")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Specimen")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Specimen", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SubmissionFeedCard(
    submission: FlatusSubmission,
    onAgreeClicked: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("submission_card_${submission.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Author & Location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = submission.authorName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = submission.authorName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = submission.context.locationType,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Upvote agreement pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { onAgreeClicked() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Agree with classification",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${submission.agreeCount} Agree",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Taxonomy Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = submission.aiAnalysis.taxonScientificName.ifBlank { "Classifying Taxon..." },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontStyle = FontStyle.Italic
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = submission.aiAnalysis.taxonCommonName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (submission.aiAnalysis.confidenceScore > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HealthGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${(submission.aiAnalysis.confidenceScore * 100).toInt()}% AI Match",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = HealthGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quantitative Metrics Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Sound Metrics Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AcousticBlue.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = AcousticBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "${submission.soundProfile.decibels} dB • ${submission.soundProfile.pitchHz} Hz",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = AcousticBlue
                            )
                            Text(
                                text = "${submission.soundProfile.durationSeconds}s • ${submission.soundProfile.waveform}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Smell Metrics Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = (if (submission.smellProfile.pungencyScale >= 7) AlertOrange else SulfurYellow).copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = null,
                            tint = if (submission.smellProfile.pungencyScale >= 7) AlertOrange else SulfurYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Pungency: ${submission.smellProfile.pungencyScale}/10",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (submission.smellProfile.pungencyScale >= 7) AlertOrange else SulfurYellow
                            )
                            Text(
                                text = "${submission.smellProfile.primaryOdorNote} • ${submission.smellProfile.dispersionRadiusMeters}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Qualitative Notes if present
            if (submission.soundProfile.qualitativeNotes.isNotBlank() || submission.smellProfile.qualitativeNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                val notes = listOfNotNull(
                    submission.soundProfile.qualitativeNotes.takeIf { it.isNotBlank() },
                    submission.smellProfile.qualitativeNotes.takeIf { it.isNotBlank() }
                ).joinToString(" — ")

                Text(
                    text = "“$notes”",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            // Health badge preview
            if (submission.aiAnalysis.healthIndicators.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        tint = HealthGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = submission.aiAnalysis.healthIndicators.first(),
                        style = MaterialTheme.typography.labelSmall,
                        color = HealthGreen
                    )
                }
            }
        }
    }
}
