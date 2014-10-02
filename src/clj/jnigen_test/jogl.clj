(ns jnigen-test.jogl
  (:require [clojure.core.async :as async])
  (:import [com.jogamp.newt.event WindowAdapter WindowEvent KeyAdapter KeyEvent MouseAdapter MouseEvent]
           [com.jogamp.newt.opengl GLWindow]
           [javax.media.opengl GLCapabilities GLProfile GLContext GL GL2 DebugGL2 DebugGL3 DebugGL4 GLEventListener GLAutoDrawable TraceGL2]
           [javax.media.nativewindow WindowClosingProtocol$WindowClosingMode]
           [nanovg NanoVG]))

(def keyboard-keys {KeyEvent/VK_ENTER :enter
                    KeyEvent/VK_ESCAPE :esc
                    KeyEvent/VK_LEFT :left
                    KeyEvent/VK_RIGHT :right
                    KeyEvent/VK_DOWN :down
                    KeyEvent/VK_UP :up
                    KeyEvent/VK_SPACE :space
                    KeyEvent/VK_BACK_SPACE :back-space
                    KeyEvent/VK_F1 :f1
                    KeyEvent/VK_F2 :f2
                    KeyEvent/VK_TAB :tab})

(def mouse-keys {MouseEvent/BUTTON1 :left-button
                 MouseEvent/BUTTON2 :right-button
                 MouseEvent/BUTTON3 :middle-button})

(defn key-code-to-key [key-code key-map]
  (let [key (key-map key-code)]
    (if key
      key
      :unknown)))

(defn create-event [event type]
  {:type type
   :time (.getWhen event)
   :shift (.isShiftDown event)
   :control (.isControlDown event)
   :alt (.isAltDown event)})

(defn create-keyboard-event [event type]
  #_(events/create-keyboard-event type
                                  (key-code-to-key (.getKeyCode event) keyboard-keys)
                                  (if (.isPrintableKey event)
                                    (.getKeyChar event)
                                    nil)
                                  (.getWhen event)))

(defn create-mouse-event [event type]
  (conj (create-event event type)
        {:x (.getX event)
         :y (.getY event)
         :key (key-code-to-key (.getButton event) mouse-keys)
         :source :mouse}))

(defn get-gl [profile ^javax.media.opengl.GLAutoDrawable drawable]
  (case profile
    :gl2 (DebugGL2. (.getGL2 (.getGL drawable)))
    :gl3 (DebugGL3. (.getGL3 (.getGL drawable)))
    :gl4 (DebugGL4. (.getGL4 (.getGL drawable))))

  #_(TraceGL2. (DebugGL2. (.getGL2 (.getGL drawable)))
               System/err))

