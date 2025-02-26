package li.songe.gkd.ui.home

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.generated.destinations.ActionLogPageDestination
import com.ramcosta.composedestinations.generated.destinations.ActivityLogPageDestination
import com.ramcosta.composedestinations.generated.destinations.AuthA11YPageDestination
import com.ramcosta.composedestinations.utils.toDestinationsNavigator
import li.songe.gkd.MainActivity
import li.songe.gkd.a11yServiceEnabledFlow
import li.songe.gkd.permission.notificationState
import li.songe.gkd.permission.requiredPermission
import li.songe.gkd.permission.writeSecureSettingsState
import li.songe.gkd.service.A11yService
import li.songe.gkd.service.ManageService
import li.songe.gkd.service.switchA11yService
import li.songe.gkd.ui.style.EmptyHeight
import li.songe.gkd.ui.style.itemHorizontalPadding
import li.songe.gkd.ui.style.itemVerticalPadding
import li.songe.gkd.ui.style.surfaceCardColors
import li.songe.gkd.util.HOME_PAGE_URL
import li.songe.gkd.util.LocalNavController
import li.songe.gkd.util.SafeR
import li.songe.gkd.util.launchAsFn
import li.songe.gkd.util.openUri
import li.songe.gkd.util.storeFlow
import li.songe.gkd.util.throttle

val controlNav = BottomNavItem(label = "主页", icon = Icons.Outlined.Home)

@Composable
fun useControlPage(): ScaffoldExt {
    val context = LocalActivity.current as MainActivity
    val navController = LocalNavController.current
    val vm = viewModel<HomeVm>()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scrollState = rememberScrollState()
    val writeSecureSettings by writeSecureSettingsState.stateFlow.collectAsState()
    return ScaffoldExt(navItem = controlNav,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(scrollBehavior = scrollBehavior, title = {
                Text(
                    text = stringResource(SafeR.app_name),
                )
            }, actions = {
                IconButton(onClick = throttle {
                    navController.toDestinationsNavigator().navigate(AuthA11YPageDestination)
                }) {
                    Icon(
                        imageVector = Icons.Outlined.RocketLaunch,
                        contentDescription = null,
                    )
                }
            })
        }
    ) { contentPadding ->
        val store by storeFlow.collectAsState()

        val a11yRunning by A11yService.isRunning.collectAsState()
        val manageRunning by ManageService.isRunning.collectAsState()
        val a11yServiceEnabled by a11yServiceEnabledFlow.collectAsState()

        // 无障碍故障: 设置中无障碍开启, 但是实际 service 没有运行
        val a11yBroken = !writeSecureSettings && !a11yRunning && a11yServiceEnabled

        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(contentPadding)
        ) {
            if (writeSecureSettings) {
                PageItemCard(
                    imageVector = Icons.Default.Memory,
                    title = "服务状态",
                    subtitle = if (a11yRunning) "无障碍服务正在运行" else "无障碍服务已关闭",
                    rightContent = {
                        Switch(
                            checked = a11yRunning,
                            onCheckedChange = throttle<Boolean> {
                                switchA11yService()
                            },
                        )
                    }
                )
            }
            if (!writeSecureSettings && !a11yRunning) {
                PageItemCard(
                    imageVector = Icons.Default.Memory,
                    title = "无障碍授权",
                    subtitle = if (a11yBroken) "服务故障,请重新授权" else "授权使无障碍服务运行",
                    rightContent = {
                        OutlinedButton(onClick = throttle {
                            navController.toDestinationsNavigator()
                                .navigate(AuthA11YPageDestination)
                        }) {
                            Text(text = "授权")
                        }
                    }
                )
            }

            PageItemCard(
                imageVector = Icons.Outlined.Notifications,
                title = "常驻通知",
                subtitle = "显示运行状态及统计数据",
                rightContent = {
                    Switch(
                        checked = manageRunning && store.enableStatusService,
                        onCheckedChange = throttle(fn = vm.viewModelScope.launchAsFn<Boolean> {
                            if (it) {
                                requiredPermission(context, notificationState)
                                storeFlow.value = store.copy(
                                    enableStatusService = true
                                )
                                ManageService.start()
                            } else {
                                storeFlow.value = store.copy(
                                    enableStatusService = false
                                )
                                ManageService.stop()
                            }
                        }),
                    )
                }
            )

            ServerStatusCard(vm)

            PageItemCard(
                title = "触发记录",
                subtitle = "规则误触可定位关闭",
                imageVector = Icons.Default.History,
                onClick = {
                    navController.toDestinationsNavigator()
                        .navigate(ActionLogPageDestination())
                }
            )

            if (store.enableActivityLog) {
                PageItemCard(
                    title = "界面记录",
                    subtitle = "记录打开的应用及界面",
                    imageVector = Icons.Outlined.Layers,
                    onClick = {
                        navController.toDestinationsNavigator()
                            .navigate(ActivityLogPageDestination)
                    }
                )
            }

            PageItemCard(
                title = "了解 GKD",
                subtitle = "查阅规则文档和常见问题",
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                onClick = {
                    openUri(HOME_PAGE_URL)
                }
            )
            Spacer(modifier = Modifier.height(EmptyHeight))
        }
    }
}


@Composable
private fun PageItemCard(
    imageVector: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {},
    rightContent: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier
            .padding(itemHorizontalPadding, 4.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = surfaceCardColors,
        onClick = throttle(fn = onClick)
    ) {
        IconTextCard(
            imageVector = imageVector,
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (rightContent != null) {
                Spacer(Modifier.width(8.dp))
                rightContent.invoke()
            }
        }
    }
}

@Composable
private fun IconTextCard(
    imageVector: ImageVector,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(itemVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(8.dp)
                .size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(itemHorizontalPadding))
        content()
    }
}

@Composable
private fun ServerStatusCard(vm: HomeVm) {
    Card(
        modifier = Modifier
            .padding(itemHorizontalPadding, 4.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = surfaceCardColors,
        onClick = {}
    ) {
        IconTextCard(
            imageVector = Icons.Outlined.Equalizer
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "数据概览",
                    style = MaterialTheme.typography.bodyLarge,
                )
                val usedSubsItemCount by vm.usedSubsItemCountFlow.collectAsState()
                AnimatedVisibility(usedSubsItemCount > 0) {
                    Text(
                        text = "已开启 $usedSubsItemCount 条订阅",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = itemVerticalPadding + 8.dp,
                )
        ) {
            val latestRecordDesc by vm.latestRecordDescFlow.collectAsState()
            val subsStatus by vm.subsStatusFlow.collectAsState()
            AnimatedVisibility(subsStatus.isNotEmpty()) {
                Text(
                    text = subsStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(latestRecordDesc != null) {
                Text(
                    text = "最近触发: $latestRecordDesc",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(itemVerticalPadding))
        }
    }
}
