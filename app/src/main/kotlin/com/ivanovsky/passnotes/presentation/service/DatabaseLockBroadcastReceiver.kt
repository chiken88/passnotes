package com.ivanovsky.passnotes.presentation.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.usecases.DatabaseLockUseCase
import com.ivanovsky.passnotes.injection.GlobalInjector.inject
import com.ivanovsky.passnotes.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DatabaseLockBroadcastReceiver : BroadcastReceiver() {

    private val dispatchers: DispatcherProvider by inject()
    private val errorInteractor: ErrorInteractor by inject()
    private val lockDatabaseUseCase: DatabaseLockUseCase by inject()

    override fun onReceive(context: Context?, intent: Intent?) {
        CoroutineScope(dispatchers.IO).launch {
            Logger.d(TAG, "Closing database")

            val lock = lockDatabaseUseCase.lockIfNeed()
            if (lock.isFailed) {
                val message = errorInteractor.processAndGetMessage(lock.error)
                Logger.d(TAG, "Unable to close database: %s", message)
            }
        }
    }

    companion object {
        private val TAG = DatabaseLockBroadcastReceiver::class.simpleName
    }
}