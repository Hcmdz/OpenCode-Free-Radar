/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.components

import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.opencode.freeradar.data.local.FilterFabPrefs
import kotlin.math.roundToInt

const val PEEK_ANIM_MILLIS = 220

/**
 * Survives rotation and process death. Defensive restore: a truncated list
 * falls back to zero instead of crashing startup.
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
 * rotation or keyboard resize. Unmeasured FAB keeps its offset; the clamp
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
 * True when the docked FAB's center sits past the container midpoint, i.e.
 * the right edge is the nearer one. Mirrors the edge rule the release snap
 * applies, so peek always continues toward the edge the FAB was dropped at.
 */
fun isSnappedToRight(dockedX: Int, containerW: Int, fabW: Int, padEndPx: Int): Boolean {
    if (containerW <= 0) return true
    val centerX = containerW - fabW - padEndPx + dockedX + fabW / 2
    return centerX > containerW / 2
}

/**
 * Drag-release snap: x to the nearest edge margin, y clamped on-screen.
 * Unmeasured inputs pass the offset through; degenerate tiny containers
 * pin to the right edge rather than parking off-screen.
 */
fun snappedOffset(
    offset: IntOffset,
    container: IntSize,
    fab: IntSize,
    padEndPx: Int,
    padBottomPx: Int
): IntOffset {
    if (container.width <= 0 || container.height <= 0) return offset
    if (fab.width <= 0 || fab.height <= 0) return offset
    val anchorLeft = container.width - fab.width - padEndPx
    val rightX = 0
    val leftX = (padEndPx - anchorLeft).coerceAtLeast(-(container.width - fab.width - padEndPx))
    val y = coerceFabOffset(offset, container, fab, padEndPx, padBottomPx).y
    return IntOffset(
        x = if (isSnappedToRight(offset.x, container.width, fab.width, padEndPx)) rightX else leftX,
        y = y
    )
}

/**
 * Edge-peek target for an idle FAB: slides toward the nearer edge leaving
 * exactly [sliverPx] of width on-screen, keeping the docked y. The sliver is
 * measured against drawn width (margins excluded by the caller), so the tap
 * target is real ink, not transparent padding. Unmeasured inputs or a
 * negative sliver keep the docked position (fail-open, never off-screen).
 */
fun peekTarget(
    docked: IntOffset,
    container: IntSize,
    fab: IntSize,
    padEndPx: Int,
    padBottomPx: Int,
    sliverPx: Int
): IntOffset {
    if (container.width <= 0 || container.height <= 0) return docked
    if (fab.width <= 0 || fab.height <= 0) return docked
    if (sliverPx < 0) return docked
    val y = coerceFabOffset(docked, container, fab, padEndPx, padBottomPx).y
    return if (isSnappedToRight(docked.x, container.width, fab.width, padEndPx)) {
        IntOffset(fab.width + padEndPx - sliverPx, y)
    } else {
        IntOffset(padEndPx + sliverPx - container.width, y)
    }
}

/**
 * Filter entry point as a freely draggable FAB that edge-peeks when idle.
 * Tap opens the sheet, drag repositions (position kept across rotation via
 * [IntOffsetSaver]; session only, no DataStore). Drag follows the finger
 * 1:1 with no animation lag; release snaps to the nearest edge and idle
 * peeks to it, both animated. Drag consumes the gesture so the list
 * underneath never scrolls mid-drag; end/cancel keep the dropped position.
 * Peek never removes the FAB from the tree: the sliver stays grabbable.
 */
@Composable
fun FilterFab(
    summary: String,
    active: Boolean,
    containerSize: IntSize,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    endPadding: Dp = 16.dp,
    bottomPadding: Dp = 16.dp,
    peeked: Boolean = false,
    sliverDp: Int = FilterFabPrefs.DEFAULT_SLIVER_DP,
    onUserInteraction: () -> Unit = {},
    onDraggingChange: (Boolean) -> Unit = {}
) {
    var dragOffset by rememberSaveable(stateSaver = IntOffsetSaver) {
        mutableStateOf(IntOffset.Zero)
    }
    var fabSize by remember { mutableStateOf(IntSize.Zero) }
    var dragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val padEndPx = with(density) { endPadding.roundToPx() }
    val padBottomPx = with(density) { bottomPadding.roundToPx() }
    val sliverPx = with(density) { sliverDp.dp.roundToPx() }
    val dockedOffset = coerceFabOffset(dragOffset, containerSize, fabSize, padEndPx, padBottomPx)
    // Null while the finger is down: raw snap, no chase lag. Everywhere else
    // the display animates toward the target, so release-snap, peek and
    // restore all glide instead of jumping.
    val programmaticTarget = when {
        dragging -> null
        peeked -> peekTarget(dockedOffset, containerSize, fabSize, padEndPx, padBottomPx, sliverPx)
        else -> dockedOffset
    }
    val animatedOffset by animateIntOffsetAsState(
        targetValue = programmaticTarget ?: dockedOffset,
        animationSpec = tween(PEEK_ANIM_MILLIS)
    )
    val displayedOffset = if (programmaticTarget == null) dockedOffset else animatedOffset
    // Icon-only small FAB: the summary text is gone from the screen, so the
    // localized summary becomes the TalkBack label (one node: label + tap).
    SmallFloatingActionButton(
        onClick = {
            onUserInteraction()
            onOpen()
        },
        // Offset first so hit-testing follows the drawn position; fresh keys
        // or the drag closure goes stale on resize.
        modifier = modifier
            .semantics { contentDescription = summary }
            .padding(end = endPadding, bottom = bottomPadding)
            .offset { displayedOffset }
            .onSizeChanged { fabSize = it }
            .pointerInput(containerSize, fabSize, padEndPx, padBottomPx) {
                detectDragGestures(
                    onDragStart = {
                        // Grab continuity: dropping the finger mid-glide must
                        // not teleport the FAB back to the docked position.
                        dragOffset = animatedOffset
                        dragging = true
                        onDraggingChange(true)
                        onUserInteraction()
                    },
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
                    // Release snaps to the edge; cancel behaves the same so an
                    // interrupted gesture never strands a mid-air position.
                    // Reads dragOffset at event time: the composed docked
                    // value can lag the final move by a frame.
                    onDragEnd = {
                        dragOffset = snappedOffset(
                            dragOffset, containerSize, fabSize, padEndPx, padBottomPx
                        )
                        dragging = false
                        onDraggingChange(false)
                        onUserInteraction()
                    },
                    onDragCancel = {
                        dragOffset = snappedOffset(
                            dragOffset, containerSize, fabSize, padEndPx, padBottomPx
                        )
                        dragging = false
                        onDraggingChange(false)
                        onUserInteraction()
                    }
                )
            }
    ) {
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
    }
}
