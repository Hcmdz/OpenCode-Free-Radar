/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.opencode.freeradar.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

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
    onDraggingChange: (Boolean) -> Unit = {},
    /** Last persisted drag position, null when never dragged (anchor). */
    persistedOffset: IntOffset? = null,
    /** Called once per drop with the snapped position; never mid-drag. */
    onPersistOffset: (IntOffset) -> Unit = {}
) {
    var dragOffset by rememberSaveable(stateSaver = IntOffsetSaver) {
        mutableStateOf(IntOffset.Zero)
    }
    // One-shot restore: disk wins only on first load, afterwards memory
    // (fresher: it includes drops whose DataStore write is still in
    // flight). Without the lock, a late emission arriving after the user
    // already dragged elsewhere yanks the button back mid-gesture.
    var restored by rememberSaveable { mutableStateOf(false) }
    // Single animated truth: drags write it instantly (snapTo), peek and
    // restore glide through it (animateTo). Nothing ever chases the finger
    // with a lagging tween, so release can't jump backwards.
    val animScope = rememberCoroutineScope()
    val animOffset = remember { Animatable(IntOffset.Zero, IntOffset.VectorConverter) }
    LaunchedEffect(persistedOffset) {
        if (!restored && persistedOffset != null) {
            dragOffset = persistedOffset
            animOffset.snapTo(persistedOffset)
            restored = true
        }
    }
    var fabSize by remember { mutableStateOf(IntSize.Zero) }
    var dragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val padEndPx = with(density) { endPadding.roundToPx() }
    val padBottomPx = with(density) { bottomPadding.roundToPx() }
    val sliverPx = with(density) { sliverDp.dp.roundToPx() }
    val dockedOffset = coerceFabOffset(dragOffset, containerSize, fabSize, padEndPx, padBottomPx)
    // Peek and restore are the only glides. Restarting this effect cancels
    // the in-flight one, so a tap mid-peek never fights the peek slide.
    // Gated on a pending restore: at startup this effect would otherwise
    // capture the pre-restore docked position and glide the freshly
    // restored button back to the anchor, racing the restore snap.
    // Null persisted (never dragged) needs no gate. Keyed on all three
    // so every interleaving converges on the post-restore target.
    LaunchedEffect(peeked, restored, persistedOffset) {
        if (!restored && persistedOffset != null) return@LaunchedEffect
        val target = if (peeked) {
            peekTarget(dockedOffset, containerSize, fabSize, padEndPx, padBottomPx, sliverPx)
        } else {
            dockedOffset
        }
        animOffset.animateTo(target, tween(PEEK_ANIM_MILLIS))
    }
    // Raw while the finger is down, animated truth everywhere else. Drops
    // snap the animation instantly (see onDragEnd), so the display never
    // sits on a lagging value.
    val displayedOffset = if (dragging) dockedOffset else animOffset.value
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
                        // Grab continuity: taking over mid-glide (e.g. a
                        // peek slide) resumes from the drawn position,
                        // never teleports back to the docked one.
                        dragOffset = animOffset.value
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
                        // Instant: the display must sit on the snapped spot,
                        // not on a tween still chasing the finger.
                        animScope.launch { animOffset.snapTo(dragOffset) }
                        dragging = false
                        onDraggingChange(false)
                        onUserInteraction()
                        onPersistOffset(dragOffset)
                    },
                    onDragCancel = {
                        dragOffset = snappedOffset(
                            dragOffset, containerSize, fabSize, padEndPx, padBottomPx
                        )
                        animScope.launch { animOffset.snapTo(dragOffset) }
                        dragging = false
                        onDraggingChange(false)
                        onUserInteraction()
                        onPersistOffset(dragOffset)
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
