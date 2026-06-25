package com.example.proje;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    class Yiyecek {
        int dbId;
        int resimId;
        String isim;
        String acilmaTarihi;
        String tettTarihi;
        String miktar;
        String fiyat;
        ImageView fizikselGorsel;
        Long applianceInstanceId;
        int rafIndex;
    }

    class EsyaInstance {
        Long instanceId;
        String type; // eşyalar
        boolean isStorage;
        List<Yiyecek> yiyecekler = new ArrayList<>();
        boolean[] rafDoluMu;
        int capacity;
    }

    private FrameLayout odaCercevesi;
    private View karartmaEkrani;
    private ImageView buyukAcikEsyaGosterici;
    private LinearLayout butonGrubu, menuYiyeceklerLayout;
    private GridLayout gridYiyecekler;
    private TextView btnEsyayiKapat, btnEnvanterPanoBtn, btnEldenPanoEkle;

    private View triggerUstDolap, triggerBuzdolabi, triggerAltDolap, triggerCesme;

    private int tiklamaDurumu = 0;
    private EsyaInstance aktifEsyaInstance = null;
    private View aktifTriggerView = null;

    private List<Yiyecek> yiyecekEnvanteri = new ArrayList<>();
    private Map<Long, EsyaInstance> esyaInstanceMap = new HashMap<>();
    private Map<View, EsyaInstance> esyaTriggerMap = new HashMap<>();
    private Veritabani db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        db = Veritabani.getVeritabani(this);

        odaCercevesi = findViewById(R.id.odaCercevesi);
        karartmaEkrani = findViewById(R.id.karartmaEkrani);
        buyukAcikEsyaGosterici = findViewById(R.id.buyukAcikEsyaGosterici);
        butonGrubu = findViewById(R.id.butonGrubu);
        btnEsyayiKapat = findViewById(R.id.btnEsyayiKapat);
        btnEnvanterPanoBtn = findViewById(R.id.btnEnvanterPano2);
        btnEldenPanoEkle = findViewById(R.id.btnEldenPanoEkle);

        menuYiyeceklerLayout = findViewById(R.id.menuYiyeceklerLayout);
        gridYiyecekler = findViewById(R.id.gridYiyecekler);

        triggerUstDolap = findViewById(R.id.triggerUstDolap);
        triggerBuzdolabi = findViewById(R.id.triggerBuzdolabi);
        triggerAltDolap = findViewById(R.id.triggerAltDolap);
        triggerCesme = findViewById(R.id.triggerCesme);

        View tabSut = findViewById(R.id.tabSut); if(tabSut != null) tabSut.setOnClickListener(v -> kategoriYukle("sut_urunleri"));
        View tabSebze = findViewById(R.id.tabSebze); if(tabSebze != null) tabSebze.setOnClickListener(v -> kategoriYukle("sebzeler"));
        View tabMeyve = findViewById(R.id.tabMeyve); if(tabMeyve != null) tabMeyve.setOnClickListener(v -> kategoriYukle("meyveler"));
        View tabEt = findViewById(R.id.tabEt); if(tabEt != null) tabEt.setOnClickListener(v -> kategoriYukle("et ürünleri"));
        View tabDiger = findViewById(R.id.tabDiger); if(tabDiger != null) tabDiger.setOnClickListener(v -> kategoriYukle("Diğer yiyecekler"));

        if (btnEnvanterPanoBtn != null) { btnEnvanterPanoBtn.setOnClickListener(v -> tatliPanoAc()); }
        if (btnEldenPanoEkle != null) { btnEldenPanoEkle.setOnClickListener(v -> tazeBoardYiyecekEklemeAç()); }
        if (btnEsyayiKapat != null) { btnEsyayiKapat.setOnClickListener(v -> esyayiEskiHalineDondur()); }
        if (karartmaEkrani != null) { karartmaEkrani.setOnClickListener(v -> esyayiEskiHalineDondur()); }

        if (odaCercevesi != null) {
            odaCercevesi.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    odaCercevesi.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    haritadakiCiziliEsyalariAyağaKaldir();
                    veritabanindanMutfağiGeriYukle();
                }
            });
        }
    }

    private void haritadakiCiziliEsyalariAyağaKaldir() {
        ciziliEsyaEntegreEt(triggerBuzdolabi, 1L, "buzdolabi", 10);
        ciziliEsyaEntegreEt(triggerCesme, 2L, "cesme", 0);
        ciziliEsyaEntegreEt(triggerUstDolap, 3L, "dolap1", 8);
        ciziliEsyaEntegreEt(triggerAltDolap, 4L, "dolap2", 8);
    }

    private void ciziliEsyaEntegreEt(View trigger, Long id, String type, int cap) {
        if (trigger == null) return;

        EsyaInstance ei = new EsyaInstance();
        ei.instanceId = id; ei.type = type; ei.isStorage = !type.equals("cesme");
        ei.capacity = cap; ei.rafDoluMu = new boolean[cap];

        esyaTriggerMap.put(trigger, ei); esyaInstanceMap.put(id, ei);

        new Thread(() -> {
            ApplianceVarlik av = new ApplianceVarlik();
            av.id = id; av.type = type; av.positionX = 0f; av.positionY = 0f; av.tagStatus = "sabit";
            db.applianceDao().applianceKaydet(av);
        }).start();

        trigger.setOnClickListener(v -> esyaTiklamaIsleminiBaslat(trigger));
    }

    private void acilDurumAnimasyonuUygula(ImageView img, String tettTarihiStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy"); Date tett = sdf.parse(tettTarihiStr);
            long kalanGun = (tett.getTime() - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);
            if (kalanGun <= 2 && kalanGun >= 0) {
                Animation pulse = new AlphaAnimation(1.0f, 0.3f); pulse.setDuration(600); pulse.setRepeatCount(Animation.INFINITE); pulse.setRepeatMode(Animation.REVERSE); img.startAnimation(pulse);
            } else { img.clearAnimation(); }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void veritabanindanMutfağiGeriYukle() {
        List<YiyecekVarlik> kayitlar = db.yiyecekDao().tumYiyecekleriGetir();
        for (YiyecekVarlik v : kayitlar) {
            if (v.applianceInstanceId == null || v.applianceInstanceId == -1) {
                Yiyecek y = new Yiyecek(); y.dbId = v.id; y.resimId = v.resimId; y.isim = v.isim;
                y.acilmaTarihi = v.acilmaTarihi; y.tettTarihi = v.tettTarihi; y.miktar = v.miktar; y.fiyat = v.fiyat;
                y.applianceInstanceId = -1L; y.rafIndex = -1; yiyecekEnvanteri.add(y); continue;
            }

            EsyaInstance hedefInstance = esyaInstanceMap.get(v.applianceInstanceId);
            if (hedefInstance == null) continue;

            Yiyecek y = new Yiyecek(); y.dbId = v.id; y.resimId = v.resimId; y.isim = v.isim;
            y.acilmaTarihi = v.acilmaTarihi; y.tettTarihi = v.tettTarihi; y.miktar = v.miktar; y.fiyat = v.fiyat;
            y.applianceInstanceId = v.applianceInstanceId; y.rafIndex = v.rafIndex;

            hedefInstance.yiyecekler.add(y);
            if (v.rafIndex >= 0 && v.rafIndex < hedefInstance.capacity) { hedefInstance.rafDoluMu[v.rafIndex] = true; }

            ImageView fizikselGorsel = new ImageView(this); fizikselGorsel.setImageResource(y.resimId);
            int yiyecekBoyut = (int) (40 * getResources().getDisplayMetrics().density); FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(yiyecekBoyut, yiyecekBoyut); fizikselGorsel.setLayoutParams(params);

            fizikselGorsel.setElevation(50f); fizikselGorsel.setVisibility(View.GONE);

            if (odaCercevesi != null) odaCercevesi.addView(fizikselGorsel); y.fizikselGorsel = fizikselGorsel;
            fizikselGorsel.setOnClickListener(imgV -> tatliYiyecekBilgiKartiniAc(y));
            yiyecekEnvanteri.add(y);
        }
    }

    private void esyaTiklamaIsleminiBaslat(View dokunulanTriggerView) {
        EsyaInstance ei = esyaTriggerMap.get(dokunulanTriggerView);
        if (ei == null) return;

        if (!ei.isStorage) {
            Toast.makeText(this, "Çeşme sadece yıkamak için, içine yiyecek konmaz! 🚰", Toast.LENGTH_SHORT).show();
            return;
        }

        aktifTriggerView = dokunulanTriggerView; aktifEsyaInstance = ei;

        if (karartmaEkrani != null) { karartmaEkrani.setVisibility(View.VISIBLE); karartmaEkrani.bringToFront(); }
        if (butonGrubu != null) { butonGrubu.bringToFront(); butonGrubu.setVisibility(View.VISIBLE); }

        if (buyukAcikEsyaGosterici != null) {
            int buyukResimId = R.drawable.dolap2_acik_buyuk;
            if (ei.type.equals("buzdolabi")) buyukResimId = R.drawable.buzdolabi_acik;
            else if (ei.type.equals("dolap1")) buyukResimId = R.drawable.dolap1_acik_buyuk;

            buyukAcikEsyaGosterici.setImageResource(buyukResimId);
            buyukAcikEsyaGosterici.setVisibility(View.VISIBLE);
            buyukAcikEsyaGosterici.bringToFront();
            if (buyukAcikEsyaGosterici.getElevation() < 6f) buyukAcikEsyaGosterici.setElevation(5f);
        }

        tiklamaDurumu = 2;
        if (menuYiyeceklerLayout != null) menuYiyeceklerLayout.setVisibility(View.VISIBLE);
        kategoriYukle("sut_urunleri");

        if (buyukAcikEsyaGosterici != null) {
            buyukAcikEsyaGosterici.post(() -> {
                for (Yiyecek y : ei.yiyecekler) {
                    if (y.fizikselGorsel != null) {
                        y.fizikselGorsel.setVisibility(View.VISIBLE);
                        y.fizikselGorsel.bringToFront();
                        guncelleYiyecekBuyukKoor(y.fizikselGorsel, y.rafIndex, ei.type);
                        y.fizikselGorsel.setElevation(50f);
                        acilDurumAnimasyonuUygula(y.fizikselGorsel, y.tettTarihi);
                    }
                }
            });
        }
    }

    private void guncelleYiyecekBuyukKoor(ImageView gorsel, int rafIndex, String type) {
        if (buyukAcikEsyaGosterici == null) return;
        int yiyBoyut = (int) (40 * getResources().getDisplayMetrics().density);

        float basX = buyukAcikEsyaGosterici.getX(); float basY = buyukAcikEsyaGosterici.getY();
        float gen = buyukAcikEsyaGosterici.getWidth(); float yuk = buyukAcikEsyaGosterici.getHeight();

        int sutunSayisi = 2; float xOran = 0.21f; float xAralik = 0.50f; float yOran = 0.20f; float yAralik = 0.10f;

        if (type.equals("buzdolabi")) {
            sutunSayisi = 2; xOran = 0.28f; xAralik = 0.10f; yOran = 0.37f; yAralik = 0.15f;
        } else if (type.equals("dolap1")) {
            sutunSayisi = 4; xOran = 0.26f; xAralik = 0.14f; yOran = 0.40f; yAralik = 0.10f;
        } else if (type.equals("dolap2")) {
            sutunSayisi = 4; xOran = 0.22f; xAralik = 0.17f; yOran = 0.50f; yAralik = 0.10f;
        }

        int satir = rafIndex / sutunSayisi; int sutun = rafIndex % sutunSayisi;
        float hedefX = basX + (gen * (xOran + (sutun * xAralik))) - (yiyBoyut / 2f);
        float hedefY = basY + (yuk * (yOran + (satir * yAralik))) - (yiyBoyut / 2f);

        gorsel.setX(hedefX); gorsel.setY(hedefY);
    }

    private void esyayiEskiHalineDondur() {
        if (aktifTriggerView != null && aktifEsyaInstance != null) {
            if (buyukAcikEsyaGosterici != null) buyukAcikEsyaGosterici.setVisibility(View.GONE);
            for (Yiyecek y : aktifEsyaInstance.yiyecekler) {
                if (y.fizikselGorsel != null) {
                    y.fizikselGorsel.clearAnimation();
                    y.fizikselGorsel.setVisibility(View.GONE);
                }
            }
            aktifTriggerView = null; aktifEsyaInstance = null;
        }
        if (karartmaEkrani != null) karartmaEkrani.setVisibility(View.GONE);
        if (butonGrubu != null) butonGrubu.setVisibility(View.GONE);
        if (menuYiyeceklerLayout != null) menuYiyeceklerLayout.setVisibility(View.GONE);
        if (gridYiyecekler != null) gridYiyecekler.removeAllViews();
        tiklamaDurumu = 0;
    }

    private void kategoriYukle(String kategori) {
        if (gridYiyecekler == null) return; gridYiyecekler.removeAllViews();
        if (kategori.equals("sut_urunleri")) { yiyecekIkonuEkle(R.drawable.sut, "Süt", "1 Şişe", "₺35.00"); yiyecekIkonuEkle(R.drawable.peynir, "Peynir", "1 Kalıp", "₺120.00"); yiyecekIkonuEkle(R.drawable.yogurt, "Yoğurt", "1 Kova", "₺65.00"); yiyecekIkonuEkle(R.drawable.labne, "Labne", "1 Kg", "₺25.00");
        } else if (kategori.equals("sebzeler")) { yiyecekIkonuEkle(R.drawable.havuc, "Havuç", "1 Kg", "₺25.00"); yiyecekIkonuEkle(R.drawable.marul, "Marul", "1 Adet", "₺15.00"); yiyecekIkonuEkle(R.drawable.biber_c, "Californiya Biberi", "1 Kg", "₺25.00"); yiyecekIkonuEkle(R.drawable.enginar, "Enginar", "1 Adet", "₺15.00"); yiyecekIkonuEkle(R.drawable.mantar, "Mantar", "1 Kg", "₺25.00"); yiyecekIkonuEkle(R.drawable.pancar, "Pancar", "1 Kg", "₺25.00"); yiyecekIkonuEkle(R.drawable.patates, "Patates", "1 Adet", "₺15.00"); yiyecekIkonuEkle(R.drawable.salatalik, "Salatalık", "1 Kg", "₺25.00"); yiyecekIkonuEkle(R.drawable.sarimsak, "Sarımsak", "1 Adet", "₺15.00"); yiyecekIkonuEkle(R.drawable.zencefil, "Zencefil", "1 Kg", "₺25.00");
        } else if (kategori.equals("meyveler")) { yiyecekIkonuEkle(R.drawable.elma, "Elma", "1 Kg", "₺30.00"); yiyecekIkonuEkle(R.drawable.cilek, "Çilek", "1 Paket", "₺50.00"); yiyecekIkonuEkle(R.drawable.bogurtlen, "Böğürtlen", "1 Kg", "₺30.00"); yiyecekIkonuEkle(R.drawable.mandalina, "Mandalina", "1 Adet", "₺80.00"); yiyecekIkonuEkle(R.drawable.ananas, "Ananas", "1 Adet", "₺80.00");
        } else if (kategori.equals("et ürünleri")) { yiyecekIkonuEkle(R.drawable.antrikot, "Antrikot", "1 Kg", "₺25.00"); yiyecekIkonuEkle(R.drawable.et, "Et", "1 Adet", "₺15.00"); yiyecekIkonuEkle(R.drawable.kiyma, "Kıyma", "1 Adet", "₺15.00"); yiyecekIkonuEkle(R.drawable.sosis, "Sosis", "1 Kg", "₺25.00"); yiyecekIkonuEkle(R.drawable.tavuk_but, "Tavuk But", "1 Kg", "₺25.00");
        } else if (kategori.equals("Diğer yiyecekler")) { yiyecekIkonuEkle(R.drawable.nutella, "Nutella", "1 Kg", "₺25.00"); yiyecekIkonuEkle(R.drawable.tursu, "Turşu", "1 Kg", "₺25.00"); yiyecekIkonuEkle(R.drawable.ketcap, "Ketçap", "1 Adet", "₺15.00"); yiyecekIkonuEkle(R.drawable.mayonez, "Mayonez", "1 Adet", "₺15.00"); }
    }

    private void yiyecekIkonuEkle(int resId, String isim, String miktar, String fiyat) {
        if (gridYiyecekler == null) return;
        int kutuBoyut = (int) (65 * getResources().getDisplayMetrics().density); int margin = (int) (6 * getResources().getDisplayMetrics().density); int resimBoyut = (int) (45 * getResources().getDisplayMetrics().density);
        LinearLayout kutu = new LinearLayout(this); GridLayout.LayoutParams params = new GridLayout.LayoutParams(); params.width = kutuBoyut; params.height = kutuBoyut; params.setMargins(margin, margin, margin, margin); kutu.setLayoutParams(params); kutu.setPadding(8, 8, 8, 8); kutu.setBackgroundResource(R.drawable.kutu_beyaz_oval); kutu.setGravity(android.view.Gravity.CENTER);
        ImageView img = new ImageView(this); img.setImageResource(resId); img.setLayoutParams(new LinearLayout.LayoutParams(resimBoyut, resimBoyut)); img.setOnClickListener(v -> yiyecekTarihEkraniAc(resId, isim, miktar, fiyat)); kutu.addView(img); gridYiyecekler.addView(kutu);
    }

    private void bildirimKur(int benzersizId, String baslik, String mesaj, long zamanMilisaniye) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE); if (alarmManager == null) return;
        Intent intent = new Intent(this, BildirimAlici.class); intent.putExtra("baslik", baslik); intent.putExtra("mesaj", mesaj);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, benzersizId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { if (alarmManager.canScheduleExactAlarms()) { alarmManager.setExact(AlarmManager.RTC_WAKEUP, zamanMilisaniye, pendingIntent); } else { alarmManager.set(AlarmManager.RTC_WAKEUP, zamanMilisaniye, pendingIntent); } } else { alarmManager.setExact(AlarmManager.RTC_WAKEUP, zamanMilisaniye, pendingIntent); }
    }

    // ELDEN ÖZEL ÜRÜN EKLEME ---
    private void tazeBoardYiyecekEklemeAç() {
        tazeYiyecekDialogunuAç(R.drawable.heart_icon, "Özel Ürün", "1 Adet", "₺10.00");
    }

    private void tazeYiyecekDialogunuAç(int resId, String defaultIsim, String defaultMiktar, String defaultFiyat) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this); LayoutInflater inflater = getLayoutInflater();
        // dialog_ozel_tarih dosyasını çağırıyoruz
        View dialogView = inflater.inflate(R.layout.dialog_ozel_tarih, null);
        builder.setView(dialogView); AlertDialog dialog = builder.create();

        EditText editUrunAdi = dialogView.findViewById(R.id.editUrunAdi);
        EditText editMiktar = dialogView.findViewById(R.id.editMiktar);
        EditText editAcilmaTarihi = dialogView.findViewById(R.id.editAcilmaTarihi);
        EditText editTett = dialogView.findViewById(R.id.editTett);
        Button btnKaydetVeEkle = dialogView.findViewById(R.id.btnKaydetVeEkle);
        Calendar takvim = Calendar.getInstance();

        if (editAcilmaTarihi != null) {
            editAcilmaTarihi.setOnClickListener(v -> {
                DatePickerDialog dp = new DatePickerDialog(this, (view, y, m, d) -> editAcilmaTarihi.setText(d + "/" + (m + 1) + "/" + y), takvim.get(Calendar.YEAR), takvim.get(Calendar.MONTH), takvim.get(Calendar.DAY_OF_MONTH));
                dp.getDatePicker().setMaxDate(System.currentTimeMillis()); dp.show();
            });
        }

        if (editTett != null) {
            editTett.setOnClickListener(v -> {
                DatePickerDialog dp = new DatePickerDialog(this, (view, y, m, d) -> editTett.setText(d + "/" + (m + 1) + "/" + y), takvim.get(Calendar.YEAR), takvim.get(Calendar.MONTH), takvim.get(Calendar.DAY_OF_MONTH));
                dp.getDatePicker().setMinDate(System.currentTimeMillis() - 1000); dp.show();
            });
        }

        if (btnKaydetVeEkle != null) {
            btnKaydetVeEkle.setOnClickListener(v -> {
                if (editTett == null || editTett.getText().toString().isEmpty()) { Toast.makeText(this, "Lütfen TETT giriniz!", Toast.LENGTH_SHORT).show(); return; }

                String nihaiAd = (editUrunAdi != null && !editUrunAdi.getText().toString().trim().isEmpty()) ? editUrunAdi.getText().toString().trim() : defaultIsim;
                String nihaiMiktar = (editMiktar != null && !editMiktar.getText().toString().trim().isEmpty()) ? editMiktar.getText().toString().trim() : defaultMiktar;
                String nihaiAcilma = (editAcilmaTarihi != null && !editAcilmaTarihi.getText().toString().trim().isEmpty()) ? editAcilmaTarihi.getText().toString().trim() : "Açılmadı";
                String strTett = editTett.getText().toString();
                long benzersizId = System.currentTimeMillis();

                Yiyecek ozelYiyecek = new Yiyecek();
                ozelYiyecek.dbId = (int) benzersizId; ozelYiyecek.resimId = resId; ozelYiyecek.isim = nihaiAd;
                ozelYiyecek.acilmaTarihi = nihaiAcilma; ozelYiyecek.tettTarihi = strTett; ozelYiyecek.miktar = nihaiMiktar;
                ozelYiyecek.fiyat = defaultFiyat; ozelYiyecek.applianceInstanceId = -1L; ozelYiyecek.rafIndex = -1;

                new Thread(() -> {
                    YiyecekVarlik dbKayit = new YiyecekVarlik(); dbKayit.resimId = resId; dbKayit.isim = nihaiAd;
                    dbKayit.acilmaTarihi = nihaiAcilma; dbKayit.tettTarihi = strTett; dbKayit.miktar = nihaiMiktar;
                    dbKayit.fiyat = defaultFiyat; dbKayit.applianceInstanceId = -1L; dbKayit.rafIndex = -1;
                    db.yiyecekDao().yiyecekEkle(dbKayit);
                }).start();

                yiyecekEnvanteri.add(ozelYiyecek);
                Toast.makeText(this, nihaiAd + " panoya eklendi! ❤️", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }
        dialog.show();
    }

    // dialog_tarih
    private void yiyecekTarihEkraniAc(int yiyecekResimId, String yiyecekIsmi, String yiyecekMiktar, String yiyecekFiyat) {
        if (aktifEsyaInstance == null) return;
        boolean etUrunumu = yiyecekIsmi.equals("Et") || yiyecekIsmi.equals("Kıyma") || yiyecekIsmi.equals("Pastırma") || yiyecekIsmi.equals("Sosis") || yiyecekIsmi.contains("Tavuk") || yiyecekIsmi.equals("Antrikot");
        if (aktifEsyaInstance.type.contains("dolap") && etUrunumu) { new AlertDialog.Builder(this).setTitle("Eyvah! 🥩🚨").setMessage("Et ürünleri dolapta saklanmaz! Lütfen onları buzdolabına koy. 🥶").setPositiveButton("Tamam", null).show(); return; }

        AlertDialog.Builder builder = new AlertDialog.Builder(this); LayoutInflater inflater = getLayoutInflater();
        // ialog_tarih
        View dialogView = inflater.inflate(R.layout.dialog_tarih, null);
        builder.setView(dialogView); AlertDialog dialog = builder.create();

        EditText editMiktar = dialogView.findViewById(R.id.editMiktar);
        EditText editAcilmaTarihi = dialogView.findViewById(R.id.editAcilmaTarihi);
        EditText editTett = dialogView.findViewById(R.id.editTett);
        Button btnKaydetVeEkle = dialogView.findViewById(R.id.btnKaydetVeEkle);
        Calendar takvim = Calendar.getInstance();

        if (editMiktar != null) { editMiktar.setText(yiyecekMiktar); }

        if (editAcilmaTarihi != null) {
            editAcilmaTarihi.setOnClickListener(v -> { DatePickerDialog dp = new DatePickerDialog(this, (view, y, m, d) -> editAcilmaTarihi.setText(d + "/" + (m + 1) + "/" + y), takvim.get(Calendar.YEAR), takvim.get(Calendar.MONTH), takvim.get(Calendar.DAY_OF_MONTH)); dp.getDatePicker().setMaxDate(System.currentTimeMillis()); dp.show(); });
        }

        if (editTett != null) { editTett.setOnClickListener(v -> { DatePickerDialog dp = new DatePickerDialog(this, (view, y, m, d) -> editTett.setText(d + "/" + (m + 1) + "/" + y), takvim.get(Calendar.YEAR), takvim.get(Calendar.MONTH), takvim.get(Calendar.DAY_OF_MONTH)); dp.getDatePicker().setMinDate(System.currentTimeMillis() - 1000); dp.show(); }); }

        boolean uzunOmurluPaketliMi = yiyecekIsmi.equals("Ketçap") || yiyecekIsmi.equals("Mayonez") || yiyecekIsmi.equals("Turşu") || yiyecekIsmi.equals("Nutella");

        if (btnKaydetVeEkle != null) {
            btnKaydetVeEkle.setOnClickListener(v -> {
                if (editTett == null || editTett.getText().toString().isEmpty()) { Toast.makeText(this, "Lütfen TETT giriniz!", Toast.LENGTH_SHORT).show(); return; }
                int ilkBosRaf = 0; while (ilkBosRaf < aktifEsyaInstance.capacity && aktifEsyaInstance.rafDoluMu[ilkBosRaf]) { ilkBosRaf++; }
                final int rafSirasi = ilkBosRaf;

                String nihaiMiktar = (editMiktar != null && !editMiktar.getText().toString().trim().isEmpty()) ? editMiktar.getText().toString().trim() : yiyecekMiktar;
                String nihaiAcilma = (editAcilmaTarihi != null && !editAcilmaTarihi.getText().toString().trim().isEmpty()) ? editAcilmaTarihi.getText().toString().trim() : "Açılmadı";
                String strTett = editTett.getText().toString();
                final long hedefAppId = aktifEsyaInstance.instanceId; final String hedefType = aktifEsyaInstance.type;

                new Thread(() -> {
                    YiyecekVarlik dbKayit = new YiyecekVarlik(); dbKayit.resimId = yiyecekResimId; dbKayit.isim = yiyecekIsmi; dbKayit.acilmaTarihi = nihaiAcilma; dbKayit.tettTarihi = strTett; dbKayit.miktar = nihaiMiktar; dbKayit.fiyat = yiyecekFiyat; dbKayit.applianceInstanceId = hedefAppId; dbKayit.rafIndex = rafSirasi;
                    long yeniDbId = db.yiyecekDao().yiyecekEkle(dbKayit);
                    runOnUiThread(() -> {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy"); Date tettTarihi = sdf.parse(strTett); Date gercekBozulmaTarihi = tettTarihi;

                            if (!nihaiAcilma.equals("Açılmadı") && !uzunOmurluPaketliMi) {
                                Date acilmaTarihi = sdf.parse(nihaiAcilma); Calendar hesaplayici = Calendar.getInstance(); hesaplayici.setTime(acilmaTarihi);
                                if (yiyecekIsmi.equals("Süt") || yiyecekIsmi.equals("Yoğurt") || yiyecekIsmi.equals("Peynir") || yiyecekIsmi.equals("Labne")) hesaplayici.add(Calendar.DAY_OF_YEAR, 5);
                                else if (etUrunumu) hesaplayici.add(Calendar.DAY_OF_YEAR, 3);
                                if (hesaplayici.getTime().before(tettTarihi)) gercekBozulmaTarihi = hesaplayici.getTime();
                            }

                            Calendar bugun = Calendar.getInstance(); bugun.set(Calendar.HOUR_OF_DAY, 0); bugun.set(Calendar.MINUTE, 0); bugun.set(Calendar.SECOND, 0); bugun.set(Calendar.MILLISECOND, 0);
                            Calendar hedef = Calendar.getInstance(); hedef.setTime(gercekBozulmaTarihi); hedef.set(Calendar.HOUR_OF_DAY, 0); hedef.set(Calendar.MINUTE, 0); hedef.set(Calendar.SECOND, 0); hedef.set(Calendar.MILLISECOND, 0);

                            long kalanGun = (hedef.getTimeInMillis() - bugun.getTimeInMillis()) / (1000 * 60 * 60 * 24);

                            if (kalanGun <= 4 && kalanGun >= 0) {
                                new AlertDialog.Builder(this).setTitle("Dikkat! 🚨").setMessage(yiyecekIsmi + " bozulmasına tam " + kalanGun + " gün kaldı!").setPositiveButton("Tamam", null).show();
                            }
                            long birGunKala = gercekBozulmaTarihi.getTime() - (1L * 24L * 60L * 60L * 1000L); if (birGunKala > System.currentTimeMillis()) bildirimKur((int) yeniDbId, "Mutfaktan Uyarı! 🧑‍🍳", yiyecekIsmi + " için son gün!", birGunKala);
                        } catch (Exception e) { e.printStackTrace(); }

                        Yiyecek yeniVeri = new Yiyecek(); yeniVeri.dbId = (int) yeniDbId; yeniVeri.resimId = yiyecekResimId; yeniVeri.isim = yiyecekIsmi; yeniVeri.acilmaTarihi = nihaiAcilma; yeniVeri.tettTarihi = strTett; yeniVeri.miktar = nihaiMiktar; yeniVeri.fiyat = yiyecekFiyat; yeniVeri.applianceInstanceId = hedefAppId; yeniVeri.rafIndex = rafSirasi;

                        EsyaInstance guncelEI = esyaInstanceMap.get(hedefAppId);
                        if (guncelEI != null && rafSirasi < guncelEI.capacity) {
                            guncelEI.yiyecekler.add(yeniVeri); guncelEI.rafDoluMu[rafSirasi] = true;
                            ImageView fizikselGorsel = new ImageView(this); fizikselGorsel.setImageResource(yiyecekResimId); int yiyecekBoyut = (int) (40 * getResources().getDisplayMetrics().density); FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(yiyecekBoyut, yiyecekBoyut); fizikselGorsel.setLayoutParams(params);

                            fizikselGorsel.setElevation(50f);

                            if (tiklamaDurumu == 2 && buyukAcikEsyaGosterici != null && buyukAcikEsyaGosterici.getVisibility() == View.VISIBLE) {
                                fizikselGorsel.setVisibility(View.VISIBLE); fizikselGorsel.bringToFront();
                                guncelleYiyecekBuyukKoor(fizikselGorsel, rafSirasi, hedefType);
                                acilDurumAnimasyonuUygula(fizikselGorsel, strTett);
                            } else { fizikselGorsel.setVisibility(View.GONE); }

                            if (odaCercevesi != null) odaCercevesi.addView(fizikselGorsel); yeniVeri.fizikselGorsel = fizikselGorsel; fizikselGorsel.setOnClickListener(imgV -> tatliYiyecekBilgiKartiniAc(yeniVeri));
                            Toast.makeText(this, "Başarıyla eklendi!", Toast.LENGTH_SHORT).show();
                        } else { Toast.makeText(this, "Dolap dolu! Pano listesine eklendi.", Toast.LENGTH_LONG).show(); }
                        yiyecekEnvanteri.add(yeniVeri);
                    });
                }).start();
                dialog.dismiss();
            });
        }
        dialog.show();
    }

    private void tatliPanoAc() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this); View dialogView = getLayoutInflater().inflate(R.layout.dialog_envanter_board, null); builder.setView(dialogView); AlertDialog dialog = builder.create(); dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        LinearLayout listeKutusu = dialogView.findViewById(R.id.boardListeKutusu); TextView btnBoardKapat = dialogView.findViewById(R.id.btnBoardKapat);
        if(listeKutusu != null) {
            for (Yiyecek y : yiyecekEnvanteri) {
                View satir = getLayoutInflater().inflate(R.layout.item_envanter_satir, null);
                ImageView ikon = satir.findViewById(R.id.imgSatirIkon); TextView txtIsim = satir.findViewById(R.id.txtSatirIsim); TextView txtDetay = satir.findViewById(R.id.txtSatirDetay); TextView txtFiyat = satir.findViewById(R.id.txtSatirFiyat); TextView btnRafaKoy = satir.findViewById(R.id.btnSatirRafaKoy);
                if(ikon != null) ikon.setImageResource(y.resimId); if(txtIsim != null) txtIsim.setText(y.isim); if(txtDetay != null) txtDetay.setText("Miktar: " + y.miktar); if(txtFiyat != null) txtFiyat.setText(y.fiyat);
                if (btnRafaKoy != null) {
                    if (y.applianceInstanceId != -1) { btnRafaKoy.setVisibility(View.GONE); } else {
                        btnRafaKoy.setVisibility(View.VISIBLE);
                        btnRafaKoy.setOnClickListener(v -> {
                            EsyaInstance musaitDolap = null; int musaitRaf = -1;
                            for (EsyaInstance ei : esyaInstanceMap.values()) { if (!ei.isStorage) continue; for (int i = 0; i < ei.capacity; i++) { if (!ei.rafDoluMu[i]) { musaitDolap = ei; musaitRaf = i; break; } } if (musaitDolap != null) break; }
                            if (musaitDolap != null) {
                                musaitDolap.rafDoluMu[musaitRaf] = true; musaitDolap.yiyecekler.add(y); y.applianceInstanceId = musaitDolap.instanceId; y.rafIndex = musaitRaf;
                                ImageView fizikselGorsel = new ImageView(this); fizikselGorsel.setImageResource(y.resimId); int yiyecekBoyut = (int) (40 * getResources().getDisplayMetrics().density); FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(yiyecekBoyut, yiyecekBoyut); fizikselGorsel.setLayoutParams(params);

                                fizikselGorsel.setElevation(50f);

                                if (tiklamaDurumu == 2 && buyukAcikEsyaGosterici != null && buyukAcikEsyaGosterici.getVisibility() == View.VISIBLE) {
                                    fizikselGorsel.setVisibility(View.VISIBLE); fizikselGorsel.bringToFront();
                                    guncelleYiyecekBuyukKoor(fizikselGorsel, musaitRaf, musaitDolap.type);
                                    acilDurumAnimasyonuUygula(fizikselGorsel, y.tettTarihi);
                                } else { fizikselGorsel.setVisibility(View.GONE); }

                                if (odaCercevesi != null) odaCercevesi.addView(fizikselGorsel); y.fizikselGorsel = fizikselGorsel; fizikselGorsel.setOnClickListener(imgV -> tatliYiyecekBilgiKartiniAc(y));
                                final long finalAppId = y.applianceInstanceId; final int finalRaf = y.rafIndex;
                                new Thread(() -> { YiyecekVarlik g = new YiyecekVarlik(); g.id = y.dbId; g.resimId = y.resimId; g.isim = y.isim; g.acilmaTarihi = y.acilmaTarihi; g.tettTarihi = y.tettTarihi; g.miktar = y.miktar; g.fiyat = y.fiyat; g.applianceInstanceId = finalAppId; g.rafIndex = finalRaf; db.yiyecekDao().yiyecekGuncelle(g); }).start();
                                Toast.makeText(this, "Rafa yerleştirildi!", Toast.LENGTH_SHORT).show(); dialog.dismiss();
                            } else { Toast.makeText(this, "Mutfaktaki hiçbir dolapta boş yer yok!", Toast.LENGTH_SHORT).show(); }
                        });
                    }
                }
                listeKutusu.addView(satir);
            }
        }
        if(btnBoardKapat != null) btnBoardKapat.setOnClickListener(v -> dialog.dismiss()); dialog.show();
    }

    private void tatliYiyecekBilgiKartiniAc(Yiyecek yiyecekVerisi) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this); View dialogView = getLayoutInflater().inflate(R.layout.dialog_yiyecek_bilgi, null); builder.setView(dialogView); AlertDialog dialog = builder.create(); dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        ImageView imgIkon = dialogView.findViewById(R.id.imgBilgiIkon); TextView txtBaslikUrunAdi = dialogView.findViewById(R.id.txtBilgiBaslikUrunAdi); TextView txtAcilma = dialogView.findViewById(R.id.txtBilgiAcilma); TextView txtTett = dialogView.findViewById(R.id.txtBilgiTett); Button btnKapat = dialogView.findViewById(R.id.btnBilgiKapat); Button btnSil = dialogView.findViewById(R.id.btnBilgiSil); Button btnTuket = dialogView.findViewById(R.id.btnBilgiTuket); TextView txtMiktar = dialogView.findViewById(R.id.txtBilgiMiktar);
        if (txtBaslikUrunAdi != null) { txtBaslikUrunAdi.setText(yiyecekVerisi.isim); }
        if(txtMiktar != null) {
            txtMiktar.setText("Miktar: " + yiyecekVerisi.miktar + " (✏️)");
            txtMiktar.setOnClickListener(v -> {
                AlertDialog.Builder b = new AlertDialog.Builder(this); final EditText input = new EditText(this); input.setInputType(InputType.TYPE_CLASS_TEXT); input.setText(yiyecekVerisi.miktar); b.setTitle("Miktarı Güncelle").setView(input);
                b.setPositiveButton("Kaydet", (d, which) -> { String yeniMiktar = input.getText().toString(); yiyecekVerisi.miktar = yeniMiktar; txtMiktar.setText("Miktar: " + yeniMiktar + " (✏️)"); new Thread(() -> { YiyecekVarlik g = new YiyecekVarlik(); g.id = yiyecekVerisi.dbId; g.resimId = yiyecekVerisi.resimId; g.isim = yiyecekVerisi.isim; g.acilmaTarihi = yiyecekVerisi.acilmaTarihi; g.tettTarihi = yiyecekVerisi.tettTarihi; g.miktar = yeniMiktar; g.fiyat = yiyecekVerisi.fiyat; g.applianceInstanceId = yiyecekVerisi.applianceInstanceId; g.rafIndex = yiyecekVerisi.rafIndex; db.yiyecekDao().yiyecekGuncelle(g); }).start(); Toast.makeText(this, "Miktar güncellendi!", Toast.LENGTH_SHORT).show(); }).setNegativeButton("İptal", null).show();
            });
        }
        if(btnTuket != null && txtMiktar != null) {
            btnTuket.setOnClickListener(v -> {
                try { String miktarStr = yiyecekVerisi.miktar.trim(); String[] parcalar = miktarStr.split(" "); int sayi = Integer.parseInt(parcalar[0]); if (sayi > 1) { sayi--; String yeniMiktar = sayi + (parcalar.length > 1 ? " " + parcalar[1] : ""); yiyecekVerisi.miktar = yeniMiktar; txtMiktar.setText("Miktar: " + yeniMiktar + " (✏️)"); new Thread(() -> { YiyecekVarlik g = new YiyecekVarlik(); g.id = yiyecekVerisi.dbId; g.resimId = yiyecekVerisi.resimId; g.isim = yiyecekVerisi.isim; g.acilmaTarihi = yiyecekVerisi.acilmaTarihi; g.tettTarihi = yiyecekVerisi.tettTarihi; g.miktar = yeniMiktar; g.fiyat = yiyecekVerisi.fiyat; g.applianceInstanceId = yiyecekVerisi.applianceInstanceId; g.rafIndex = yiyecekVerisi.rafIndex; db.yiyecekDao().yiyecekGuncelle(g); }).start(); Toast.makeText(this, "Afiyet olsun! 1 birim tüketildi.", Toast.LENGTH_SHORT).show(); } else { if (btnSil != null) btnSil.performClick(); Toast.makeText(this, "Ürün tamamen bitti!", Toast.LENGTH_SHORT).show(); } } catch (Exception e) { Toast.makeText(this, "Miktar sayısal değil, lütfen kalemle güncelleyin.", Toast.LENGTH_SHORT).show(); }
            });
        }
        if(imgIkon != null) imgIkon.setImageResource(yiyecekVerisi.resimId); if(txtTett != null) txtTett.setText("TETT: " + yiyecekVerisi.tettTarihi);
        if(txtAcilma != null) {
            if (yiyecekVerisi.acilmaTarihi.equals("Açılmadı")) { txtAcilma.setText("Açılış Tarihi: Açılmadı (✏️)"); txtAcilma.setTextColor(Color.parseColor("#2E7D32")); } else { txtAcilma.setText("Açılış Tarihi: " + yiyecekVerisi.acilmaTarihi + " (✏️)"); }
            txtAcilma.setOnClickListener(v -> { takvimEkraniAç(yiyecekVerisi, txtAcilma); });
        }
        if(btnKapat != null) btnKapat.setOnClickListener(v -> dialog.dismiss());
        if(btnSil != null) {
            btnSil.setOnClickListener(v -> {
                EsyaInstance bagliEI = esyaInstanceMap.get(yiyecekVerisi.applianceInstanceId); if (bagliEI != null && yiyecekVerisi.rafIndex >= 0) { bagliEI.rafDoluMu[yiyecekVerisi.rafIndex] = false; bagliEI.yiyecekler.remove(yiyecekVerisi); }
                new Thread(() -> { YiyecekVarlik sil = new YiyecekVarlik(); sil.id = yiyecekVerisi.dbId; db.yiyecekDao().yiyecekSil(sil); }).start();
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE); Intent intent = new Intent(this, BildirimAlici.class); PendingIntent pendingIntent = PendingIntent.getBroadcast(this, yiyecekVerisi.dbId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE); if(alarmManager != null) alarmManager.cancel(pendingIntent);
                if(yiyecekVerisi.fizikselGorsel != null) { yiyecekVerisi.fizikselGorsel.clearAnimation(); if (odaCercevesi != null) odaCercevesi.removeView(yiyecekVerisi.fizikselGorsel); } yiyecekEnvanteri.remove(yiyecekVerisi); Toast.makeText(this, "Çöpe atıldı.", Toast.LENGTH_SHORT).show(); dialog.dismiss();
            });
        }
        dialog.show();
    }

    private void takvimEkraniAç(Yiyecek yiyecekVerisi, TextView txtAcilma) {
        Calendar takvim = Calendar.getInstance(); DatePickerDialog dp = new DatePickerDialog(this, (view, y, m, d) -> { String yeniTarih = d + "/" + (m + 1) + "/" + y; yiyecekVerisi.acilmaTarihi = yeniTarih; txtAcilma.setText("Açılış Tarihi: " + yeniTarih + " (✏️)"); txtAcilma.setTextColor(Color.parseColor("#3E2723")); new Thread(() -> { YiyecekVarlik g = new YiyecekVarlik(); g.id = yiyecekVerisi.dbId; g.resimId = yiyecekVerisi.resimId; g.isim = yiyecekVerisi.isim; g.acilmaTarihi = yeniTarih; g.tettTarihi = yiyecekVerisi.tettTarihi; g.miktar = yiyecekVerisi.miktar; g.fiyat = yiyecekVerisi.fiyat; g.applianceInstanceId = yiyecekVerisi.applianceInstanceId; g.rafIndex = yiyecekVerisi.rafIndex; db.yiyecekDao().yiyecekGuncelle(g); }).start(); try { SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy"); Date tettTarihi = sdf.parse(yiyecekVerisi.tettTarihi); Date acilmaTarihi = sdf.parse(yeniTarih); Calendar hesaplayici = Calendar.getInstance(); hesaplayici.setTime(acilmaTarihi); boolean etUrunumu = yiyecekVerisi.isim.equals("Et") || yiyecekVerisi.isim.equals("Kıyma") || yiyecekVerisi.isim.equals("Pastırma") || yiyecekVerisi.isim.equals("Sosis") || yiyecekVerisi.isim.contains("Tavuk") || yiyecekVerisi.isim.equals("Antrikot"); boolean uzunOmurluPaketliMi = yiyecekVerisi.isim.equals("Ketçap") || yiyecekVerisi.isim.equals("Mayonez") || yiyecekVerisi.isim.equals("Turşu") || yiyecekVerisi.isim.equals("Nutella"); Date gercekBozulmaTarihi = tettTarihi; if (!uzunOmurluPaketliMi) { if (yiyecekVerisi.isim.equals("Süt") || yiyecekVerisi.isim.equals("Yoğurt") || yiyecekVerisi.isim.equals("Peynir") || yiyecekVerisi.isim.equals("Labne")) hesaplayici.add(Calendar.DAY_OF_YEAR, 5); else if (etUrunumu) hesaplayici.add(Calendar.DAY_OF_YEAR, 3); if (hesaplayici.getTime().before(tettTarihi)) gercekBozulmaTarihi = hesaplayici.getTime(); } Calendar bugun = Calendar.getInstance(); bugun.set(Calendar.HOUR_OF_DAY, 0); bugun.set(Calendar.MINUTE, 0); bugun.set(Calendar.SECOND, 0); bugun.set(Calendar.MILLISECOND, 0); Calendar hedef = Calendar.getInstance(); hedef.setTime(gercekBozulmaTarihi); hedef.set(Calendar.HOUR_OF_DAY, 0); hedef.set(Calendar.MINUTE, 0); hedef.set(Calendar.SECOND, 0); hedef.set(Calendar.MILLISECOND, 0); long kalanGun = (hedef.getTimeInMillis() - bugun.getTimeInMillis()) / (1000 * 60 * 60 * 24); if (kalanGun <= 4 && kalanGun >= 0) Toast.makeText(this, "Dikkat! Ömrü kısaldı. " + kalanGun + " gün kaldı!", Toast.LENGTH_LONG).show(); long birGunKala = gercekBozulmaTarihi.getTime() - (1L * 24L * 60L * 60L * 1000L); if (birGunKala > System.currentTimeMillis()) bildirimKur(yiyecekVerisi.dbId, "Mutfaktan Uyarı! 🧑‍🍳", yiyecekVerisi.isim + " için son gün!", birGunKala); } catch (Exception e) { e.printStackTrace(); } Toast.makeText(this, "Açılma tarihi başarıyla güncellendi!", Toast.LENGTH_SHORT).show(); }, takvim.get(Calendar.YEAR), takvim.get(Calendar.MONTH), takvim.get(Calendar.DAY_OF_MONTH)); dp.getDatePicker().setMaxDate(System.currentTimeMillis()); dp.show();
    }
}
