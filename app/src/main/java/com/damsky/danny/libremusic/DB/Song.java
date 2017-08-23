package com.damsky.danny.libremusic.DB;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Generated;

@Entity (nameInDb = "SONGS")
public class Song {
    @Id (autoincrement = true)
    private Long id;

    @NotNull
    private String data;

    @NotNull
    private String title;

    @NotNull
    private String album;

    @NotNull
    private String artist;

    @NotNull
    private int track;

    @NotNull
    private int year;

    @NotNull
    private int starttime;

    @NotNull
    private int endtime;

    @NotNull
    private int duration;

    @NotNull
    private String cover;

    @Generated(hash = 253332579)
    public Song(Long id, @NotNull String data, @NotNull String title,
            @NotNull String album, @NotNull String artist, int track, int year,
            int starttime, int endtime, int duration, @NotNull String cover) {
        this.id = id;
        this.data = data;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.track = track;
        this.year = year;
        this.starttime = starttime;
        this.endtime = endtime;
        this.duration = duration;
        this.cover = cover;
    }

    @Generated(hash = 87031450)
    public Song() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getData() {
        return this.data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return this.album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return this.artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getTrack() {
        return this.track;
    }

    public void setTrack(int track) {
        this.track = track;
    }

    public int getYear() {
        return this.year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getStarttime() {
        return this.starttime;
    }

    public void setStarttime(int starttime) {
        this.starttime = starttime;
    }

    public int getEndtime() {
        return this.endtime;
    }

    public void setEndtime(int endtime) {
        this.endtime = endtime;
    }

    public int getDuration() {
        return this.duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getCover() {
        return this.cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }
}
