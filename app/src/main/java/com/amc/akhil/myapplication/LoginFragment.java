package com.amc.akhil.myapplication;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;

import javax.xml.validation.Validator;

/**
 * Created by Akhil on 20/11/2017.
 */
@EFragment
public class LoginFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        Validator.ValidationListener{

    @AfterViews
    void afterLoad() {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
