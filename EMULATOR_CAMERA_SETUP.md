# Android Emulator Camera Configuration Guide

## Steps to Configure Your Laptop Camera

1. **Close the Android Emulator** (if running)

2. **Find your AVD directory**:
   - Windows: C:\Users\YourUsername\.android\avd\
   - Look for folder ending with .avd (e.g., Pixel_5_API_33.avd)

3. **Edit config.ini**:
   - Open the file: YourEmulatorName.avd\config.ini
   - Add or modify these lines:

```ini
hw.camera.back=webcam0
hw.camera.front=webcam0
```

4. **Save and restart emulator**

## Troubleshooting

### If camera doesn't work:
1. Make sure your laptop webcam is not being used by another application
2. Check Windows privacy settings:
   - Settings → Privacy → Camera
   - Enable "Allow apps to access your camera"
3. Try restarting Android Studio and the emulator
4. Update emulator tools: Tools → SDK Manager → SDK Tools → Update Android Emulator

### Test the camera:
1. Open the emulator
2. Open the Camera app (in app drawer)
3. If you see your webcam feed, it's working!
4. Now test in your social media app

## Camera Permissions in Your App

Your app will automatically request these permissions when you try to:
- Upload a story
- Take a profile picture
- Use camera feature

Just click "Allow" when prompted!

