// Mendefinisikan package untuk proyek Android, ini adalah alamat unik aplikasi Anda.
package com.andre.jamsholat

// Mengimpor kelas atau pustaka yang dibutuhkan oleh aplikasi.
import android.animation.ObjectAnimator // Untuk membuat animasi properti objek, seperti rotasi.
import android.content.Context // Memberikan akses ke sumber daya dan layanan sistem Android.
import android.hardware.Sensor // Merepresentasikan sebuah sensor di perangkat (misal: akselerometer).
import android.hardware.SensorEvent // Merepresentasikan data yang dihasilkan oleh sebuah sensor.
import android.hardware.SensorEventListener // Interface untuk menerima pembaruan dari sensor.
import android.hardware.SensorManager // Kelas utama untuk mengakses dan mengelola sensor perangkat.
import android.os.Bundle // Untuk menyimpan dan meneruskan data antar state Activity.
import android.widget.FrameLayout // Komponen UI untuk menata elemen secara bertumpuk.
import android.widget.ImageView // Komponen UI untuk menampilkan gambar.
import android.widget.TextView // Komponen UI untuk menampilkan teks.
import android.widget.Toast // Untuk menampilkan pesan singkat yang muncul sementara.
import androidx.appcompat.app.AppCompatActivity // Kelas dasar untuk Activity yang mendukung fitur-fitur modern.
import androidx.core.content.ContextCompat // Membantu mengakses sumber daya (seperti warna) dengan aman.
import androidx.core.graphics.drawable.DrawableCompat // Membantu memanipulasi drawable (gambar) agar kompatibel.
import kotlin.math.abs // Fungsi matematika untuk mendapatkan nilai absolut (selalu positif).
import kotlin.math.atan2 // Fungsi matematika untuk menghitung sudut dari koordinat (x, y).
import kotlin.math.cos // Fungsi matematika untuk menghitung kosinus.
import kotlin.math.sin // Fungsi matematika untuk menghitung sinus.

// Mendefinisikan kelas QiblaActivity, yang merupakan layar kompas kiblat.
// Ia juga mengimplementasikan SensorEventListener agar bisa "mendengarkan" data dari sensor.
class QiblaActivity : AppCompatActivity(), SensorEventListener {

    // --- Deklarasi Variabel untuk Komponen UI (Tampilan) ---
    // 'lateinit' berarti kita berjanji akan menginisialisasinya sebelum digunakan.
    private lateinit var compassBaseGroup: FrameLayout // Grup yang berisi semua elemen kompas yang berputar.
    private lateinit var imageViewQiblaPointer: ImageView // Gambar panah yang menunjuk arah kiblat.
    private lateinit var textViewQiblaDirection: TextView // Teks untuk menampilkan derajat kiblat.

    // --- Deklarasi Variabel untuk Sensor ---
    // Manajer utama untuk mengakses semua sensor di ponsel.
    private lateinit var sensorManager: SensorManager
    // Variabel untuk sensor akselerometer (mengukur gravitasi). '?' berarti bisa null.
    private var accelerometer: Sensor? = null
    // Variabel untuk sensor magnetometer (mengukur medan magnet utara).
    private var magnetometer: Sensor? = null

    // Array untuk menyimpan data mentah dari sensor.
    private val gravity = FloatArray(3) // Menyimpan data 3 sumbu (x, y, z) dari akselerometer.
    private val geomagnetic = FloatArray(3) // Menyimpan data 3 sumbu dari magnetometer.
    private val rotationMatrix = FloatArray(9) // Matriks 3x3 untuk menyimpan hasil perhitungan rotasi.
    private val orientationAngles = FloatArray(3) // Array untuk menyimpan hasil akhir: azimuth, pitch, roll.

    // --- Variabel untuk Logika Kompas ---
    // Menyimpan posisi rotasi kompas saat ini di layar, untuk animasi yang mulus.
    private var currentViewRotation: Float = 0f
    // Menyimpan hasil perhitungan sudut arah kiblat (dalam derajat).
    private var qiblaAngle: Float = 0f

    // --- Variabel untuk Data Lokasi ---
    // Menyimpan data latitude pengguna yang diterima dari MainActivity.
    private var userLatitude: Double = 0.0
    // Menyimpan data longitude pengguna yang diterima dari MainActivity.
    private var userLongitude: Double = 0.0

    // --- Konstanta Koordinat Tujuan ---
    // Koordinat Ka'bah di Mekkah (latitude).
    private val KAABA_LATITUDE = 21.4225
    // Koordinat Ka'bah di Mekkah (longitude).
    private val KAABA_LONGITUDE = 39.8262

