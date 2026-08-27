package com.rndm.app.presentation.draw.result

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.R
import com.rndm.app.core.ui.components.RndmButton
import com.rndm.app.core.ui.components.RndmButtonType
import com.rndm.app.core.ui.components.RndmTopAppBar
import com.rndm.app.domain.model.DrawType
import com.rndm.app.presentation.draw.result.components.PairingsResultView
import com.rndm.app.presentation.draw.result.components.SingleWinnerResultView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawResultScreen(
    profileId: Long,
    drawType: DrawType,
    onNavigateBack: () -> Unit,
    onRedoDraw: (Long, DrawType) -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: DrawResultViewModel = hiltViewModel()
) {
    LaunchedEffect(profileId, drawType) {
        viewModel.initialize(profileId, drawType)
    }

    val drawResult by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            RndmTopAppBar(
                title = "نتيجة القرعة",
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                RndmButton(
                    onClick = { onRedoDraw(profileId, drawType) },
                    type = RndmButtonType.PRIMARY,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_wheel),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "إعادة القرعة")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                RndmButton(
                    onClick = onNavigateToHome,
                    type = RndmButtonType.OUTLINED,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "العودة للرئيسية")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val result = drawResult
        if (result == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "جارٍ تحميل النتيجة...")
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                if (result.selectedItem != null) {
                    SingleWinnerResultView(winner = result.selectedItem.label)
                } else if (result.pairings.isNotEmpty()) {
                    PairingsResultView(pairings = result.pairings)
                }
            }
        }
    }
}
