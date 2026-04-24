package com.example.poseoverlay.feature.overlay.ui

import android.graphics.Rect
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.TransformOrigin.Companion.Center
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.poseoverlay.feature.overlay.OverlayEvent
import com.example.poseoverlay.feature.overlay.OverlayState
import kotlinx.coroutines.delay
import java.util.*
import kotlin.math.*

// 擴充方法：用於向 state 回報組件位置
fun Modifier.reportBounds(state: OverlayState, key: String): Modifier = this.onGloballyPositioned { coords ->
    val rect = coords.boundsInWindow()
    val androidRect = Rect(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())

    // 只有當位置真的改變或不在 map 裡才更新，避免過度 Recompose
    if (state.interactiveBounds[key] != androidRect) {
        state.interactiveBounds = state.interactiveBounds + (key to androidRect)
    }
}

@Composable
fun OverlayComposeView(
    state: OverlayState,
    event: (OverlayEvent) -> Unit
) {

    Box(modifier = Modifier.fillMaxSize()) {

        TransformableImage(state = state)
        VerticalSidebar(
            state = state,
            event = event,
            modifier = Modifier.align(
                BiasAlignment(horizontalBias = 1f, verticalBias = -0.6f) // 關鍵：自定義對齊比例
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalIndirectPointerApi::class)
@Composable
fun VerticalSidebar(
    state: OverlayState,
    event: (OverlayEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isUserInteracting by remember { mutableStateOf(false) }

    val width = 40.dp

    val isHidden by produceState(initialValue = false, lastInteractionTime) {
        value = false
        if (!isUserInteracting) {
            delay(5000)
            value = true
        }
    }

    val offsetX by animateDpAsState(
        targetValue = if (isHidden) width / 2 else 0.dp,
        label = "SidebarOffset"
    )
    val opacity by animateFloatAsState(
        targetValue = if (isHidden) 0.6f else 1f,
        label = "SidebarOpacity"
    )

    fun resetTimer() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInteractionTime > 50) {
            lastInteractionTime = currentTime
        }
    }

    Box(
        modifier = modifier
            .wrapContentSize()
            .reportBounds(state, "sidebar_root")
            .pointerInput(Unit) {
                // 監聽任何觸碰事件
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        resetTimer()
                    }
                }
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier
                .offset(x = offsetX)
                .width(width)
                .height(300.dp)
                .graphicsLayer(alpha = opacity)
                .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                .pointerInput(isHidden) {
                    awaitPointerEventScope {
                        while (true) {
                            // 使用 Initial 階段搶先偵測
                            val event = awaitPointerEvent(PointerEventPass.Initial)

                            val isDown = event.changes.any { it.changedToDown() }

                            if (isDown) {
                                resetTimer()
                                if (isHidden) {
                                    event.changes.forEach { it.consume() }
                                }
                            }

                            if (isHidden) {
                                event.changes.forEach { if (it.pressed) it.consume() }
                            }
                        }
                    }
                },
            color = Color.Transparent,
        ) {
            // 使用漸層與微發光邊框營造現代感
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color(0xFF1A1A1A).copy(alpha = 0.9f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp)
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 透明度指示文字與滑桿
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .height(180.dp)
                            .fillMaxWidth()
                    ) {
                        Box(
                            modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Slider(
                                value = state.alpha,
                                onValueChange = {
                                    if (!isHidden) {
                                        state.alpha = it
                                        resetTimer()
                                    }
                                },
                                modifier = Modifier
                                    .graphicsLayer {
                                        rotationZ = -90f
                                        transformOrigin = Center
                                    }
                                    .requiredWidth(160.dp),
                                // 關鍵：自定義軌道來控制形狀與間距
                                track = { sliderState ->
                                    val inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    val activeTrackColor = Color(0xFF007AFF)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(12.dp) // 軌道總高度 (粗細)
                                            // 1. 給整個軌道一個外圓角
                                            .background(inactiveTrackColor, CircleShape)
                                            .clip(CircleShape) // 強制剪裁內容 (Active 部分)
                                    ) {
                                        // 2. 畫 Active Track (藍色部分)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                // 根據進度計算寬度，填滿父容器 (Box With Constraints)
                                                .fillMaxWidth(fraction = sliderState.value.coerceIn(0f, 1f))
                                                // 關鍵：將 Active 的開頭端點設為直角，以免跟末端重複
                                                // 末端則因為父容器的 clip(CircleShape) 而自然變圓
                                                .background(
                                                    activeTrackColor,
                                                    // 如果進度是 1 (最大)，末端變直角以便完美填滿
                                                    if (sliderState.value >= 1f) RectangleShape else CircleShape
                                                )
                                        )
                                    }
                                },
                                thumb = {}, // 既然要隱藏，直接傳入空 Composable 比透明色更乾淨

                                interactionSource = remember { MutableInteractionSource() }
                            )
                        }
                    }

                    // 下方控制按鈕
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {

                        IconButton(
                            onClick = {
                                if (!isHidden) {
                                    state.isVisible = !state.isVisible;
                                    resetTimer()
                                }
                            },
                            modifier = Modifier.weight(1f)) {
                            Icon(
                                if (state.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                "sb_visibility",
                                tint = Color.White)
                        }
                        IconButton(
                            onClick = {
                                if (!isHidden) {
                                    event(OverlayEvent.toggleLock(!state.isLocked))
                                    resetTimer()
                                }
                            },
                            modifier = Modifier.weight(1f)) {
                            Icon(if (state.isLocked) Icons.Outlined.LockClock else Icons.Outlined.LockOpen, "Lock", tint = Color.White)
                        }
                        IconButton(
                            onClick = {
                                if (!isHidden) {
                                    event(OverlayEvent.onNavigateToGallery)
                                    resetTimer()
                                }
                            },
                            modifier = Modifier.weight(1f)) { Icon(Icons.Default.PhotoLibrary, "photo_library", tint = Color.White) }
//                        IconButton(
//                            onClick = {
//                                onMaximize()
//                                resetTimer()
//                            },
//                            modifier = Modifier.weight(1f)) { Icon(Icons.Default.OpenInFull, "sb_maximize", tint = Color.White) }
                    }
                }
            }
        }
    }
}


