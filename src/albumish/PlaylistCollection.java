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
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class PlaylistCollection extends ArrayList<Playlist> {
    private static final long serialVersionUID = 1L;

    public static final int AUTO_PLAYLIST = 0;

    private File directory;

    /**
     * Find each .jspf file in the given directory. Parse each jspf file into a playlist, and add
     * the playlist to the collection.
     */
    public PlaylistCollection(Database database, File directory) {
        this.directory = directory;
        // Create auto playlist.
        Playlist playlist = new Playlist();
        playlist.name = "Auto-Playlist";
        playlist.id = AUTO_PLAYLIST;
        add(playlist);

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        Gson gson = new Gson();
        for (File file : files) {
            if (file.getName().endsWith(".jspf")) {
                FileReader reader = null;
                try {
                    reader = new FileReader(file);
                    JsonPlaylist root = gson.fromJson(reader, JsonPlaylist.class);
                    add_loaded_playlist(database, root);
                } catch (Exception exception) {
                    System.err.println(file.getAbsolutePath() + ": " + exception);
                }
                Utils.quietClose(reader);
            }
        }
    }

    /**
     * Convert jspf playlist to Albumish internal playlist.
     */
    private void add_loaded_playlist(Database database, JsonPlaylist root) {
        if (root == null || root.playlist == null || root.playlist.title == null) {
            return;
        }
        Playlist playlist = new Playlist();
        playlist.id = this.size();
        playlist.name = root.playlist.title;
        if (root.playlist.track != null) {
            for (JsonPlaylist.Track track : root.playlist.track) {
                int songid = get_track_songid(database, track);
                if (songid > 0) {
                    playlist.add(songid);
                }
            }
        }
        add(playlist);
    }

    /**
     * Find the song in the database that best matches the given track description.
     *
     * Extremely lazy implementation: Just look for exact matches in pathname. Obviously, we could
     * add a hundred types of fuzzy matching, including filename matches that ignore prefixes,
     * matches by exact song title and artist, etc.
     */
    private int get_track_songid(Database database, JsonPlaylist.Track track) {
        if (track.location == null) {
            return 0;
        }
        for (Song song : database.song_list) {
            try {
                if (track.location.equals(new URL("file://" + song.filename))) {
                    return song.id;
                }
            } catch (Exception exception) {
            }
        }
        return 0;
    }

    /**
     * Reset the contents of the Auto Playlist.
     *
     * @return the id of the playlist that was modified.
     */
    public int set_auto_playlist(IntList song_list) {
        get(AUTO_PLAYLIST).reset(song_list);
        return AUTO_PLAYLIST;
    }

    /**
     * @return new playlist in collection.
     */
    public Playlist new_playlist() {
        Playlist playlist = new Playlist();
        playlist.id = this.size();
        add(playlist);
        return playlist;
    }

    /**
     * Save each changed playlist as a .jspf file in the playlist directory.
     */
    public void save(Database database) {
        for (int idx = 0; idx < size(); idx++) {
            if (idx == AUTO_PLAYLIST) {
                continue;
            }
            Playlist playlist = get(idx);
            if (!playlist.is_changed) {
                continue;
            }
            save_playlist(database, playlist);
        }
    }

    /**
     * Convert Albumish internal playlist to jspf playlist and save it.
     */
    private void save_playlist(Database database, Playlist playlist) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        JsonPlaylist.Playlist jplaylist = new JsonPlaylist.Playlist();
        jplaylist.title = playlist.name;
        jplaylist.date = formatter.format(new Date());
        IntList song_list = playlist.get_song_list();
        JsonPlaylist.Track[] track_arr = new JsonPlaylist.Track[song_list.size()];
        for (int idx = 0; idx < track_arr.length; idx++) {
            JsonPlaylist.Track obj = new JsonPlaylist.Track();
            Song song = database.song_list.get(song_list.get(idx));
            if (song.filename != null) {
                try {
                    obj.location = new URL("file://" + song.filename);
                } catch (Exception exception) {
                    System.out.println(song.filename + ": " + exception);
                }
            }
            if (song.title != null) {
                obj.title = song.title;
            }
            Artist artist = database.artist_list.get(song.artistid);
            if (artist.name != null) {
                obj.creator = artist.name;
            }
            // If this song title + artist is ambiguous in the current
            // database, then also record album name?
            track_arr[idx] = obj;
        }
        jplaylist.track = track_arr;
        JsonPlaylist root = new JsonPlaylist();
        root.playlist = jplaylist;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String text = gson.toJson(root);
        File file = new File(this.directory, playlist.name + ".jspf");
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(text);
            writer.close();
            playlist.is_changed = false;
        } catch (Exception exception) {
            System.err.println(file.toString() + ": " + exception);
        }
        Utils.quietClose(writer);
    }
}
