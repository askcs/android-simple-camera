package com.askcs.camera;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import com.askcs.simple_camera.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import uk.co.senab.photoview.PhotoView;

import java.io.File;

/**
 * TODO: provide class level documentation
 */
/* package */ class PicturePreviewFragment extends Fragment implements Callback {

    private static final String EXTRA_PICTURE_LOCATION = "extra_picture_location";

    public static PicturePreviewFragment newInstance(File picture) {
        PicturePreviewFragment fragment = new PicturePreviewFragment();

        Bundle args = new Bundle(1);
        args.putString(EXTRA_PICTURE_LOCATION, picture.toString());

        fragment.setArguments(args);

        return fragment;
    }

    private PhotoView photoView;
    private File picture;

    private ImageButton btnAccept;
    private ImageButton btnReject;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sc_picture_preview, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        photoView = (PhotoView) view.findViewById(R.id.sc_photo);

        btnAccept = (ImageButton) view.findViewById(R.id.sc_btn_accept);
        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCameraActivity().onAccept(picture);
            }
        });

        btnReject = (ImageButton) view.findViewById(R.id.sc_btn_reject);
        btnReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCameraActivity().onReject(picture);
            }
        });

        picture = new File(getArguments().getString(EXTRA_PICTURE_LOCATION));
        Picasso.with(getActivity()).load(picture).into(photoView, this);
    }

    public SimpleCameraActivity getCameraActivity() {
        return (SimpleCameraActivity) getActivity();
    }

    @Override
    public void onSuccess() {

        btnAccept.setEnabled(true);
        btnReject.setEnabled(true);
    }

    @Override
    public void onError() {

        btnAccept.setEnabled(false);
        btnReject.setEnabled(false);
    }
}
