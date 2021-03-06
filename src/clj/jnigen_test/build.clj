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

(defn reset-folder [folder-name]
  (when (.exists (File. folder-name))
    (shell/sh "rm" "-r" folder-name))

  (shell/sh "mkdir" folder-name))

(defn deploy [jar-file-name group artifact version]
  (shell/sh "mvn"
            "install:install-file"
            (str "-Dfile=" jar-file-name)
            (str "-DgroupId=" group)
            (str "-DartifactId=" artifact)
            (str "-Dversion=" version)
            "-Dpackaging=jar"))

(defn generate-jni [jni-folder sources]
  (reset-folder jni-folder)
  (let [jnigen (NativeCodeGenerator.)
        ant-script-generator (AntScriptGenerator.)]

    (.generate jnigen "src/java" "target/classes" jni-folder (into-array String sources) nil)
    (.generate ant-script-generator (BuildConfig. "native-lib") (into-array BuildTarget
                                                                            [(BuildTarget/newDefaultTarget com.badlogic.gdx.jnigen.BuildTarget$TargetOs/MacOsX true)
                                                                             (BuildTarget/newDefaultTarget com.badlogic.gdx.jnigen.BuildTarget$TargetOs/Linux false)]))))

(defn build-jni [jni-folder]
  (BuildExecutor/executeAnt (str jni-folder "/build-macosx64.xml") "-v -Dhas-compiler=true clean postcompile")
  (BuildExecutor/executeAnt (str jni-folder "/build-linux32.xml") "-v -Dhas-compiler=true clean postcompile")
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

(defn compile-native [compiler source-file-name object-folder include-folders]


  (let [include-directives (map (fn [folder] (str "-I" folder))
                                include-folders)]

    ;; mac
    #_(throw-on-shell-error
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
                       "-o" (str object-folder "/" (source-file-name-to-object-file-name source-file-name))])))

    ;; linux
    (throw-on-shell-error
     (apply shell/sh compiler
            "-c"
            "-Wall"
            "-v"
            "-O2"
            ;; "-arch" "x86_64"
            "-DFIXED_POINT"
            "-fPIC"
            "-fmessage-length=0"
            ;; "-mmacosx-version-min=10.5"
            (concat include-directives
                    [source-file-name
                     "-o" (str object-folder "/" (source-file-name-to-object-file-name source-file-name))])))))



(defn link-shared-library [library-file-name linker-parameters object-file-names]
  (println "linking " object-file-names)

  ;; mac
  #_(println (throw-on-shell-error
              (apply shell/sh
                     "g++"
                     "-v"
                     "-shared"
                     "-arch" "x86_64"
                     "-mmacosx-version-min=10.5"
                     (concat linker-parameters
                             ["-o" library-file-name]
                             object-file-names))))
  ;; linux
  (println (throw-on-shell-error
            (apply shell/sh
                   "g++"
                   "-v"
                   "-shared"
                   ;;               "-arch" "x86_64"
                   ;;               "-mmacosx-version-min=10.5"
                   (concat linker-parameters
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


(defn make-jar [package-folder jar-file-name]
  (shell/sh "cp" "-r" "target/classes/" (str package-folder "/"))
  (shell/sh "rm" "-r" (str package-folder "/META-INF"))
  (shell/sh "zip" "-r" jar-file-name "." "-i" "*" :dir package-folder))

(defn compile [object-folder source-file-names header-folders]
  (reset-folder object-folder)
  (doseq [source-file-name source-file-names]
    (compile-native (if (.endsWith source-file-name ".cpp") "g++" "gcc")
                    source-file-name
                    object-folder
                    header-folders)))

(defn link [package-folder linker-parameters object-file-names native-folder shared-library-file-name]
  (reset-folder package-folder)
  (shell/sh "mkdir" "-p" (str package-folder native-folder))
  (link-shared-library (str package-folder native-folder "/" shared-library-file-name)
                       linker-parameters
                       object-file-names))

(defn start []
  (let [object-folder "obj"
        package-folder "package"
        jni-folder "jni"
        jar-file-name "nanovg.jar"
        source-file-names ["src/c/nanovg/nanovg.c"
                           (str jni-folder "/memcpy_wrap.c")
                           (str jni-folder "/nanovg.NanoVG.cpp")]
        jni-header-folders [jni-folder
                            (str jni-folder "/jni-headers")]
        object-file-names (map (fn [source-file-name]
                                 (str object-folder "/" (source-file-name-to-object-file-name source-file-name)))
                               source-file-names)]

    (compile-java)
    
    (generate-jni jni-folder ["**/NanoVG.java"])

    #_(compile object-folder
               source-file-names
               (concat jni-header-folders [(str jni-folder "/jni-headers/mac") "src/c" "src/c/nanovg"]))

    (compile object-folder
             source-file-names
             (concat jni-header-folders [(str jni-folder "/jni-headers/linux") "src/c" "src/c/nanovg"]))

    ;; mac
    #_(link package-folder
            ["-framework" "OpenGL" "-framework" "Cocoa" "-framework" "IOKit" "-framework" "CoreVideo"]
            object-file-names
            "/native/macosx/x86_64"
            "libnanovg.dylib")

    ;; linux 32
    (link package-folder
          ["-lGL" "-lGLU" "-lm" "-lGLEW"]
          object-file-names
          "/native/linux/x86"
          "libnanovg.so")

    (make-jar package-folder jar-file-name)

    (deploy (str package-folder "/" jar-file-name) "org.clojars.jvillste" "nanovg" "1.0.0")

    #_(shell/sh "cp" "-r" (str package-folder "/native") "target/")

    #_(build-jni jni-folder)
    #_(deploy "libs/native-lib-natives.jar" "org.clojars.jvillste2" "nanovg" "1.0.0")))

#_(start)

#_(deploy "/Users/jukka/Downloads/jglfw-nightly-20140921/jglfw-natives.jar" "org.clojars.jvillste" "jglfw-natives" "1.0.0")
#_(deploy "/Users/jukka/Downloads/jglfw-nightly-20140921/jglfw.jar" "org.clojars.jvillste" "jglfw" "1.0.0")

#_(run-tests)
