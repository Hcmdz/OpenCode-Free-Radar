/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Survives rotation and process death. Defensive restore: a truncated list
 * falls back to zero instead of crashing startup (T1).
 */
val IntOffsetSaver = listSaver<IntOffset, Int>(
    save = { listOf(it.x, it.y) },
    restore = { IntOffset(it.getOrElse(0) { 0 }, it.getOrElse(1) { 0 }) }
)

/**
 * Clamps a bottom-end-anchored drag offset so the FAB stays fully inside its
 * container: only up-left travel is allowed (positive would leave the screen
 * past the anchor padding). Re-applied on every recomposition, not just while
 * dragging, so a restored offset never strands the FAB off-screen after a
 * rotation or keyboard resize (T2). Unmeasured FAB keeps its offset; the clamp
 * lands once sizes are known.
 */
fun coerceFabOffset(
    offset: IntOffset,
    container: IntSize,
    fab: IntSize,
    padEndPx: Int,
    padBottomPx: Int
): IntOffset {
    if (container.width <= 0 || container.height <= 0) return IntOffset.Zero
    if (fab.width <= 0 || fab.height <= 0) return offset
    val minX = -(container.width - fab.width - padEndPx).coerceAtLeast(0)
    val minY = -(container.height - fab.height - padBottomPx).coerceAtLeast(0)
    return IntOffset(
        x = offset.x.coerceIn(minX, 0),
        y = offset.y.coerceIn(minY, 0)
    )
}

/**
 * Filter entry point as a freely draggable FAB. Tap opens the sheet, drag
 * repositions (position kept across rotation via [IntOffsetSaver]; session
 * only, no DataStore). Drag consumes the gesture so the list underneath never
 * scrolls mid-drag; end/cancel intentionally keep the offset (no snap-back).
 */
@Composable
fun FilterFab(
    summary: String,
    active: Boolean,
    containerSize: IntSize,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    endPadding: Dp = 16.dp,
    bottomPadding: Dp = 16.dp
) {
    var dragOffset by rememberSaveable(stateSaver = IntOffsetSaver) {
        mutableStateOf(IntOffset.Zero)
    }
    var fabSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val padEndPx = with(density) { endPadding.roundToPx() }
    val padBottomPx = with(density) { bottomPadding.roundToPx() }
    val offset = coerceFabOffset(dragOffset, containerSize, fabSize, padEndPx, padBottomPx)
    ExtendedFloatingActionButton(
        text = {
            Text(
                text = summary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 200.dp)
            )
        },
        icon = {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(imageVector = Icons.Filled.Tune, contentDescription = null)
                if (active) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = CircleShape
                            )
                    )
                }
            }
        },
        onClick = onOpen,
        // Offset first so hit-testing follows the drawn position; fresh keys
        // or the drag closure goes stale on resize (T4).
        modifier = modifier
            .padding(end = endPadding, bottom = bottomPadding)
            .offset { offset }
            .onSizeChanged { fabSize = it }
            .pointerInput(containerSize, fabSize, padEndPx, padBottomPx) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val next = IntOffset(
                            x = (dragOffset.x + dragAmount.x.roundToInt()),
                            y = (dragOffset.y + dragAmount.y.roundToInt())
                        )
                        dragOffset = coerceFabOffset(
                            next, containerSize, fabSize, padEndPx, padBottomPx
                        )
                    },
                    onDragEnd = {},
                    onDragCancel = {}
                )
            }
    )
}
