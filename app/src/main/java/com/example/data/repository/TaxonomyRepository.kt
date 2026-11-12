package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.FartSubmission
import com.example.data.model.FlatusSubmission
import com.example.data.model.Taxon
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TaxonomyRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // Observe global submission feed
    fun observeSubmissions(): Flow<List<FlatusSubmission>> = flow {
        val path = "submissions"
        emitAll(
            db.collection(path)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FartSubmission::class.java)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // Observe taxa catalog
    fun observeTaxa(): Flow<List<Taxon>> = flow {
        val path = "taxa"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    val list = snapshot.toObjects(Taxon::class.java)
                    if (list.isEmpty()) {
                        getDefaultCanonicalTaxa()
                    } else {
                        list
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // Submit a new flatulence emission record
    suspend fun addSubmission(submission: FlatusSubmission) {
        val uid = requireUserId()
        val docRef = db.collection("submissions").document()
        val payload = mapOf(
            "userId" to uid,
            "authorName" to submission.authorName,
            "authorPhoto" to submission.authorPhoto,
            "soundProfile" to mapOf(
                "durationSeconds" to submission.soundProfile.durationSeconds,
                "decibels" to submission.soundProfile.decibels,
                "pitchHz" to submission.soundProfile.pitchHz,
                "waveform" to submission.soundProfile.waveform,
                "resonance" to submission.soundProfile.resonance,
                "qualitativeNotes" to submission.soundProfile.qualitativeNotes
            ),
            "smellProfile" to mapOf(
                "pungencyScale" to submission.smellProfile.pungencyScale,
                "lingeringSeconds" to submission.smellProfile.lingeringSeconds,
                "dispersionRadiusMeters" to submission.smellProfile.dispersionRadiusMeters,
                "thermalSensation" to submission.smellProfile.thermalSensation,
                "primaryOdorNote" to submission.smellProfile.primaryOdorNote,
                "qualitativeNotes" to submission.smellProfile.qualitativeNotes
            ),
            "context" to mapOf(
                "locationType" to submission.context.locationType,
                "recentFoods" to submission.context.recentFoods,
                "physicalSensation" to submission.context.physicalSensation
            ),
            "aiAnalysis" to mapOf(
                "taxonScientificName" to submission.aiAnalysis.taxonScientificName,
                "taxonCommonName" to submission.aiAnalysis.taxonCommonName,
                "confidenceScore" to submission.aiAnalysis.confidenceScore,
                "taxonomicRank" to submission.aiAnalysis.taxonomicRank,
                "classificationReasoning" to submission.aiAnalysis.classificationReasoning,
                "healthIndicators" to submission.aiAnalysis.healthIndicators,
                "gutHealthSummary" to submission.aiAnalysis.gutHealthSummary,
                "dietaryRecommendations" to submission.aiAnalysis.dietaryRecommendations,
                "disclaimerNotice" to submission.aiAnalysis.disclaimerNotice
            ),
            "agreeCount" to 0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            // Increment user submission count
            val userRef = db.collection("users").document(uid)
            userRef.set(
                mapOf(
                    "userId" to uid,
                    "submissionCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // Upvote / Agree with taxonomy classification
    suspend fun upvoteSubmission(submissionId: String) {
        require(submissionId.isNotBlank())
        val docRef = db.collection("submissions").document(submissionId)
        try {
            docRef.update(
                mapOf(
                    "agreeCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // Seed canonical taxa if needed
    suspend fun seedTaxaIfEmpty() {
        try {
            val snapshot = db.collection("taxa").limit(1).get().await()
            if (snapshot.isEmpty) {
                val batch = db.batch()
                getDefaultCanonicalTaxa().forEach { taxon ->
                    val doc = db.collection("taxa").document()
                    val payload = mapOf(
                        "scientificName" to taxon.scientificName,
                        "commonName" to taxon.commonName,
                        "phylum" to taxon.phylum,
                        "flatusClass" to taxon.flatusClass,
                        "flatusOrder" to taxon.flatusOrder,
                        "family" to taxon.family,
                        "genus" to taxon.genus,
                        "archetype" to taxon.archetype,
                        "species" to taxon.species,
                        "rarity" to taxon.rarity,
                        "acousticSignature" to taxon.acousticSignature,
                        "olfactorySignature" to taxon.olfactorySignature,
                        "description" to taxon.description,
                        "contributorCount" to taxon.contributorCount,
                        "verifiedByAi" to taxon.verifiedByAi,
                        "typicalDecibels" to taxon.typicalDecibels,
                        "typicalPungency" to taxon.typicalPungency,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(doc, payload)
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "taxa")
        }
    }

    // Seed sample submissions if collection is empty so user immediately experiences rich community
    suspend fun seedSampleSubmissionsIfEmpty(currentUserDisplayName: String, currentUid: String) {
        try {
            val snapshot = db.collection("submissions").limit(1).get().await()
            if (snapshot.isEmpty) {
                val samples = listOf(
                    FartSubmission(
                        authorName = "Dr. Gasparo",
                        soundProfile = com.example.data.model.SoundProfile(
                            durationSeconds = 2.8,
                            decibels = 86,
                            pitchHz = 95,
                            waveform = "Continuous Drone",
                            resonance = "Echoey",
                            qualitativeNotes = "Deep brassy baritone resonance, reverberated off wooden chair"
                        ),
                        smellProfile = com.example.data.model.SmellProfile(
                            pungencyScale = 3,
                            lingeringSeconds = 15,
                            dispersionRadiusMeters = 2.0,
                            thermalSensation = "Cool",
                            primaryOdorNote = "Earthy",
                            qualitativeNotes = "Mild musty aroma with low sulfur content"
                        ),
                        context = com.example.data.model.SubmissionContext(
                            locationType = "Office",
                            recentFoods = listOf("Oatmeal", "Black Coffee", "Apple"),
                            physicalSensation = "Sudden Relief"
                        ),
                        aiAnalysis = com.example.data.model.AiAnalysisResult(
                            taxonScientificName = "Flatus bombus baritonus",
                            taxonCommonName = "The Resonant Baritone",
                            confidenceScore = 0.94,
                            taxonomicRank = "Archetype",
                            classificationReasoning = "Low fundamental frequency (95 Hz) paired with high amplitude (86 dB) and low sulfur pungency index.",
                            healthIndicators = listOf("Optimal fiber transit", "Predominant aerophagia / swallowed nitrogen", "Low sulfate-reducing bacteria activity"),
                            gutHealthSummary = "Benign mechanical gas release driven by high carbohydrate breakdown and swallowed air rather than putrefactive fermentation.",
                            dietaryRecommendations = listOf("Healthy biome state; maintain steady hydration", "Pace coffee intake to decrease esophageal air gulping")
                        ),
                        agreeCount = 14
                    ),
                    FartSubmission(
                        authorName = "AromaHunter",
                        soundProfile = com.example.data.model.SoundProfile(
                            durationSeconds = 0.4,
                            decibels = 30,
                            pitchHz = 620,
                            waveform = "Silent",
                            resonance = "Muffled",
                            qualitativeNotes = "Undetectable acoustic presence; completely stealthy egress"
                        ),
                        smellProfile = com.example.data.model.SmellProfile(
                            pungencyScale = 9,
                            lingeringSeconds = 120,
                            dispersionRadiusMeters = 8.5,
                            thermalSensation = "Thermal Inferno",
                            primaryOdorNote = "Sulfur",
                            qualitativeNotes = "Dense hydrogen sulfide pungency, cleared the entire elevator"
                        ),
                        context = com.example.data.model.SubmissionContext(
                            locationType = "Elevator",
                            recentFoods = listOf("Hard Boiled Eggs", "Whey Protein", "Roasted Broccoli"),
                            physicalSensation = "Abdominal Cramp Relief"
                        ),
                        aiAnalysis = com.example.data.model.AiAnalysisResult(
                            taxonScientificName = "Sibilus sulfuris occultus",
                            taxonCommonName = "The Silent Sulfur Specter",
                            confidenceScore = 0.97,
                            taxonomicRank = "Archetype",
                            classificationReasoning = "Acoustically imperceptible with an extraordinary hydrogen sulfide and methanethiol dispersion footprint.",
                            healthIndicators = listOf("Hyper-active Desulfovibrio bloom", "High cysteine/methionine protein degradation", "Prolonged cecal transit time"),
                            gutHealthSummary = "Significant sulfur-reducing bacterial fermentation metabolizing sulfur-rich amino acids from eggs and whey protein.",
                            dietaryRecommendations = listOf("Rotate protein sources away from concentrated whey toward collagen or pea protein", "Consume activated polyphenols or berries to temper sulfur reduction", "Increase dietary soluble fibers (psyllium, acacia)")
                        ),
                        agreeCount = 28
                    )
                )

                for (s in samples) {
                    addSubmission(s)
                }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "submissions")
        }
    }

    companion object {
        fun getDefaultCanonicalTaxa(): List<Taxon> {
            return listOf(
                Taxon(
                    id = "bombus_grandis",
                    scientificName = "Flatus bombus grandis",
                    commonName = "The Thunderous Bass",
                    phylum = "Aeroflatulata",
                    flatusClass = "Sonoric",
                    flatusOrder = "Baritonis",
                    family = "Bombidae",
                    genus = "Flatus",
                    archetype = "bombus grandis",
                    species = "bombus grandis",
                    rarity = "Common",
                    acousticSignature = "75-95 dB, 50-120 Hz, continuous low-frequency drone",
                    olfactorySignature = "Low pungency (2-4), largely nitrogen & carbon dioxide",
                    description = "Characterized by immense acoustic energy generated by high volume expulsion through relaxed sphincter tissues.",
                    contributorCount = 84,
                    typicalDecibels = 85,
                    typicalPungency = 3
                ),
                Taxon(
                    id = "sulfuris_occultus",
                    scientificName = "Sibilus sulfuris occultus",
                    commonName = "The Silent Sulfur Specter (SBD)",
                    phylum = "Aeroflatulata",
                    flatusClass = "Mutata",
                    flatusOrder = "Olfactoris",
                    family = "Sulfuridae",
                    genus = "Sibilus",
                    archetype = "sulfuris occultus",
                    species = "sulfuris occultus",
                    rarity = "Rare",
                    acousticSignature = "<35 dB, near silent or subtle high whistle",
                    olfactorySignature = "Extremely high pungency (8-10), hydrogen sulfide & mercaptans",
                    description = "Acoustically imperceptible yet devastatingly persistent. Originates from intense bacterial fermentation of sulfur amino acids.",
                    contributorCount = 120,
                    typicalDecibels = 28,
                    typicalPungency = 9
                ),
                Taxon(
                    id = "tremolo_crepitans",
                    scientificName = "Tremolo staccato crepitans",
                    commonName = "The Machine Gun Staccato",
                    phylum = "Aeroflatulata",
                    flatusClass = "Sonoric",
                    flatusOrder = "Pulsatilis",
                    family = "Vibratidae",
                    genus = "Tremolo",
                    archetype = "staccato crepitans",
                    species = "staccato crepitans",
                    rarity = "Uncommon",
                    acousticSignature = "Rapid fluttering valve oscillations, 3-8 flaps per second",
                    olfactorySignature = "Moderate pungency (4-6), yeasty / fermented note",
                    description = "Produced when pressurized flatus escapes past tense epidermal membranes in rapid succession, mimicking automatic fire.",
                    contributorCount = 52,
                    typicalDecibels = 72,
                    typicalPungency = 5
                ),
                Taxon(
                    id = "trombonis_resonus",
                    scientificName = "Trombonis resonus",
                    commonName = "The Brass Glissando",
                    phylum = "Aeroflatulata",
                    flatusClass = "Sonoric",
                    flatusOrder = "Harmonica",
                    family = "Trombonidae",
                    genus = "Trombonis",
                    archetype = "resonus",
                    species = "resonus",
                    rarity = "Rare",
                    acousticSignature = "Pitch-shifting glissando from 400 Hz descending to 150 Hz",
                    olfactorySignature = "Pungency (5-7), cooked cabbage (methanethiol)",
                    description = "Features an audible musical pitch shift as internal rectal pressure drops during the release phase.",
                    contributorCount = 37,
                    typicalDecibels = 68,
                    typicalPungency = 6
                ),
                Taxon(
                    id = "thermo_pyroclasticus",
                    scientificName = "Vapor thermo-pyroclasticus",
                    commonName = "The Thermal Inferno",
                    phylum = "Aeroflatulata",
                    flatusClass = "Calorific",
                    flatusOrder = "Pyroclastica",
                    family = "Thermatidae",
                    genus = "Vapor",
                    archetype = "thermo-pyroclasticus",
                    species = "thermo-pyroclasticus",
                    rarity = "Legendary",
                    acousticSignature = "Hot hiss, steady friction hiss, 45-60 dB",
                    olfactorySignature = "Pungency (8-10), burning rubber, pungent acid notes",
                    description = "Accompanied by a distinct localized thermal radiation sensation. Strongly associated with spicy capsicum and bile secretion.",
                    contributorCount = 63,
                    typicalDecibels = 50,
                    typicalPungency = 9
                ),
                Taxon(
                    id = "populus_vespertinus",
                    scientificName = "Populus vespertinus",
                    commonName = "The Evening Firecracker",
                    phylum = "Aeroflatulata",
                    flatusClass = "Sonoric",
                    flatusOrder = "Explosiva",
                    family = "Populi",
                    genus = "Populus",
                    archetype = "vespertinus",
                    species = "vespertinus",
                    rarity = "Common",
                    acousticSignature = "<0.3s explosive snap, 80-90 dB, sharp impulse wave",
                    olfactorySignature = "Low pungency (1-3), instantaneous dissipation",
                    description = "Single instantaneous gaseous rupture. High velocity release of trapped air with minimal fermentation.",
                    contributorCount = 95,
                    typicalDecibels = 82,
                    typicalPungency = 2
                ),
                Taxon(
                    id = "zefyrus_inodorus",
                    scientificName = "Zefyrus inodorus",
                    commonName = "The Ghost Breeze",
                    phylum = "Aeroflatulata",
                    flatusClass = "Invisibilis",
                    flatusOrder = "Aura",
                    family = "Zefyridae",
                    genus = "Zefyrus",
                    archetype = "inodorus",
                    species = "inodorus",
                    rarity = "Uncommon",
                    acousticSignature = "Completely silent (<15 dB)",
                    olfactorySignature = "Odorless (pungency 1/10)",
                    description = "The elusive clean gas release. Composed almost purely of swallowed atmospheric nitrogen and oxygen without enteric fermentation.",
                    contributorCount = 44,
                    typicalDecibels = 15,
                    typicalPungency = 1
                ),
                Taxon(
                    id = "fermentum_cerevisiae",
                    scientificName = "Fermentum cerevisiae",
                    commonName = "The Baker's Yeast Surge",
                    phylum = "Aeroflatulata",
                    flatusClass = "Biochemica",
                    flatusOrder = "Fermentosa",
                    family = "Zymotica",
                    genus = "Fermentum",
                    archetype = "cerevisiae",
                    species = "cerevisiae",
                    rarity = "Rare",
                    acousticSignature = "Low bubbling acoustic rumble, 60-70 dB",
                    olfactorySignature = "Sweet, bready, fermented malt aroma (pungency 6)",
                    description = "Result of lively carbohydrate and yeast fermentation in the colon following consumption of unfiltered ales, sourdough, or legume starches.",
                    contributorCount = 29,
                    typicalDecibels = 65,
                    typicalPungency = 6
                )
            )
        }
    }
}
