# Akıllı Mutfak & Gıda Envanter Yönetimi (Smart Kitchen Inventory)

Bu proje, evsel gıda israfını önlemek amacıyla "Cozy Game" estetiği ile tasarlanmış, akıllı karar destek sistemine (Decision Support System) sahip bir Android mobil uygulamasıdır. Standart ve sıkıcı form arayüzleri yerine, interaktif ve oyunlaştırılmış bir kullanıcı deneyimi (UX) sunar.

## Projenin Öne Çıkan Özellikleri

* ** İnteraktif 'Cozy' UI/UX:** Arka plandaki retro mutfak çizimi üzerine inşa edilmiş, dinamik Z-ekseni (Elevation) katmanlandırması ve milimetrik matris ızgara (Grid) algoritması ile çalışan sürükle-bırak destekli arayüz.
* ** Akıllı Karar Destek Algoritması:** Ürünlerin açılma tarihlerini ve yapılarını (Süt, Et, Uzun Ömürlü Soslar) analiz ederek gerçek bozulma tarihlerini hesaplayan otonom iş mantığı.
* ** Otonom Sistem Bildirimleri (Event-Driven):** Uygulama kapalı dahi olsa `AlarmManager` ve `BroadcastReceiver` mimarisiyle çalışan, işletim sistemi seviyesinde "Son Kullanma Tarihi" push bildirimleri.
* ** Güvenli Veri Kalıcılığı:** Google Room Database altyapısı kullanılarak, eşyaların fiziksel koordinatları ile dijital kimliklerini birbirine bağlayan İlişkisel Haritalama (Relational Mapping) ve Asenkron (Background Thread) CRUD işlemleri.
* ** Boot Persistence:** Cihaz yeniden başlatıldığında dahi silinen alarmları hafızadan tarayarak işletim sistemine geri yükleyen güvenli OS kalkanı.

##  Kullanılan Teknolojiler & Mimari
* **Dil & Platform:** Java, Android SDK (Android Studio)
* **Veritabanı:** SQLite tabanlı Room Database (Entities, DAOs, Upsert Strategies)
* **Asenkron İşlemler:** Threading, RunOnUiThread
* **Arka Plan Servisleri:** BroadcastReceiver, NotificationManager, AlarmManager (Android 12+ API 31 Uyumlu Güvenli Düşüş Mimarisi)

---
*Bu proje, modern yazılım mühendisliği prensipleri (Kalıcılık, Olay Yönelimli Mimari ve Veritabanı Bütünlüğü) gözetilerek geliştirilmiştir.*
