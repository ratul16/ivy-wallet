package com.ivy.wallet.domain.logic

import com.ivy.wallet.domain.data.entity.Transaction
import com.ivy.wallet.io.persistence.dao.TransactionDao
import com.ivy.wallet.ui.edit.core.SUGGESTIONS_LIMIT
import com.ivy.wallet.utils.capitalizeWords
import com.ivy.wallet.utils.isNotNullOrBlank
import java.util.*

@Deprecated("Migrate to FP Style")
class SmartTitleSuggestionsLogic(
    private val transactionDao: TransactionDao
) {

    /**
     * Suggests titles based on:
     * - title match
     * - most used titles for categories
     * - if suggestions.size < SUGGESTIONS_LIMIT most used titles for accounts
     */
    fun suggest(
        title: String?,
        categoryId: UUID?,
        accountId: UUID?
    ): Set<String> {
        val suggestions = mutableSetOf<String>()

        if (title != null && title.isNotEmpty()) {
            //suggest by title
            val suggestionsByTitle = transactionDao.findAllByTitleMatchingPattern("${title}%")
                .extractUniqueTitles()
                .sortedByMostUsedFirst {
                    transactionDao.countByTitleMatchingPattern("${it}%")
                }

            suggestions.addAll(suggestionsByTitle)
        }

        if (categoryId != null) {
            //suggest by category
            //all titles used for the specific category
            //ordered by N times used

            val suggestionsByCategory = transactionDao
                .findAllByCategory(
                    categoryId = categoryId
                )
                //exclude already suggested suggestions so they're ordered by priority at the end
                .extractUniqueTitles(excludeSuggestions = suggestions)
                .sortedByMostUsedFirst {
                    transactionDao.countByTitleMatchingPatternAndCategoryId(
                        pattern = it,
                        categoryId = categoryId
                    )
                }

            suggestions.addAll(suggestionsByCategory)
        }


        if (suggestions.size < SUGGESTIONS_LIMIT && accountId != null) {
            //last resort, suggest by account
            //all titles used for the specific account
            //ordered by N times used

            val suggestionsByAccount = transactionDao
                .findAllByAccount(
                    accountId = accountId
                )
                //exclude already suggested suggestions so they're ordered by priority at the end
                .extractUniqueTitles(excludeSuggestions = suggestions)
                .sortedByMostUsedFirst {
                    transactionDao.countByTitleMatchingPatternAndAccountId(
                        pattern = it,
                        accountId = accountId
                    )
                }

            suggestions.addAll(suggestionsByAccount)
        }

        return suggestions
            .filter { it != title }
            .toSet()
    }
}

private fun List<Transaction>.extractUniqueTitles(
    excludeSuggestions: Set<String>? = null
): Set<String> {
    return this
        .filter { it.title.isNotNullOrBlank() }
        .map { it.title!!.trim().capitalizeWords() }
        .filter { excludeSuggestions == null || !excludeSuggestions.contains(it) }
        .toSet()
}

private fun Set<String>.sortedByMostUsedFirst(countUses: (String) -> Long): Set<String> {
    val titleCountMap = this
        .map {
            it to countUses(it)
        }
        .toMap()

    val sortedSuggestions = this
        .sortedByDescending {
            titleCountMap.getOrDefault(it, 0)
        }
        .toSet()

    return sortedSuggestions
}