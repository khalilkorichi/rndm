package com.rndm.app.presentation.tournament.promotion

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.rndm.app.core.ui.components.RndmTopAppBar
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.R
import com.rndm.app.core.ui.components.RndmButton
import com.rndm.app.presentation.profile.detail.components.ProfileDetailSkeleton
import com.rndm.app.presentation.tournament.promotion.components.PromotionCandidateCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionCandidateScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBracket: (Long) -> Unit,
    viewModel: PromotionCandidateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isBracketGenerated) {
        if (uiState.isBracketGenerated) {
            uiState.tournament?.id?.let { onNavigateToBracket(it) }
        }
    }

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = "ترقية واكتمال المتأهلين",
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            ProfileDetailSkeleton(modifier = Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "المتأهلون المباشرون (${uiState.directQualifiers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.directQualifiers.forEach { participant ->
                        PromotionCandidateCard(
                            participant = participant,
                            badgeText = "متأهل مباشر",
                            iconRes = R.drawable.ic_check
                        )
                    }
                }

                if (uiState.promotedCandidates.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "المرشحون للترقية (أفضل أصحاب المركز الثالث)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.promotedCandidates.forEach { participant ->
                            PromotionCandidateCard(
                                participant = participant,
                                badgeText = "ترقية بالنقاط",
                                iconRes = R.drawable.ic_star
                            )
                        }
                    }
                }

                if (uiState.isTieBreakNeeded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "تساوي في النقاط وفارق الأهداف — يتطلب قرعة فصل",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.tiedCandidates.forEach { participant ->
                            PromotionCandidateCard(
                                participant = participant,
                                badgeText = if (uiState.selectedTieBreakWinner?.playerItemId == participant.playerItemId) "تم اختياره بالقرعة" else "مرشح متعادل",
                                iconRes = if (uiState.selectedTieBreakWinner?.playerItemId == participant.playerItemId) R.drawable.ic_target else null
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    RndmButton(
                        onClick = viewModel::performTieBreakDraw,
                        enabled = !uiState.isSpinning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_dice),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (uiState.isSpinning) "جارٍ سحب القرعة..."
                                else if (uiState.selectedTieBreakWinner == null) "إجراء قرعة الحسم الآن"
                                else "إعادة القرعة"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val isReadyToConfirm = !uiState.isTieBreakNeeded || uiState.selectedTieBreakWinner != null
                RndmButton(
                    onClick = viewModel::confirmAndGenerateBracket,
                    enabled = isReadyToConfirm,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تثبيت المتأهلين وبدء الأدوار الإقصائية")
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
