package com.damsky.danny.libremusic.data.db.model;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "LINKS".
*/
public class LinkDao extends AbstractDao<Link, Long> {

    public static final String TABLENAME = "LINKS";

    /**
     * Properties of entity Link.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property PlayListId = new Property(1, Long.class, "playListId", false, "PLAY_LIST_ID");
        public final static Property SongId = new Property(2, Long.class, "songId", false, "SONG_ID");
    }


    public LinkDao(DaoConfig config) {
        super(config);
    }
    
    public LinkDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"LINKS\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                "\"PLAY_LIST_ID\" INTEGER," + // 1: playListId
                "\"SONG_ID\" INTEGER);"); // 2: songId
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"LINKS\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, Link entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
 
        Long playListId = entity.getPlayListId();
        if (playListId != null) {
            stmt.bindLong(2, playListId);
        }
 
        Long songId = entity.getSongId();
        if (songId != null) {
            stmt.bindLong(3, songId);
        }
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, Link entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
 
        Long playListId = entity.getPlayListId();
        if (playListId != null) {
            stmt.bindLong(2, playListId);
        }
 
        Long songId = entity.getSongId();
        if (songId != null) {
            stmt.bindLong(3, songId);
        }
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    @Override
    public Link readEntity(Cursor cursor, int offset) {
        Link entity = new Link( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.isNull(offset + 1) ? null : cursor.getLong(offset + 1), // playListId
            cursor.isNull(offset + 2) ? null : cursor.getLong(offset + 2) // songId
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, Link entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setPlayListId(cursor.isNull(offset + 1) ? null : cursor.getLong(offset + 1));
        entity.setSongId(cursor.isNull(offset + 2) ? null : cursor.getLong(offset + 2));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(Link entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(Link entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(Link entity) {
        return entity.getId() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
}
