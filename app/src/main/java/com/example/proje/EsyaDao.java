package com.example.proje;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface EsyaDao {
    // Aynı eşya varsa üzerine yazmak için (prmary key)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void esyaKaydet(EsyaVarlik esya);

    @Query("SELECT * FROM esya_tablosu WHERE esyaIsmi = :esyaIsmi LIMIT 1")
    EsyaVarlik esyaGetir(String esyaIsmi);
}
