package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
enum class MatchStage(val displayName: String) {
    GROUP_STAGE("دور المجموعات"),
    PROMOTION_PLAYOFF("مباراة الترشح"),
    ROUND_OF_64("دور الـ 64"),
    ROUND_OF_32("دور الـ 32"),
    ROUND_OF_16("دور الـ 16"),
    QUARTER_FINALS("ربع النهائي"),
    SEMI_FINALS("نصف النهائي"),
    THIRD_PLACE("تحديد المركز الثالث"),
    FINAL("النهائي")
}
