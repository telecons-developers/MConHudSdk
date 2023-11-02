package kr.co.telecons.mconhudsdk

import kr.co.telecons.mconhudsdk.models.Auth
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface MConHudRetrofitAPI {
    @GET("api/auth")
    fun authAppKey(@Query("appKey") appKey: String) : Call<Auth>
}