package com.aetheria.forevercompanion.data.local.dao

import androidx.room.*
import com.aetheria.forevercompanion.data.local.entities.CurrencyBalanceEntity
import com.aetheria.forevercompanion.data.local.entities.CurrencyTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM currency_balance WHERE userId = 'local_user'")
    fun getBalance(): Flow<CurrencyBalanceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBalance(balance: CurrencyBalanceEntity)

    @Query("UPDATE currency_balance SET aetherShards = aetherShards + :amount, lifetimeEarnedShards = lifetimeEarnedShards + :amount, lastUpdated = :time WHERE userId = 'local_user'")
    suspend fun addShards(amount: Int, time: Long = System.currentTimeMillis())

    @Query("UPDATE currency_balance SET aetherShards = aetherShards - :amount, lifetimeSpentShards = lifetimeSpentShards + :amount, lastUpdated = :time WHERE userId = 'local_user' AND aetherShards >= :amount")
    suspend fun spendShards(amount: Int, time: Long = System.currentTimeMillis()): Int

    @Insert
    suspend fun insertTransaction(tx: CurrencyTransactionEntity)

    @Query("SELECT * FROM currency_transactions ORDER BY timestamp DESC LIMIT :limit")
    fun getTransactions(limit: Int = 20): Flow<List<CurrencyTransactionEntity>>
}
