package com.example.myapp.ui.chat.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;


public class DBHelper extends SQLiteOpenHelper {

    private final static String DB_NAME = "info.db";
    private static int VERSION = 1;

    private static final String createChatTB = "" +
            "create table if not exists Chat(" +
            "id integer primary key autoincrement,"+
            "isRcv boolean not null," +
            "targetId integer not null," +
            "createTime timestamp not null default CURRENT_TIMESTAMP," +
            "message varchar(100) not null);";

    public DBHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public DBHelper(Context context){
        super(context, DB_NAME, null, VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createChatTB);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
