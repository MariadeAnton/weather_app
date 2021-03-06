package com.mirhoseini.weatherapp.ui.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mirhoseini.appsettings.AppSettings;
import com.mirhoseini.utils.Utils;
import com.mirhoseini.weatherapp.R;
import com.mirhoseini.weatherapp.core.presentation.WeatherPresenter;
import com.mirhoseini.weatherapp.core.service.WeatherApiService;
import com.mirhoseini.weatherapp.core.util.CacheProvider;
import com.mirhoseini.weatherapp.core.util.Constants;
import com.mirhoseini.weatherapp.core.util.SchedulerProvider;
import com.mirhoseini.weatherapp.core.view.MainView;
import com.mirhoseini.weatherapp.di.ApplicationComponent;
import com.mirhoseini.weatherapp.di.WeatherModule;
import com.mirhoseini.weatherapp.ui.fragment.CurrentWeatherFragment;
import com.mirhoseini.weatherapp.ui.fragment.ForecastWeatherFragment;
import com.mirhoseini.weatherapp.ui.fragment.HistoryWeatherFragment;

import org.openweathermap.model.WeatherHistory;
import org.openweathermap.model.WeatherMix;

import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import timber.log.Timber;

/**
 * Created by Mohsen on 30/04/16.
 */
public class MainActivity extends BaseActivity implements MainView, ForecastWeatherFragment.OnCurrentFragmentInteractionListener {

    public static final String TAG_CURRENT_FRAGMENT = "current_fragment";
    public static final String TAG_FORECAST_FRAGMENT = "forecast_fragment";
    public static final int DOUBLE_BACK_PRESSED_DELAY = 2500;
    static boolean doubleBackToExitPressedOnce;


    AlertDialog internetConnectionDialog;

    //injecting dependencies via Dagger
    @Inject
    SchedulerProvider schedulerProvider;
    @Inject
    CacheProvider cacheProvider;
    @Inject
    WeatherApiService weatherApiService;
    @Inject
    WeatherPresenter presenter;

    //injecting views via ButterKnife
    @BindView(R.id.progress_container)
    ViewGroup progressContainer;
    @BindView(R.id.progress_message)
    TextView progressMessage;
    @BindView(R.id.error_container)
    ViewGroup errorContainer;
    @BindView(R.id.city)
    EditText city;
    @BindView(R.id.fragment_container)
    ViewGroup fragmentContainer;
    private CurrentWeatherFragment currentWeatherFragment;
    private ForecastWeatherFragment forecastWeatherFragment;


    @OnEditorAction(R.id.city)
    public boolean onEditorAction(TextView textView, int action, KeyEvent keyEvent) {
        if (action == EditorInfo.IME_ACTION_GO || keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            submit(textView);
        }
        return false;
    }

    @OnClick(R.id.go)
    public void submit(View view) {
        //hide keyboard for better UX
        Utils.hideKeyboard(this, city);

        getWeatherData(city.getText().toString().trim());
    }

