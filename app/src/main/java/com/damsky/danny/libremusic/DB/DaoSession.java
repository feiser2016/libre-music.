package com.damsky.danny.libremusic.DB;

import java.util.Map;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.AbstractDaoSession;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.identityscope.IdentityScopeType;
import org.greenrobot.greendao.internal.DaoConfig;

import com.damsky.danny.libremusic.DB.Song;
import com.damsky.danny.libremusic.DB.Artist;
import com.damsky.danny.libremusic.DB.Album;

import com.damsky.danny.libremusic.DB.SongDao;
import com.damsky.danny.libremusic.DB.ArtistDao;
import com.damsky.danny.libremusic.DB.AlbumDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see org.greenrobot.greendao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig songDaoConfig;
    private final DaoConfig artistDaoConfig;
    private final DaoConfig albumDaoConfig;

    private final SongDao songDao;
    private final ArtistDao artistDao;
    private final AlbumDao albumDao;

    public DaoSession(Database db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        songDaoConfig = daoConfigMap.get(SongDao.class).clone();
        songDaoConfig.initIdentityScope(type);

        artistDaoConfig = daoConfigMap.get(ArtistDao.class).clone();
        artistDaoConfig.initIdentityScope(type);

        albumDaoConfig = daoConfigMap.get(AlbumDao.class).clone();
        albumDaoConfig.initIdentityScope(type);

        songDao = new SongDao(songDaoConfig, this);
        artistDao = new ArtistDao(artistDaoConfig, this);
        albumDao = new AlbumDao(albumDaoConfig, this);

        registerDao(Song.class, songDao);
        registerDao(Artist.class, artistDao);
        registerDao(Album.class, albumDao);
    }
    
    public void clear() {
        songDaoConfig.clearIdentityScope();
        artistDaoConfig.clearIdentityScope();
        albumDaoConfig.clearIdentityScope();
    }

    public SongDao getSongDao() {
        return songDao;
    }

    public ArtistDao getArtistDao() {
        return artistDao;
    }

    public AlbumDao getAlbumDao() {
        return albumDao;
    }

}
