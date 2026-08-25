package com.rndm.app.presentation.admin.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rndm.app.core.theme.StatsWarningAmber
import com.rndm.app.core.theme.UpdateBluePrimary
import com.rndm.app.core.theme.UpdateSuccessGreen
import com.rndm.app.domain.model.UserProfile
import com.rndm.app.domain.model.UserRole

@Composable
fun LoginSuccessDialog(
    userRole: UserRole,
    userProfile: UserProfile?,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onContinue) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            LoginSuccessContent(
                userRole = userRole,
                userProfile = userProfile,
                onContinue = onContinue,
                modifier = Modifier.padding(20.dp)
            )
        }
    }
}

@Composable
fun LoginSuccessContent(
    userRole: UserRole,
    userProfile: UserProfile?,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val (roleTitle, roleSubtitle, roleBadgeText, roleColor, roleBgColor, roleIcon) = when (userRole) {
        UserRole.ADMIN -> RoleDisplayConfig(
            title = "مدير النظام (Admin)",
            subtitle = "أنت مسجل الآن كمدير وتملك كامل الصلاحيات الإدارية والسحابية",
            badgeText = "مدير بصلاحيات كاملة",
            accentColor = StatsWarningAmber, // Gold / Amber
            containerColor = StatsWarningAmber.copy(alpha = 0.15f),
            icon = Icons.Default.AdminPanelSettings
        )
        UserRole.USER -> RoleDisplayConfig(
            title = "مستخدم معتمد (User)",
            subtitle = "حسابك مفعل بنجاح وتملك صلاحيات إنشاء البطولات ومزامنتها",
            badgeText = "مستخدم معتمد",
            accentColor = MaterialTheme.colorScheme.tertiary,
            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
            icon = Icons.Default.VerifiedUser
        )
        UserRole.VIEWER -> RoleDisplayConfig(
            title = "مشاهد (Viewer)",
            subtitle = "يمكنك متابعة البطولات والقرعات المباشرة ومواكبة النتائج",
            badgeText = "صلاحيات المشاهدة 👁️",
            accentColor = UpdateBluePrimary,
            containerColor = UpdateBluePrimary.copy(alpha = 0.15f),
            icon = Icons.Default.Visibility
        )
        UserRole.GUEST -> RoleDisplayConfig(
            title = "وضع الضيف (Guest)",
            subtitle = "تستخدم التطبيق بوضع الضيف على هذا الجهاز",
            badgeText = "ضيف 🚶",
            accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            icon = Icons.Default.Person
        )
    }

    val permissionsList = when (userRole) {
        UserRole.ADMIN -> listOf(
            RolePermissionItem(
                icon = Icons.Default.AdminPanelSettings,
                title = "إدارة النظام والبطولات",
                desc = "إنشاء ومزامنة وتعديل وحذف أي بطولة سحابياً"
            ),
            RolePermissionItem(
                icon = Icons.Default.Group,
                title = "إدارة المستخدمين والترقيات",
                desc = "ترقية وتعيين مدراء جدد للنظام وتعديل صلاحياتهم"
            ),
            RolePermissionItem(
                icon = Icons.Default.Shield,
                title = "اعتماد طلبات تعديل النتائج",
                desc = "مراجعة وقبول أو رفض طلبات تصحيح نتائج المباريات"
            ),
            RolePermissionItem(
                icon = Icons.Default.CloudSync,
                title = "مزامنة فورية ودائمة",
                desc = "حفظ البيانات على خوادم Firebase السحابية بشكل فوري"
            )
        )
        UserRole.USER -> listOf(
            RolePermissionItem(
                icon = Icons.Default.Stars,
                title = "إنشاء ومزامنة البطولات",
                desc = "إنشاء بطولاتك الخاصة ومزامنتها على السحابة"
            ),
            RolePermissionItem(
                icon = Icons.Default.Key,
                title = "مشاركة أكواد البطولات",
                desc = "توليد كود مشاركة خاص لكل بطولة ليدخل به اللاعبون"
            ),
            RolePermissionItem(
                icon = Icons.Default.Shield,
                title = "إرسال طلبات التعديل",
                desc = "تقديم طلبات تعديل النتائج لإدارة النظام في البطولات المشتركة"
            ),
            RolePermissionItem(
                icon = Icons.Default.CloudSync,
                title = "حفظ السجلات والإحصائيات",
                desc = "حفظ إحصائياتك وقرعاتك في حسابك واسترجاعها بأي وقت"
            )
        )
        UserRole.VIEWER -> listOf(
            RolePermissionItem(
                icon = Icons.Default.Visibility,
                title = "متابعة مباشرة للبطولات",
                desc = "استعراض المباريات والنتائج والقرعات لحظياً"
            ),
            RolePermissionItem(
                icon = Icons.Default.Stars,
                title = "مشاهدة جداول الترتيب",
                desc = "متابعة جداول وترتيب الفرق واللاعبين"
            )
        )
        UserRole.GUEST -> listOf(
            RolePermissionItem(
                icon = Icons.Default.Person,
                title = "إجراء القرعات المحلية",
                desc = "إجراء القرعات وحفظها محلياً على هذا الجهاز فقط"
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success Header Badge
        Surface(
            shape = CircleShape,
            color = UpdateSuccessGreen.copy(alpha = 0.15f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = UpdateSuccessGreen,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "تم تسجيل الدخول بنجاح!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        val displayName = userProfile?.displayName?.ifBlank { null }
            ?: userProfile?.username?.ifBlank { null }
            ?: userProfile?.email?.substringBefore("@")
        if (displayName != null) {
            Text(
                text = "أهلاً بك، $displayName 👋",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (userProfile?.email?.isNotBlank() == true) {
            Text(
                text = userProfile.email,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Role Highlight Box
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = roleBgColor,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = roleColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = roleIcon,
                        contentDescription = null,
                        tint = roleColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = roleTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = roleColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = roleBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = roleColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = roleSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Permissions & Features Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "الصلاحيات والميزات المتاحة لدورك:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            permissionsList.forEach { perm ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = roleColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = perm.icon,
                                    contentDescription = null,
                                    tint = roleColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = perm.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = perm.desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Primary Action Button
        Button(
            onClick = onContinue,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "متابعة واستخدام التطبيق",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private data class RoleDisplayConfig(
    val title: String,
    val subtitle: String,
    val badgeText: String,
    val accentColor: Color,
    val containerColor: Color,
    val icon: ImageVector
)

private data class RolePermissionItem(
    val icon: ImageVector,
    val title: String,
    val desc: String
)
