package in.buckbask.eye2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Color;
import android.os.Handler;
import android.os.Vibrator;

import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;

import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Frame.TrackingState;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PlaneHitResult;
import com.google.ar.core.Session;

import in.buckbask.eye2.rendering.BackgroundRenderer;
import in.buckbask.eye2.rendering.ObjectRenderer;
import in.buckbask.eye2.rendering.ObjectRenderer.BlendMode;
import in.buckbask.eye2.rendering.PlaneAttachment;
import in.buckbask.eye2.rendering.PlaneRenderer;
import in.buckbask.eye2.rendering.PointCloudRenderer;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.R.attr.data;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class DemoArActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = DemoArActivity.class.getSimpleName();

    /**
     * Whether or not the system UI should be auto-hidden after
     * {link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;

    // TODO potentially clean out unused material here
    private View mContentView;
    private View mControlsView;
    private boolean mVisible;

    private Vibrator v;
    private float distance; // determines vibration rate
    private float lastX;
    private float lastY;
    private ByteBuffer bb;
    private CentralPatternGenerator cpg;
    private Handler handler;
    private boolean activeVibration = false;


    // New code based on google example

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView mSurfaceView;

    private Config mDefaultConfig;
    private Session mSession;
    private BackgroundRenderer mBackgroundRenderer = new BackgroundRenderer();
    private GestureDetector mGestureDetector;
    private Snackbar mLoadingMessageSnackbar = null;

    private ObjectRenderer mVirtualObject = new ObjectRenderer();
    private ObjectRenderer mVirtualObjectShadow = new ObjectRenderer();
    private PlaneRenderer mPlaneRenderer = new PlaneRenderer();
    private PointCloudRenderer mPointCloud = new PointCloudRenderer();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] mAnchorMatrix = new float[16];

    // Tap handling and UI.
    private ArrayBlockingQueue<MotionEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(16);
    private ArrayList<PlaneAttachment> mTouches = new ArrayList<>();
    private MotionEvent last_move = null;

    // New code based on the Google example

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bb = ByteBuffer.allocateDirect(4);
        cpg = new CentralPatternGenerator();
        bb.order(ByteOrder.LITTLE_ENDIAN);
        setContentView(R.layout.activity_demoar);

        Log.d("DemoArActivity:onCreate", "setContentView!");

        mSurfaceView = (GLSurfaceView) findViewById(R.id.surfaceview);

        mSession = new Session(/*context=*/this);

        // Create default config, check is supported, create session from that config.
        mDefaultConfig = Config.createDefaultConfig();
        if (!mSession.isSupported(mDefaultConfig)) {
            Toast.makeText(this, "This device does not support AR", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // vibration output
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Set up tap listener.
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onSingleTap(e);
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                // update finger location to try and ray trace + pick colors
                lastX = e2.getRawX();
                lastY = e2.getRawY();

                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });

        // Set up renderer.
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        handler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (CameraPermissionHelper.hasCameraPermission(this)) {
            showLoadingMessage("Searching for surfaces...");
            // Note that order matters - see the note in onPause(), the reverse applies here.
            mSession.resume(mDefaultConfig);
            mSurfaceView.onResume();
        } else {
            CameraPermissionHelper.requestCameraPermission(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call mSession.update() and get a SessionPausedException.
        mSurfaceView.onPause();
        mSession.pause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            // | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            // | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void onSingleTap(MotionEvent e) {
        // Queue tap if there is space. Tap is lost if queue is full.
        mQueuedSingleTaps.offer(e);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create the texture and pass it to ARCore session to be filled during update().
        mBackgroundRenderer.createOnGlThread(/*context=*/this);
        mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());

        // Prepare the other rendering objects.
        try {
            mVirtualObject.createOnGlThread(/*context=*/this, "andy.obj", "andy.png");
            mVirtualObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

            mVirtualObjectShadow.createOnGlThread(/*context=*/this,
                    "andy_shadow.obj", "andy_shadow.png");
            mVirtualObjectShadow.setBlendMode(BlendMode.Shadow);
            mVirtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read obj file");
        }
        try {
            mPlaneRenderer.createOnGlThread(/*context=*/this, "trigrid.png");
        } catch (IOException e) {
            Log.e(TAG, "Failed to read plane texture");
        }
        mPointCloud.createOnGlThread(/*context=*/this);
    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        mSession.setDisplayGeometry(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        try {
            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = mSession.update();

            // Handle taps. Handling only one tap per frame, as taps are usually low frequency
            // compared to frame rate.
            MotionEvent tap = mQueuedSingleTaps.poll();
            if (tap != null && frame.getTrackingState() == TrackingState.TRACKING) {
                for (HitResult hit : frame.hitTest(tap)) {
                    // Check if any plane was hit, and if it was hit inside the plane polygon.
                    if (hit instanceof PlaneHitResult && ((PlaneHitResult) hit).isHitInPolygon()) {
                        // Cap the number of objects created. This avoids overloading both the
                        // rendering system and ARCore.
                        if (mTouches.size() >= 16) {
                            mSession.removeAnchors(Arrays.asList(mTouches.get(0).getAnchor()));
                            mTouches.remove(0);
                        }
                        // Adding an Anchor tells ARCore that it should track this position in
                        // space. This anchor will be used in PlaneAttachment to place the 3d model
                        // in the correct position relative both to the world and to the plane.
                        mTouches.add(new PlaneAttachment(
                                ((PlaneHitResult) hit).getPlane(),
                                mSession.addAnchor(hit.getHitPose())));

                        // Hits are sorted by depth. Consider only closest hit on a plane.
                        break;
                    }
                }
            }

            // Draw background.
            mBackgroundRenderer.draw(frame);

            GLES20.glReadPixels((int) lastX, (int) lastY, 1, 1,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bb);
            // Log.d("read pixel", ""+lastX+" "+lastY+" ");

            // If not tracking, don't draw 3d objects.
            if (frame.getTrackingState() == TrackingState.NOT_TRACKING) {
                showLoadingMessage("I'm lost.");
                return;
            }

            // processRayTracing(frame);
            hiResRayTracing(frame);

            // Get projection matrix.
            float[] projmtx = new float[16];
            mSession.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            frame.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            final float lightIntensity = frame.getLightEstimate().getPixelIntensity();

            // Visualize tracked points.
            mPointCloud.update(frame.getPointCloud());
            mPointCloud.draw(frame.getPointCloudPose(), viewmtx, projmtx);

            // Check if we detected at least one plane. If so, hide the loading message.
            if (mLoadingMessageSnackbar != null) {
                for (Plane plane : mSession.getAllPlanes()) {
                    if (plane.getType() == com.google.ar.core.Plane.Type.HORIZONTAL_UPWARD_FACING &&
                            plane.getTrackingState() == Plane.TrackingState.TRACKING) {
                        hideLoadingMessage();
                        break;
                    }
                }
            }

            // Visualize planes.
            mPlaneRenderer.drawPlanes(mSession.getAllPlanes(), frame.getPose(), projmtx);

            // Visualize anchors created by touch.
            float scaleFactor = 1.0f;
            for (PlaneAttachment planeAttachment : mTouches) {
                if (!planeAttachment.isTracking()) {
                    continue;
                }
                // Get the current combined pose of an Anchor and Plane in world space. The Anchor
                // and Plane poses are updated during calls to session.update() as ARCore refines
                // its estimate of the world.
                planeAttachment.getPose().toMatrix(mAnchorMatrix, 0);

                // Update and draw the model and its shadow.
                mVirtualObject.updateModelMatrix(mAnchorMatrix, scaleFactor);
                mVirtualObjectShadow.updateModelMatrix(mAnchorMatrix, scaleFactor);
                mVirtualObject.draw(viewmtx, projmtx, lightIntensity);
                mVirtualObjectShadow.draw(viewmtx, projmtx, lightIntensity);
            }

            // capture color


        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    private void showLoadingMessage(String content) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingMessageSnackbar = Snackbar.make(
                        DemoArActivity.this.findViewById(android.R.id.content),
                        content, Snackbar.LENGTH_INDEFINITE);
                mLoadingMessageSnackbar.getView().setBackgroundColor(0xbf323232);
                mLoadingMessageSnackbar.show();
            }
        });
    }

    private void hideLoadingMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingMessageSnackbar.dismiss();
                mLoadingMessageSnackbar = null;
            }
        });
    }

    private void processRayTracing(Frame frame) {
        // Ray Trace on display center, in a 3x3 grid
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        Float center_dist = (float) (0.0);
        for (int i = 1; i < 4; i++) {
            for (int j = 1; j < 4; j++) {
                List<HitResult> results = frame.hitTest((float) (size.x * i * 0.25), (float) (size.y * j * 0.25));
                for (HitResult hr : results) {
                    center_dist = center_dist / 2 + hr.getDistance() / 2;
                    break;
                }
            }
        }

        Float left_dist = 6.0f;
        for (HitResult hr : frame.hitTest((float) (size.x * .125), (float) (size.y * .5))) {
            left_dist = hr.getDistance();
        }
        Float right_dist = 6.0f;
        for (HitResult hr : frame.hitTest((float) (size.x * (1 - .125)), (float) (size.y * .5))) {
            right_dist = hr.getDistance();
        }
        Float top_dist = 6.0f;
        for (HitResult hr : frame.hitTest((float) (size.x * 0.5), (float) (size.y * 1.0/12))) {
            top_dist = hr.getDistance();
        }
        Float bottom_dist = 6.0f;
        for (HitResult hr : frame.hitTest((float) (size.x * 0.5), (float) (size.y * (1 - 1.0/12)))) {
            bottom_dist = hr.getDistance();
        }

        Log.d("DemoArActivity:processR", "dist to obstacle " + center_dist.toString());

        updateText(R.id.LeftRayTraceView, left_dist);
        updateText(R.id.RightRayTraceView, right_dist);
        updateText(R.id.TopRayTraceView, top_dist);
        updateText(R.id.BottomRayTraceView, bottom_dist);
        updateText(R.id.CenterRayTraceView, center_dist);
    }

    public void endVibrator() {
        this.activeVibration = false;
        this.v.cancel();
    }

    private CentralPatternGenerator.COLOR colorMode(ByteBuffer bb) {
        float r = 127 - bb.get(0);
        float g = 127 - bb.get(1);
        float b = 127 - bb.get(2);
        // Log.d("rgb", ""+r+" "+g+" "+b);
        float[] hsv = new float[3];

        Color.RGBToHSV((int) (r), (int) (g), (int) (b), hsv);

        Log.d("hsv", ""+hsv[0]+" "+hsv[1]+" "+hsv[2]);

        if(hsv[2] > 0.8 && hsv[1] < .2) {
            return CentralPatternGenerator.COLOR.WHITE;
        } else if (hsv[2] < .2 && hsv[1] < .2) {
            return CentralPatternGenerator.COLOR.BLACK;
        } else if (hsv[0] > 60 && hsv[0] < 180) {
            return CentralPatternGenerator.COLOR.GREEN;
        } else if (hsv[0] >= 180 && hsv[0] < 300) {
            return CentralPatternGenerator.COLOR.BLUE;
        }
        return CentralPatternGenerator.COLOR.RED;
    }

    private void hiResRayTracing(Frame frame) {
        List<HitResult> results = frame.hitTest(lastX, lastY);
        if (results.size() > 0) {
            distance = (distance + results.get(0).getDistance()) / 2;
            updateText(R.id.CenterRayTraceView, distance);
            if (!activeVibration) {
                v.vibrate(cpg.pattern(colorMode(bb), 1.0f / distance), 0);
                activeVibration = true;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        endVibrator();
                    }
                }, 600);
            }
        } else {
            // ease back because nothing was found. Do no vibration
            if (distance < .01f) {
                distance = .01f;
            }
            distance *= 2;
        }
    }

    int spaceJokesIndex = 0;

    private void updateText(int viewId, Float dist) {
        // TODO add insightful quotes here
        dist -= 1;
        if (dist > 5) {
            findViewById(viewId).setContentDescription(SPACE_JOKES[spaceJokesIndex++]);
            spaceJokesIndex = spaceJokesIndex % SPACE_JOKES.length;
        } else if (dist < 1.0) {
            findViewById(viewId).setContentDescription("You're pretty close to an obstacle here");
        } else if (dist < .01) {
            findViewById(viewId).setContentDescription("I really don't know about this one.");
        }
        else {
            findViewById(viewId).setContentDescription("Avoid the obstacle here in about "+ ((int) dist.floatValue()) + "meters");
        }
    }

    private static final String[] SPACE_JOKES = {
      "There's room in your life for more cowbell here. And walking.",
            "I would walk 500 miles and I would walk 500 more...",
            "These boots are made for walking, and that's just what they'll do",
    };
}
