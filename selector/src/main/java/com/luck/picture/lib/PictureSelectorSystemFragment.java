package com.luck.picture.lib;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.luck.picture.lib.basic.PictureCommonFragment;
import com.luck.picture.lib.config.PermissionEvent;
import com.luck.picture.lib.config.SelectMimeType;
import com.luck.picture.lib.config.SelectModeConfig;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.interfaces.OnRequestPermissionListener;
import com.luck.picture.lib.manager.SelectedManager;
import com.luck.picture.lib.permissions.PermissionChecker;
import com.luck.picture.lib.permissions.PermissionConfig;
import com.luck.picture.lib.utils.SdkVersionUtils;
import com.luck.picture.lib.utils.ToastUtils;

/**
 * @author：luck
 * @date：2022/1/16 10:22 下午
 * @describe：PictureSelectorSystemFragment
 */
public class PictureSelectorSystemFragment extends PictureCommonFragment {
    public static final String TAG = PictureSelectorSystemFragment.class.getSimpleName();

    public static PictureSelectorSystemFragment newInstance() {
        return new PictureSelectorSystemFragment();
    }

    @Override
    public String getFragmentTag() {
        return TAG;
    }

    @Override
    public int getResourceId() {
        return R.layout.ps_empty;
    }


    private ActivityResultLauncher<PickVisualMediaRequest> pickResultLauncher;

    private ActivityResultLauncher<PickVisualMediaRequest> pickMultipleMediaLauncher;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        createSystemContracts();
        openSystemAlbum();
    }

    @Override
    public void onApplyPermissionsEvent(int event, String[] permissionArray) {
        if (event == PermissionEvent.EVENT_SYSTEM_SOURCE_DATA) {
            selectorConfig.onPermissionsEventListener.requestPermission(this,
                    PermissionConfig.getReadPermissionArray(getAppContext(), selectorConfig.chooseMode), new OnRequestPermissionListener() {
                        @Override
                        public void onCall(String[] permissionArray, boolean isResult) {
                            if (isResult) {
                                openSystemAlbum();
                            } else {
                                handlePermissionDenied(permissionArray);
                            }
                        }
                    });
        }
    }

    /**
     * 打开系统相册
     */
    private void openSystemAlbum() {
        PickVisualMediaRequest request;
        if (selectorConfig.chooseMode == SelectMimeType.ofVideo()) {
            request = new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.VideoOnly.INSTANCE)
                    .build();
        } else if (selectorConfig.chooseMode == SelectMimeType.ofImage()) {
            request = new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build();
        } else {
            request = new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                    .build();
        }
        //单选
        if (selectorConfig.selectionMode == SelectModeConfig.SINGLE) {
            pickResultLauncher.launch(request);
        } else {//多选
            pickMultipleMediaLauncher.launch(request);
        }
    }

    private void createSystemContracts() {
        if (selectorConfig.selectionMode == SelectModeConfig.SINGLE) {
            createSingleContents();
        } else {
            createMultipleContents();
        }
    }

    /**
     * 单选图片或视频
     */
    private void createSingleContents() {
        pickResultLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), result -> {
            if (result == null) {
                onKeyBackFragmentFinish();
            } else {
                LocalMedia media = buildLocalMedia(result.toString());
                media.setPath(SdkVersionUtils.isQ() ? media.getPath() : media.getRealPath());
                int selectResultCode = confirmSelect(media, false);
                if (selectResultCode == SelectedManager.ADD_SUCCESS) {
                    dispatchTransformResult();
                } else {
                    onKeyBackFragmentFinish();
                }
            }
        });
    }

    /**
     * 多选图片或视频
     */
    private void createMultipleContents() {
        pickMultipleMediaLauncher = registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(9), result -> {
            if (result == null || result.isEmpty()) {
                onKeyBackFragmentFinish();
            } else {
                for (int i = 0; i < result.size(); i++) {
                    LocalMedia media = buildLocalMedia(result.get(i).toString());
                    media.setPath(SdkVersionUtils.isQ() ? media.getPath() : media.getRealPath());
                    selectorConfig.addSelectResult(media);
                }
                dispatchTransformResult();
            }
        });
    }

    /**
     * 获取选资源取类型
     *
     * @return
     */
    private String getInput() {
        if (selectorConfig.chooseMode == SelectMimeType.ofVideo()) {
            return SelectMimeType.SYSTEM_VIDEO;
        } else if (selectorConfig.chooseMode == SelectMimeType.ofAudio()) {
            return SelectMimeType.SYSTEM_AUDIO;
        } else {
            return SelectMimeType.SYSTEM_IMAGE;
        }
    }

    @Override
    public void handlePermissionSettingResult(String[] permissions) {
        onPermissionExplainEvent(false, null);
        boolean isCheckReadStorage;
        if (selectorConfig.onPermissionsEventListener != null) {
            isCheckReadStorage = selectorConfig.onPermissionsEventListener
                    .hasPermissions(this, permissions);
        } else {
            isCheckReadStorage = PermissionChecker.isCheckReadStorage(selectorConfig.chooseMode, getContext());
        }
        if (isCheckReadStorage) {
            openSystemAlbum();
        } else {
            ToastUtils.showToast(getContext(), getString(R.string.ps_jurisdiction));
            onKeyBackFragmentFinish();
        }
        PermissionConfig.CURRENT_REQUEST_PERMISSION = new String[]{};
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_CANCELED) {
            onKeyBackFragmentFinish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (pickResultLauncher != null) {
            pickResultLauncher.unregister();
        }
        if (pickMultipleMediaLauncher != null) {
            pickMultipleMediaLauncher.unregister();
        }
    }
}
