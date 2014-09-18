package jnigen.test;

import com.badlogic.gdx.jnigen.*;

public class Example {
    // @off

    static public native int add (int a, int b); /*
                                                   return a + b + 1;
                                                 */

    public static void main (String[] args) throws Exception {
        new JniGenSharedLibraryLoader().load("my-native-lib");
        System.out.println(add(1, 2));
    }
}
