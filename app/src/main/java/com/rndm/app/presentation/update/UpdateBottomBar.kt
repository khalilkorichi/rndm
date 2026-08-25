package com.rndm.app.presentation.update

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.core.theme.*
import com.rndm.app.domain.model.UpdateInfo
import java.io.File

@Composable
fun UpdateBottomBar(
    uiState: UpdateUiState,
    onUpdateClick: (UpdateInfo) -> Unit,
    onPauseClick: (UpdateInfo) -> Unit,
    onResumeClick: (UpdateInfo) -> Unit,
    onInstallClick: (UpdateInfo, File) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientColors = remember(uiState) {
        when (uiState) {
            is UpdateUiState.UpdateAvailable -> listOf(UpdateBluePrimary, UpdateBlueDark)
            is UpdateUiState.Downloading, is UpdateUiState.Paused -> listOf(UpdateBlueNavy, UpdateBluePrimary)
            is UpdateUiState.ReadyToInstall -> listOf(UpdateSuccessGreen, UpdateSuccessGreenDark)
            is UpdateUiState.DownloadFailed, is UpdateUiState.Error -> listOf(UpdateErrorRed, UpdateErrorRedDark)
            else -> listOf(UpdateBluePrimary, UpdateBlueDark)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(18.dp))
            .background(brush = Brush.horizontalGradient(gradientColors), shape = RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable { onNavigateToUpdates() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            when (uiState) {
                is UpdateUiState.UpdateAvailable -> Icon(Icons.Default.NewReleases, contentDescription = null, tint = Color.White)
                is UpdateUiState.Downloading -> Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White)
                is UpdateUiState.Paused -> Icon(Icons.Default.PauseCircle, contentDescription = null, tint = Color.White)
                is UpdateUiState.ReadyToInstall -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                else -> Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Color.White)
            }

            Column(modifier = Modifier.weight(1f)) {
                when (uiState) {
                    is UpdateUiState.UpdateAvailable -> {
                        Text("تحديث جديد متوفر! 🎉", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("الإصدار v${uiState.info.versionName} جاهز للتحميل.", color = Color.White.copy(0.85f), fontSize = 11.sp, maxLines = 1)
                    }
                    is UpdateUiState.Downloading -> {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("جاري التحميل: ${uiState.speed}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("${uiState.progress}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { uiState.progress / 100f },
                            color = UpdateSuccessGreenLight,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                    is UpdateUiState.Paused -> {
                        Text("تم إيقاف التحميل مؤقتاً (${uiState.progress}%)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    is UpdateUiState.ReadyToInstall -> {
                        Text("اكتمل التحميل بنجاح!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("اضغط لتثبيت الإصدار الجديد الآن.", color = Color.White.copy(0.85f), fontSize = 11.sp)
                    }
                    is UpdateUiState.DownloadFailed -> {
                        Text("فشل تحميل التحديث", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(uiState.error, color = Color.White.copy(0.85f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    else -> {}
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                when (uiState) {
                    is UpdateUiState.UpdateAvailable -> {
                        Button(
                            onClick = { onUpdateClick(uiState.info) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = UpdateBlueDark),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("تحميل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    is UpdateUiState.Downloading -> {
                        IconButton(onClick = { onPauseClick(uiState.info) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Pause, contentDescription = "إيقاف", tint = Color.White)
                        }
                    }
                    is UpdateUiState.Paused -> {
                        IconButton(onClick = { onResumeClick(uiState.info) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "استئناف", tint = Color.White)
                        }
                    }
                    is UpdateUiState.ReadyToInstall -> {
                        Button(
                            onClick = { onInstallClick(uiState.info, uiState.localApkFile) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = UpdateSuccessGreenDark),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("تثبيت", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {}
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White.copy(0.7f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
