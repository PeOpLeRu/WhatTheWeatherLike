package com.example.whattheweatherlike

import com.example.whattheweatherlike.databinding.ActivityMainBinding
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.*
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var weatherAadapter: WeatherRecycleAdapter

    lateinit var API_KEY: String

    private var cities = mutableListOf<String>()
    private var weatherInfoList = mutableListOf<Weather>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setContentView(binding.root)

        this.API_KEY = resources.getString(R.string.API_KEY)

        weatherAadapter = WeatherRecycleAdapter(this.weatherInfoList as ArrayList<Weather>)
        binding.weatherList.apply {
            layoutManager =
                LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
            adapter = weatherAadapter
        }

        binding.btnAddCity.setOnClickListener {
            if (addCity(binding.inputCityName.text.toString())) {
                this.getWeatherForCity(binding.inputCityName.text.toString())
                binding.inputCityName.setText("")
            }
        }

        binding.btnUpdateTemp.setOnClickListener {
            this.updateWeatherForAllCities()
        }
    }

    private fun addCity(text: String): Boolean {
        return if (text == "") {
            Toast.makeText(this, "The name of the city is empty", Toast.LENGTH_SHORT).show()
            false
        } else {
            this.cities.add(text)
            true
        }
    }

    private fun requestWeatherForCity(nameCity: String): Weather {
        val weatherURL =
            "https://api.openweathermap.org/data/2.5/weather?q=${nameCity}&appid=${this.API_KEY}&units=metric";
        var data = ""
        val weather = Weather()
        weather.city = nameCity

        lateinit var stream: InputStream

        try {
            stream = URL(weatherURL).getContent() as InputStream
        } catch (e: IOException) {
            weather.city = "Error Internet connection!"
        }

        try {
            data = Scanner(stream).nextLine() ?: ""

            val jsonObj = JSONObject(data)

            weather.temp = jsonObj.getJSONObject("main").getString("temp").let { "$it°" }
            weather.humidity = jsonObj.getJSONObject("main").getString("humidity").let { "$it%" }
        } catch (e: Exception) {
            if (weather.city == "") {
                weather.city = "Ошибка запроса! (проверьте название города)"
            }
        }

        return weather
    }

    private fun updateWeatherForAllCities() {
        GlobalScope.launch(Dispatchers.IO) {
            weatherInfoList.clear()

            cities.forEach { it -> getWeatherForCity(it) }
        }
    }

    private fun getWeatherForCity(nameCity: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val resWeather = requestWeatherForCity(nameCity)
            weatherInfoList.add(resWeather)
            withContext(Dispatchers.Main) {
                weatherAadapter.updateData(ArrayList<Weather>(weatherInfoList))
            }
        }
    }
}