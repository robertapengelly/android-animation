package robertapengelly.support.animation;

import  android.util.Log;

import  java.lang.reflect.InvocationTargetException;
import  java.lang.reflect.Method;
import  java.util.HashMap;
import  java.util.concurrent.locks.ReentrantReadWriteLock;

import  robertapengelly.support.util.FloatProperty;
import  robertapengelly.support.util.IntProperty;
import  robertapengelly.support.util.Property;

/**
 * This class holds information about a property and the values that that property
 * should take on during an animation. PropertyValuesHolder objects can be used to create
 * animations with ValueAnimator or ObjectAnimator that operate on several different properties
 * in parallel.
 */
public class PropertyValuesHolder implements Cloneable {

    // These maps hold all property entries for a particular class. This map
    // is used to speed up property/setter/getter lookups for a given class/property
    // combination. No need to use reflection on the combination more than once.
    private static final HashMap<Class, HashMap<String, Method>> sGetterPropertyMap = new HashMap<>();
    private static final HashMap<Class, HashMap<String, Method>> sSetterPropertyMap = new HashMap<>();
    
    // type evaluators for the primitive types handled by this implementation
    private static final TypeEvaluator sFloatEvaluator = new FloatEvaluator();
    private static final TypeEvaluator sIntEvaluator = new IntEvaluator();
    
    // This lock is used to ensure that only one thread is accessing the property maps at a time.
    final ReentrantReadWriteLock mPropertyMapLock = new ReentrantReadWriteLock();
    
    // Used to pass single value to varargs parameter in setter invocation
    final Object[] mTmpValueArray = new Object[1];
    
    // We try several different types when searching for appropriate setter/getter functions.
    // The caller may have supplied values in a type that does not match the setter/getter
    // functions (such as the integers 0 and 1 to represent floating point values for alpha).
    // Also, the use of generics in constructors means that we end up with the Object versions
    // of primitive types (Float vs. float). But most likely, the setter/getter functions will take primitive types
    // instead. So we supply an ordered array of other types to try before giving up.
    private static Class[] DOUBLE_VARIANTS = {double.class, Double.class, float.class,
        int.class, Float.class, Integer.class};
    private static Class[] FLOAT_VARIANTS = {float.class, Float.class, double.class,
        int.class, Double.class, Integer.class};
    private static Class[] INTEGER_VARIANTS = {int.class, Integer.class, float.class,
        double.class, Float.class, Double.class};
    
    /**
     * The value most recently calculated by calculateValue(). This is set during
     * that function and might be retrieved later either by ValueAnimator.animatedValue() or
     * by the property-setting logic in ObjectAnimator.animatedValue().
     */
    private Object mAnimatedValue;
    
    /**
     * The type evaluator used to calculate the animated values. This evaluator is determined
     * automatically based on the type of the start/end objects passed into the constructor,
     * but the system only knows about the primitive types int and float. Any other
     * type will need to set the evaluator to a custom evaluator for that type.
     */
    private TypeEvaluator mEvaluator;
    
    /**
     * The getter function, if needed. ObjectAnimator hands off this functionality to
     * PropertyValuesHolder, since it holds all of the per-property information. This
     * property is automatically
     * derived when the animation starts in setupSetterAndGetter() if using ObjectAnimator.
     * The getter is only derived and used if one of the values is null.
     */
    private Method mGetter = null;
    
    /** @hide */
    protected Property mProperty;
    
    /** The set of keyframes (time/value pairs) that define this animation. */
    KeyframeSet mKeyframeSet = null;
    
    /**
     * The name of the property associated with the values. This need not be a real property,
     * unless this object is being used with ObjectAnimator. But this is the name by which
     * aniamted values are looked up with getAnimatedValue(String) in ValueAnimator.
     */
    String mPropertyName;
    
    /**
     * The setter function, if needed. ObjectAnimator hands off this functionality to
     * PropertyValuesHolder, since it holds all of the per-property information. This
     * property is automatically
     * derived when the animation starts in setupSetterAndGetter() if using ObjectAnimator.
     */
    Method mSetter = null;
    
    /**
     * The type of values supplied. This information is used both in deriving the setter/getter
     * functions and in deriving the type of TypeEvaluator.
     */
    Class mValueType;
    
