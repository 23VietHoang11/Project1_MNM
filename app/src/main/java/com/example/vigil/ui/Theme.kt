package com.example.vigil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object C {
    val Bg = Color(0xFF0D0B1E)
    val Card = Color(0xFF181530)
    val Border = Color(0xFF2A2650)
    val Orange = Color(0xFFFF6B35)
    val Yellow = Color(0xFFFFC94D)
    val Purple = Color(0xFF9B7BFF)
    val Pink = Color(0xFFFF5A7A)
    val Green = Color(0xFF4CC38A)
    val Blue = Color(0xFF4DA8E8)
    val Text = Color(0xFFF5EBDD)
    val Muted = Color(0xFF8E88B3)
}

@Composable
fun VigilTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        primary = C.Orange, background = C.Bg, surface = C.Card,
        onBackground = C.Text, onSurface = C.Text
    ),
    content = content
)

@Composable
fun VCard(modifier: Modifier = Modifier, border: Color = C.Border, onClick: (() -> Unit)? = null,
          content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(C.Card).border(1.dp, border, shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun SectionLabel(t: String) = Text(
    t.uppercase(), color = C.Muted, fontSize = 12.sp, letterSpacing = 2.sp,
    fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
)

@Composable
fun ProgressBar(p: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF0F0D22))) {
        Box(Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(color))
    }
}

@Composable
fun BigButton(text: String, color: Color = C.Orange, textColor: Color = Color.White, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(color)
            .clickable { onClick() }.padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
}

@Composable
fun Chip(text: String, color: Color = C.Yellow, onClick: (() -> Unit)? = null) = Text(
    text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold,
    modifier = Modifier
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF0F0D22))
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(horizontal = 12.dp, vertical = 8.dp)
)

@Composable
fun Quote(t: String) = Text(
    t, color = C.Muted, fontSize = 15.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
)

@Composable
fun StatBadge(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}
