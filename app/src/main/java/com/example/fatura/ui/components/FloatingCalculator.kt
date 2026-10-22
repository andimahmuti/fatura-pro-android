package com.example.fatura.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.fatura.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FloatingCalculator(viewModel: MainViewModel) {
    val isVisible by viewModel.isFloatingCalculatorVisible.collectAsState()
    val isMinimized by viewModel.isCalculatorMinimized.collectAsState()
    if (!isVisible) return

    var offsetX by remember { mutableStateOf(50f) }
    var offsetY by remember { mutableStateOf(1000f) }

    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }
    var memory by remember { mutableStateOf(0.0) }
    var history by remember { mutableStateOf(listOf<String>()) }
    val historyState = rememberLazyListState()
    val currentScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Popup(
        offset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt()),
        properties = PopupProperties(focusable = false), 
        onDismissRequest = { }
    ) {
        if (isMinimized) {
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                    .clickable { viewModel.setCalculatorMinimized(false) },
                shape = CircleShape,
                color = Color(0xFFD4AF37),
                shadowElevation = 8.dp
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = Color.White, modifier = Modifier.padding(12.dp))
            }
        } else {
            Card(
                modifier = Modifier
                    .width(240.dp) // Slightly wider for more space
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DragHandle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Spacer(Modifier.width(4.dp))
                            Text("Kalkulatori", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row {
                            IconButton(onClick = { viewModel.setCalculatorMinimized(true) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Remove, contentDescription = "Minimize", modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { viewModel.toggleFloatingCalculator(false) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp), tint = Color.Red)
                            }
                        }
                    }

                    // Mini Screen - Expanded with Memory Display
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp) // Slightly taller
                            .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                            .padding(4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Memory Display Row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            if (memory != 0.0) {
                                Surface(
                                    color = Color(0xFFD4AF37).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(2.dp)
                                ) {
                                    Text(
                                        " MEM: ${if (memory % 1.0 == 0.0) memory.toLong().toString() else memory.toString()} ",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD4AF37)
                                    )
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            state = historyState,
                            horizontalAlignment = Alignment.End
                        ) {
                            items(history) { line ->
                                Text(line, fontSize = 9.sp, color = Color.Gray, maxLines = 1)
                            }
                        }
                        
                        if (history.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.LightGray.copy(alpha = 0.2f)))
                        }

                        val display = if (expression.isEmpty()) result else expression
                        
                        LaunchedEffect(display) {
                            currentScrollState.animateScrollTo(Int.MAX_VALUE)
                        }

                        Text(
                            text = display,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(currentScrollState),
                            textAlign = TextAlign.End
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    val buttons = listOf(
                        listOf("M+", "BACK", "C", "/"),
                        listOf("7", "8", "9", "*"),
                        listOf("4", "5", "6", "-"),
                        listOf("1", "2", "3", "+"),
                        listOf("0", ".", "=")
                    )

                    buttons.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { char ->
                                Button(
                                    onClick = {
                                        when (char) {
                                            "C" -> { 
                                                expression = ""
                                                result = "0"
                                                history = emptyList()
                                                memory = 0.0 
                                            }
                                            "BACK" -> { if (expression.isNotEmpty()) expression = expression.substring(0, expression.length - 1) }
                                            "M+" -> {
                                                try {
                                                    // If we have an expression, evaluate it first
                                                    val valToAdd = if (expression.isNotEmpty()) {
                                                        evaluateSimpleExpression(expression)
                                                    } else {
                                                        result.toDoubleOrNull() ?: 0.0
                                                    }
                                                    memory += valToAdd
                                                    // Optional: clear current after adding to memory
                                                    // expression = ""
                                                    // result = "0"
                                                } catch (_: Exception) {}
                                            }
                                            "=" -> {
                                                if (expression.isNotEmpty()) {
                                                    try {
                                                        val res = evaluateSimpleExpression(expression)
                                                        val resStr = if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
                                                        val newHistory = history.toMutableList()
                                                        newHistory.add("$expression = $resStr")
                                                        if (newHistory.size > 20) newHistory.removeAt(0)
                                                        history = newHistory
                                                        result = resStr
                                                        expression = ""
                                                        scope.launch { if (history.isNotEmpty()) historyState.animateScrollToItem(history.size - 1) }
                                                    } catch (_: Exception) { result = "Gabim" }
                                                }
                                            }
                                            "/", "*", "+", "-" -> {
                                                if (expression.isEmpty() && result != "0") {
                                                    expression = result + char
                                                } else {
                                                    expression += char
                                                }
                                            }
                                            else -> { expression += char }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = if (char == "=" || char == "C" || char == "M+" || char == "BACK") 
                                        ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                                        else ButtonDefaults.buttonColors(),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(char, fontWeight = FontWeight.Bold, fontSize = if (char.length > 1) 10.sp else 16.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

private fun evaluateSimpleExpression(expression: String): Double {
    if (expression.isEmpty()) return 0.0
    val tokens = mutableListOf<String>()
    var number = ""
    for (char in expression) {
        if (char in "0123456789.") {
            number += char
        } else if (char in "+-*/") {
            if (number.isNotEmpty()) tokens.add(number)
            tokens.add(char.toString())
            number = ""
        }
    }
    if (number.isNotEmpty()) tokens.add(number)
    if (tokens.isEmpty()) return 0.0
    if (tokens.size == 1) return tokens[0].toDoubleOrNull() ?: 0.0

    var i = 0
    while (i < tokens.size) {
        if (tokens[i] == "*" || tokens[i] == "/") {
            if (i == 0 || i + 1 >= tokens.size) break
            val op = tokens[i]
            val left = tokens[i - 1].toDoubleOrNull() ?: 0.0
            val right = tokens[i + 1].toDoubleOrNull() ?: 0.0
            val res = if (op == "*") left * right else {
                if (right == 0.0) 0.0 else left / right
            }
            tokens[i - 1] = res.toString()
            tokens.removeAt(i)
            tokens.removeAt(i)
            i--
        }
        i++
    }

    var result = tokens[0].toDoubleOrNull() ?: 0.0
    i = 1
    while (i < tokens.size) {
        val op = tokens[i]
        if (i + 1 >= tokens.size) break
        val nextVal = tokens[i + 1].toDoubleOrNull() ?: 0.0
        result = if (op == "+") result + nextVal else result - nextVal
        i += 2
    }
    return result
}
