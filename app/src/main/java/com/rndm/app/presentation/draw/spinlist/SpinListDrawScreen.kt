package com.rndm.app.presentation.draw.spinlist

import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.R
import com.rndm.app.core.ui.components.EmptyState
import com.rndm.app.core.ui.components.RndmButton
import com.rndm.app.core.ui.components.RndmButtonType
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.core.util.Constants
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpinListDrawScreen(
    profileId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToResult: () -> Unit,
    viewModel: SpinListDrawViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(profileId) {
        viewModel.initializeWithProfileId(profileId)
    }

    LaunchedEffect(uiState.targetScrollIndex) {
        if (uiState.targetScrollIndex > 0) {
            listState.animateScrollToItem(
                index = uiState.targetScrollIndex,
                scrollOffset = 0
            )
            delay(Constants.AUTO_NAVIGATE_DELAY_MS)
            onNavigateToResult()
        }
    }

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = "القائمة المتدرجة (Spin List)",
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                RndmButton(
                    onClick = viewModel::startSpin,
                    enabled = !uiState.isSpinning,
                    type = RndmButtonType.PRIMARY,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (uiState.isSpinning) "جارِ التدوير..." else "بدء التدوير")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val items = uiState.profile?.items ?: emptyList()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (items.isNotEmpty()) {
                // Repeating items 10 times to provide infinite scrolling sensation
                val extendedList = List(10) { items }.flatten()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.shapes.large
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Center Selection Indicator Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.shapes.small
                            )
                    )

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(
                            count = extendedList.size,
                            key = { index -> "$index-${extendedList[index].id}" }
                        ) { index ->
                            val item = extendedList[index]
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                EmptyState(
                    icon = painterResource(id = R.drawable.ic_spinlist),
                    title = "لا توجد عناصر في هذا البروفايل",
                    description = "أضف أسماء أو عناصر إلى البروفايل للبدء في تدوير القائمة العشوائية",
                    actionText = "العودة",
                    actionIcon = painterResource(id = R.drawable.ic_arrow_back),
                    onActionClick = onNavigateBack
                )
            }
        }
    }
}
