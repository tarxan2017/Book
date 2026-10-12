package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.viewmodel.Screen

@Composable
fun LivingPageBottomNavBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .testTag("bottom_nav_bar")
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        // 1. Главная
        val isHome = currentScreen is Screen.Home
        NavigationBarItem(
            selected = isHome,
            onClick = { onNavigate(Screen.Home) },
            icon = {
                Icon(
                    imageVector = if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Главная"
                )
            },
            label = { Text("Главная", style = MaterialTheme.typography.labelMedium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_item_home")
        )

        // 2. Моя книга
        val isMyBook = currentScreen is Screen.MyBook || currentScreen is Screen.Reader
        NavigationBarItem(
            selected = isMyBook,
            onClick = { onNavigate(Screen.MyBook) },
            icon = {
                Icon(
                    imageVector = if (isMyBook) Icons.Filled.AutoStories else Icons.Outlined.AutoStories,
                    contentDescription = "Моя книга"
                )
            },
            label = { Text("Моя книга", style = MaterialTheme.typography.labelMedium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_item_my_book")
        )

        // 3. Друзья
        val isFriends = currentScreen is Screen.Friends || currentScreen is Screen.SendSurprise || currentScreen is Screen.ReceiveSurprise
        NavigationBarItem(
            selected = isFriends,
            onClick = { onNavigate(Screen.Friends) },
            icon = {
                Icon(
                    imageVector = if (isFriends) Icons.Filled.People else Icons.Outlined.People,
                    contentDescription = "Друзья"
                )
            },
            label = { Text("Друзья", style = MaterialTheme.typography.labelMedium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_item_friends")
        )

        // 4. Настройки
        val isSettings = currentScreen is Screen.Settings
        NavigationBarItem(
            selected = isSettings,
            onClick = { onNavigate(Screen.Settings) },
            icon = {
                Icon(
                    imageVector = if (isSettings) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "Настройки"
                )
            },
            label = { Text("Настройки", style = MaterialTheme.typography.labelMedium) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_item_settings")
        )
    }
}
