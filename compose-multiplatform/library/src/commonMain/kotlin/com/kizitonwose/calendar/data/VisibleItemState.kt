package com.kizitonwose.calendar.data

import androidx.compose.runtime.Immutable

@Immutable
public class VisibleItemState(
    public val firstVisibleItemIndex: Int = 0,
    public val firstVisibleItemScrollOffset: Int = 0,
)
