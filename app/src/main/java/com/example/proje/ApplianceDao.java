package com.example.proje;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ApplianceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long applianceKaydet(ApplianceVarlik appliance); //yeni buzdolabı-dolap eklenirse

    @Update
    void applianceGuncelle(ApplianceVarlik appliance); //konumu değişirse

    @Query("DELETE FROM appliance_tablosu WHERE id = :avId")
    void applianceSilId(long avId);

    @Query("SELECT * FROM appliance_tablosu")
    List<ApplianceVarlik> tumAppliancelariGetir();
}
