package com.example.proje;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "esya_tablosu")
public class EsyaVarlik {

    @PrimaryKey
    @NonNull
    public String esyaIsmi = ""; // hata buradaki @NonNull ile çözüldü

    public float positionX;
    public float positionY;
    public String tagStatus;
}