    // Fungsi utama yang dijalankan pertama kali saat Activity (layar) ini dibuat.
    override fun onCreate(savedInstanceState: Bundle?) {
        // Memanggil implementasi default dari kelas induknya.
        super.onCreate(savedInstanceState)
        // Menghubungkan file kode Kotlin ini dengan file desain layout activity_qibla.xml.
        setContentView(R.layout.activity_qibla)

        // Menyembunyikan Action Bar (bar judul) default agar tampilan lebih penuh.
        supportActionBar?.hide()

        // --- Proses Inisialisasi: Menghubungkan variabel dengan elemen di layout XML ---
        compassBaseGroup = findViewById(R.id.compass_base_group)
        imageViewQiblaPointer = findViewById(R.id.imageViewQiblaPointer)
        textViewQiblaDirection = findViewById(R.id.textViewQiblaDirection)

        // --- Mengambil Data Lokasi dari Intent ---
        // Mengambil data "USER_LATITUDE" yang dikirim oleh MainActivity. Jika tidak ada, gunakan 0.0.
        userLatitude = intent.getDoubleExtra("USER_LATITUDE", 0.0)
        // Mengambil data "USER_LONGITUDE".
        userLongitude = intent.getDoubleExtra("USER_LONGITUDE", 0.0)

        // Pengecekan keamanan: jika data lokasi tidak valid (masih 0.0).
        if (userLatitude == 0.0 || userLongitude == 0.0) {
            // Tampilkan pesan error kepada pengguna.
            Toast.makeText(this, "Lokasi tidak valid, kembali ke halaman utama.", Toast.LENGTH_LONG).show()
            // Tutup layar kompas ini.
            finish()
            // Hentikan eksekusi fungsi onCreate lebih lanjut.
            return
        }

        // --- Inisialisasi Sensor ---
        // Mendapatkan layanan SensorManager dari sistem Android.
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        // Mendapatkan sensor akselerometer default dari perangkat.
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        // Mendapatkan sensor medan magnet default dari perangkat.
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        // --- Perhitungan Awal ---
        // Memanggil fungsi untuk menghitung sudut arah kiblat.
        calculateQiblaDirection()
        // Mengatur rotasi awal panah kiblat agar menunjuk ke arah yang benar pada kompas.
        imageViewQiblaPointer.rotation = qiblaAngle
    }

    // Fungsi yang dipanggil saat aplikasi/layar ini kembali aktif di depan pengguna.
    override fun onResume() {
        super.onResume()
        // Mulai "mendengarkan" data dari sensor akselerometer.
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        // Mulai "mendengarkan" data dari sensor magnetometer.
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    // Fungsi yang dipanggil saat aplikasi/layar ini tidak lagi aktif (misal: ditutup atau pindah app).
    override fun onPause() {
        super.onPause()
        // Berhenti "mendengarkan" data dari semua sensor untuk menghemat baterai.
        sensorManager.unregisterListener(this)
    }

    // Fungsi yang dipanggil setiap kali ada data baru dari sensor yang kita dengarkan.
    override fun onSensorChanged(event: SensorEvent?) {
        // Memastikan event (data sensor) tidak null.
        event?.let {
            // Faktor penghalusan untuk low-pass filter, membuat data lebih stabil.
            val alpha = 0.97f
            // Cek tipe sensor yang mengirim data.
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                // Terapkan low-pass filter pada data gravitasi sumbu X.
                gravity[0] = alpha * gravity[0] + (1 - alpha) * it.values[0]
                // Terapkan low-pass filter pada data gravitasi sumbu Y.
                gravity[1] = alpha * gravity[1] + (1 - alpha) * it.values[1]
                // Terapkan low-pass filter pada data gravitasi sumbu Z.
                gravity[2] = alpha * gravity[2] + (1 - alpha) * it.values[2]
            } else if (it.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                // Terapkan low-pass filter pada data medan magnet sumbu X.
                geomagnetic[0] = alpha * geomagnetic[0] + (1 - alpha) * it.values[0]
                // Terapkan low-pass filter pada data medan magnet sumbu Y.
                geomagnetic[1] = alpha * geomagnetic[1] + (1 - alpha) * it.values[1]
                // Terapkan low-pass filter pada data medan magnet sumbu Z.
                geomagnetic[2] = alpha * geomagnetic[2] + (1 - alpha) * it.values[2]
            }

            // Meminta sistem Android untuk menghitung matriks rotasi dari data gravitasi dan medan magnet.
            val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
            // Jika perhitungan berhasil.
            if (success) {
                // Meminta sistem untuk mengubah matriks rotasi menjadi sudut orientasi (azimuth, pitch, roll).
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                // Ambil sudut azimuth (rotasi terhadap utara) dan konversi dari radian ke derajat.
                val azimuthInDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                // Panggil fungsi untuk memperbarui tampilan kompas di layar.
                updateCompass(azimuthInDegrees)
            }
        }
    }

