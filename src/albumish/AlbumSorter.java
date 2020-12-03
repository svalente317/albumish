/*
 *  Copyright (c) 2014  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import java.util.Arrays;
import java.util.Comparator;

public class AlbumSorter implements Comparator<Integer> {

    public enum SortType {
        YEAR_NEWEST,
        YEAR_OLDEST,
        ALBUM,
        ARTIST
    }

    private final Database database;
    private SortType sort_type;

    public AlbumSorter(Database database) {
        this.database = database;
    }

    public void set_sort_type(SortType sort_type) {
        this.sort_type = sort_type;
    }

    public void sort(int[] album_list) {
        Integer[] obj_list = new Integer[album_list.length];
        for (int idx = 0; idx < album_list.length; idx++) {
            obj_list[idx] = album_list[idx];
        }
        Arrays.sort(obj_list, this);
        for (int idx = 0; idx < album_list.length; idx++) {
            album_list[idx] = obj_list[idx];
        }
    }

    public int compare(Integer id1, Integer id2) {
        Album a1 = this.database.album_list.get(id1);
        Album a2 = this.database.album_list.get(id2);
        int value = 0;
        switch (this.sort_type) {
        case YEAR_NEWEST:
            value = a2.year - a1.year;
            break;
        case YEAR_OLDEST:
            value = a1.year - a2.year;
            break;
        case ALBUM:
            value = string_compare(a1.name, a2.name);
            break;
        case ARTIST:
            value = string_compare(
                    this.database.artist_list.get(a1.artistid).name,
                    this.database.artist_list.get(a2.artistid).name);
            break;
        }
        if (value != 0) {
            return value;
        }
        switch (this.sort_type) {
        case ARTIST:
            value = a1.year - a2.year;
            break;
        case YEAR_NEWEST:
        case YEAR_OLDEST:
            value = string_compare(a1.name, a2.name);
            break;
        case ALBUM:
            value = string_compare(
                    this.database.artist_list.get(a1.artistid).name,
                    this.database.artist_list.get(a2.artistid).name);
            break;
        }
        return value;
    }

    private static int string_compare(String s1, String s2) {
        if (s1 == null) {
            if (s2 == null) {
                return 0;
            }
            return 1;
        } else if (s2 == null) {
            return -1;
        }
        return s1.toLowerCase().compareTo(s2.toLowerCase());
    }
}
