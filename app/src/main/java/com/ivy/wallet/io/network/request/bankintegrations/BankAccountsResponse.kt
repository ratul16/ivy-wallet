package com.ivy.wallet.io.network.request.bankintegrations

import com.google.gson.annotations.SerializedName
import com.ivy.wallet.domain.data.bankintegrations.SEAccount

data class BankAccountsResponse(
    @SerializedName("accounts")
    val accounts: List<SEAccount>
)