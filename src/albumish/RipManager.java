/*
 *  Copyright (c) 2017  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RipManager implements SelectionListener {

    private Jukebox jukebox;
    private ProgressDialog progress_dialog;
    private Shell edit_dialog;
    private Text textbox;

    public void rip_cd(Jukebox jukebox) {
        this.jukebox = jukebox;
        this.progress_dialog = new ProgressDialog(this.jukebox.main_window,
                "Rip CD...", "Reading CD Table of Contents...");

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                run_rip_manager();
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private static class MbAlbum {
        public String name;
        public String artist;
        public String year;
        public String[] tracklist;
    }

    private void run_rip_manager() {
        String url = null;
        try {
            JMBDiscId discId = new JMBDiscId();
            discId.init("/usr/lib/x86_64-linux-gnu/libdiscid.so.0");
            url = discId.getWebServiceUrl(null);
            System.out.println("MusicBrainz url: " + url);
        } catch (Exception exception) {
            exception.printStackTrace();
            return;
        }
        final MbAlbum album = new MbAlbum();
        // TODO try to get album metadata from cache
        try {
            get_musicbrainz_album(url, album);
        } catch (Exception exception) {
            exception.printStackTrace();
            // Continue. The user can manually enter the album metadata.
        }
        this.progress_dialog.close_and_run(new Runnable() {
            @Override
            public void run() {
                RipManager.this.progress_dialog = null;
                open_album_metadata_dialog(album);
            }
        });
    }

    private void get_musicbrainz_album(String url, MbAlbum album) throws Exception {
        this.progress_dialog.set_bottom_label("Downloading " + url + "...");
        URLConnection connection = new URL(url).openConnection();
        connection.connect();
        InputStream istream = connection.getInputStream();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(istream);
        istream.close();

        Element root = document.getDocumentElement();
        Element releaseList = child(root, "release-list");
        Element release = child(releaseList, "release");
        Element title = child(release, "title");
        album.name = text(title);
        Element artist = child(release, "artist");
        Element artist2 = child(artist, "name");
        album.artist = text(artist2);
        Element trackList = child(release, "track-list");
        NodeList nodeList = trackList.getChildNodes();
        int numTracks = nodeList.getLength();
        album.tracklist = new String[numTracks];
        for (int idx = 0; idx < numTracks; idx++) {
            Element trackElem = (Element) nodeList.item(idx);
            Element titleElem = child(trackElem, "title");
            album.tracklist[idx] = text(titleElem);
        }
        Element eventList = child(release, "release-event-list");
        nodeList = eventList.getChildNodes();
        int numEvents = nodeList.getLength();
        for (int idx = 0; idx < numEvents; idx++) {
            Element eventElem = (Element) nodeList.item(idx);
            String country = eventElem.getAttribute("country");
            if ((country != null) && country.equals("US")) {
                album.year = eventElem.getAttribute("date").substring(0, 4);
                break;
            }
        }
    }

    private static Element child(Element element, String tag) {
        if (element == null) {
            return null;
        }
        NodeList nodeList = element.getChildNodes();
        int count = nodeList.getLength();
        for (int idx = 0; idx < count; idx++) {
            Node child = nodeList.item(idx);
            if (child instanceof Element) {
                Element ce = (Element) child;
                if (ce.getTagName().equals(tag)) {
                    return ce;
                }
            }
        }
        return null;
    }

    private static String text(Element element) {
        String value = element.getFirstChild().getNodeValue();
        return Utils.make_ascii(value);
    }

    private void open_album_metadata_dialog(MbAlbum album) {
        this.edit_dialog = new Shell(this.jukebox.main_window,
                SWT.SHELL_TRIM | SWT.MODELESS);
        this.edit_dialog.setText("Edit Album Metadata");

        GridLayout layout = new GridLayout(2, true);
        layout.marginWidth = layout.marginHeight = 20;
        layout.verticalSpacing = 20;
        this.edit_dialog.setLayout(layout);
        GridData data;

        this.textbox = new Text(this.edit_dialog, SWT.MULTI | SWT.BORDER |
                SWT.V_SCROLL | SWT.H_SCROLL);
        data = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        data.widthHint = 315;
        data.heightHint = 315;
        this.textbox.setLayoutData(data);

        Label label = new Label(this.edit_dialog, SWT.SEPARATOR | SWT.HORIZONTAL);
        data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = SWT.FILL;
        data.horizontalSpan = 2;
        label.setLayoutData(data);

        Button button = new Button(this.edit_dialog, SWT.PUSH);
        button.setText("OK");
        data = new GridData(SWT.CENTER, SWT.BEGINNING, true, false);
        data.widthHint = 60;
        button.setLayoutData(data);
        button.setData(SWT.OK);
        button.addSelectionListener(this);

        button = new Button(this.edit_dialog, SWT.PUSH);
        button.setText("Cancel");
        button.setLayoutData(data);
        button.setData(SWT.CANCEL);
        button.addSelectionListener(this);

        add_line(this.textbox, "name", album.name);
        add_line(this.textbox, "artist", album.artist);
        add_line(this.textbox, "year", album.year);
        if (album.tracklist != null) {
            for (int idx = 0; idx < album.tracklist.length; idx++) {
                add_line(this.textbox, Integer.toString(idx+1), album.tracklist[idx]);
            }
        }
        this.textbox.setSelection(0);
        this.textbox.showSelection();

        this.edit_dialog.pack();
        this.edit_dialog.open();
        this.edit_dialog.setActive();
    }

    private void add_line(Text textbox, String key, String value) {
        textbox.append(key + "=" + (value == null ? "" : value) +
                textbox.getLineDelimiter());
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent event) {
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        switch ((int) event.widget.getData()) {
        case SWT.OK:
            String text = this.textbox.getText();
            MbAlbum album = null;
            try {
                album = parse_album_metadata(text);
            } catch (Exception exception) {
                exception.printStackTrace();
                return;
            }
            this.edit_dialog.close();
            // TODO save text in file
            rip_cd_in_thread(album);
            break;
        case SWT.CANCEL:
            this.edit_dialog.close();
            break;
        default:
            break;
        }
    }

    private MbAlbum parse_album_metadata(String text) throws Exception {
        MbAlbum album = new MbAlbum();
        List<String> tracklist = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new StringReader(text));
            String line;
            int line_num = 0;
            while ((line = reader.readLine()) != null) {
                line_num++;
                line = line.trim();
                if (line.equals("") || line.startsWith("#")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx <= 0) {
                    throw new Exception("Line " + line_num + " invalid");
                }
                String tag = line.substring(0,  idx).trim();
                String value = line.substring(idx + 1).trim();
                if (tag.equals("name")) {
                    album.name = value;
                } else if (tag.equals("artist")) {
                    album.artist = value;
                } else if (tag.equals("year")) {
                    album.year = value;
                } else {
                    int track = Utils.parseInt(tag);
                    if (track <= 0 || track > 999) {
                        throw new Exception("Line " + line_num + " invalid");
                    }
                    track--;
                    while (tracklist.size() <= track) {
                        tracklist.add(null);
                    }
                    tracklist.set(track, value);
                }
            }
            if (Utils.isEmpty(album.name)) {
                throw new Exception("album name undefined");
            }
            if (Utils.isEmpty(album.artist)) {
                throw new Exception("album artist undefined");
            }
            album.tracklist = tracklist.toArray(new String[0]);
            return album;
        } finally {
            Utils.quietClose(reader);
        }
    }

    private void rip_cd_in_thread(final MbAlbum album) {
        this.progress_dialog = new ProgressDialog(this.jukebox.main_window,
                "Rip CD...", "Copying CD to Library...");

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                rip_cd_with_metadata(album);
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void rip_cd_with_metadata(MbAlbum album) {
        String home = System.getProperty("user.home");
        File music_dir = new File(home, "xMusic");
        StringBuilder builder = new StringBuilder();
        Utils.name_to_dirname(builder, album.artist);
        builder.append('/');
        Utils.name_to_dirname(builder, album.name);
        File album_dir = new File(music_dir, builder.toString());
        album_dir.mkdirs();

        int total = album.tracklist.length + 1;
        Process lame_process = null;
        File last_wav_file = null;
        for (int idx = 0; idx < album.tracklist.length; idx++) {
            String title = album.tracklist[idx];
            if (Utils.isEmpty(title)) {
                continue;
            }
            String track = Integer.toString(idx + 1);
            this.progress_dialog.set_bottom_label("Copying track " + track + "...");
            this.progress_dialog.set_progress(idx, total);
            if (track.length() < 2) {
                track = "0" + track;
            }
            File wav_file = new File(album_dir, track + ".wav");
            ProcessBuilder pb = new ProcessBuilder("cdparanoia", "-w", track,
                    wav_file.getAbsolutePath());
            Process process = null;
            System.out.println("cdparanoia " + wav_file);
            try {
                process = pb.start();
            } catch (Exception exception) {
                exception.printStackTrace();
                continue;
            }
            if (lame_process != null) {
                finish_lame_process(lame_process, last_wav_file);
                lame_process = null;
            }
            try {
                process.waitFor();
            } catch (Exception exception) {
                exception.printStackTrace();
                continue;
            }
            builder.setLength(0);
            builder.append(track);
            builder.append(" - ");
            Utils.name_to_filename(builder, title);
            builder.append(".mp3");
            File mp3_file = new File(album_dir, builder.toString());
            pb = new ProcessBuilder("lame", "-S", "-V", "3", "--tt", title, "--ta",
                    album.artist, "--tl", album.name, "--ty", album.year, "--tn", track,
                    wav_file.getAbsolutePath(), mp3_file.getAbsolutePath());
            System.out.println("lame " + mp3_file);
            try {
                lame_process = pb.start();
                last_wav_file = wav_file;
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        if (lame_process != null) {
            this.progress_dialog.set_bottom_label("Encoding final track...");
            this.progress_dialog.set_progress(album.tracklist.length, total);
            finish_lame_process(lame_process, last_wav_file);
        }
        System.out.println("done");
        this.progress_dialog.close_and_run(null);
    }

    private void finish_lame_process(Process process, File wav_file) {
        try {
            process.waitFor();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        wav_file.delete();
    }
}
