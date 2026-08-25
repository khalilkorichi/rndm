package com.rndm.app.presentation.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rndm.app.core.theme.UpdateSuccessGreen
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.domain.model.AdminRequest
import com.rndm.app.domain.model.RequestStatus
import com.rndm.app.domain.model.RequestType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminRequestsTabContent(
    viewModel: AdminRequestsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedbackMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedbackMessages()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Metrics Card
            item {
                AdminRequestsSummaryCard(
                    pendingCount = uiState.pendingCount,
                    approvedCount = uiState.approvedCount,
                    rejectedCount = uiState.rejectedCount
                )
            }

            // Filter Chips Bar
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(RequestFilter.entries, key = { it.name }) { filter ->
                        val isSelected = uiState.selectedFilter == filter
                        val count = when (filter) {
                            RequestFilter.ALL -> uiState.requests.size
                            RequestFilter.PENDING -> uiState.pendingCount
                            RequestFilter.APPROVED -> uiState.approvedCount
                            RequestFilter.REJECTED -> uiState.rejectedCount
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onFilterSelected(filter) },
                            label = {
                                Text(
                                    text = "${filter.title} ($count)",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Requests List
            if (uiState.filteredRequests.isEmpty()) {
                item {
                    EmptyRequestsPlaceholder(filter = uiState.selectedFilter)
                }
            } else {
                items(uiState.filteredRequests, key = { it.id }) { request ->
                    AdminRequestCard(
                        request = request,
                        isActionInProgress = uiState.isActionInProgress,
                        onApprove = { viewModel.approveRequest(request.id) },
                        onReject = { viewModel.onOpenRejectDialog(request) }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }

    // Rejection Dialog with Reason input
    if (uiState.selectedRequestForReject != null) {
        RejectReasonDialog(
            request = uiState.selectedRequestForReject!!,
            reason = uiState.rejectReason,
            isActionInProgress = uiState.isActionInProgress,
            onReasonChange = viewModel::onRejectReasonChanged,
            onConfirm = viewModel::confirmRejectRequest,
            onDismiss = viewModel::onDismissRejectDialog
        )
    }
}

@Composable
private fun AdminRequestsSummaryCard(
    pendingCount: Int,
    approvedCount: Int,
    rejectedCount: Int
) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "لوحة طلبات المستخدمين",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "صلاحيات الأدمن",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "راجع طلبات تعديل النتائج وترتيب المباريات وتبديل اللاعبين المقدمة من المستخدمين ووافق عليها أو ارفضها فورياً.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricItem(
                    label = "قيد الانتظار",
                    count = pendingCount,
                    color = com.rndm.app.core.theme.UpdateWarningAmber,
                    icon = Icons.Default.HourglassTop,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "المقبولة",
                    count = approvedCount,
                    color = UpdateSuccessGreen,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "المرفوضة",
                    count = rejectedCount,
                    color = MaterialTheme.colorScheme.error,
                    icon = Icons.Default.Cancel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    count: Int,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdminRequestCard(
    request: AdminRequest,
    isActionInProgress: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val dateFormatted = remember(request.createdAt) {
        val sdf = SimpleDateFormat("dd/MM - HH:mm", Locale.getDefault())
        sdf.format(Date(request.createdAt))
    }

    val typeTitle = when (request.type) {
        RequestType.CHANGE_SCORE -> "تعديل نتيجة مباراة"
        RequestType.SWAP_MATCH_ORDER -> "تغيير ترتيب المباريات"
        RequestType.SWAP_PLAYERS -> "تبديل الأشخاص في المباريات"
        RequestType.PLAYER_REPLACE -> "تعديل / استبدال لاعب"
        RequestType.PUBLISH_TOURNAMENT -> "اعتماد ونشر بطولة"
        RequestType.GENERAL -> "طلب عام"
    }

    val typeIcon = when (request.type) {
        RequestType.CHANGE_SCORE -> Icons.Default.SportsScore
        RequestType.SWAP_MATCH_ORDER -> Icons.Default.SwapVert
        RequestType.SWAP_PLAYERS -> Icons.Default.People
        RequestType.PLAYER_REPLACE -> Icons.Default.Person
        RequestType.PUBLISH_TOURNAMENT -> Icons.Default.CloudUpload
        RequestType.GENERAL -> Icons.Default.Info
    }

    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Type Icon & Name + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = typeIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = typeTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "البطولة: ${request.tournamentName.ifBlank { "غير محدد" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                RequestStatusBadge(status = request.status)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Requester Info & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${request.requesterName} (${request.requesterEmail.ifBlank { "مستخدم" }})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details / Payload Preview
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (request.type == RequestType.CHANGE_SCORE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${request.playerOneName ?: "لاعب 1"} ضد ${request.playerTwoName ?: "لاعب 2"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "النتيجة المقترحة: ${request.scoreOne ?: 0} - ${request.scoreTwo ?: 0}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (request.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = request.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (request.status == RequestStatus.REJECTED && !request.adminNote.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "سبب الرفض: ${request.adminNote}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Actions for PENDING requests
            if (request.status == RequestStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        enabled = !isActionInProgress,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "رفض",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("رفض الطلب", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onApprove,
                        enabled = !isActionInProgress,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UpdateSuccessGreen,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "موافقة",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("موافقة واعتماد", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestStatusBadge(status: RequestStatus) {
    val (bgColor, textColor, text) = when (status) {
        RequestStatus.PENDING -> Triple(com.rndm.app.core.theme.UpdateWarningAmber.copy(alpha = 0.15f), com.rndm.app.core.theme.UpdateWarningAmber, "قيد الانتظار")
        RequestStatus.APPROVED -> Triple(UpdateSuccessGreen.copy(alpha = 0.15f), UpdateSuccessGreen, "تمت الموافقة")
        RequestStatus.REJECTED -> Triple(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), MaterialTheme.colorScheme.error, "مرفوض")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyRequestsPlaceholder(filter: RequestFilter) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = when (filter) {
                    RequestFilter.ALL -> "لا توجد طلبات واردة حالياً"
                    RequestFilter.PENDING -> "لا توجد طلبات معلقة بانتظار المراجعة"
                    RequestFilter.APPROVED -> "لا توجد طلبات تمت الموافقة عليها"
                    RequestFilter.REJECTED -> "لا توجد طلبات مرفوضة"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "أي طلب يقدمه المستخدمون لتعديل المباريات سيظهر هنا فوراً",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RejectReasonDialog(
    request: AdminRequest,
    reason: String,
    isActionInProgress: Boolean,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "رفض طلب المستخدم",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "يرجى كتابة سبب الرفض ليتم إبلاغ صاحب الطلب به:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text("سبب الرفض (اختياري)") },
                    placeholder = { Text("مثال: تم إدخال نتيجة غير متطابقة") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = onConfirm,
                        enabled = !isActionInProgress,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("تأكيد الرفض", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
