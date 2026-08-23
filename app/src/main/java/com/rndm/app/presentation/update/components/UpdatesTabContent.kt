package com.rndm.app.presentation.update.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.BuildConfig
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.core.ui.components.RndmButton
import com.rndm.app.core.ui.components.RndmButtonType
import com.rndm.app.presentation.update.UpdateUiState
import com.rndm.app.presentation.update.UpdatesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UpdatesTabContent(
    viewModel: UpdatesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val checkingStep by viewModel.checkingStep.collectAsStateWithLifecycle()
    val downloadedApks by viewModel.downloadedApks.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val isChecking = uiState is UpdateUiState.Checking

    val buildDateStr = remember {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        sdf.format(Date(BuildConfig.BUILD_TIMESTAMP))
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current Version Info Card
        item {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "تطبيق RNDM",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "الإصدار المثبت: v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME}",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تاريخ بناء النسخة الحالية:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = buildDateStr,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Check for updates button
                    RndmButton(
                        onClick = { viewModel.checkForUpdates(isBackground = false) },
                        enabled = !isChecking,
                        type = RndmButtonType.PRIMARY,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري الفحص...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("فحص وجود تحديثات جديدة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Animated Checking Steps Indicator
        if (isChecking) {
            item {
                CheckingStepIndicator(step = checkingStep)
            }
        }

        // Active Update State Cards
        when (val state = uiState) {
            is UpdateUiState.UpdateAvailable -> {
                item {
                    UpdateCard(
                        info = state.info,
                        onDownloadClick = { viewModel.downloadUpdate(state.info) }
                    )
                }
            }
            is UpdateUiState.Downloading -> {
                item {
                    DownloadProgressCard(
                        progress = state.progress,
                        speed = state.speed,
                        eta = state.eta,
                        isPaused = false,
                        onPauseClick = { viewModel.pauseDownload(state.info) },
                        onResumeClick = { viewModel.resumeDownload(state.info) },
                        onCancelClick = { viewModel.cancelDownload(state.info) }
                    )
                }
            }
            is UpdateUiState.Paused -> {
                item {
                    DownloadProgressCard(
                        progress = state.progress,
                        speed = "",
                        eta = "",
                        isPaused = true,
                        onPauseClick = { viewModel.pauseDownload(state.info) },
                        onResumeClick = { viewModel.resumeDownload(state.info) },
                        onCancelClick = { viewModel.cancelDownload(state.info) }
                    )
                }
            }
            is UpdateUiState.ReadyToInstall -> {
                item {
                    ReadyToInstallCard(
                        info = state.info,
                        localFile = state.localApkFile,
                        onInstallClick = {
                            viewModel.installUpdate(context, state.info, state.localApkFile)
                        }
                    )
                }
            }
            is UpdateUiState.NoUpdate -> {
                item {
                    val checkTimeStr = remember(state.checkedAt) {
                        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        sdf.format(Date(state.checkedAt))
                    }

                    BentoCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "التطبيق محدث لأحدث إصدار! ✨",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "تمت المطابقة مع مستودع: ${state.repositoryName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "الإصدار المثبت محلياً:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "v${state.localVersion} (بناء ${state.localCode})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "إصدار المستودع المفحوص:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "v${state.remoteVersion} (بناء ${state.remoteCode})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "وقت آخر فحص:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = checkTimeStr,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is UpdateUiState.DownloadFailed -> {
                item {
                    BentoCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "فشل تحميل التحديث",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                            Text(
                                text = state.error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            RndmButton(
                                onClick = { viewModel.downloadUpdate(state.info) },
                                type = RndmButtonType.OUTLINED,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إعادة المحاولة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            is UpdateUiState.Error -> {
                item {
                    BentoCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "تعذر التحقق من التحديثات",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = state.error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            RndmButton(
                                onClick = { viewModel.checkForUpdates(isBackground = false) },
                                type = RndmButtonType.OUTLINED,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إعادة المحاولة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            else -> {}
        }

        // Locally Downloaded APKs Section
        if (downloadedApks.isNotEmpty()) {
            item {
                DownloadedApksSection(
                    apks = downloadedApks,
                    onInstallApk = { file -> viewModel.installDownloadedFile(context, file) },
                    onDeleteApk = { file -> viewModel.deleteDownloadedApk(file) },
                    onCopyToDownloads = { file ->
                        viewModel.copyApkToDownloads(file, file.name) { uri ->
                            if (uri != null) {
                                Toast.makeText(context, "تم حفظ ${file.name} في مجلد Downloads", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "تعذر نقل الملف للمجلد الخارجي", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }

        // Fallback Emergency Recovery Flow Card
        item {
            FallbackRecoveryFlowCard(
                onBackupClick = { /* Silent safety backup hook */ }
            )
        }
    }
}
