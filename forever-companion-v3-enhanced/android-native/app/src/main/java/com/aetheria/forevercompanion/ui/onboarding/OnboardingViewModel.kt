package com.aetheria.forevercompanion.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aetheria.forevercompanion.data.local.dao.BondProgressDao
import com.aetheria.forevercompanion.data.local.dao.CompanionDao
import com.aetheria.forevercompanion.data.local.dao.CurrencyDao
import com.aetheria.forevercompanion.data.local.entities.BondProgressEntity
import com.aetheria.forevercompanion.data.local.entities.CompanionEntity
import com.aetheria.forevercompanion.data.local.entities.CurrencyBalanceEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val companionDao: CompanionDao,
    private val bondProgressDao: BondProgressDao,
    private val currencyDao: CurrencyDao
) : ViewModel() {

    fun createCompanion(name: String) {
        viewModelScope.launch {
            val companion = CompanionEntity(name = name)
            companionDao.insert(companion)
            bondProgressDao.insert(BondProgressEntity(companionId = companion.id))
            currencyDao.upsertBalance(CurrencyBalanceEntity())
        }
    }
}
