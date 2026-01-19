package com.cinecam.cinematiccamera.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cinecam.cinematiccamera.domain.model.*
import com.cinecam.cinematiccamera.ui.theme.CineCamColors
import com.cinecam.cinematiccamera.ui.theme.CinematicCameraTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Settings Activity - Camera configuration
 */
@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            CinematicCameraTheme {
                SettingsScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    var resolution by remember { mutableStateOf(Resolution.FHD_1080P) }
    var bitrate by remember { mutableStateOf(Bitrate.HIGH) }
    var stabilizationEnabled by remember { mutableStateOf(true) }
    var logProfileEnabled by remember { mutableStateOf(false) }
    var auto180Rule by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CineCamColors.Surface,
                    titleContentColor = CineCamColors.OnBackground
                )
            )
        },
        containerColor = CineCamColors.Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video Quality Section
            item {
                SettingsSection(title = "Video Quality") {
                    // Resolution
                    SettingsDropdown(
                        label = "Resolution",
                        selected = resolution.displayName,
                        options = Resolution.entries.map { it.displayName },
                        onSelect = { name ->
                            resolution = Resolution.entries.first { it.displayName == name }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Bitrate
                    SettingsDropdown(
                        label = "Bitrate",
                        selected = bitrate.displayName,
                        options = Bitrate.entries.map { it.displayName },
                        onSelect = { name ->
                            bitrate = Bitrate.entries.first { it.displayName == name }
                        }
                    )
                }
            }
            
            // Cinematic Options Section
            item {
                SettingsSection(title = "Cinematic Options") {
                    SettingsSwitch(
                        label = "Auto 180° Shutter Rule",
                        description = "Automatically set shutter speed to 2x frame rate",
                        checked = auto180Rule,
                        onCheckedChange = { auto180Rule = it }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    SettingsSwitch(
                        label = "Log Profile Recording",
                        description = "Record in flat profile for color grading",
                        checked = logProfileEnabled,
                        onCheckedChange = { logProfileEnabled = it }
                    )
                }
            }
            
            // Stabilization Section
            item {
                SettingsSection(title = "Stabilization") {
                    SettingsSwitch(
                        label = "Electronic Stabilization (EIS)",
                        description = "Software-based image stabilization",
                        checked = stabilizationEnabled,
                        onCheckedChange = { stabilizationEnabled = it }
                    )
                }
            }
            
            // About Section
            item {
                SettingsSection(title = "About") {
                    Text(
                        text = "CineCam v1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CineCamColors.OnSurface
                    )
                    Text(
                        text = "Professional Cinematic Camera",
                        style = MaterialTheme.typography.bodySmall,
                        color = CineCamColors.OnSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = CineCamColors.Surface,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = CineCamColors.Primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = CineCamColors.OnBackground
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .width(180.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CineCamColors.Primary,
                    unfocusedBorderColor = CineCamColors.ButtonBorder,
                    focusedTextColor = CineCamColors.OnBackground,
                    unfocusedTextColor = CineCamColors.OnSurface
                )
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitch(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = CineCamColors.OnBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = CineCamColors.OnSurface.copy(alpha = 0.7f)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CineCamColors.Primary,
                checkedTrackColor = CineCamColors.Primary.copy(alpha = 0.5f)
            )
        )
    }
}
