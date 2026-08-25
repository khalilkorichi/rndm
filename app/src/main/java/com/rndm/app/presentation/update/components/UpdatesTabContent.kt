package com.rndm.app.presentation.update.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.BuildConfig
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.core.ui.components.RndmButton
import com.rndm.app.core.ui.components.RndmButtonType
import com.rndm.app.domain.model.CheckingStep
import com.rndm.app.domain.model.UpdateInfo
import com.rndm.app.presentation.update.UpdateUiState
import com.rndm.app.presentation.update.UpdatesViewModel
import java.io.File
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Unified Main Update Hub Card (دمج صندوق الحالة والتحميل)
        item {
            MainUpdateHubCard(
                uiState = uiState,
                checkingStep = checkingStep,
                onCheckClick = { viewModel.checkForUpdates(isBackground = false) },
                onDownloadClick = { info -> viewModel.downloadUpdate(info) },
                onPauseClick = { info -> viewModel.pauseDownload(info) },
                onResumeClick = { info -> viewModel.resumeDownload(info) },
                onCancelClick = { info -> viewModel.cancelDownload(info) },
                onInstallClick = { info, file -> viewModel.installUpdate(context, info, file) },
                context = context
            )
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

/**
 * الصندوق الرئيسي الموحد لكافة حالات التحديثات (دمج صندوق الفحص مع صندوق التفاصيل والتحميل)
 */
@Composable
private fun MainUpdateHubCard(
    uiState: UpdateUiState,
    checkingStep: CheckingStep,
    onCheckClick: () -> Unit,
    onDownloadClick: (UpdateInfo) -> Unit,
    onPauseClick: (UpdateInfo) -> Unit,
    onResumeClick: (UpdateInfo) -> Unit,
    onCancelClick: (UpdateInfo) -> Unit,
    onInstallClick: (UpdateInfo, File) -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var showShaDialog by remember { mutableStateOf(false) }
    var targetSha256 by remember { mutableStateOf("") }

    val buildDateStr = remember {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        sdf.format(Date(BuildConfig.BUILD_TIMESTAMP))
    }

    val isChecking = uiState is UpdateUiState.Checking

    BentoCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header Row (App identity + Current Version + Status Badge)
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
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (uiState) {
                                    is UpdateUiState.UpdateAvailable -> MaterialTheme.colorScheme.primaryContainer
                                    is UpdateUiState.NoUpdate -> com.rndm.app.core.theme.UpdateSuccessGreen.copy(alpha = 0.15f)
                                    is UpdateUiState.DownloadFailed, is UpdateUiState.Error -> com.rndm.app.core.theme.UpdateErrorRed.copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (uiState) {
                                is UpdateUiState.UpdateAvailable -> Icons.Default.NewReleases
                                is UpdateUiState.NoUpdate -> Icons.Default.CheckCircle
                                is UpdateUiState.DownloadFailed, is UpdateUiState.Error -> Icons.Default.WarningAmber
                                else -> Icons.Default.SystemUpdate
                            },
                            contentDescription = null,
                            tint = when (uiState) {
                                is UpdateUiState.UpdateAvailable -> MaterialTheme.colorScheme.primary
                                is UpdateUiState.NoUpdate -> com.rndm.app.core.theme.UpdateSuccessGreen
                                is UpdateUiState.DownloadFailed, is UpdateUiState.Error -> com.rndm.app.core.theme.UpdateErrorRed
                                else -> MaterialTheme.colorScheme.primary
                            },
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

                // Dynamic Status Badge on the right
                Surface(
                    color = when (uiState) {
                        is UpdateUiState.UpdateAvailable -> com.rndm.app.core.theme.UpdateSuccessGreen.copy(alpha = 0.15f)
                        is UpdateUiState.NoUpdate -> com.rndm.app.core.theme.UpdateSuccessGreen.copy(alpha = 0.15f)
                        is UpdateUiState.Downloading -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        is UpdateUiState.Paused -> com.rndm.app.core.theme.UpdateWarningAmber.copy(alpha = 0.15f)
                        is UpdateUiState.ReadyToInstall -> com.rndm.app.core.theme.UpdateSuccessGreen.copy(alpha = 0.2f)
                        is UpdateUiState.DownloadFailed, is UpdateUiState.Error -> com.rndm.app.core.theme.UpdateErrorRed.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when (uiState) {
                            is UpdateUiState.UpdateAvailable -> "تحديث متوفر"
                            is UpdateUiState.NoUpdate -> "الإصدار الأحدث"
                            is UpdateUiState.Downloading -> "%${uiState.progress} جاري التحميل"
                            is UpdateUiState.Paused -> "متوقف مؤقتاً"
                            is UpdateUiState.ReadyToInstall -> "جاهز للتثبيت"
                            is UpdateUiState.DownloadFailed, is UpdateUiState.Error -> "تنبيه خطأ"
                            else -> "v${BuildConfig.VERSION_NAME}"
                        },
                        color = when (uiState) {
                            is UpdateUiState.UpdateAvailable -> com.rndm.app.core.theme.UpdateSuccessGreen
                            is UpdateUiState.NoUpdate -> com.rndm.app.core.theme.UpdateSuccessGreen
                            is UpdateUiState.Downloading -> MaterialTheme.colorScheme.primary
                            is UpdateUiState.Paused -> com.rndm.app.core.theme.UpdateWarningAmber
                            is UpdateUiState.ReadyToInstall -> com.rndm.app.core.theme.UpdateSuccessGreen
                            is UpdateUiState.DownloadFailed, is UpdateUiState.Error -> com.rndm.app.core.theme.UpdateErrorRed
                            else -> MaterialTheme.colorScheme.primary
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // 2. Animated Body Content based on UpdateUiState
            AnimatedContent(
                targetState = uiState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "update_hub_content"
            ) { state ->
                when (state) {
                    is UpdateUiState.Idle -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

                            RndmButton(
                                onClick = onCheckClick,
                                enabled = !isChecking,
                                type = RndmButtonType.PRIMARY,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("فحص وجود تحديثات جديدة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is UpdateUiState.Checking -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "جاري التحقق من التحديثات...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            CheckingStepInlineView(step = checkingStep)
                        }
                    }

                    is UpdateUiState.UpdateAvailable -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Version Transition Banner
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "v${BuildConfig.VERSION_NAME}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "v${state.info.versionName}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Text(
                                        text = formatApkSize(state.info.apkSize),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            // Release Notes Container
                            if (!state.info.releaseNotes.isNullOrBlank()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.FormatListBulleted,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "سجل التغييرات:",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = state.info.releaseNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            // SHA-256 Badge & Action
                            if (state.info.apkSha256.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Shield,
                                            contentDescription = null,
                                            tint = com.rndm.app.core.theme.UpdateSuccessGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "بصمة SHA-256 موثقة",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            targetSha256 = state.info.apkSha256
                                            showShaDialog = true
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("نسخ الهاش", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            // Download Action Button
                            RndmButton(
                                onClick = { onDownloadClick(state.info) },
                                type = RndmButtonType.PRIMARY,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تحميل وتثبيت التحديث الآن", fontWeight = FontWeight.Bold)
                            }

                            // Re-check option
                            TextButton(
                                onClick = onCheckClick,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إعادة الفحص", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    is UpdateUiState.Downloading, is UpdateUiState.Paused -> {
                        val isPaused = state is UpdateUiState.Paused
                        val progress = if (state is UpdateUiState.Downloading) state.progress else (state as UpdateUiState.Paused).progress
                        val speed = if (state is UpdateUiState.Downloading) state.speed else ""
                        val eta = if (state is UpdateUiState.Downloading) state.eta else ""
                        val info = if (state is UpdateUiState.Downloading) state.info else (state as UpdateUiState.Paused).info

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isPaused) "التحميل متوقف مؤقتاً" else "جاري تحميل التحديث v${info.versionName}...",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$progress%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            LinearProgressIndicator(
                                progress = { (progress / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (isPaused) com.rndm.app.core.theme.UpdateWarningAmber else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            if (!isPaused && speed.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "السرعة: $speed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (eta.isNotBlank()) {
                                        Text(
                                            text = "المتبقي: $eta",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isPaused) {
                                    RndmButton(
                                        onClick = { onResumeClick(info) },
                                        type = RndmButtonType.PRIMARY,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("استئناف", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    RndmButton(
                                        onClick = { onPauseClick(info) },
                                        type = RndmButtonType.OUTLINED,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("إيقاف مؤقت", fontWeight = FontWeight.Bold)
                                    }
                                }

                                RndmButton(
                                    onClick = { onCancelClick(info) },
                                    type = RndmButtonType.OUTLINED,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إلغاء", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    is UpdateUiState.ReadyToInstall -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = com.rndm.app.core.theme.UpdateSuccessGreen.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = com.rndm.app.core.theme.UpdateSuccessGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "تم تحميل التحديث بنجاح!",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "تمت مطابقة وفحص بصمة SHA-256 بأمان.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            RndmButton(
                                onClick = { onInstallClick(state.info, state.localApkFile) },
                                type = RndmButtonType.PRIMARY,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تثبيت التحديث الآن", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is UpdateUiState.NoUpdate -> {
                        val checkTimeStr = remember(state.checkedAt) {
                            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            sdf.format(Date(state.checkedAt))
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
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
                                            text = "حالة التطبيق:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "محدث لأحدث إصدار ✨",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = com.rndm.app.core.theme.UpdateSuccessGreen
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "الإصدار المفحوص:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "v${state.remoteVersion} (بناء ${state.remoteCode})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
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

                            RndmButton(
                                onClick = onCheckClick,
                                type = RndmButtonType.OUTLINED,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إعادة فحص التحديثات", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is UpdateUiState.DownloadFailed -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = com.rndm.app.core.theme.UpdateErrorRed.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = com.rndm.app.core.theme.UpdateErrorRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "فشل تحميل التحديث",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = com.rndm.app.core.theme.UpdateErrorRed
                                        )
                                    }
                                    Text(
                                        text = state.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            RndmButton(
                                onClick = { onDownloadClick(state.info) },
                                type = RndmButtonType.PRIMARY,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إعادة محاولة التحميل", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is UpdateUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = com.rndm.app.core.theme.UpdateWarningAmber.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = com.rndm.app.core.theme.UpdateWarningAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "تعذر التحقق من التحديثات",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = com.rndm.app.core.theme.UpdateWarningAmber
                                        )
                                    }
                                    Text(
                                        text = state.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            RndmButton(
                                onClick = onCheckClick,
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
        }
    }

    // SHA Dialog
    if (showShaDialog) {
        AlertDialog(
            onDismissRequest = { showShaDialog = false },
            title = {
                Text("بصمة التحقق SHA-256", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "تُستخدم هذه البصمة للتحقق من سلامة وأمان ملف التحديث قبل التثبيت:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = targetSha256,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(targetSha256))
                        Toast.makeText(context, "تم نسخ البصمة للحافظة", Toast.LENGTH_SHORT).show()
                        showShaDialog = false
                    }
                ) {
                    Text("نسخ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShaDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

/**
 * خطوط خطوات الفحص المضمنة داخل الصندوق الموحد
 */
@Composable
private fun CheckingStepInlineView(step: CheckingStep) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CheckingStepLine(
            title = "قراءة الإصدار المحلي والمطابقة",
            isActive = step is CheckingStep.ReadingLocalVersion,
            isDone = step !is CheckingStep.Idle && step !is CheckingStep.ReadingLocalVersion
        )
        CheckingStepLine(
            title = "فحص خادم GitHub Raw CDN (update.json)",
            isActive = step is CheckingStep.FetchingManifest,
            isDone = step is CheckingStep.ComparingVersions || step is CheckingStep.Success
        )
        CheckingStepLine(
            title = "الفحص الاحتياطي عبر GitHub Releases API",
            isActive = step is CheckingStep.FetchingReleaseFallback,
            isDone = step is CheckingStep.Success
        )
        CheckingStepLine(
            title = "مقارنة التواقيع وبصمة SHA-256",
            isActive = step is CheckingStep.ComparingVersions,
            isDone = step is CheckingStep.Success
        )
    }
}

@Composable
private fun CheckingStepLine(
    title: String,
    isActive: Boolean,
    isDone: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isDone -> com.rndm.app.core.theme.UpdateSuccessGreen
                        isActive -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = when {
                isActive -> MaterialTheme.colorScheme.primary
                isDone -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
            fontWeight = if (isActive || isDone) FontWeight.Medium else FontWeight.Normal
        )
    }
}
