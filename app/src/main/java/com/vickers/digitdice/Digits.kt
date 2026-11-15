package com.vickers.digitdice

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser

/**
 * A digit is the thing under a finger. Plain dot, dinosaur, or unicorn.
 *
 * The creatures are drawn in a 100x100 box facing left and scaled to fit.
 * Each one is a shared silhouette recoloured per player, with three variants
 * of the one part that carries the character (mane and tail on a unicorn,
 * the back on a dinosaur). Ten colour sets against three variants is enough
 * that nobody has to argue about whose is whose.
 */

internal val Ash = Color(0xFF4A4459)

internal const val DIGIT_SLOTS = 10

private class Skin(val body: Color, val accent: Color, val trim: Color, val eye: Color)

private enum class Part { Body, Trim, Eye }

private fun Skin.colorOf(part: Part): Color = when (part) {
    Part.Body -> body
    Part.Trim -> trim
    Part.Eye -> eye
}

// ---- palettes -----------------------------------------------------------

internal val DOT_COLORS = listOf(
    Color(0xFFFF4D6D), Color(0xFFFF7B00), Color(0xFFFFB703), Color(0xFFB5E48C),
    Color(0xFF57CC99), Color(0xFF2EC4B6), Color(0xFF4CC9F0), Color(0xFF4D8CFF),
    Color(0xFF9B5DE5), Color(0xFFF72585)
)

private val UNICORN_EYE = Color(0xFF2B1B36)

private val UNICORN_SKINS = listOf(
    Skin(Color(0xFFFF8FCF), Color(0xFFC77DFF), Color(0xFFFFD166), UNICORN_EYE),
    Skin(Color(0xFFFFB3C6), Color(0xFFFF6B9D), Color(0xFFFFF1B6), UNICORN_EYE),
    Skin(Color(0xFFC77DFF), Color(0xFFFF8FCF), Color(0xFFFFD166), UNICORN_EYE),
    Skin(Color(0xFFFF6B9D), Color(0xFFFFC2E0), Color(0xFFE8E8FF), UNICORN_EYE),
    Skin(Color(0xFFE0AAFF), Color(0xFFB388EB), Color(0xFFFFD166), UNICORN_EYE),
    Skin(Color(0xFFFFC2E0), Color(0xFFC77DFF), Color(0xFFFFB4A2), UNICORN_EYE),
    Skin(Color(0xFFF72585), Color(0xFFFFB3C6), Color(0xFFFFD166), UNICORN_EYE),
    Skin(Color(0xFFB388EB), Color(0xFFFF8FCF), Color(0xFFE8E8FF), UNICORN_EYE),
    Skin(Color(0xFFFF9E9E), Color(0xFFC77DFF), Color(0xFFFFF1B6), UNICORN_EYE),
    Skin(Color(0xFFFFD6E8), Color(0xFFF72585), Color(0xFFFFD166), UNICORN_EYE)
)

private val DINO_EYE = Color(0xFF0C1A2B)

private val DINO_SKINS = listOf(
    Skin(Color(0xFF4CC9F0), Color(0xFF5E60CE), Color(0xFF9BE0FF), DINO_EYE),
    Skin(Color(0xFF4D8CFF), Color(0xFF7BDFF2), Color(0xFFA9CCFF), DINO_EYE),
    Skin(Color(0xFF2EC4B6), Color(0xFF4D8CFF), Color(0xFFA8F0E8), DINO_EYE),
    Skin(Color(0xFF57CC99), Color(0xFF2EC4B6), Color(0xFFC8F5DC), DINO_EYE),
    Skin(Color(0xFF7B8CFF), Color(0xFF4CC9F0), Color(0xFFD0D6FF), DINO_EYE),
    Skin(Color(0xFF00D4E8), Color(0xFF5E60CE), Color(0xFFA8F0F5), DINO_EYE),
    Skin(Color(0xFF9BE0FF), Color(0xFF4D8CFF), Color(0xFFE0F4FF), DINO_EYE),
    Skin(Color(0xFF35A7FF), Color(0xFF7BDFF2), Color(0xFFB8E4FF), DINO_EYE),
    Skin(Color(0xFF8AC926), Color(0xFF2EC4B6), Color(0xFFDCF5B8), DINO_EYE),
    Skin(Color(0xFF5E60CE), Color(0xFF4CC9F0), Color(0xFFC0C4FF), DINO_EYE)
)

// ---- unicorn ------------------------------------------------------------

/** Mane and tail, one pair per variant. These are what make yours yours. */
private val UNICORN_ACCENTS: List<Pair<String, String>> = listOf(
    "M40,14 C54,20 54,38 49,54 C46,62 38,60 39,52 C46,40 46,24 36,20 Z" to
        "M76,56 C90,48 96,60 92,74 C89,84 78,84 80,73 C85,64 86,58 77,63 Z",
    "M37,15 a7,7 0 1,0 14,0 a7,7 0 1,0 -14,0 M43,29 a7,7 0 1,0 14,0 a7,7 0 1,0 -14,0 M45,43 a6,6 0 1,0 12,0 a6,6 0 1,0 -12,0" to
        "M77,57 a8,8 0 1,0 16,0 a8,8 0 1,0 -16,0 M81,71 a7,7 0 1,0 14,0 a7,7 0 1,0 -14,0",
    "M37,13 L51,19 L44,26 L55,33 L46,39 L56,49 L45,53 L47,61 L38,53 C45,39 45,25 35,18 Z" to
        "M76,57 L93,50 L86,62 L97,65 L86,73 L93,82 L77,73 Z"
)

