/*
 *  Copyright (c) 2014  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class Database {

    public File directory;
    public String filename;
    public List<Artist> artist_list;
    public List<Album> album_list;
    public List<Song> song_list;
    public boolean is_changed;

    public static class JsonSong {
        String filename;
        int mtime;
        String title;
        String artist;
        int year;
        String album;
        String album_artist;
        int track_number;
        int duration;
        String bitrate;
        int add_time;
    }

    /**
     * Create a new Database instance, for loading from a JSON file, or adding songs from mp3 files.
     */
    public Database() {
        // There is no artist #0, album #0, or song #0.
        this.artist_list = new ArrayList<>();
        this.artist_list.add(new Artist());
        this.album_list = new ArrayList<>();
        this.album_list.add(new Album());
        this.song_list = new ArrayList<>();
        this.song_list.add(new Song());
    }

    /**
     * Read a song database from a JSON file.
     */
    public boolean load(File directory, String filename) {
        this.directory = directory;
        this.filename = filename;
        File file = new File(directory, filename);
        JsonArray songArray;
        try {
            FileReader reader = new FileReader(file);
            JsonElement root = JsonParser.parseReader(reader);
            reader.close();
            songArray = root.getAsJsonArray();
        } catch (Exception exception) {
            System.err.println(file.toString() + ": " + exception);
            return false;
        }
        Gson gson = new Gson();
        for (JsonElement element : songArray) {
            JsonSong obj = gson.fromJson(element, JsonSong.class);
            if (obj.filename == null) {
                continue;
            }
            add_song(obj);
        }
        this.is_changed = false;
        return true;
    }

    public void add_song(JsonSong obj) {
        Song song = new Song();
        song.id = this.song_list.size();
        song.filename = obj.filename;
        song.mtime = obj.mtime;
        song.title = obj.title;
        song.artistid = get_artistid(obj.artist);
        song.year = obj.year;
        int artistid = get_artistid(obj.album_artist);
        if (artistid == 0) {
            artistid = song.artistid;
        }
        song.albumid = get_albumid(obj.album, artistid, song.year);
        song.track_number = obj.track_number;
        song.duration = obj.duration;
        song.bitrate = obj.bitrate;
        song.add_time = obj.add_time;
        this.song_list.add(song);
        add_song_to_album(song);
        this.is_changed = true;
    }

    /**
     * Get the artist's ID or add the artist to the database.
     */
    public int get_artistid(String name) {
        if (name == null) {
            return 0;
        }
        int num_artists = this.artist_list.size();
        for (int idx = 1; idx < num_artists; idx++) {
            if (this.artist_list.get(idx).name.equals(name)) {
                return idx;
            }
        }
        Artist artist = new Artist();
        artist.id = num_artists;
        artist.name = name;
        this.artist_list.add(artist);
        return artist.id;
    }

    /**
     * Get the album's ID or add the album to the database. If the album is a "Greatest Hits"
     * collection, then the release date of the album is the date of its newest song.
     */
    private int get_albumid(String name, int artistid, int year) {
        if (name == null) {
            name = "";
        }
        int num_albums = this.album_list.size();
        for (int idx = 1; idx < num_albums; idx++) {
            Album album = this.album_list.get(idx);
            if ((album.artistid == artistid) && album.name.equals(name)) {
                if (year > album.year) {
                    album.year = year;
                }
                return idx;
            }
        }
        Album album = new Album();
        album.id = num_albums;
        album.name = name;
        album.artistid = artistid;
        album.year = year;
        this.album_list.add(album);
        return album.id;
    }

    /**
     * Insertion sort song into album's list of songs.
     */
    private void add_song_to_album(Song song) {
        Album album = this.album_list.get(song.albumid);
        int size = album.song_list == null ? 0 : album.song_list.length;
        int[] new_list = new int[size + 1];
        if (size > 0) {
            System.arraycopy(album.song_list, 0, new_list, 0, size);
        }
        new_list[size] = song.id;
        for (int idx = new_list.length - 2; idx >= 0; idx--) {
            Song other = this.song_list.get(new_list[idx]);
            if (other.track_number <= song.track_number) {
                break;
            }
            new_list[idx + 1] = other.id;
            new_list[idx] = song.id;
        }
        album.song_list = new_list;
    }

    /**
     * Overwrite the database file with the current contents of the database.
     */
    public void save() {
        if (!this.is_changed) {
            return;
        }
        int num_songs = this.song_list.size();
        List<JsonSong> songArray = new ArrayList<>(num_songs);
        for (Song song : this.song_list) {
            if (song.filename == null) {
                continue;
            }
            JsonSong obj = new JsonSong();
            obj.filename = song.filename;
            obj.mtime = song.mtime;
            obj.title = song.title;
            obj.artist = this.artist_list.get(song.artistid).name;
            obj.album = this.album_list.get(song.albumid).name;
            int artistid = this.album_list.get(song.albumid).artistid;
            if ((artistid > 0) && (artistid != song.artistid)) {
                obj.album_artist = this.artist_list.get(artistid).name;
            }
            obj.track_number = song.track_number;
            obj.year = song.year;
            obj.duration = song.duration;
            obj.bitrate = song.bitrate;
            obj.add_time = song.add_time;
            songArray.add(obj);
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String text = gson.toJson(songArray);
        songArray = null;
        File file = new File(this.directory, this.filename);
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(text);
            writer.close();
            this.is_changed = false;
        } catch (Exception exception) {
            System.err.println(file.toString() + ": " + exception);
        }
        Utils.quietClose(writer);
    }

    public void delete_album(int albumid) {
        Album album = this.album_list.get(albumid);
        for (int songid : album.song_list) {
            this.song_list.set(songid, new Song());
        }
        this.album_list.set(albumid, new Album());
        this.is_changed = true;
    }
}
