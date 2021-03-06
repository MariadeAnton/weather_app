package com.mirhoseini.weatherapp.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mirhoseini.weatherapp.R;
import com.mirhoseini.weatherapp.util.WeatherUtils;

import org.openweathermap.model.Forecast;
import org.openweathermap.model.WeatherForecast;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Mohsen on 30/04/16.
 */
public class ForecastWeatherFragment extends Fragment {
    private static final String ARG_WEATHER_FORECAST = "weather_forecast";
    SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("dd/MM");
    @BindView(R.id.forecast_recycler)
    RecyclerView mForecastRecyclerView;
    private WeatherForecast mWeatherForecast;

    private OnCurrentFragmentInteractionListener mListener;
    private String mCity;

    public ForecastWeatherFragment() {
        // Required empty public constructor
    }

    public static ForecastWeatherFragment newInstance(WeatherForecast weatherForecast) {
        ForecastWeatherFragment fragment = new ForecastWeatherFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_WEATHER_FORECAST, weatherForecast);
        fragment.setArguments(args);
        return fragment;
    }

    @OnClick(R.id.history_button)
    public void history(View view) {
        if (mListener != null) {
            mListener.onLoadHistory(mCity);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mWeatherForecast = (WeatherForecast) getArguments().getSerializable(ARG_WEATHER_FORECAST);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forecast, container, false);
        ButterKnife.bind(this, view);

        // load weather forecast data recyclerView using adapter
        if (mWeatherForecast != null) {
            mCity = mWeatherForecast.getCity().getName();

            ForecastAdapter adapter = new ForecastAdapter(mWeatherForecast);
            mForecastRecyclerView.setAdapter(adapter);
            mForecastRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            mForecastRecyclerView.setItemAnimator(new DefaultItemAnimator());
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);


        if (context instanceof OnCurrentFragmentInteractionListener) {
            mListener = (OnCurrentFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnCurrentFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnCurrentFragmentInteractionListener {
        void onLoadHistory(String city);
    }

    private class ForecastAdapter extends RecyclerView.Adapter<ForecastHolder> {
        WeatherForecast mWeatherForecast;

        public ForecastAdapter(WeatherForecast weatherForecast) {
            mWeatherForecast = weatherForecast;
        }


        @Override
        public ForecastHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.forecast_item, parent, false);
            return new ForecastHolder(view);
        }

        @Override
        public void onBindViewHolder(ForecastHolder holder, int position) {
            Forecast forecast = mWeatherForecast.getList().get(position);

            holder.getDateTextView().setText(mSimpleDateFormat.format(new Date(forecast.getDt() * 1000)));
            holder.getIconImageView().setImageResource(WeatherUtils.convertIconToResource(forecast.getWeather().get(0).getIcon()));
            holder.getTempTextView().setText(forecast.getTemp().getDay() + "°C");
            holder.getWindSpeedTextView().setText(forecast.getSpeed() + "m/s");
        }

        @Override
        public int getItemCount() {
            return mWeatherForecast.getList().size();
        }


    }

    public class ForecastHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.date)
        TextView mDateTextView;
        @BindView(R.id.icon)
        AppCompatImageView mIconImageView;
        @BindView(R.id.temp)
        TextView mTempTextView;
        @BindView(R.id.windspeed)
        TextView mWindSpeedTextView;

        public ForecastHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        public AppCompatImageView getIconImageView() {
            return mIconImageView;
        }

        public TextView getTempTextView() {
            return mTempTextView;
        }

        public TextView getDateTextView() {
            return mDateTextView;
        }

        public TextView getWindSpeedTextView() {
            return mWindSpeedTextView;
        }

    }

}
