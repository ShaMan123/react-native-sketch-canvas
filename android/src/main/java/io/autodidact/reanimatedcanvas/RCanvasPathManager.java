package io.autodidact.reanimatedcanvas;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

public class RCanvasPathManager extends SimpleViewManager<RCanvasPathHandler> {
    final static String NAME = "ReanimatedCanvasPathManager";

    @interface Props {
        String ID = "id";
        String POINTS = "points";
        String ANIMATE = "animate";
        String ANIMATION_CONTROLLER = "index";
    }

    public RCanvasPathManager() {
        super();
    }

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @NonNull
    @Override
    protected RCanvasPathHandler createViewInstance(@NonNull ThemedReactContext reactContext) {
        return new RCanvasPathHandler(reactContext);
    }

    @ReactProp(name = Props.ID)
    public void setPathId(RCanvasPathHandler view, String id) {
        view.setPathId(id);
    }

    @ReactProp(name = RCanvasManager.Props.HARDWARE_ACCELERATED)
    public void setHardwareAccelerated(RCanvasPathHandler view, boolean useAcceleration) {
        view.setHardwareAcceleration(useAcceleration);
    }

    @ReactProp(name = RCanvasManager.Props.STROKE_COLOR)
    public void setStrokeColor(RCanvasPathHandler view, int color) {
        view.setStrokeColor(color);
    }

    @ReactProp(name = RCanvasManager.Props.STROKE_WIDTH)
    public void setStrokeWidth(RCanvasPathHandler view, float width) {
        view.setStrokeWidth(PixelUtil.toPixelFromDIP(width));
    }

    @ReactProp(name = Props.POINTS)
    public void setPoints(RCanvasPathHandler view, ReadableArray points) {
        view.preCommitPoints(Utility.processPointArray(points));
    }

    @ReactProp(name = Props.ANIMATE)
    public void setShouldAnimatePath(RCanvasPathHandler view, Boolean animate) {
        view.shouldAnimatePath(animate);
    }

    @ReactProp(name = Props.ANIMATION_CONTROLLER)
    public void setPathAnimationController(RCanvasPathHandler view, int index) {
        view.commitPoint(index);
    }

    @ReactProp(name = RCanvasManager.Props.HIT_SLOP)
    public void setHitSlop(RCanvasPathHandler view, @Nullable ReadableMap hitSlop) {
        view.setHitSlop(Utility.parseHitSlop(hitSlop), true);
    }

    @Override
    protected void onAfterUpdateTransaction(@NonNull RCanvasPathHandler view) {
        super.onAfterUpdateTransaction(view);
        view.onAfterUpdateTransaction();
        if (view.getParent() != null && view.getParent() instanceof RCanvasHandler) {
            ((RCanvasHandler) view.getParent()).finalizeUpdate(view);
        }
    }
}