private val UNICORN_REST: List<Pair<Part, String>> = listOf(
    Part.Body to "M40,72 L40,88 a4,4 0 0,0 8,0 L48,72 Z",
    Part.Body to "M50,76 L50,88 a4,4 0 0,0 8,0 L58,76 Z",
    Part.Body to "M62,76 L62,88 a4,4 0 0,0 8,0 L70,76 Z",
    Part.Body to "M70,72 L70,88 a4,4 0 0,0 8,0 L78,72 Z",
    Part.Body to "M34,64 a22,16 0 1,0 44,0 a22,16 0 1,0 -44,0",
    Part.Body to "M40,56 C36,44 32,36 31,28 L44,26 C46,38 50,48 52,58 Z",
    Part.Body to "M22,24 a11,11 0 1,0 22,0 a11,11 0 1,0 -22,0",
    Part.Body to "M14,31 a7,7 0 1,0 14,0 a7,7 0 1,0 -14,0",
    Part.Body to "M40,16 L47,12 L43,23 Z",
    Part.Trim to "M28,15 L38,13 L32,1 Z",
    Part.Eye to "M27,23 a2.6,2.6 0 1,0 5.2,0 a2.6,2.6 0 1,0 -5.2,0"
)

// ---- dinosaur -----------------------------------------------------------

private const val DINO_TAIL = "M70,64 C86,62 96,50 98,34 C100,52 92,70 74,74 Z"

/** Plates, spikes, or a sail. */
private val DINO_ACCENTS: List<String> = listOf(
    "M40,46 L45,28 L50,45 Z M52,44 L58,26 L64,44 Z M65,46 L71,31 L76,48 Z",
    "M41,46 L45,25 L49,45 Z M53,44 L57,22 L61,44 Z M65,46 L69,27 L73,47 Z M29,52 L32,36 L36,50 Z",
    "M38,48 C44,22 68,18 78,48 C68,37 48,37 38,48 Z"
)

private val DINO_REST: List<Pair<Part, String>> = listOf(
    Part.Body to "M38,72 L38,88 a5,5 0 0,0 10,0 L48,72 Z",
    Part.Body to "M58,72 L58,88 a5,5 0 0,0 10,0 L68,72 Z",
    Part.Body to "M28,62 a24,18 0 1,0 48,0 a24,18 0 1,0 -48,0",
    Part.Trim to "M37,70 a15,9 0 1,0 30,0 a15,9 0 1,0 -30,0",
    Part.Body to "M32,50 C28,40 26,32 28,26 L40,30 C39,38 40,46 42,54 Z",
    Part.Body to "M13,22 a13,10 0 1,0 26,0 a13,10 0 1,0 -26,0",
    Part.Body to "M9,27 a6,6 0 1,0 12,0 a6,6 0 1,0 -12,0",
    Part.Eye to "M20,19 a2.6,2.6 0 1,0 5.2,0 a2.6,2.6 0 1,0 -5.2,0"
)

// ---- drawing ------------------------------------------------------------

private val pathCache = HashMap<String, Path>()

/** Parsed once, then reused. Only ever touched from the main thread. */
private fun pathOf(data: String): Path =
    pathCache.getOrPut(data) { PathParser().parsePathString(data).toPath() }

/** The colour a digit reads as, used for its glow and its countdown ring. */
internal fun digitColor(theme: Theme, index: Int): Color = when (theme) {
    Theme.Dots -> DOT_COLORS[index % DOT_COLORS.size]
    Theme.Dinos -> DINO_SKINS[index % DINO_SKINS.size].body
    Theme.Unicorns -> UNICORN_SKINS[index % UNICORN_SKINS.size].body
}

private fun DrawScope.paint(data: String, color: Color, grey: Float, alpha: Float) {
    drawPath(path = pathOf(data), color = lerp(color, Ash, grey), alpha = alpha)
}

/**
 * @param grey 0f for normal, 1f for fully drained. Loser mode uses this.
 * @param rotation degrees, pivoting on the feet so a wiggle looks like a wiggle.
 */
internal fun DrawScope.drawDigit(
    theme: Theme,
    index: Int,
    center: Offset,
    sizePx: Float,
    alpha: Float = 1f,
    grey: Float = 0f,
    rotation: Float = 0f
) {
    if (theme == Theme.Dots) {
        drawCircle(
            color = lerp(DOT_COLORS[index % DOT_COLORS.size], Ash, grey),
            radius = sizePx / 2f,
            center = center,
            alpha = alpha
        )
        return
    }

    withTransform({
        translate(center.x - sizePx / 2f, center.y - sizePx / 2f)
        if (rotation != 0f) {
            rotate(degrees = rotation, pivot = Offset(sizePx / 2f, sizePx * 0.9f))
        }
        scale(scaleX = sizePx / 100f, scaleY = sizePx / 100f, pivot = Offset.Zero)
    }) {
        if (theme == Theme.Unicorns) {
            val skin = UNICORN_SKINS[index % UNICORN_SKINS.size]
            val accent = UNICORN_ACCENTS[index % UNICORN_ACCENTS.size]
            paint(accent.second, skin.accent, grey, alpha)
            paint(accent.first, skin.accent, grey, alpha)
            for ((part, data) in UNICORN_REST) {
                paint(data, skin.colorOf(part), grey, alpha)
            }
        } else {
            val skin = DINO_SKINS[index % DINO_SKINS.size]
            paint(DINO_TAIL, skin.body, grey, alpha)
            paint(DINO_ACCENTS[index % DINO_ACCENTS.size], skin.accent, grey, alpha)
            for ((part, data) in DINO_REST) {
                paint(data, skin.colorOf(part), grey, alpha)
            }
        }
    }
}
