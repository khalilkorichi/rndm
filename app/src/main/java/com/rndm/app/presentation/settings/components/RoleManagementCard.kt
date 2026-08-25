package com.rndm.app.presentation.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rndm.app.R
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.domain.model.UserProfile
import com.rndm.app.domain.model.UserRole

@Composable
fun RoleManagementCard(
    userRole: UserRole,
    currentUserProfile: UserProfile? = null,
    onOpenAdminLogin: () -> Unit,
    onOpenUserManagement: () -> Unit = {},
    onViewRoleInfo: () -> Unit = {},
    onLogoutAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (userRole) {
                            UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                            UserRole.USER -> Icons.Default.VerifiedUser
                            else -> Icons.Default.Person
                        },
                        contentDescription = null,
                        tint = when (userRole) {
                            UserRole.ADMIN -> MaterialTheme.colorScheme.primary
                            UserRole.USER -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الصلاحيات وإدارة الحساب",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = when (userRole) {
                        UserRole.ADMIN -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        UserRole.USER -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onViewRoleInfo() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val roleIcon = when (userRole) {
                            UserRole.ADMIN -> R.drawable.ic_crown
                            UserRole.USER -> R.drawable.ic_person
                            else -> null
                        }
                        if (roleIcon != null) {
                            Icon(
                                painter = painterResource(id = roleIcon),
                                contentDescription = null,
                                tint = when (userRole) {
                                    UserRole.ADMIN -> MaterialTheme.colorScheme.primary
                                    UserRole.USER -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = when (userRole) {
                                UserRole.ADMIN -> "مدير (Admin)"
                                UserRole.USER -> "مستخدم (User)"
                                else -> "وضع الضيف"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (userRole) {
                                UserRole.ADMIN -> MaterialTheme.colorScheme.primary
                                UserRole.USER -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (currentUserProfile != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onViewRoleInfo() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = currentUserProfile.displayName.ifBlank { currentUserProfile.username },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentUserProfile.email,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "تفاصيل الصلاحيات",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = when (userRole) {
                    UserRole.ADMIN -> "تملك صلاحيات كاملة كمدير لإدارة ومزامنة البطولات وترقية المستخدمين الآخرين وقبول طلبات تعديل النتائج."
                    UserRole.USER -> "حسابك مسجل ويتيح لك إنشاء ومزامنة البطولات ومشاركة أكوادها وإرسال طلبات تعديل النتائج للأدمن."
                    else -> "وضع الضيف يتيح لك إجراء القرعات ومتابعة البطولات المشتركة عبر كود الانضمام. سجل حسابك للاستفادة من المزامنة والطلبات."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (userRole == UserRole.ADMIN) {
                // Admin Special Action: Manage users and promote
                Button(
                    onClick = onOpenUserManagement,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "إدارة الصلاحيات",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إدارة الصلاحيات وترقية المستخدمين", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (userRole == UserRole.ADMIN || userRole == UserRole.USER) {
                OutlinedButton(
                    onClick = onLogoutAdmin,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "تسجيل الخروج",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (userRole == UserRole.ADMIN) "تسجيل الخروج من وضع المدير" else "تسجيل الخروج من الحساب",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Button(
                    onClick = onOpenAdminLogin,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "تسجيل الدخول / إنشاء حساب",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل الدخول / إنشاء حساب جديد", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
