package com.ivy.wallet.ui.reports

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.insets.systemBarsPadding
import com.ivy.design.api.navigation
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.wallet.R
import com.ivy.wallet.domain.data.TransactionType
import com.ivy.wallet.domain.data.entity.Account
import com.ivy.wallet.domain.data.entity.Category
import com.ivy.wallet.ui.IvyWalletPreview
import com.ivy.wallet.ui.PieChartStatistic
import com.ivy.wallet.ui.Report
import com.ivy.wallet.ui.ivyWalletCtx
import com.ivy.wallet.ui.statistic.level2.IncomeExpensesCards
import com.ivy.wallet.ui.theme.*
import com.ivy.wallet.ui.theme.components.*
import com.ivy.wallet.ui.theme.transaction.TransactionsDividerLine
import com.ivy.wallet.ui.theme.transaction.transactions
import com.ivy.wallet.utils.clickableNoIndication
import com.ivy.wallet.utils.onScreenStart

@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.ReportScreen(
    screen: Report
) {
    val viewModel: ReportViewModel = viewModel()
    val state by viewModel.state().collectAsState()

    onScreenStart {
        viewModel.start()
    }

    UI(
        state = state,
        onEventHandler = viewModel::onEvent
    )
}

@ExperimentalFoundationApi
@Composable
private fun BoxWithConstraintsScope.UI(
    state: ReportScreenState = ReportScreenState(),
    onEventHandler: (ReportScreenEvent) -> Unit = {}
) {
    val ivyContext = ivyWalletCtx()
    val nav = navigation()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    if (state.loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1000f)
                .background(pureBlur())
                .clickableNoIndication {
                    //consume clicks
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Generating report...",
                style = UI.typo.b1.style(
                    fontWeight = FontWeight.ExtraBold,
                    color = Orange
                )
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        stickyHeader {
            Toolbar(
                onExport = {
                    onEventHandler.invoke(ReportScreenEvent.OnExport(context = context))
                },
                onFilter = {
                    onEventHandler.invoke(
                        ReportScreenEvent.OnFilterOverlayVisible(
                            filterOverlayVisible = true
                        )
                    )
                }
            )
        }

        item {
            Text(
                modifier = Modifier.padding(
                    start = 32.dp
                ),
                text = "Reports",
                style = UI.typo.h2.style(
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.height(8.dp))

            BalanceRow(
                modifier = Modifier
                    .padding(start = 32.dp),
                textColor = UI.colors.pureInverse,
                currency = state.baseCurrency,
                balance = state.balance,
                balanceAmountPrefix = when {
                    state.balance > 0 -> "+"
                    else -> null
                }
            )

            Spacer(Modifier.height(20.dp))

            IncomeExpensesCards(
                history = state.history,
                currency = state.baseCurrency,
                income = state.income,
                expenses = state.expenses,
                hasAddButtons = false,
                itemColor = UI.colors.pure,
                incomeHeaderCardClicked = {
                    if (state.transactions.isNotEmpty())
                        nav.navigateTo(
                            PieChartStatistic(
                                type = TransactionType.INCOME,
                                transactions = state.transactions,
                                accountList = state.accountIdFilters
                            )
                        )
                },
                expenseHeaderCardClicked = {
                    if (state.transactions.isNotEmpty())
                        nav.navigateTo(
                            PieChartStatistic(
                                type = TransactionType.EXPENSE,
                                transactions = state.transactions,
                                accountList = state.accountIdFilters
                            )
                        )
                }
            )

            Spacer(Modifier.height(32.dp))

            TransactionsDividerLine(
                paddingHorizontal = 0.dp
            )

            Spacer(Modifier.height(4.dp))
        }

        if (state.filter != null) {
            transactions(
                ivyContext = ivyContext,
                nav = nav,
                baseCurrency = state.baseCurrency,

                upcomingIncome = state.upcomingIncome,
                upcomingExpenses = state.upcomingExpenses,
                overdueIncome = state.overdueIncome,
                overdueExpenses = state.overdueExpenses,

                categories = state.categories,
                accounts = state.accounts,
                listState = listState,

                overdue = state.overdueTransactions,
                overdueExpanded = state.overdueExpanded,
                setOverdueExpanded = {
                    onEventHandler.invoke(ReportScreenEvent.OnOverdueExpanded(overdueExpanded = it))
                },

                history = state.history,

                upcoming = state.upcomingTransactions,
                upcomingExpanded = state.upcomingExpanded,
                setUpcomingExpanded = {
                    onEventHandler.invoke(ReportScreenEvent.OnUpcomingExpanded(upcomingExpanded = it))
                },

                lastItemSpacer = 48.dp,
                onPayOrGet = {
                    onEventHandler.invoke(ReportScreenEvent.OnPayOrGet(transaction = it))
                },
                emptyStateTitle = "No transactions",

                emptyStateText = "You don't have any transactions for your filter."
            )
        } else {
            item {
                NoFilterEmptyState(
                    setFilterOverlayVisible = {
                        onEventHandler.invoke(
                            ReportScreenEvent.OnFilterOverlayVisible(
                                filterOverlayVisible = it
                            )
                        )
                    }
                )
            }
        }
    }

    FilterOverlay(
        visible = state.filterOverlayVisible,
        baseCurrency = state.baseCurrency,
        accounts = state.accounts,
        categories = state.categories,
        filter = state.filter,
        onClose = {
            onEventHandler.invoke(
                ReportScreenEvent.OnFilterOverlayVisible(
                    filterOverlayVisible = false
                )
            )
        },
        onSetFilter = {
            onEventHandler.invoke(ReportScreenEvent.OnFilter(filter = it))
        }
    )
}

@Composable
private fun NoFilterEmptyState(
    setFilterOverlayVisible: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        IvyIcon(
            icon = R.drawable.ic_filter_l,
            tint = Gray
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "No Filter",
            style = UI.typo.b1.style(
                color = Gray,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = "To generate a report you must first set a valid filter.",
            style = UI.typo.b2.style(
                color = Gray,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )

        Spacer(Modifier.height(32.dp))

        IvyButton(
            iconStart = R.drawable.ic_filter_xs,
            text = "Set Filter"
        ) {
            setFilterOverlayVisible(true)
        }

        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun Toolbar(
    onExport: () -> Unit,
    onFilter: () -> Unit
) {
    val nav = navigation()
    IvyToolbar(
        backButtonType = BackButtonType.CLOSE,
        onBack = {
            nav.back()
        }
    ) {
        Spacer(Modifier.weight(1f))

        //Export CSV
        IvyOutlinedButton(
            text = "Export",
            iconTint = Green,
            textColor = Green,
            solidBackground = true,
            padding = 8.dp,
            iconStart = R.drawable.ic_export_csv
        ) {
            onExport()
        }

        Spacer(Modifier.width(16.dp))

        //Filter
        CircleButtonFilled(
            icon = R.drawable.ic_filter_xs
        ) {
            onFilter()
        }

        Spacer(Modifier.width(24.dp))
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview() {
    IvyWalletPreview {
        val acc1 = Account("Cash", color = Green.toArgb())
        val acc2 = Account("DSK", color = GreenDark.toArgb())
        val cat1 = Category("Science", color = Purple1Dark.toArgb(), icon = "atom")
        val state = ReportScreenState(
            baseCurrency = "BGN",
            balance = -6405.66,
            income = 2000.0,
            expenses = 8405.66,
            upcomingIncome = 4800.23,
            upcomingExpenses = 0.0,
            overdueIncome = 2335.12,
            overdueExpenses = 0.0,
            history = emptyList(),
            upcomingTransactions = emptyList(),
            overdueTransactions = emptyList(),

            upcomingExpanded = true,
            overdueExpanded = true,
            filter = ReportFilter.emptyFilter("BGN"),
            loading = false,
            accounts = listOf(
                acc1,
                acc2,
                Account("phyre", color = GreenLight.toArgb(), icon = "cash"),
                Account("Revolut", color = IvyDark.toArgb()),
            ),
            categories = listOf(
                cat1,
                Category("Pet", color = Red3Light.toArgb(), icon = "pet"),
                Category("Home", color = Green.toArgb(), icon = null),
            ),
        )

        UI(state = state)
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview_NO_FILTER() {
    IvyWalletPreview {
        val acc1 = Account("Cash", color = Green.toArgb())
        val acc2 = Account("DSK", color = GreenDark.toArgb())
        val cat1 = Category("Science", color = Purple1Dark.toArgb(), icon = "atom")
        val state = ReportScreenState(
            baseCurrency = "BGN",
            balance = 0.0,
            income = 0.0,
            expenses = 0.0,
            upcomingIncome = 0.0,
            upcomingExpenses = 0.0,
            overdueIncome = 0.0,
            overdueExpenses = 0.0,

            history = emptyList(),
            upcomingTransactions = emptyList(),
            overdueTransactions = emptyList(),

            upcomingExpanded = true,
            overdueExpanded = true,

            filter = null,
            loading = false,

            accounts = listOf(
                acc1,
                acc2,
                Account("phyre", color = GreenLight.toArgb(), icon = "cash"),
                Account("Revolut", color = IvyDark.toArgb()),
            ),
            categories = listOf(
                cat1,
                Category("Pet", color = Red3Light.toArgb(), icon = "pet"),
                Category("Home", color = Green.toArgb(), icon = null),
            ),
        )

        UI(state = state)
    }
}