package li.songe.gkd.ui.home

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blankj.utilcode.util.KeyboardUtils
import com.ramcosta.composedestinations.generated.destinations.AppConfigPageDestination
import com.ramcosta.composedestinations.utils.toDestinationsNavigator
import kotlinx.coroutines.flow.update
import li.songe.gkd.MainActivity
import li.songe.gkd.ui.component.AppBarTextField
import li.songe.gkd.ui.component.AppIcon
import li.songe.gkd.ui.component.AppNameText
import li.songe.gkd.ui.component.EmptyText
import li.songe.gkd.ui.component.QueryPkgAuthCard
import li.songe.gkd.ui.component.autoFocus
import li.songe.gkd.ui.style.EmptyHeight
import li.songe.gkd.ui.style.appItemPadding
import li.songe.gkd.ui.style.menuPadding
import li.songe.gkd.util.LocalNavController
import li.songe.gkd.util.SortTypeOption
import li.songe.gkd.util.launchTry
import li.songe.gkd.util.mapHashCode
import li.songe.gkd.util.ruleSummaryFlow
import li.songe.gkd.util.storeFlow
import li.songe.gkd.util.throttle

val appListNav = BottomNavItem(
    label = "应用", icon = Icons.Default.Apps
)

@Composable
fun useAppListPage(): ScaffoldExt {
    val navController = LocalNavController.current
    val context = LocalActivity.current as MainActivity
    val softwareKeyboardController = LocalSoftwareKeyboardController.current

    val vm = viewModel<HomeVm>()
    val showSystemApp by vm.showSystemAppFlow.collectAsState()
    val showHiddenApp by vm.showHiddenAppFlow.collectAsState()
    val sortType by vm.sortTypeFlow.collectAsState()
    val orderedAppInfos by vm.appInfosFlow.collectAsState()
    val searchStr by vm.searchStrFlow.collectAsState()
    val ruleSummary by ruleSummaryFlow.collectAsState()

    val globalDesc = if (ruleSummary.globalGroups.isNotEmpty()) {
        "${ruleSummary.globalGroups.size}全局"
    } else {
        null
    }
    val showSearchBar by vm.showSearchBarFlow.collectAsState()
    val listState = rememberLazyListState()

    var isFirstVisit by remember { mutableStateOf(true) }
    LaunchedEffect(
        key1 = orderedAppInfos.mapHashCode { it.id }
    ) {
        if (isFirstVisit) {
            isFirstVisit = false
        } else {
            listState.scrollToItem(0)
        }
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    return ScaffoldExt(
        navItem = appListNav,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DisposableEffect(null) {
                onDispose {
                    if (vm.searchStrFlow.value.isEmpty()) {
                        vm.showSearchBarFlow.value = false
                    }
                }
            }
            TopAppBar(scrollBehavior = scrollBehavior, title = {
                if (showSearchBar) {
                    BackHandler {
                        if (KeyboardUtils.isSoftInputVisible(context)) {
                            softwareKeyboardController?.hide()
                        } else {
                            vm.showSearchBarFlow.value = false
                        }
                    }
                    AppBarTextField(
                        value = searchStr,
                        onValueChange = { newValue -> vm.searchStrFlow.value = newValue.trim() },
                        hint = "请输入应用名称/ID",
                        modifier = Modifier.autoFocus()
                    )
                } else {
                    Text(
                        modifier = Modifier.clickable(
                            enabled = orderedAppInfos.isNotEmpty(),
                            onClick = throttle {
                                if (orderedAppInfos.isNotEmpty()) {
                                    vm.viewModelScope.launchTry { listState.scrollToItem(0) }
                                }
                            }
                        ),
                        text = appListNav.label,
                    )
                }
            }, actions = {
                if (showSearchBar) {
                    IconButton(onClick = throttle {
                        if (vm.searchStrFlow.value.isEmpty()) {
                            vm.showSearchBarFlow.value = false
                        } else {
                            vm.searchStrFlow.value = ""
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null
                        )
                    }
                } else {
                    IconButton(onClick = throttle {
                        vm.showSearchBarFlow.value = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    }
                }
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = {
                    expanded = true
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null
                    )
                }
                Box(
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart)
                ) {
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        Text(
                            text = "排序",
                            modifier = Modifier.menuPadding(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        SortTypeOption.allSubObject.forEach { sortOption ->
                            DropdownMenuItem(
                                text = {
                                    Text(sortOption.label)
                                },
                                trailingIcon = {
                                    RadioButton(selected = sortType == sortOption,
                                        onClick = {
                                            storeFlow.update { s -> s.copy(sortType = sortOption.value) }
                                        }
                                    )
                                },
                                onClick = {
                                    storeFlow.update { s -> s.copy(sortType = sortOption.value) }
                                },
                            )
                        }
                        Text(
                            text = "筛选",
                            modifier = Modifier.menuPadding(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        DropdownMenuItem(
                            text = {
                                Text("显示系统应用")
                            },
                            trailingIcon = {
                                Checkbox(
                                    checked = showSystemApp,
                                    onCheckedChange = {
                                        storeFlow.update { s -> s.copy(showSystemApp = !showSystemApp) }
                                    }
                                )
                            },
                            onClick = {
                                storeFlow.update { s -> s.copy(showSystemApp = !showSystemApp) }
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text("显示隐藏应用")
                            },
                            trailingIcon = {
                                Checkbox(
                                    checked = showHiddenApp,
                                    onCheckedChange = {
                                        storeFlow.update { s -> s.copy(showHiddenApp = !s.showHiddenApp) }
                                    })
                            },
                            onClick = {
                                storeFlow.update { s -> s.copy(showHiddenApp = !showHiddenApp) }
                            },
                        )
                    }
                }

            })
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.padding(contentPadding),
            state = listState
        ) {
            items(orderedAppInfos, { it.id }) { appInfo ->
                Row(
                    modifier = Modifier
                        .clickable(onClick = throttle {
                            navController
                                .toDestinationsNavigator()
                                .navigate(AppConfigPageDestination(appInfo.id))
                        })
                        .appItemPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(appInfo = appInfo)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        AppNameText(appInfo = appInfo)
                        val appGroups = ruleSummary.appIdToAllGroups[appInfo.id] ?: emptyList()
                        val appDesc = if (appGroups.isNotEmpty()) {
                            when (val disabledCount = appGroups.count { g -> !g.enable }) {
                                0 -> {
                                    "${appGroups.size}组规则"
                                }

                                appGroups.size -> {
                                    "${appGroups.size}组规则/${disabledCount}关闭"
                                }

                                else -> {
                                    "${appGroups.size}组规则/${appGroups.size - disabledCount}启用/${disabledCount}关闭"
                                }
                            }
                        } else {
                            null
                        }
                        val desc = if (globalDesc != null) {
                            if (appDesc != null) {
                                "$globalDesc/$appDesc"
                            } else {
                                globalDesc
                            }
                        } else {
                            appDesc
                        }
                        if (desc != null) {
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(EmptyHeight))
                if (orderedAppInfos.isEmpty() && searchStr.isNotEmpty()) {
                    val hasShowAll = showSystemApp && showHiddenApp
                    EmptyText(text = if (hasShowAll) "暂无搜索结果" else "暂无搜索结果,请尝试修改筛选条件")
                }
                QueryPkgAuthCard()
            }
        }
    }
}