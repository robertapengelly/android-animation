package robertapengelly.support.animation;

import  android.os.Build;
import  android.view.View;

import  java.util.HashMap;

import  robertapengelly.support.util.Property;

/**
 * This subclass of {@link ValueAnimator} provides support for animating properties on target objects.
 * The constructors of this class take parameters to define the target object that will be animated
 * as well as the name of the property that will be animated. Appropriate set/get functions
 * are then determined internally and the animation will call these functions as necessary to
 * animate the property.
 *
 * @see #setPropertyName(String)
 *
 */
public final class ObjectAnimator extends ValueAnimator {

    /** Whether or not the current running platform needs to be proxied. */
    private static final boolean NEEDS_PROXY = (Build.VERSION.SDK_INT < 11);
    private static final HashMap<String, Property> PROXY_PROPERTIES = new HashMap<>();
    
    static {
        PROXY_PROPERTIES.put("alpha", PreHoneycombCompat.ALPHA);
        PROXY_PROPERTIES.put("pivotX", PreHoneycombCompat.PIVOT_X);
        PROXY_PROPERTIES.put("pivotY", PreHoneycombCompat.PIVOT_Y);
        PROXY_PROPERTIES.put("rotation", PreHoneycombCompat.ROTATION);
        PROXY_PROPERTIES.put("rotationX", PreHoneycombCompat.ROTATION_X);
        PROXY_PROPERTIES.put("rotationY", PreHoneycombCompat.ROTATION_Y);
        PROXY_PROPERTIES.put("scaleX", PreHoneycombCompat.SCALE_X);
        PROXY_PROPERTIES.put("scaleY", PreHoneycombCompat.SCALE_Y);
        PROXY_PROPERTIES.put("scrollX", PreHoneycombCompat.SCROLL_X);
        PROXY_PROPERTIES.put("scrollY", PreHoneycombCompat.SCROLL_Y);
        PROXY_PROPERTIES.put("translationX", PreHoneycombCompat.TRANSLATION_X);
        PROXY_PROPERTIES.put("translationY", PreHoneycombCompat.TRANSLATION_Y);
        PROXY_PROPERTIES.put("x", PreHoneycombCompat.X);
        PROXY_PROPERTIES.put("y", PreHoneycombCompat.Y);
    }
    
    private Property mProperty;
    
    private String mPropertyName;
    
    // The target object on which the property exists, set in the constructor
    private Object mTarget;
    
    /**
     * Creates a new ObjectAnimator object. This default constructor is primarily for
     * use internally; the other constructors which take parameters are more generally
     * useful.
     */
    public ObjectAnimator() {}
    
    /**
     * Private utility constructor that initializes the target object and name of the
     * property being animated.
     *
     * @param target        The object whose property is to be animated. This object should
     *                      have a public method on it called <code>setName()</code>, where <code>name</code> is
     *                      the value of the <code>propertyName</code> parameter.
     * @param propertyName  The name of the property being animated.
     */
    private ObjectAnimator(Object target, String propertyName) {
    
        mTarget = target;
        setPropertyName(propertyName);
    
    }
    
    /**
     * Private utility constructor that initializes the target object and property being animated.
     *
     * @param target    The object whose property is to be animated.
     * @param property  The property being animated.
     */
    private <T> ObjectAnimator(T target, Property<T, ?> property) {
    
        mTarget = target;
        setProperty(property);
    
    }
    
    /**
     * This method is called with the elapsed fraction of the animation during every
     * animation frame. This function turns the elapsed fraction into an interpolated fraction
     * and then into an animated value (from the evaluator. The function is called mostly during
     * animation updates, but it is also called when the <code>end()</code>
     * function is called, to set the final value on the property.
     *
     * <p>Overrides of this method must call the superclass to perform the calculation
     * of the animated value.</p>
     *
     * @param fraction The elapsed fraction of the animation.
     */
    @Override
    void animateValue(float fraction) {
        super.animateValue(fraction);
        
        for (PropertyValuesHolder holder: mValues)
            holder.setAnimatedValue(mTarget);
    
    }
    
    @Override
    public ObjectAnimator clone() {
        return (ObjectAnimator) super.clone();
    }
    
    /**
     * Gets the name of the property that will be animated. This name will be used to derive
     * a setter function that will be called to set animated values.
     * For example, a property name of <code>foo</code> will result
     * in a call to the function <code>setFoo()</code> on the target object. If either
     * <code>valueFrom</code> or <code>valueTo</code> is null, then a getter function will
     * also be derived and called.
     */
    public String getPropertyName() {
        return mPropertyName;
    }
    
