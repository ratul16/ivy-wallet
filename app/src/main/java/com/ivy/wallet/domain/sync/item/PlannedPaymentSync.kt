package com.ivy.wallet.domain.sync.item

import com.ivy.wallet.domain.sync.uploader.PlannedPaymentRuleUploader
import com.ivy.wallet.io.network.IvySession
import com.ivy.wallet.io.network.RestClient
import com.ivy.wallet.io.persistence.SharedPrefs
import com.ivy.wallet.io.persistence.dao.PlannedPaymentRuleDao
import com.ivy.wallet.utils.timeNowUTC
import com.ivy.wallet.utils.toEpochSeconds

class PlannedPaymentSync(
    private val sharedPrefs: SharedPrefs,
    private val dao: PlannedPaymentRuleDao,
    restClient: RestClient,
    private val uploader: PlannedPaymentRuleUploader,
    private val ivySession: IvySession
) {
    private val service = restClient.plannedPaymentRuleService

    fun isSynced(): Boolean =
        dao.findByIsSyncedAndIsDeleted(synced = false, deleted = false).isEmpty() &&
                dao.findByIsSyncedAndIsDeleted(synced = false, deleted = true).isEmpty()

    suspend fun sync() {
        if (!ivySession.isLoggedIn()) return


        val syncStart = timeNowUTC().toEpochSeconds()

        uploadUpdated()
        deleteDeleted()
        fetchNew()

        sharedPrefs.putLong(SharedPrefs.LAST_SYNC_DATE_PLANNED_PAYMENTS, syncStart)
    }

    private suspend fun uploadUpdated() {
        val toSync = dao.findByIsSyncedAndIsDeleted(
            synced = false,
            deleted = false
        )

        for (item in toSync) {
            uploader.sync(item)
        }
    }

    private suspend fun deleteDeleted() {
        val toDelete = dao.findByIsSyncedAndIsDeleted(
            synced = false,
            deleted = true
        )

        for (item in toDelete) {
            uploader.delete(item.id)
        }
    }

    private suspend fun fetchNew() {
        try {
            val afterTimestamp =
                sharedPrefs.getEpochSeconds(SharedPrefs.LAST_SYNC_DATE_PLANNED_PAYMENTS)

            val response = service.get(after = afterTimestamp)

            response.rules.forEach { item ->
                dao.save(
                    item.copy(
                        isSynced = true,
                        isDeleted = false
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}