(defn create
  ;; ([width height]
  ;;    (create width height identity (fn [gl width height]) nil :gl2))

  ;; ([width height init reshape]
  ;;    (create width height init reshape nil :gl2))

  ;; ([width height init reshape event-channel]
  ;;    (create width height init reshape event-channel :gl2))

  ([width height & {:keys [init reshape event-channel profile close-automatically] :or {init identity reshape (fn [gl width height]) event-channel nil profile :gl2 close-automatically false}}]
     (let [gl-profile (GLProfile/get (case profile
                                       :gl2 GLProfile/GL2
                                       :gl3 GLProfile/GL3
                                       :gl4 GLProfile/GL4))
           gl-capabilities (doto (GLCapabilities. gl-profile)
                             (.setDoubleBuffered true))
           display-atom (atom (fn [gl]))
           window (GLWindow/create gl-capabilities)]


       (when event-channel
         (doto window
           (.addKeyListener (proxy [KeyAdapter] []
                              (keyPressed [event]
                                (async/put! event-channel (create-keyboard-event event :key-pressed)))
                              (keyReleased [event]
                                (async/put! event-channel (create-keyboard-event event :key-released)))))

           (.addMouseListener (proxy [MouseAdapter] []
                                (mouseMoved [event]
                                  (async/put! event-channel (create-mouse-event event :mouse-moved)))
                                (mouseDragged [event]
                                  (async/put! event-channel (create-mouse-event event :mouse-dragged)))
                                (mousePressed [event]
                                  (async/put! event-channel (create-mouse-event event :mouse-pressed)))
                                (mouseReleased [event]
                                  (async/put! event-channel (create-mouse-event event :mouse-released)))
                                (mouseClicked [event]
                                  (async/put! event-channel (create-mouse-event event :mouse-clicked)))
                                (mouseWheelMoved [event]
                                  (async/put! event-channel (let [[x-distance y-distance z-distance] (.getRotation event)]
                                                              (assoc (create-mouse-event event :mouse-wheel-moved)
                                                                :x-distance x-distance
                                                                :y-distance y-distance
                                                                :z-distance z-distance
                                                                :rotation-scale (.getRotationScale event)))))))

           (.addWindowListener (proxy [WindowAdapter] []
                                 (windowDestroyNotify [event]
                                   (async/put! event-channel
                                               {:type :close-requested} #_(events/create-close-requested-event)))
                                 (windowResized [event]
                                   #_(async/go (async/>! event-channel
                                                         (events/create-resize-requested-event (.getWidth window)
                                                                                               (.getHeight window)))))))))

       (doto window
         (.addGLEventListener (proxy [GLEventListener] []
                                (display [^javax.media.opengl.GLAutoDrawable drawable]

                                  (let [gl (get-gl profile drawable)]
                                    (when @display-atom
                                      (do (@display-atom gl)
                                          (.swapBuffers drawable)))))

                                (init [^javax.media.opengl.GLAutoDrawable drawable]
                                  (let [gl (get-gl profile drawable)]
                                    (init gl)))

                                (reshape [^javax.media.opengl.GLAutoDrawable drawable x y width height]
                                  (async/go (async/>! event-channel
                                                      {} #_(events/create-resize-requested-event width height))))

                                (dispose [drawable])
                                (displayChanged [drawable mode-changed device-changed])))


         (.setAutoSwapBufferMode false)

         (.setSize width height)
         (.setVisible true))

       (when (not close-automatically)
         (.setDefaultCloseOperation window WindowClosingProtocol$WindowClosingMode/DO_NOTHING_ON_CLOSE))

       {:gl-window  window
        :display-atom display-atom
        :profile profile
        :event-channel event-channel})))

(defn width [window]
  (.getWidth (:gl-window window)))

(defn height [window]
  (.getHeight (:gl-window window)))

(defn close [window]
  (.destroy (:gl-window window)))

(defn render* [window renderer]
  (reset! (:display-atom window) renderer)
  (.display (:gl-window window)))

(defn swap-buffers [window]
  (.swapBuffers (:gl-window window)))

(defmacro render [window gl & body]
  `(render* ~window (fn [~gl] ~@body)))

(defmacro set-display [window gl & body]
  `(let [value-atom# (atom {})]
     (render* ~window
              (fn [~gl]
                (reset! value-atom#
                        (do ~@body))))
     @value-atom#))

(defmacro with-gl [window gl & body]
  `(let [value-atom# (atom {})]
     (render* ~window
              (fn [~gl]
                (reset! value-atom#
                        (do ~@body))))
     (reset! (:display-atom ~window) nil)
     @value-atom#))

(defn size [gl]
  (let [result-buffer (int-array 4)]
    (.glGetIntegerv gl GL2/GL_VIEWPORT result-buffer 0)
    {:width (aget result-buffer 2) :height (aget result-buffer 3)}))

(defn start []
  (let [width 300
        height 300
        margin 100
        triangle-width (- width (* 2 margin))
        triangle-height (- height (* 2 margin))
        window (create width
                       height
                       :profile :gl3
                       :close-automatically true)]

    (let [nvg (with-gl window gl
                (NanoVG/init))]

      (render window gl
              (let [{:keys [width height]} (size gl)]
                (.glClearColor gl 0 0 0 1)
                (.glClear gl GL2/GL_COLOR_BUFFER_BIT)
                (doto nvg
                  (NanoVG/beginFrame width height)
                  (NanoVG/run)
                  (NanoVG/fillColor (char 255) (char 0) (char 0) (char 255))
                  (NanoVG/beginPath)
                  (NanoVG/rect 100 100 100 100)
                  (NanoVG/fill)
                  (NanoVG/endFrame)))))

    #_(Thread/sleep 2000)
    #_(close window)))
