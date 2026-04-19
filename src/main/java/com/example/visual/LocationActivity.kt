package com.example.visual

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.os.*
import android.provider.Settings
import android.telephony.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class LocationActivity : AppCompatActivity(), LocationListener {

    private lateinit var lm: LocationManager
    private lateinit var tvLat: TextView
    private lateinit var tvLon: TextView
    private lateinit var tvAlt: TextView
    private lateinit var tvTime: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)
        tvLat = findViewById(R.id.tv_lat)
        tvLon = findViewById(R.id.tv_lon)
        tvAlt = findViewById(R.id.tv_alt)
        tvTime = findViewById(R.id.tv_time)
        lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        findViewById<Button>(R.id.btn_clear_log).setOnClickListener {
            tvLat.text = "Широта: "
            tvLon.text = "Долгота: "
            tvAlt.text = "Высота: "
            File(externalCacheDir, "location.json").delete()
            Toast.makeText(this, "Лог очищен", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(object : Runnable {
            override fun run() {
                tvTime.text = "Время: ${SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())}"
                handler.postDelayed(this, 1000)
            }
        })
        if (listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE)
                .all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) startLocationUpdates()
            else startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE), 100)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
        lm.removeUpdates(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) startLocationUpdates()
            else startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else Toast.makeText(this, "Нужны разрешения", Toast.LENGTH_LONG).show()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1f, this)
        lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { showLocation(it) }
    }

    private fun showLocation(loc: Location) {
        tvLat.text = "Широта: ${loc.latitude}"
        tvLon.text = "Долгота: ${loc.longitude}"
        tvAlt.text = "Высота: ${loc.altitude} м"
    }

    override fun onLocationChanged(loc: Location) {
        showLocation(loc)
        val netInfo = collectNetworkInfo()
        saveToJson(loc, netInfo)
        executor.execute { sendToServer(loc, netInfo) }
    }

    private fun collectNetworkInfo(): JSONObject {
        val json = JSONObject()
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return json
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return json.put("networkType", "No permission")
        json.put("networkOperator", tm.networkOperator ?: "")
        json.put("networkOperatorName", tm.networkOperatorName?.toString() ?: "")
        json.put("networkType", when (tm.dataNetworkType) {
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
            TelephonyManager.NETWORK_TYPE_NR -> "NR"
            else -> "Unknown"
        })
        tm.allCellInfo?.forEach { cell ->
            when (cell) {
                is CellInfoLte -> {
                    val id = cell.cellIdentity as CellIdentityLte
                    val sig = cell.cellSignalStrength
                    json.put("lteBand", if (id.bands.isNotEmpty()) id.bands[0] else -1)
                    json.put("lteCellId", id.ci).put("lteEarfcn", id.earfcn).put("lteMcc", id.mcc).put("lteMnc", id.mnc)
                    json.put("ltePci", id.pci).put("lteTac", id.tac).put("lteAsuLevel", sig.asuLevel).put("lteCqi", sig.cqi)
                    json.put("lteRsrp", sig.rsrp).put("lteRsrq", sig.rsrq).put("lteRssi", sig.rssi).put("lteRssnr", sig.rssnr)
                    json.put("lteTimingAdvance", sig.timingAdvance)
                }
                is CellInfoGsm -> {
                    val id = cell.cellIdentity
                    val sig = cell.cellSignalStrength
                    json.put("gsmCellId", id.cid).put("gsmBsic", id.bsic).put("gsmArfcn", id.arfcn).put("gsmLac", id.lac)
                    json.put("gsmMcc", id.mcc).put("gsmMnc", id.mnc).put("gsmPsc", id.psc).put("gsmDbm", sig.dbm)
                    json.put("gsmRssi", sig.rssi).put("gsmTimingAdvance", sig.timingAdvance)
                }
                is CellInfoNr -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val id = cell.cellIdentity
                    val sig = cell.cellSignalStrength
                    fun Any.getInt(m: String) = try { javaClass.getMethod(m).invoke(this) as? Int ?: -1 } catch (e: Exception) { -1 }
                    fun Any.getLong(m: String) = try { javaClass.getMethod(m).invoke(this) as? Long ?: -1L } catch (e: Exception) { -1L }
                    json.put("nrBand", id.getInt("getBand")).put("nrNci", id.getLong("getNci")).put("nrPci", id.getInt("getPci"))
                    json.put("nrNrarfcn", id.getInt("getNrarfcn")).put("nrTac", id.getInt("getTac")).put("nrMcc", id.getInt("getMcc"))
                    json.put("nrMnc", id.getInt("getMnc")).put("nrSsRsrp", sig.getInt("getSsRsrp")).put("nrSsRsrq", sig.getInt("getSsRsrq"))
                    json.put("nrSsSinr", sig.getInt("getSsSinr")).put("nrTimingAdvance", sig.getInt("getTimingAdvance"))
                }
            }
        }
        return json
    }

    private fun saveToJson(loc: Location, net: JSONObject) {
        try {
            val time = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())
            val json = JSONObject().apply {
                put("latitude", loc.latitude).put("longitude", loc.longitude).put("altitude", loc.altitude)
                put("accuracy", loc.accuracy).put("time", time)
                put("networkType", net.optString("networkType")).put("networkOperator", net.optString("networkOperator"))
                put("networkOperatorName", net.optString("networkOperatorName"))
            }
            File(externalCacheDir, "location.json").writeText(json.toString(2))
        } catch (e: Exception) { }
    }

    private fun sendToServer(loc: Location, net: JSONObject) {
        var ctx: ZContext? = null
        var sock: ZMQ.Socket? = null
        try {
            ctx = ZContext()
            sock = ctx.createSocket(ZMQ.REQ).apply {
                receiveTimeOut = 3000
                sendTimeOut = 3000
                connect("tcp://172.25.212.40:5555")
            }
            val json = JSONObject().apply {
                put("latitude", loc.latitude).put("longitude", loc.longitude).put("altitude", loc.altitude).put("accuracy", loc.accuracy)
                put("time", SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date()))
                put("networkType", net.optString("networkType", "Unknown"))
                put("networkOperator", net.optString("networkOperator", ""))
                put("networkOperatorName", net.optString("networkOperatorName", ""))
                listOf("lteBand","lteCellId","lteEarfcn","lteMcc","lteMnc","ltePci","lteTac","lteAsuLevel","lteCqi","lteRsrp","lteRsrq","lteRssi","lteRssnr","lteTimingAdvance").forEach { put(it, net.optInt(it, -1)) }
                listOf("gsmCellId","gsmBsic","gsmArfcn","gsmLac","gsmMcc","gsmMnc","gsmPsc","gsmDbm","gsmRssi","gsmTimingAdvance").forEach { put(it, net.optInt(it, -1)) }
                listOf("nrBand","nrPci","nrNrarfcn","nrTac","nrMcc","nrMnc","nrSsRsrp","nrSsRsrq","nrSsSinr","nrTimingAdvance").forEach { put(it, net.optInt(it, -1)) }
                put("nrNci", net.optLong("nrNci", -1L))
            }
            sock.send(json.toString().toByteArray(StandardCharsets.UTF_8))
            sock.recvStr(0)
        } catch (e: Exception) { } finally { sock?.close(); ctx?.close() }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}