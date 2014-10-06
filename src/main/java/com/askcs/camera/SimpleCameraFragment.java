package com.askcs.camera;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.MenuItemCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.askcs.R;
import com.commonsware.cwac.camera.CameraUtils;
import com.commonsware.cwac.camera.PictureTransaction;
import com.commonsware.cwac.camera.SimpleCameraHost;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * <p>A fragment that uses the device camera to take a picture or record a video. It starts showing a preview of the
 * camera, waiting for the user to take a picture or record a video.</p>
 *
 * <p><b>Pictures</b>The user can take a picture by clicking the inline camera icon. The live preview of the camera will
 * be shutdown, and the picture is showed to the user, whom can accept it or throw it away. </p>
 *
 * <p>To document</p>
 * <ul>
 * <li>Callbacks on which actions</li>
 * <li>Landscape locked or not?</li>
 * <li>Using front facing camera</li>
 * <li>File fileName</li>
 * <li>Flash modes</li>
 * <li>Auto focusing</li>
 * <li></li>
 * </ul>
 */
public class SimpleCameraFragment extends com.commonsware.cwac.camera.CameraFragment {

    private static final String TAG = SimpleCameraFragment.class.getSimpleName();

    public static final int MENU_ITEM_QUALITY = 18438;

    interface Contract {

        void onPictureTaken(File photo);

        void onVideoRecorded(File video);

        void startSwitchingCamera();

        void changeCameraContainerSize(int width, int height);
    }

    public static SimpleCameraFragment newInstance(
            boolean startWithFrontFacingCamera,
            File directory,
            String location,
            Size pictureSize) {

        SimpleCameraFragment fragment = new SimpleCameraFragment();

        Bundle args = new Bundle(4);
        args.putBoolean(KEY_USE_FFC, startWithFrontFacingCamera);
        args.putString(KEY_FILE_DIR, directory == null ? null : directory.getPath());
        args.putString(KEY_FILE_NAME, location != null ? location : UUID.randomUUID().toString());
        args.putInt(KEY_PICTURE_SIZE, pictureSize.ordinal());

        fragment.setArguments(args);
        return fragment;
    }

    public enum Size {
        AVATAR(640 * 480),
        NORMAL(1280 * 960);

        private final int product;

        Size(int product) {
            this.product = product;
        }

        static Size tryOrdinal(int ordinal) {
            for (Size size : values()) {
                if (size.ordinal() == ordinal) {
                    return size;
                }
            }
            return NORMAL;
        }
    }

    public static final String KEY_USE_FFC = "com.askcs.teamup.ui.fragment.CameraFragment.USE_FFC";
    public static final String KEY_FILE_DIR = "com.askcs.teamup.ui.fragment.CameraFragment.FILE_DIR";
    public static final String KEY_FILE_NAME = "com.askcs.teamup.ui.fragment.CameraFragment.EXTRA_FILENAME";
    public static final String KEY_PICTURE_SIZE = "com.askcs.teamup.ui.fragment.CameraFragment.PICTURE_SIZE";

    private Contract contractor;
    private String flashMode = null;

    private boolean useFrontFacingCamera;
    private boolean autoFocusAvailable;
    private File finalFile;
    private File dir;
    private String fileName;

    private int jpegQuality = 85;
    private Size imageSize = Size.NORMAL;

    private ImageButton btnTakePicture;
    private ImageButton btnSwitchCamera;
    private ImageButton btnRecordVideo;
    private ImageButton btnStopRecordingVideo;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        setHasOptionsMenu(true);

        String dirString = getArguments().getString(KEY_FILE_DIR);
        fileName = getArguments().getString(KEY_FILE_NAME);
        imageSize = Size.tryOrdinal(getArguments().getInt(KEY_PICTURE_SIZE, 0));
        useFrontFacingCamera = getArguments().getBoolean(KEY_USE_FFC);

        dir = TextUtils.isEmpty(dirString) ? getActivity().getCacheDir() : new File(dirString);

        SimpleCameraHost.Builder builder =
                new SimpleCameraHost.Builder(new CameraHost(getActivity()));

        builder.useFullBleedPreview(false);

        setHost(builder.build());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = super.onCreateView(inflater, container, savedInstanceState);

        FrameLayout root = (FrameLayout) inflater.inflate(R.layout.sc_camera_fragment, null);

