package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.PasswordGenerator
import com.example.ui.components.CyberButton
import com.example.ui.components.CyberCard
import com.example.ui.components.PasswordStrengthBar
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

@Composable
fun PasswordGeneratorScreen(
    onCopyPassword: (String) -> Unit
) {
    var length by remember { mutableFloatStateOf(20f) }
    var includeUppercase by remember { mutableStateOf(true) }
    var includeLowercase by remember { mutableStateOf(true) }
    var includeDigits by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }
    var excludeAmbiguous by remember { mutableStateOf(false) }

    var generatedPassword by remember {
        mutableStateOf(
            PasswordGenerator.generatePassword(
                length = length.toInt(),
                includeUppercase = includeUppercase,
                includeLowercase = includeLowercase,
                includeDigits = includeDigits,
                includeSymbols = includeSymbols,
                excludeAmbiguous = excludeAmbiguous
            )
        )
    }

    fun regenerate() {
        generatedPassword = PasswordGenerator.generatePassword(
            length = length.toInt(),
            includeUppercase = includeUppercase,
            includeLowercase = includeLowercase,
            includeDigits = includeDigits,
            includeSymbols = includeSymbols,
            excludeAmbiguous = excludeAmbiguous
        )
    }

    val strengthAnalysis = remember(generatedPassword) {
        PasswordGenerator.evaluateStrength(generatedPassword)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "ENTROPY GENERATOR",
            color = CyberTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp
        )
        Text(
            text = "CRYPTOGRAPHICALLY SECURE PSEUDORANDOM GENERATOR",
            color = CyberCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Password Display Box
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GENERATED PASSWORD",
                        color = CyberTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(onClick = { regenerate() }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerate",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.CutCornerShape(10.dp))
                        .background(CyberSurfaceVariant)
                        .padding(16.dp)
                ) {
                    Text(
                        text = generatedPassword,
                        color = CyberTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                PasswordStrengthBar(analysis = strengthAnalysis)

                Spacer(modifier = Modifier.height(14.dp))

                CyberButton(
                    text = "COPY TO SECURE CLIPBOARD",
                    icon = Icons.Default.ContentCopy,
                    onClick = { onCopyPassword(generatedPassword) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Options Card
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PASSWORD LENGTH",
                        color = CyberTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${length.toInt()} chars",
                        color = CyberCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Slider(
                    value = length,
                    onValueChange = {
                        length = it
                        regenerate()
                    },
                    valueRange = 8f..64f,
                    steps = 56,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = CyberSurfaceVariant
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "CHARACTER SETS",
                    color = CyberTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                GeneratorOptionCheckbox(
                    label = "Uppercase Letters (A-Z)",
                    checked = includeUppercase,
                    onCheckedChange = {
                        includeUppercase = it
                        regenerate()
                    }
                )

                GeneratorOptionCheckbox(
                    label = "Lowercase Letters (a-z)",
                    checked = includeLowercase,
                    onCheckedChange = {
                        includeLowercase = it
                        regenerate()
                    }
                )

                GeneratorOptionCheckbox(
                    label = "Numeric Digits (0-9)",
                    checked = includeDigits,
                    onCheckedChange = {
                        includeDigits = it
                        regenerate()
                    }
                )

                GeneratorOptionCheckbox(
                    label = "Special Symbols (!@#$%^&*)",
                    checked = includeSymbols,
                    onCheckedChange = {
                        includeSymbols = it
                        regenerate()
                    }
                )

                GeneratorOptionCheckbox(
                    label = "Exclude Ambiguous Characters (0, O, 1, l, I)",
                    checked = excludeAmbiguous,
                    onCheckedChange = {
                        excludeAmbiguous = it
                        regenerate()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun GeneratorOptionCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = CyberCyan,
                uncheckedColor = CyberBorder,
                checkmarkColor = CyberBackground
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = CyberTextPrimary,
            fontSize = 13.sp
        )
    }
}
