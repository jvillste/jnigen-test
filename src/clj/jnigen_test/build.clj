(ns jnigen-test.build
  (:require [clojure.java.shell :as shell])
  (:import [com.badlogic.gdx.jnigen
            NativeCodeGenerator
            BuildTarget
            BuildConfig
            AntScriptGenerator
            BuildExecutor
            JniGenSharedLibraryLoader]))

(defn deploy [jni-directory name group artifact version]
  (BuildExecutor/executeAnt (str jni-directory "/build.xml") "-v pack-natives")

  (shell/sh "mvn"
            "install:install-file"
            (str "-Dfile=libs/" name "-natives.jar")
            (str "-DgroupId=" group)
            (str "-DartifactId=" artifact)
            (str "-Dversion=" version)
            "-Dpackaging=jar"))

(defn build [jni-directory sources name]
  (let [jnigen (NativeCodeGenerator.)
        mac (BuildTarget/newDefaultTarget com.badlogic.gdx.jnigen.BuildTarget$TargetOs/MacOsX true)
        ant-script-generator (AntScriptGenerator.)]

    (.generate jnigen "src/java" "target/classes" jni-directory (into-array String sources) nil)
    (.generate ant-script-generator
               (BuildConfig. name "target" "libs" jni-directory)
               (into-array BuildTarget [mac]))
    (BuildExecutor/executeAnt (str jni-directory "/build-macosx64.xml") "-v -Dhas-compiler=true clean postcompile")))

(defn build-loader []
  (build "jni_loader" ["**/Loader.java"] "jni-test-loader"))

(defn build-example []
  (build "jni_example" ["**/Example.java"] "jni-test-example"))

(defn deploy-loader []
  (deploy "jni_loader" "jni-test-loader" "jni-test" "loader" "1.0.0"))

(defn deploy-example []
  (deploy "jni_example" "jni-test-example" "jni-test" "example" "1.0.0"))

(defn compile-java []
  (shell/sh "lein javac"))
