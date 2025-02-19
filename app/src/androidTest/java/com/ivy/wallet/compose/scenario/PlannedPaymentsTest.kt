package com.ivy.wallet.compose.scenario

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.ivy.wallet.compose.IvyComposeTest
import com.ivy.wallet.compose.helpers.*
import com.ivy.wallet.domain.data.IntervalType
import com.ivy.wallet.domain.data.TransactionType
import com.ivy.wallet.utils.timeNowUTC
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class PlannedPaymentsTest : IvyComposeTest() {

    private val onboardingFlow = OnboardingFlow(composeTestRule)
    private val transactionFlow = TransactionFlow(composeTestRule)
    private val editPlannedScreen = EditPlannedScreen(composeTestRule)
    private val homeTab = HomeTab(composeTestRule)
    private val mainBottomBar = MainBottomBar(composeTestRule)
    private val deleteConfirmationModal = DeleteConfirmationModal(composeTestRule)
    private val homeMoreMenu = HomeMoreMenu(composeTestRule)
    private val plannedPaymentsScreen = PlannedPaymentsScreen(composeTestRule)

    @Test
    fun Onboard_CreatePlannedPaymentFromPrompt() = testWithRetry {
        onboardingFlow.quickOnboarding()

        transactionFlow.addIncome(
            amount = 100.0
        )

        composeTestRule.onNodeWithTag("cta_prompt_add_planned_payment")
            .performClick()

        editPlannedScreen.addPlannedPayment(
            type = TransactionType.INCOME,
            oneTime = false,
            amount = "2,000",
            category = "Bills & Fees",
            startDate = timeNowUTC().withDayOfMonth(1),
            intervalN = 1,
            intervalType = IntervalType.MONTH,
            title = "Salary"
        )

        homeTab.assertUpcomingIncome(
            amount = "2,000.00",
            currency = "USD"
        )

        homeTab.clickUpcoming()
        homeTab.clickTransaction(
            amount = "2,000.00",
            category = "Bills & Fees",
            title = "Salary"
        )

        editPlannedScreen.clickGet()

        homeTab.assertBalance(
            amount = "2,100",
            amountDecimal = ".00",
            currency = "USD"
        )
    }

    @Test
    fun CreateOneTimePlannedPayment_fromFAB() = testWithRetry {
        onboardingFlow.quickOnboarding()

        //Add one transaction so the "Adjust Balance" prompt can disappear and the screen to be scrollable
        transactionFlow.addIncome(
            amount = 100.0,
            title = "Adjust Balance"
        )

        homeTab.dismissPrompt() //Dismiss "Add Planned Payment" prompt

        mainBottomBar.clickAddFAB()
        mainBottomBar.clickAddPlannedPayment()
        editPlannedScreen.addPlannedPayment(
            type = TransactionType.EXPENSE,
            oneTime = true,
            amount = "530.25",
            category = "Transport",
            startDate = null,
            intervalN = null,
            intervalType = null,
            title = "Netherlands airplane"
        )

        homeTab.assertBalance(
            amount = "100",
            amountDecimal = ".00"
        )

        homeTab.assertUpcomingExpense(
            amount = "530.25",
            currency = "USD"
        )

        homeTab.clickUpcoming()
        homeTab.clickTransactionPay()

        composeTestRule.waitForIdle()

        homeTab.assertBalance(
            amount = "-430", //-530.25 + 100.00 = -430.25
            amountDecimal = ".25"
        )
    }

    @Test
    fun DeletePlannedPayment_Instance() = testWithRetry {
        onboardingFlow.quickOnboarding()
        mainBottomBar.clickAddFAB()
        mainBottomBar.clickAddPlannedPayment()

        editPlannedScreen.addPlannedPayment(
            type = TransactionType.EXPENSE,
            oneTime = false,
            amount = "650",
            category = "Bills & Fees",
            startDate = null,
            intervalN = 1,
            intervalType = IntervalType.MONTH,
            title = "Rent"
        )

        homeTab.assertUpcomingExpense(
            amount = "650.00",
            currency = "USD"
        )

        homeTab.clickUpcoming()
        homeTab.clickTransaction(
            amount = "650.00",
            category = "Bills & Fees",
            title = "Rent"
        )

        editPlannedScreen.clickDelete()
        deleteConfirmationModal.confirmDelete()

        homeTab.assertBalance(
            amount = "0",
            amountDecimal = ".00"
        )

        homeTab.assertUpcomingDoesNotExist()

        //Assert that the root planned payment exists
        homeMoreMenu.clickOpenCloseArrow()
        homeMoreMenu.clickPlannedPayments()

        plannedPaymentsScreen.clickPlannedPayment(
            amount = "650.00"
        )
    }

    @Test
    fun DeletePlannedPayment_Root() = testWithRetry {
        onboardingFlow.quickOnboarding()

        homeMoreMenu.clickOpenCloseArrow()
        homeMoreMenu.clickPlannedPayments()

        plannedPaymentsScreen.clickAddPlannedPayment()

        editPlannedScreen.addPlannedPayment(
            type = TransactionType.EXPENSE,
            oneTime = false,
            amount = "650",
            category = "Bills & Fees",
            startDate = null,
            intervalN = 1,
            intervalType = IntervalType.MONTH,
            title = "Rent"
        )

        plannedPaymentsScreen.assertUpcomingExpense(
            amount = "650.00",
            currency = "USD"
        )


        plannedPaymentsScreen.clickPlannedPayment(
            amount = "650.00"
        )

        editPlannedScreen.clickDelete()
        deleteConfirmationModal.confirmDelete()

        plannedPaymentsScreen.assertPlannedPaymentDoesNotExist(
            amount = "650.00"
        )

        plannedPaymentsScreen.clickClose()

        homeTab.assertBalance(
            amount = "0",
            amountDecimal = ".00"
        )
        homeTab.assertUpcomingDoesNotExist()
    }

    @Test
    fun AddSeveralPlannedPayments() = testWithRetry {
        onboardingFlow.quickOnboarding()

        //Add recurring INCOME
        mainBottomBar.clickAddFAB()
        mainBottomBar.clickAddPlannedPayment()
        editPlannedScreen.addPlannedPayment(
            type = TransactionType.INCOME,
            oneTime = false,
            amount = "5,250.33",
            category = "Bills & Fees",
            startDate = timeNowUTC().withDayOfMonth(1),
            intervalN = 31,
            intervalType = IntervalType.DAY,
            title = "Salary"
        )

        homeTab.assertUpcomingIncome(
            amount = "5,250.33",
            currency = "USD"
        )

        //Add recurring EXPENSE
        mainBottomBar.clickAddFAB()
        mainBottomBar.clickAddPlannedPayment()
        editPlannedScreen.addPlannedPayment(
            type = TransactionType.EXPENSE,
            oneTime = false,
            amount = "12.99",
            category = "Entertainment",
            startDate = timeNowUTC().withDayOfMonth(1),
            intervalN = 1,
            intervalType = IntervalType.MONTH,
            title = "Netflix"
        )

        homeTab.assertUpcomingExpense(
            amount = "12.99",
            currency = "USD"
        )

        //Add one-time EXPENSE
        mainBottomBar.clickAddFAB()
        mainBottomBar.clickAddPlannedPayment()
        editPlannedScreen.addPlannedPayment(
            type = TransactionType.EXPENSE,
            oneTime = true,
            amount = "2,000",
            category = "Transport",
            startDate = timeNowUTC().withDayOfMonth(1),
            intervalN = null,
            intervalType = null,
            title = "Vacation"
        )

        homeTab.assertUpcomingExpense(
            amount = "2,012.99", //2,000 + 12.99
            currency = "USD"
        )
        homeTab.assertUpcomingIncome(
            amount = "5,250.33",
            currency = "USD"
        )
        homeTab.assertBalance(
            amount = "0",
            amountDecimal = ".00"
        )
    }
}