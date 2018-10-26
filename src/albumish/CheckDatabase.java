/*
 *  Copyright (c) 2014  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class CheckDatabase {

    private Database database;
    private File file;
    private boolean default_value;
    private Set<Integer> songids;
    private boolean is_changed;

    /**
     * Read the list of files to display with check marks. Rather than integrating this data into
     * either the database or the playlists, we choose to store it in a third location.
     */
    public CheckDatabase(Database database, File directory, String filename) {
        this.database = database;
        this.file = new File(directory, filename);
        this.default_value = true;
        this.songids = new TreeSet<>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(this.file));
            String line = reader.readLine();
            this.default_value = (Integer.parseInt(line) != 0);
            Map<String, Integer> known_files = new TreeMap<>();
            int num_songs = database.song_list.size();
            for (int idx = 1; idx < num_songs; idx++) {
                Song song = database.song_list.get(idx);
                if ((song != null) && (song.filename != null)) {
                    known_files.put(song.filename, song.id);
                }
            }
            while ((line = reader.readLine()) != null) {
                Integer songid = known_files.get(line);
                if (songid != null) {
                    this.songids.add(songid);
                }
            }
            reader.close();
            reader = null;
        } catch (Exception exception) {
            Utils.quietClose(reader);
            System.err.println(this.file + ": " + exception);
        }
        this.is_changed = false;
    }

    /**
     * Write the list of checked files.
     */
    public void save() {
        if (!this.is_changed) {
            return;
        }
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(this.file));
            writer.write(this.default_value ? "1" : "0");
            writer.newLine();
            for (Integer songid : this.songids) {
                Song song = this.database.song_list.get(songid);
                if ((song != null) && (song.filename != null)) {
                    writer.write(song.filename);
                    writer.newLine();
                }
            }
            writer.close();
            writer = null;
            this.is_changed = false;
        } catch (Exception exception) {
            Utils.quietClose(writer);
            System.err.println(this.file + ": " + exception);
        }
    }

    public boolean get(int songid) {
        boolean found = this.songids.contains(songid);
        return (this.default_value ? !found : found);
    }

    public void set(int songid, boolean value) {
        if (this.default_value == value) {
            this.songids.remove(songid);
        } else {
            this.songids.add(songid);
        }
        this.is_changed = true;
    }

    /**
     * Write the list of checked files in a format that can be imported by iTunes, so you can use
     * this program to select the songs to sync to your iPod.
     */
    public void export_playlist(String pathname) {
        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(pathname));
            writer.write("Name\tArtist\tAlbum\r\n");
            int num_songs = this.database.song_list.size();
            for (int songid = 1; songid < num_songs; songid++) {
                if (!get(songid)) {
                    continue;
                }
                Song song = this.database.song_list.get(songid);
                if (song != null && song.filename != null) {
                    writer.write(song.title);
                    writer.write("\t");
                    Artist artist = this.database.artist_list.get(song.artistid);
                    if (artist.name != null) {
                        writer.write(artist.name);
                    }
                    writer.write("\t");
                    Album album = this.database.album_list.get(song.albumid);
                    if (album.name != null) {
                        writer.write(album.name);
                    }
                    writer.write("\r\n");
                }
            }
            writer.close();
            writer = null;
        } catch (Exception exception) {
            System.err.println(exception.toString());
        }
        Utils.quietClose(writer);
    }

    /**
     * @return array with every checked song in the database.
     */
    public IntList get_checked_songids() {
        if (!this.default_value) {
            return new IntList(this.songids.toArray(new Integer[0]));
        }
        // TODO
        return null;
    }
}
