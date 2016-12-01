package robertapengelly.support.animation;

import  android.graphics.Camera;
import  android.graphics.Matrix;
import  android.graphics.RectF;
import  android.view.View;
import  android.view.animation.Animation;
import  android.view.animation.Transformation;

import  java.lang.ref.WeakReference;
import  java.util.WeakHashMap;

/**
 * A proxy class to allow for modifying post-3.0 view properties on all pre-3.0
 * platforms.
 */
final class AnimatorProxy extends Animation {

    private static final WeakHashMap<View, AnimatorProxy> PROXIES = new WeakHashMap<>();
    
    private final RectF mAfter = new RectF();
    private final RectF mBefore = new RectF();
    private final Camera mCamera = new Camera();
    private final Matrix mTempMatrix = new Matrix();
    private final WeakReference<View> mView;
    
    private float mAlpha = 1;
    private boolean mHasPivot;
    private float mPivotX;
    private float mPivotY;
    private float mRotationX;
    private float mRotationY;
    private float mRotationZ;
    private float mScaleX = 1;
    private float mScaleY = 1;
    private float mTranslationX;
    private float mTranslationY;
    
    private AnimatorProxy(View view) {
    
        setDuration(0);
        setFillAfter(true);
        
        view.setAnimation(this);
        mView = new WeakReference<>(view);
    
    }
    
    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
    
        View view = mView.get();
        
