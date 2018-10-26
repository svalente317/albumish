/*
 *  Copyright (c) 2014  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TypedListener;

public class MySlider extends Canvas implements PaintListener, MouseListener, MouseMoveListener {

    private static final int CHANNEL_HEIGHT = 4;
    private static final int SLIDER_HEIGHT = 24;
    private static final int SLIDER_WIDTH = 30;

    private int selection;
    private int maximum;
    private int slider_x;
    private int slider_y;
    private boolean is_grabbed;
    private int grab_offset;

    public MySlider(Composite parent) {
        super(parent, SWT.DOUBLE_BUFFERED);
        addPaintListener(this);
        addMouseListener(this);
        addMouseMoveListener(this);
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        return super.computeSize(wHint, hHint, changed);
    }

    @Override
    public void paintControl(PaintEvent event) {
        Point size = getSize();
        if (size.y < CHANNEL_HEIGHT) {
            return;
        }
        event.gc.drawRectangle(0, (size.y - CHANNEL_HEIGHT) / 2, size.x - 1, CHANNEL_HEIGHT - 1);
        int height = SLIDER_HEIGHT;
        if (size.y < height) {
            height = size.y;
        }
        int channel_width = size.x - SLIDER_WIDTH;
        if (channel_width < 0 || this.maximum == 0) {
            return;
        }
        double ratio = (double) channel_width / (double) this.maximum;
        this.slider_x = (int) (ratio * this.selection);
        this.slider_y = (size.y - height) / 2;
        event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_LIST_SELECTION));
        event.gc.fillRectangle(this.slider_x, this.slider_y, SLIDER_WIDTH - 1, height - 1);
        event.gc.drawRectangle(this.slider_x, this.slider_y, SLIDER_WIDTH - 1, height - 1);
    }

    @Override
    public void mouseDoubleClick(MouseEvent event) {
    }

    @Override
    public void mouseDown(MouseEvent event) {
        if (this.maximum == 0) {
            return;
        }
        if (event.x < this.slider_x || event.x > this.slider_x + SLIDER_WIDTH) {
            return;
        }
        if (event.y < this.slider_y || event.y > this.slider_y + SLIDER_HEIGHT) {
            return;
        }
        this.is_grabbed = true;
        this.grab_offset = event.x - this.slider_x;
    }

    @Override
    public void mouseUp(MouseEvent mouseEvent) {
        if (!this.is_grabbed) {
            return;
        }
        this.is_grabbed = false;
        Event event = new Event();
        event.stateMask = mouseEvent.stateMask;
        event.time = mouseEvent.time;
        event.x = mouseEvent.x;
        event.y = mouseEvent.y;
        event.detail = this.selection;
        notifyListeners(SWT.Selection, event);
    }

    @Override
    public void mouseMove(MouseEvent event) {
        if (!this.is_grabbed) {
            return;
        }
        Point size = getSize();
        int channel_width = size.x - SLIDER_WIDTH;
        double ratio = (double) this.maximum / (double) channel_width;
        int new_selection = (int) (ratio * (event.x - this.grab_offset));
        if (new_selection < 0) {
            new_selection = 0;
        }
        if (new_selection > this.maximum) {
            new_selection = this.maximum;
        }
        if (new_selection != this.selection) {
            this.selection = new_selection;
            redraw();
        }
    }

    public void setMaximum(int value) {
        this.maximum = value;
        redraw();
    }

    public void setSelection(int value) {
        if (!this.is_grabbed) {
            this.selection = value;
            redraw();
        }
    }

    public void addSelectionListener(SelectionListener listener) {
        addListener(SWT.Selection, new TypedListener(listener));
    }
}
