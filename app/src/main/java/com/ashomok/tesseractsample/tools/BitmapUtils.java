package com.ashomok.tesseractsample.tools;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by Jc Miñarro (josecarlos.minarro@gmail.com) on 26/01/17.
 */

public class BitmapUtils {

    private static final int RGB_MASK = 0x00FFFFFF;

    public static Bitmap invertColor(Bitmap bitmap) {
        Bitmap invertedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
        for (int col = 0; col < bitmap.getWidth(); col++) {
            for (int row = 0; row < bitmap.getHeight(); row++) {
                invertedBitmap.setPixel(col, row, bitmap.getPixel(col, row) ^ RGB_MASK);
            }
        }
        return invertedBitmap;
    }

    public static Bitmap noiseReduction(Bitmap bitmap) {
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
        for (int col = 0; col < bitmap.getWidth(); col++) {
            for (int row = 0; row < bitmap.getHeight(); row++) {
                int pixel = bitmap.getPixel(col, row);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);
                result.setPixel(col, row, pixel);
                if (red > 162 && green > 162 && blue > 162) {
                    result.setPixel(col, row, Color.WHITE);
                }
                if (red < 162 && green < 162 && blue < 162) {
                    result.setPixel(col, row, Color.BLACK);
                }
            }
        }
        return result;
    }

    public static Bitmap convertGreyScale(Bitmap bitmap) {
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
        for (int col = 0; col < bitmap.getWidth(); col++) {
            for (int row = 0; row < bitmap.getHeight(); row++) {
                int pixel = bitmap.getPixel(col, row);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);
                int grey = (byte)(.299 * red + .587 * green + .114 * blue);
                result.setPixel(col, row, grey);
            }
        }

        return result;
    }

    public static Bitmap convertToMonocromoBitmap(Bitmap bitmap) {
        Bitmap bwBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
        float[] hsv = new float[ 3 ];
        for( int col = 0; col < bitmap.getWidth(); col++ ) {
            for( int row = 0; row < bitmap.getHeight(); row++ ) {
                Color.colorToHSV( bitmap.getPixel( col, row ), hsv );
                if( hsv[ 2 ] > 0.5f ) {
                    bwBitmap.setPixel( col, row, 0xffffffff );
                } else {
                    bwBitmap.setPixel( col, row, 0xff000000 );
                }
            }
        }
        return bwBitmap;
    }

    public static void storeBitmapIntoFile(Bitmap bitmap, String fileName) {
        File file = new File(fileName);
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
