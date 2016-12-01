package robertapengelly.support.animation;

import  robertapengelly.support.util.FloatProperty;
import  robertapengelly.support.util.IntProperty;
import  robertapengelly.support.util.Property;
import  android.view.View;

final class PreHoneycombCompat {

    static Property<View, Float> ALPHA = new FloatProperty<View>("alpha") {
    
        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getAlpha();
        }
        
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setAlpha(value);
        }
    
    };
    
    static Property<View, Float> PIVOT_X = new FloatProperty<View>("pivotX") {
    
        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getPivotX();
        }
        
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setPivotX(value);
        }
    
    };
    
    static Property<View, Float> PIVOT_Y = new FloatProperty<View>("pivotY") {
    
        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getPivotY();
        }
        
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setPivotY(value);
        }
    
    };
    
    static Property<View, Float> ROTATION = new FloatProperty<View>("rotation") {
    
        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getRotation();
        }
        
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setRotation(value);
        }
    
    };
    
    static Property<View, Float> ROTATION_X = new FloatProperty<View>("rotationX") {
    
        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getRotationX();
        }
        
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setRotationX(value);
        }
    
    };
    
    static Property<View, Float> ROTATION_Y = new FloatProperty<View>("rotationY") {
    
        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getRotationY();
        }
        
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setRotationY(value);
        }
    
    };
    
    static Property<View, Float> SCALE_X = new FloatProperty<View>("scaleX") {
    
        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getScaleX();
        }
        
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setScaleX(value);
        }
    
    };
    
    static Property<View, Float> SCALE_Y = new FloatProperty<View>("scaleY") {
    
        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getScaleY();
        }
        
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setScaleY(value);
        }
    
    };
    
    static Property<View, Integer> SCROLL_X = new IntProperty<View>("scrollX") {
    
        @Override
        public Integer get(View object) {
            return AnimatorProxy.wrap(object).getScrollX();
        }
        
        @Override
        public void setValue(View object, int value) {
            AnimatorProxy.wrap(object).setScrollX(value);
        }
    
    };
    
    static Property<View, Integer> SCROLL_Y = new IntProperty<View>("scrollY") {
    
        @Override
        public Integer get(View object) {
            return AnimatorProxy.wrap(object).getScrollY();
        }
        
        @Override
        public void setValue(View object, int value) {
            AnimatorProxy.wrap(object).setScrollY(value);
        }
    
    };
    
    static Property<View, Float> TRANSLATION_X = new FloatProperty<View>("translationX") {
    
        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getTranslationX();
        }
        
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setTranslationX(value);
        }
    
    };
    
    static Property<View, Float> TRANSLATION_Y = new FloatProperty<View>("translationY") {
    
        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getTranslationY();
        }
        
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setTranslationY(value);
        }
    
    };
    
    static Property<View, Float> X = new FloatProperty<View>("x") {
    
        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getX();
        }
        
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setX(value);
        }
    
    };
    
    static Property<View, Float> Y = new FloatProperty<View>("y") {
    
        @Override
        public Float get(View object) {
            return AnimatorProxy.wrap(object).getY();
        }
        
        @Override
        public void setValue(View object, float value) {
            AnimatorProxy.wrap(object).setY(value);
        }
    
    };

}