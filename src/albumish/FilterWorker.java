/*
 *  Copyright (c) 2014  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class FilterWorker {

    private Database database;
    private CheckDatabase check_database;
    private List<Filter> filter_list;

    public FilterWorker(Database database, CheckDatabase check_database) {
        this.database = database;
        this.check_database = check_database;
        this.filter_list = new LinkedList<>();
    }

    /**
     * Add the given filter and update the displayed albums.
     */
    public int[] add_filter(Filter filter) {
        boolean found = false;
        ListIterator<Filter> iterator = this.filter_list.listIterator(0);
        while (iterator.hasNext()) {
            Filter current = iterator.next();
            if (current.fid == filter.fid) {
                iterator.set(filter);
                found = true;
                break;
            }
        }
        if (!found) {
            this.filter_list.add(filter);
        }
        return generate_album_list();
    }

    /**
     * Remove the given filter and update the displayed albums.
     */
    public int[] remove_filter(int fid) {
        Iterator<Filter> iterator = this.filter_list.iterator();
        while (iterator.hasNext()) {
            Filter filter = iterator.next();
            if (filter.fid == fid) {
                iterator.remove();
                break;
            }
        }
        return generate_album_list();
    }

    /**
     * For each album in the database, if the album contains at least one song that matches all
     * filters, then add the album to the given list.
     */
    public int[] generate_album_list() {
        IntList album_list = new IntList();
        IntList song_list = new IntList();
        int size = this.database.album_list.size();
        for (int idx = 1; idx < size; idx++) {
            Album album = this.database.album_list.get(idx);
            if (generate_song_list(album, song_list, false)) {
                album_list.add(album.id);
            }
        }
        return album_list.finish();
    }

    /**
     * For each song on the album, if the song matches all filters, then add it to the given list.
     *
     * @param find_all If true, then find all songs that match all filters. Otherwise, stop after
     *            the first song.
     */
    private boolean generate_song_list(Album album, IntList song_list, boolean find_all) {
        if (album.song_list == null) {
            return false;
        }
        List<Filter> new_filter_list = new LinkedList<>();
        for (Filter filter : this.filter_list) {
            if (!album_match(album, filter)) {
                // Check the individual songs against this filter.
                new_filter_list.add(filter);
            }
        }
        for (int songid : album.song_list) {
            Song song = this.database.song_list.get(songid);
            if (is_song_displayed(song, new_filter_list)) {
                if (!find_all) {
                    return true;
                }
                song_list.add(song.id);
            }
        }
        return false;
    }

    /**
     * @return true if the album matches the filter.
     */
    private boolean album_match(Album album, Filter filter) {
        if (filter.text.equals(":unchecked")) {
            int count = 0;
            for (int songid : album.song_list) {
                if (this.check_database.get(songid)) {
                    count++;
                }
            }
            return (count == 0);
        }
        if (string_match(album.name, filter.text)) {
            return true;
        }
        if (album.artistid > 0) {
            Artist artist = this.database.artist_list.get(album.artistid);
            if (string_match(artist.name, filter.text)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the song matches all filters.
     */
    private boolean is_song_displayed(Song song, List<Filter> new_filter_list) {
        for (Filter filter : new_filter_list) {
            if (!song_match(song, filter)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return true if the song matches the filter.
     */
    private boolean song_match(Song song, Filter filter) {
        if (string_match(song.title, filter.text)) {
            return true;
        }
        if (string_match(song.tags, filter.text)) {
            return true;
        }
        if (song.year > 0) {
            try {
                if (song.year == Integer.parseInt(filter.text)) {
                    return true;
                }
            } catch (Exception dummy) {
            }
        }
        if (song.artistid > 0) {
            Artist artist = this.database.artist_list.get(song.artistid);
            if (string_match(artist.name, filter.text)) {
                return true;
            }
        }
        return false;
    }

    private boolean string_match(String song_text, String filter_text) {
        if (song_text == null || filter_text == null) {
            return false;
        }
        return song_text.toLowerCase().contains(filter_text.toLowerCase());
    }

    /**
     * For each song on the album, if the song matches all filters, then add it to the given list.
     */
    public IntList generate_song_list(int albumid) {
        Album album = this.database.album_list.get(albumid);
        IntList song_list = new IntList();
        if (album.song_list != null) {
            generate_song_list(album, song_list, true);
        }
        return song_list;
    }
}
