package com.ivy.wallet.domain.action

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class Action<in I, out O> {
    abstract suspend fun I.willDo(): O

    suspend operator fun invoke(input: I): O {
        return input.willDo()
    }

    protected suspend fun <T> io(action: suspend () -> T): T = withContext(Dispatchers.IO) {
        return@withContext action()
    }

    protected suspend fun <T> computation(action: suspend () -> T): T =
        withContext(Dispatchers.Main) {
            return@withContext action()
        }
}

infix fun <A, B, C> Action<B, C>.after(act1: Action<A, B>): Action<A, C> = object : Action<A, C>() {
    override suspend fun A.willDo(): C {
        val b = act1(this@willDo) //A -> B
        return this@after(b) //B -> C
    }
}

///**
// * Action composition example
// */
//suspend fun example(
//    calcWalletBalance: CalcWalletBalanceAct,
//    getBaseCurrency: GetBaseCurrencyAct
//): BigDecimal {
//    return (calcWalletBalance after getBaseCurrency)(Unit)
//}