    /**
     * Internal utility constructor, used by the factory methods to set the property.
     *
     * @param property The property for this holder.
     */
    private PropertyValuesHolder(Property property) {
    
        mProperty = property;
        
        if (property != null)
            mPropertyName = property.getName();
    
    }
    
    /**
     * Internal utility constructor, used by the factory methods to set the property name.
     *
     * @param propertyName The name of the property for this holder.
     */
    private PropertyValuesHolder(String propertyName) {
        mPropertyName = propertyName;
    }
    
    /**
     * Function used to calculate the value according to the evaluator set up for
     * this PropertyValuesHolder object. This function is called by ValueAnimator.animateValue().
     *
     * @param fraction The elapsed, interpolated fraction of the animation.
     */
    void calculateValue(float fraction) {
        mAnimatedValue = mKeyframeSet.getValue(fraction);
    }
    
    @Override
    public PropertyValuesHolder clone() {
    
        try {
        
            PropertyValuesHolder newPVH = (PropertyValuesHolder) super.clone();
            newPVH.mEvaluator = mEvaluator;
            newPVH.mKeyframeSet = mKeyframeSet.clone();
            newPVH.mProperty = mProperty;
            newPVH.mPropertyName = mPropertyName;
            
            return newPVH;
        
        } catch (CloneNotSupportedException e) {
        
            // won't reach here
            return null;
        
        }
    
    }
    
    /**
     * Internal function, called by ValueAnimator and ObjectAnimator, to retrieve the value
     * most recently calculated in calculateValue().
     */
    Object getAnimatedValue() {
        return mAnimatedValue;
    }
    
    /**
     * Utility method to derive a setter/getter method name from a property name, where the
     * prefix is typically "set" or "get" and the first letter of the property name is
     * capitalized.
     *
     * @param prefix       The precursor to the method name, before the property name begins, typically
     *                     "set" or "get".
     * @param propertyName The name of the property that represents the bulk of the method name
     *                     after the prefix. The first letter of this word will be capitalized in the resulting
     *                     method name.
     *
     * @return String the property name converted to a method name according to the conventions
     * specified above.
     */
    static String getMethodName(String prefix, String propertyName) {
    
        if ((propertyName == null) || (propertyName.length() == 0)) {
        
            // shouldn't get here
            return prefix;
        
        }
        
        char firstLetter = Character.toUpperCase(propertyName.charAt(0));
        return prefix + firstLetter + propertyName.substring(1);
    
    }
    
