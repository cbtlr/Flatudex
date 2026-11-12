package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class SoundProfile(
    val durationSeconds: Double = 1.0,
    val decibels: Int = 65,
    val pitchHz: Int = 200,
    val waveform: String = "Continuous Drone", // "Continuous Drone", "Staccato Flap", "Rising Whistle", "Explosive Pop", "Bubbling Tremolo", "Silent"
    val resonance: String = "Echoey", // "Hollow", "Tight", "Echoey", "Muffled", "Sharp"
    val qualitativeNotes: String = ""
)

@IgnoreExtraProperties
data class SmellProfile(
    val pungencyScale: Int = 5, // 1 - 10
    val lingeringSeconds: Int = 30, // seconds
    val dispersionRadiusMeters: Double = 3.0,
    val thermalSensation: String = "Neutral", // "Cool", "Neutral", "Warm", "Thermal Inferno"
    val primaryOdorNote: String = "Sulfur", // "Sulfur", "Cabbage", "Sweet Fermentation", "Earthy", "Burnt Rubber", "Metallic", "Yeasty", "Odorless"
    val qualitativeNotes: String = ""
)

@IgnoreExtraProperties
data class SubmissionContext(
    val locationType: String = "Home", // "Home", "Office", "Elevator", "Gym", "Transit", "Outdoors", "Bed"
    val recentFoods: List<String> = emptyList(), // "Eggs", "Broccoli", "Beans", "Dairy", "Beer", "Meat"
    val physicalSensation: String = "Sudden Relief" // "Sudden Relief", "Abdominal Cramp Relief", "Surprise", "Exertion"
)

@IgnoreExtraProperties
data class AiAnalysisResult(
    val taxonScientificName: String = "",
    val taxonCommonName: String = "",
    val confidenceScore: Double = 0.0,
    val taxonomicRank: String = "Archetype",
    val classificationReasoning: String = "",
    val healthIndicators: List<String> = emptyList(),
    val gutHealthSummary: String = "",
    val dietaryRecommendations: List<String> = emptyList(),
    val disclaimerNotice: String = "For educational bio-acoustic tracking only; consult a physician for gastrointestinal diagnosis."
)

@IgnoreExtraProperties
data class FartSubmission(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorPhoto: String = "",
    val soundProfile: SoundProfile = SoundProfile(),
    val smellProfile: SmellProfile = SmellProfile(),
    val context: SubmissionContext = SubmissionContext(),
    val aiAnalysis: AiAnalysisResult = AiAnalysisResult(),
    val agreeCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

typealias FlatusSubmission = FartSubmission

@IgnoreExtraProperties
data class Taxon(
    @DocumentId val id: String = "",
    val scientificName: String = "",
    val commonName: String = "",
    val phylum: String = "Aeroflatulata",
    val flatusClass: String = "Sonoric",
    val flatusOrder: String = "Baritonis",
    val family: String = "Bombidae",
    val genus: String = "Flatus",
    val archetype: String = "bombus grandis",
    val species: String = "bombus grandis",
    val rarity: String = "Common", // "Common", "Uncommon", "Rare", "Legendary", "Mythic"
    val acousticSignature: String = "",
    val olfactorySignature: String = "",
    val description: String = "",
    val contributorCount: Int = 1,
    val verifiedByAi: Boolean = true,
    val typicalDecibels: Int = 70,
    val typicalPungency: Int = 5,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val userId: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val bio: String = "Bio-acoustic contributor & flatulence taxonomist",
    val submissionCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class InferredSpecimenProperties(
    val durationSeconds: Double = 1.8,
    val decibels: Int = 72,
    val pitchHz: Int = 130,
    val waveform: String = "Continuous Drone",
    val resonance: String = "Echoey",
    val suggestedPungency: Int = 5,
    val suggestedThermal: String = "Neutral",
    val suggestedPrimaryOdor: String = "Sulfur",
    val soundNotes: String = "",
    val classificationReasoning: String = "",
    val archetypeScientific: String = "Flatus bombus grandis",
    val archetypeCommon: String = "The Thunderous Bass"
)
