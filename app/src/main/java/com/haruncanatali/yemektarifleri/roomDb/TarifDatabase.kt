package com.haruncanatali.yemektarifleri.roomDb

import androidx.room.Database
import androidx.room.RoomDatabase
import com.haruncanatali.yemektarifleri.models.Tarif

@Database(entities = [Tarif::class], version = 1)
abstract class TarifDatabase : RoomDatabase() {
    abstract fun tarifDao() : TarifDAO
}