    /**
     * Determine the setter or getter function using the JavaBeans convention of setFoo or
     * getFoo for a property named 'foo'. This function figures out what the name of the
     * function should be and uses reflection to find the Method with that name on the
     * target object.
     *
     * @param targetClass The class to search for the method
     * @param prefix      "set" or "get", depending on whether we need a setter or getter.
     * @param valueType   The type of the parameter (in the case of a setter). This type
     *                    is derived from the values set on this PropertyValuesHolder. This type is used as
     *                    a first guess at the parameter type, but we check for methods with several different
     *                    types to avoid problems with slight mis-matches between supplied values and actual
     *                    value types used on the setter.
     *
     * @return Method the method associated with mPropertyName.
     */
    @SuppressWarnings("unchecked")
    private Method getPropertyFunction(Class targetClass, String prefix, Class valueType) {
    
        // TODO: faster implementation...
        Class args[] = null;
        Method returnVal = null;
        String methodName = getMethodName(prefix, mPropertyName);
        
        if (valueType == null) {
        
            try {
                returnVal = targetClass.getMethod(methodName, (Class[]) null);
            } catch (NoSuchMethodException e) {
            
                /* The native implementation uses JNI to do reflection, which allows access to private methods.
                 * getDeclaredMethod(..) does not find superclass methods, so it's implemented as a fallback.
                 */
                try {
                
                    returnVal = targetClass.getDeclaredMethod(methodName, (Class[]) null);
                    returnVal.setAccessible(true);
                
                } catch (NoSuchMethodException e2) {
                    Log.e("PropertyValuesHolder", "Couldn't find no-arg method for property " + mPropertyName + ": " + e2);
                }
            
            }
        
        } else {
        
            args = new Class[1];
            Class typeVariants[];
            
            if (mValueType.equals(Double.class))
                typeVariants = DOUBLE_VARIANTS;
            else if (mValueType.equals(Float.class))
                typeVariants = FLOAT_VARIANTS;
            else if (mValueType.equals(Integer.class))
                typeVariants = INTEGER_VARIANTS;
            else {
            
                typeVariants = new Class[1];
                typeVariants[0] = mValueType;
            
            }
            
            for (Class typeVariant : typeVariants) {
            
                args[0] = typeVariant;
                
                try {
                
                    returnVal = targetClass.getMethod(methodName, args);
                    
                    // change the value type to suit
                    mValueType = typeVariant;
                    
                    return returnVal;
                
                } catch (NoSuchMethodException e) {
                
                    /* The native implementation uses JNI to do reflection, which allows access to private methods.
                     * getDeclaredMethod(..) does not find superclass methods, so it's implemented as a fallback.
                     */
                    try {
                    
                        returnVal = targetClass.getDeclaredMethod(methodName, args);
                        returnVal.setAccessible(true);
                        
                        // change the value type to suit
                        mValueType = typeVariant;
                        
                        return returnVal;
                    
                    } catch (NoSuchMethodException e2) {
                        // Swallow the error and keep trying other variants
                    }
                
                }
            
            }
            
            // If we got here, then no appropriate function was found
            Log.e("PropertyValuesHolder", "Couldn't find setter/getter for property " + mPropertyName +
                    " with value type " + mValueType);
        
        }
        
        return returnVal;
    
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
     * Internal function, called by ValueAnimator, to set up the TypeEvaluator that will be used
     * to calculate animated values.
     */
    void init() {
    
        if (mEvaluator == null) {
        
            // We already handle int and float automatically, but not their Object equivalents
            mEvaluator = ((mValueType == Integer.class) ? sIntEvaluator :
                    (mValueType == Float.class) ? sFloatEvaluator : null);
            
            // KeyframeSet knows how to evaluate the common types - only give it a custom
            // evaluator if one has been set on this class
            mKeyframeSet.setEvaluator(mEvaluator);
        
        }
    
    }
    
    /**
     * Constructs and returns a PropertyValuesHolder with a given property and set of float values.
     *
     * @param property The property being animated. Should not be null.
     * @param values   The values that the property will animate between.
     *
     * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
     */
    public static PropertyValuesHolder ofFloat(Property<?, Float> property, float... values) {
        return new FloatPropertyValuesHolder(property, values);
    }
    
    /**
     * Constructs and returns a PropertyValuesHolder with a given property name and set of float values.
     *
     * @param propertyName The name of the property being animated.
     * @param values       The values that the named property will animate between.
     *
     * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
     */
    public static PropertyValuesHolder ofFloat(String propertyName, float... values) {
        return new FloatPropertyValuesHolder(propertyName, values);
    }
    
    /**
     * Constructs and returns a PropertyValuesHolder with a given property and set of int values.
     *
     * @param property The property being animated. Should not be null.
     * @param values   The values that the property will animate between.
     *
     * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
     */
    public static PropertyValuesHolder ofInt(Property<?, Integer> property, int... values) {
        return new IntPropertyValuesHolder(property, values);
    }
    
    /**
     * Constructs and returns a PropertyValuesHolder with a given property name and set of int values.
     *
     * @param propertyName The name of the property being animated.
     * @param values       The values that the named property will animate between.
     * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
     */
    public static PropertyValuesHolder ofInt(String propertyName, int... values) {
        return new IntPropertyValuesHolder(propertyName, values);
    }
    
    /**
     * Constructs and returns a PropertyValuesHolder object with the specified property and set
     * of values. These values can be of any type, but the type should be consistent so that
     * an appropriate {@link robertapengelly.support.animation.TypeEvaluator} can be found that matches
     * the common type.
     * <p>If there is only one value, it is assumed to be the end value of an animation,
     * and an initial value will be derived, if possible, by calling the property's
     * {@link robertapengelly.support.util.Property#get(Object)} function.
     * Also, if any value is null, the value will be filled in when the animation
     * starts in the same way. This mechanism of automatically getting null values only works
     * if the PropertyValuesHolder object is used in conjunction with
     * {@link ObjectAnimator}, since otherwise PropertyValuesHolder has
     * no way of determining what the value should be.
     *
     * @param property The property associated with this set of values. Should not be null.
     * @param values   The set of values to animate between.
     */
    public static PropertyValuesHolder ofKeyframe(Property property, Keyframe... values) {
    
        KeyframeSet keyframeSet = KeyframeSet.ofKeyframe(values);
        
        if (keyframeSet instanceof IntKeyframeSet)
            return new IntPropertyValuesHolder(property, (IntKeyframeSet) keyframeSet);
        else if (keyframeSet instanceof FloatKeyframeSet)
            return new FloatPropertyValuesHolder(property, (FloatKeyframeSet) keyframeSet);
        else {
        
            PropertyValuesHolder pvh = new PropertyValuesHolder(property);
            pvh.mKeyframeSet = keyframeSet;
            pvh.mValueType = ((Keyframe) values[0]).getType();
            
            return pvh;
        
        }
    
    }
    
    /**
     * Constructs and returns a PropertyValuesHolder object with the specified property name and set
     * of values. These values can be of any type, but the type should be consistent so that
     * an appropriate {@link robertapengelly.support.animation.TypeEvaluator} can be found that matches
     * the common type.
     * <p>If there is only one value, it is assumed to be the end value of an animation,
     * and an initial value will be derived, if possible, by calling a getter function
     * on the object. Also, if any value is null, the value will be filled in when the animation
     * starts in the same way. This mechanism of automatically getting null values only works
     * if the PropertyValuesHolder object is used in conjunction
     * {@link ObjectAnimator}, and with a getter function
     * derived automatically from <code>propertyName</code>, since otherwise PropertyValuesHolder has
     * no way of determining what the value should be.
     *
     * @param propertyName The name of the property associated with this set of values. This
     *                     can be the actual property name to be used when using a ObjectAnimator object, or
     *                     just a name used to get animated values, such as if this object is used with an
     *                     ValueAnimator object.
     * @param values       The set of values to animate between.
     */
    public static PropertyValuesHolder ofKeyframe(String propertyName, Keyframe... values) {
    
        KeyframeSet keyframeSet = KeyframeSet.ofKeyframe(values);
        
        if (keyframeSet instanceof IntKeyframeSet)
            return new IntPropertyValuesHolder(propertyName, (IntKeyframeSet) keyframeSet);
        else if (keyframeSet instanceof FloatKeyframeSet)
            return new FloatPropertyValuesHolder(propertyName, (FloatKeyframeSet) keyframeSet);
        else {
        
            PropertyValuesHolder pvh = new PropertyValuesHolder(propertyName);
            pvh.mKeyframeSet = keyframeSet;
            pvh.mValueType = ((Keyframe) values[0]).getType();
            
            return pvh;
        
        }
    
    }
    
    /**
     * Constructs and returns a PropertyValuesHolder with a given property and
     * set of Object values. This variant also takes a TypeEvaluator because the system
     * cannot automatically interpolate between objects of unknown type.
     *
     * @param property  The property being animated. Should not be null.
     * @param evaluator A TypeEvaluator that will be called on each animation frame to
     *                  provide the necessary interpolation between the Object values to derive the animated
     *                  value.
     * @param values    The values that the property will animate between.
     *
     * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
     */
    public static <V> PropertyValuesHolder ofObject(Property property, TypeEvaluator<V> evaluator, V... values) {
    
        PropertyValuesHolder pvh = new PropertyValuesHolder(property);
        pvh.setEvaluator(evaluator);
        pvh.setObjectValues(values);
        
        return pvh;
    
    }
    
    /**
     * Constructs and returns a PropertyValuesHolder with a given property name and
     * set of Object values. This variant also takes a TypeEvaluator because the system
     * cannot automatically interpolate between objects of unknown type.
     *
     * @param propertyName The name of the property being animated.
     * @param evaluator    A TypeEvaluator that will be called on each animation frame to
     *                     provide the necessary interpolation between the Object values to derive the animated
     *                     value.
     * @param values       The values that the named property will animate between.
     *
     * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
     */
    public static PropertyValuesHolder ofObject(String propertyName, TypeEvaluator evaluator, Object... values) {
    
        PropertyValuesHolder pvh = new PropertyValuesHolder(propertyName);
        pvh.setEvaluator(evaluator);
        pvh.setObjectValues(values);
        
        return pvh;
    
    }
    
    /**
     * Internal function to set the value on the target object, using the setter set up
     * earlier on this PropertyValuesHolder object. This function is called by ObjectAnimator
     * to handle turning the value calculated by ValueAnimator into a value set on the object
     * according to the name of the property.
     *
     * @param target The target object on which the value is set
     */
    @SuppressWarnings({"TryWithIdenticalCatches", "unchecked"})
    void setAnimatedValue(Object target) {
    
        if (mProperty != null)
            mProperty.set(target, getAnimatedValue());
        
        if (mSetter != null) {
        
            try {
            
                mTmpValueArray[0] = getAnimatedValue();
                mSetter.invoke(target, mTmpValueArray);
            
            } catch (InvocationTargetException e) {
                Log.e("PropertyValuesHolder", e.toString());
            } catch (IllegalAccessException e) {
                Log.e("PropertyValuesHolder", e.toString());
            }
        
        }
    
    }
    
    /**
     * The TypeEvaluator will the automatically determined based on the type of values
     * supplied to PropertyValuesHolder. The evaluator can be manually set, however, if so
     * desired. This may be important in cases where either the type of the values supplied
     * do not match the way that they should be interpolated between, or if the values
     * are of a custom type or one not currently understood by the animation system. Currently,
     * only values of type float and int (and their Object equivalents: Float
     * and Integer) are  correctly interpolated; all other types require setting a TypeEvaluator.
     *
     * @param evaluator
     */
    public void setEvaluator(TypeEvaluator evaluator) {
    
        mEvaluator = evaluator;
        mKeyframeSet.setEvaluator(evaluator);
    
    }
    
    /**
     * Set the animated values for this object to this set of floats.
     * If there is only one value, it is assumed to be the end value of an animation,
     * and an initial value will be derived, if possible, by calling a getter function
     * on the object. Also, if any value is null, the value will be filled in when the animation
     * starts in the same way. This mechanism of automatically getting null values only works
     * if the PropertyValuesHolder object is used in conjunction
     * {@link ObjectAnimator}, and with a getter function
     * derived automatically from <code>propertyName</code>, since otherwise PropertyValuesHolder has
     * no way of determining what the value should be.
     *
     * @param values One or more values that the animation will animate between.
     */
    public void setFloatValues(float... values) {
    
        mKeyframeSet = KeyframeSet.ofFloat(values);
        mValueType = float.class;
    
    }
    
    /**
     * Set the animated values for this object to this set of ints.
     * If there is only one value, it is assumed to be the end value of an animation,
     * and an initial value will be derived, if possible, by calling a getter function
     * on the object. Also, if any value is null, the value will be filled in when the animation
     * starts in the same way. This mechanism of automatically getting null values only works
     * if the PropertyValuesHolder object is used in conjunction
     * {@link ObjectAnimator}, and with a getter function
     * derived automatically from <code>propertyName</code>, since otherwise PropertyValuesHolder has
     * no way of determining what the value should be.
     *
     * @param values One or more values that the animation will animate between.
     */
    public void setIntValues(int... values) {
    
        mKeyframeSet = KeyframeSet.ofInt(values);
        mValueType = int.class;
    
    }
    
    /**
     * Set the animated values for this object to this set of Keyframes.
     *
     * @param values One or more values that the animation will animate between.
     */
    public void setKeyframes(Keyframe... values) {
    
        int numKeyframes = values.length;
        
        Keyframe keyframes[] = new Keyframe[Math.max(numKeyframes, 2)];
        mValueType = values[0].getType();
        
        for (int i = 0; i < numKeyframes; ++i)
            keyframes[i] = values[i];
        
        mKeyframeSet = new KeyframeSet(keyframes);
    
    }
    
    /**
     * Set the animated values for this object to this set of Objects.
     * If there is only one value, it is assumed to be the end value of an animation,
     * and an initial value will be derived, if possible, by calling a getter function
     * on the object. Also, if any value is null, the value will be filled in when the animation
     * starts in the same way. This mechanism of automatically getting null values only works
     * if the PropertyValuesHolder object is used in conjunction
     * {@link ObjectAnimator}, and with a getter function
     * derived automatically from <code>propertyName</code>, since otherwise PropertyValuesHolder has
     * no way of determining what the value should be.
     *
     * @param values One or more values that the animation will animate between.
     */
    public void setObjectValues(Object... values) {
    
        mKeyframeSet = KeyframeSet.ofObject(values);
        mValueType = values[0].getClass();
    
    }
    
    /**
     * Sets the property that will be animated.
     *
     * <p>Note that if this PropertyValuesHolder object is used with ObjectAnimator, the property
     * must exist on the target object specified in that ObjectAnimator.</p>
     *
     * @param property The property being animated.
     */
    public void setProperty(Property property) {
        mProperty = property;
    }
    
    /**
     * Sets the name of the property that will be animated. This name is used to derive
     * a setter function that will be called to set animated values.
     * For example, a property name of <code>foo</code> will result
     * in a call to the function <code>setFoo()</code> on the target object. If either
     * <code>valueFrom</code> or <code>valueTo</code> is null, then a getter function will
     * also be derived and called.
     *
     * <p>Note that the setter function derived from this property name
     * must take the same parameter type as the
     * <code>valueFrom</code> and <code>valueTo</code> properties, otherwise the call to
     * the setter function will fail.</p>
     *
     * @param propertyName The name of the property being animated.
     */
    public void setPropertyName(String propertyName) {
        mPropertyName = propertyName;
    }
    
    /**
     * This function is called by ObjectAnimator when setting the end values for an animation.
     * The end values are set according to the current values in the target object. The
     * property whose value is extracted is whatever is specified by the propertyName of this
     * PropertyValuesHolder object.
     *
     * @param target The object which holds the start values that should be set.
     */
    void setupEndValue(Object target) {
        setupValue(target, mKeyframeSet.mKeyframes.get(mKeyframeSet.mKeyframes.size() - 1));
    }
    
    /** Utility function to get the getter from targetClass */
    private void setupGetter(Class targetClass) {
        mGetter = setupSetterOrGetter(targetClass, sGetterPropertyMap, "get", null);
    }
    
    /**
     * Utility function to get the setter from targetClass
     *
     * @param targetClass The Class on which the requested method should exist.
     */
    void setupSetter(Class targetClass) {
        mSetter = setupSetterOrGetter(targetClass, sSetterPropertyMap, "set", mValueType);
    }
    
    /**
     * Internal function (called from ObjectAnimator) to set up the setter and getter
     * prior to running the animation. If the setter has not been manually set for this
     * object, it will be derived automatically given the property name, target object, and
     * types of values supplied. If no getter has been set, it will be supplied iff any of the
     * supplied values was null. If there is a null value, then the getter (supplied or derived)
     * will be called to set those null values to the current value of the property
     * on the target object.
     *
     * @param target The object on which the setter (and possibly getter) exist.
     */
    @SuppressWarnings({"TryWithIdenticalCatches", "unchecked"})
    void setupSetterAndGetter(Object target) {
    
        if (mProperty != null) {
        
            // check to make sure that mProperty is on the class of target
            try {
            
                for (Keyframe kf : mKeyframeSet.mKeyframes)
                    if (!kf.hasValue())
                        kf.setValue(mProperty.get(target));
                
                return;
            
            } catch (ClassCastException e) {
            
                Log.e("PropertyValuesHolder", "No such property (" + mProperty.getName() + ") on target object " + target
                        + ". Trying reflection instead");
                
                mProperty = null;
            
            }
        
        }
        
        Class targetClass = target.getClass();
        
        if (mSetter == null)
            setupSetter(targetClass);
        
        for (Keyframe kf : mKeyframeSet.mKeyframes) {
        
            if (!kf.hasValue()) {
            
                if (mGetter == null)
                    setupGetter(targetClass);
                
                try {
                    kf.setValue(mGetter.invoke(target));
                } catch (InvocationTargetException e) {
                    Log.e("PropertyValuesHolder", e.toString());
                } catch (IllegalAccessException e) {
                    Log.e("PropertyValuesHolder", e.toString());
                }
            
            }
        
        }
    
    }
    
    /**
     * Returns the setter or getter requested. This utility function checks whether the
     * requested method exists in the propertyMapMap cache. If not, it calls another
     * utility function to request the Method from the targetClass directly.
     *
     * @param targetClass    The Class on which the requested method should exist.
     * @param propertyMapMap The cache of setters/getters derived so far.
     * @param prefix         "set" or "get", for the setter or getter.
     * @param valueType      The type of parameter passed into the method (null for getter).
     *
     * @return Method the method associated with mPropertyName.
     */
    private Method setupSetterOrGetter(Class targetClass, HashMap<Class, HashMap<String, Method>> propertyMapMap,
                                        String prefix, Class valueType) {
    
        Method setterOrGetter = null;
        
        try {
        
            // Have to lock property map prior to reading it, to guard against
            // another thread putting something in there after we've checked it
            // but before we've added an entry to it
            mPropertyMapLock.writeLock().lock();
            HashMap<String, Method> propertyMap = propertyMapMap.get(targetClass);
            
            if (propertyMap != null)
                setterOrGetter = propertyMap.get(mPropertyName);
            
            if (setterOrGetter == null) {
            
                setterOrGetter = getPropertyFunction(targetClass, prefix, valueType);
                
                if (propertyMap == null) {
                
                    propertyMap = new HashMap<>();
                    propertyMapMap.put(targetClass, propertyMap);
                
                }
                
                propertyMap.put(mPropertyName, setterOrGetter);
            
            }
        
        } finally {
            mPropertyMapLock.writeLock().unlock();
        }
        
        return setterOrGetter;
    
    }
    
    /**
     * This function is called by ObjectAnimator when setting the start values for an animation.
     * The start values are set according to the current values in the target object. The
     * property whose value is extracted is whatever is specified by the propertyName of this
     * PropertyValuesHolder object.
     *
     * @param target The object which holds the start values that should be set.
     */
    void setupStartValue(Object target) {
        setupValue(target, mKeyframeSet.mKeyframes.get(0));
    }
    
    /**
     * Utility function to set the value stored in a particular Keyframe. The value used is
     * whatever the value is for the property name specified in the keyframe on the target object.
     *
     * @param target The target object from which the current value should be extracted.
     * @param kf     The keyframe which holds the property name and value.
     */
    @SuppressWarnings({"TryWithIdenticalCatches", "unchecked"})
    private void setupValue(Object target, Keyframe kf) {
    
        if (mProperty != null)
            kf.setValue(mProperty.get(target));
        
        try {
        
            if (mGetter == null) {
            
                Class targetClass = target.getClass();
                setupGetter(targetClass);
            
            }
            
            kf.setValue(mGetter.invoke(target));
        
        } catch (InvocationTargetException e) {
            Log.e("PropertyValuesHolder", e.toString());
        } catch (IllegalAccessException e) {
            Log.e("PropertyValuesHolder", e.toString());
        }
    
    }
    
    @Override
    public String toString() {
        return (mPropertyName + ": " + mKeyframeSet.toString());
    }
    
    static class FloatPropertyValuesHolder extends PropertyValuesHolder {
    
        private FloatProperty mFloatProperty;
        
        float mFloatAnimatedValue;
        FloatKeyframeSet mFloatKeyframeSet;
        
        FloatPropertyValuesHolder(Property property, float... values) {
            super(property);
            
            setFloatValues(values);
            
            if (property instanceof FloatProperty)
                mFloatProperty = (FloatProperty) mProperty;
        
        }
        
        FloatPropertyValuesHolder(Property property, FloatKeyframeSet keyframeSet) {
            super(property);
            
            mFloatKeyframeSet = keyframeSet;
            mKeyframeSet = keyframeSet;
            mValueType = float.class;
            
            if (property instanceof FloatProperty)
                mFloatProperty = (FloatProperty) mProperty;
        
        }
        
        FloatPropertyValuesHolder(String propertyName, float... values) {
            super(propertyName);
            
            setFloatValues(values);
        
        }
        
        FloatPropertyValuesHolder(String propertyName, FloatKeyframeSet keyframeSet) {
            super(propertyName);
            
            mFloatKeyframeSet = keyframeSet;
            mKeyframeSet = keyframeSet;
            mValueType = float.class;
        
        }
        
        @Override
        void calculateValue(float fraction) {
            mFloatAnimatedValue = mFloatKeyframeSet.getFloatValue(fraction);
        }
        
        @Override
        public FloatPropertyValuesHolder clone() {
        
            FloatPropertyValuesHolder newPVH = (FloatPropertyValuesHolder) super.clone();
            newPVH.mFloatKeyframeSet = (FloatKeyframeSet) newPVH.mKeyframeSet;
            
            return newPVH;
        
        }
        
        @Override
        Object getAnimatedValue() {
            return mFloatAnimatedValue;
        }
        
        /**
         * Internal function to set the value on the target object, using the setter set up
         * earlier on this PropertyValuesHolder object. This function is called by ObjectAnimator
         * to handle turning the value calculated by ValueAnimator into a value set on the object
         * according to the name of the property.
         *
         * @param target The target object on which the value is set
         */
        @Override
        @SuppressWarnings({"TryWithIdenticalCatches", "unchecked"})
        void setAnimatedValue(Object target) {
        
            if (mFloatProperty != null) {
            
                mFloatProperty.setValue(target, mFloatAnimatedValue);
                return;
            
            }
            
            if (mProperty != null) {
            
                mProperty.set(target, mFloatAnimatedValue);
                return;
            
            }
            
            if (mSetter != null) {
            
                try {
                
                    mTmpValueArray[0] = mFloatAnimatedValue;
                    mSetter.invoke(target, mTmpValueArray);
                
                } catch (InvocationTargetException e) {
                    Log.e("PropertyValuesHolder", e.toString());
                } catch (IllegalAccessException e) {
                    Log.e("PropertyValuesHolder", e.toString());
                }
            
            }
        
        }
        
        @Override
        public void setFloatValues(float... values) {
            super.setFloatValues(values);
            
            mFloatKeyframeSet = (FloatKeyframeSet) mKeyframeSet;
        
        }
        
        @Override
        void setupSetter(Class targetClass) {
        
            if (mProperty != null)
                return;
            
            super.setupSetter(targetClass);
        
        }
    
    }
    
    static class IntPropertyValuesHolder extends PropertyValuesHolder {
    
        private IntProperty mIntProperty;
        
        int mIntAnimatedValue;
        IntKeyframeSet mIntKeyframeSet;
        
        IntPropertyValuesHolder(Property property, int... values) {
            super(property);
            
            setIntValues(values);
            
            if (property instanceof IntProperty)
                mIntProperty = (IntProperty) mProperty;
        
        }
        
        IntPropertyValuesHolder(Property property, IntKeyframeSet keyframeSet) {
            super(property);
            
            mIntKeyframeSet = keyframeSet;
            mKeyframeSet = keyframeSet;
            mValueType = int.class;
            
            if (property instanceof IntProperty)
                mIntProperty = (IntProperty) mProperty;
        
        }
        
        IntPropertyValuesHolder(String propertyName, int... values) {
            super(propertyName);
            
            setIntValues(values);
        
        }
        
        IntPropertyValuesHolder(String propertyName, IntKeyframeSet keyframeSet) {
            super(propertyName);
            
            mIntKeyframeSet = keyframeSet;
            mKeyframeSet = keyframeSet;
            mValueType = int.class;
        
        }
        
        @Override
        void calculateValue(float fraction) {
            mIntAnimatedValue = mIntKeyframeSet.getIntValue(fraction);
        }
        
        @Override
        public IntPropertyValuesHolder clone() {
        
            IntPropertyValuesHolder newPVH = (IntPropertyValuesHolder) super.clone();
            newPVH.mIntKeyframeSet = (IntKeyframeSet) newPVH.mKeyframeSet;
            
            return newPVH;
        
        }
        
        @Override
        Object getAnimatedValue() {
            return mIntAnimatedValue;
        }
        
        /**
         * Internal function to set the value on the target object, using the setter set up
         * earlier on this PropertyValuesHolder object. This function is called by ObjectAnimator
         * to handle turning the value calculated by ValueAnimator into a value set on the object
         * according to the name of the property.
         *
         * @param target The target object on which the value is set
         */
        @Override
        @SuppressWarnings({"TryWithIdenticalCatches", "unchecked"})
        void setAnimatedValue(Object target) {
        
            if (mIntProperty != null) {
            
                mIntProperty.setValue(target, mIntAnimatedValue);
                return;
            
            }
            
            if (mProperty != null) {
            
                mProperty.set(target, mIntAnimatedValue);
                return;
            
            }
            
            if (mSetter != null) {
            
                try {
                
                    mTmpValueArray[0] = mIntAnimatedValue;
                    mSetter.invoke(target, mTmpValueArray);
                
                } catch (InvocationTargetException e) {
                    Log.e("PropertyValuesHolder", e.toString());
                } catch (IllegalAccessException e) {
                    Log.e("PropertyValuesHolder", e.toString());
                }
            
            }
        
        }
        
        @Override
        public void setIntValues(int... values) {
            super.setIntValues(values);
            
            mIntKeyframeSet = (IntKeyframeSet) mKeyframeSet;
        
        }
        
        @Override
        void setupSetter(Class targetClass) {
        
            if (mProperty != null)
                return;
            
            super.setupSetter(targetClass);
        
        }
    
    }

}