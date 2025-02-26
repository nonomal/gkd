package li.songe.gkd.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.GlobalGroupListPageDestination
import com.ramcosta.composedestinations.generated.destinations.SubsAppGroupListPageDestination
import com.ramcosta.composedestinations.utils.toDestinationsNavigator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import li.songe.gkd.MainActivity
import li.songe.gkd.data.ActionLog
import li.songe.gkd.data.ExcludeData
import li.songe.gkd.data.RawSubscription
import li.songe.gkd.data.SubsConfig
import li.songe.gkd.db.DbSet
import li.songe.gkd.ui.component.AppNameText
import li.songe.gkd.ui.component.EmptyText
import li.songe.gkd.ui.component.FixedTimeText
import li.songe.gkd.ui.component.LocalNumberCharWidth
import li.songe.gkd.ui.component.StartEllipsisText
import li.songe.gkd.ui.component.TowLineText
import li.songe.gkd.ui.component.measureNumberTextWidth
import li.songe.gkd.ui.component.useSubs
import li.songe.gkd.ui.component.waitResult
import li.songe.gkd.ui.style.EmptyHeight
import li.songe.gkd.ui.style.itemHorizontalPadding
import li.songe.gkd.ui.style.scaffoldPadding
import li.songe.gkd.util.LocalNavController
import li.songe.gkd.util.ProfileTransitions
import li.songe.gkd.util.appInfoCacheFlow
import li.songe.gkd.util.launchAsFn
import li.songe.gkd.util.map
import li.songe.gkd.util.subsIdToRawFlow
import li.songe.gkd.util.subsItemsFlow
import li.songe.gkd.util.throttle
import li.songe.gkd.util.toast

@Destination<RootGraph>(style = ProfileTransitions::class)
@Composable
fun ActionLogPage(
    subsId: Long? = null,
    appId: String? = null,
) {
    val context = LocalActivity.current as MainActivity
    val navController = LocalNavController.current
    val vm = viewModel<ActionLogVm>()
    val actionDataItems = vm.pagingDataFlow.collectAsLazyPagingItems()

    val timeTextWidth = measureNumberTextWidth(MaterialTheme.typography.bodySmall)

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
        TopAppBar(
            scrollBehavior = scrollBehavior,
            navigationIcon = {
                IconButton(onClick = throttle {
                    navController.popBackStack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                    )
                }
            },
            title = {
                val title = "触发记录"
                if (subsId != null) {
                    TowLineText(
                        title = title,
                        subtitle = useSubs(subsId)?.name ?: subsId.toString()
                    )
                } else if (appId != null) {
                    TowLineText(
                        title = title,
                        subtitle = appId,
                        showApp = true,
                    )
                } else {
                    Text(text = title)
                }
            },
            actions = {
                if (actionDataItems.itemCount > 0) {
                    IconButton(onClick = throttle(fn = context.mainVm.viewModelScope.launchAsFn {
                        val text = if (subsId != null) {
                            "确定删除当前订阅所有触发记录?"
                        } else if (appId != null) {
                            "确定删除当前应用所有触发记录?"
                        } else {
                            "确定删除所有触发记录?"
                        }
                        context.mainVm.dialogFlow.waitResult(
                            title = "删除记录",
                            text = text,
                            error = true,
                        )
                        if (subsId != null) {
                            DbSet.actionLogDao.deleteSubsAll(subsId)
                        } else if (appId != null) {
                            DbSet.actionLogDao.deleteAppAll(appId)
                        } else {
                            DbSet.actionLogDao.deleteAll()
                        }

                    })) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            })
    }, content = { contentPadding ->
        LazyColumn(
            modifier = Modifier.scaffoldPadding(contentPadding),
        ) {
            items(
                count = actionDataItems.itemCount,
                key = actionDataItems.itemKey { c -> c.first.id }
            ) { i ->
                val item = actionDataItems[i] ?: return@items
                val lastItem = if (i > 0) actionDataItems[i - 1] else null
                CompositionLocalProvider(
                    LocalNumberCharWidth provides timeTextWidth
                ) {
                    ActionLogCard(
                        i = i,
                        item = item,
                        lastItem = lastItem,
                        onClick = {
                            vm.showActionLogFlow.value = item.first
                        },
                        subsId = subsId,
                        appId = appId,
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(EmptyHeight))
                if (actionDataItems.itemCount == 0 && actionDataItems.loadState.refresh !is LoadState.Loading) {
                    EmptyText(text = "暂无记录")
                }
            }
        }
    })

    vm.showActionLogFlow.collectAsState().value?.let {
        ActionLogDialog(
            vm = vm,
            actionLog = it,
            onDismissRequest = {
                vm.showActionLogFlow.value = null
            }
        )
    }
}


