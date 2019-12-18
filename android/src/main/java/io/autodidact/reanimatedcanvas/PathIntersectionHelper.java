package io.autodidact.reanimatedcanvas;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableArray;

import java.util.ArrayList;

public class PathIntersectionHelper {

    private final RCanvas mView;

    public PathIntersectionHelper(RCanvas view) {
        mView = view;
    }

    private ArrayList<RCanvasPath> getPaths() {
        return mView.getPaths();
    }

    private int getIndex(String pathId) {
        return mView.getPathIndex(pathId);
    }

    @TargetApi(19)
    boolean isTransparent(PointF point, String pathId){
        ArrayList<RCanvasPath> mPaths = getPaths();
        int start = getIndex(pathId);
        //int beginAt = Math.min(start + 1, mPaths.size() - 1);
        for (int i = start; i < mPaths.size(); i++){
            RCanvasPath mPath = mPaths.get(i);
            if(mPath.isPointOnPath(point) && mPath.getStrokeColor() == Color.TRANSPARENT) {
                return true;
            }
        }
        return false;
    }

    @TargetApi(19)
    public boolean isPointOnPath(PointF point, String pathId){
        if(isTransparent(point, pathId)) {
            return false;
        }
        else {
            RCanvasPath mPath = getPaths().get(getIndex(pathId));
            return mPath.isPointOnPath(point);
        }
    }

    @TargetApi(19)
    public WritableArray isPointOnPath(PointF point){
        WritableArray array = Arguments.createArray();
        RCanvasPath mPath;
        ArrayList<RCanvasPath> paths = getPaths();
        String id;

        if (BuildConfig.DEBUG) {
            DebugRect.draw(mView, point);
        }

        for (int i = 0; i < paths.size(); i++) {
            mPath = paths.get(i);
            id = mPath.getPathId();
            if (mPath.isPointOnPath(point) && !isTransparent(point, id)) {
                array.pushString(id);
            }
        }

        return array;
    }

    @TargetApi(19)
    static class IntersectionOperator {

        static boolean intersectsPath(final PointF point, final RectF hitSlop, final Path path) {
            RectF finalHitRect = Utility.applyHitSlop(point, hitSlop);
            Rect roundedHitRect = new Rect();
            finalHitRect.roundOut(roundedHitRect);

            Path mTouchPath = new Path();
            mTouchPath.addRect(finalHitRect, Path.Direction.CW);

            mTouchPath.op(path, Path.Op.INTERSECT);
            return !mTouchPath.isEmpty();
        }


        static boolean intersetsRegion(final PointF point, final RectF hitSlop, final Path path) {
            RectF finalHitRect = Utility.applyHitSlop(point, hitSlop);
            Rect roundedHitRect = new Rect();
            finalHitRect.roundOut(roundedHitRect);
            Region region1 = new Region();
            region1.set(roundedHitRect);
            region1.setPath(path, region1);
            return !region1.isEmpty();
        }

        //  see: https://stackoverflow.com/questions/11184397/path-intersection-in-android
        boolean intersectsWithRegions(final PointF point, final RectF hitSlop, final Path path, final Region boundingRegion /* Utility.getViewRegion((View) getParent()) */) {
            RectF finalHitRect = Utility.applyHitSlop(point, hitSlop);
            Rect roundedHitRect = new Rect();
            finalHitRect.roundOut(roundedHitRect);

            Region region1 = new Region();
            region1.set(roundedHitRect);
            Region region2 = new Region();
            region2.setPath(path, boundingRegion);

            return !region1.quickReject(region2) && region1.op(region2, Region.Op.INTERSECT);
        }
    }

    static class DebugRect extends RCanvasPath {
        private RectF mRect;
        private Paint mPaint;
        DebugRect(RCanvas view,PointF point) {
            super((ReactContext) view.getContext());
            mRect = Utility.applyHitSlop(point, view.mHitSlop);
            setStrokeColor(Color.BLUE);
            setStrokeWidth(10);
            mPaint = getPaint();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawRect(mRect, mPaint);
        }

        static void draw(final RCanvas view, final PointF point) {
            final DebugRect debugRect = new DebugRect(view, point);
            view.addView(debugRect);
            view.postInvalidateOnAnimation();
            view.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            view.removeView(debugRect);
                            view.postInvalidateOnAnimation();
                        }
                    },
                    1500
            );
        }
    }
}