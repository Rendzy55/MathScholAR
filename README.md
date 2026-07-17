# MathScholAR

MathScholAR adalah aplikasi edukasi berbasis *Augmented Reality* (AR) yang dirancang khusus untuk membantu siswa Sekolah Dasar (SD) kelas 5-6 dalam memahami materi Geometri. Melalui visualisasi 3D yang interaktif, siswa dapat mempelajari bangun ruang seperti Kubus, Balok, dan Prisma secara lebih mendalam dan menyenangkan.

## Fitur Utama

- **Visualisasi Augmented Reality (AR):** Menampilkan bangun ruang geometri secara 3D langsung di lingkungan sekitar pengguna menggunakan kamera ponsel (diperkuat oleh ARCore).
- **Tutor AI Terintegrasi (Polya Method):** Dilengkapi dengan asisten cerdas yang memandu siswa menjawab soal-soal matematika. AI tidak akan memberikan jawaban secara instan, melainkan memandu pemahaman siswa menggunakan metode problem-solving Polya.
- **Sistem Pembaruan Otomatis (In-App Updates):** Aplikasi akan mendeteksi dan mengunduh versi terbaru secara langsung di dalam aplikasi melalui sistem Over-The-Air (OTA).
- **Pemantauan Performa dan Stabilitas:** Memanfaatkan teknologi pencatatan otomatis (*crash reporting*) untuk memastikan kelancaran pengalaman belajar.

## Arsitektur & Teknologi

Proyek ini dibangun menggunakan berbagai teknologi dan pustaka modern pada ekosistem Android:
- **Bahasa Pemrograman:** Kotlin
- **Networking:** Ktor Client (untuk integrasi API secara *asynchronous*) & Kotlinx Serialization
- **Database & Backend:** Supabase (mengelola pembaruan aplikasi dan konfigurasi *cloud*)
- **Augmented Reality:** ARCore / Sceneform
- **Artificial Intelligence:** Groq API (menggunakan model generasi bahasa untuk Tutor AI)
- **Monitoring:** Sentry.io (pemantauan *error* & kinerja aplikasi secara *real-time*)
- **Build System:** Gradle (didukung oleh GitHub Actions untuk CI/CD rilis otomatis)

## Persyaratan Sistem

- Perangkat Android dengan dukungan **Google Play Services for AR (ARCore)**
- Minimum OS: Android 7.0 (API Level 24)
- Target OS: Android 14 (API Level 34)

## Kontribusi dan Pelaporan Bug

Jika Anda menemukan kendala saat menggunakan aplikasi atau memiliki saran perbaikan, silakan manfaatkan fitur *Issues* di repositori ini. Karena ini adalah lingkungan pembelajaran, kami memprioritaskan stabilitas serta keakuratan materi. Setiap masukan teknis maupun masukan kurikulum sangat diapresiasi.

---
*MathScholAR - Membawa dunia matematika ke dalam dimensi nyata.*
