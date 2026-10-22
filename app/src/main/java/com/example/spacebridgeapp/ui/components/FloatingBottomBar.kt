package com.example.spacebridgeapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spacebridgeapp.ui.theme.SpaceBridgeAppTheme
import com.example.spacebridgeapp.ui.theme.SpaceBridgeTheme

import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.LocalConfiguration

enum class BottomBarTab {
    NOTES,
    FILES,
}

@Composable
fun FloatingBottomBar(
    currentTab: BottomBarTab,
    onTabSelected: (BottomBarTab) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SpaceBridgeTheme.colors
    val configuration = LocalConfiguration.current
    val isCompactHeight = configuration.screenHeightDp < 500

    val barHeight = if (isCompactHeight) 52.dp else 64.dp
    val verticalPadding = if (isCompactHeight) 6.dp else 12.dp
    val fabSize = if (isCompactHeight) 46.dp else 54.dp
    val fabIconSize = if (isCompactHeight) 22.dp else 26.dp
    val centerSpacerWidth = if (isCompactHeight) 52.dp else 64.dp

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = verticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = colors.surface,
            modifier =
                Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .height(barHeight)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = colors.textPrimary.copy(alpha = 0.08f),
                        spotColor = colors.textPrimary.copy(alpha = 0.12f),
                    ).border(
                        width = 1.dp,
                        color = colors.border,
                        shape = RoundedCornerShape(32.dp),
                    ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Tab 1: Catatan
                BottomBarItem(
                    selected = currentTab == BottomBarTab.NOTES,
                    icon = Icons.AutoMirrored.Filled.Notes,
                    label = "Catatan",
                    onClick = { onTabSelected(BottomBarTab.NOTES) },
                    modifier = Modifier.weight(1f),
                    compact = isCompactHeight,
                )

                // Spacer untuk FAB tengah
                Spacer(modifier = Modifier.width(centerSpacerWidth))

                // Tab 2: Berkas Drive
                BottomBarItem(
                    selected = currentTab == BottomBarTab.FILES,
                    icon = Icons.Default.Folder,
                    label = "Berkas",
                    onClick = { onTabSelected(BottomBarTab.FILES) },
                    modifier = Modifier.weight(1f),
                    compact = isCompactHeight,
                )
            }
        }

        // Tombol FAB Utama di Tengah
        Box(
            modifier =
                Modifier
                    .size(fabSize)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = colors.primary.copy(alpha = 0.25f),
                        spotColor = colors.primary.copy(alpha = 0.35f),
                    ).background(colors.primary600, CircleShape)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = colors.textOnAccent),
                        onClick = onFabClick,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Buat baru",
                tint = colors.textOnAccent,
                modifier = Modifier.size(fabIconSize),
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = SpaceBridgeTheme.colors
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.primary600 else colors.textTertiary,
        label = "BottomBarContentColor",
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) colors.primary50 else colors.surface,
        label = "BottomBarBgColor",
    )

    val itemHeight = if (compact) 36.dp else 44.dp
    val iconSize = if (compact) 18.dp else 20.dp
    val fontSize = if (compact) 12.sp else 13.sp

    Box(
        modifier =
            modifier
                .height(itemHeight)
                .clip(RoundedCornerShape(20.dp))
                .background(bgColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = colors.primary500.copy(alpha = 0.15f)),
                    onClick = onClick,
                ).padding(horizontal = if (compact) 8.dp else 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(iconSize),
            )
            Spacer(modifier = Modifier.width(if (compact) 4.dp else 6.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = fontSize,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FloatingBottomBarPreview() {
    SpaceBridgeAppTheme(isDarkTheme = false) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(SpaceBridgeTheme.colors.background)
                    .padding(vertical = 24.dp),
        ) {
            FloatingBottomBar(
                currentTab = BottomBarTab.NOTES,
                onTabSelected = {},
                onFabClick = {},
            )
        }
    }
}
