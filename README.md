# android-animation

Use the new animation classes on pre honycomb devices. Supports Android 2.3 API 9 (GINGERBREAD) and up.

Usage:

    Step 1. Add the JitPack repository to your build file
    
    Add it in your root build.gradle at the end of repositories:
    
    allprojects {
		  repositories {
			  ...
			  maven { url 'https://jitpack.io' }
		  }
	}
    
    Step 2. Add the dependency
    
    dependencies {
	        compile 'com.github.robertapengelly:android-animation:1.0.6'
	}

Usage:

    Import the animation classes
    
        import robertapengelly.support.animation.ValueAnimator;
    
    Creating an animator
    
        ValueAnimator anim = ValueAnimator.ofFloat(1f, 5f);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Log.i("ValueAnimator", "AnimatorUpdateLister: " + animation.getAnimatedValue()); 
            }
        
        });
        radiusAnimator.setDuration(200);
        radiusAnimator.setInterpolator(new LinearInterpolator());
        radiusAnimator.start();
