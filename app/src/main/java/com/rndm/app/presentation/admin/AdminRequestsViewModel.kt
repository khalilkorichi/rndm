package com.rndm.app.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rndm.app.domain.model.AdminRequest
import com.rndm.app.domain.usecase.request.ApproveAdminRequestUseCase
import com.rndm.app.domain.usecase.request.ObserveAdminRequestsUseCase
import com.rndm.app.domain.usecase.request.RejectAdminRequestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminRequestsViewModel @Inject constructor(
    private val observeAdminRequestsUseCase: ObserveAdminRequestsUseCase,
    private val approveAdminRequestUseCase: ApproveAdminRequestUseCase,
    private val rejectAdminRequestUseCase: RejectAdminRequestUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminRequestsUiState())
    val uiState: StateFlow<AdminRequestsUiState> = _uiState.asStateFlow()

    init {
        observeRequests()
    }

    private fun observeRequests() {
        viewModelScope.launch {
            observeAdminRequestsUseCase()
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = "تعذر تحميل الطلبات السحابية") }
                }
                .collect { list ->
                    _uiState.update { it.copy(requests = list) }
                }
        }
    }

    fun onFilterSelected(filter: RequestFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun onOpenRejectDialog(request: AdminRequest) {
        _uiState.update { it.copy(selectedRequestForReject = request, rejectReason = "") }
    }

    fun onDismissRejectDialog() {
        _uiState.update { it.copy(selectedRequestForReject = null, rejectReason = "") }
    }

    fun onRejectReasonChanged(value: String) {
        _uiState.update { it.copy(rejectReason = value) }
    }

    fun approveRequest(requestId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null, successMessage = null) }
            val result = approveAdminRequestUseCase(requestId)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        successMessage = "تمت الموافقة وتطبيق التعديل على البطولة بنجاح"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "فشل اعتماد الطلب"
                    )
                }
            }
        }
    }

    fun confirmRejectRequest() {
        val request = _uiState.value.selectedRequestForReject ?: return
        val reason = _uiState.value.rejectReason.trim().ifBlank { "تم الرفض بواسطة الأدمن" }

        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null, successMessage = null) }
            val result = rejectAdminRequestUseCase(request.id, reason)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        selectedRequestForReject = null,
                        rejectReason = "",
                        successMessage = "تم رفض الطلب بنجاح"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "فشل رفض الطلب"
                    )
                }
            }
        }
    }

    fun clearFeedbackMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
