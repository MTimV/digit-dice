package com.vickers.digitdice

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

private val Ink = Color(0xFFEDEAF6)
private val Sheet = Color(0xFF1B1630)
private val Muted = Color(0xFF6E6784)
private val Backdrop = Color(0xFF0A0810)

private val DotSize = 88.dp
private val CreatureSize = 108.dp
private val GlowRadius = 130.dp
private val RingFar = 128.dp
private val RingNear = 66.dp
private val WinRadius = 64.dp
private val WaveSpan = 280.dp
private val WinGlow = 230.dp
private val HopHeight = 22.dp

/** How long the winner stays on screen after their finger lifts. */
private const val WinLingerMillis = 3000
/** The tail end of the linger, spent fading out. */
private const val WinFadeMillis = 500f

private fun digitSize(theme: Theme): Dp = if (theme == Theme.Dots) DotSize else CreatureSize

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFF4D6D),
    onPrimary = Color(0xFF2B0410),
    secondaryContainer = Color(0xFF3A2140),
    onSecondaryContainer = Ink,
    surface = Sheet,
    onSurface = Ink,
    surfaceVariant = Color(0xFF2A2440),
    onSurfaceVariant = Ink,
    background = Backdrop,
    onBackground = Ink,
    outline = Muted
)

@Composable
fun DigitDiceApp() {
    MaterialTheme(colorScheme = DarkScheme) {
        DigitDiceScreen()
    }
}

@Composable
private fun DigitDiceScreen() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val haptics = remember { Haptics(context) }
    val sfx = remember { Sfx(context) }
    val game = remember { Game(DIGIT_SLOTS) }

    DisposableEffect(Unit) { onDispose { sfx.release() } }

    var mode by remember { mutableStateOf(prefs.mode) }
    var theme by remember { mutableStateOf(prefs.theme) }
    var holdMillis by remember { mutableIntStateOf(prefs.holdMillis) }
    var buzzOn by remember { mutableStateOf(prefs.haptics) }
    var soundOn by remember { mutableStateOf(prefs.sound) }

    var frame by remember { mutableLongStateOf(0L) }
    var fingerCount by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(Phase.Idle) }
    var sheetOpen by remember { mutableStateOf(false) }

    // One tick per displayed frame. Compose pauses this when we are not visible.
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { }
            val now = SystemClock.uptimeMillis()
            frame = now
            val picked = game.tick(now, holdMillis)
            phase = game.phase // also catches a lingering winner timing out
            if (picked) {
                if (buzzOn) haptics.reveal()
                if (soundOn) {
                    if (mode == Mode.Winner) sfx.win() else sfx.lose()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    try {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val now = SystemClock.uptimeMillis()
                                for (change in event.changes) {
                                    val id = change.id.value
                                    when {
                                        !change.pressed -> game.up(
                                            id,
                                            now,
                                            if (mode == Mode.Winner) WinLingerMillis else 0
                                        )
                                        game.has(id) -> game.move(id, change.position)
                                        !change.isConsumed ->
                                            if (game.down(id, change.position, now)) {
                                                if (buzzOn) haptics.tap()
                                                if (soundOn) sfx.tap()
                                            }
                                    }
                                }
                                fingerCount = game.count
                                phase = game.phase
                            }
                        }
                    } finally {
                        // Backgrounding cancels this block, so drop any finger
                        // we never saw lift rather than leaving it stuck down.
                        game.reset()
                        fingerCount = 0
                        phase = Phase.Idle
                    }
                }
        ) {
            val now = frame // reading this state is what redraws us each frame
            drawBackdrop()
            if (game.phase == Phase.Chosen) {
                val fade = (game.lingerLeftMillis(now) / WinFadeMillis).coerceIn(0f, 1f)
                drawReveal(game, theme, mode, game.secondsSincePick(now), fade)
            } else {
                drawWaiting(game, theme, game.progress(now, holdMillis))
            }
        }

        val hint = when {
            phase == Phase.Chosen && fingerCount == 0 -> "Everyone put a finger down"
            phase == Phase.Chosen && mode == Mode.Loser -> "The grey one loses"
            phase == Phase.Chosen -> "Lift up to play again"
            fingerCount == 0 -> "Everyone put a finger down"
            fingerCount == 1 -> "One more finger"
            else -> ""
        }

        // Hold the last words on screen so they can fade out instead of blinking off.
        var shownHint by remember { mutableStateOf("") }
        LaunchedEffect(hint) { if (hint.isNotEmpty()) shownHint = hint }
        val hintAlpha by animateFloatAsState(
            targetValue = if (hint.isEmpty()) 0f else 1f,
            animationSpec = tween(durationMillis = 240),
            label = "hintAlpha"
        )

        Text(
            text = shownHint,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .safeDrawingPadding()
                .padding(top = 28.dp, start = 40.dp, end = 40.dp)
                .alpha(hintAlpha),
            color = Ink.copy(alpha = 0.46f),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.3.sp,
            textAlign = TextAlign.Center
        )

        // The gear gets out of the way the moment anyone starts playing.
        AnimatedVisibility(
            visible = fingerCount == 0 && phase != Phase.Chosen,
            enter = fadeIn(tween(240)),
            exit = fadeOut(tween(160)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
        ) {
            IconButton(onClick = { sheetOpen = true }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Muted
                )
            }
        }

        if (sheetOpen) {
            SettingsSheet(
                mode = mode,
                theme = theme,
                holdMillis = holdMillis,
                buzzOn = buzzOn,
                soundOn = soundOn,
                onMode = { mode = it; prefs.mode = it },
                onTheme = { theme = it; prefs.theme = it },
                onHoldChange = { holdMillis = it },
                onHoldSettled = { prefs.holdMillis = holdMillis },
                onBuzz = { buzzOn = it; prefs.haptics = it },
                onSound = { soundOn = it; prefs.sound = it },
                onDismiss = { sheetOpen = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    mode: Mode,
    theme: Theme,
    holdMillis: Int,
    buzzOn: Boolean,
    soundOn: Boolean,
    onMode: (Mode) -> Unit,
    onTheme: (Theme) -> Unit,
    onHoldChange: (Int) -> Unit,
    onHoldSettled: () -> Unit,
    onBuzz: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Sheet,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Muted) }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Label("Digits")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Theme.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = theme == entry,
                        onClick = { onTheme(entry) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = Theme.entries.size
                        )
                    ) {
                        Text(entry.label)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Caption(
                when (theme) {
                    Theme.Dots -> "Plain coloured dots."
                    Theme.Dinos -> "Blue dinosaurs. Everyone gets a different one."
                    Theme.Unicorns -> "Pink unicorns. Everyone gets a different one."
                }
            )

            Spacer(Modifier.height(28.dp))
            Label("Mode")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Mode.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = mode == entry,
                        onClick = { onMode(entry) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = Mode.entries.size
                        )
                    ) {
                        Text(entry.label)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Caption(
                if (mode == Mode.Winner) {
                    "One digit is picked to win. The rest fade away."
                } else {
                    "One digit is picked to lose. It turns grey and stays put."
                }
            )

            Spacer(Modifier.height(28.dp))
            Label("Hold time")
            Caption("${holdMillis / 1000}.${(holdMillis % 1000) / 100} seconds")
            Slider(
                value = holdMillis.toFloat(),
                onValueChange = { onHoldChange(it.roundToInt()) },
                onValueChangeFinished = onHoldSettled,
                valueRange = 1500f..4000f,
                steps = 4
            )

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sound", modifier = Modifier.weight(1f), color = Ink, fontSize = 16.sp)
                Switch(checked = soundOn, onCheckedChange = onSound)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vibrate", modifier = Modifier.weight(1f), color = Ink, fontSize = 16.sp)
                Switch(checked = buzzOn, onCheckedChange = onBuzz)
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(bottom = 10.dp),
        color = Ink,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp
    )
}

