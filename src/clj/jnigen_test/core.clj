(ns jnigen-test.core
  (:require [clojure.java.shell :as shell]
            [jnigen-test.build :as build])
  (:import [nanovg NanoVG]
           [com.badlogic.gdx.jnigen JniGenSharedLibraryLoader]))

(defn load-nanovg []
  (.load (JniGenSharedLibraryLoader.) "nanovg"))


(defn start []
  
  (load-nanovg)
  (NanoVG/run))
