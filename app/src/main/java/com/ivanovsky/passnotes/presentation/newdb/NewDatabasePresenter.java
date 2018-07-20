package com.ivanovsky.passnotes.presentation.newdb;

import android.content.Context;

import com.ivanovsky.passnotes.App;
import com.ivanovsky.passnotes.R;
import com.ivanovsky.passnotes.data.entity.FileDescriptor;
import com.ivanovsky.passnotes.data.repository.EncryptedDatabaseRepository;
import com.ivanovsky.passnotes.data.entity.DatabaseDescriptor;
import com.ivanovsky.passnotes.data.ObserverBus;
import com.ivanovsky.passnotes.data.entity.UsedFile;
import com.ivanovsky.passnotes.data.repository.keepass.KeepassDatabaseKey;
import com.ivanovsky.passnotes.data.repository.UsedFileRepository;
import com.ivanovsky.passnotes.injection.Injector;
import com.ivanovsky.passnotes.presentation.core.FragmentState;
import com.ivanovsky.passnotes.presentation.newdb.NewDatabaseContract.Presenter;
import com.ivanovsky.passnotes.util.FileUtils;

import java.io.File;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class NewDatabasePresenter implements Presenter {

	@Inject
	EncryptedDatabaseRepository encryptedDbProvider;

	@Inject
	UsedFileRepository usedFileRepository;

	@Inject
	ObserverBus observerBus;

	private final NewDatabaseContract.View view;
	private final Context context;
	private final CompositeDisposable disposables;

	NewDatabasePresenter(NewDatabaseContract.View view, Context context) {
		Injector.getInstance().getAppComponent().inject(this);
		this.view = view;
		this.context = context;
		this.disposables = new CompositeDisposable();
	}

	@Override
	public void start() {
		view.setState(FragmentState.DISPLAYING_DATA);
	}

	@Override
	public void stop() {
		disposables.clear();
	}

	@Override
	public void createNewDatabaseFile(String filename, String password) {
		view.setDoneButtonVisible(false);

		File dir = FileUtils.getKeepassDir(context);
		if (dir != null) {
			File dbFile = new File(dir, filename + ".kdbx");//TODO: fix db name creation

			Disposable disposable = Observable.fromCallable(() -> createNewDatabase(dbFile, password))
					.subscribeOn(Schedulers.newThread())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(created -> onNewDatabaseCreated(created, dbFile, password));
			disposables.add(disposable);

			view.setState(FragmentState.LOADING);
		} else {
			view.showError(context.getString(R.string.error_was_occurred));
		}
	}

	private Boolean createNewDatabase(File dbFile, String password) {
		boolean result = false;

		KeepassDatabaseKey key = new KeepassDatabaseKey(password);

		if (!dbFile.exists()
				&& encryptedDbProvider.createNew(key, FileDescriptor.newRegularFile(dbFile))) {

			UsedFile usedFile = new UsedFile();
			usedFile.setFilePath(dbFile.getPath());
			usedFile.setLastAccessTime(System.currentTimeMillis());
			usedFileRepository.insert(usedFile);

			//TODO: move observerBus call to repository
			observerBus.notifyUsedFileDataSetChanged();

			result = true;
		}

		return result;
	}

	private void onNewDatabaseCreated(Boolean created, File dbFile, String password) {
		if (created) {
			view.showGroupsScreen(new DatabaseDescriptor(password, dbFile));
		} else {
			view.showError(context.getString(R.string.error_was_occurred));
			view.setDoneButtonVisible(true);
		}
	}
}