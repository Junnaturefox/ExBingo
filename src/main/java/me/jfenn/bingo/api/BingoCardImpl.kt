package me.jfenn.bingo.api

import me.jfenn.bingo.api.data.IBingoCard
import me.jfenn.bingo.api.data.IBingoCardEntry
import me.jfenn.bingo.api.data.IBingoCardOptions
import java.util.UUID

internal class BingoCardImpl(
    override val id: UUID,
    override val seed: Long,
    override val entries: List<IBingoCardEntry>,
    override val options: IBingoCardOptions,
) : IBingoCard

internal class BingoCardEntryImpl(
    override val objectiveId: String,
    override val x: Int,
    override val y: Int,
    override val tier: String?,
    override val source: String?,
    override val tileName: String?,
) : IBingoCardEntry

internal class BingoCardOptionsImpl(
    override val goalType: String,
    override val goalCount: Int,
    override val distribution: List<Int>,
    override val isLockoutMode: Boolean,
    override val isInventoryMode: Boolean,
    override val isHiddenItemsMode: Boolean,
    override val isConsumeItemsMode: Boolean,
) : IBingoCardOptions
