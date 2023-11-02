package kr.co.telecons.mconhudsdk

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object MConHudRetrofitClient {
    private var instance: Retrofit? = null
    private val gson = GsonBuilder().setLenient().create()

    private fun getInstance(): Retrofit {
        if (instance == null) {
            val clientBuilder = OkHttpClient.Builder()
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            clientBuilder.addInterceptor(loggingInterceptor)
            clientBuilder.connectTimeout(1, TimeUnit.MINUTES)
            clientBuilder.readTimeout(30, TimeUnit.SECONDS)
            clientBuilder.writeTimeout(15, TimeUnit.SECONDS)


            instance = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }

        return instance!!
    }

    fun service() : MConHudRetrofitAPI {
        return getInstance().create(MConHudRetrofitAPI::class.java)
    }
}