package io.autodidact.reanimatedcanvas;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import io.autodidact.reanimatedcanvas.RPath.ResizeMode;

public class RPathManager extends SimpleViewManager<RPathHandler> {
    final static String NAME = "ReanimatedPathManager";

    @interface Props {
        String ID = "id";
        String POINTS = "points";
        String ANIMATE = "animate";
        String ANIMATION_CONTROLLER = "index";
        String RESIZE_MODE = "resizeMode";
    }

    public RPathManager() {
        super();
    }

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @NonNull
    @Override
    protected RPathHandler createViewInstance(@NonNull ThemedReactContext reactContext) {
        return new RPathHandler(reactContext);
    }

    @ReactProp(name = Props.ID)
    public void setPathId(RPathHandler view, int id) {
        view.setPathId(id);
    }

    @ReactProp(name = RCanvasManager.Props.STROKE_COLOR, customType = "Color")
    public void setStrokeColor(RPathHandler view, int color) {
        view.setStrokeColor(color);
    }

    @ReactProp(name = RCanvasManager.Props.STROKE_WIDTH)
    public void setStrokeWidth(RPathHandler view, float width) {
        view.setStrokeWidth(PixelUtil.toPixelFromDIP(width));
    }

    @ReactProp(name = Props.POINTS)
    public void setPoints(RPathHandler view, ReadableArray points) {
        view.setPoints(Utility.processPointArray(points));
    }

    @ReactProp(name = RCanvasManager.Props.HIT_SLOP)
    public void setHitSlop(RPathHandler view, @Nullable ReadableMap hitSlop) {
        view.setHitSlop(Utility.parseHitSlop(hitSlop), true);
    }

    @ReactProp(name = Props.RESIZE_MODE)
    public void setResizeMode(RPathHandler view, @Nullable @ResizeMode String resizeMode) {
        view.setResizeMode(resizeMode != null ? resizeMode : RPath.ResizeMode.NONE);
    }

    @Override
    protected void onAfterUpdateTransaction(@NonNull RPathHandler view) {
        super.onAfterUpdateTransaction(view);
        view.finalizeUpdate();
    }
}
