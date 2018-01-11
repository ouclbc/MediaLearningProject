package com.ouclbc.medialearningproject.Camera;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.ouclbc.medialearningproject.R;

/**camera2拍照
 * 1.调用Camera.open()，打开相机，默认为后置，可以根据摄像头ID来指定打开前置还是后置
 2.调用Camera.getParameters()得到一个Camera.Parameters对象
 3.使用步骤2得到的Camera.Parameters对象，对拍照参数进行设置
 4.调用Camera.setPreviewDispaly(SurfaceHolder holder)，指定使用哪个SurfaceView来显示预览图片
 5.调用Camera.startPreview()方法开始预览取景
 6.调用Camera.takePicture()方法进行拍照
 7.拍照结束后，调用Camera.stopPreview()结束取景预览，之后再replease()方法释放资源
 camera2
 1.获得摄像头管理器CameraManager mCameraManager，mCameraManager.openCamera()来打开摄像头
 2. 指定要打开的摄像头，并创建openCamera()所需要的CameraDevice.StateCallback stateCallback
 3.在CameraDevice.StateCallback stateCallback中调用takePreview()，这个方法中，使用CaptureRequest.Builder创建预览需要的CameraRequest，并初始化了CameraCaptureSession，最后调用了setRepeatingRequest(previewRequest, null, childHandler)进行了预览
 4.点击屏幕，调用takePicture()，这个方法内，最终调用了capture(mCaptureRequest, null, childHandler)
 5.在new ImageReader.OnImageAvailableListener(){}回调方法中，将拍照拿到的图片进行展示
 * Created by libaocheng on 2017-11-16.
 * https://github.com/googlesamples/android-Camera2Basic
 * https://github.com/googlesamples/android-Camera2Video ,TextureView
 *
 */

public class Camera2Activity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_layout);
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2SurfaceViewFragment.newInstance())
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