    /**
     * The target object whose property will be animated by this animation
     *
     * @return The object being animated
     */
    public Object getTarget() {
        return mTarget;
    }
    
    /**
     * This function is called immediately before processing the first animation
     * frame of an animation. If there is a nonzero <code>startDelay</code>, the
     * function is called after that delay ends.
     * It takes care of the final initialization steps for the
     * animation. This includes setting mEvaluator, if the user has not yet
     * set it up, and the setter/getter methods, if the user did not supply
     * them.
     *
     *  <p>Overriders of this method should call the superclass method to cause
     *  internal mechanisms to be set up correctly.</p>
     */
    @Override
    void initAnimation() {
    
        if (mInitialized)
            return;
        
        if((mProperty == null) && (mTarget instanceof View) && NEEDS_PROXY)
            setProperty(PROXY_PROPERTIES.get(mPropertyName));
        
        // mValueType may change due to setter/getter setup; do this before calling super.init(),
        // which uses mValueType to set up the default type evaluator.
        for (PropertyValuesHolder holder : mValues)
            holder.setupSetterAndGetter(mTarget);
        
        super.initAnimation();
    
    }
    
    /**
     * Constructs and returns an ObjectAnimator that animates between float values. A single
     * value implies that that value is the one being animated to. Two values imply a starting
     * and ending values. More than two values imply a starting value, values to animate through
     * along the way, and an ending value (these values will be distributed evenly across
     * the duration of the animation).
     *
     * @param target        The object whose property is to be animated. This object should
     *                      have a public method on it called <code>setName()</code>, where <code>name</code> is
     *                      the value of the <code>propertyName</code> parameter.
     * @param propertyName  The name of the property being animated.
     * @param values        A set of values that the animation will animate between over time.
     *
     * @return An ObjectAnimator object that is set up to animate between the given values.
     */
    public static ObjectAnimator ofFloat(Object target, String propertyName, float... values) {
    
        ObjectAnimator anim = new ObjectAnimator(target, propertyName);
        anim.setFloatValues(values);
        
        return anim;
    
    }

    /**
     * Constructs and returns an ObjectAnimator that animates between float values. A single
     * value implies that that value is the one being animated to. Two values imply a starting
     * and ending values. More than two values imply a starting value, values to animate through
     * along the way, and an ending value (these values will be distributed evenly across
     * the duration of the animation).
     *
     * @param target    The object whose property is to be animated.
     * @param property  The property being animated.
     * @param values    A set of values that the animation will animate between over time.
     *
     * @return An ObjectAnimator object that is set up to animate between the given values.
     */
    public static <T> ObjectAnimator ofFloat(T target, Property<T, Float> property, float... values) {
    
        ObjectAnimator anim = new ObjectAnimator(target, property);
        anim.setFloatValues(values);
        
        return anim;
    
    }
    
    /**
     * Constructs and returns an ObjectAnimator that animates between int values. A single
     * value implies that that value is the one being animated to. Two values imply a starting
     * and ending values. More than two values imply a starting value, values to animate through
     * along the way, and an ending value (these values will be distributed evenly across
     * the duration of the animation).
     *
     * @param target        The object whose property is to be animated. This object should
     *                      have a public method on it called <code>setName()</code>, where <code>name</code> is
     *                      the value of the <code>propertyName</code> parameter.
     * @param propertyName  The name of the property being animated.
     * @param values        A set of values that the animation will animate between over time.
     *
     * @return An ObjectAnimator object that is set up to animate between the given values.
     */
    public static ObjectAnimator ofInt(Object target, String propertyName, int... values) {
    
        ObjectAnimator anim = new ObjectAnimator(target, propertyName);
        anim.setIntValues(values);
        
        return anim;
    
    }
    
    /**
     * Constructs and returns an ObjectAnimator that animates between int values. A single
     * value implies that that value is the one being animated to. Two values imply a starting
     * and ending values. More than two values imply a starting value, values to animate through
     * along the way, and an ending value (these values will be distributed evenly across
     * the duration of the animation).
     *
     * @param target    The object whose property is to be animated.
     * @param property  The property being animated.
     * @param values    A set of values that the animation will animate between over time.
     *
     * @return An ObjectAnimator object that is set up to animate between the given values.
     */
    public static <T> ObjectAnimator ofInt(T target, Property<T, Integer> property, int... values) {
    
        ObjectAnimator anim = new ObjectAnimator(target, property);
        anim.setIntValues(values);
        
        return anim;
    
    }
    
