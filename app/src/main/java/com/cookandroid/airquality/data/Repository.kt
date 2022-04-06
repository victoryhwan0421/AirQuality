package com.cookandroid.airquality.data

import com.cookandroid.airquality.BuildConfig
import com.cookandroid.airquality.data.services.KakaoLocalApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

object Repository {

    // 안드로이드에서 경-위도를 받아서 인자로 넘겨줌
    suspend fun getNearbyMonitoringStation(latitude: Double, longitude: Double) {
        // 카카오 로컬 api 호출
        val tmCoordinates = kakaoLocalApiService.getTmCoordinates(longitude, latitude)
            // 응답순서대로 아래 입력
            .body()     // retrofit의 body로, 성공할 경우 값이 있지만, 실패할 경우 nullable
            ?.documents // 배열로 되어 있으므로
            ?.firstOrNull() // 첫번째 값을 가져오되, 값이 없다면 null 반환

        // tm좌표의 X, Y값 받기
        val tmx = tmCoordinates?.x
        val tmY = tmCoordinates?.y
    }

    private val kakaoLocalApiService: KakaoLocalApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Url.KAKAO_API_BASE_URL)    // baseUrl
            .addConverterFactory(GsonConverterFactory.create())  // Gson으로 변환
            .client(buildHttpClient()) // logging 추가 시 필요
            .build()  // 여기까지 API_Service 완성, Retrofit까지만 생성
            .create()
    }

    // logging 목적
    private fun buildHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    // level에 따라 보여지는 logging의 정보가 달라짐
                    // DEBUG 상태에서 확인할 경우에만 보여지도록 설정
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .build()
  }