/*
 *  Copyright (c) 2014  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

public class AddToLibraryManager {

    private Jukebox jukebox;
    private String pathname;
    private ProgressDialog dialog;
    private int file_count;
    private int done_count;

    /**
     * Pop up an input dialog to select a folder to add.
     */
    public void add_to_library(Jukebox jukebox) {
        this.jukebox = jukebox;

        InputDialog.InputRunnable runnable = new InputDialog.InputRunnable() {
            @Override
            public void run(String input) {
                add_folder_to_library(input);
            }
        };
        new InputDialog(this.jukebox.main_window, "Add Folder to Library",
                "Enter Folder to Add to Library.", null, runnable);
    }

    /**
     * Pop up a progress dialog, and start a background thread to add the folder.
     */
    private void add_folder_to_library(String pathname) {
        this.pathname = pathname;
        this.dialog = new ProgressDialog(this.jukebox.main_window,
                "Add to Library...", "Processing " + this.pathname + "...");

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                run_add_to_library();
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * Find every file in the directory tree rooted at the given pathname. Add each media file to
     * the database.
     */
    private void run_add_to_library() {
        Database database = this.jukebox.database;
        Set<String> known_files = new TreeSet<>();
        for (Song song : database.song_list) {
            if (song.filename != null) {
                known_files.add(song.filename);
            }
        }
        List<String> file_list = Utils.read_directory_tree(
                new File(this.pathname), known_files);
        this.file_count = file_list.size();
        this.done_count = 0;
        this.dialog.set_bottom_label("Done: 0 / " + this.file_count);
        for (String filename : file_list) {
            if (this.pathname == null) {
                break;
            }
            Database.JsonSong song = read_id3_tags(filename);
            if (song != null && song.title != null) {
                database.add_song(song);
            }
            this.done_count++;
            this.dialog.set_progress(this.file_count, this.done_count);
            this.dialog.set_bottom_label("Done: " + this.done_count + " / " + this.file_count);
        }
        this.dialog.close_and_run(new Runnable() {
            @Override
            public void run() {
                AddToLibraryManager.this.jukebox.reset_albums();
            }
        });
    }

    private Database.JsonSong read_id3_tags(String filename) {
        File file = new File(filename);
        AudioFile audio;
        try {
            audio = AudioFileIO.read(file);
        } catch (Exception exception) {
            System.err.println(exception.toString());
            return null;
        }
        Tag tag = audio.getTag();
        Database.JsonSong song = new Database.JsonSong();
        song.filename = filename;
        song.mtime = (int) (file.lastModified() / 1000L);
        if (tag != null) {
            song.title = normalize(tag.getFirst(FieldKey.TITLE));
            song.artist = normalize(tag.getFirst(FieldKey.ARTIST));
            song.year = Utils.parseInt(tag.getFirst(FieldKey.YEAR));
            song.album = normalize(tag.getFirst(FieldKey.ALBUM));
            song.album_artist = normalize(tag.getFirst(FieldKey.ALBUM_ARTIST));
            song.track_number = Utils.parseInt(tag.getFirst(FieldKey.TRACK));
        }
        if (song.title == null) {
            song.title = file.getName();
        }
        AudioHeader header = audio.getAudioHeader();
        song.duration = header.getTrackLength();
        song.bitrate = header.getBitRate();
        song.add_time = (int) (System.currentTimeMillis() / 1000L);
        return song;
    }

    private static String normalize(String item) {
        return (item == null || item.isEmpty() ? null : item);
    }
}
