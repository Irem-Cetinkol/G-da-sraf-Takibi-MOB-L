package com.example.proje;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "appliance_tablosu")  //veritabanı tablosuna dönüştürülmeli
public class ApplianceVarlik {
    @PrimaryKey(autoGenerate = true)
    public Long id; // Unique ID

    public String type; // hangi eşya olduğunu anlamamız için
    public float positionX; // Eşyanın yeri
    public float positionY;
    public String tagStatus; // sabit mi wareketli mi
}
