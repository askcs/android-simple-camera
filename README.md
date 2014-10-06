#Simple Camera
---

For our app [TeamUp](https://teamup.ask-cs.com), we needed complete control over the camera, due to a highly demanding
privacy requirement. An implementation on top of the [`cwac-camera`](https://github.com/commonsguy/cwac-camera) library,
created by [Mark Murphy](www.commonsware.org). 

You can also read the [https://github.com/askcs/android-simple-camera](blog post), with a more tutorial like example.

##Usage

This implementation is built around the [`CameraFragment`](https://github.com/commonsguy/cwac-camera/blob/master/camera/src/com/commonsware/cwac/camera/CameraFragment.java).
The CameraFragment is wrapped in an Activity, together with a Builder class, that only reveals a couple of the options
of the SimpleCameraHost. This is done to keep it simple.

###Using the Builder class
The following snippet creates a new `SimpleCameraActivity.Buidler` object, chooses the back facing camera, selects the 
`Size.AVATAR` and gives no location. If no location is given, the picture will be saved in the cache folder of your app. 

####Things to configure
Call these methods on the `SimpleCameraActivity.Builder` to configure.

__`frontFacingCamera(boolean)`__
Set to true to use the front facing camera. If the front facing camera is not available on the device, it will fall back on the back facing camera. 
Default `false`.

__`dir(File)`__
Sets the directory where the picture/video should be saved. 
Default: `context.getCacheDir()`.

__`filename(String)`__
Sets the filename. Make sure ___NOT___ to include the file extension! A picture will always be JPEG, a video will always be MP4.
Default: `UUID.randomUUID().toString()`

__`size(SimpleCameraFragment.Size)`__
Sets the size of the picture that will be saved. The preview of the SimpleCameraFragment will be adjusted to this size.
Default: `Size.NORMAL`

When done, `build(Context)` to get the Intent, or `startForResult(Activity, int)` to directly start the SimpleCameraActivity. 

Also, a `build(Context, Class)` and `startForResult(Activity, Class, int)` are available, whenever you subclass SimpleCameraActivity.

####Example
__MyActivity.java__

```java
public class MyActivity extends Activity {

	public static final int PICTURE_CODE = 1337;

	// Left out framework callbacks and other stuff for readability

    public void onClick(View view) {
        
        new SimpleCameraActivity.Builder()
                .frontFacingCamera(false)
                .size(SimpleCameraFragment.Size.AVATAR)
                .startForResult(this, PICTURE_CODE);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // We'll need this, keep on reading...
    }
}
```

###Or via an Intent
The following snippet creates a new Intent for `SimpleCameraActivity`. It chooses the back facing camera, selects the 
`Size.NORMAL` and gives the app cache directory as directory location and an earlier set filename.

#####Things to configure
Include these extras in the Intent to configure the `SimpleCameraActivity`. 
You could also use the `Builder` to configure it, and than call `.build(Context)` on it to get the Intent.

__`EXTRA_START_WITH_FRONT_FACING_CAMERA`__ _boolean_
Set to true to use the front facing camera. If the front facing camera is not available on the device, it will fall back on the back facing camera. 
Default `false`.

__`EXTRA_DIR`__ _String_
Sets the directory where the picture/video should be saved. 
Default : `context.getCacheDir()`.

__`EXTRA_FILENAME`__ _String_
The filename. Make sure ___NOT___ to include the file extension! A picture will always be JPEG, a video will always be MP4.
Default: `UUID.randomUUID().toString()`

__`EXTRA_SIZE`__ _int_
Sets the size of the picture that will be saved. Use the `.ordinal()` of a `SimpleCameraFragment.Size`. If the found integer is not known as an ordinal of `Size`, the default is used. The preview of the SimpleCameraFragment will be adjusted to this size.
Default: `Size.NORMAL`

####Example
__MyActivity.java__

```java
public class MyActivity extends Activity {

	public static final int PICTURE_CODE = 1337;

	// Left out framework callbacks and other stuff for readability

    public void onClick(View view) {
        
        Intent simpleCameraIntent = new Intent(context, SimpleCameraActivity.class)
                .putExtra(EXTRA_START_WITH_FRONT_FACING_CAMERA, frontFacingCamera)
                .putExtra(EXTRA_DIR, dir.getPath())
                .putExtra(EXTRA_FILENAME, fileName)
                .putExtra(EXTRA_SIZE, SimpleCameraFragment.Size.NORMAL.ordinal());
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // We'll need this, keep on reading...
    }
}
```

###Handle the result in onActivityResult()

__MyActivity.java__

```java
@Override
public void onActivityResult(int requestCode, int resultCode, Intent data) {

    switch (requestCode) {
        case PICTURE_CODE:

            switch (resultCode) {

                case SimpleCameraActivity.RESULT_CODE_ACCEPTED:

                    // Hurray! The image / video was taken by the user and succesfully saved to the given location
                    // You can find the location using data.getStringExtra(SimpleCameraActivity.EXTRA_FILENAME)

                    break;

                case SimpleCameraActivity.RESULT_CODE_CANCELLED: // fall through allowed, has same message
                case SimpleCameraActivity.RESULT_CODE_REJECTED:
                    
                    // The user was not satisfied, or did not want to take photo after all.
                    // You could display a message that taking the photo / video was cancelled
                    
                    break;

                case SimpleCameraActivity.RESULT_CODE_ERROR:
                    
                    // Oh snap! Something went wrong! The photo / video is not saved, if it was taken by the user
                    
                    break;

                default:
                    // Meh, the CameraActivity did not invoke setResult(). Please report that with the reason as an issue on GitHub :)
            }

            break;
        default:
            Log.d(TAG, "Unknown request code: " + requestCode + ". Expected " + requestCode);
    }
}
```

Some notes:

* A picture is always saved with a .jpg suffix
* A video is always saved with a .mp4 suffix
* The cwac-camera takes care of rotating the pictures the right way
* Recording videos and switching the camera is currently disabled

###Styling

The following attributes are available in the SimpleCamera theme:

* `scButtonSwitchCameraStyle`: Allows the user to switch front/back facing camera. Only when multiple cameras are available
* `scButtonTakePictureStyle`: Button to take the actual picture
* `scButtonRecordVideoStyle`: Button to start recording a video
* `scButtonStopRecordingStyle`: Button to stop recording a video. Visible after record button is clicked
* `scButtonAcceptStyle`: Button to accept the picture / video after it is taken
* `scButtonRejectStyle`: Button to reject the picture / video after it is taken

Don't forget to set the theme in your manifest:

__manifest.xml__
This is a part of the manifest. See the snippet at the top of the blog article for the complete manifest.

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" >

  <application android:icon="@drawable/ic_launcher" android:label="Your app">

    <!-- ... -->

    <activity android:name=".ui.activity.CameraActivity"
              android:icon="@drawable/ic_logo_team_up"
              android:theme="@style/Theme.SimpleCamera"/>

  </application>
</manifest>
```

## Prerequisites

The following should be locally installed:

* Git
* Maven 3.0.3+
* Android SDK

## Get Up and Running

### Install dependencies

The Android Support Library v13 and Google Maps v2 are needed by the
TeamUp App, which are not in the central Maven repository. You need to
install them using Android's SDK manager, and then use the
`maven-android-sdk-deployer` to put these libraries in your local Maven
repository.

#### Install Android Support Library v13

Start the SDK manager by doing:

```bash
$ANDROID_HOME/tools/android sdk
```

and install/update the Android Support Library v13 (it's located under
Extras), and install everything under SDK API level 17.

And then install it to your local Maven repository:

```bash
cd /tmp
git clone https://github.com/mosabua/maven-android-sdk-deployer.git
cd maven-android-sdk-deployer/extras/compatibility-v13
mvn install
```

###Clone this project

```bash
git clone https://github.com/askcs/android-simple-camera.git
cd team-up
```

###Create local.properties
Inside the root folder, `android-simple-camera/`, create a file called local.properties and add a single entry to it:

```bash
sdk.dir=/path/to/your/local/android/sdk
```

###Install
And install the library into your local maven repository

```bash
mvn clean install
```

##Using in your project

In your POM, add this library as a dependency. Note to add it as an `apklib`.

```bash
<dependency>
  <groupId>com.askcs</groupId>
  <artifactId>android-simple-camera</artifactId>
  <version>0.1-snapshot</version>
  <type>apklib</type>
</dependency>
```
