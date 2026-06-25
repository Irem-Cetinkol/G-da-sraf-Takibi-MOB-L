package com.example.proje;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface YiyecekDao {
    @Insert
    long yiyecekEkle(YiyecekVarlik yiyecek);

    @Delete
    void yiyecekSil(YiyecekVarlik yiyecek);

    @Update
    void yiyecekGuncelle(YiyecekVarlik yiyecek);

    @Query("SELECT * FROM yiyecek_tablosu")
    List<YiyecekVarlik> tumYiyecekleriGetir();

    // --- HATA BURADA ÇÖZÜLDÜ: Sütun adını yeni mimariye (applianceInstanceId) göre uyarladık ---
    @Query("DELETE FROM yiyecek_tablosu WHERE applianceInstanceId = :esyaId")
    void esyayaAitYiyecekleriSil(String esyaId);
}
