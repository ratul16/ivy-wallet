package com.ivy.wallet.domain.logic

import com.ivy.wallet.domain.data.entity.Budget
import com.ivy.wallet.domain.logic.model.CreateBudgetData
import com.ivy.wallet.domain.sync.uploader.BudgetUploader
import com.ivy.wallet.io.persistence.dao.BudgetDao
import com.ivy.wallet.utils.ioThread

class BudgetCreator(
    private val paywallLogic: PaywallLogic,
    private val budgetDao: BudgetDao,
    private val budgetUploader: BudgetUploader
) {
    suspend fun createBudget(
        data: CreateBudgetData,
        onRefreshUI: suspend (Budget) -> Unit
    ) {
        val name = data.name
        if (name.isBlank()) return
        if (data.amount <= 0) return

        try {
            paywallLogic.protectAddWithPaywall(
                addBudget = true,
            ) {
                val newBudget = ioThread {
                    val budget = Budget(
                        name = name.trim(),
                        amount = data.amount,
                        categoryIdsSerialized = data.categoryIdsSerialized,
                        accountIdsSerialized = data.accountIdsSerialized,
                        orderId = budgetDao.findMaxOrderNum() + 1,
                        isSynced = false
                    )

                    budgetDao.save(budget)
                    budget
                }

                onRefreshUI(newBudget)

                ioThread {
                    budgetUploader.sync(newBudget)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    suspend fun editBudget(
        updatedBudget: Budget,
        onRefreshUI: suspend (Budget) -> Unit
    ) {
        if (updatedBudget.name.isBlank()) return
        if (updatedBudget.amount <= 0.0) return

        try {
            ioThread {
                budgetDao.save(
                    updatedBudget.copy(
                        isSynced = false
                    )
                )
            }

            onRefreshUI(updatedBudget)

            ioThread {
                budgetUploader.sync(updatedBudget)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteBudget(
        budget: Budget,
        onRefreshUI: suspend () -> Unit
    ) {
        try {
            ioThread {
                budgetDao.flagDeleted(budget.id)
            }

            onRefreshUI()

            ioThread {
                budgetUploader.delete(budget.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}