package com.stardust.autojs.core.image;

import android.graphics.Color;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.stardust.util.ScreenMetrics;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

/**
 * Created by Stardust on 2017/5/18.
 */

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class ColorFinder {

    private ScreenMetrics mScreenMetrics;

    public ColorFinder(ScreenMetrics screenMetrics) {
        mScreenMetrics = screenMetrics;
    }

    public Point findColorEquals(ImageWrapper imageWrapper, int color) {
        return findColorEquals(imageWrapper, color, null);
    }

    public Point findColorEquals(ImageWrapper imageWrapper, int color, Rect region) {
        return findColor(imageWrapper, color, 0, region);
    }

    public Point findColor(ImageWrapper imageWrapper, int color, int threshold) {
        return findColor(imageWrapper, color, threshold, null);
    }

    public Point findColor(ImageWrapper image, int color, int threshold, Rect rect) {
        MatOfPoint matOfPoint = findColorInner(image, color, threshold, rect);
        if (matOfPoint == null) {
            return null;
        }
        Point point = matOfPoint.toArray()[0];
        if (rect != null) {
            point.x = mScreenMetrics.scaleX((int) (point.x + rect.x));
            point.y = mScreenMetrics.scaleX((int) (point.y + rect.y));
        }
        return point;
    }

    public Point[] findAllPointsForColor(ImageWrapper image, int color, int threshold, Rect rect) {
        MatOfPoint matOfPoint = findColorInner(image, color, threshold, rect);
        if (matOfPoint == null) {
            return new Point[0];
        }
        Point[] points = matOfPoint.toArray();
        if (rect != null) {
            for (int i = 0; i < points.length; i++) {
                points[i].x = mScreenMetrics.scaleX((int) (points[i].x + rect.x));
                points[i].y = mScreenMetrics.scaleX((int) (points[i].y + rect.y));
            }
        }
        return points;
    }

    private MatOfPoint findColorInner(ImageWrapper image, int color, int threshold, Rect rect) {
        Mat bi = new Mat();
        Scalar lowerBound = new Scalar(Color.red(color) - threshold, Color.green(color) - threshold,
                Color.blue(color) - threshold, 255);
        Scalar upperBound = new Scalar(Color.red(color) + threshold, Color.green(color) + threshold,
                Color.blue(color) + threshold, 255);
        if (rect != null) {
            Core.inRange(new Mat(image.getMat(), rect), lowerBound, upperBound, bi);
        } else {
            Core.inRange(image.getMat(), lowerBound, upperBound, bi);
        }
        Mat nonZeroPos = new Mat();
        Core.findNonZero(bi, nonZeroPos);
        if (nonZeroPos.rows() == 0 || nonZeroPos.cols() == 0) {
            return null;
        }
        return new MatOfPoint(nonZeroPos);
    }

    public Point findMultiColors(ImageWrapper image, int firstColor, int threshold, Rect rect, int[] points) {
        Point[] firstPoints = findAllPointsForColor(image, firstColor, threshold, rect);
        for (Point firstPoint : firstPoints) {
            if (firstPoint == null)
                continue;
            if (checksPath(image, firstPoint, threshold, rect, points)) {
                return firstPoint;
            }
        }
        return null;
    }

    private boolean checksPath(ImageWrapper image, Point startingPoint, int threshold, Rect rect, int[] points) {
        for (int i = 0; i < points.length; i += 3) {
            int x = points[i];
            int y = points[i + 1];
            int color = points[i + 2];
            ColorDetector colorDetector = new ColorDetector.DifferenceDetector(color, threshold);
            int c = image.pixel((int) (x + startingPoint.x), (int) (y + startingPoint.y));
            if (!colorDetector.detectsColor(Color.red(c), Color.green(c), Color.blue(c))) {
                return false;
            }

        }
        return true;
    }
}