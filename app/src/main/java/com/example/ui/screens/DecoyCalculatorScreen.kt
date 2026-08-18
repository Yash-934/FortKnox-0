package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Ultra-realistic Stealth Decoy Calculator Screen.
 *
 * Implements:
 * - Fully functional arithmetic & scientific calculator (Add, Subtract, Multiply, Divide, Percent, Square Root, Trig, Log).
 * - Multi-vector covert triggers:
 *   1. Secret passcode formula: Entering `7777=` or `9999=` unlocks the real master vault.
 *   2. Long-press on display or header for 4+ seconds.
 *   3. Corner tap sequence: Top-Left -> Top-Right -> Bottom-Left -> Bottom-Right.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DecoyCalculatorScreen(
    onTriggerSecretVault: () -> Unit
) {
    var displayValue by remember { mutableStateOf("0") }
    var expressionHistory by remember { mutableStateOf("") }
    var previousValue by remember { mutableStateOf<Double?>(null) }
    var pendingOperator by remember { mutableStateOf<String?>(null) }
    var isNewInput by remember { mutableStateOf(true) }
    var showScientific by remember { mutableStateOf(false) }

    // Covert Corner tap sequence tracker
    val cornerTaps = remember { mutableStateListOf<Int>() }

    fun registerCornerTap(corner: Int) {
        cornerTaps.add(corner)
        if (cornerTaps.size > 4) {
            cornerTaps.removeAt(0)
        }
        // Sequence: 1 (TL) -> 2 (TR) -> 3 (BL) -> 4 (BR)
        if (cornerTaps.toList() == listOf(1, 2, 3, 4)) {
            onTriggerSecretVault()
        }
    }

    val df = remember { DecimalFormat("#,###.########") }

    fun handleDigit(d: String) {
        if (isNewInput || displayValue == "0") {
            displayValue = d
            isNewInput = false
        } else {
            if (displayValue.length < 15) {
                displayValue += d
            }
        }
    }

    fun handleDot() {
        if (isNewInput) {
            displayValue = "0."
            isNewInput = false
        } else if (!displayValue.contains(".")) {
            displayValue += "."
        }
    }

    fun handleClear() {
        displayValue = "0"
        expressionHistory = ""
        previousValue = null
        pendingOperator = null
        isNewInput = true
    }

    fun handleBackspace() {
        if (displayValue.length > 1) {
            displayValue = displayValue.dropLast(1)
        } else {
            displayValue = "0"
            isNewInput = true
        }
    }

    fun calculate(op: String, a: Double, b: Double): Double {
        return when (op) {
            "+" -> a + b
            "-" -> a - b
            "×" -> a * b
            "÷" -> if (b != 0.0) a / b else Double.NaN
            else -> b
        }
    }

    fun handleOperator(op: String) {
        val currentNum = displayValue.replace(",", "").toDoubleOrNull() ?: 0.0
        val prev = previousValue
        if (prev != null && pendingOperator != null && !isNewInput) {
            val result = calculate(pendingOperator!!, prev, currentNum)
            displayValue = df.format(result)
            previousValue = result
        } else {
            previousValue = currentNum
        }
        pendingOperator = op
        expressionHistory = "${df.format(previousValue)} $op"
        isNewInput = true
    }

    fun handleEquals() {
        // Covert Vault Passcode Trigger!
        val rawInput = displayValue.replace(",", "")
        if (rawInput == "7777" || rawInput == "9999" || expressionHistory.startsWith("7777") || expressionHistory.startsWith("9999")) {
            onTriggerSecretVault()
            return
        }

        val currentNum = rawInput.toDoubleOrNull() ?: 0.0
        val prev = previousValue
        val op = pendingOperator
        if (prev != null && op != null) {
            val result = calculate(op, prev, currentNum)
            expressionHistory = "${df.format(prev)} $op ${df.format(currentNum)} ="
            displayValue = if (result.isNaN()) "Error" else df.format(result)
            previousValue = null
            pendingOperator = null
            isNewInput = true
        }
    }

    fun handleUnary(func: String) {
        val raw = displayValue.replace(",", "").toDoubleOrNull() ?: 0.0
        val res = when (func) {
            "%" -> raw / 100.0
            "√" -> if (raw >= 0) sqrt(raw) else Double.NaN
            "+/-" -> -raw
            "sin" -> sin(Math.toRadians(raw))
            "cos" -> cos(Math.toRadians(raw))
            "ln" -> if (raw > 0) ln(raw) else Double.NaN
            else -> raw
        }
        displayValue = if (res.isNaN()) "Error" else df.format(res)
        isNewInput = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = { onTriggerSecretVault() },
                                onClick = { }
                            )
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFFFFB74D))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Calculator",
                            fontWeight = FontWeight.Medium,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showScientific = !showScientific }) {
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = "Toggle Scientific",
                            tint = if (showScientific) Color(0xFFFFB74D) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E242B)
                )
            )
        },
        containerColor = Color(0xFF13171C)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Invisible corner tap targets for covert entry
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.TopStart)
                    .combinedClickable(onClick = { registerCornerTap(1) })
            )
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.TopEnd)
                    .combinedClickable(onClick = { registerCornerTap(2) })
            )
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.BottomStart)
                    .combinedClickable(onClick = { registerCornerTap(3) })
            )
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.BottomEnd)
                    .combinedClickable(onClick = { registerCornerTap(4) })
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Calculator Display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(androidx.compose.foundation.shape.CutCornerShape(16.dp))
                        .background(Color(0xFF1B2028))
                        .combinedClickable(
                            onLongClick = { onTriggerSecretVault() },
                            onClick = { }
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = expressionHistory,
                        fontSize = 18.sp,
                        color = Color(0xFF8892B0),
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayValue,
                        fontSize = if (displayValue.length > 9) 36.sp else 48.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        modifier = Modifier.testTag("calculator_display")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Optional Scientific Row
                AnimatedVisibility(visible = showScientific) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalcButton("sin", Color(0xFF263238), Color(0xFF80CBC4), Modifier.weight(1f)) { handleUnary("sin") }
                        CalcButton("cos", Color(0xFF263238), Color(0xFF80CBC4), Modifier.weight(1f)) { handleUnary("cos") }
                        CalcButton("ln", Color(0xFF263238), Color(0xFF80CBC4), Modifier.weight(1f)) { handleUnary("ln") }
                        CalcButton("√", Color(0xFF263238), Color(0xFF80CBC4), Modifier.weight(1f)) { handleUnary("√") }
                    }
                }

                // Keypad Buttons Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Row 1: AC, +/-, %, ÷
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        CalcButton("AC", Color(0xFF455A64), Color(0xFFFF8A80), Modifier.weight(1f)) { handleClear() }
                        CalcButton("+/-", Color(0xFF37474F), Color(0xFFECEFF1), Modifier.weight(1f)) { handleUnary("+/-") }
                        CalcButton("%", Color(0xFF37474F), Color(0xFFECEFF1), Modifier.weight(1f)) { handleUnary("%") }
                        CalcButton("÷", Color(0xFFE65100), Color.White, Modifier.weight(1f)) { handleOperator("÷") }
                    }

                    // Row 2: 7, 8, 9, ×
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        CalcButton("7", Color(0xFF263238), Color.White, Modifier.weight(1f)) { handleDigit("7") }
                        CalcButton("8", Color(0xFF263238), Color.White, Modifier.weight(1f)) { handleDigit("8") }
                        CalcButton("9", Color(0xFF263238), Color.White, Modifier.weight(1f)) { handleDigit("9") }
                        CalcButton("×", Color(0xFFE65100), Color.White, Modifier.weight(1f)) { handleOperator("×") }
                    }

                    // Row 3: 4, 5, 6, -
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        CalcButton("4", Color(0xFF263238), Color.White, Modifier.weight(1f)) { handleDigit("4") }
                        CalcButton("5", Color(0xFF263238), Color.White, Modifier.weight(1f)) { handleDigit("5") }
                        CalcButton("6", Color(0xFF263238), Color.White, Modifier.weight(1f)) { handleDigit("6") }
                        CalcButton("-", Color(0xFFE65100), Color.White, Modifier.weight(1f)) { handleOperator("-") }
                    }

                    // Row 4: 1, 2, 3, +
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        CalcButton("1", Color(0xFF263238), Color.White, Modifier.weight(1f)) { handleDigit("1") }
                        CalcButton("2", Color(0xFF263238), Color.White, Modifier.weight(1f)) { handleDigit("2") }
                        CalcButton("3", Color(0xFF263238), Color.White, Modifier.weight(1f)) { handleDigit("3") }
                        CalcButton("+", Color(0xFFE65100), Color.White, Modifier.weight(1f)) { handleOperator("+") }
                    }

                    // Row 5: 0, ., ⌫, =
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        CalcButton("0", Color(0xFF263238), Color.White, Modifier.weight(1f)) { handleDigit("0") }
                        CalcButton(".", Color(0xFF263238), Color.White, Modifier.weight(1f)) { handleDot() }
                        CalcButton("⌫", Color(0xFF37474F), Color(0xFFECEFF1), Modifier.weight(1f)) { handleBackspace() }
                        CalcButton("=", Color(0xFF00C853), Color.White, Modifier.weight(1f)) { handleEquals() }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(64.dp)
            .clip(CircleShape),
        shape = CircleShape,
        color = containerColor
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}
