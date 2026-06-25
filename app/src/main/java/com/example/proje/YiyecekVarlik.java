package com.example.proje;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "yiyecek_tablosu")
public class YiyecekVarlik {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int resimId;
    public String isim;
    public String acilmaTarihi;
    public String tettTarihi;
    public String miktar;
    public String fiyat;

    // hangi eşya instanceında (-1 ise sadece panoda bekliyor)
    public Long applianceInstanceId;
    // Eşyanın kaçıncı raf-sütun noktasında (0-5 veya 0-8)
    public int rafIndex;  // O eşyanın içindeki matris noktası
}