    /**
     * Constructs and returns an ObjectAnimator that animates between Object values. A single
     * value implies that that value is the one being animated to. Two values imply a starting
     * and ending values. More than two values imply a starting value, values to animate through
     * along the way, and an ending value (these values will be distributed evenly across
     * the duration of the animation).
     *
     * @param target        The object whose property is to be animated. This object should
     *                      have a public method on it called <code>setName()</code>, where <code>name</code> is
     *                      the value of the <code>propertyName</code> parameter.
     * @param propertyName  The name of the property being animated.
     * @param evaluator     A TypeEvaluator that will be called on each animation frame to
     *                      provide the necessary interpolation between the Object values to derive the animated value.
     * @param values        A set of values that the animation will animate between over time.
     *
     * @return An ObjectAnimator object that is set up to animate between the given values.
     */
    public static ObjectAnimator ofObject(Object target, String propertyName, TypeEvaluator evaluator, Object... values) {
    
        ObjectAnimator anim = new ObjectAnimator(target, propertyName);
        anim.setEvaluator(evaluator);
        anim.setObjectValues(values);
        
        return anim;
    
    }
    
    /**
     * Constructs and returns an ObjectAnimator that animates between Object values. A single
     * value implies that that value is the one being animated to. Two values imply a starting
     * and ending values. More than two values imply a starting value, values to animate through
     * along the way, and an ending value (these values will be distributed evenly across
     * the duration of the animation).
     *
     * @param target    The object whose property is to be animated.
     * @param property  The property being animated.
     * @param evaluator A TypeEvaluator that will be called on each animation frame to
     *                  provide the necessary interpolation between the Object values to derive the animated value.
     * @param values    A set of values that the animation will animate between over time.
     *
     * @return An ObjectAnimator object that is set up to animate between the given values.
     */
    @SafeVarargs
    public static <T, V> ObjectAnimator ofObject(T target, Property<T, V> property, TypeEvaluator<V> evaluator, V... values) {
    
        ObjectAnimator anim = new ObjectAnimator(target, property);
        anim.setEvaluator(evaluator);
        anim.setObjectValues((Object[]) values);
        
        return anim;
    
    }
    
    /**
     * Constructs and returns an ObjectAnimator that animates between the sets of values specified
     * in <code>PropertyValueHolder</code> objects. This variant should be used when animating
     * several properties at once with the same ObjectAnimator, since PropertyValuesHolder allows
     * you to associate a set of animation values with a property name.
     *
     * @param target    The object whose property is to be animated. Depending on how the
     *                  PropertyValuesObjects were constructed, the target object should either have the {@link
     *                  robertapengelly.support.util.Property} objects used to construct the PropertyValuesHolder objects or (if the
     *                  PropertyValuesHOlder objects were created with property names) the target object should have
     *                  public methods on it called <code>setName()</code>, where <code>name</code> is the name of
     *                  the property passed in as the <code>propertyName</code> parameter for each of the
     *                  PropertyValuesHolder objects.
     * @param values    A set of PropertyValuesHolder objects whose values will be animated between over time.
     *
     * @return An ObjectAnimator object that is set up to animate between the given values.
     */
    public static ObjectAnimator ofPropertyValuesHolder(Object target, PropertyValuesHolder... values) {
    
        ObjectAnimator anim = new ObjectAnimator();
        anim.mTarget = target;
        anim.setValues(values);
        
        return anim;
    
    }
    
