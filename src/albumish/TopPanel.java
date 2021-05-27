/*
 *  Copyright (c) 2014  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class TopPanel extends Composite implements SelectionListener {

    private enum Cmd {
        PREV_BUTTON,
        PLAY_BUTTON,
        NEXT_BUTTON,
        FILTER_ENTRY,
        ADD_FILTER_BUTTON,
        DELETE_FILTER_BUTTON,
        TIME_SCALE
    }

    private final Jukebox jukebox;
    private boolean is_play_button;
    private final Image play_icon;
    private final Image pause_icon;
    private final Button play_button;
    private final Label title_label;
    private final Label artist_label;
    private final MySlider time_scale;
    private final Label time_label;
    private final Label duration_label;
    private final Text filter_entry;
    private final Label filter_label;

    public TopPanel(Jukebox jukebox, Composite parent) {
        super(parent, 0);
        this.jukebox = jukebox;

        GridLayout layout = new GridLayout(3, false);
        this.setLayout(layout);

        Composite top_left = new Composite(this, SWT.BORDER);
        GridData data = new GridData(SWT.BEGINNING, SWT.FILL, false, true);
        top_left.setLayoutData(data);
        layout = new GridLayout(3, true);
        top_left.setLayout(layout);

        this.play_icon = jukebox.get_icon("gtk_media_play_ltr.png");
        this.pause_icon = jukebox.get_icon("gtk_media_pause.png");
        Button button;
        button = new Button(top_left, SWT.PUSH);
        button.setImage(jukebox.get_icon("gtk_media_forward_rtl.png"));
        button.setData(Cmd.PREV_BUTTON);
        button.addSelectionListener(this);
        data = new GridData(SWT.FILL, SWT.CENTER, true, true);
        data.widthHint = data.heightHint = 50;
        button.setLayoutData(data);
        this.play_button = new Button(top_left, SWT.PUSH);
        this.play_button.setImage(this.play_icon);
        this.play_button.setData(Cmd.PLAY_BUTTON);
        this.play_button.addSelectionListener(this);
        data = new GridData(SWT.FILL, SWT.CENTER, true, true);
        data.widthHint = data.heightHint = 50;
        this.play_button.setLayoutData(data);
        button = new Button(top_left, SWT.PUSH);
        button.setImage(jukebox.get_icon("gtk_media_forward_ltr.png"));
        button.setData(Cmd.NEXT_BUTTON);
        button.addSelectionListener(this);
        data = new GridData(SWT.FILL, SWT.CENTER, true, true);
        data.widthHint = data.heightHint = 50;
        button.setLayoutData(data);

        Composite label_vbox = new Composite(this, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.FILL, true, true);
        label_vbox.setLayoutData(data);
        layout = new GridLayout(3, false);
        label_vbox.setLayout(layout);
        this.title_label = new Label(label_vbox, SWT.CENTER);
        data = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
        this.title_label.setLayoutData(data);
        this.artist_label = new Label(label_vbox, SWT.CENTER);
        data = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
        this.artist_label.setLayoutData(data);
        this.time_label = new Label(label_vbox, 0);
        data = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
        this.time_label.setLayoutData(data);
        this.time_label.setAlignment(SWT.RIGHT);
        this.time_scale = new MySlider(label_vbox);
        this.time_scale.setData(Cmd.TIME_SCALE);
        this.time_scale.addSelectionListener(this);
        data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        this.time_scale.setLayoutData(data);
        this.duration_label = new Label(label_vbox, 0);
        data = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
        this.duration_label.setLayoutData(data);

        Composite filter_table = new Composite(this, SWT.BORDER);
        data = new GridData(SWT.END, SWT.FILL, false, true);
        filter_table.setLayoutData(data);
        layout = new GridLayout(2, false);
        layout.marginTop = 8;
        filter_table.setLayout(layout);
        this.filter_entry = new Text(filter_table, SWT.BORDER | SWT.SINGLE);
        this.filter_entry.setData(Cmd.FILTER_ENTRY);
        this.filter_entry.addSelectionListener(this);
        data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.widthHint = 170;
        this.filter_entry.setLayoutData(data);
        button = new Button(filter_table, SWT.PUSH);
        button.setImage(jukebox.get_icon("gtk_add.png"));
        button.setData(Cmd.ADD_FILTER_BUTTON);
        button.addSelectionListener(this);
        data = new GridData(SWT.FILL, SWT.FILL, false, false);
        button.setLayoutData(data);
        this.filter_label = new Label(filter_table, 0);
        data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        this.filter_label.setLayoutData(data);
        button = new Button(filter_table, SWT.PUSH);
        button.setImage(jukebox.get_icon("gtk_cancel.png"));
        button.setData(Cmd.DELETE_FILTER_BUTTON);
        button.addSelectionListener(this);
        data = new GridData(SWT.FILL, SWT.FILL, false, false);
        button.setLayoutData(data);

        this.is_play_button = true;
        display_duration(0);
    }

    public void grab_focus() {
        this.filter_entry.setFocus();
    }

    public void display(String song_name, String artist_name, String album_name, int duration) {
        if (song_name == null) {
            this.title_label.setText("");
        } else {
            this.title_label.setText(song_name);
        }
        if (artist_name == null) {
            if (album_name == null) {
                this.artist_label.setText("");
            } else {
                this.artist_label.setText(album_name);
            }
        } else {
            if (album_name == null) {
                this.artist_label.setText(artist_name);
            } else {
                this.artist_label.setText(artist_name + " - " + album_name);
            }
        }
        display_duration(duration);
        if (duration > 0) {
            display_pause_button();
        } else {
            display_play_button();
        }
    }

    private void display_duration(int duration) {
        this.time_scale.setSelection(0);
        if (duration <= 0) {
            this.time_scale.setEnabled(false);
            this.time_label.setText("---:---");
            this.duration_label.setText("---:---");
        } else {
            this.time_scale.setEnabled(true);
            this.time_scale.setMaximum((int) TimeUnit.SECONDS.toMillis(duration));
            this.time_label.setText("0:00");
            int seconds = duration % 60;
            String text = "" + duration / 60 + ":" + (seconds < 10 ? "0" : "") + seconds;
            this.duration_label.setText(text);
        }
    }

    public void display_play_button() {
        if (!this.is_play_button) {
            this.play_button.setImage(this.play_icon);
            this.is_play_button = true;
        }
    }

    public void display_pause_button() {
        if (this.is_play_button) {
            this.play_button.setImage(this.pause_icon);
            this.is_play_button = false;
        }
    }

    public void display_time(int position) {
        this.time_scale.setSelection(position);
        int current_time = (int) TimeUnit.MILLISECONDS.toSeconds(position);
        int seconds = current_time % 60;
        String text = "" + current_time / 60 + ":" + (seconds < 10 ? "0" : "") + seconds;
        this.time_label.setText(text);
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        if (event.widget.getData() == null) {
            return;
        }
        switch ((Cmd) event.widget.getData()) {
        case PREV_BUTTON:
            this.jukebox.do_previous();
            break;
        case PLAY_BUTTON:
            if (this.is_play_button) {
                this.jukebox.do_play();
            } else {
                this.jukebox.do_pause();
            }
            break;
        case NEXT_BUTTON:
            this.jukebox.do_next();
            break;
        case FILTER_ENTRY:
        case ADD_FILTER_BUTTON:
            String text = this.filter_entry.getText();
            if (text == null || text.isEmpty()) {
                return;
            }
            this.jukebox.set_filter(text);
            this.filter_label.setText(text);
            this.filter_entry.setText("");
            break;
        case DELETE_FILTER_BUTTON:
            this.jukebox.set_filter(null);
            this.filter_label.setText("");
            grab_focus();
            break;
        case TIME_SCALE:
            this.jukebox.do_audio_jump((float) event.detail);
            break;
        default:
            break;
        }
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent event) {
        if (event.widget.getData() == Cmd.FILTER_ENTRY) {
            widgetSelected(event);
        }
    }
}
