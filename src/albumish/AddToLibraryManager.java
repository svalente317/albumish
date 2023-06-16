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
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.id3.ID3v1Tag;

public class AddToLibraryManager {

    public Jukebox jukebox;
    private String pathname;
    private String tags;
    private ProgressDialog dialog;
    private int file_count;
    private int done_count;

    /**
     * Pop up an input dialog to select a folder to add.
     */
    public void add_to_library(Jukebox jukebox) {
        this.jukebox = jukebox;
        new AddToLibraryDialog(this);
    }

    /**
     * Pop up a progress dialog, and start a background thread to add the folder.
     */
    public void add_folder_to_library(String pathname, String tags) {
        this.pathname = pathname;
        if (tags != null && !tags.equals("")) {
            this.tags = tags;
        }
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
            filename = this.jukebox.getRelativeFilename(filename);
            Song song = new Song();
            SongInfo obj = new SongInfo();
            read_id3_tags(filename, song, obj);
            if (song.title != null) {
                song.tags = tags;
                database.add_song(song, obj);
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

    private void read_id3_tags(String filename, Song song, SongInfo obj) {
        song.filename = filename;
        File file = this.jukebox.getFile(song);
        AudioFile audio;
        try {
            audio = AudioFileIO.read(file);
        } catch (Exception exception) {
            System.err.println(exception.toString());
            return;
        }
        Tag tag = audio.getTag();
        song.mtime = (int) (file.lastModified() / 1000L);
        if (tag != null) {
            song.title = normalize(tag.getFirst(FieldKey.TITLE));
            obj.artist = normalize(tag.getFirst(FieldKey.ARTIST));
            obj.album = normalize(tag.getFirst(FieldKey.ALBUM));
            obj.album_artist = normalize(tag.getFirst(FieldKey.ALBUM_ARTIST));
            song.track_number = Utils.parseInt(tag.getFirst(FieldKey.TRACK));
            song.year = Utils.parseInt(tag.getFirst(FieldKey.YEAR));
            song.bpm = normalize(tag.getFirst(FieldKey.BPM));
            song.id3 = getTagClass(tag);
            song.encoder = getEncoder(song, tag);
        }
        if (song.title == null) {
            song.title = file.getName();
        }
        AudioHeader header = audio.getAudioHeader();
        song.duration = header.getTrackLength();
        song.bitrate = header.getBitRate();
        song.add_time = (int) (System.currentTimeMillis() / 1000L);
    }

    private static String normalize(String item) {
        return (item == null || item.isEmpty() ? null : item);
    }

    private static String getTagClass(Tag tag) {
        String name = tag.getClass().getSimpleName();
        int idx = name.lastIndexOf('.');
        if (idx > 0) {
            name = name.substring(idx+1);
        }
        if (name.endsWith("Tag")) {
            name = name.substring(0, name.length()-3);
        }
        return name;
    }

    private static String getEncoder(Song song, Tag tag) {
        String encoder = null;
        if (!(tag instanceof ID3v1Tag)) {
            try {
                encoder = tag.getFirst("TSSE");
            } catch (Exception dummy) {
            }
        }
        if (encoder != null && encoder.startsWith("LAME ")) {
            song.encoderVersion = encoder.substring(5);
            String[] words = song.encoderVersion.split(" ");
            if (words.length == 4 && words[0].endsWith("bits") && words[1].equals("version")) {
                song.encoderVersion = "v" + words[2];
            }
            return "lame";
        }
        String comment = tag.getFirst(FieldKey.COMMENT);
        if (comment != null && !comment.isEmpty()) {
            if (comment.startsWith("Amazon.com Song ID: ")) {
                return "amazon";
            }
            if (comment.equals("Created by Grip")) {
                return "grip";
            }
        }
        encoder = tag.getFirst(FieldKey.ENCODER);
        if (encoder != null && encoder.startsWith("iTunes ")) {
            song.encoderVersion = encoder.substring(7);
            return "itunes";
        }
        TagField field = tag.getFirstField("PRIV");
        if (field != null) {
            byte[] content = null;
            try {
                content = field.getRawContent();
            } catch (Exception dummy) {
            }
            if (content != null) {
                for (int i = 0; i+2 < content.length; i++) {
                    if (content[i] == 'W' && content[i+1] == 'M' && content[i+2] == '/') {
                        return "windows";
                    }
                }
            }
        }
        return null;
    }
}