    /**
     * Sets the length of the animation. The default duration is 300 milliseconds.
     *
     * @param duration The length of the animation, in milliseconds.
     *
     * @return ObjectAnimator   The object called with setDuration(). This return
     *                          value makes it easier to compose statements together that construct and then set the
     *                          duration, as in
     * <code>ObjectAnimator.ofInt(target, propertyName, 0, 10).setDuration(500).start()</code>.
     */
    @Override
    public ObjectAnimator setDuration(long duration) {
        super.setDuration(duration);
        return this;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void setFloatValues(float... values) {
    
        if ((mValues == null) || (mValues.length == 0)) {
        
            // No values yet - this animator is being constructed piecemeal. Init the values with
            // whatever the current propertyName is
            if (mProperty != null)
                setValues(PropertyValuesHolder.ofFloat(mProperty, values));
            else
                setValues(PropertyValuesHolder.ofFloat(mPropertyName, values));
        
        } else
            super.setFloatValues(values);
    
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void setIntValues(int... values) {
    
        if ((mValues == null) || (mValues.length == 0)) {
        
            // No values yet - this animator is being constructed piecemeal. Init the values with
            // whatever the current propertyName is
            if (mProperty != null)
                setValues(PropertyValuesHolder.ofInt(mProperty, values));
            else
                setValues(PropertyValuesHolder.ofInt(mPropertyName, values));
        
        } else
            super.setIntValues(values);
    
    }
    
    @Override
    public void setObjectValues(Object... values) {
    
        if ((mValues == null) || (mValues.length == 0)) {
        
            // No values yet - this animator is being constructed piecemeal. Init the values with
            // whatever the current propertyName is
            if (mProperty != null)
                setValues(PropertyValuesHolder.ofObject(mProperty, null, values));
            else
                setValues(PropertyValuesHolder.ofObject(mPropertyName, null, values));
        
        } else
            super.setObjectValues(values);
    
    }
    
    /**
     * Sets the property that will be animated. Property objects will take precedence over
     * properties specified by the {@link #setPropertyName(String)} method. Animations should
     * be set up to use one or the other, not both.
     *
     * @param property The property being animated. Should not be null.
     */
    public void setProperty(Property property) {
    
        // mValues could be null if this is being constructed piecemeal. Just record the
        // propertyName to be used later when setValues() is called if so.
        if (mValues != null) {
        
            PropertyValuesHolder valuesHolder = mValues[0];
            String oldName = valuesHolder.getPropertyName();
            
            valuesHolder.setProperty(property);
            
            mValuesMap.remove(oldName);
            mValuesMap.put(mPropertyName, valuesHolder);
        
        }
        
        if (mProperty != null)
            mPropertyName = property.getName();
        
        mProperty = property;
        
        // New property/values/target should cause re-initialization prior to starting
        mInitialized = false;
    
    }
    
    /**
     * Sets the name of the property that will be animated. This name is used to derive
     * a setter function that will be called to set animated values.
     * For example, a property name of <code>foo</code> will result
     * in a call to the function <code>setFoo()</code> on the target object. If either
     * <code>valueFrom</code> or <code>valueTo</code> is null, then a getter function will
     * also be derived and called.
     *
     * <p>For best performance of the mechanism that calls the setter function determined by the
     * name of the property being animated, use <code>float</code> or <code>int</code> typed values,
     * and make the setter function for those properties have a <code>void</code> return value. This
     * will cause the code to take an optimized path for these constrained circumstances. Other
     * property types and return types will work, but will have more overhead in processing
     * the requests due to normal reflection mechanisms.</p>
     *
     * <p>Note that the setter function derived from this property name
     * must take the same parameter type as the
     * <code>valueFrom</code> and <code>valueTo</code> properties, otherwise the call to
     * the setter function will fail.</p>
     *
     * <p>If this ObjectAnimator has been set up to animate several properties together,
     * using more than one PropertyValuesHolder objects, then setting the propertyName simply
     * sets the propertyName in the first of those PropertyValuesHolder objects.</p>
     *
     * @param propertyName The name of the property being animated. Should not be null.
     */
    public void setPropertyName(String propertyName) {
    
        // mValues could be null if this is being constructed piecemeal. Just record the
        // propertyName to be used later when setValues() is called if so.
        if (mValues != null) {
        
            PropertyValuesHolder valuesHolder = mValues[0];
            String oldName = valuesHolder.getPropertyName();
            
            valuesHolder.setPropertyName(propertyName);
            
            mValuesMap.remove(oldName);
            mValuesMap.put(propertyName, valuesHolder);
        
        }
        
        mPropertyName = propertyName;
        
        // New property/values/target should cause re-initialization prior to starting
        mInitialized = false;
    
    }
    
    /**
     * Sets the target object whose property will be animated by this animation
     *
     * @param target The object being animated
     */
    @Override
    public void setTarget(Object target) {
    
        if(mTarget == target)
            return;
        
        final Object oldTarget = mTarget;
        mTarget = target;
        
        if ((oldTarget != null) && (target != null) && (oldTarget.getClass() == target.getClass()))
            return;
        
        // New target type should cause re-initialization prior to starting
        mInitialized = false;
    
    }
    
    @Override
    public void setupEndValues() {
    
        initAnimation();
        
        for (PropertyValuesHolder holder : mValues)
            holder.setupEndValue(mTarget);
    
    }
    
    @Override
    public void setupStartValues() {
    
        initAnimation();
        
        for (PropertyValuesHolder holder : mValues)
            holder.setupStartValue(mTarget);
    
    }
    
    @Override
    public String toString() {
    
        String returnVal = "ObjectAnimator@" + Integer.toHexString(hashCode()) + ", target " + mTarget;
        
        if (mValues != null)
            for (PropertyValuesHolder holder : mValues)
                returnVal += "\n    " + holder.toString();
        
        return returnVal;
    
    }

}