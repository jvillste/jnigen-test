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

(defn deploy [jar-file-name group artifact version]
  (shell/sh "mvn"
            "install:install-file"
            (str "-Dfile=" jar-file-name)
            (str "-DgroupId=" group)
            (str "-DartifactId=" artifact)
            (str "-Dversion=" version)
            "-Dpackaging=jar"))

(defn default-mac-target []
  (BuildTarget/newDefaultTarget com.badlogic.gdx.jnigen.BuildTarget$TargetOs/MacOsX true))

(defn generate-jni [jni-folder sources]
  (let [jnigen (NativeCodeGenerator.)
        ant-script-generator (AntScriptGenerator.)]

    (.generate jnigen "src/java" "target/classes" jni-folder (into-array String sources) nil)
    (.generate ant-script-generator (BuildConfig. "native-lib") (into-array BuildTarget [(default-mac-target)]))))

(defn build-jni [jni-folder]
  (BuildExecutor/executeAnt (str jni-folder "/build-macosx64.xml") "-v -Dhas-compiler=true clean postcompile")
  (BuildExecutor/executeAnt (str jni-folder "/build.xml") "-v pack-natives"))

(defn source-file-name-to-object-file-name [source-file-name]
  (-> (subs source-file-name
            (inc (.lastIndexOf source-file-name "/")))
      (string/replace ".cpp" ".o")
      (string/replace ".c" ".o")))

(deftest source-file-name-to-object-file-name-test
  (is (= (source-file-name-to-object-file-name "foo/bar.c") "bar.o"))
  (is (= (source-file-name-to-object-file-name "foo/bar.cpp") "bar.o"))
  (is (= (source-file-name-to-object-file-name "bar.cpp") "bar.o")))

(defn throw-on-shell-error [shell-return-value]
  (let [{:keys [exit err]} shell-return-value]
    (when (not (= exit 0))
      (throw (Exception. err)))
    shell-return-value))

(defn compile-native [compiler source-file-name jni-folder object-folder include-folders]


  (let [include-directives (map (fn [folder] (str "-I" folder))
                                include-folders)]

    (throw-on-shell-error
     (apply shell/sh compiler
            "-c"
            "-Wall"
            "-v"
            "-O2"
            "-arch" "x86_64"
            "-DFIXED_POINT"
            "-fPIC"
            "-fmessage-length=0"
            "-mmacosx-version-min=10.5"
            (concat include-directives
                    [source-file-name
                     "-o" (str object-folder "/" (source-file-name-to-object-file-name source-file-name))])))))

(defn reset-folder [folder-name]
  (when (.exists (File. folder-name))
    (shell/sh "rm" "-r" folder-name))

  (shell/sh "mkdir" folder-name))

(defn link-shared-library [library-file-name linkder-parameters object-file-names]
  (println "linking " object-file-names)

  (println (throw-on-shell-error
            (apply shell/sh
                   "g++"
                   "-v"
                   "-shared"
                   "-arch" "x86_64"
                   "-mmacosx-version-min=10.5"
                   (concat linkder-parameters
                           ["-o" library-file-name]
                           object-file-names)))))

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
  (throw-on-shell-error (shell/sh "lein" "javac")))

(defn package []
  (reset-folder "package"))

(defn start []
  (let [object-folder "obj"
        package-folder "package"
        jni-folder "jni"
        jar-file-name "nanovg.jar"
        source-file-names [#_"src/c/nanovg_wrapper.c"
                           "src/c/nanovg/nanovg.c"
                           (str jni-folder "/memcpy_wrap.c")
                           (str jni-folder "/nanovg.NanoVG.cpp")]
        jni-header-folders [jni-folder
                            (str jni-folder "/jni-headers")
                            (str jni-folder "/jni-headers/mac")]
        linker-parameters [#_"-lglfw3" "-framework" "OpenGL" "-framework" "Cocoa" "-framework" "IOKit" "-framework" "CoreVideo"]
        object-file-names (map (fn [source-file-name]
                                 (str object-folder "/" (source-file-name-to-object-file-name source-file-name)))
                               source-file-names)]

    (compile-java)

    (reset-folder jni-folder)
    (generate-jni jni-folder ["**/NanoVG.java"])

    (reset-folder object-folder)
    (doseq [source-file-name source-file-names]
      (compile-native (if (.endsWith source-file-name ".cpp") "g++" "gcc") source-file-name jni-folder object-folder
                      (concat jni-header-folders ["src/c" "src/c/nanovg"])))

    (reset-folder package-folder)
    (let [native-folder "/native/macosx/x86_64"]
      (shell/sh "mkdir" "-p" (str package-folder native-folder))
      (link-shared-library (str package-folder native-folder "/libnanovg.dylib")
                           linker-parameters
                           object-file-names))

    (shell/sh "cp" "-r" "target/classes/" (str package-folder "/"))
    (shell/sh "rm" "-r" (str package-folder "/META-INF"))

    (shell/sh "zip" "-r" jar-file-name "." "-i" "*" :dir package-folder)
    (deploy (str package-folder "/" jar-file-name) "org.clojars.jvillste" "nanovg" "1.0.0")


    (shell/sh "cp" "-r" (str package-folder "/native") "target/")
    (shell/sh "cp" "-r" "package/native" "target/")

    #_(build-jni jni-folder)
    #_(deploy "libs/native-lib-natives.jar" "org.clojars.jvillste2" "nanovg" "1.0.0")))

#_(start)

#_(deploy "/Users/jukka/Downloads/jglfw-nightly-20140921/jglfw-natives.jar" "org.clojars.jvillste" "jglfw-natives" "1.0.0")
#_(deploy "/Users/jukka/Downloads/jglfw-nightly-20140921/jglfw.jar" "org.clojars.jvillste" "jglfw" "1.0.0")

#_(run-tests)
