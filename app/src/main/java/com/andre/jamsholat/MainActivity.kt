// Mendefinisikan package untuk proyek Android, ini adalah alamat unik aplikasi Anda.
package com.andre.jamsholat

// Mengimpor semua kelas atau pustaka yang dibutuhkan oleh aplikasi.
import android.Manifest // Untuk izin-izin sistem seperti lokasi.
import android.annotation.SuppressLint // Untuk menandai bahwa kita sudah menangani peringatan kode.
import android.app.AlarmManager // Untuk menjadwalkan notifikasi pada waktu yang tepat.
import android.app.NotificationChannel // Untuk membuat "jalur" notifikasi (wajib di Android 8+).
import android.app.NotificationManager // Untuk mengelola notifikasi.
import android.app.PendingIntent // "Surat perintah" yang akan dieksekusi nanti oleh AlarmManager.
import android.content.Context // Memberikan akses ke sumber daya dan layanan sistem Android.
import android.content.Intent // Untuk berpindah antar Activity (layar) atau mengirim broadcast.
import android.content.pm.PackageManager // Untuk memeriksa status izin yang diberikan pengguna.
import android.location.Geocoder // Untuk mengubah koordinat GPS menjadi nama alamat/kota.
import android.location.Location // Kelas yang merepresentasikan data lokasi GPS (latitude, longitude, dll).
import android.os.Build // Untuk memeriksa versi Android yang sedang berjalan di perangkat.
import android.os.Bundle // Untuk menyimpan dan meneruskan data antar state Activity.
import android.os.Handler // Untuk menjadwalkan tugas yang akan dijalankan di masa depan (misal: timer).
import android.os.Looper // Mengelola antrian pesan untuk sebuah thread.
import android.provider.Settings // Untuk mengakses pengaturan sistem.
import android.util.Log // Untuk mencetak pesan log saat debugging.
import android.view.View // Kelas dasar untuk semua komponen UI.
import android.widget.Button // Komponen UI tombol.
import android.widget.LinearLayout // Komponen UI untuk menata elemen secara linear (vertikal/horizontal).
import android.widget.TextView // Komponen UI untuk menampilkan teks.
import android.widget.Toast // Untuk menampilkan pesan singkat yang muncul sementara.
import androidx.annotation.RequiresApi // Menandai bahwa sebuah fungsi memerlukan versi Android tertentu.
import androidx.appcompat.app.AppCompatActivity // Kelas dasar untuk Activity yang mendukung fitur-fitur modern.
import androidx.core.app.ActivityCompat // Membantu dalam mengelola izin.
import androidx.core.app.NotificationCompat // Membantu membuat notifikasi yang kompatibel dengan berbagai versi Android.
import androidx.core.content.ContextCompat // Membantu mengakses sumber daya dengan cara yang aman.
import com.google.android.gms.location.* // Mengimpor semua kelas dari pustaka lokasi Google.
import org.json.JSONObject // Untuk mengurai (parse) data teks berformat JSON.
import okhttp3.* // Mengimpor semua kelas dari pustaka OkHttp untuk koneksi internet.
import java.io.IOException // Kelas untuk menangani error saat operasi input/output.
import java.time.LocalDateTime // Kelas modern untuk merepresentasikan tanggal dan waktu.
import java.time.format.DateTimeFormatter // Untuk memformat tanggal dan waktu menjadi teks.
import java.time.temporal.ChronoUnit // Untuk menghitung selisih antara dua waktu.
import java.util.Locale // Untuk menentukan format bahasa dan negara (misal: Bahasa Indonesia).
import kotlin.jvm.java // Helper untuk interoperabilitas Java.

// Mendefinisikan kelas MainActivity, yang merupakan layar utama aplikasi kita.
class MainActivity : AppCompatActivity() {

