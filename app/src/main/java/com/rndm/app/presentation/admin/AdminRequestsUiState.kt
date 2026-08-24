package com.rndm.app.presentation.admin

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.AdminRequest
import com.rndm.app.domain.model.RequestStatus

enum class RequestFilter(val title: String) {
    ALL("الكل"),
    PENDING("قيد الانتظار"),
    APPROVED("المقبولة"),
    REJECTED("المرفوضة")
}

@Immutable
data class AdminRequestsUiState(
    val requests: List<AdminRequest> = emptyList(),
    val selectedFilter: RequestFilter = RequestFilter.ALL,
    val selectedRequestForReject: AdminRequest? = null,
    val rejectReason: String = "",
    val isActionInProgress: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val filteredRequests: List<AdminRequest>
        get() = when (selectedFilter) {
            RequestFilter.ALL -> requests
            RequestFilter.PENDING -> requests.filter { it.status == RequestStatus.PENDING }
            RequestFilter.APPROVED -> requests.filter { it.status == RequestStatus.APPROVED }
            RequestFilter.REJECTED -> requests.filter { it.status == RequestStatus.REJECTED }
        }

    val pendingCount: Int
        get() = requests.count { it.status == RequestStatus.PENDING }

    val approvedCount: Int
        get() = requests.count { it.status == RequestStatus.APPROVED }

    val rejectedCount: Int
        get() = requests.count { it.status == RequestStatus.REJECTED }
}
