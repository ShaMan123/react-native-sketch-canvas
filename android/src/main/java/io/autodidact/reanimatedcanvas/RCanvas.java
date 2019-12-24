package io.autodidact.reanimatedcanvas;

import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.JSApplicationIllegalArgumentException;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.views.view.ReactViewGroup;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Stack;

import static io.autodidact.reanimatedcanvas.RCanvasManager.TAG;

public class RCanvas extends ReactViewGroup {

    protected ArrayList<RPath> mPaths = new ArrayList<>();
    protected ArrayList<String> mInteractionContainer = new ArrayList<>();
    protected RectF mHitSlop = new RectF();
    private RPath mNextPath;
    protected Stack<RCanvasState> mStateStack;
    private final IntersectionHelper mIntersectionHelper;

    public RCanvas(ThemedReactContext context) {
        super(context);
        mIntersectionHelper = new IntersectionHelper(this);
        mStateStack = new Stack<>();
        mStateStack.push(new RCanvasState());
        allocNext();
        save();
    }

    public IntersectionHelper getIntersectionHelper(){
        return mIntersectionHelper;
    }

    public void setStrokeColor(int color) {
        RCanvasState currentState = mStateStack.peek();
        currentState.strokeColor = color;
    }

    public void setStrokeWidth(float width) {
        RCanvasState currentState = mStateStack.peek();
        currentState.strokeWidth = width;
    }

    public int save() {
        mStateStack.push(mStateStack.peek());
        for (RPath path: paths()) {
            path.save();
        }
        return mStateStack.size() - 1;
    }

    public ArrayList<RPath> restore(int saveCount) {
        if (saveCount == -1) {
            saveCount = Math.max(mStateStack.size() - 1, 0);
        } else if (saveCount >= mStateStack.size() || saveCount < 0) {
            throw new JSApplicationIllegalArgumentException(String.format(Locale.ENGLISH, "%s: bad save count %d", TAG, saveCount));
        }

        ArrayList<RPath> changedPaths = new ArrayList<>();
        mStateStack.setSize(saveCount + 1);
        Log.d(TAG, "restore: " + saveCount + "  " + mStateStack.peek());
        for (RPath path: paths()) {
            if (path.restore(saveCount)) {
                changedPaths.add(path);
            }
        }

        postInvalidateOnAnimation();
        return changedPaths;
    }

    public void setHitSlop(RectF hitSlop){
        mHitSlop = hitSlop;
        for (RPath path: paths()) {
            path.setHitSlop(mHitSlop);
        }
    }

    public ArrayList<RPath> paths() {
        return new ArrayList<>(mPaths);
    }

    public RPath getPath(String id) {
        for (RPath path: paths()) {
            if (path.getPathId() != null && path.getPathId().equals(id)) {
                return path;
            }
        }

        throw new JSApplicationIllegalArgumentException(String.format(Locale.ENGLISH, "%s failed to find path#%s", TAG, id));
    }

    public int getPathIndex(String pathId) {
        ArrayList<RPath> paths = paths();
        RPath path;
        for (int i = 0; i < paths.size(); i++) {
            path = paths.get(i);
            if(path.getPathId() != null && path.getPathId().equals(pathId)) {
                return i;
            }
        }
        return -1;
    }

    private void allocNext() {
        mNextPath = new RPath((ReactContext) getContext());
        addView(mNextPath, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public String init() {
        RCanvasState currentState = mStateStack.peek();
        String pathId = Utility.generateId();
        init(pathId, currentState.strokeColor, currentState.strokeWidth);
        return pathId;
    }

    public void init(String pathId, @Nullable Integer strokeColor, @Nullable Float strokeWidth) {
        RCanvasState currentState = mStateStack.peek();
        strokeColor = strokeColor == null ? currentState.strokeColor : strokeColor;
        strokeWidth = strokeWidth == null ? currentState.strokeWidth : strokeWidth;
        RPath path = init(pathId);
        path.setStrokeColor(strokeColor);
        path.setStrokeWidth(strokeWidth);
    }

    protected RPath init(String pathId) {
        if (getPathIndex(pathId) == -1) {
            RPath path = mNextPath;
            path.setPathId(pathId);
            path.setHitSlop(mHitSlop);
            mPaths.add(path);
            allocNext();
            return path;
        } else {
            throw new JSApplicationIllegalArgumentException(String.format(Locale.ENGLISH, "%s: path#%s already exists", TAG, pathId));
        }
    }

    public void drawPoint(String pathId, PointF point) {
        UiThreadUtil.assertOnUiThread();
        if (mInteractionContainer.indexOf(pathId) == -1) {
            mInteractionContainer.add(pathId);
        }
        getPath(pathId).addPoint(point);
        postInvalidateOnAnimation();
    }

    public void endInteraction(String pathId) {
        mInteractionContainer.remove(pathId);
    }

    public void clear() {
        removePaths(filterPaths(paths(), false));
        postInvalidateOnAnimation();
    }

    protected void removePaths(final ArrayList<RPath> paths) {
        mPaths.removeAll(paths);
        for (RPath path: paths) {
            removeView(path);
        }
    }

    protected ArrayList<RPath> filterPaths(final ArrayList<RPath> paths, final boolean pathInteractionInProgress) {
        ArrayList<RPath> filteredList = new ArrayList<>();
        for (RPath path: paths) {
            if ((mInteractionContainer.indexOf(path.getPathId()) > -1) == pathInteractionInProgress) {
                filteredList.add(path);
            }
        }
        return filteredList;
    }

    public void tearDown(){

    }
}