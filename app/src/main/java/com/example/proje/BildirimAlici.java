package com.example.proje; // zamanlanmış görevler ve sistem olaylarını yakalamak için

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BildirimAlici extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                "android.intent.action.LOCKED_BOOT_COMPLETED".equals(intent.getAction())) {  //cihazın yeniden başlatılması

            new Thread(() -> {  //cihazın yoğunuğunu azaltmak için
                Veritabani db = Veritabani.getVeritabani(context);
                List<YiyecekVarlik> tumYiyecekler = db.yiyecekDao().tumYiyecekleriGetir();
                SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy");
                long suAnkiZaman = System.currentTimeMillis();

                for (YiyecekVarlik y : tumYiyecekler) {
                    try {
                        Date tettTarihi = sdf.parse(y.tettTarihi);
                        Date gercekBozulmaTarihi = tettTarihi;

                        if (!y.acilmaTarihi.equals("Açılmadı")) {
                            Date acilmaTarihi = sdf.parse(y.acilmaTarihi);
                            Calendar hesaplayici = Calendar.getInstance();
                            hesaplayici.setTime(acilmaTarihi);

                            boolean etUrunumu = y.isim.equals("Et") || y.isim.equals("Kıyma") ||
                                    y.isim.equals("Pastırma") || y.isim.equals("Sosis") ||
                                    y.isim.contains("Tavuk") || y.isim.equals("Antrikot");

                            if (y.isim.equals("Süt") || y.isim.equals("Yoğurt") || y.isim.equals("Peynir") || y.isim.equals("Labne")) {
                                hesaplayici.add(Calendar.DAY_OF_YEAR, 5);
                            } else if (etUrunumu) {
                                hesaplayici.add(Calendar.DAY_OF_YEAR, 3);
                            }

                            if (hesaplayici.getTime().before(tettTarihi)) {
                                gercekBozulmaTarihi = hesaplayici.getTime();
                            }
                        }

                        long birGunKala = gercekBozulmaTarihi.getTime() - (1L * 24L * 60L * 60L * 1000L);
                        if (birGunKala > suAnkiZaman) {

                            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                            if (alarmManager == null) continue;

                            Intent alarmIntent = new Intent(context, BildirimAlici.class);
                            alarmIntent.putExtra("baslik", "Mutfaktan Uyarı! 🧑‍🍳");
                            alarmIntent.putExtra("mesaj", y.isim + " ürününü tüketmek için son günün!");

                            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, y.id, alarmIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                            // --- BURADAKİ KIRMIZI HATA GÜVENLİK KONTROLÜYLE ÇÖZÜLDÜ ---
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                if (alarmManager.canScheduleExactAlarms()) {
                                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, birGunKala, pendingIntent);
                                } else {
                                    alarmManager.set(AlarmManager.RTC_WAKEUP, birGunKala, pendingIntent);
                                }
                            } else {
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, birGunKala, pendingIntent);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            return;
        }

        // bildirim
        String baslik = intent.getStringExtra("baslik");
        String mesaj = intent.getStringExtra("mesaj");

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("mutfak_kanal", "Mutfak Bildirimleri", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "mutfak_kanal")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(baslik)
                .setContentText(mesaj)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
