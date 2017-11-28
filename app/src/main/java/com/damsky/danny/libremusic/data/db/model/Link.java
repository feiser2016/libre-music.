package com.damsky.danny.libremusic.data.db.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

@Entity(nameInDb = "LINKS")
public class Link {
    @Id(autoincrement = true)
    private Long id;

    private Long playListId;
    private Long songId;

    @Generated(hash = 485006021)
    public Link(Long id, Long playListId, Long songId) {
        this.id = id;
        this.playListId = playListId;
        this.songId = songId;
    }

    @Generated(hash = 225969300)
    public Link() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlayListId() {
        return this.playListId;
    }

    public void setPlayListId(Long playListId) {
        this.playListId = playListId;
    }

    public Long getSongId() {
        return this.songId;
    }

    public void setSongId(Long songId) {
        this.songId = songId;
    }
}
