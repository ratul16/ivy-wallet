package com.ivy.wallet.domain.logic.currency

import com.ivy.wallet.domain.data.entity.Account
import com.ivy.wallet.domain.data.entity.ExchangeRate
import com.ivy.wallet.domain.data.entity.PlannedPaymentRule
import com.ivy.wallet.domain.data.entity.Transaction
import com.ivy.wallet.io.network.RestClient
import com.ivy.wallet.io.network.service.CoinbaseService
import com.ivy.wallet.io.persistence.dao.AccountDao
import com.ivy.wallet.io.persistence.dao.ExchangeRateDao
import com.ivy.wallet.io.persistence.dao.SettingsDao
import com.ivy.wallet.utils.sendToCrashlytics
import java.util.*

class ExchangeRatesLogic(
    restClient: RestClient,
    private val exchangeRateDao: ExchangeRateDao
) {
    private val coinbaseService = restClient.coinbaseService

    suspend fun sync(
        baseCurrency: String
    ) {
        try {
            if (baseCurrency.isBlank()) return

            val response = coinbaseService.getExchangeRates(
                url = CoinbaseService.exchangeRatesUrl(
                    baseCurrencyCode = baseCurrency
                )
            )

            response.data.rates.forEach { (currency, rate) ->
                exchangeRateDao.save(
                    ExchangeRate(
                        baseCurrency = baseCurrency,
                        currency = currency,
                        rate = rate
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            e.sendToCrashlytics("Failed to sync exchange rates")
        }
    }

    fun amountBaseCurrency(
        plannedPayment: PlannedPaymentRule,
        baseCurrency: String,
        accounts: List<Account> //helper
    ): Double {
        return amountBaseCurrency(
            amount = plannedPayment.amount,
            accountId = plannedPayment.accountId,
            baseCurrency = baseCurrency,
            accounts = accounts
        )
    }

    fun amountBaseCurrency(
        transaction: Transaction,
        baseCurrency: String,
        accounts: List<Account> //helper
    ): Double {
        return amountBaseCurrency(
            amount = transaction.amount,
            accountId = transaction.accountId,
            baseCurrency = baseCurrency,
            accounts = accounts
        )
    }

    fun toAmountBaseCurrency(
        transaction: Transaction,
        baseCurrency: String,
        accounts: List<Account> //helper
    ): Double {
        val amount = transaction.toAmount ?: transaction.amount
        val toCurrency = accounts.find { it.id == transaction.toAccountId }?.currency
            ?: return amount // no conversion

        return amountBaseCurrency(
            amount = amount,
            amountCurrency = toCurrency,
            baseCurrency = baseCurrency
        )
    }

    private fun amountBaseCurrency(
        amount: Double,
        accountId: UUID,
        baseCurrency: String,
        accounts: List<Account> //helper
    ): Double {
        val trnCurrency = accounts.find { it.id == accountId }?.currency
            ?: return amount //no conversion

        return amountBaseCurrency(
            amount = amount,
            amountCurrency = trnCurrency,
            baseCurrency = baseCurrency
        )
    }

    fun amountBaseCurrency(
        amount: Double,
        amountCurrency: String,
        baseCurrency: String
    ): Double {
        return if (amountCurrency != baseCurrency) {
            //convert to base currency
            amount / exchangeRate(baseCurrency = baseCurrency, currency = amountCurrency)
        } else {
            //no conversion needed, return amount
            amount
        }
    }

    fun convertAmount(
        baseCurrency: String,
        amount: Double,
        fromCurrency: String,
        toCurrency: String
    ): Double {
        if (fromCurrency == toCurrency) return amount

        val amountBaseCurrency =
            amount / exchangeRate(baseCurrency = baseCurrency, currency = fromCurrency)
        return amountBaseCurrency * exchangeRate(baseCurrency = baseCurrency, currency = toCurrency)
    }

    /**
     * base = BGN, currency = EUR => rate = 0.51
     */
    private fun exchangeRate(
        baseCurrency: String,
        currency: String
    ): Double {
        val rate = exchangeRateDao.findByBaseCurrencyAndCurrency(
            baseCurrency = baseCurrency,
            currency = currency
        )?.rate ?: return 1.0
        if (rate <= 0) {
            return 1.0
        }
        return rate
    }
}

fun Iterable<Transaction>.sumInBaseCurrency(
    exchangeRatesLogic: ExchangeRatesLogic,
    settingsDao: SettingsDao,
    accountDao: AccountDao,
): Double {
    val baseCurrency = settingsDao.findFirst().currency
    val accounts = accountDao.findAll()

    return sumOf {
        exchangeRatesLogic.amountBaseCurrency(
            transaction = it,
            baseCurrency = baseCurrency,
            accounts = accounts
        )
    }
}

fun Iterable<PlannedPaymentRule>.sumByDoublePlannedInBaseCurrency(
    exchangeRatesLogic: ExchangeRatesLogic,
    settingsDao: SettingsDao,
    accountDao: AccountDao,
): Double {
    val baseCurrency = settingsDao.findFirst().currency
    val accounts = accountDao.findAll()

    return sumOf {
        exchangeRatesLogic.amountBaseCurrency(
            plannedPayment = it,
            baseCurrency = baseCurrency,
            accounts = accounts
        )
    }
}