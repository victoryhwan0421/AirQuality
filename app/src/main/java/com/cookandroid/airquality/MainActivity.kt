package com.cookandroid.airquality

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import com.cookandroid.airquality.data.Repository
import com.cookandroid.airquality.data.models.airquality.Grade
import com.cookandroid.airquality.data.models.airquality.MeasuredValue
import com.cookandroid.airquality.data.models.monitoringstations.MonitoringStation
import com.cookandroid.airquality.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var cancellationTokenSource: CancellationTokenSource? = null

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val scope = MainScope()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        bindViews()
        initVariables()
        requestLocationPermissions()

    }


    override fun onDestroy() {
        super.onDestroy()
        cancellationTokenSource?.cancel()
        scope.cancel()
    }


    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val locationPermissionGranted =
            requestCode == REQUEST_ACCESS_LOCATION_PERMISSIONS &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED

        if(!locationPermissionGranted){
            finish()
        } else {
            // fetchData
            fetchAirQualityData()
        }
    }

    // 스와프하여 새로고침
    private  fun bindViews() {
        binding.refresh.setOnRefreshListener {
            fetchAirQualityData()
        }
    }


    private fun initVariables(){
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }


    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            REQUEST_ACCESS_LOCATION_PERMISSIONS

        )
    }

    @SuppressLint("MissingPermission")
    private fun fetchAirQualityData() {
        cancellationTokenSource = CancellationTokenSource()

        fusedLocationProviderClient.getCurrentLocation(
            LocationRequest.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource!!.token
        ).addOnSuccessListener { location ->
            scope.launch {
                // 시작하고 errorDescription 발생 후 재시도했을 경우 에러화면을 제거해야하므로
                binding.errorDescriptionTextView.visibility =View.GONE
                try {
                    val monitoringStation =
                        Repository.getNearbyMonitoringStation(location.latitude, location.longitude)

                    // 관측소를 잘 불러오는지 확인!
                    // Optinal이므로 monitoringStation?. 로 safe_call 로 부르기
                    // binding.textView.text = monitoringStation?.stationName
                    val measuredValue =
                        Repository.getLatestAirQualityData(monitoringStation!!.stationName!!)

                    //binding.textView.text = measuredValue.toString()
                    displayAirQualityData(monitoringStation, measuredValue!!)
                } catch (exception: Exception) {
                    // 문제가 발생했다면 errorDescriptionTextView 보이기
                    binding.errorDescriptionTextView.visibility =View.VISIBLE
                    // 정상적으로 로딩되었다가 재시도할 때 Exception 이 발생하여
                    // errorDescriptionTextView 를 중복으로 보여주는 것을 방지하기 위해 alpha값을 0으로 설정
                    binding.contentsLayout.alpha = 0F

                } finally {
                    // 모든 처리가 잘 이루어졌다면, 로딩창과, 새로고침 마무리
                    binding.progressBar.visibility = View.GONE
                    binding.refresh.isRefreshing = false
                }

            }
           // binding.textView.text = "${location.latitude}, ${location.longitude}"
        }
    }

    @SuppressLint("SetTextI18n")
    fun displayAirQualityData(monitoringStation: MonitoringStation, measuredValue: MeasuredValue) {
        binding.contentsLayout.animate()
            // alpha값을 default로 설정되어 있는 시간에 걸쳐 1로 변환(fade-in효과)
            .alpha(1F)
            .start()

        binding.measuringStationNameTextView.text = monitoringStation.stationName
        binding.measuringStationAddressTextView.text = monitoringStation.addr

        // measuredValue 의 통합지수가 null 일 경우 UNKNOWN 으로 예외처리하고
        // 그 외에는 정상 출력
        (measuredValue.khaiGrade ?: Grade.UNKNOWN).let { grade ->
            binding.root.setBackgroundResource(grade.colorsResId)
            binding.totalGradeLabelTextView.text = grade.label
            binding.totalGradeEmojiTextView.text = grade.emoji
        }

        // 미세먼지, 초미세먼지 랜더링
        // 계속해서 각각의 항목들 보여주기 위해서 measuredValue에 계속 접근해야함
        with(measuredValue){
            binding.fineDustInformationTextView.text =
                "미세먼지: $pm10Value ㎍/㎥ ${(pm10Grade ?: Grade.UNKNOWN).emoji}"

            binding.ultrafineDustInformationTextView.text =
                "초미세먼지: $pm25Value ㎍/㎥ ${(pm25Grade ?: Grade.UNKNOWN).emoji}"

            with(binding.so2Item) {
                labelTextView.text = "아황산가스"
                gradeTextView.text = (so2Grade ?: Grade.UNKNOWN).toString()
                valueTextView.text = "$so2Value ppm"
            }

            with(binding.co2Item) {
                labelTextView.text = "일산화탄소"
                gradeTextView.text = (coGrade ?: Grade.UNKNOWN).toString()
                valueTextView.text = "$coValue ppm"
            }

            with(binding.o3Item) {
                labelTextView.text = "오존"
                gradeTextView.text = (o3Grade ?: Grade.UNKNOWN).toString()
                valueTextView.text = "$o3Value ppm"
            }

            with(binding.no2Item) {
                labelTextView.text = "이산화질소"
                gradeTextView.text = (no2Grade ?: Grade.UNKNOWN).toString()
                valueTextView.text = "$no2Value ppm"
            }
        }
    }


    companion object {
        private const val REQUEST_ACCESS_LOCATION_PERMISSIONS = 100
    }

}