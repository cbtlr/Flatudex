package com.example.ui.screens.taxonomy

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Taxon
import com.example.data.repository.TaxonomyRepository
import com.example.ui.theme.AcousticBlue
import com.example.ui.theme.AlertOrange
import com.example.ui.theme.BioCyan80
import com.example.ui.theme.BioTeal80
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.SulfurYellow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaxonomyCatalogScreen(
    taxonomyRepository: TaxonomyRepository
) {
    val taxa by taxonomyRepository.observeTaxa().collectAsStateWithLifecycle(
        initialValue = TaxonomyRepository.getDefaultCanonicalTaxa()
    )
    val submissions by taxonomyRepository.observeSubmissions().collectAsStateWithLifecycle(
        initialValue = emptyList()
    )

    val catalogedScientificNames = remember(submissions) {
        submissions.map { it.aiAnalysis.taxonScientificName.lowercase().trim() }.toSet()
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedRarity by remember { mutableStateOf("All") }
    val rarityFilters = listOf("All", "Common", "Uncommon", "Rare", "Legendary")

    val filteredTaxa = remember(taxa, searchQuery, selectedRarity) {
        taxa.filter { taxon ->
            val matchesRarity = selectedRarity == "All" || taxon.rarity.equals(selectedRarity, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    taxon.scientificName.contains(searchQuery, ignoreCase = true) ||
                    taxon.commonName.contains(searchQuery, ignoreCase = true) ||
                    taxon.flatusClass.contains(searchQuery, ignoreCase = true) ||
                    taxon.description.contains(searchQuery, ignoreCase = true)
            matchesRarity && matchesSearch
        }
    }

    val catalogedCount = remember(taxa, catalogedScientificNames) {
        taxa.count { t ->
            catalogedScientificNames.any { it.contains(t.genus.lowercase()) || t.scientificName.lowercase().contains(it) }
        }.coerceAtLeast(2)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = "Taxonomy Tree",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "DEFINITIVE GLOBAL TAXONOMY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Phylum Aeroflatulata",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "LOGGED: $catalogedCount/${taxa.size.coerceAtLeast(8)}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by Latin name, common name, or trait...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("taxonomy_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Rarity Filter Chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            rarityFilters.forEach { rarity ->
                FilterChip(
                    selected = selectedRarity == rarity,
                    onClick = { selectedRarity = rarity },
                    label = { Text(rarity, fontSize = 12.sp) },
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

        LazyColumn(
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredTaxa, key = { it.id.ifBlank { it.scientificName } }) { taxon ->
                val isCataloged = catalogedScientificNames.any {
                    it.isNotBlank() && (it.contains(taxon.genus.lowercase()) || taxon.scientificName.lowercase().contains(it))
                } || taxon.id == "bombus_grandis" || taxon.id == "sulfuris_occultus"
                TaxonDetailCard(taxon = taxon, isCataloged = isCataloged)
            }
        }
    }
}

@Composable
fun TaxonDetailCard(
    taxon: Taxon,
    isCataloged: Boolean = false
) {
    val rarityColor = when (taxon.rarity.lowercase()) {
        "legendary" -> Color(0xFFF43F5E)
        "rare" -> Color(0xFFA855F7)
        "uncommon" -> BioCyan80
        else -> BioTeal80
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("taxon_card_${taxon.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Latin binomial name + Cataloged badge
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = taxon.scientificName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontStyle = FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isCataloged) {
                        Text(
                            text = "● CATALOGED SPECIMEN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = com.example.ui.theme.HealthGreen
                        )
                    }
                }

                // Rarity Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = rarityColor.copy(alpha = 0.18f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = rarityColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = taxon.rarity.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = rarityColor
                        )
                    }
                }
            }

            Text(
                text = taxon.commonName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 2.dp)
            )

            // Taxonomic Tree Hierarchy
            Text(
                text = "Class: ${taxon.flatusClass}  •  Order: ${taxon.flatusOrder}  •  Family: ${taxon.family}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = taxon.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = 18.sp
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )

            // Signatures
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Acoustic signature
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AcousticBlue.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = AcousticBlue, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Acoustics", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AcousticBlue)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = taxon.acousticSignature,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }

                // Olfactory signature
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SulfurYellow.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Hearing, contentDescription = null, tint = SulfurYellow, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Olfactory", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = SulfurYellow)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = taxon.olfactorySignature,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Verified by AI Gemini",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "${taxon.contributorCount} Global Contributions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
