(ns jnigen-test.core
  (:import [nanovg NanoVG]
           [com.badlogic.gdx.jnigen JniGenSharedLibraryLoader]))

(defn load-nanovg []
  #_(System/loadLibrary "nanovg")
  #_(System/load "/Users/jukka/src/jnigen-test/target/native/macosx/x86_64/libnanovg.jnilib")
  #_(System/load "/Users/jukka/src/jnigen-test/libs/macosx64/libnative-lib64.dylib")
  #_(System/load "/private/var/folders/q6/nnwm65ss5zs_qv_zqx3zy3_w0000gn/T/jnigen/1244790105/libnative-lib64.dylib")
  
  #_(.load (JniGenSharedLibraryLoader.) "native-lib" #_"nanovg")
  (NanoVG/load))

(defn library-path []
  (System/getProperty "java.library.path") )

(defn start []
  #_(load-nanovg)
  #_(NanoVG/run)
  (NanoVG/init)

  )

#_(start)