@Composable
fun TransformableImage(state: OverlayState) {
    var scaleX by remember { mutableFloatStateOf(1f) }
    var scaleY by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isSelected by remember { mutableStateOf(false) }

    // 圖片原始大小 (用於計算縮放)
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    // 重置功能
    LaunchedEffect(state.imageUri) {
        scaleX = 1f
        scaleY = 1f
        rotation = 0f
        offset = Offset.Zero
        isSelected = false
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { isSelected = false }
            },
        contentAlignment = Alignment.Center
    ) {
        if (state.imageUri != null) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                    .graphicsLayer(
                        scaleX = scaleX,
                        scaleY = scaleY,
                        rotationZ = rotation,
                        alpha = if (state.isVisible) state.alpha else 0f
                    )
                    .onGloballyPositioned { coords ->
                        imageSize = coords.size
                        // 更新互動區域，讓系統知道這裡不可點透 (如果需要的話)
                        val rect = coords.boundsInWindow()
                        state.interactiveBounds = state.interactiveBounds + ("image" to Rect(
                            rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt()
                        ))
                    }
                    .pointerInput(state.isLocked) {
                        if (state.isLocked) return@pointerInput
                        detectTransformGestures { _, pan, zoom, rotationChange ->
                            offset += pan
                            scaleX *= zoom
                            scaleY *= zoom
                            rotation += rotationChange
                        }
                    }
                    .pointerInput(state.isLocked) {
                        if (state.isLocked) return@pointerInput
                        detectTapGestures(
                            onPress = {isSelected = true},
                            onDoubleTap = {
                                scaleX = 1f
                                scaleY = 1f
                                rotation = 0f
                                offset = Offset.Zero
                            }
                        )
                    }
                    .then(
                        if (isSelected && !state.isLocked) Modifier.border(2.dp / max(scaleX, scaleY), Color.White)
                        else Modifier
                    )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(state.imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.wrapContentSize(),
                    contentScale = ContentScale.Fit
                )

                if (isSelected && !state.isLocked) {
                    val handleSize = 16.dp
                    // 補償縮放後的 handle 大小，使其在視覺上保持固定
                    val adaptiveHandleSize = handleSize / max(scaleX, scaleY)

                    // 定義 8 個控制點的位置
                    val handles = listOf(
                        Alignment.TopStart, Alignment.TopCenter, Alignment.TopEnd,
                        Alignment.CenterStart, Alignment.CenterEnd,
                        Alignment.BottomStart, Alignment.BottomCenter, Alignment.BottomEnd
                    )

                    handles.forEach { alignment ->
                        Box(
                            modifier = Modifier
                                .align(alignment)
                                .size(adaptiveHandleSize)
                                .offset {
                                    val biasAlignment = alignment as? BiasAlignment
                                    val xSign = when (biasAlignment?.horizontalBias) {
                                        -1f -> -0.5f
                                        1f -> 0.5f
                                        else -> 0f
                                    }
                                    val ySign = when (biasAlignment?.verticalBias) {
                                        -1f -> -0.5f
                                        1f -> 0.5f
                                        else -> 0f
                                    }
                                    IntOffset(
                                        (xSign * adaptiveHandleSize.toPx()).roundToInt(),
                                        (ySign * adaptiveHandleSize.toPx()).roundToInt()
                                    )
                                }
                                .background(Color.White, CircleShape)
                                .border(1.dp / max(scaleX, scaleY), Color.Gray, CircleShape)
                                .pointerInput(alignment, state.isLocked) {
                                    if (state.isLocked) return@pointerInput
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val biasAlignment = alignment as? BiasAlignment
                                        val horizontalFactor = biasAlignment?.horizontalBias ?: 0f
                                        val verticalFactor = biasAlignment?.verticalBias ?: 0f
                                        if (horizontalFactor == 0f && verticalFactor == 0f) return@detectDragGestures
                                        val localDragX = dragAmount.x * scaleX
                                        val localDragY = dragAmount.y * scaleY
                                        val deltaScaleX = if (horizontalFactor != 0f) (localDragX * horizontalFactor) / imageSize.width else 0f
                                        val deltaScaleY = if (verticalFactor != 0f) (localDragY * verticalFactor) / imageSize.height else 0f
                                        val localOffsetX = (imageSize.width * deltaScaleX / 2f) * horizontalFactor
                                        val localOffsetY = (imageSize.height * deltaScaleY / 2f) * verticalFactor
                                        val rotationRad = rotation * (PI.toFloat() / 180f)
                                        val cosR = cos(rotationRad)
                                        val sinR = sin(rotationRad)
                                        val screenOffsetX = localOffsetX * cosR - localOffsetY * sinR
                                        val screenOffsetY = localOffsetX * sinR + localOffsetY * cosR
                                        scaleX = (scaleX + deltaScaleX).coerceAtLeast(0.01f)
                                        scaleY = (scaleY + deltaScaleY).coerceAtLeast(0.01f)
                                        offset += Offset(screenOffsetX, screenOffsetY)
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF555555)
@Composable
fun VerticalSidebarPreview() {
    MaterialTheme {
        OverlayComposeView(
            state = OverlayState().apply {
                alpha = 0.7f
                isVisible = true
                isMinimized = true
                imageUri = "".toUri()
            },
            event = {}
        )
    }
}

@Composable
fun NoteBox(
    text: String,
    onTextChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.85f),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .padding(32.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notes", color = Color.White, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close Note", tint = Color.White)
                }
            }
            TextField(
                value = text,
                onValueChange = onTextChange,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                placeholder = { Text("Write your thoughts...", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun NoteBoxPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Gray), contentAlignment = Alignment.Center) {
            NoteBox(
                text = "這是一段測試筆記內容...",
                onTextChange = {},
                onClose = {}
            )
        }
    }
}
