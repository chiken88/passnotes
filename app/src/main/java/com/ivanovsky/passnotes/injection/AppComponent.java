package com.ivanovsky.passnotes.injection;

import com.ivanovsky.passnotes.ui.StartActivity;
import com.ivanovsky.passnotes.ui.newdb.NewDatabaseActivity;
import com.ivanovsky.passnotes.ui.recentlyused.RecentlyUsedPresenter;

import javax.inject.Singleton;

import dagger.Component;

@Component(modules = { AppModule.class })
@Singleton
public interface AppComponent {

	void inject(StartActivity startActivity);
	void inject(RecentlyUsedPresenter recentlyUsedPresenter);
	void inject(NewDatabaseActivity newDatabaseActivity);
}