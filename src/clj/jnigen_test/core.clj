(ns jnigen-test.core
  (:require [clojure.java.shell :as shell])
  (:import [jnigen.test Example Loader]
           [jnigen-test.build :as build]))


(defn start []
  (build)
  (load)
  (Loader/load)
  (Example/add 2 2))
