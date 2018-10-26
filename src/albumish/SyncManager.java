/*
 *  Copyright (c) 2017  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.StandardArtwork;

import albumish.GvfsClient.FileInfo;

public class SyncManager {
    public static final String STORAGE_DIR = "Phone";
    public static final String MUSIC_DIR = "Music";

    private Jukebox jukebox;
    private String user_home;
    private String musicUri;
    private ProgressDialog dialog;

    private static class Filenames {
        String filename;
        String media_name;

        Filenames(String filename, String media_name) {
            this.filename = filename;
            this.media_name = media_name;
        }
    }

    private Set<String> subdir_set;
    private List<Filenames> to_add;
    private List<Filenames> to_update;
    private List<String> to_delete;

    /**
     * Start the process of syncing checked songs to device.
     */
    public void sync_to_device(Jukebox jukebox) {
        this.jukebox = jukebox;
        this.user_home = System.getProperty("user.home");
        String[] device_uris = GvfsClient.getMtpDeviceList();
        if (device_uris == null || device_uris.length == 0) {
            sync_to_mounted_device();
            return;
        }
        String device_uri = device_uris[0];
        if (device_uris.length > 1) {
            System.out.println("Found " + device_uris.length + " MTP devices.");
            System.out.println("Using " + device_uri);
        }
        this.musicUri = device_uri + STORAGE_DIR + "/" + MUSIC_DIR;
        sync_to_music_uri();
    }

    private void sync_to_mounted_device() {

        InputDialog.InputRunnable runnable = new InputDialog.InputRunnable() {
            @Override
            public void run(String input) {
                SyncManager.this.musicUri = input + "/" + MUSIC_DIR;
                sync_to_music_uri();
            }
        };

        new InputDialog(this.jukebox.main_window, "Sync to Device",
                "Enter device mount point.", "/media/sal/MUSIC", runnable);
    }

    private void sync_to_music_uri() {

        this.dialog = new ProgressDialog(this.jukebox.main_window,
                "Sync to Device...", "Reading " + this.musicUri + "...");

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                run_sync_manager();
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * In a background thread, sync to the device, while updating the view.
     */
    private void run_sync_manager() {
        this.subdir_set = new TreeSet<>();
        Map<String, FileInfo> fileinfo_map = new TreeMap<>();

        List<String> workqueue = new ArrayList<>();
        List<String> subdirs = new ArrayList<>();
        List<FileInfo> fileinfos = new ArrayList<>();
        workqueue.add("");
        int processed = 0;
        while (!workqueue.isEmpty()) {
            String directory = workqueue.remove(0);
            this.dialog.set_bottom_label(directory);
            GvfsClient.listDirectory(this.musicUri, directory, subdirs, fileinfos);
            this.subdir_set.addAll(subdirs);
            for (FileInfo fileinfo : fileinfos) {
                fileinfo_map.put(fileinfo.pathname, fileinfo);
            }
            workqueue.addAll(subdirs);
            Collections.sort(workqueue);
            subdirs.clear();
            fileinfos.clear();
            processed++;
            this.dialog.set_progress(processed, processed + workqueue.size());
        }

        // Compare the local database to the contents of the device.
        // Build lists of files to add, update, and delete.
        this.to_add = new ArrayList<>();
        this.to_update = new ArrayList<>();
        this.to_delete = new ArrayList<>();
        for (Song song : this.jukebox.database.song_list) {
            if (song.filename == null || !this.jukebox.check_database.get(song.id)) {
                continue;
            }
            String media_name = generate_media_name(song, this.jukebox.database);
            File cache_file = get_song_file_with_artwork(song, media_name);
            String song_filename = cache_file != null ?
                    cache_file.getAbsolutePath() : song.filename;
            FileInfo fileinfo = fileinfo_map.remove(media_name);
            if (fileinfo == null) {
                this.to_add.add(new Filenames(song_filename, media_name));
            } else if (is_modified(song_filename, fileinfo)) {
                this.to_update.add(new Filenames(song_filename, media_name));
            }
        }
        for (FileInfo fileinfo : fileinfo_map.values()) {
            this.to_delete.add(fileinfo.pathname);
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("TO ADD:\n");
        for (Filenames song : this.to_add) {
            builder.append(song.media_name);
            builder.append('\n');
        }
        builder.append("\nTO UPDATE:\n");
        for (Filenames song : this.to_update) {
            builder.append(song.media_name);
            builder.append('\n');
        }
        builder.append("\nTO DELETE:\n");
        for (String filename : this.to_delete) {
            builder.append(filename);
            builder.append('\n');
        }
        // TODO no changes?

        final String header = "Sync to " + this.musicUri + "...";
        this.dialog.close_and_run(new Runnable() {
            @Override
            public void run() {
                SyncManager pthis = SyncManager.this;
                pthis.dialog = null;
                new SyncManagerDialog(pthis.jukebox.main_window, header,
                        builder.toString(), pthis);
            }
        });
    }

    /**
     * Generate filename [Artist]/[Album]/filename.
     */
    private static String generate_media_name(Song song, Database database) {
        if (song.filename == null) {
            return null;
        }
        String filename = song.filename;
        int idx = filename.lastIndexOf('/');
        if (idx >= 0) {
            filename = filename.substring(idx + 1);
        }
        if (filename.isEmpty()) {
            return null;
        }
        Album album = database.album_list.get(song.albumid);
        Artist artist = database.artist_list.get(album.artistid);
        StringBuilder builder = new StringBuilder();
        Utils.name_to_dirname(builder, artist.name == null ? "Unknown" : artist.name);
        builder.append('/');
        Utils.name_to_dirname(builder, album.name == null ? "Untitled" : album.name);
        builder.append('/');
        Utils.name_to_filename(builder, filename);
        return builder.toString();
    }

    /**
     * If this song has artwork in the gallery but not in the mp3 file, then write
     * a copy of the mp3 file with the artwork embedded, to write to the device.
     * If we already have a cached copy of the file with the embedded artwork, then
     * use that.
     */
    private File get_song_file_with_artwork(Song song, String media_name) {
        Database database = this.jukebox.database;
        Gallery gallery = this.jukebox.gallery;
        Album album = database.album_list.get(song.albumid);
        File image_file = gallery.get_image_file(album);
        if (image_file == null || !image_file.exists()) {
            return null;
        }
        try {
            if (gallery.audio_file_has_artwork(song.id)) {
                return null;
            }
        } catch (Exception exception) {
            System.out.println(song.filename + ": artwork: " + exception);
            return null;
        }
        File song_file = new File(song.filename);
        File cache_dir = new File(this.user_home, "cache");
        File cache_file = new File(cache_dir, media_name);
        long timestamp = cache_file.lastModified();
        long library_timestamp = Math.max(image_file.lastModified(), song_file.lastModified());
        if (timestamp >= library_timestamp) {
            return cache_file;
        }
        try {
            Artwork artwork = StandardArtwork.createArtworkFromFile(image_file);
            cache_file.getParentFile().mkdirs();
            Utils.copy_file(song_file, cache_file);
            AudioFile audio = AudioFileIO.read(cache_file);
            Tag tag = audio.getTag();
            tag.setField(artwork);
            audio.commit();
        } catch (Exception exception) {
            System.out.println(song_file + ": " + exception);
            cache_file.delete();
            return null;
        }
        cache_file.setLastModified(library_timestamp);
        return cache_file;
    }


    /**
     * Compare size and modification time of the file on the device to the file on
     * the computer.
     */
    private static boolean is_modified(String song_filename, FileInfo fileinfo) {
        File file = new File(song_filename);
        if (!file.exists()) {
            return false;
        }
        if (file.length() != fileinfo.size) {
            System.out.println("local=" + song_filename + " device=" + fileinfo.pathname +
                    " localsize=" + file.length() + " ds=" + fileinfo.size);
            return true;
        }
        long lastModified = file.lastModified() / 1000;
        return lastModified > fileinfo.timeModified;
    }

    /**
     * Start the process of applying changes to the device.
     */
    public void apply_changes_to_device() {
        this.dialog = new ProgressDialog(this.jukebox.main_window,
                "Sync to Device...", "Writing " + this.musicUri + "...");

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                run_apply_changes();
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * In a background thread, add, update, and delete files on the device.
     */
    private void run_apply_changes() {
        this.to_add.addAll(this.to_update);
        this.to_update = null;

        int total = this.to_add.size() + this.to_delete.size();
        int current = 0;
        for (Filenames song : this.to_add) {
            this.dialog.set_bottom_label(song.media_name);
            String directory = get_parent_directory(song.media_name);
            if (!this.subdir_set.contains(directory)) {
                GvfsClient.makeDirectory(this.musicUri + "/" + directory);
                this.subdir_set.add(directory);
            }
            GvfsClient.copyFile(song.filename, this.musicUri + "/" + song.media_name);
            current++;
            this.dialog.set_progress(current, total);
        }
        for (String pathname : this.to_delete) {
            this.dialog.set_bottom_label(pathname);
            GvfsClient.removeFile(this.musicUri + "/" + pathname);
            current++;
            this.dialog.set_progress(current, total);
        }
        this.dialog.close_and_run(null);
    }

    /**
     * Strip off the filename component.
     */
    private static String get_parent_directory(String name) {
        int idx = name.lastIndexOf('/');
        return (idx < 0 ? name : name.substring(0, idx));
    }
}
