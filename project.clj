(defproject jnigen-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.badlogicgames.gdx/gdx-jnigen "1.3.1"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 #_[org.clojars.jvillste/nanovg "1.0.0"]
                 #_[org.clojars.jvillste2/nanovg "1.0.0"]
                 #_[org.clojars.jvillste/jglfw-natives "1.0.0"]
                 #_[org.clojars.jvillste/jglfw "1.0.0"]
                 #_[org.clojars.jvillste/jogl-all "2.1.5"]]
  :source-paths ["src/clj"]
  ;; :jvm-opts ["-XstartOnFirstThread"]
  ;; :main jnigen-test.jglfw
  ;; :aot [jnigen-test.jglfw]
  :java-source-paths ["src/java"])
