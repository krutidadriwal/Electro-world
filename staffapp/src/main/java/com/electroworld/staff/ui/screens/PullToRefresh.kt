package com.electroworld.staff.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// How far down a list has to be pulled before letting go refreshes it --
// deliberately well past Material's default (80dp) so an ordinary scroll
// back to the top doesn't set off a reload by accident.
private val PULL_TO_REFRESH_THRESHOLD = 140.dp

// Pull down from the top of the list to reload. Every tab wraps its list
// in this. isRefreshing should only be true for a reload started by a pull
// (not the first load or the reloads after saving something), so the
// spinner up top only ever answers a pull.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffPullToRefresh(
  isRefreshing: Boolean,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit
) {
  val state = rememberPullToRefreshState()
  Box(
    modifier = modifier
      .fillMaxSize()
      .pullToRefresh(isRefreshing = isRefreshing, state = state, threshold = PULL_TO_REFRESH_THRESHOLD, onRefresh = onRefresh)
  ) {
    content()
    PullToRefreshDefaults.Indicator(
      state = state,
      isRefreshing = isRefreshing,
      threshold = PULL_TO_REFRESH_THRESHOLD,
      modifier = Modifier.align(Alignment.TopCenter)
    )
  }
}

// A full-height, centered message ("No items", an error...) that can still
// be pulled to refresh -- a pull is only picked up from something
// scrollable, so a plain Box would ignore it.
@Composable
fun PullableMessage(content: @Composable BoxScope.() -> Unit) {
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val viewportHeight = maxHeight
    Box(
      modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).height(viewportHeight),
      contentAlignment = Alignment.Center,
      content = content
    )
  }
}