        ((FrameLayout) root.findViewById(R.id.camera_container)).addView(view);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (autoFocusAvailable) {
                    autoFocus();
                }
            }
        });

        btnTakePicture = (ImageButton) root.findViewById(R.id.btn_take_picture);
        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTakePictureClick();
            }
        });

        btnSwitchCamera = (ImageButton) root.findViewById(R.id.btn_flip_camera);
        btnSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSwitchCameraClick();
            }
        });

        btnRecordVideo = (ImageButton) root.findViewById(R.id.btn_record_video);
        btnRecordVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRecordVideoClick();
            }
        });

        btnStopRecordingVideo = (ImageButton) root.findViewById(R.id.btn_stop_recording_video);
        btnStopRecordingVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStopRecordVideoClick();
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        resetButtons();
    }

    void onTakePictureClick() {

        btnTakePicture.setEnabled(false);
//        btnSwitchCamera.setEnabled(false);
        btnRecordVideo.setEnabled(false);

        takePicture();
    }

    void onSwitchCameraClick() {

        btnTakePicture.setEnabled(false);
        btnSwitchCamera.setEnabled(false);
        btnRecordVideo.setEnabled(false);
        btnStopRecordingVideo.setEnabled(false);

        // TODO
    }

    void onRecordVideoClick() {

        btnTakePicture.setEnabled(false);
        btnTakePicture.setVisibility(GONE);

        btnSwitchCamera.setEnabled(false);
        btnSwitchCamera.setVisibility(GONE);

        btnRecordVideo.setEnabled(false);
        btnRecordVideo.setVisibility(GONE);

        btnStopRecordingVideo.setEnabled(true);
        btnStopRecordingVideo.setVisibility(VISIBLE);

        try {
            record();
        }
        catch (Exception e) {
            Log.e(TAG, "Something went wrong while calling record()", e);
        }
    }

    void onStopRecordVideoClick() {

        btnStopRecordingVideo.setEnabled(false);

        try {
            stopRecording();
        }
        catch (IOException e) {
            Log.e(TAG, "Something went wrong while calling stopRecording()", e);
        }
    }

    void resetButtons() {

        if (btnTakePicture != null) {
            btnTakePicture.setVisibility(VISIBLE);
            btnTakePicture.setEnabled(true);
        }

        if (btnRecordVideo != null) {
            btnRecordVideo.setVisibility(GONE);
            btnRecordVideo.setEnabled(false);
        }

        if (btnSwitchCamera != null) {
            btnSwitchCamera.setVisibility(GONE);
            btnSwitchCamera.setEnabled(false);
        }

        if (btnStopRecordingVideo != null) {
            btnStopRecordingVideo.setVisibility(GONE);
            btnStopRecordingVideo.setEnabled(false);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.contractor = (Contract) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        this.contractor = null;
    }

    @Override
    public void takePicture() {
        if (!btnTakePicture.isEnabled()) {

            btnTakePicture.setVisibility(View.GONE);
            btnTakePicture.setEnabled(false);

            super.takePicture(new PictureTransaction(getHost()).flashMode(flashMode).mirrorFFC(true));
        }
    }

    @Override
    public void stopRecording() throws IOException {
        super.stopRecording();

        contractor.onVideoRecorded(finalFile);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem qualitySlider = menu.add(0, MENU_ITEM_QUALITY, 0, R.string.sc_menu_label_quality);
        MenuItemCompat.setShowAsAction(qualitySlider, MenuItem.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case MENU_ITEM_QUALITY:

                new DialogFragment() {

                    private int newJpegQuality;

                    @Override
                    public Dialog onCreateDialog(Bundle savedInstanceState) {

                        LinearLayout rootView = (LinearLayout) View.inflate(getActivity(), R.layout.sc_dialog_quality, null);

                        SeekBar qualityBar = (SeekBar) rootView.getChildAt(0);
                        qualityBar.setMax(50);
                        qualityBar.setProgress(jpegQuality - 50);
                        qualityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                if (fromUser) {
                                    newJpegQuality = 50 + progress;
                                    getDialog().setTitle(getString(R.string.sc_image_quality, newJpegQuality));
                                }
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {
                                // ignored
                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {
                                // ignored
                            }
                        });

                        return new AlertDialog.Builder(getActivity())
                                .setTitle("Image quality: " + jpegQuality + "%")
                                .setView(rootView)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        jpegQuality = newJpegQuality;
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .create();
                    }
                }.show(getFragmentManager(), "quality-slider");

                return true;
        }

        return (super.onOptionsItemSelected(item));
    }

    class CameraHost extends SimpleCameraHost {

        public CameraHost(Context _ctxt) {
            super(_ctxt);
        }

        @Override
        public Camera.Parameters adjustPictureParameters(PictureTransaction xact, Camera.Parameters parameters) {

            parameters.setJpegQuality(jpegQuality);

            for (Camera.Size size : parameters.getSupportedPictureSizes()) {
                Log.v(TAG, "w=" + size.width + ", h=" + size.height);
            }

            return parameters;
        }

        @Override
        public Camera.Size getPictureSize(PictureTransaction xact, Camera.Parameters parameters) {

            List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
            ArrayList<Integer> calculated = new ArrayList<Integer>();

            for (Camera.Size size : sizes) {
                calculated.add(size.height * size.width);
            }

            int closestIndexUntilNow = 0;
            int lastDelta = Integer.MAX_VALUE;
            for (int i = 0; i < calculated.size(); i++) {

                int delta = Math.abs(imageSize.product - calculated.get(i));

                if (delta < lastDelta) {
                    closestIndexUntilNow = i;
                }

                lastDelta = delta;
            }

            Camera.Size size = sizes.get(closestIndexUntilNow);

            Log.v(TAG, "Chosen size: w=" + size.width + ", h=" + size.height);

            return size;
        }

        @Override
        public float maxPictureCleanupHeapUsage() {
            return super.maxPictureCleanupHeapUsage();
        }

        @Override
        protected File getPhotoPath() {

            if (finalFile == null) {
                finalFile = super.getPhotoPath();
            }

            return finalFile;
        }

        @Override
        public RecordingHint getRecordingHint() {
            return RecordingHint.STILL_ONLY;
        }

        @Override
        protected File getVideoPath() {

            if (finalFile == null) {
                finalFile = super.getVideoPath();
            }

            return finalFile;
        }

        @Override
        protected File getPhotoDirectory() {
            return dir;
        }

        @Override
        protected String getPhotoFilename() {

            if (!fileName.endsWith(".jpg")) {
                fileName += ".jpg";
            }

            Log.v(TAG, String.valueOf(fileName));

            return fileName;
        }

        @Override
        protected File getVideoDirectory() {
            return dir;
        }

        @Override
        protected String getVideoFilename() {

            if (!fileName.endsWith(".mp4")) {
                fileName += ".mp4";
            }

            Log.v(TAG, String.valueOf(fileName));

            return fileName;
        }

        @Override
        public boolean useFrontFacingCamera() {
            return useFrontFacingCamera;
        }

        @Override
        public boolean useSingleShotMode() {
            return true;
        }

        @Override
        public void saveImage(PictureTransaction xact, byte[] image) {

            File photo = getPhotoPath();

            if (photo.exists()) {
                photo.delete();
            }

            try {
                FileOutputStream fos = new FileOutputStream(photo.getPath());
                BufferedOutputStream bos = new BufferedOutputStream(fos);

                bos.write(image);
                bos.flush();
                fos.getFD().sync();
                bos.close();
            }
            catch (IOException e) {
                handleException(e);
            }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    btnTakePicture.setEnabled(false);
                    btnRecordVideo.setEnabled(false);
                    btnStopRecordingVideo.setEnabled(false);

                    btnTakePicture.setVisibility(GONE);
                    btnRecordVideo.setVisibility(GONE);
                    btnStopRecordingVideo.setVisibility(GONE);

                    resetButtons();

                    contractor.onPictureTaken(new File(dir, fileName));
                }
            });
        }

        @Override
        public void autoFocusAvailable() {
            autoFocusAvailable = true;
        }

        @Override
        public void autoFocusUnavailable() {
            autoFocusAvailable = false;
        }

        @Override
        public void onCameraFail(com.commonsware.cwac.camera.CameraHost.FailureReason reason) {
            super.onCameraFail(reason);

            Toast.makeText(getActivity(),
                    "Sorry, but you cannot use the camera now!",
                    Toast.LENGTH_LONG).show();
        }

        @Override
        public Camera.Parameters adjustPreviewParameters(Camera.Parameters parameters) {
            flashMode =
                    CameraUtils.findBestFlashModeMatch(parameters,
                            Camera.Parameters.FLASH_MODE_RED_EYE,
                            Camera.Parameters.FLASH_MODE_AUTO,
                            Camera.Parameters.FLASH_MODE_ON);

            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            ArrayList<Integer> calculated = new ArrayList<Integer>();

            for (Camera.Size size : sizes) {
                calculated.add(size.height * size.width);
            }

            int closestIndexUntilNow = 0;
            int lastDelta = Integer.MAX_VALUE;
            for (int i = 0; i < calculated.size(); i++) {

                int delta = Math.abs(imageSize.product - calculated.get(i));

                if (delta < lastDelta) {
                    closestIndexUntilNow = i;
                }

                lastDelta = delta;
            }

            Camera.Size size = sizes.get(closestIndexUntilNow);

            parameters.setPreviewSize(size.width, size.height);

            return (super.adjustPreviewParameters(parameters));
        }

        @Override
        public Camera.Size getPreviewSize(int displayOrientation, int width, int height, Camera.Parameters parameters) {

            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            ArrayList<Integer> calculated = new ArrayList<Integer>();

            for (Camera.Size size : sizes) {
                calculated.add(size.width * size.height);
            }

            int closestIndexUntilNow = 0;
            int lastDelta = Integer.MAX_VALUE;
            for (int i = 0; i < calculated.size(); i++) {

                int delta = Math.abs(imageSize.product - calculated.get(i));

                if (delta < lastDelta) {
                    closestIndexUntilNow = i;
                }

                lastDelta = delta;
            }

            Camera.Size size = sizes.get(closestIndexUntilNow);
//            contractor.changeCameraContainerSize(size.width, size.height);

            return size;
        }

        @Override
        public boolean mirrorFFC() {
            return true;
        }
    }
}

