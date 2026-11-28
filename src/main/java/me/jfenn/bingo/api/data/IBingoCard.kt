package me.jfenn.bingo.api.data

import java.util.UUID

/**
 * A read-only snapshot of a bingo card, exposed to other mods through [me.jfenn.bingo.api.IBingoApi].
 */
interface IBingoCard {
    val id: UUID

    /** The seed used to generate this card. */
    val seed: Long

    /** The 25 entries, in row-major order (index `x + y * 5`). */
    val entries: List<IBingoCardEntry>

    /** The rules this card was generated with; this is an immutable copy. */
    val options: IBingoCardOptions
}

/** One tile of the card. */
interface IBingoCardEntry {
    val objectiveId: String

    /** Column, 0..4. */
    val x: Int

    /** Row, 0..4. */
    val y: Int

    /** The difficulty tier the objective was drawn from, if any. */
    val tier: String?

    /** The tier list the objective was drawn from, if any. */
    val source: String?

    /** A custom tile name set by the pack, if any. */
    val tileName: String?
}

/** Immutable snapshot of the card rules (the live options object is mutable). */
interface IBingoCardOptions {
    /** `"lines"` or `"items"`. */
    val goalType: String

    /** The number of lines/items required by the goal. */
    val goalCount: Int

    /** Number of tiles per difficulty tier (S, A, B, C, D). */
    val distribution: List<Int>

    val isLockoutMode: Boolean
    val isInventoryMode: Boolean
    val isHiddenItemsMode: Boolean
    val isConsumeItemsMode: Boolean
}
