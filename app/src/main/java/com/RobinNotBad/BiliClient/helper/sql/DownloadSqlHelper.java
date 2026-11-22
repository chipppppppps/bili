package com.RobinNotBad.BiliClient.helper.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.RobinNotBad.BiliClient.util.MsgUtil;

public class DownloadSqlHelper extends SQLiteOpenHelper {
    public DownloadSqlHelper(@Nullable Context context) {
        super(context, "download.db", null, 5);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table download(id INTEGER primary key autoincrement," +
                "type TEXT," +
                "state TEXT," +
                "aid BIGINT," +
                "cid BIGINT," +
                "qn INTEGER," +
                "title TEXT," +
                "child TEXT," +
                "cover TEXT," +
                "download_type TEXT DEFAULT 'video'," + // 下载类型：video(完整视频), audio_only(仅音频)
                "audio_url TEXT," + // 音频流URL
                "video_type TEXT DEFAULT 'video')"); // 视频类型：video(普通视频), bangumi(番剧)
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion)
            try {
                // 兼容升级：从版本3升级，添加下载类型/音频流字段
                if (oldVersion == 3 && newVersion >= 4) {
                    db.execSQL("ALTER TABLE download ADD COLUMN download_type TEXT DEFAULT 'video'");
                    db.execSQL("ALTER TABLE download ADD COLUMN audio_url TEXT");
                    oldVersion = 4;
                }
                // 兼容升级：从版本4升级，添加视频类型字段（用于区分番剧缓存）
                if (oldVersion == 4 && newVersion >= 5) {
                    db.execSQL("ALTER TABLE download ADD COLUMN video_type TEXT DEFAULT 'video'");
                    oldVersion = 5;
                }
                if (oldVersion != newVersion) {
                    // 其他情况重建表
                    db.execSQL("drop table if exists download");
                    onCreate(db);
                }
            } catch (Throwable e) {
                MsgUtil.err(e);
            }
    }
}
