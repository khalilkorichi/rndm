package com.rndm.app.domain.usecase.update

import com.rndm.app.domain.model.CheckingStep
import com.rndm.app.domain.model.UpdateInfo
import com.rndm.app.domain.repository.UpdateRepository
import javax.inject.Inject

class CheckForUpdateUseCase @Inject constructor(
    private val updateRepository: UpdateRepository
) {
    suspend operator fun invoke(onStep: suspend (CheckingStep) -> Unit = {}): Result<UpdateInfo> {
        return updateRepository.checkForUpdates(onStep)
    }
}