@Composable
private fun ActionLogCard(
    i: Int,
    item: Triple<ActionLog, RawSubscription.RawGroupProps?, RawSubscription.RawRuleProps?>,
    lastItem: Triple<ActionLog, RawSubscription.RawGroupProps?, RawSubscription.RawRuleProps?>?,
    onClick: () -> Unit,
    subsId: Long?,
    appId: String?,
) {
    val context = LocalActivity.current as MainActivity
    val (actionLog, group, rule) = item
    val lastActionLog = lastItem?.first
    val isDiffApp = actionLog.appId != lastActionLog?.appId
    val verticalPadding = if (i == 0) 0.dp else if (isDiffApp) 12.dp else 8.dp
    val subsIdToRaw by subsIdToRawFlow.collectAsState()
    val subscription = subsIdToRaw[actionLog.subsId]
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = itemHorizontalPadding,
                end = itemHorizontalPadding,
                top = verticalPadding
            )
    ) {
        if (isDiffApp && appId == null) {
            AppNameText(appId = actionLog.appId)
        }
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            if (appId == null) {
                Spacer(modifier = Modifier.width(2.dp))
            }
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                FixedTimeText(
                    text = actionLog.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
                    val showActivityId = actionLog.showActivityId
                    if (showActivityId != null) {
                        StartEllipsisText(
                            text = showActivityId,
                            modifier = Modifier.height(LocalTextStyle.current.lineHeight.value.dp),
                        )
                    } else {
                        Text(
                            text = "null",
                            color = LocalContentColor.current.copy(alpha = 0.5f),
                        )
                    }
                    if (subsId == null) {
                        Text(
                            text = subscription?.name ?: "id=${actionLog.subsId}",
                            modifier = Modifier.clickable(onClick = throttle {
                                if (subsItemsFlow.value.any { it.id == actionLog.subsId }) {
                                    context.mainVm.sheetSubsIdFlow.value = actionLog.subsId
                                } else {
                                    toast("订阅不存在")
                                }
                            })
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val groupDesc = group?.name.toString()
                        if (actionLog.groupType == SubsConfig.GlobalGroupType) {
                            Icon(
                                imageVector = Icons.Default.SportsBasketball,
                                contentDescription = null,
                                modifier = Modifier
                                    .clickable(onClick = throttle {
                                        toast("${group?.name ?: "当前规则组"} 是全局规则组")
                                    })
                                    .size(LocalTextStyle.current.lineHeight.value.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = groupDesc,
                            color = LocalContentColor.current.let {
                                if (group?.name == null) it.copy(alpha = 0.5f) else it
                            },
                        )

                        val ruleDesc = rule?.name ?: (if ((group?.rules?.size ?: 0) > 1) {
                            val keyDesc = actionLog.ruleKey?.let { "key=$it, " } ?: ""
                            "${keyDesc}index=${actionLog.ruleIndex}"
                        } else {
                            null
                        })
                        if (ruleDesc != null) {
                            Text(
                                text = ruleDesc,
                                modifier = Modifier.padding(start = 8.dp),
                                color = LocalContentColor.current.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionLogDialog(
    vm: ViewModel,
    actionLog: ActionLog,
    onDismissRequest: () -> Unit,
) {
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    val appInfo = remember(actionLog.appId) {
        appInfoCacheFlow.map(scope) { it[actionLog.appId] }
    }.collectAsState().value
    val subsConfig = remember(actionLog) {
        (if (actionLog.groupType == SubsConfig.AppGroupType) {
            DbSet.subsConfigDao.queryAppGroupTypeConfig(
                actionLog.subsId, actionLog.appId, actionLog.groupKey
            )
        } else {
            DbSet.subsConfigDao.queryGlobalGroupTypeConfig(actionLog.subsId, actionLog.groupKey)
        }).stateIn(vm.viewModelScope, SharingStarted.Eagerly, null)
    }.collectAsState().value

    val oldExclude = remember(subsConfig?.exclude) {
        ExcludeData.parse(subsConfig?.exclude)
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            ItemText(
                text = "查看规则组",
                onClick = {
                    onDismissRequest()
                    if (actionLog.groupType == SubsConfig.AppGroupType) {
                        navController
                            .toDestinationsNavigator()
                            .navigate(
                                SubsAppGroupListPageDestination(
                                    actionLog.subsId, actionLog.appId, actionLog.groupKey
                                )
                            )
                    } else if (actionLog.groupType == SubsConfig.GlobalGroupType) {
                        navController
                            .toDestinationsNavigator()
                            .navigate(
                                GlobalGroupListPageDestination(
                                    actionLog.subsId, actionLog.groupKey
                                )
                            )
                    }
                }
            )
            HorizontalDivider()

            if (actionLog.groupType == SubsConfig.GlobalGroupType) {
                val subs = remember(actionLog.subsId) {
                    subsIdToRawFlow.map(scope) { it[actionLog.subsId] }
                }.collectAsState().value
                val group = subs?.globalGroups?.find { g -> g.key == actionLog.groupKey }
                val appChecked = if (group != null) {
                    getChecked(
                        oldExclude,
                        group,
                        actionLog.appId,
                        appInfo
                    )
                } else {
                    null
                }
                if (appChecked != null) {
                    ItemText(
                        text = if (appChecked) "在此应用禁用" else "移除在此应用的禁用",
                        onClick = vm.viewModelScope.launchAsFn {
                            val subsConfig = subsConfig ?: SubsConfig(
                                type = SubsConfig.GlobalGroupType,
                                subsItemId = actionLog.subsId,
                                groupKey = actionLog.groupKey,
                            )
                            val newSubsConfig = subsConfig.copy(
                                exclude = oldExclude
                                    .copy(
                                        appIds = oldExclude.appIds
                                            .toMutableMap()
                                            .apply {
                                                set(actionLog.appId, appChecked)
                                            })
                                    .stringify()
                            )
                            DbSet.subsConfigDao.insert(newSubsConfig)
                            toast("更新禁用")
                        }
                    )
                    HorizontalDivider()
                }
            }

            if (actionLog.activityId != null) {
                val disabled =
                    oldExclude.activityIds.contains(actionLog.appId to actionLog.activityId)
                ItemText(
                    text = if (disabled) "移除在此页面的禁用" else "在此页面禁用",
                    onClick = vm.viewModelScope.launchAsFn {
                        val subsConfig = if (actionLog.groupType == SubsConfig.AppGroupType) {
                            subsConfig ?: SubsConfig(
                                type = SubsConfig.AppGroupType,
                                subsItemId = actionLog.subsId,
                                appId = actionLog.appId,
                                groupKey = actionLog.groupKey,
                            )
                        } else {
                            subsConfig ?: SubsConfig(
                                type = SubsConfig.GlobalGroupType,
                                subsItemId = actionLog.subsId,
                                groupKey = actionLog.groupKey,
                            )
                        }
                        val newSubsConfig = subsConfig.copy(
                            exclude = oldExclude
                                .switch(
                                    actionLog.appId,
                                    actionLog.activityId
                                )
                                .stringify()
                        )
                        DbSet.subsConfigDao.insert(newSubsConfig)
                        toast("更新禁用")
                    }
                )
                HorizontalDivider()
            }

            ItemText(
                text = "删除记录",
                color = MaterialTheme.colorScheme.error,
                onClick = vm.viewModelScope.launchAsFn {
                    onDismissRequest()
                    DbSet.actionLogDao.delete(actionLog)
                    toast("删除成功")
                }
            )
        }
    }
}

@Composable
fun ItemText(
    text: String,
    color: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val modifier = Modifier
        .clickable(onClick = throttle(onClick))
        .fillMaxWidth()
        .padding(16.dp)
    Text(
        modifier = modifier,
        text = text,
        color = color,
    )
}
