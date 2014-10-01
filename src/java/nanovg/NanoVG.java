package nanovg;

public class NanoVG {
    /*JNI


      #include <stdio.h>
      #include <OpenGL/gl3.h>
      #include "nanovg.h"
      #define NANOVG_GL3_IMPLEMENTATION
      #include "nanovg_gl.h"

    */

    static private native long initJni();/*
                                           return (jlong)nvgCreateGL3(NVG_ANTIALIAS | NVG_STENCIL_STROKES | NVG_DEBUG);
                                         */

    static public long init() {
        System.loadLibrary("nanovg");
        return initJni();
    }

    static public native void beginFrame(long vg, int width, int height);/*
                                                                           glViewport(0, 0, width, height);
                                                                           nvgBeginFrame((NVGcontext*)vg, width, height, 1.0);
                                                                             */

    static public native void endFrame(long vg);/*
                                                   nvgEndFrame((NVGcontext*)vg);
                                                    */

    
    static public native void delete(long vg);/*
                                                nvgDeleteGL3((NVGcontext*)vg);
                                              */


    static public native void beginPath(long vg);/*
                                                   nvgBeginPath((NVGcontext*)vg);
                                                 */


    static public native void rect(long vg, float x, float y, float w, float h);/*
                                                                                  nvgRect((NVGcontext*) vg, x, y, w, h);
                                                                                */


    static public native void roundRect(long vg, float x, float y, float w, float h, float r);/*
                                                                                                nvgRoundedRect((NVGcontext*) vg, x, y, w, h, r);
                                                                                              */

    static public native void fill(long vg);/*
                                              nvgFill((NVGcontext*)vg);
                                            */


    static public native void stroke(long vg);/*
                                                nvgStroke((NVGcontext*)vg);
                                              */

    static public native void fillColor(long vg, char r, char g, char b, char a);/*
                                                                                   nvgFillColor((NVGcontext*)vg, nvgRGBA(r,g,b,a));
                                                                                 */

    static public native void strokeColor(long vg, char r, char g, char b, char a);/*
                                                                                   nvgStrokeColor((NVGcontext*)vg, nvgRGBA(r,g,b,a));
                                                                                 */


    static public native void run (long vg_long);/*
                                                   NVGcontext* vg = (NVGcontext*)vg_long;

                                                   float x = 100;
                                                   float y = 100;
                                                   float width = 100;
                                                   int i;

                                                   nvgSave(vg);

                                                   nvgStrokeColor(vg, nvgRGBA(255,0,0,255));

                                                   for (i = 0; i < 20; i++) {
                                                   float w = (i+0.5f)*0.1f;
                                                   nvgStrokeWidth(vg, w);
                                                   nvgBeginPath(vg);
                                                   nvgMoveTo(vg, x,y);
                                                   nvgLineTo(vg, x+width,y+width*0.3f);
                                                   nvgStroke(vg);
                                                   y += 10;
                                                   }

                                                   nvgRestore(vg);

                                                 */



}