    private void getWeatherData(String city) {
        if (city == null || city.isEmpty()) {
            //current user location can be loaded using GPS data for next version
            hideProgress();
        } else {
            //load city weather data
            presenter.loadWeather(city, Utils.isConnected(this));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // binding Views using ButterKnife
        ButterKnife.bind(this);

        Timber.d("Activity Created");


        //load last city from Memory using savedInstanceState or DiskCache using sharedPreferences
        if (savedInstanceState == null) { //load lastCity from sharePreferences
            loadLastLoadedCity();
        } else { //load lastCity from saved state before UI change
            city.setText(savedInstanceState.getString(Constants.KEY_LAST_CITY));
        }
    }

    @Override
    protected void injectDependencies(ApplicationComponent component) {
        component
                .plus(new WeatherModule(this))
                .inject(this);
    }

    @Override
    protected void onResume() {
        Timber.d("Activity Resumed");
        super.onResume();

        FragmentManager fragmentManager = getSupportFragmentManager();
        currentWeatherFragment = (CurrentWeatherFragment) fragmentManager.findFragmentByTag(TAG_CURRENT_FRAGMENT);
        forecastWeatherFragment = (ForecastWeatherFragment) fragmentManager.findFragmentByTag(TAG_FORECAST_FRAGMENT);

        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (currentWeatherFragment == null || forecastWeatherFragment == null) {
            getWeatherData(city.getText().toString().trim());
        } else {
            showFragments();
        }

        doubleBackToExitPressedOnce = false;

        // dismiss no internet connection dialog in case of getting back from setting and connection fixed
        if (internetConnectionDialog != null)
            internetConnectionDialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        Timber.d("Activity Destroyed");

        // call destroy to remove references to objects
        presenter.setView(null);
        presenter = null;

        super.onDestroy();
    }

    @Override
    public void showProgress() {
        Timber.d("Showing Progress");

        progressContainer.setVisibility(View.VISIBLE);
        fragmentContainer.setVisibility(View.INVISIBLE);
        errorContainer.setVisibility(View.INVISIBLE);
    }

    @Override
    public void hideProgress() {
        Timber.d("Hiding Progress");

        progressContainer.setVisibility(View.INVISIBLE);
    }

    @Override
    public void setWeatherValues(WeatherMix weatherMix) {
        Timber.d("Setting Weather: %s", weatherMix.toString());

        saveLastLoadedCity(weatherMix.getWeatherCurrent().getName());
        createFragments(weatherMix);

        showFragments();
    }

    // save user last city
    private void saveLastLoadedCity(String cityName) {
        Timber.d("Saving Last City");

        AppSettings.setValue(this, Constants.KEY_LAST_CITY, cityName);
        city.setText(cityName);
    }

    // save user last city
    private String loadLastLoadedCity() {
        Timber.d("Loading Last City");

        String cityName = AppSettings.getString(this, Constants.KEY_LAST_CITY, Constants.CITY_DEFAULT_VALUE);
        city.setText(cityName);

        return cityName;
    }

    private void createFragments(WeatherMix weatherMix) {
        currentWeatherFragment = CurrentWeatherFragment.newInstance(weatherMix.getWeatherCurrent());
        forecastWeatherFragment = ForecastWeatherFragment.newInstance(weatherMix.getWeatherForecast());
    }

    private void showFragments() {
        fragmentContainer.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.INVISIBLE);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.current_fragment, currentWeatherFragment, TAG_CURRENT_FRAGMENT);
        fragmentTransaction.replace(R.id.forecast_fragment, forecastWeatherFragment, TAG_FORECAST_FRAGMENT);
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void setWeatherHistoryValues(WeatherHistory weatherHistory) {
        Timber.d("Setting Weather History: %s", weatherHistory.toString());

        fragmentContainer.setVisibility(View.VISIBLE);

        HistoryWeatherFragment historyWeatherFragment = HistoryWeatherFragment.newInstance(weatherHistory);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.current_fragment, historyWeatherFragment).commit();
    }

    @Override
    public void showToastMessage(String message) {
        Timber.d("Showing Toast Message: %s", message);

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void updateProgressMessage(String newMessage) {
        Timber.d("Showing Progress Message: %s", newMessage);

        progressMessage.setText(newMessage);
    }

    @Override
    public void showOfflineMessage() {
        Timber.d("Showing Offline Message");

        Snackbar.make(city, R.string.offline_message, Snackbar.LENGTH_LONG)
                .setAction(R.string.go_online, v -> {
                    startActivity(new Intent(
                            Settings.ACTION_WIFI_SETTINGS));
                })
                .setActionTextColor(Color.GREEN)
                .show();

        errorContainer.setVisibility(View.VISIBLE);
    }


    @Override
    public void showExitMessage() {
        Timber.d("Showing Exit Message");

        Toast.makeText(this, R.string.msg_exit, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showConnectionError() {
        Timber.d("Showing Connection Error Message");

        errorContainer.setVisibility(View.VISIBLE);

        if (internetConnectionDialog != null)
            internetConnectionDialog.dismiss();

        internetConnectionDialog = Utils.showNoInternetConnectionDialog(this, false);
    }

    @Override
    public void showRetryMessage() {
        Timber.d("Showing Retry Message");

        errorContainer.setVisibility(View.VISIBLE);

        Snackbar.make(city, R.string.retry_message, Snackbar.LENGTH_LONG)
                .setAction(R.string.load_retry, v -> {
                    getWeatherData(city.getText().toString().trim());
                })
                .setActionTextColor(Color.RED)
                .show();
    }


    @Override
    public void onBackPressed() {
        Timber.d("Activity Back Pressed");

        // check for double back press to exit
        if (doubleBackToExitPressedOnce) {
            Timber.d("Exiting");

            super.onBackPressed();
        } else {

            doubleBackToExitPressedOnce = true;

            showExitMessage();

            Timer t = new Timer();
            t.schedule(new TimerTask() {

                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }

            }, DOUBLE_BACK_PRESSED_DELAY);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Timber.d("Activity Saving Instance State");

        //save TimeSpan selected by user before data loaded and saved to SharedPreferences
        outState.putString(Constants.KEY_LAST_CITY, city.getText().toString().trim());

        super.onSaveInstanceState(outState);
    }


    @Override
    public void onLoadHistory(String city) {
        // load city weather history, call from forecast fragment
        presenter.loadWeatherHistory(city, Utils.isConnected(this));
    }
}