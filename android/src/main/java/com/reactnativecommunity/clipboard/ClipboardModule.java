/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.reactnativecommunity.clipboard;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

/**
 * A module that allows JS to get/set clipboard contents.
 */
@ReactModule(name = ClipboardModule.NAME)
public class ClipboardModule extends ContextBaseJavaModule {

  public static final String CLIPBOARD_TEXT_CHANGED = "RNCClipboard_TEXT_CHANGED";
  private ReactApplicationContext reactContext;
  private ClipboardManager.OnPrimaryClipChangedListener listener = null;

  public ClipboardModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  public static final String NAME = "RNCClipboard";

  @Override
  public String getName() {
    return ClipboardModule.NAME;
  }

  private ClipboardManager getClipboardService() {
    return (ClipboardManager) getContext().getSystemService(getContext().CLIPBOARD_SERVICE);
  }

  @ReactMethod
  public void getString(Promise promise) {
    try {
      ClipboardManager clipboard = getClipboardService();
      ClipData clipData = clipboard.getPrimaryClip();
      if (clipData != null && clipData.getItemCount() >= 1) {
        ClipData.Item firstItem = clipboard.getPrimaryClip().getItemAt(0);
        promise.resolve("" + firstItem.getText());
      } else {
        promise.resolve("");
      }
    } catch (Exception e) {
      promise.reject(e);
    }
  }

  @ReactMethod
  public void setString(String text) {
    try {
      ClipData clipdata = ClipData.newPlainText(null, text);
      ClipboardManager clipboard = getClipboardService();
      clipboard.setPrimaryClip(clipdata);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @ReactMethod
  public void hasString(Promise promise) {
    try {
      ClipboardManager clipboard = getClipboardService();
      ClipData clipData = clipboard.getPrimaryClip();
      promise.resolve(clipData != null && clipData.getItemCount() >= 1);
    } catch (Exception e) {
      promise.reject(e);
    }
  }

  @ReactMethod
  public void setListener() {
    try {
      ClipboardManager clipboard = getClipboardService();
      listener = new ClipboardManager.OnPrimaryClipChangedListener() {
        @Override
        public void onPrimaryClipChanged() {
          reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(CLIPBOARD_TEXT_CHANGED, null);
        }
      };
      clipboard.addPrimaryClipChangedListener(listener);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @ReactMethod
  public void setImage(String base64) {
    String replaceBase64 = base64.replace("data:application/octet-stream;base64,","");
    byte[] decodedString = Base64.decode(replaceBase64, Base64.DEFAULT);
    // Convert Bitmap From base64 data
    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    String filename = "ex_clip.png";
    File file = Environment.getExternalStorageDirectory();
    File dest = new File(file, filename);
    try {
      FileOutputStream out = new FileOutputStream(dest);
      bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
      out.flush();
      out.close();

      ClipboardManager clipboard = getClipboardService();
      ContentValues values = new ContentValues(2);
      values.put(MediaStore.Images.Media.MIME_TYPE,"image/png");
      values.put(MediaStore.Images.Media.DATA,dest.getAbsolutePath());
      ContentResolver contentResolver = getContext().getContentResolver();
      Uri imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
      ClipData clipData = ClipData.newUri(theContent, dest.getName(), imageUri);
      clipboard.setPrimaryClip(clipData);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @ReactMethod
  void removeListener() {
    if(listener != null){
      try{
        ClipboardManager clipboard = getClipboardService();
        clipboard.removePrimaryClipChangedListener(listener);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