@Composable
private fun Caption(text: String) {
    Text(text = text, color = Muted, fontSize = 14.sp)
}

// ---- drawing ------------------------------------------------------------

private fun DrawScope.drawBackdrop() {
    drawRect(
        brush = Brush.radialGradient(
            0f to Color(0xFF241E42),
            0.55f to Color(0xFF14111F),
            1f to Backdrop,
            center = Offset(size.width / 2f, size.height * 0.06f),
            radius = size.height * 1.05f
        )
    )
}

private fun DrawScope.drawGlow(center: Offset, radius: Float, color: Color, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            0f to color.copy(alpha = alpha),
            1f to color.copy(alpha = 0f),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

/**
 * The signature move. The ring hangs wide, then rushes in and tightens on the
 * digit. No numbers, because the four year old cannot read a countdown.
 */
private fun DrawScope.drawWaiting(game: Game, theme: Theme, progress: Float) {
    val eased = progress.pow(2.2f)
    val far = RingFar.toPx()
    val ringRadius = far - (far - RingNear.toPx()) * eased
    val size = digitSize(theme).toPx()

    for (finger in game.fingers) {
        val color = digitColor(theme, finger.colorIndex)
        drawGlow(finger.pos, GlowRadius.toPx(), color, 0.30f)
        drawDigit(theme, finger.colorIndex, finger.pos, size)
        if (progress > 0f) {
            drawCircle(
                color = color.copy(alpha = 0.30f + eased * 0.70f),
                radius = ringRadius,
                center = finger.pos,
                style = Stroke(width = (3f + eased * 5f).dp.toPx())
            )
        }
    }
}

/** @param fade only applies to a winner whose finger has already lifted. */
private fun DrawScope.drawReveal(game: Game, theme: Theme, mode: Mode, seconds: Float, fade: Float) {
    for (finger in game.fingers) {
        val picked = finger.id == game.pickedId
        when {
            picked && mode == Mode.Winner -> drawChosen(theme, finger, seconds)
            picked -> drawEliminated(theme, finger, seconds)
            mode == Mode.Winner -> drawFading(theme, finger, seconds)
            else -> drawSurvivor(theme, finger, seconds)
        }
    }
    // The winner lifted, but stays put for a moment so everyone can see it.
    val lingering = game.picked
    if (mode == Mode.Winner && game.pickedLifted && lingering != null && fade > 0f) {
        drawChosen(theme, lingering, seconds, fade)
    }
}

private fun DrawScope.drawChosen(theme: Theme, finger: Finger, seconds: Float, fade: Float = 1f) {
    val color = digitColor(theme, finger.colorIndex)

    for (i in 0 until 3) {
        val offsetSeconds = seconds - i * 0.30f
        if (offsetSeconds < 0f) continue
        val k = (offsetSeconds % 1.7f) / 1.7f
        drawCircle(
            color = color.copy(alpha = (1f - k) * 0.45f * fade),
            radius = WinRadius.toPx() + k * WaveSpan.toPx(),
            center = finger.pos,
            style = Stroke(width = (6f * (1f - k) + 1f).dp.toPx())
        )
    }

    if (theme == Theme.Dots) {
        val pop = 1f + (1f - seconds * 4.5f).coerceAtLeast(0f) * 0.55f
        val breathe = 1f + sin(seconds * 3.6f) * 0.035f
        val radius = WinRadius.toPx() * pop * breathe
        drawGlow(finger.pos, WinGlow.toPx(), color, 0.45f * fade)
        drawCircle(color = color, radius = radius, center = finger.pos, alpha = fade)
        drawCircle(
            color = Color.White.copy(alpha = 0.95f * fade),
            radius = radius * 0.40f,
            center = finger.pos
        )
    } else {
        // The hop does two jobs. It reads as celebrating, and it lifts the digit
        // out from under the finger, which is the only way the winner gets to
        // see their own.
        val swing = sin(seconds * 5f)
        val at = Offset(finger.pos.x, finger.pos.y - abs(swing) * HopHeight.toPx())
        val grow = 1.45f + (1f - seconds * 4f).coerceAtLeast(0f) * 0.25f
        drawGlow(at, WinGlow.toPx(), color, 0.45f * fade)
        drawDigit(
            theme = theme,
            index = finger.colorIndex,
            center = at,
            sizePx = CreatureSize.toPx() * grow,
            alpha = fade,
            rotation = swing * 7f
        )
    }
}

/** Loser mode. The colour drains out and the ring collapses onto the digit. */
private fun DrawScope.drawEliminated(theme: Theme, finger: Finger, seconds: Float) {
    val drained = (seconds * 2.5f).coerceIn(0f, 1f)
    val collapse = (1f - seconds * 2.2f).coerceAtLeast(0f)
    val size = digitSize(theme).toPx()

    if (collapse > 0f) {
        drawCircle(
            color = Ash.copy(alpha = collapse * 0.8f),
            radius = size / 2f + RingNear.toPx() * collapse,
            center = finger.pos,
            style = Stroke(width = 4.dp.toPx())
        )
    }
    drawDigit(
        theme = theme,
        index = finger.colorIndex,
        center = finger.pos,
        sizePx = size * (1f - drained * 0.14f),
        grey = drained
    )
}

/** Loser mode. Everyone else gets a small relieved pop and stays lit. */
private fun DrawScope.drawSurvivor(theme: Theme, finger: Finger, seconds: Float) {
    val color = digitColor(theme, finger.colorIndex)
    val pop = 1f + (1f - seconds * 5f).coerceAtLeast(0f) * 0.35f
    drawGlow(finger.pos, GlowRadius.toPx(), color, 0.30f)
    drawDigit(theme, finger.colorIndex, finger.pos, digitSize(theme).toPx() * pop)
}

/** Winner mode. The ones who did not get picked shrink away. */
private fun DrawScope.drawFading(theme: Theme, finger: Finger, seconds: Float) {
    val k = (1f - seconds * 2.4f).coerceAtLeast(0f)
    if (k <= 0f) return
    drawDigit(
        theme = theme,
        index = finger.colorIndex,
        center = finger.pos,
        sizePx = digitSize(theme).toPx() * k,
        alpha = k * 0.6f
    )
}
