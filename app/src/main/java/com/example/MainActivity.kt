package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.model.FlatusSubmission
import com.example.data.repository.AiTaxonomyRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.TaxonomyRepository
import com.example.ui.screens.analysis.AnalysisDetailScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.feed.FeedScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.submit.SubmitScreen
import com.example.ui.screens.taxonomy.TaxonomyCatalogScreen
import com.example.ui.theme.BioCyan80
import com.example.ui.theme.BioTeal80
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.FlatudexTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var taxonomyRepository: TaxonomyRepository
    private lateinit var aiRepository: AiTaxonomyRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MANDATORY: Step 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        authRepository = AuthRepository(this)
        taxonomyRepository = TaxonomyRepository(this)
        aiRepository = AiTaxonomyRepository()

        // Background auto sign-in attempt
        authRepository.attemptAutoSignIn(lifecycleScope)

        setContent {
            FlatudexTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    AppRoot(
                        authRepository = authRepository,
                        taxonomyRepository = taxonomyRepository,
                        aiRepository = aiRepository
                    )
                }
            }
        }
    }
}

@Composable
fun AppRoot(
    authRepository: AuthRepository,
    taxonomyRepository: TaxonomyRepository,
    aiRepository: AiTaxonomyRepository
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var showTutorial by remember {
        mutableStateOf(!com.example.ui.components.TutorialPreferences.hasSeenTutorial(context))
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (showTutorial) {
        com.example.ui.components.OnboardingTutorialDialog(
            onDismiss = {
                com.example.ui.components.TutorialPreferences.markTutorialSeen(context)
                showTutorial = false
            }
        )
    }

    if (currentUser == null) {
        AuthScreen(
            authRepository = authRepository,
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            },
            onOpenTutorial = {
                showTutorial = true
            }
        )
    } else {
        val uid = currentUser!!.uid
        val displayName = currentUser!!.displayName ?: "Taxonomist"
        val email = currentUser!!.email ?: ""

        key(uid) {
            MainAppNavigation(
                currentUserId = uid,
                currentUserDisplayName = displayName,
                currentUserEmail = email,
                authRepository = authRepository,
                taxonomyRepository = taxonomyRepository,
                aiRepository = aiRepository,
                onOpenTutorial = {
                    showTutorial = true
                }
            )
        }
    }
}

enum class SubScreen {
    NONE,
    SUBMIT,
    DETAIL
}

@Composable
fun MainAppNavigation(
    currentUserId: String,
    currentUserDisplayName: String,
    currentUserEmail: String,
    authRepository: AuthRepository,
    taxonomyRepository: TaxonomyRepository,
    aiRepository: AiTaxonomyRepository,
    onOpenTutorial: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentSubScreen by remember { mutableStateOf(SubScreen.NONE) }
    var selectedSubmission by remember { mutableStateOf<FlatusSubmission?>(null) }

    Scaffold(
        bottomBar = {
            if (currentSubScreen == SubScreen.NONE) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                if (selectedTab == 0) Icons.Default.Public else Icons.Outlined.Public,
                                contentDescription = "Feed"
                            )
                        },
                        label = { Text("Global Feed") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_feed_tab")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                if (selectedTab == 1) Icons.Default.AccountTree else Icons.Outlined.AccountTree,
                                contentDescription = "Taxonomy"
                            )
                        },
                        label = { Text("Taxonomy") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_taxonomy_tab")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                if (selectedTab == 2) Icons.Default.Person else Icons.Outlined.Person,
                                contentDescription = "Profile"
                            )
                        },
                        label = { Text("Credentials") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_profile_tab")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentSubScreen) {
                SubScreen.SUBMIT -> {
                    SubmitScreen(
                        currentUserId = currentUserId,
                        currentUserDisplayName = currentUserDisplayName,
                        taxonomyRepository = taxonomyRepository,
                        aiRepository = aiRepository,
                        onSubmissionPublished = {
                            currentSubScreen = SubScreen.NONE
                            selectedTab = 0 // Return to global feed to see published entry
                        },
                        onBack = {
                            currentSubScreen = SubScreen.NONE
                        }
                    )
                }

                SubScreen.DETAIL -> {
                    selectedSubmission?.let { submission ->
                        AnalysisDetailScreen(
                            submission = submission,
                            taxonomyRepository = taxonomyRepository,
                            onBack = {
                                currentSubScreen = SubScreen.NONE
                                selectedSubmission = null
                            }
                        )
                    } ?: run {
                        currentSubScreen = SubScreen.NONE
                    }
                }

                SubScreen.NONE -> {
                    when (selectedTab) {
                        0 -> FeedScreen(
                            currentUserId = currentUserId,
                            currentUserDisplayName = currentUserDisplayName,
                            taxonomyRepository = taxonomyRepository,
                            onNavigateToSubmit = {
                                currentSubScreen = SubScreen.SUBMIT
                            },
                            onSelectSubmission = { sub ->
                                selectedSubmission = sub
                                currentSubScreen = SubScreen.DETAIL
                            },
                            onOpenTutorial = onOpenTutorial,
                            onNavigateToBadges = {
                                selectedTab = 2
                            }
                        )

                        1 -> TaxonomyCatalogScreen(
                            taxonomyRepository = taxonomyRepository
                        )

                        2 -> ProfileScreen(
                            currentUserId = currentUserId,
                            currentUserDisplayName = currentUserDisplayName,
                            currentUserEmail = currentUserEmail,
                            authRepository = authRepository,
                            taxonomyRepository = taxonomyRepository,
                            onOpenTutorial = onOpenTutorial,
                            onSignOutComplete = {
                                // Handled via auth state listener
                            }
                        )
                    }
                }
            }
        }
    }
}
