package com.ivy.wallet.domain.logic

import com.ivy.wallet.domain.data.entity.PlannedPaymentRule
import com.ivy.wallet.domain.data.entity.Transaction
import com.ivy.wallet.io.persistence.dao.TransactionDao
import java.time.LocalDateTime

@Deprecated("Migrate to FP Style")
class PlannedPaymentsGenerator(
    private val transactionDao: TransactionDao
) {
    companion object {
        private const val GENERATED_INSTANCES_LIMIT = 72
    }

    fun generate(rule: PlannedPaymentRule) {
        //delete all not happened transactions
        transactionDao.flagDeletedByRecurringRuleIdAndNoDateTime(
            recurringRuleId = rule.id
        )

        if (rule.oneTime) {
            generateOneTime(rule)
        } else {
            generateRecurring(rule)
        }
    }

    private fun generateOneTime(rule: PlannedPaymentRule) {
        val trns = transactionDao.findAllByRecurringRuleId(recurringRuleId = rule.id)

        if (trns.isEmpty()) {
            generateTransaction(rule, rule.startDate!!)
        }
    }

    private fun generateRecurring(rule: PlannedPaymentRule) {
        val startDate = rule.startDate!!
        val endDate = startDate.plusYears(3)

        val trns = transactionDao.findAllByRecurringRuleId(recurringRuleId = rule.id)
        var trnsToSkip = trns.size

        var generatedTransactions = 0

        var date = startDate
        while (date.isBefore(endDate)) {
            if (generatedTransactions >= GENERATED_INSTANCES_LIMIT) {
                break
            }

            if (trnsToSkip > 0) {
                //skip first N happened transactions
                trnsToSkip--
            } else {
                //generate transaction
                generateTransaction(
                    rule = rule,
                    dueDate = date
                )
                generatedTransactions++
            }

            val intervalN = rule.intervalN!!.toLong()
            date = rule.intervalType!!.incrementDate(
                date = date,
                intervalN = intervalN
            )
        }
    }

    private fun generateTransaction(rule: PlannedPaymentRule, dueDate: LocalDateTime) {
        transactionDao.save(
            Transaction(
                type = rule.type,
                accountId = rule.accountId,
                recurringRuleId = rule.id,
                categoryId = rule.categoryId,
                amount = rule.amount,
                title = rule.title,
                description = rule.description,
                dueDate = dueDate,
                dateTime = null,
                toAccountId = null,

                isSynced = false
            )
        )
    }

}