package com.example.spacebridgeapp.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spacebridgeapp.R
import com.example.spacebridgeapp.ui.theme.SpaceBridgeTheme

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun DriveOnboardingScreen(
    viewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val consentLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            viewModel.handleConsentResult(result.resultCode, result.data)
        }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.NavigateToHome -> onNavigateToHome()
                is AuthEvent.LaunchConsent -> consentLauncher.launch(event.intentSenderRequest)
                else -> {}
            }
        }
    }

    DriveOnboardingContent(
        uiState = uiState,
        onConnectDriveClick = { viewModel.completeDriveOnboarding(context.findActivity()) },
        onSkipClick = { viewModel.skipDriveOnboarding() },
        modifier = modifier,
    )
}

@Composable
fun DriveOnboardingContent(
    uiState: AuthUiState,
    onConnectDriveClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Step & Context Ribbon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colors.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "2",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textOnAccent,
                        )
                    }
                    Text(
                        text = stringResource(R.string.drive_onboarding_step),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceMuted,
                    border = BorderStroke(1.dp, colors.border),
                ) {
                    Text(
                        text = stringResource(R.string.drive_onboarding_step_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textTertiary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Archival Document Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.border),
                shadowElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Emblem container: Folder + Add to drive
                    Box(
                        modifier =
                            Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F1FA)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderShared,
                            contentDescription = null,
                            tint = Color(0xFF2F6FB0),
                            modifier = Modifier.size(42.dp),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .size(26.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 2.dp, y = 2.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color(0xFF2F6FB0),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Title
                    Text(
                        text = stringResource(R.string.drive_onboarding_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description
                    Text(
                        text = stringResource(R.string.drive_onboarding_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Permission Points Ledger
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceMuted,
                        border = BorderStroke(1.dp, colors.border),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            PermissionCheckPoint(text = stringResource(R.string.drive_onboarding_point_1))
                            PermissionCheckPoint(text = stringResource(R.string.drive_onboarding_point_2))
                            PermissionCheckPoint(text = stringResource(R.string.drive_onboarding_point_3))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary Context Card / Workspace Verification
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = colors.surfaceMuted,
                border = BorderStroke(1.dp, colors.border),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.drive_onboarding_auto_sync),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = stringResource(R.string.drive_onboarding_auto_sync_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !uiState.isDriveConnecting, onClick = onConnectDriveClick),
                shape = RoundedCornerShape(10.dp),
                color = colors.primary,
                shadowElevation = 2.dp,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (uiState.isDriveConnecting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = colors.textOnAccent,
                            )
                            Text(
                                text = stringResource(R.string.drive_onboarding_connecting),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textOnAccent,
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = colors.textOnAccent,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = stringResource(R.string.drive_onboarding_btn_connect),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textOnAccent,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Skip Ghost Button
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onSkipClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.drive_onboarding_btn_skip),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Guarantee Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PrivacyTip,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.drive_onboarding_security_footer),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun PermissionCheckPoint(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEBF5EE)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF27774A),
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
            lineHeight = 20.sp,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DriveOnboardingScreenPreview() {
    com.example.spacebridgeapp.ui.theme.SpaceBridgeAppTheme {
        DriveOnboardingContent(
            uiState = AuthUiState(),
            onConnectDriveClick = {},
            onSkipClick = {},
        )
    }
}
