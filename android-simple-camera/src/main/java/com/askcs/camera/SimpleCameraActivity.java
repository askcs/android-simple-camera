package com.askcs.camera;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;
import com.askcs.simple_camera.R;


import java.io.File;
import java.util.UUID;

/**
 * TODO: provide class level documentation
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SimpleCameraActivity extends ActionBarActivity implements SimpleCameraFragment.Contract {

    public static Intent getIntent(Context context, boolean startWithFrontFacingCamera, String filename) {
        return new Intent(context, SimpleCameraActivity.class)
                .putExtra(EXTRA_START_WITH_FRONT_FACING_CAMERA, startWithFrontFacingCamera)
                .putExtra(EXTRA_FILENAME, filename);
    }

    public static Intent getIntent(Context context, boolean startWithFrontFacingCamera) {
        return new Intent(context, SimpleCameraActivity.class)
                .putExtra(EXTRA_START_WITH_FRONT_FACING_CAMERA, startWithFrontFacingCamera);
    }

    public static final int RESULT_CODE_ACCEPTED = 2;
    public static final int RESULT_CODE_REJECTED = 3;
    public static final int RESULT_CODE_CANCELLED = 4;
    public static final int RESULT_CODE_ERROR = 5;

    private static final String STATE_CAMERA_ID = "camera_id";

    public static final String EXTRA_START_WITH_FRONT_FACING_CAMERA = "start_with_front_facing_camera";
    public static final String EXTRA_CAMERA_SWITCHABLE = "camera_is_switchable";
    public static final String EXTRA_DIR = "dir";
    public static final String EXTRA_FILENAME = "filename";
    public static final String EXTRA_ERROR = "extra_error";
    public static final String EXTRA_SIZE = "extra_size";

    private static final String STATE_LOCK_TO_LANDSCAPE = "lock_to_landscape";

    private SimpleCameraFragment current = null;
    public static PicturePreviewFragment picturePreviewFragment = null;
    private VideoPreviewFragment videoPreviewFragment = null;

    private boolean hasTwoCameras = (Camera.getNumberOfCameras() > 1);
    private boolean isLockedToLandscape = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.sc_camera_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(Color.argb(50, 0, 0, 0)));
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        Intent intent = getIntent();

        current = SimpleCameraFragment.newInstance(
                intent.getBooleanExtra(EXTRA_START_WITH_FRONT_FACING_CAMERA, false),
                intent.getBooleanExtra(EXTRA_CAMERA_SWITCHABLE, false),
                intent.getStringExtra(EXTRA_DIR) == null ? null : new File(intent.getStringExtra(EXTRA_DIR)),
                intent.getStringExtra(EXTRA_FILENAME),
                SimpleCameraFragment.Size.tryOrdinal(intent.getIntExtra(EXTRA_SIZE, SimpleCameraFragment.Size.NORMAL.ordinal())));

        getSupportFragmentManager().beginTransaction().replace(R.id.sc_camera_container, current).commit();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {

        isLockedToLandscape = savedInstanceState.getBoolean(STATE_LOCK_TO_LANDSCAPE);

        if (current != null) {
            current.lockToLandscape(isLockedToLandscape);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_LOCK_TO_LANDSCAPE, isLockedToLandscape);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_CAMERA && current != null) {

            current.takePicture();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onPictureTaken(File photo) {

        picturePreviewFragment = PicturePreviewFragment.newInstance(photo);

        getSupportFragmentManager().beginTransaction().replace(R.id.sc_camera_container, picturePreviewFragment).commit();
    }

    @Override
    public void onVideoRecorded(File video) {

        Toast.makeText(this, "Video not implemented yet.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void startSwitchingCamera(boolean currentlyFront) {

        // Rebuild the SimpleCameraFragment with the same intent extra values (as in onCreate)
        // All parameters except the useCamFront is changed;
        // Tt's based on the current used camera, and then uses the other once (flip/switch cam)

        Intent intent = getIntent();

        // Determine which camera to use now
        boolean camFront = true;
        if(currentlyFront){
            camFront = false;
        }

        current = SimpleCameraFragment.newInstance(
                camFront,
                intent.getBooleanExtra(EXTRA_CAMERA_SWITCHABLE, false),
                intent.getStringExtra(EXTRA_DIR) == null ? null : new File(intent.getStringExtra(EXTRA_DIR)),
                intent.getStringExtra(EXTRA_FILENAME),
                SimpleCameraFragment.Size.tryOrdinal(intent.getIntExtra(EXTRA_SIZE, SimpleCameraFragment.Size.NORMAL.ordinal())));

        // Replace the existing fragment with the new (switched camera) one
        getSupportFragmentManager().beginTransaction().replace(R.id.sc_camera_container, current).commit();

    }

    @Override
    public void changeCameraContainerSize(int width, int height) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:
                setResult(RESULT_CODE_CANCELLED);
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onError(Throwable throwable) {

        setResult(RESULT_CODE_ERROR, new Intent().putExtra(EXTRA_ERROR, throwable.getCause()));
        finish();
    }

    protected void onCancel() {

        setResult(RESULT_CODE_CANCELLED);
        finish();
    }

    protected void onAccept(File file) {

        setResult(RESULT_CODE_ACCEPTED, new Intent().putExtra(EXTRA_FILENAME, file.getPath()));
        finish();
    }

    protected void onReject(File file) {

        setResult(RESULT_CODE_REJECTED, new Intent().putExtra(EXTRA_FILENAME, file.getPath()));
        finish();
    }

    public static abstract class Builder {

        private static final String TAG = Builder.class.getSimpleName();
        private boolean frontFacingCamera = false;
        private boolean cameraSwitchable = false;
        private File dir;
        private String fileName;
        private SimpleCameraFragment.Size size;

        public Builder() {
        }

        public Builder frontFacingCamera(boolean ffc) {
            this.frontFacingCamera = ffc;
            return this;
        }

        public Builder setCameraSwitchable(boolean cs) {
            this.cameraSwitchable = cs;
            return this;
        }

        public Builder dir(File dir) {
            if (dir.isDirectory()) {
                this.dir = dir;
            }
            else {
                Log.w(TAG, "dir is not a directory! Using default instead");
            }
            return this;
        }

        public Builder filename(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder size(SimpleCameraFragment.Size size) {
            this.size = size;
            return this;
        }

        protected Intent build(Context context, Class<? extends SimpleCameraActivity> cameraActivityClassyClass) {

            if (dir == null || !dir.isDirectory()) {
                dir = context.getCacheDir();
            }

            if (TextUtils.isEmpty(fileName)) {
                fileName = UUID.randomUUID().toString();
            }

            if (size == null) {
                size = SimpleCameraFragment.Size.NORMAL;
            }

            return new Intent(context, cameraActivityClassyClass)
                    .putExtra(EXTRA_START_WITH_FRONT_FACING_CAMERA, frontFacingCamera)
                    .putExtra(EXTRA_CAMERA_SWITCHABLE, cameraSwitchable)
                    .putExtra(EXTRA_DIR, dir.getPath())
                    .putExtra(EXTRA_FILENAME, fileName)
                    .putExtra(EXTRA_SIZE, size.ordinal());
        }

        protected void startForResult(Activity activity, Class<? extends SimpleCameraActivity> cameraActivityClass, int requestCode) {
            activity.startActivityForResult(build(activity, cameraActivityClass), requestCode);
        }

        public abstract Intent build(Context context);

        public abstract void startForResult(Activity activity, int requestCode);
    }
}
