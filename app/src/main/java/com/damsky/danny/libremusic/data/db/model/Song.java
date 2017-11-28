package com.damsky.danny.libremusic.data.db.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;

@Entity(nameInDb = "SONGS")
public class Song {
    @Id(autoincrement = true)
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
    private String genre;

    @NotNull
    private int track;

    @NotNull
    private int year;

    @NotNull
    private int startTime;

    @NotNull
    private int endTime;

    @NotNull
    private int duration;

    @NotNull
    private String cover;

    @Generated(hash = 397393521)
    public Song(Long id, @NotNull String data, @NotNull String title,
                @NotNull String album, @NotNull String artist, @NotNull String genre,
                int track, int year, int startTime, int endTime, int duration,
                @NotNull String cover) {
        this.id = id;
        this.data = data;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.genre = genre;
        this.track = track;
        this.year = year;
        this.startTime = startTime;
        this.endTime = endTime;
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

    public String getGenre() {
        return this.genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
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

    public int getStartTime() {
        return this.startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getEndTime() {
        return this.endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
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
