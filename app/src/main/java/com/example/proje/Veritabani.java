package com.example.proje;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// entities kısmına ApplianceVarlik.class ekledik, version'ı up yaptık
@Database(entities = {YiyecekVarlik.class, ApplianceVarlik.class}, version = 2, exportSchema = false)
public abstract class Veritabani extends RoomDatabase {
    public abstract YiyecekDao yiyecekDao();
    public abstract ApplianceDao applianceDao();

    private static volatile Veritabani INSTANCE;

    public static Veritabani getVeritabani(final Context context) {
        if (INSTANCE == null) {
            synchronized (Veritabani.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    Veritabani.class, "mutfak_veritabani")
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration() // Basit migrate için tabloları uçurur (sorun olmaz)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
