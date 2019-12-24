package io.autodidact.reanimatedcanvas;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

public class RPathManager extends SimpleViewManager<RPathHandler> {
    final static String NAME = "ReanimatedPathManager";

    @interface Props {
        String ID = "id";
        String POINTS = "points";
        String ANIMATE = "animate";
        String ANIMATION_CONTROLLER = "index";
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
    public void setPathId(RPathHandler view, String id) {
        view.setPathId(id);
    }

    @Override
    public void setRenderToHardwareTexture(@NonNull RPathHandler view, boolean useHWTexture) {
        view.setLayerType(useHWTexture ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_SOFTWARE, null);
    }

    @ReactProp(name = RCanvasManager.Props.STROKE_COLOR)
    public void setStrokeColor(RPathHandler view, int color) {
        view.setStrokeColor(color);
    }

    @ReactProp(name = RCanvasManager.Props.STROKE_WIDTH)
    public void setStrokeWidth(RPathHandler view, float width) {
        view.setStrokeWidth(PixelUtil.toPixelFromDIP(width));
    }

    @ReactProp(name = Props.POINTS)
    public void setPoints(RPathHandler view, ReadableArray points) {
        view.preCommitPoints(Utility.processPointArray(points));
    }

    @ReactProp(name = Props.ANIMATE)
    public void setShouldAnimatePath(RPathHandler view, Boolean animate) {
        view.shouldAnimatePath(animate);
    }

    @ReactProp(name = Props.ANIMATION_CONTROLLER)
    public void setPathAnimationController(RPathHandler view, int index) {
        view.commitPoint(index);
    }

    @ReactProp(name = RCanvasManager.Props.HIT_SLOP)
    public void setHitSlop(RPathHandler view, @Nullable ReadableMap hitSlop) {
        view.setHitSlop(Utility.parseHitSlop(hitSlop), true);
    }

    @Override
    protected void onAfterUpdateTransaction(@NonNull RPathHandler view) {
        super.onAfterUpdateTransaction(view);
        view.onAfterUpdateTransaction();
        if (view.getParent() != null && view.getParent() instanceof RCanvasHandler) {
            ((RCanvasHandler) view.getParent()).finalizeUpdate(view);
        }
    }
}