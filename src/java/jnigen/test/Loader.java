package jnigen.test;

import com.badlogic.gdx.jnigen.*;

public class Loader {
    // @off
    /*JNI 
      #include <dlfcn.h>
     */

    static public native void load (); /*
        dlopen("libs/macosx64/libmy-native-lib64.dylib", RTLD_LAZY);
    */

}
