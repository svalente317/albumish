/*
 *  Copyright (c) 2014  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingDeque;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

public class Gallery implements Runnable {

    private final Jukebox player;
    private final Display display;
    private final File topdir;
    private final int small_size;
    private final int big_size;
    private final List<Boolean> is_loaded;
    private final Map<Integer, Image> small_map;
    private final Map<Integer, Image> big_map;
    private final LinkedBlockingDeque<Integer> workstack;

    public Gallery(Jukebox player, Display display, File topdir, int[] sizes) {
        this.player = player;
        this.display = display;
        this.topdir = topdir;
        this.small_size = sizes[0];
        this.big_size = sizes[1];
        this.is_loaded = new ArrayList<>(player.database.album_list.size());
        this.small_map = new TreeMap<>();
        this.big_map = new TreeMap<>();
        this.workstack = new LinkedBlockingDeque<>();
        Thread thread = new Thread(this);
        thread.start();
    }

    public void get_sizes(int[] sizes) {
        sizes[0] = this.small_size;
        sizes[1] = this.big_size;
    }

    public Image get(int albumid, boolean is_selected) {
        while (this.is_loaded.size() <= albumid) {
            this.is_loaded.add(false);
        }
        if (!this.is_loaded.get(albumid)) {
            if (!this.workstack.contains(albumid)) {
                this.workstack.add(albumid);
            }
            return null;
        }
        return (is_selected ?
                this.big_map.get(albumid) :
                this.small_map.get(albumid));
    }

    @Override
    public void run() {
        while (true) {
            try {
                while (this.workstack.size() > 10) {
                    this.workstack.removeFirst();
                }
                int albumid = this.workstack.takeFirst();
                if (!this.is_loaded.get(albumid)) {
                    this.is_loaded.set(albumid, true);
                    load_image(albumid);
                }
            } catch (Exception exception) {
                System.err.println("load_image: " + exception);
            }
        }
    }

    private void load_image(final int albumid) {
        ImageData[] images = null;
        Album album = this.player.database.album_list.get(albumid);
        File file = get_image_file(album);
        if (file != null && file.exists()) {
            try {
                images = ImageConverter.getScaledImages(file,
                        this.small_size, this.big_size);
            } catch (Exception exception) {
                System.err.println(file + ": " + exception);
            }
        }
        if (images == null) {
            try {
                Artwork artwork = get_audio_file_artwork(album.song_list[0]);
                if (artwork != null) {
                    images = ImageConverter.getScaledImages(artwork,
                            this.small_size, this.big_size);
                }
            } catch (Exception exception) {
                System.err.println(file + ": " + exception);
            }
        }
        if (images == null) {
            return;
        }
        if (images[0] == null || images[1] == null) {
            System.err.println(album.name + ": failed to scale image");
            return;
        }
        this.small_map.put(albumid, new Image(this.display, images[0]));
        this.big_map.put(albumid, new Image(this.display, images[1]));
        this.display.asyncExec(new Runnable() {
            @Override
            public void run() {
                Gallery.this.player.invalidate_album(albumid);
            }
        });
    }

    public File get_image_file(Album album) {
        if (album.name == null) {
            return null;
        }
        int artistid = album.artistid;
        if (artistid <= 0) {
            return null;
        }
        String artist_name = this.player.database.artist_list.get(artistid).name;
        if (artist_name == null) {
            return null;
        }
        StringBuilder filename = new StringBuilder();
        Utils.name_to_filename(filename, artist_name);
        filename.append(" - ");
        Utils.name_to_filename(filename, album.name);
        filename.append(".jpg");
        return new File(this.topdir, filename.toString());
    }

    private Artwork get_audio_file_artwork(int songid) throws Exception {
        Song song = this.player.database.song_list.get(songid);
        AudioFile audio = AudioFileIO.read(new File(song.filename));
        Tag tag = audio.getTag();
        // List<Artwork> list = tag.getArtworkList();
        // return list == null || list.isEmpty() ? null : list.get(list.size()-1);
        return tag.getFirstArtwork();
    }


    public boolean audio_file_has_artwork(int songid) throws Exception {
        return get_audio_file_artwork(songid) != null;
    }

    /**
     * Copy image file into the album images directory.
     */
    public void copy_file(int albumid, String srcfile) {
        Album album = this.player.database.album_list.get(albumid);
        File dstfile = get_image_file(album);
        Utils.copy_file(new File(srcfile), dstfile);
        if (this.is_loaded.size() > albumid) {
            this.is_loaded.set(albumid, false);
        }
    }

    public void invalidate(int albumid) {
        if (this.is_loaded.size() > albumid) {
            this.is_loaded.set(albumid, false);
        }
    }
}
