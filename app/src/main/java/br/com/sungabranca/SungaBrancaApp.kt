package br.com.sungabranca

import android.app.Application
import androidx.room.Room
import br.com.sungabranca.data.AppDatabase
import br.com.sungabranca.data.CatalogSeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SungaBrancaApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "sunga_branca.db").build()
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            val dao = database.exerciseDao()
            if (dao.countCatalog() == 0) {
                dao.insertAll(CatalogSeed.exercises)
            }
        }
    }
}