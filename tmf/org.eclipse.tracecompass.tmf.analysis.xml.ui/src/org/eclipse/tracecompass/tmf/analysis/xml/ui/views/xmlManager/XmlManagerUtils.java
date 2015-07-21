package org.eclipse.tracecompass.tmf.analysis.xml.ui.views.xmlManager;

import org.eclipse.swt.layout.GridLayout;

/**
 * This class contains some utilities for the XmlManagerViewer.
 *
 * @author Jonathan Sauvé
 */
public class XmlManagerUtils {
    /** Make this class non-instantiable */
    private XmlManagerUtils() {

    }

    /**
     * Remove the hashtag char from an hexadecimal color value
     * @param s
     *              The hexa color (with '#')
     * @return
     *              The hexa color (without '#')
     */
    public static String removeHashTag(String s)
    {
        return (s.charAt(0)=='#') ? s.substring(1,7):s;
    }

    /**
     * Convert hexadecimal color value to red component (int)
     * @param s
     *              The hexa color (without '#')
     * @return
     *              The red component
     */
    public static int hexaToRed(String s)
    {
        return Integer.parseInt(removeHashTag(s).substring(0, 2), 16);
    }

    /**
     * Convert hexadecimal color value to green component (int)
     * @param s
     *              The hexa color (without '#')
     * @return
     *              The green component
     */
    public static int hexaToGreen(String s)
    {
        return Integer.parseInt(removeHashTag(s).substring(2, 4), 16);
    }

    /**
     * Convert hexadecimal color value to blue component (int)
     * @param s
     *              The hexa color (without '#')
     * @return
     *              The blue component
     */
    public static int hexaToBlue(String s)
    {
        return Integer.parseInt(removeHashTag(s).substring(4, 6), 16);
    }

    /**
     * Convert rbg color (int) to an hexadecimal string
     * @param r
     *              The red component
     * @param g
     *              The green component
     * @param b
     *              The blue component
     * @return
     *              The hexa value of the rgb
     */
    public static String rgbToHexa(int r, int g, int b)
    {
        int red = Math.max(0, Math.min(r,255));
        int green = Math.max(0, Math.min(g,255));
        int blue = Math.max(0, Math.min(b,255));

        String hexaValues = "0123456789ABCDEF"; //$NON-NLS-1$
        char[] cRed = {hexaValues.charAt((red - red%16)/16), hexaValues.charAt(red%16)};
        char[] cGreen = {hexaValues.charAt((green - green%16)/16), hexaValues.charAt(green%16)};
        char[] cBlue = {hexaValues.charAt((blue - blue%16)/16), hexaValues.charAt(blue%16)};

        String sRed = new String(cRed);
        String sGreen = new String(cGreen);
        String sBlue = new String(cBlue);

        return "#" + sRed + sGreen + sBlue; //$NON-NLS-1$
    }

    /**
     * Create a new GridLayout
     * @param numColumns
     *              The number of columns
     * @param marginWidth
     *              The number of pixels of horizontal margin that will
     *              be placed along the left and right edges of the layout.
     * @param marginHeight
     *              The number of pixels of vertical margin that will
     *              be placed along the top and bottom edges of the layout.
     * @return
     *              The new GridLayout
     * */
    public static GridLayout createGridLayout(int numColumns, int marginWidth, int marginHeight) {
        GridLayout grid = new GridLayout(numColumns, false);
        grid.horizontalSpacing = 0; grid.verticalSpacing = 0;
        grid.marginWidth = marginWidth; grid.marginHeight = marginHeight;
        return grid;
    }
}
