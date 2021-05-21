package com.ivanovsky.passnotes.injection

import android.content.Context
import androidx.room.Room
import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.data.ObserverBus
import com.ivanovsky.passnotes.data.repository.RemoteFileRepository
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository
import com.ivanovsky.passnotes.data.repository.SettingsRepository
import com.ivanovsky.passnotes.data.repository.UsedFileRepository
import com.ivanovsky.passnotes.data.repository.db.AppDatabase
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseRepository
import com.ivanovsky.passnotes.domain.*
import com.ivanovsky.passnotes.domain.interactor.ErrorInteractor
import com.ivanovsky.passnotes.domain.interactor.debugmenu.DebugMenuInteractor
import com.ivanovsky.passnotes.domain.interactor.filepicker.FilePickerInteractor
import com.ivanovsky.passnotes.domain.interactor.group.GroupInteractor
import com.ivanovsky.passnotes.domain.interactor.groups.GroupsInteractor
import com.ivanovsky.passnotes.domain.interactor.newdb.NewDatabaseInteractor
import com.ivanovsky.passnotes.domain.interactor.note.NoteInteractor
import com.ivanovsky.passnotes.domain.interactor.note_editor.NoteEditorInteractor
import com.ivanovsky.passnotes.domain.interactor.server_login.GetDebugCredentialsUseCase
import com.ivanovsky.passnotes.domain.interactor.server_login.ServerLoginInteractor
import com.ivanovsky.passnotes.domain.interactor.storagelist.StorageListInteractor
import com.ivanovsky.passnotes.domain.interactor.unlock.UnlockInteractor
import com.ivanovsky.passnotes.presentation.debugmenu.DebugMenuViewModel
import com.ivanovsky.passnotes.presentation.filepicker.FilePickerViewModel
import com.ivanovsky.passnotes.presentation.group.GroupViewModel
import com.ivanovsky.passnotes.presentation.groups.GroupsViewModel
import com.ivanovsky.passnotes.presentation.groups.factory.GroupsCellModelFactory
import com.ivanovsky.passnotes.presentation.groups.factory.GroupsCellViewModelFactory
import com.ivanovsky.passnotes.presentation.newdb.NewDatabaseViewModel
import com.ivanovsky.passnotes.presentation.note.NoteViewModel
import com.ivanovsky.passnotes.presentation.note_editor.NoteEditorViewModel
import com.ivanovsky.passnotes.presentation.note_editor.factory.NoteEditorCellModelFactory
import com.ivanovsky.passnotes.presentation.note_editor.factory.NoteEditorCellViewModelFactory
import com.ivanovsky.passnotes.presentation.server_login.ServerLoginArgs
import com.ivanovsky.passnotes.presentation.server_login.ServerLoginViewModel
import com.ivanovsky.passnotes.presentation.storagelist.StorageListViewModel
import com.ivanovsky.passnotes.util.Logger
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object KoinModule {

    val appModule = module {
        single { SettingsRepository(get()) }
        single { provideAppDatabase(get()) }
        single { FileHelper(get(), get()) }
        single { PermissionHelper(get()) }
        single { ResourceProvider(get()) }
        single { ErrorInteractor(get()) }
        single { LocaleProvider(get()) }
        single { DispatcherProvider() }
        single { ObserverBus() }
        single { ClipboardHelper(get()) }
        single { DateFormatProvider(get()) }
        single { NoteDiffer() }
        single { provideOkHttp() }

        single { provideRemoteFileRepository(get()) }
        single { FileSystemResolver(get(), get(), get(), get(), get()) }
        single { FileSyncHelper(get()) }
        single { UsedFileRepository(get(), get()) }
        single { KeepassDatabaseRepository(get(), get()) as EncryptedDatabaseRepository }

        single { GetDebugCredentialsUseCase() }

        single { FilePickerInteractor(get()) }
        single { UnlockInteractor(get(), get(), get()) }
        single { StorageListInteractor(get(), get()) }
        single { NewDatabaseInteractor(get(), get(), get()) }
        single { GroupInteractor(get(), get(), get()) }
        single { DebugMenuInteractor(get(), get(), get(), get()) }
        single { NoteInteractor(get(), get()) }
        single { GroupsInteractor(get(), get()) }
        single { NoteEditorInteractor(get(), get()) }
        single { ServerLoginInteractor(get(), get(), get()) }

        single { GroupsCellModelFactory(get()) }
        single { GroupsCellViewModelFactory() }

        single { NoteEditorCellModelFactory(get()) }
        single { NoteEditorCellViewModelFactory(get()) }

        viewModel { StorageListViewModel(get(), get(), get(), get(), get()) }
        viewModel { FilePickerViewModel(get(), get(), get(), get(), get()) }
        viewModel { NewDatabaseViewModel(get(), get(), get(), get()) }
        viewModel { GroupViewModel(get(), get(), get()) }
        viewModel { DebugMenuViewModel(get(), get(), get(), get()) }
        viewModel { NoteViewModel(get(), get(), get(), get(), get()) }
        viewModel { GroupsViewModel(get(), get(), get(), get(), get(), get()) }
        viewModel { NoteEditorViewModel(get(), get(), get(), get(), get(), get(), get()) }
        viewModel { (args: ServerLoginArgs) -> ServerLoginViewModel(get(), get(), get(), args) }
    }

    private fun provideOkHttp(): OkHttpClient {
        val builder = OkHttpClient.Builder()

        if (BuildConfig.DEBUG) {
            val interceptor = HttpLoggingInterceptor {
                Logger.d("OkHttp", it)
            }.apply {
                setLevel(HttpLoggingInterceptor.Level.BASIC)
            }

            builder.addInterceptor(interceptor)
        }

        return builder.build()
    }

    private fun provideRemoteFileRepository(
        database: AppDatabase
    ): RemoteFileRepository {
        return RemoteFileRepository(database.remoteFileDao)
    }

    private fun provideAppDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.FILE_NAME
        ).build()
    }
}