        if (view != null) {
        
            t.setAlpha(mAlpha);
            transformMatrix(t.getMatrix(), view);
        
        }
    
    }
    
    private void computeRect(final RectF rect, View view) {
    
        final float height = view.getHeight();
        final float width = view.getWidth();
        
        // use a rectangle at 0,0 to make sure we don't run into issues with scaling
        rect.set(0, 0, width, height);
        
        final Matrix matrix = mTempMatrix;
        matrix.reset();
        
        transformMatrix(matrix, view);
        
        mTempMatrix.mapRect(rect);
        rect.offset(view.getLeft(), view.getTop());
        
        // Straighten coords if rotations flipped them
        if (rect.right < rect.left) {
        
            final float right = rect.right;
            
            rect.right = rect.left;
            rect.left = right;
        
        }
        
        if (rect.bottom < rect.top) {
        
            final float top = rect.top;
            
            rect.top = rect.bottom;
            rect.top = top;
        
        }
    
    }
    
    public float getAlpha() {
        return mAlpha;
    }
    
    public float getPivotX() {
        return mPivotX;
    }
    
    public float getPivotY() {
        return mPivotY;
    }
    
    public float getRotation() {
        return mRotationZ;
    }
    
    public float getRotationX() {
        return mRotationX;
    }
    
    public float getRotationY() {
        return mRotationY;
    }
    
    public float getScaleX() {
        return mScaleX;
    }
    
    public float getScaleY() {
        return mScaleY;
    }
    
    public int getScrollX() {
    
        View view = mView.get();
        
        if (view == null)
            return 0;
        
        return view.getScrollX();
    
    }
    
    public int getScrollY() {
    
        View view = mView.get();
        
        if (view == null)
            return 0;
        
        return view.getScrollY();
    
    }
    
    public float getTranslationX() {
        return mTranslationX;
    }
    
    public float getTranslationY() {
        return mTranslationY;
    }
    
    public float getX() {
    
        View view = mView.get();
        
        if (view == null)
            return 0;
        
        return (view.getLeft() + mTranslationX);
    
    }
    
    public float getY() {
    
        View view = mView.get();
        
        if (view == null)
            return 0;
        
        return (view.getTop() + mTranslationY);
    
    }
    
    private void invalidateAfterUpdate() {
    
        View view = mView.get();
        
        if ((view == null) || (view.getParent() == null))
            return;
        
        final RectF after = mAfter;
        
        computeRect(after, view);
        after.union(mBefore);
        
        ((View) view.getParent()).invalidate((int) Math.floor(after.left), (int) Math.floor(after.top),
            (int) Math.ceil(after.right), (int) Math.ceil(after.bottom));
    
    }
    
    private void prepareForUpdate() {
    
        View view = mView.get();
        
        if (view != null)
            computeRect(mBefore, view);
    
    }
    
    public void setAlpha(float alpha) {
    
        if (mAlpha == alpha)
            return;
        
        mAlpha = alpha;
        View view = mView.get();
        
        if (view != null)
            view.invalidate();
    
    }
    
    public void setPivotX(float pivotX) {
    
        if (mHasPivot || (mPivotX == pivotX))
            return;
        
        prepareForUpdate();
        
        mHasPivot = true;
        mPivotX = pivotX;
        
        invalidateAfterUpdate();
    
    }
    
    public void setPivotY(float pivotY) {
    
        if (mHasPivot || (mPivotY == pivotY))
            return;
        
        prepareForUpdate();
        
        mHasPivot = true;
        mPivotY = pivotY;
        
        invalidateAfterUpdate();
    
    }
    
    public void setRotation(float rotation) {
    
        if (mRotationZ == rotation)
            return;
        
        prepareForUpdate();
        
        mRotationZ = rotation;
        
        invalidateAfterUpdate();
    
    }
    
    public void setRotationX(float rotation) {
    
        if (mRotationX == rotation)
            return;
        
        prepareForUpdate();
        
        mRotationX = rotation;
        
        invalidateAfterUpdate();
    
    }
    
    public void setRotationY(float rotation) {
    
        if (mRotationY == rotation)
            return;
        
        prepareForUpdate();
        
        mRotationY = rotation;
        
        invalidateAfterUpdate();
    
    }
    
    public void setScaleX(float scale) {
    
        if (mScaleX == scale)
            return;
        
        prepareForUpdate();
        
        mScaleX = scale;
        
        invalidateAfterUpdate();
    
    }
    
    public void setScaleY(float scale) {
    
        if (mScaleY == scale)
            return;
        
        prepareForUpdate();
        
        mScaleY = scale;
        
        invalidateAfterUpdate();
    
    }
    
    public void setScrollX(int value) {
    
        View view = mView.get();
        
        if (view != null)
            view.scrollTo(value, view.getScrollY());
    
    }
    
    public void setScrollY(int value) {
    
        View view = mView.get();
        
        if (view != null)
            view.scrollTo(view.getScrollX(), value);
    
    }
    
    public void setTranslationX(float value) {
    
        if (mTranslationX == value)
            return;
        
        prepareForUpdate();
        
        mTranslationX = value;
        
        invalidateAfterUpdate();
    
    }
    
    public void setTranslationY(float value) {
    
        if (mTranslationY == value)
            return;
        
        prepareForUpdate();
        
        mTranslationY = value;
        
        invalidateAfterUpdate();
    
    }
    
    public void setX(float value) {
    
        View view = mView.get();
        
        if (view != null)
            setTranslationX(value - view.getLeft());
    
    }
    
    public void setY(float value) {
    
        View view = mView.get();
        
        if (view != null)
            setTranslationY(value - view.getTop());
    
    }
    
    private void transformMatrix(Matrix matrix, View view) {
    
        final float h = view.getHeight();
        final float w = view.getWidth();
        
        final boolean hasPivot = mHasPivot;
        
        final float pX = (hasPivot ? mPivotX : (w / 2f));
        final float pY = (hasPivot ? mPivotY : (h / 2f));
        
        final float rX = mRotationX;
        final float rY = mRotationY;
        final float rZ = mRotationZ;
        
        if ((rX != 0) || (rY != 0) || (rZ != 0)) {
        
            final Camera camera = mCamera;
            camera.save();
            
            camera.rotateX(rX);
            camera.rotateY(rY);
            camera.rotateZ(-rZ);
            
            camera.getMatrix(matrix);
            camera.restore();
            
            matrix.preTranslate(-pX, -pY);
            matrix.postTranslate(pX, pY);
        
        }
        
        final float sX = mScaleX;
        final float sY = mScaleY;
        
        if ((sX != 1.0f) || (sY != 1.0f)) {
        
            matrix.postScale(sX, sY);
            
            final float sPX = -((pX / w) * ((sX * w) - w));
            final float sPY = -((pY / h) * ((sY * h) - h));
            
            matrix.postTranslate(sPX, sPY);
        
        }
        
        matrix.postTranslate(mTranslationX, mTranslationY);
    
    }
    
    /**
     * Create a proxy to allow for modifying post-3.0 view properties on all
     * pre-3.0 platforms.
     *
     * @param view View to wrap.
     *
     * @return Proxy to post-3.0 properties.
     */
    public static AnimatorProxy wrap(View view) {
    
        AnimatorProxy proxy = PROXIES.get(view);
        
        if ((proxy == null) || (proxy != view.getAnimation())) {
        
            proxy = new AnimatorProxy(view);
            PROXIES.put(view, proxy);
        
        }
        
        return proxy;
    
    }

}