    // --- Deklarasi Variabel untuk Komponen UI (Tampilan) ---
    // Variabel ini akan dihubungkan dengan elemen di file layout XML nanti.
    // 'lateinit' berarti kita berjanji akan menginisialisasinya sebelum digunakan.
    private lateinit var clockTextView: TextView // Untuk menampilkan jam digital.
    private lateinit var dateTextView: TextView // Untuk menampilkan tanggal.
    private lateinit var layoutIqamah: LinearLayout // Wadah untuk tampilan countdown iqamah.
    private lateinit var iqamahCountdownTextView: TextView // Teks untuk countdown iqamah.
    private lateinit var layoutPrayerTimes: LinearLayout // Wadah untuk daftar waktu sholat.
    private lateinit var locationTextView: TextView // Untuk menampilkan nama lokasi/kota.
    private lateinit var qiblaButton: Button // Tombol untuk membuka layar kompas kiblat.

    // --- Variabel untuk setiap waktu sholat ---
    private lateinit var fajrTextView: TextView
    private lateinit var sunriseTextView: TextView
    private lateinit var dhuhrTextView: TextView
    private lateinit var asrTextView: TextView
    private lateinit var maghribTextView: TextView
    private lateinit var ishaTextView: TextView

    // --- Deklarasi Variabel Pembantu (Helper) ---
    // Handler untuk menjalankan tugas berulang, seperti memperbarui jam setiap detik.
    private val handler = Handler(Looper.getMainLooper())
    // Klien HTTP untuk mengambil data dari internet (API).
    private val client = OkHttpClient()

    // --- Deklarasi Variabel untuk GPS dan Lokasi ---
    // Komponen utama dari Google untuk mendapatkan lokasi perangkat.
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // Objek yang berisi konfigurasi permintaan lokasi (seberapa sering, seberapa akurat).
    private lateinit var locationRequest: LocationRequest
    // Objek yang akan menerima pembaruan lokasi dari fusedLocationClient.
    private lateinit var locationCallback: LocationCallback
    // Variabel untuk menyimpan data koordinat latitude terakhir. '?' berarti bisa null (kosong).
    private var currentLatitude: Double? = null
    // Variabel untuk menyimpan data koordinat longitude terakhir.
    private var currentLongitude: Double? = null
    // Kode unik untuk permintaan izin lokasi, agar bisa diidentifikasi saat hasilnya kembali.
    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    // --- Deklarasi Variabel untuk Notifikasi ---
    // ID unik untuk "channel" notifikasi. Wajib untuk Android 8 ke atas.
    private val CHANNEL_ID = "prayer_time_channel"
    // ID dasar untuk setiap notifikasi sholat, agar tidak saling menimpa.
    private val NOTIFICATION_ID_BASE = 1000
    // Kode unik untuk permintaan izin notifikasi (wajib untuk Android 13+).
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 101

    // --- Variabel Konfigurasi Aplikasi ---
    // Lokasi default jika GPS tidak aktif atau tidak diizinkan.
    private var CITY = "Jakarta"
    private var COUNTRY = "Indonesia"
    // Metode perhitungan waktu sholat dari API AlAdhan (3 = Egyptian General Authority).
    private val METHOD = 3

    // Map (kamus) untuk menentukan jeda waktu (dalam menit) dari adzan ke iqamah.
    private val IQAMAH_OFFSET_MINUTES = mapOf(
        "Fajr" to 15,
        "Dhuhr" to 10,
        "Asr" to 10,
        "Maghrib" to 5,
        "Isha" to 15
    )

    // Variabel untuk menyimpan data jadwal sholat yang didapat dari API.
    private var currentPrayerTimes: Map<String, String>? = null
    // Variabel untuk menyimpan informasi sholat berikutnya.
    private var nextPrayer: Pair<String, LocalDateTime>? = null

