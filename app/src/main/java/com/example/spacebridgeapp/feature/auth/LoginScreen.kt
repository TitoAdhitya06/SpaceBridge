package com.example.spacebridgeapp.feature.auth

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spacebridgeapp.R
import com.example.spacebridgeapp.ui.theme.SpaceBridgeTheme

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToDriveOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.NavigateToHome -> onNavigateToHome()
                is AuthEvent.NavigateToDriveOnboarding -> onNavigateToDriveOnboarding()
                is AuthEvent.ShowSnackbar -> {
                    // Handled through inline banner or snackbar
                }
                is AuthEvent.LaunchConsent -> {}
            }
        }
    }

    LoginContent(
        uiState = uiState,
        onSignInClick = {
            viewModel.signIn(context as? Activity ?: context)
        },
        modifier = modifier,
    )
}

@Composable
fun LoginContent(
    uiState: AuthUiState,
    onSignInClick: () -> Unit,
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
        Surface(
            modifier =
                Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            shadowElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Architectural notch indicator
                Box(
                    modifier =
                        Modifier
                            .width(48.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.surfaceMuted),
                )

                Spacer(modifier = Modifier.height(24.dp))

                // App Icon container with cloud + document symbols
                Box(
                    modifier =
                        Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.surfaceMuted),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(34.dp),
                        )
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier =
                                Modifier
                                    .size(26.dp)
                                    .offset(x = (-8).dp, y = 6.dp),
                        )
                    }

                    // Dot stamp accent
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-8).dp, y = 8.dp)
                                .clip(CircleShape)
                                .background(colors.primary),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // App title
                Text(
                    text = stringResource(R.string.login_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = stringResource(R.string.login_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.widthIn(max = 320.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error alert banner
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    uiState.errorMessage?.let { message ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFDF2F2))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB91C1C),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Google SSO Button
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !uiState.isLoading, onClick = onSignInClick),
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.borderStrong),
                    shadowElevation = 1.dp,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (uiState.isLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.primary,
                                )
                                Text(
                                    text = stringResource(R.string.login_connecting),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary,
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_google),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = stringResource(R.string.login_continue_google),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Divider line with ledger badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = colors.border,
                        thickness = 1.dp,
                    )
                    Text(
                        text = stringResource(R.string.login_archival_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textTertiary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = colors.border,
                        thickness = 1.dp,
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Security trust badge
                Surface(
                    shape = CircleShape,
                    color = colors.surfaceMuted,
                    border = BorderStroke(1.dp, colors.border),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = colors.success,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.login_privacy_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Privacy statement
                Text(
                    text = stringResource(R.string.login_privacy_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.widthIn(max = 340.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    com.example.spacebridgeapp.ui.theme.SpaceBridgeAppTheme {
        LoginContent(
            uiState = AuthUiState(),
            onSignInClick = {},
        )
    }
}
