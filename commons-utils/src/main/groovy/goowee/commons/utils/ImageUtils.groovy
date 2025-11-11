/*
 * Copyright 2021 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package goowee.commons.utils

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer

import javax.imageio.ImageIO
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage

/**
 * Utility class for image manipulation and PDF preview generation.
 * <p>
 * Provides methods to load and save images, convert images to byte arrays,
 * resize, scale, rotate images, and generate previews from PDF files.
 * </p>
 * Author: Gianluca Sartori
 */
@Slf4j
@CompileStatic
class ImageUtils {

    /**
     * Loads an image from a file.
     *
     * @param pathname the path of the image file
     * @return a BufferedImage representing the loaded image
     * @throws IOException if an error occurs during reading the file
     */
    static BufferedImage load(String pathname) {
        return ImageIO.read(new File(pathname))
    }

    /**
     * Saves an image to a file.
     *
     * @param image the BufferedImage to save
     * @param pathname the path where the image will be saved
     * @param format the file format (e.g., GIF, PNG, JPG)
     * @throws Exception if an error occurs while writing the file
     */
    static void save(BufferedImage image, String pathname, ImageUtilsFormat format = getFormatFromFilename(pathname)) {
        File file = new File(pathname)

        try {
            ImageIO.write(image, format.extension, file)

        } catch (IOException e) {
            throw new Exception("Error writing file '${file}': ${e.message}")
        }
    }

    /**
     * Converts a BufferedImage into a byte array.
     *
     * @param image the image to convert
     * @param format the image format (e.g., GIF, PNG, JPG)
     * @return byte array representing the image
     * @throws IOException if an error occurs during writing
     */
    static byte[] toByteArray(BufferedImage image, ImageUtilsFormat format) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ImageIO.write(image, format.extension, baos)
        return baos.toByteArray()
    }

    /**
     * Scales an image proportionally based on the specified width.
     *
     * @param image the image to scale
     * @param width the target width
     * @return the scaled BufferedImage
     */
    static BufferedImage scaleWidth(BufferedImage image , Integer width) {
        return resize(image, width, -1)
    }

    /**
     * Scales an image proportionally based on the specified height.
     *
     * @param image the image to scale
     * @param height the target height
     * @return the scaled BufferedImage
     */
    static BufferedImage scaleHeight(BufferedImage image , Integer height) {
        return resize(image, -1, height)
    }

    /**
     * Resizes an image to the specified width and height.
     * If one dimension is -1, it is automatically calculated to preserve aspect ratio.
     *
     * @param image the image to resize
     * @param width the target width (or -1 to auto calculate)
     * @param height the target height (or -1 to auto calculate)
     * @return the resized BufferedImage
     */
    static BufferedImage resize(BufferedImage image , Integer width, Integer height = -1) {
        Image resizedImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH)
        return imageToBufferedImage(resizedImage)
    }

    /**
     * Rotates an image 90 degrees to the right or left.
     *
     * @param bi the image to rotate
     * @param right if true rotates 90° clockwise, if false 90° counterclockwise
     * @return the rotated BufferedImage
     */
    static BufferedImage rotate(BufferedImage bi, Boolean right = true) {
        Integer w = bi.width
        Integer h = bi.height
        Integer angle = right ? 90 : -90
        BufferedImage rotated = new BufferedImage(h, w, bi.type)
        Graphics2D g2d = rotated.createGraphics()

        AffineTransform at = new AffineTransform()
        at.translate((h - w) / 2 as double, (w - h) / 2 as double)
        at.rotate(Math.toRadians(angle), w/2 as double, h/2 as double)
        g2d.setTransform(at)
        g2d.drawImage(bi, 0, 0, null)
        g2d.dispose()

        return rotated
    }

    /**
     * Generates a preview image from the first page of a PDF file.
     *
     * @param pathname path to the PDF file
     * @param dpi resolution in dots per inch for rendering (default 300)
     * @return a BufferedImage representing the preview
     * @throws IOException if the PDF cannot be loaded or rendered
     */
    static BufferedImage generatePdfPreview(String pathname, Integer dpi = 300) {
        PDDocument pd = PDDocument.load(new File(pathname))
        PDFRenderer pr = new PDFRenderer(pd)
        return pr.renderImageWithDPI(0, dpi)
    }

    /**
     * Determines the image format based on the file extension.
     *
     * @param filename the filename to inspect
     * @return the corresponding ImageUtilsFormat
     */
    static ImageUtilsFormat getFormatFromFilename(String filename) {
        String extension = FileUtils.stripExtension(filename)
        return ImageUtilsFormat.get(extension)
    }

    /**
     * Converts a generic Image object into a BufferedImage.
     *
     * @param img the Image to convert
     * @return a BufferedImage with the same contents as the input Image
     */
    private static BufferedImage imageToBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img
        }

        // Create a buffered image with transparency
        BufferedImage bi = new BufferedImage(
                img.getWidth(null), img.getHeight(null),
                BufferedImage.TYPE_INT_RGB)

        Graphics2D graphics2D = bi.createGraphics()
        graphics2D.drawImage(img, 0, 0, null)
        graphics2D.dispose()

        return bi
    }
}