    // Anotasi untuk menandai bahwa kita sudah menangani ID yang mungkin hilang (karena kita yakin ada di layout).
    @SuppressLint("MissingInflatedId")
    // Anotasi yang menandakan fungsi ini memerlukan minimal Android Oreo (API 26) untuk berjalan.
    @RequiresApi(Build.VERSION_CODES.O)
    // Fungsi utama yang dijalankan pertama kali saat Activity (layar) ini dibuat.
    override fun onCreate(savedInstanceState: Bundle?) {
        // Memanggil implementasi default dari kelas induknya.
        super.onCreate(savedInstanceState)
        // Menghubungkan file kode Kotlin ini dengan file desain layout activity_main.xml.
        setContentView(R.layout.activity_main)

        // --- Proses Inisialisasi: Menghubungkan variabel dengan elemen di layout XML ---
        clockTextView = findViewById(R.id.textViewClock)
        dateTextView = findViewById(R.id.textViewDate)
        layoutIqamah = findViewById(R.id.layoutIqamah)
        iqamahCountdownTextView = findViewById(R.id.textViewIqamahCountdown)
        layoutPrayerTimes = findViewById(R.id.layoutPrayerTimes)
        locationTextView = findViewById(R.id.textViewLocation)
        qiblaButton = findViewById(R.id.buttonQibla)

        fajrTextView = findViewById(R.id.textViewFajr)
        sunriseTextView = findViewById(R.id.textViewSunrise)
        dhuhrTextView = findViewById(R.id.textViewDhuhr)
        asrTextView = findViewById(R.id.textViewAsr)
        maghribTextView = findViewById(R.id.textViewMaghrib)
        ishaTextView = findViewById(R.id.textViewIsha)

        // Menginisialisasi komponen utama untuk mendapatkan lokasi GPS.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // --- Memanggil fungsi-fungsi persiapan awal ---
        createLocationRequest()       // Menyiapkan konfigurasi permintaan lokasi.
        createLocationCallback()      // Menyiapkan apa yang harus dilakukan saat lokasi didapat.
        createNotificationChannel()   // Mendaftarkan channel notifikasi ke sistem.
        startClock()                  // Memulai jam digital agar berdetak.
        checkLocationPermissionsAndFetch() // Memeriksa izin lokasi dan memulai pengambilan data.

        // Memberikan aksi pada tombol kiblat saat diklik.
        qiblaButton.setOnClickListener {
            // Cek apakah data latitude dan longitude sudah tersedia.
            if (currentLatitude != null && currentLongitude != null) {
                // Buat sebuah "Intent" (niat) untuk pindah ke QiblaActivity.
                val intent = Intent(this, QiblaActivity::class.java).apply {
                    // Sisipkan data latitude dan longitude ke dalam Intent.
                    putExtra("USER_LATITUDE", currentLatitude)
                    putExtra("USER_LONGITUDE", currentLongitude)
                }
                // Jalankan Intent untuk membuka layar baru.
                startActivity(intent)
            } else {
                // Jika lokasi belum ada, tampilkan pesan singkat.
                Toast.makeText(this, "Lokasi saat ini belum tersedia. Mohon tunggu.", Toast.LENGTH_SHORT).show()
            }
        }

        // Menjadwalkan tugas kecil untuk dijalankan.
        handler.post(object : Runnable {
            override fun run() {
                // Dapatkan waktu saat ini.
                val now = LocalDateTime.now()
                // Jika waktu menunjukkan tepat tengah malam (jam 0, menit 0, detik 0).
                if (now.hour == 0 && now.minute == 0 && now.second == 0) {
                    // Ambil kembali jadwal sholat untuk hari yang baru.
                    fetchPrayerTimes(currentLatitude, currentLongitude)
                }
                // Tugas ini hanya perlu dijalankan sekali saat startup untuk pengecekan awal,
                // karena startClock() sudah menjalankan loop setiap detik.
            }
        })
    }

