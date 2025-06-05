package com.kizitonwose.calendar.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.format.toIso8601String

@Suppress("FunctionName")
internal fun LazyListScope.CalendarMonths(
    monthCount: Int,
    monthData: (offset: Int) -> CalendarMonth,
    contentHeightMode: ContentHeightMode,
    dayContent: @Composable BoxScope.(CalendarDay) -> Unit,
    monthHeader: (@Composable ColumnScope.(CalendarMonth) -> Unit)?,
    monthBody: (@Composable ColumnScope.(CalendarMonth, content: @Composable () -> Unit) -> Unit)?,
    monthFooter: (@Composable ColumnScope.(CalendarMonth) -> Unit)?,
    monthContainer: (@Composable LazyItemScope.(CalendarMonth, container: @Composable () -> Unit) -> Unit)?,
    onItemPlaced: (itemCoordinates: ItemCoordinates) -> Unit,
) { 
    items(
        count = monthCount,
        key = { offset -> monthData(offset).yearMonth.toIso8601String() },
    ) { offset ->
        val month = monthData(offset)
        val fillHeight = contentHeightMode == ContentHeightMode.Fill
        val currentOnItemPlaced by rememberUpdatedState(onItemPlaced)

        monthContainer.or(defaultMonthContainer)(month) {
            var itemRootCoordinates: LayoutCoordinates? = null
            var firstDayCoordinates: LayoutCoordinates? = null

            fun notifyIfReady() {
                val root = itemRootCoordinates
                val firstDay = firstDayCoordinates
                if (root != null && firstDay != null) {
                    currentOnItemPlaced(ItemCoordinates(root, firstDay))
                }
            }

            Column(
                modifier = Modifier
                    .onPlaced {
                        itemRootCoordinates = it
                        notifyIfReady()
                    }
                    .fillMaxWidth()
                    .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier.wrapContentHeight())
            ) {
                monthHeader?.invoke(this, month)
                monthBody.or(defaultMonthBody)(month) {
                    MonthGrid(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (fillHeight) Modifier.weight(1f) else Modifier.wrapContentHeight()),
                        month = month,
                        dayContent = dayContent,
                        onFirstDayPlaced = {
                            firstDayCoordinates = it
                            notifyIfReady()
                        }
                    )
                }
                monthFooter?.invoke(this, month)
            }
        }
    }
}

@Composable
private fun MonthGrid(
    modifier: Modifier = Modifier,
    month: CalendarMonth,
    dayContent: @Composable BoxScope.(CalendarDay) -> Unit,
    onFirstDayPlaced: (LayoutCoordinates) -> Unit,
) {
    val weeks = month.weekDays
    val onFirstDayPlacedCallback by rememberUpdatedState(onFirstDayPlaced)
    val dayCountInWeek = 7
    val weekCount = weeks.size

    Layout(
        modifier = modifier,
        content = {
            for ((index, day) in weeks.flatten().withIndex()) {
                if (index == 0) {
                    Box(Modifier.onPlaced { onFirstDayPlacedCallback(it) }) {
                        dayContent(day)
                    }
                } else {
                    Box {
                        dayContent(day)
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val cellWidth = constraints.maxWidth / dayCountInWeek
        val cellHeight = when {
            constraints.hasBoundedHeight -> constraints.maxHeight / weekCount
            else -> measurables.firstOrNull()?.minIntrinsicHeight(cellWidth) ?: 0
        }

        val placeables = measurables.map { measurable ->
            measurable.measure(constraints.copy(minWidth = cellWidth, maxWidth = cellWidth))
        }

        layout(constraints.maxWidth, cellHeight * weekCount) {
            placeables.forEachIndexed { index, placeable ->
                placeable.place(
                    x = (index % dayCountInWeek) * cellWidth,
                    y = (index / dayCountInWeek) * cellHeight
                )
            }
        }
    }
}


@Stable
internal class ItemCoordinatesStore(
    private val onItemPlaced: (itemCoordinates: ItemCoordinates) -> Unit,
) {
    private var itemRootCoordinates: LayoutCoordinates? = null
    private var firstDayCoordinates: LayoutCoordinates? = null

    fun onItemRootPlaced(coordinates: LayoutCoordinates) {
        itemRootCoordinates = coordinates
        check()
    }

    fun onFirstDayPlaced(coordinates: LayoutCoordinates) {
        firstDayCoordinates = coordinates
        check()
    }

    private fun check() {
        val itemRootCoordinates = itemRootCoordinates ?: return
        val firstDayCoordinates = firstDayCoordinates ?: return
        val itemCoordinates = ItemCoordinates(
            itemRootCoordinates = itemRootCoordinates,
            firstDayCoordinates = firstDayCoordinates,
        )
        onItemPlaced(itemCoordinates)
    }
}

private inline fun Modifier.onFirstDayPlaced(
    dateRow: Int,
    dateColumn: Int,
    noinline onFirstDayPlaced: (coordinates: LayoutCoordinates) -> Unit,
) = if (dateRow == 0 && dateColumn == 0) {
    onPlaced(onFirstDayPlaced)
} else {
    this
}

private val defaultMonthContainer: (@Composable LazyItemScope.(CalendarMonth, container: @Composable () -> Unit) -> Unit) =
    { _, container -> container() }

private val defaultMonthBody: (@Composable ColumnScope.(CalendarMonth, content: @Composable () -> Unit) -> Unit) =
    { _, content -> content() }

@Suppress("NOTHING_TO_INLINE")
internal inline fun <T> T?.or(default: T) = this ?: default

