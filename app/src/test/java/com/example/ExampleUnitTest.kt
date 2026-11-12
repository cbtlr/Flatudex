package com.example

import com.example.data.model.SmellProfile
import com.example.data.model.SoundProfile
import com.example.data.model.SubmissionContext
import com.example.data.repository.AiTaxonomyRepository
import com.example.data.repository.TaxonomyRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun canonicalTaxaArePopulatedAndValid() {
    val taxa = TaxonomyRepository.getDefaultCanonicalTaxa()
    assertTrue(taxa.size >= 8)
    val grandis = taxa.firstOrNull { it.id == "bombus_grandis" }
    assertNotNull(grandis)
    assertEquals("Flatus bombus grandis", grandis?.scientificName)
    assertEquals("bombus grandis", grandis?.archetype)
  }

  @Test
  fun deterministicClassifierIdentifiesSbdSulfurProfile() {
    val repo = AiTaxonomyRepository()
    val sound = SoundProfile(decibels = 28, waveform = "Silent")
    val smell = SmellProfile(pungencyScale = 9, primaryOdorNote = "Sulfur")
    val context = SubmissionContext(recentFoods = listOf("Eggs", "Whey Protein"))

    val result = repo.fallbackDeterministicAnalysis(sound, smell, context)
    assertEquals("Sibilus sulfuris occultus", result.taxonScientificName)
    assertTrue(result.healthIndicators.any { it.contains("Sulfate-Reducing", true) || it.contains("Desulfovibrio", true) })
    assertTrue(result.dietaryRecommendations.isNotEmpty())
  }

  @Test
  fun gamificationCalculatorComputesEightBadgesAndXp() {
    val sampleSubmission = com.example.data.model.FlatusSubmission(
      userId = "researcher_1",
      soundProfile = SoundProfile(decibels = 88, pitchHz = 90, waveform = "Staccato Flap"),
      smellProfile = SmellProfile(pungencyScale = 9, thermalSensation = "Thermal Inferno", primaryOdorNote = "Sulfur"),
      aiAnalysis = com.example.data.model.AiAnalysisResult(taxonScientificName = "Sibilus sulfuris occultus"),
      agreeCount = 20
    )
    val stats = com.example.ui.components.GamificationCalculator.calculateStats(
      submissions = listOf(sampleSubmission),
      currentUserId = "researcher_1"
    )
    assertEquals(8, stats.badges.size)
    assertTrue(stats.unlockedBadgeCount >= 5)
    assertTrue(stats.totalXp > 0)
  }

  @Test
  fun audioInferenceAutoFillsPropertiesForLoudDroneSpecimen() {
    val repo = AiTaxonomyRepository()
    val inferred = repo.fallbackDeterministicAudioInference(
      measuredDuration = 2.4,
      measuredDecibels = 86,
      measuredPitchHz = 95,
      detectedWaveform = "Continuous Drone",
      detectedResonance = "Hollow"
    )
    assertEquals(2.4, inferred.durationSeconds, 0.01)
    assertEquals(86, inferred.decibels)
    assertEquals(95, inferred.pitchHz)
    assertEquals("Continuous Drone", inferred.waveform)
    assertEquals("Hollow", inferred.resonance)
    assertEquals("Flatus bombus grandis", inferred.archetypeScientific)
    assertTrue(inferred.soundNotes.isNotEmpty())
  }

  @Test
  fun audioInferenceAutoFillsPropertiesForSilentDeadlySpecimen() {
    val repo = AiTaxonomyRepository()
    val inferred = repo.fallbackDeterministicAudioInference(
      measuredDuration = 0.5,
      measuredDecibels = 28,
      measuredPitchHz = 600,
      detectedWaveform = "Silent",
      detectedResonance = "Muffled"
    )
    assertEquals("Silent", inferred.waveform)
    assertEquals("Sibilus sulfuris occultus", inferred.archetypeScientific)
    assertEquals(9, inferred.suggestedPungency)
    assertEquals("Sulfur", inferred.suggestedPrimaryOdor)
    assertEquals("Thermal Inferno", inferred.suggestedThermal)
  }

  @Test
  fun audioInferenceAutoFillsPropertiesForExplosivePopSpecimen() {
    val repo = AiTaxonomyRepository()
    val inferred = repo.fallbackDeterministicAudioInference(
      measuredDuration = 0.3,
      measuredDecibels = 88,
      measuredPitchHz = 220,
      detectedWaveform = "Explosive Pop",
      detectedResonance = "Sharp"
    )
    assertEquals("Explosive Pop", inferred.waveform)
    assertEquals("Populus vespertinus", inferred.archetypeScientific)
    assertEquals(2, inferred.suggestedPungency)
  }
}