    // Fungsi yang dipanggil saat aplikasi kembali aktif (misal dari background).
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        // Jika izin lokasi sudah ada, mulai kembali pembaruan lokasi untuk efisiensi.
        if (checkLocationPermissions()) {
            startLocationUpdates()
        }
    }

    // Fungsi yang dipanggil saat aplikasi tidak lagi di depan (misal: pengguna menekan tombol home).
    override fun onPause() {
        super.onPause()
        // Hentikan pembaruan lokasi untuk menghemat baterai.
        stopLocationUpdates()
    }

    // Fungsi untuk memeriksa apakah izin lokasi sudah diberikan oleh pengguna.
    private fun checkLocationPermissions(): Boolean {
        // Mengembalikan true jika kedua izin (FINE dan COARSE) sudah diberikan.
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    // Fungsi untuk menampilkan dialog permintaan izin kepada pengguna.
    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            // Daftar izin yang diminta.
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            // Kode unik permintaan.
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // Fungsi yang secara otomatis dipanggil setelah pengguna merespons dialog izin.
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Cek apakah ini adalah respons untuk permintaan izin lokasi kita.
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // Cek apakah hasilnya tidak kosong dan izin pertama diberikan.
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Jika diizinkan, tampilkan pesan dan mulai pembaruan lokasi.
                Toast.makeText(this, "Izin lokasi diberikan.", Toast.LENGTH_SHORT).show()
                startLocationUpdates()
            } else {
                // Jika ditolak, tampilkan pesan dan gunakan lokasi default.
                Toast.makeText(this, "Izin lokasi ditolak. Menggunakan lokasi default.", Toast.LENGTH_LONG).show()
                locationTextView.text = "Lokasi: ${CITY}, ${COUNTRY} (Default)"
                fetchPrayerTimes(null, null) // Ambil jadwal sholat untuk Jakarta.
            }
        }
        // Cek izin notifikasi untuk Android 13 (Tiramisu) ke atas.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", "Izin notifikasi diberikan.")
                } else {
                    Log.d("MainActivity", "Izin notifikasi ditolak.")
                    Toast.makeText(this, "Izin notifikasi ditolak. Notifikasi waktu sholat tidak akan muncul.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Fungsi utama untuk mengorkestrasi alur permintaan izin.
    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkLocationPermissionsAndFetch() {
        // Jika izin sudah ada, langsung mulai update.
        if (checkLocationPermissions()) {
            startLocationUpdates()
        } else {
            // Jika belum, minta izin terlebih dahulu.
            requestLocationPermissions()
        }
        // Untuk Android 13+, cek juga izin untuk mengirim notifikasi.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Jika belum ada, minta izin notifikasi.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    // Fungsi untuk membuat dan mengkonfigurasi objek LocationRequest.
    private fun createLocationRequest() {
        // Menggunakan Builder pattern untuk membuat LocationRequest.
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 60000L) // Prioritas akurasi tinggi, interval ideal 60 detik.
            .setWaitForAccurateLocation(false) // Tidak perlu menunggu lokasi yang sangat akurat.
            .setMinUpdateIntervalMillis(30000L) // Interval tercepat pembaruan adalah 30 detik.
            .build() // Membuat objek LocationRequest.
    }

    // Fungsi untuk membuat objek LocationCallback yang akan menerima data lokasi.
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createLocationCallback() {
        // Membuat objek anonim dari kelas LocationCallback.
        locationCallback = object : LocationCallback() {
            // Fungsi ini akan dipanggil setiap kali ada hasil lokasi baru.
            override fun onLocationResult(locationResult: LocationResult) {
                // Ambil lokasi terakhir dari hasil yang diterima.
                locationResult.lastLocation?.let { location ->
                    // Cek apakah koordinatnya berubah dari yang terakhir disimpan.
                    if (currentLatitude != location.latitude || currentLongitude != location.longitude) {
                        // Jika berubah, update variabel.
                        currentLatitude = location.latitude
                        currentLongitude = location.longitude
                        Log.d("MainActivity", "Lokasi diperbarui: Lat=${currentLatitude}, Lon=${currentLongitude}")
                        // Ubah koordinat menjadi nama kota.
                        getCityNameFromLocation(location.latitude, location.longitude)
                        // Ambil jadwal sholat untuk lokasi baru ini.
                        fetchPrayerTimes(currentLatitude, currentLongitude)
                    }
                }
            }
        }
    }

    // Fungsi untuk memulai pembaruan lokasi.
    @SuppressLint("MissingPermission") // Kita sudah cek izin sebelumnya.
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startLocationUpdates() {
        // Cek lagi untuk keamanan.
        if (checkLocationPermissions()) {
            try {
                // Memerintahkan fusedLocationClient untuk mulai mengirim update.
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                Log.d("MainActivity", "Memulai pembaruan lokasi.")
            } catch (unlikely: SecurityException) {
                // Menangani error jika terjadi masalah keamanan (meskipun jarang).
                Log.e("MainActivity", "Tidak ada izin lokasi. Tidak dapat memulai pembaruan.", unlikely)
            }
        }
    }

    // Fungsi untuk menghentikan pembaruan lokasi.
    private fun stopLocationUpdates() {
        // Memerintahkan fusedLocationClient untuk berhenti mengirim update.
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("MainActivity", "Menghentikan pembaruan lokasi.")
    }

    // Fungsi untuk mengubah data koordinat (angka) menjadi nama kota (teks).
    private fun getCityNameFromLocation(latitude: Double, longitude: Double) {
        // Cek apakah layanan Geocoder tersedia di perangkat.
        if (Geocoder.isPresent()) {
            // Buat objek Geocoder dengan format Bahasa Indonesia.
            val geocoder = Geocoder(this, Locale("id", "ID"))
            try {
                // Minta Geocoder untuk mencari alamat dari koordinat (maksimal 1 hasil).
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                // Jika hasilnya tidak kosong.
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    // Ambil nama kecamatan, atau kota, atau provinsi. Mana saja yang tersedia.
                    val cityName = address.subAdminArea ?: address.locality ?: address.adminArea ?: "Tidak Dikenal"
                    // Tampilkan nama kota di UI.
                    locationTextView.text = "Lokasi: $cityName"
                } else {
                    // Jika tidak ada nama yang ditemukan, tampilkan koordinatnya saja.
                    locationTextView.text = "Lokasi: Lat ${"%.4f".format(latitude)}, Lon ${"%.4f".format(longitude)}"
                }
            } catch (e: IOException) {
                // Tangani jika ada error jaringan saat menghubungi layanan geocoder.
                Log.e("MainActivity", "Geocoder error: ${e.message}", e)
            }
        } else {
            // Jika Geocoder tidak tersedia di perangkat, tampilkan koordinatnya saja.
            locationTextView.text = "Lokasi: Lat ${"%.4f".format(latitude)}, Lon ${"%.4f".format(longitude)}"
        }
    }

    // Fungsi untuk memulai jam digital.
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startClock() {
        // Menggunakan handler untuk menjalankan tugas berulang.
        handler.post(object : Runnable {
            override fun run() {
                // Dapatkan waktu saat ini.
                val currentTime = LocalDateTime.now()
                // Buat format untuk jam:menit:detik.
                val clockFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                // Terapkan format dan tampilkan di UI.
                clockTextView.text = currentTime.format(clockFormatter)

                // Buat format untuk Hari, tanggal Bulan tahun dalam Bahasa Indonesia.
                val dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                // Terapkan format dan tampilkan di UI.
                dateTextView.text = currentTime.format(dateFormatter)

                // Panggil fungsi untuk memperbarui highlight waktu sholat dan countdown iqamah.
                updateNextPrayerAndIqamah()
                // Jadwalkan tugas ini untuk dijalankan lagi 1000 milidetik (1 detik) dari sekarang.
                handler.postDelayed(this, 1000)
            }
        })
    }

    // Fungsi untuk mengambil data jadwal sholat dari API Aladhan.com.
    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchPrayerTimes(latitude: Double?, longitude: Double?) {
        // Jalankan di UI thread untuk menyembunyikan tampilan lama.
        runOnUiThread {
            layoutPrayerTimes.visibility = View.GONE
            layoutIqamah.visibility = View.GONE
            Log.d("MainActivity", "Memuat waktu sholat...")
        }

        // Dapatkan tanggal hari ini.
        val today = LocalDateTime.now()
        // Format tanggal menjadi "dd-MM-yyyy" sesuai permintaan API.
        val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val dateString = today.format(dateFormatter)

        // Buat URL API. Jika ada data GPS, gunakan endpoint 'timings'. Jika tidak, gunakan 'timingsByCity'.
        val url = if (latitude != null && longitude != null) {
            "https://api.aladhan.com/v1/timings/$dateString?latitude=$latitude&longitude=$longitude&method=$METHOD"
        } else {
            "https://api.aladhan.com/v1/timingsByCity/$dateString?city=$CITY&country=$COUNTRY&method=$METHOD"
        }

        // Buat objek Request dari URL.
        val request = Request.Builder().url(url).build()

        // Kirim request secara asynchronous (di background thread).
        client.newCall(request).enqueue(object : Callback {
            // Fungsi ini akan dipanggil jika terjadi kegagalan koneksi.
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                // Tampilkan pesan error di UI thread.
                runOnUiThread {
                    showErrorMessage("Gagal mengambil waktu sholat. Cek koneksi internet.")
                }
            }

            // Fungsi ini akan dipanggil jika server memberikan respons.
            override fun onResponse(call: Call, response: Response) {
                // Ambil isi respons sebagai teks (String).
                response.body?.string()?.let {
                    try {
                        // Ubah teks JSON menjadi objek JSON.
                        val jsonObject = JSONObject(it)
                        // Ambil objek "data" dari dalam JSON.
                        val data = jsonObject.getJSONObject("data")
                        // Ambil objek "timings" dari dalam "data".
                        val timings = data.getJSONObject("timings")

                        // Buat Map (kamus) untuk menyimpan hasil parsing.
                        val parsedTimings = mutableMapOf<String, String>()
                        parsedTimings["Fajr"] = timings.getString("Fajr")
                        parsedTimings["Sunrise"] = timings.getString("Sunrise")
                        parsedTimings["Dhuhr"] = timings.getString("Dhuhr")
                        parsedTimings["Asr"] = timings.getString("Asr")
                        parsedTimings["Maghrib"] = timings.getString("Maghrib")
                        parsedTimings["Isha"] = timings.getString("Isha")

                        // Simpan hasil parsing ke variabel global.
                        currentPrayerTimes = parsedTimings

                        // Jalankan di UI thread untuk memperbarui tampilan.
                        runOnUiThread {
                            updatePrayerTimesUI(parsedTimings) // Tampilkan jadwal baru.
                            updateNextPrayerAndIqamah()        // Update highlight.
                            layoutPrayerTimes.visibility = View.VISIBLE // Munculkan kembali layout.
                            schedulePrayerNotifications(parsedTimings) // Jadwalkan notifikasi.
                        }
                    } catch (e: Exception) {
                        // Tangani jika ada error saat parsing JSON.
                        e.printStackTrace()
                        runOnUiThread {
                            showErrorMessage("Gagal memproses data waktu sholat.")
                        }
                    }
                }
            }
        })
    }

    // Fungsi untuk mengisi data jadwal sholat ke elemen-elemen TextView.
    private fun updatePrayerTimesUI(timings: Map<String, String>) {
        fajrTextView.text = "Subuh: ${timings["Fajr"]}"
        sunriseTextView.text = "Terbit: ${timings["Sunrise"]}"
        dhuhrTextView.text = "Dzuhur: ${timings["Dhuhr"]}"
        asrTextView.text = "Ashar: ${timings["Asr"]}"
        maghribTextView.text = "Maghrib: ${timings["Maghrib"]}"
        ishaTextView.text = "Isya: ${timings["Isha"]}"

        // Reset semua latar belakang ke warna default.
        val allPrayerTextViews = listOf(fajrTextView, sunriseTextView, dhuhrTextView, asrTextView, maghribTextView, ishaTextView)
        for (tv in allPrayerTextViews) {
            tv.background = ContextCompat.getDrawable(this, R.drawable.rounded_background_dark_opacity)
            tv.setTextColor(ContextCompat.getColor(this, R.color.white_text_color))
        }
    }

    // Fungsi inti untuk menentukan sholat aktif, sholat berikutnya, dan countdown iqamah.
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateNextPrayerAndIqamah() {
        // Jika data jadwal sholat belum ada, hentikan fungsi.
        val prayerTimes = currentPrayerTimes ?: return
        // Dapatkan waktu saat ini.
        val now = LocalDateTime.now()
        // Urutan sholat yang benar.
        val prayerOrder = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
        // Map untuk menghubungkan nama sholat dengan TextView-nya.
        val prayerTextViews = mapOf(
            "Fajr" to fajrTextView, "Sunrise" to sunriseTextView, "Dhuhr" to dhuhrTextView,
            "Asr" to asrTextView, "Maghrib" to maghribTextView, "Isha" to ishaTextView
        )

        // Ubah semua string waktu sholat (misal "04:30") menjadi objek LocalDateTime untuk hari ini.
        val prayerLocalTimes = prayerOrder.mapNotNull { name ->
            prayerTimes[name]?.let { timeStr ->
                val (hours, minutes) = timeStr.split(":").map { it.toInt() }
                name to now.withHour(hours).withMinute(minutes).withSecond(0).withNano(0)
            }
        }.toMap()

        // Filter untuk mendapatkan semua sholat yang waktunya SETELAH sekarang.
        val upcomingPrayers = prayerLocalTimes.filter { it.value.isAfter(now) }
        // Filter untuk mendapatkan semua sholat yang waktunya SEBELUM sekarang (kecuali Terbit).
        val pastPrayers = prayerLocalTimes.filter { it.value.isBefore(now) && it.key != "Sunrise"}

        // Cari sholat berikutnya (waktu paling kecil dari upcomingPrayers).
        val nextPrayerInfo = upcomingPrayers.minByOrNull { it.value }
        // Cari sholat aktif (waktu paling besar dari pastPrayers).
        val currentActivePrayerInfo = pastPrayers.maxByOrNull { it.value }

        // Tentukan nilai variabel global 'nextPrayer'.
        nextPrayer = if (nextPrayerInfo != null) {
            // Jika ada sholat berikutnya hari ini, gunakan itu.
            nextPrayerInfo.toPair()
        } else {
            // Jika tidak ada (sudah Isya), maka sholat berikutnya adalah Subuh besok.
            prayerLocalTimes["Fajr"]?.let { "Fajr" to it.plusDays(1) }
        }

        // Jalankan di UI thread untuk memperbarui tampilan.
        runOnUiThread {
            // Reset semua latar belakang ke default.
            prayerTextViews.values.forEach {
                it.background = ContextCompat.getDrawable(this, R.drawable.rounded_background_dark_opacity)
                it.setTextColor(ContextCompat.getColor(this, R.color.white_text_color))
            }

            // Jika ada sholat aktif, beri highlight hijau.
            currentActivePrayerInfo?.let { (name, _) ->
                prayerTextViews[name]?.apply {
                    background = ContextCompat.getDrawable(this@MainActivity, R.drawable.rounded_background_green_highlight)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.teal_100_color))
                }
            }

            // Jika ada sholat berikutnya, beri highlight biru.
            nextPrayer?.let { (name, _) ->
                prayerTextViews[name]?.apply {
                    background = ContextCompat.getDrawable(this@MainActivity, R.drawable.rounded_background_blue_highlight)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.blue_highlight_text_color))
                }
            }
            // Panggil fungsi untuk menghitung countdown iqamah berdasarkan sholat aktif.
            calculateIqamahCountdown(currentActivePrayerInfo?.value)
        }
    }

    // Fungsi untuk menghitung dan menampilkan countdown menuju iqamah.
    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateIqamahCountdown(currentActivePrayerTime: LocalDateTime?) {
        // Jika tidak ada sholat yang aktif, sembunyikan tampilan countdown dan hentikan fungsi.
        if (currentActivePrayerTime == null) {
            layoutIqamah.visibility = View.GONE
            return
        }

        // Cari nama sholat dari waktu aktifnya.
        val prayerName = currentPrayerTimes?.entries?.find {
            val (h, m) = it.value.split(":").map { it.toInt() }
            currentActivePrayerTime.hour == h && currentActivePrayerTime.minute == m
        }?.key ?: return

        // Ambil jeda iqamah dari map konfigurasi.
        val offset = IQAMAH_OFFSET_MINUTES[prayerName] ?: 0
        // Hitung waktu iqamah = waktu adzan + jeda.
        val iqamahTime = currentActivePrayerTime.plusMinutes(offset.toLong())
        val now = LocalDateTime.now()

        // Jika waktu sekarang masih SEBELUM waktu iqamah.
        if (now.isBefore(iqamahTime)) {
            // Hitung sisa waktu dalam detik.
            val remainingSeconds = ChronoUnit.SECONDS.between(now, iqamahTime)
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            // Format menjadi "mm:ss" dan tampilkan di UI.
            iqamahCountdownTextView.text = String.format("%02d:%02d", minutes, seconds)
            // Tampilkan layout countdown.
            layoutIqamah.visibility = View.VISIBLE
        } else {
            // Jika waktu iqamah sudah lewat, sembunyikan layout countdown.
            layoutIqamah.visibility = View.GONE
        }
    }

    // Fungsi untuk membuat channel notifikasi (wajib untuk Android 8+).
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        // Nama channel yang akan muncul di pengaturan aplikasi.
        val name = getString(R.string.channel_name)
        // Deskripsi channel.
        val descriptionText = getString(R.string.channel_description)
        // Tingkat kepentingan notifikasi (HIGH agar muncul sebagai pop-up).
        val importance = NotificationManager.IMPORTANCE_HIGH
        // Buat objek channel dengan ID, nama, dan tingkat kepentingan.
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // Dapatkan layanan NotificationManager dari sistem.
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Daftarkan channel ini ke sistem Android.
        notificationManager.createNotificationChannel(channel)
    }

    // Fungsi untuk menjadwalkan notifikasi untuk semua waktu sholat.
    @RequiresApi(Build.VERSION_CODES.O)
    private fun schedulePrayerNotifications(prayerTimesMap: Map<String, String>) {
        // Dapatkan layanan AlarmManager dari sistem.
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Daftar sholat yang akan diberi notifikasi.
        val prayerNames = listOf("Fajr", "Dhr", "Asr", "Maghrib", "Isha")
        val now = LocalDateTime.now()

        // Lakukan perulangan untuk setiap nama sholat.
        prayerNames.forEachIndexed { index, prayerName ->
            // Ambil string waktu dari map, jika tidak ada, lewati.
            val timeStr = prayerTimesMap[prayerName] ?: return@forEachIndexed
            // Ubah string "HH:mm" menjadi angka jam dan menit.
            val (hours, minutes) = timeStr.split(":").map { it.toInt() }
            // Buat objek LocalDateTime untuk waktu sholat hari ini.
            var prayerTime = now.withHour(hours).withMinute(minutes).withSecond(0).withNano(0)

            // Jika waktu sholat hari ini sudah lewat, jadwalkan untuk besok.
            if (prayerTime.isBefore(now)) {
                prayerTime = prayerTime.plusDays(1)
            }

            // Konversi waktu alarm ke format milidetik (epoch).
            val triggerMillis = prayerTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            // Buat Intent yang akan dijalankan saat alarm berbunyi.
            val intent = Intent(this, PrayerAlarmReceiver::class.java).apply {
                action = "com.andre.jamsholat.PRAYER_ALARM"
                putExtra("prayer_name", prayerName) // Sisipkan nama sholat.
            }
            // Bungkus Intent ke dalam PendingIntent.
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                NOTIFICATION_ID_BASE + index, // ID unik untuk setiap alarm.
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Cek versi Android untuk metode penjadwalan yang benar.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
                // Cek apakah aplikasi punya izin untuk alarm presisi.
                if (alarmManager.canScheduleExactAlarms()) {
                    // Jadwalkan alarm presisi yang bisa berjalan bahkan saat mode hemat daya.
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    Log.d("MainActivity", "Notifikasi ${prayerName} dijadwalkan untuk: $prayerTime")
                } else {
                    Log.w("MainActivity", "Tidak dapat menjadwalkan notifikasi persis. Izin tidak diberikan.")
                }
            } else { // Android di bawah 12.
                // Langsung jadwalkan alarm presisi.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
                Log.d("MainActivity", "Notifikasi ${prayerName} dijadwalkan untuk: $prayerTime (API < 31)")
            }
        }
    }

    // Fungsi pembantu untuk menampilkan pesan singkat (Toast).
    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e("MainActivity", "Error displayed to user: $message")
    }
}
