package com.pixelwater.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DevDeviceFakerCard(
    viewModel: WaterViewModel,
    appLanguage: String,
    modifier: Modifier = Modifier
) {
    val fakeEnabled by viewModel.fakeDeviceEnabled.collectAsStateWithLifecycle()
    val fakeBrand by viewModel.fakeDeviceBrand.collectAsStateWithLifecycle()
    val fakeModel by viewModel.fakeDeviceModel.collectAsStateWithLifecycle()

    ChunkySettingCard {
        Column(
            modifier = modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (appLanguage == "el") "Προσομοιωτής Ταυτότητας Συσκευής" else "Device Identity Faker",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                ChunkySettingSwitch(
                    checked = fakeEnabled,
                    onCheckedChange = {
                        viewModel.triggerButtonHaptic()
                        viewModel.updateFakeDevice(fakeBrand, fakeModel, it)
                    }
                )
            }

            Text(
                text = if (appLanguage == "el")
                    "Επιτρέπει τον έλεγχο συμπεριφοράς διεπαφής και συγκεκριμένων μοντέλων συσκευών."
                    else "Allows testing UI behavior, layout adaptations, and specific device models.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = fakeBrand,
                    onValueChange = { viewModel.updateFakeDevice(it, fakeModel, fakeEnabled) },
                    label = { Text(if (appLanguage == "el") "Μάρκα" else "Brand", fontSize = 12.sp) },
                    placeholder = { Text("Google", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = fakeModel,
                    onValueChange = { viewModel.updateFakeDevice(fakeBrand, it, fakeEnabled) },
                    label = { Text(if (appLanguage == "el") "Μοντέλο" else "Model", fontSize = 12.sp) },
                    placeholder = { Text("Pixel 8 Pro", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = if (appLanguage == "el") "Γρήγορες Προεπιλογές:" else "Developer Quick Presets:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val presets = listOf(
                "Pixel 6 Pro" to ("Google" to "Pixel 6 Pro"),
                "Pixel 8a" to ("Google" to "Pixel 8a"),
                "S24 Ultra" to ("Samsung" to "Galaxy S24 Ultra"),
                "iPhone 15 Pro" to ("Apple" to "iPhone 15 Pro Max")
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { (label, pair) ->
                    val (brand, model) = pair
                    SuggestionChip(
                        onClick = {
                            viewModel.triggerButtonHaptic()
                            viewModel.updateFakeDevice(brand, model, true)
                        },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
        }
    }
}
