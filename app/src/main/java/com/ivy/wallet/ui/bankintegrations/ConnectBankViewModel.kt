package com.ivy.wallet.ui.bankintegrations

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivy.wallet.domain.logic.bankintegrations.BankIntegrationsLogic
import com.ivy.wallet.domain.sync.IvySync
import com.ivy.wallet.io.network.IvySession
import com.ivy.wallet.io.persistence.SharedPrefs
import com.ivy.wallet.io.persistence.dao.UserDao
import com.ivy.wallet.ui.RootActivity
import com.ivy.wallet.utils.OpResult
import com.ivy.wallet.utils.TestIdlingResource
import com.ivy.wallet.utils.asLiveData
import com.ivy.wallet.utils.ioThread
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectBankViewModel @Inject constructor(
    private val bankIntegrationsLogic: BankIntegrationsLogic,
    private val ivySession: IvySession,
    private val userDao: UserDao,
    private val ivySync: IvySync,
    private val sharedPrefs: SharedPrefs
) : ViewModel() {

    private val _opSyncTransactions = MutableLiveData<OpResult<Unit>>()
    val opSyncTransactions = _opSyncTransactions.asLiveData()

    private val _bankSyncEnabled = MutableLiveData(false)
    val bankSyncEnabled = _bankSyncEnabled.asLiveData()

    fun start() {
        _bankSyncEnabled.value = sharedPrefs.getBoolean(SharedPrefs.ENABLE_BANK_SYNC, false)
    }

    fun connectBank(rootActivity: RootActivity) {
        viewModelScope.launch {
            TestIdlingResource.increment()

            try {
                val user = ivySession.getUserIdSafe()?.let {
                    ioThread { userDao.findById(it) }
                }

                if (user != null) {
                    bankIntegrationsLogic.connect(rootActivity)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            TestIdlingResource.decrement()
        }
    }

    fun syncTransactions() {
        viewModelScope.launch {
            TestIdlingResource.increment()

            _opSyncTransactions.value = OpResult.loading()

            try {
                bankIntegrationsLogic.sync()
                ioThread {
                    ivySync.sync()
                }

                _opSyncTransactions.value = OpResult.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                _opSyncTransactions.value = OpResult.failure(e)
            }

            TestIdlingResource.decrement()
        }
    }

    fun setBankSyncEnabled(enabled: Boolean) {
        sharedPrefs.putBoolean(SharedPrefs.ENABLE_BANK_SYNC, enabled)
        _bankSyncEnabled.value = enabled
    }

    fun removeCustomer() {
        viewModelScope.launch {
            TestIdlingResource.increment()
            bankIntegrationsLogic.removeCustomer()
            TestIdlingResource.decrement()
        }
    }
}