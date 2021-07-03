 /*
 *  Copyright (c) 2021  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

public class DownloadBpmManager implements SelectionListener {

    private final Jukebox jukebox;
    private final Shell download_dialog;
    private final Table table;
    private boolean is_cancelled;
    private Map<Integer, String> artist_id_map;
    private Database database;

    public DownloadBpmManager(Jukebox jukebox) {
        this.jukebox = jukebox;
        this.download_dialog = new Shell(this.jukebox.main_window, SWT.SHELL_TRIM | SWT.MODELESS);
        this.download_dialog.setText("Download BPM Data");

        GridLayout layout = new GridLayout(1, true);
        layout.marginWidth = layout.marginHeight = 20;
        layout.verticalSpacing = 20;
        this.download_dialog.setLayout(layout);
        GridData data;

        this.table = new Table(this.download_dialog, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        TableColumn column;
        column = new TableColumn(this.table, SWT.LEFT);
        column.setText("songs");
        column.pack();
        column = new TableColumn(this.table, SWT.LEFT);
        column.setText("found");
        column.pack();
        column = new TableColumn(this.table, SWT.LEFT);
        column.setText("Album");
        column.pack();
        this.table.setHeaderVisible(true);
        this.table.setLinesVisible(true);
        data = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        data.widthHint = 400;
        data.heightHint = 315;
        this.table.setLayoutData(data);

        Label label = new Label(this.download_dialog, SWT.SEPARATOR | SWT.HORIZONTAL);
        data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = SWT.FILL;
        label.setLayoutData(data);

        Button button = new Button(this.download_dialog, SWT.PUSH);
        button.setText("Cancel");
        data = new GridData(SWT.CENTER, SWT.BEGINNING, true, false);
        button.setLayoutData(data);
        button.setData(SWT.CANCEL);
        button.addSelectionListener(this);

        this.download_dialog.pack();
        this.download_dialog.open();
        this.download_dialog.setActive();
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent event) {
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        if ((int) event.widget.getData() == SWT.CANCEL) {
            // TODO close when finished
            this.is_cancelled = true;
        }
    }

    private void display_row(final Album album, final int found) {
        final Table table = this.table;
        this.jukebox.main_window.getDisplay().asyncExec(() -> {
            TableItem item = new TableItem(table, 0);
            item.setText(0, Integer.toString(album.song_list.length));
            item.setText(1, Integer.toString(found));
            item.setText(2, album.name);
        });
    }

    private void update_row(final int found) {
        final Table table = this.table;
        this.jukebox.main_window.getDisplay().asyncExec(() -> {
            TableItem item = table.getItem(table.getItemCount()-1);
            item.setText(1, Integer.toString(found));
        });
    }

    private void update_window_done() {
        if (this.is_cancelled) {
            this.jukebox.main_window.getDisplay().asyncExec(this.download_dialog::close);
        }
    }

    // ---

    public static final String MUSICBRAINZ_URL = "https://musicbrainz.org/ws/2/";
    public static final String TYPE_ARTIST = "artist";
    public static final String TYPE_RELEASE = "release";
    public static final String TYPE_RECORDING = "recording";
    public static final String ACOUSTICBRAINZ_URL = "https://acousticbrainz.org/api/v1/low-level";

    static class RecordingScore {
        double score;
        int idx;
        String title;
        String recording_id;
    }

    public void run() {
        Thread thread = new Thread(this::download_bpm_data_safe);
        thread.start();
    }

    private void download_bpm_data_safe() {
        try {
            download_bpm_data();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * For every album in the database, if none of the songs in the album have "bpm" defined,
     * then get the album's song's BPM from acousticbrainz, and update the database.
     */
    private void download_bpm_data() throws Exception {
        this.artist_id_map = new TreeMap<>();
        this.database = this.jukebox.database;

        for (Album album : this.database.album_list) {
            if (this.is_cancelled) {
                break;
            }
            if (album == null || album.song_list == null) {
                continue;
            }
            int count = count_bpm_songs(album);
            display_row(album, count);
            if (count > 0) {
                continue;
            }
            String artist_name = this.database.artist_list.get(album.artistid).name;
            if (!artist_name.equals("Van Morrison")) {
                continue;
            }
            String artist_id = get_artist_id(album.artistid);
            System.out.println("artist " + artist_name + ": " + artist_id);
            if (artist_id == null) {
                continue;
            }
            String release_id = get_release_id(album.name, artist_id);
            System.out.println("release " + album.name + ": " + release_id);
            if (release_id == null) {
                continue;
            }
            Map<String, String> recordings = get_recordings(release_id);
            String[] recording_ids = get_song_ids(album, recordings);
            for (String recording_id : recording_ids) {
                if (recording_id != null) {
                    String bpm = get_recording_bpm(recording_id);
                    System.out.println("song: " + bpm);
                    // TODO record in database
                }
            }
            update_row(count_bpm_songs(album));
        }
        update_window_done();
    }

    private int count_bpm_songs(Album album) {
        int count = 0;
        for (int songid : album.song_list) {
            Song song = this.database.song_list.get(songid);
            if (song.bpm != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get the array of objects (artist, release, recording) for the given parameters.
     */
    public JsonArray get_musicbrainz_array(String otype, String... params)
            throws Exception {
        String url = MUSICBRAINZ_URL + otype + "?fmt=json&";
        for (int idx = 0; idx < params.length; idx++) {
            String key = params[idx];
            idx++;
            String value = URLEncoder.encode(params[idx], StandardCharsets.UTF_8);
            url += key + "=" + value + "&";
        }
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET().uri(new URI(url)).build();
        HttpResponse<String> response;
        response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonElement root = JsonParser.parseString(response.body());
        JsonElement element = root.getAsJsonObject().get(otype+"s");
        if (element == null) {
            return null;
        }
        return element.getAsJsonArray();
    }

    /**
     * Get the object of the given type (artist, release) that best matches the query.
     */
    public JsonObject get_musicbrainz_object(String otype, String query, String... params)
            throws Exception {
        String[] q_params = Utils.addAll(params, "query", query, "limit", "1");
        JsonArray array = get_musicbrainz_array(otype, q_params);
        return array == null || array.size() == 0 ? null : array.get(0).getAsJsonObject();
    }

    /**
     * Get the artist's MBID from the local cache or from musicbrainz.
     */
    private String get_artist_id(int artistid) throws Exception {
        if (artistid == 0) {
            return null;
        }
        String mbid = this.artist_id_map.get(artistid);
        if (mbid != null) {
            return mbid;
        }
        Artist artist = this.database.artist_list.get(artistid);
        JsonObject object = get_musicbrainz_object(TYPE_ARTIST, artist.name);
        if (object == null) {
            return null;
        }
        mbid = object.get("id").getAsString();
        this.artist_id_map.put(artistid, mbid);
        return mbid;
    }

    /**
     * Get the album's release MBID from musicbrainz.
     */
    private String get_release_id(String name, String artist_id) throws Exception {
        JsonObject object = get_musicbrainz_object(TYPE_RELEASE, name, TYPE_ARTIST, artist_id);
        return object.get("id").getAsString();
    }

    /**
     * Get the album's song list from musicbrainz. The song names may be completely different
     * from the song names in the local database.
     *
     * @return Map from musicbrainz song name -> mbid
     */
    private Map<String, String> get_recordings(String release_id) throws Exception {
        JsonArray items = get_musicbrainz_array(TYPE_RECORDING, TYPE_RELEASE, release_id);
        Map<String, String> recordings = new TreeMap<>();
        for (JsonElement item : items) {
            JsonObject object = item.getAsJsonObject();
            String id = object.get("id").getAsString();
            String title = object.get("title").getAsString();
            recordings.put(title, id);
        }
        return recordings;
    }

    /**
     * Generate the list song_ids for the album such that:
     * song_ids[n] is the album's n'th song's MBID or null.
     * (The list contains null for the songs where no matching song title was found.)
     */
    private String[] get_song_ids(Album album, Map<String, String> recordings) {
        PriorityQueue<RecordingScore> heap = new PriorityQueue<>(
                (a, b) -> -Double.compare(a.score, b.score));
        String[] song_ids = new String[album.song_list.length];
        int missing = 0;
        for (int idx = 0; idx < song_ids.length; idx++) {
            Song song = this.database.song_list.get(album.song_list[idx]);
            song_ids[idx] = find_song(song.title, recordings, heap, idx);
            if (song_ids[idx] == null) {
                missing++;
            }
        }
        while (missing > 0) {
            RecordingScore item = heap.poll();
            if (item == null) {
                break;
            }
            if (song_ids[item.idx] == null) {
                song_ids[item.idx] = item.recording_id;
                missing--;
            }
        }
        return song_ids;
    }

    /**
     * If the given song has an exact match from musicbrainz, then return its MBID.
     * Otherwise, update the given heap with information about the song's possible
     * matches, so we can ultimately find the best matches between the local and
     * musicbrainz databases.
     */
    private String find_song(String title, Map<String, String> recordings,
                             PriorityQueue<RecordingScore> heap, int idx) {
        JaroWinklerSimilarity calculator = new JaroWinklerSimilarity();
        for (Map.Entry<String, String> entry : recordings.entrySet()) {
            double score = calculator.apply(title, entry.getKey());
            if (score == 1.0) {
                recordings.remove(entry.getKey());
                return entry.getValue();
            }
            if (score >= 0.75) {
                RecordingScore item = new RecordingScore();
                item.score = score;
                item.idx = idx;
                item.title = entry.getKey();
                item.recording_id = entry.getValue();
                heap.add(item);
            }
        }
        return null;
    }

    /**
     * Get the recording's bpm from acousticbrainz.
     */
    private String get_recording_bpm(String recording_id) throws Exception {
        String url = ACOUSTICBRAINZ_URL + "?features=rhythm.bpm&recording_ids=" +
                recording_id + "&";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET().uri(new URI(url)).build();
        HttpResponse<String> response;
        response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
        JsonElement root = JsonParser.parseString(response.body());
        JsonObject object = root.getAsJsonObject().getAsJsonObject(recording_id);
        if (object != null) {
            object = object.getAsJsonObject("0");
            if (object != null) {
                object = object.getAsJsonObject("rhythm");
                if (object != null) {
                    JsonElement element = object.get("bpm");
                    if (element != null) {
                        return element.toString();
                    }
                }
            }
        }
        return null;
    }
}
