/* ******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ****************************************************************************** */

package albumish;

/*
 * example snippet: convert between SWT Image and AWT BufferedImage
 *
 * For a list of all SWT example snippets see
 * http://www.eclipse.org/swt/snippets/
 */
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.imgscalr.Scalr;
import org.jaudiotagger.tag.images.Artwork;

public class ImageConverter {

    static ImageData convertToSWT(BufferedImage bufferedImage) {
        if (bufferedImage.getColorModel() instanceof DirectColorModel) {
            DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
            PaletteData palette = new PaletteData(colorModel.getRedMask(),
                    colorModel.getGreenMask(), colorModel.getBlueMask());
            ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
                    colorModel.getPixelSize(), palette);
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    int rgb = bufferedImage.getRGB(x, y);
                    int pixel = palette.getPixel(new RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF,
                            rgb & 0xFF));
                    data.setPixel(x, y, pixel);
                    if (colorModel.hasAlpha()) {
                        data.setAlpha(x, y, (rgb >> 24) & 0xFF);
                    }
                }
            }
            return data;
        }
        if (bufferedImage.getColorModel() instanceof IndexColorModel) {
            IndexColorModel colorModel = (IndexColorModel) bufferedImage.getColorModel();
            int size = colorModel.getMapSize();
            byte[] reds = new byte[size];
            byte[] greens = new byte[size];
            byte[] blues = new byte[size];
            colorModel.getReds(reds);
            colorModel.getGreens(greens);
            colorModel.getBlues(blues);
            RGB[] rgbs = new RGB[size];
            for (int i = 0; i < rgbs.length; i++) {
                rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
            }
            PaletteData palette = new PaletteData(rgbs);
            ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
                    colorModel.getPixelSize(), palette);
            data.transparentPixel = colorModel.getTransparentPixel();
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = new int[1];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    raster.getPixel(x, y, pixelArray);
                    data.setPixel(x, y, pixelArray[0]);
                }
            }
            return data;
        }
        if (bufferedImage.getColorModel() instanceof ComponentColorModel) {
            ComponentColorModel colorModel = (ComponentColorModel) bufferedImage.getColorModel();
            PaletteData palette = new PaletteData(0x0000FF, 0x00FF00, 0xFF0000);
            ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
                    colorModel.getPixelSize(), palette);
            // This is valid because we are using a 3-byte Data model with no transparent pixels
            data.transparentPixel = -1;
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = colorModel.getComponentSize();
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    raster.getPixel(x, y, pixelArray);
                    int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1],
                            pixelArray[2]));
                    data.setPixel(x, y, pixel);
                }
            }
            return data;
        }
        return null;
    }

    /**
     * Read the given file as an AWT image. Then, for each specified size, scale the image to a
     * square with the given size. Then, convert the scaled AWT images to SWT images.
     */
    public static ImageData[] getScaledImages(File file, int... sizes) throws IOException {
        return getScaledImages(ImageIO.read(file), sizes);
    }

    public static ImageData[] getScaledImages(Artwork artwork, int... sizes) throws IOException {
        return getScaledImages((BufferedImage) artwork.getImage(), sizes);
    }

    private static ImageData[] getScaledImages(BufferedImage originalImage, int... sizes) {
        ImageData[] results = new ImageData[sizes.length];
        for (int idx = 0; idx < sizes.length; idx++) {
            BufferedImage image = Scalr.resize(originalImage, sizes[idx]);
            results[idx] = convertToSWT(image);
        }
        return results;
    }
}
