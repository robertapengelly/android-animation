package robertapengelly.support.animation;

import  android.view.animation.Interpolator;

import  java.util.ArrayList;

/**
 * This is the superclass for classes which provide basic support for animations which can be started, ended, and have
 * <code>AnimatorListeners</code> added to them.
 */
public abstract class Animator implements Cloneable {

    /** The set of listeners to be sent events through the life of an animation. */
    ArrayList<AnimatorListener> mListeners = null;
    
    /**
     * Adds a listener to the set of listeners that are sent events through the life of an animation, such as start, repeat,
     * and end.
     *
     * @param listener the listener to be added to the current set of listeners for this animation.
     */
    public void addListener(AnimatorListener listener) {
    
        if (mListeners == null)
            mListeners = new ArrayList<>();
        
        mListeners.add(listener);
    
    }
    
    /**
     * Cancels the animation. Unlike {@link #end()}, <code>cancel()</code> causes the animation to stop in its tracks,
     * sending an {@link AnimatorListener#onAnimationCancel(Animator)} to its listeners, followed by an
     * {@link AnimatorListener#onAnimationEnd(Animator)} message.
     *
     * <p>This method must be called on the thread that is running the animation.</p>
     */
    public void cancel() {}
    
    @Override
    public Animator clone() {
    
        try {
        
            final Animator anim = (Animator) super.clone();
            
            if (mListeners != null)
                anim.mListeners = new ArrayList<>(mListeners);
            
            return anim;
        
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    
    }
    
    /**
     * Gets the length of the animation.
     *
     * @return The length of the animation, in milliseconds.
     */
    public abstract long getDuration();
    
    /**
     * Gets the set of {@link AnimatorListener} objects that are currently listening for events on this <code>Animator</code>
     * object.
     *
     * @return ArrayList<AnimatorListener> The set of listeners.
     */
    public ArrayList<AnimatorListener> getListeners() {
        return mListeners;
    }
    
    /**
     * The amount of time, in milliseconds, to delay starting the animation after {@link #start()} is called.
     *
     * @return the number of milliseconds to delay running the animation
     */
    public abstract long getStartDelay();
    
    /**
     * Ends the animation. This causes the animation to assign the end value of the property being animated, then calling the
     * {@link AnimatorListener#onAnimationEnd(Animator)} method on its listeners.
     *
     * <p>This method must be called on the thread that is running the animation.</p>
     */
    public void end() {}
    
    /**
     * Returns whether this Animator is currently running (having been started and gone past any initial startDelay period
     * and not yet ended).
     *
     * @return Whether the Animator is running.
     */
    public abstract boolean isRunning();
    
    /**
     * Returns whether this Animator has been started and not yet ended. This state is a superset of the state of
     * {@link #isRunning()}, because an Animator with a nonzero {@link #getStartDelay() startDelay} will return true for
     * <code>isStarted()</code> during the delay phase, whereas {@link #isRunning()} will return true only after the delay
     * phase is complete.
     *
     * @return Whether the Animator has been started and not yet ended.
     */
    public boolean isStarted() {
    
        // Default method returns value for isRunning(). Subclasses should override to return a real value.
        return isRunning();
    
    }
    
    /**
     * Removes all listeners from this object. This is equivalent to calling <code>getListeners()</code> followed by calling
     * <code>clear()</code> on the returned list of listeners.
     */
    public void removeAllListeners() {
    
        if (mListeners != null) {
        
            mListeners.clear();
            mListeners = null;
        
        }
    
    }
    
    /**
     * Removes a listener from the set listening to this animation.
     *
     * @param listener the listener to be removed from the current set of listeners for this animation.
     */
    public void removeListener(AnimatorListener listener) {
    
        if (mListeners == null)
            return;
        
        mListeners.remove(listener);
        
        if (mListeners.size() == 0)
            mListeners = null;
    
    }
    
    /**
     * Sets the length of the animation.
     *
     * @param duration The length of the animation, in milliseconds.
     */
    public abstract Animator setDuration(long duration);
    
    /**
     * The interpolator used in calculating the elapsed fraction of this animation. The interpolator determines whether the
     * animation runs with linear or non-linear motion, such as acceleration and deceleration.
     * The default value is {@link android.view.animation.AccelerateDecelerateInterpolator}
     *
     * @param value the interpolator to be used by this animation
     */
    public abstract void setInterpolator(Interpolator value);
    
    /**
     * The amount of time, in milliseconds, to delay starting the animation after {@link #start()} is called.
     *
     * @param startDelay The amount of the delay, in milliseconds
     */
    public abstract void setStartDelay(long startDelay);
    
    /**
     * Sets the target object whose property will be animated by this animation. Not all subclasses operate on target
     * objects (for example, {@link ValueAnimator}, but this method is on the superclass for the convenience of dealing
     * generically with those subclasses that do handle targets.
     *
     * @param target The object being animated
     */
    public void setTarget(Object target) {}
    
    /**
     * This method tells the object to use appropriate information to extract ending values for the animation.
     * For example, a AnimatorSet object will pass this call to its child objects to tell them to set up the values.
     * A ObjectAnimator object will use the information it has about its target object and PropertyValuesHolder objects to
     * get the start values for its properties.
     * An ValueAnimator object will ignore the request since it does not have enough information (such as a target object)
     * to gather these values.
     */
    public void setupEndValues() {}
    
    /**
     * This method tells the object to use appropriate information to extract starting values for the animation.
     * For example, a AnimatorSet object will pass this call to its child objects to tell them to set up the values.
     * A ObjectAnimator object will use the information it has about its target object and PropertyValuesHolder objects to
     * get the start values for its properties.
     * An ValueAnimator object will ignore the request since it does not have enough information (such as a target object)
     * to gather these values.
     */
    public void setupStartValues() {}
    
    /**
     * Starts this animation. If the animation has a nonzero startDelay, the animation will start running after that delay
     * elapses. A non-delayed animation will have its initial value(s) set immediately, followed by calls to
     * {@link AnimatorListener#onAnimationStart(Animator)} for any listeners of this animator.
     *
     * <p>The animation started by calling this method will be run on the thread that called this method.
     * This thread should have a Looper on it (a runtime exception will be thrown if this is not the case).
     * Also, if the animation will animate properties of objects in the view hierarchy, then the calling thread should be
     * the UI thread for that view hierarchy.</p>
     */
    public void start() {}
    
    /**
     * <p>An animation listener receives notifications from an animation.
     * Notifications indicate animation related events, such as the end or the repetition of the animation.</p>
     */
    public interface AnimatorListener {
    
        /**
         * <p>Notifies the cancellation of the animation. This callback is not invoked for animations with repeat count set
         * to INFINITE.</p>
         *
         * @param animation The animation which was canceled.
         */
        void onAnimationCancel(Animator animation);
        
        /**
         * <p>Notifies the end of the animation. This callback is not invoked for animations with repeat count set to
         * INFINITE.</p>
         *
         * @param animation The animation which reached its end.
         */
        void onAnimationEnd(Animator animation);
        
        /**
         * <p>Notifies the repetition of the animation.</p>
         *
         * @param animation The animation which was repeated.
         */
        void onAnimationRepeat(Animator animation);
        
        /**
         * <p>Notifies the start of the animation.</p>
         *
         * @param animation The started animation.
         */
        void onAnimationStart(Animator animation);
    
    }

}