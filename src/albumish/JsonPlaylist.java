package albumish;

import java.net.URL;

/**
 * JSPF file format: http://wiki.xiph.org/JSPF_Draft
 */
public class JsonPlaylist {

    public static class Track {
        URL location;
        URL identifier;
        String title;
        String creator;
        String annotation;
        URL info;
        URL image;
        String album;
        Integer trackNum;
        Integer duration;
    }

    public static class Playlist {
        String title;
        String creator;
        String annotation;
        URL info;
        URL location;
        URL identifier;
        URL image;
        String date;
        URL license;
        Track[] track;
    }

    Playlist playlist;
}
