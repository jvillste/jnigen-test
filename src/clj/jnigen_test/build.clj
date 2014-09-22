(ns jnigen-test.build
  (:require [clojure.java.shell :as shell]
            [clojure.string :as string])
  (:import [com.badlogic.gdx.jnigen
            NativeCodeGenerator
            BuildTarget
            BuildConfig
            AntScriptGenerator
            BuildExecutor]
           [java.io File])
  (:use clojure.test))

(defn deploy [jni-directory name group artifact version]
  (BuildExecutor/executeAnt (str jni-directory "/build.xml") "-v pack-natives")

  (shell/sh "mvn"
            "install:install-file"
            (str "-Dfile=libs/" name "-natives.jar")
            (str "-DgroupId=" group)
            (str "-DartifactId=" artifact)
            (str "-Dversion=" version)
            "-Dpackaging=jar"))

(defn default-mac-target []
  (BuildTarget/newDefaultTarget com.badlogic.gdx.jnigen.BuildTarget$TargetOs/MacOsX true))

(defn build [jni-directory sources name build-targets]
  (let [jnigen (NativeCodeGenerator.)
        ant-script-generator (AntScriptGenerator.)]

    (.generate jnigen "src/java" "target/classes" jni-directory (into-array String sources) nil)
    (.generate ant-script-generator
               (BuildConfig. name "target" "libs" jni-directory)
               (into-array BuildTarget build-targets))
    (BuildExecutor/executeAnt (str jni-directory "/build-macosx64.xml") "-v -Dhas-compiler=true clean postcompile")))

(defn build-nanovg []
  (let [mac (default-mac-target)]
    (set! (.cIncludes mac) (into-array String [#_"../nanovg/nanovg.c" "../src/c/nanovg_wrapper.c"]) )
    (set! (.headerDirs mac) (into-array String [#_"../nanovg" "../src/c" #_"/usr/local/Cellar/glfw3/3.0.4/include"]) )
    #_(set! (.linkerFlags mac) "-L/usr/local/Cellar/glfw3/3.0.4/lib -framework OpenGL -framework Cocoa -framework IOKit -framework CoreVideo")
    #_(set! (.libraries mac) "-lglfw3")
    (build "jni_nanovg" ["**/NanoVG.java"] "nanovg" [mac])))

(defn deploy-nanovg []
  (deploy "jni_nanovg" "nanovg" "nanovg" "nanovg" "1.0.0"))

(defn source-file-name-to-object-file-name [source-file-name]
  (-> (subs source-file-name
            (inc (.lastIndexOf source-file-name "/")))
      (string/replace ".cpp" ".o")
      (string/replace ".c" ".o")))

(deftest source-file-name-to-object-file-name-test
  (is (= (source-file-name-to-object-file-name "foo/bar.c") "bar.o"))
  (is (= (source-file-name-to-object-file-name "foo/bar.cpp") "bar.o"))
  (is (= (source-file-name-to-object-file-name "bar.cpp") "bar.o")))

(defn compile-native [source-file-name object-folder]
  (shell/sh "gcc"
            "-c"
            #_"-v"
            "-fPIC"
            "-Wall"
            "-O2"
            "-arch" "x86_64"
            "-DFIXED_POINT"
            "-fmessage-length=0"
            "-mmacosx-version-min=10.5"
            "-Ijni_nanovg/jni-headers"
            "-Ijni_nanovg/jni-headers/mac"
            "-Ijni_nanovg"
            "-Isrc/c"
            source-file-name
            "-o" (str object-folder "/" (source-file-name-to-object-file-name source-file-name))))

(defn reset-folder [folder-name]
  (when (.exists (File. folder-name))
    (shell/sh "rm" "-r" folder-name))

  (shell/sh "mkdir" folder-name))

(defn make-shared-library [library-file-name object-file-names]
  (apply shell/sh
         "gcc"
         "-shared"
         "-o" library-file-name
         object-file-names))

(defn create-pom
  ([group-id artifact-id version]
     (create-pom group-id artifact-id version artifact-id "" ""))

  ([group-id artifact-id version name description url]
     (-> (slurp "pom-template.xml")
         (string/replace "group-id-placeholder" group-id)
         (string/replace "artifact-id-placeholder" artifact-id )
         (string/replace "version-placeholder" version)
         (string/replace "name-placeholder" name)
         (string/replace "description-placeholder" description)
         (string/replace "url-placeholder" url))))

(defn compile-java []
  (shell/sh "lein" "javac"))

(defn package []
  (reset-folder "package"))

(defn start []
  (let [object-folder "obj"
        package-folder "package"
        source-file-names ["src/c/nanovg_wrapper.cpp"
                           "jni_nanovg/nanovg.NanoVG.cpp"]
        object-file-names (map (fn [source-file-name]
                                 (str object-folder "/" (source-file-name-to-object-file-name source-file-name)))
                               source-file-names)]

    (reset-folder object-folder)
    
    (doseq [source-file-name source-file-names]
      (compile-native source-file-name object-folder))

    (reset-folder package-folder)
    (shell/sh "mkdir" "-p" (str package-folder "/native/macosx/X86_64"))
    (make-shared-library (str package-folder "/native/macosx/X86_64/nanovg.dylib")
                         object-file-names)

    
    (shell/sh "cp" "-r" "target/classes/" (str package-folder "/"))
    (shell/sh "rm" "-r" (str package-folder "/META-INF"))
    (shell/sh "zip" "-r" "nanovg.jar" "." "-i" "*" :dir package-folder)))

#_(start)

(run-tests)
