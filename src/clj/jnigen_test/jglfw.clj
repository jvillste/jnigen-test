#_(ns jnigen-test.jglfw
  (:gen-class)
  (:import [com.badlogic.jglfw Glfw GlfwCallbackAdapter]))

#_(Glfw/glfwInit)

#_(def window (Glfw/glfwCreateWindow 640, 480
                                     "My Title"
                                     0
                                     0))

#_(def window (nanovg.NanoVG/createWindow))


#_(Glfw/glfwMakeContextCurrent window)
#_(Glfw/glfwDestroyWindow window)

#_(defn -main [& args]

     (Glfw/glfwInit)

     (Glfw/glfwSetCallback (proxy [GlfwCallbackAdapter] []
                             (error [error description]
                               (println description))
                             (windowPos [window x y]
                               (println "windowPos" x y))
                             (windowClose [window ]
                               (println "windowClose" window))
                             (windowRefresh [window ]
                               (println "windowRefresh" window))
                             (windowIconify [window]
                               (println "windowIconify" window))))

     (def window (Glfw/glfwCreateWindow 640, 480
                                        "My Title"
                                        0
                                        0))

     (Thread/sleep 2000)

     (Glfw/glfwDestroyWindow window))
