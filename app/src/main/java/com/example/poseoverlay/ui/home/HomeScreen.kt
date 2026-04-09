package com.example.poseoverlay.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poseoverlay.ui.theme.PoseOverlayTheme


// ═══════════════════════════════════════════════════════════════════════════
//  Stateful wrapper — 目前沒有 ViewModel，但保留作為日後擴展入口
//  （例如：取 user name、上次使用紀錄等）
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun HomeScreen(
    onStartClick: () -> Unit
) {
    // 未來有狀態邏輯放這裡
    HomeContent(onStartClick = onStartClick)
}


// ═══════════════════════════════════════════════════════════════════════════
//  Pure UI — 完全不依賴外部狀態，可直接 Preview
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun HomeContent(
    onStartClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Aesthetic Circle Background
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "Pose Overlay",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "The easiest way to perfect your shot",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
        )

        Button(
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                "Enter",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                letterSpacing = 1.sp
            )
        }
    }
}


// ═══════════════════════════════════════════════════════════════════════════
//  Previews — 直接呼叫 HomeContent
// ═══════════════════════════════════════════════════════════════════════════
@Preview(showBackground = true, name = "Home Screen")
@Composable
fun HomeContentPreview() {
    PoseOverlayTheme {
        HomeContent(onStartClick = {})
    }
}