    // Fungsi yang dipanggil jika akurasi sensor berubah.
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Jika akurasi sensor dianggap tidak dapat diandalkan.
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            // Tampilkan pesan kepada pengguna untuk melakukan kalibrasi.
            Toast.makeText(this, "Akurasi sensor rendah, coba gerakkan perangkat.", Toast.LENGTH_SHORT).show()
        }
    }

    // Fungsi untuk memperbarui tampilan visual kompas di layar.
    private fun updateCompass(azimuth: Float) {
        // Tentukan sudut rotasi tujuan (negatif karena sistem koordinat Android berbeda).
        val targetRotation = -azimuth

        // Hitung selisih antara rotasi tujuan dan rotasi saat ini.
        var rotationDiff = targetRotation - currentViewRotation
        // Logika untuk mencari jalur putar terpendek (menghindari putaran 360 derajat yang tidak perlu).
        if (rotationDiff > 180) {
            rotationDiff -= 360
        } else if (rotationDiff < -180) {
            rotationDiff += 360
        }

        // Hitung posisi rotasi akhir yang akan dianimasikan.
        val finalRotation = currentViewRotation + rotationDiff

        // Gunakan ObjectAnimator untuk membuat animasi rotasi yang mulus.
        // Ini akan menganimasikan properti "rotation" dari compassBaseGroup.
        val animator = ObjectAnimator.ofFloat(compassBaseGroup, "rotation", currentViewRotation, finalRotation)
        // Atur durasi animasi menjadi 250 milidetik (seperempat detik).
        animator.duration = 250
        // Mulai animasi.
        animator.start()

        // Simpan posisi rotasi terakhir untuk perhitungan animasi berikutnya.
        currentViewRotation = finalRotation

        // Perbarui teks di layar untuk menampilkan derajat kiblat.
        textViewQiblaDirection.text = "${qiblaAngle.toInt()}° dari Utara"

        // --- Logika Pengecekan Keselarasan ---
        // Batas toleransi untuk dianggap lurus (dalam derajat).
        val alignmentThreshold = 3.0f
        // Hitung selisih absolut antara arah ponsel (azimuth) dan arah kiblat.
        val diff = abs(azimuth - qiblaAngle)

        // Cek apakah selisihnya sangat kecil (mendekati 0 atau 360).
        if (diff < alignmentThreshold || diff > (360 - alignmentThreshold)) {
            // Jika ya, ubah warna panah menjadi hijau.
            setQiblaPointerColor(R.color.green_alignment)
        } else {
            // Jika tidak, kembalikan warna panah ke warna default (biru kehijauan).
            setQiblaPointerColor(R.color.teal_200)
        }
    }

    // Fungsi untuk mengubah warna panah kiblat.
    private fun setQiblaPointerColor(colorResId: Int) {
        // Dapatkan drawable (gambar vektor) dari ImageView.
        val drawable = imageViewQiblaPointer.drawable
        // Bungkus drawable agar bisa dimodifikasi warnanya dengan aman.
        val wrappedDrawable = DrawableCompat.wrap(drawable.mutate())
        // Atur warna baru pada drawable yang sudah dibungkus.
        DrawableCompat.setTint(wrappedDrawable, ContextCompat.getColor(this, colorResId))
        // Set drawable yang sudah diwarnai kembali ke ImageView
        imageViewQiblaPointer.setImageDrawable(wrappedDrawable)
    }
//tes woi
    // Fungsi untuk menghitung sudut arah kiblat dari lokasi pengguna.
    private fun calculateQiblaDirection() {
        // Konversi semua data derajat (latitude/longitude) ke radian untuk perhitungan matematika.
        val userLatRad = Math.toRadians(userLatitude)
        val kaabaLatRad = Math.toRadians(KAABA_LATITUDE)
        val lonDiff = Math.toRadians(KAABA_LONGITUDE - userLongitude)

        // Rumus Haversine untuk menghitung bearing (sudut arah) antara dua titik di bumi.
        val y = sin(lonDiff) * cos(kaabaLatRad)
        val x = cos(userLatRad) * sin(kaabaLatRad) - sin(userLatRad) * cos(kaabaLatRad) * cos(lonDiff)

        // Hitung sudut menggunakan atan2, lalu konversi kembali dari radian ke derajat.
        val angle = Math.toDegrees(atan2(y, x)).toFloat()
        // Normalisasi sudut agar selalu berada di antara 0 dan 360 derajat.
        qiblaAngle = (angle + 360) % 360
    }
}
